package com.example.api.orcamentos.application

import com.example.api.orcamentos.api.dto.CronogramaCelulaDto
import com.example.api.orcamentos.api.dto.CronogramaDto
import com.example.api.orcamentos.api.dto.CronogramaLinhaDto
import com.example.api.orcamentos.api.dto.CronogramaPeriodoDto
import com.example.api.orcamentos.api.dto.CronogramaSalvarDto
import com.example.api.orcamentos.domain.CronogramaAlocacao
import com.example.api.orcamentos.domain.DomainSecurityException
import com.example.api.orcamentos.domain.EapItem
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.infrastructure.CronogramaAlocacaoRepository
import com.example.api.orcamentos.infrastructure.OrcamentoRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Cronograma físico-financeiro (planilha 005): alocação % prevista × real por
 * módulo da EAP e período, com avanço físico ponderado por valor, curva de
 * desembolso e acumulados (curva S). Tudo derivado — a planilha original
 * quebrava com #REF! porque os valores eram colados de outro arquivo.
 */
@Service
class CronogramaUseCase(
    private val orcamentoRepository: OrcamentoRepository,
    private val alocacaoRepository: CronogramaAlocacaoRepository
) {

    @Transactional(readOnly = true)
    fun obter(userId: UUID, orcamentoId: UUID, periodos: Int?): CronogramaDto {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        val alocacoes = alocacaoRepository.findAllByOrcamentoId(orcamentoId)
        return montar(orcamento, alocacoes, periodos)
    }

    /** Upsert em lote das células do grid (módulo × período). */
    @Transactional
    fun salvar(userId: UUID, orcamentoId: UUID, dto: CronogramaSalvarDto, periodos: Int?): CronogramaDto {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)

        val modulosValidos = orcamento.modulos.map { it.id }.toSet()

        dto.alocacoes.forEach { celula ->
            if (celula.moduloId !in modulosValidos) {
                throw DomainSecurityException("Módulo ${celula.moduloId} não pertence a este orçamento.")
            }
            val existente = alocacaoRepository.findByOrcamentoIdAndModuloIdAndPeriodo(orcamentoId, celula.moduloId, celula.periodo)
            if (existente != null) {
                existente.atualizar(celula.previstoPct, celula.realPct)
                alocacaoRepository.save(existente)
            } else {
                alocacaoRepository.save(
                    CronogramaAlocacao(
                        orcamentoId = orcamentoId,
                        moduloId = celula.moduloId,
                        periodo = celula.periodo,
                        previstoPct = celula.previstoPct,
                        realPct = celula.realPct
                    )
                )
            }
        }

        // Valida os somatórios por módulo APÓS o upsert (previsto e real ≤ 100%)
        val todas = alocacaoRepository.findAllByOrcamentoId(orcamentoId)
        todas.groupBy { it.moduloId }.forEach { (moduloId, doModulo) ->
            val somaPrevisto = doModulo.fold(BigDecimal.ZERO) { acc, a -> acc.add(a.previstoPct) }
            val somaReal = doModulo.fold(BigDecimal.ZERO) { acc, a -> acc.add(a.realPct) }
            if (somaPrevisto > BigDecimal.ONE || somaReal > BigDecimal.ONE) {
                val nome = orcamento.modulos.firstOrNull { it.id == moduloId }?.nome ?: moduloId.toString()
                throw IllegalArgumentException(
                    "Alocação do módulo '$nome' ultrapassa 100% (previsto ${pct(somaPrevisto)}, real ${pct(somaReal)})."
                )
            }
        }

        return montar(orcamento, todas, periodos)
    }

    // ---------- Montagem ----------

    private fun montar(orcamento: Orcamento, alocacoes: List<CronogramaAlocacao>, periodosParam: Int?): CronogramaDto {
        val fatorBdi = BigDecimal.ONE.add(orcamento.bdi)
        val maiorPeriodoUsado = alocacoes.maxOfOrNull { it.periodo } ?: 0
        val periodos = (periodosParam ?: 12).coerceAtLeast(maiorPeriodoUsado).coerceIn(1, 120)

        val porModulo = alocacoes.groupBy { it.moduloId }

        val linhas = orcamento.modulos.map { modulo ->
            val valorModulo = modulo.eapItens.flatMap { coletarFolhas(it) }
                .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.custoTotal) }
                .multiply(fatorBdi)

            val celulasDoModulo = porModulo[modulo.id].orEmpty().associateBy { it.periodo }
            val celulas = (1..periodos).map { p ->
                val alocacao = celulasDoModulo[p]
                val previsto = alocacao?.previstoPct ?: BigDecimal.ZERO
                val real = alocacao?.realPct ?: BigDecimal.ZERO
                CronogramaCelulaDto(
                    periodo = p,
                    previstoPct = previsto,
                    realPct = real,
                    previstoValor = previsto.multiply(valorModulo),
                    realValor = real.multiply(valorModulo)
                )
            }

            val totalPrevisto = celulas.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.previstoPct) }
            val totalReal = celulas.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.realPct) }

            CronogramaLinhaDto(
                moduloId = modulo.id,
                moduloNome = modulo.nome,
                valorTotal = valorModulo,
                celulas = celulas,
                totalPrevistoPct = totalPrevisto,
                totalRealPct = totalReal,
                avancoRealValor = totalReal.multiply(valorModulo)
            )
        }

        val valorTotal = linhas.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.valorTotal) }

        var previstoAcumulado = BigDecimal.ZERO
        var realAcumulado = BigDecimal.ZERO
        val porPeriodo = (1..periodos).map { p ->
            val financeiroPrevisto = linhas.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.celulas[p - 1].previstoValor) }
            val financeiroReal = linhas.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.celulas[p - 1].realValor) }
            previstoAcumulado = previstoAcumulado.add(financeiroPrevisto)
            realAcumulado = realAcumulado.add(financeiroReal)

            CronogramaPeriodoDto(
                periodo = p,
                fisicoPrevistoPct = fracao(financeiroPrevisto, valorTotal),
                fisicoRealPct = fracao(financeiroReal, valorTotal),
                fisicoPrevistoAcumuladoPct = fracao(previstoAcumulado, valorTotal),
                fisicoRealAcumuladoPct = fracao(realAcumulado, valorTotal),
                financeiroPrevisto = financeiroPrevisto,
                financeiroReal = financeiroReal,
                financeiroPrevistoAcumulado = previstoAcumulado,
                financeiroRealAcumulado = realAcumulado
            )
        }

        // Desvio no último período em que houve medição real
        val ultimoComReal = porPeriodo.lastOrNull { it.financeiroReal.signum() > 0 }
        val desvio = ultimoComReal
            ?.let { it.fisicoRealAcumuladoPct.subtract(it.fisicoPrevistoAcumuladoPct) }
            ?: BigDecimal.ZERO

        return CronogramaDto(
            periodos = periodos,
            valorTotal = valorTotal,
            linhas = linhas,
            porPeriodo = porPeriodo,
            desvioFisicoPct = desvio
        )
    }

    private fun fracao(parte: BigDecimal, total: BigDecimal): BigDecimal =
        if (total.signum() == 0) BigDecimal.ZERO
        else parte.divide(total, 4, RoundingMode.HALF_UP)

    private fun pct(v: BigDecimal): String = "${v.multiply(BigDecimal(100)).setScale(1, RoundingMode.HALF_UP)}%"

    private fun coletarFolhas(item: EapItem): List<EapItem> =
        if (item.subItens.isEmpty()) listOf(item)
        else item.subItens.flatMap { coletarFolhas(it) }

    private fun carregarOrcamentoDoDono(userId: UUID, orcamentoId: UUID): Orcamento {
        val orcamento = orcamentoRepository.findById(orcamentoId)
            .orElseThrow { EntityNotFoundException("Orçamento não encontrado") }
        if (orcamento.ownerId != userId) {
            throw DomainSecurityException("Você não tem permissão para acessar este orçamento.")
        }
        return orcamento
    }
}
