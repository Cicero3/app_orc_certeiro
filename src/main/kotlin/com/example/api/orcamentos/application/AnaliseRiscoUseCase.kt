package com.example.api.orcamentos.application

import com.example.api.orcamentos.api.dto.AnaliseRiscoDto
import com.example.api.orcamentos.api.dto.AplicarContingenciaDto
import com.example.api.orcamentos.api.dto.HistogramaFaixaDto
import com.example.api.orcamentos.api.dto.RiscoDto
import com.example.api.orcamentos.api.dto.RiscoUpsertDto
import com.example.api.orcamentos.api.dto.SimulacaoDto
import com.example.api.orcamentos.domain.AnaliseRiscoCalculadora
import com.example.api.orcamentos.domain.DomainSecurityException
import com.example.api.orcamentos.domain.FormacaoPreco
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.domain.Risco
import com.example.api.orcamentos.infrastructure.CustoIndiretoRepository
import com.example.api.orcamentos.infrastructure.FormacaoPrecoRepository
import com.example.api.orcamentos.infrastructure.OrcamentoRepository
import com.example.api.orcamentos.infrastructure.RiscoRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Registro de riscos, matriz P×I, valor esperado e contingência por
 * Simulação de Monte Carlo (planilhas de referência 008/009).
 */
@Service
class AnaliseRiscoUseCase(
    private val orcamentoRepository: OrcamentoRepository,
    private val riscoRepository: RiscoRepository,
    private val custoIndiretoRepository: CustoIndiretoRepository,
    private val formacaoPrecoRepository: FormacaoPrecoRepository
) {

    // ---------- CRUD ----------

    @Transactional
    fun adicionarRisco(userId: UUID, orcamentoId: UUID, dto: RiscoUpsertDto): RiscoDto {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        checkMutavel(orcamento)
        val risco = Risco(
            orcamentoId = orcamentoId,
            descricao = dto.descricao,
            categoria = dto.categoria,
            probabilidade = dto.probabilidade,
            impactoMin = dto.impactoMin,
            impactoProvavel = dto.impactoProvavel,
            impactoMax = dto.impactoMax,
            resposta = dto.resposta
        )
        return riscoRepository.save(risco).toDto(orcamento.custoDireto)
    }

    @Transactional
    fun atualizarRisco(userId: UUID, orcamentoId: UUID, riscoId: UUID, dto: RiscoUpsertDto): RiscoDto {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        checkMutavel(orcamento)
        val risco = carregarRiscoDoOrcamento(riscoId, orcamentoId)
        risco.atualizar(dto.descricao, dto.categoria, dto.probabilidade, dto.impactoMin, dto.impactoProvavel, dto.impactoMax, dto.resposta)
        return riscoRepository.save(risco).toDto(orcamento.custoDireto)
    }

    @Transactional
    fun removerRisco(userId: UUID, orcamentoId: UUID, riscoId: UUID) {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        checkMutavel(orcamento)
        riscoRepository.delete(carregarRiscoDoOrcamento(riscoId, orcamentoId))
    }

    // ---------- Análise ----------

    @Transactional(readOnly = true)
    fun analisar(userId: UUID, orcamentoId: UUID, iteracoes: Int, seed: Long?): AnaliseRiscoDto {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        val riscos = riscoRepository.findAllByOrcamentoIdOrderByCreatedAtAsc(orcamentoId)
        return montarAnalise(orcamento, riscos, iteracoes, seed)
    }

    /**
     * Roda a simulação e grava o percentil escolhido como contingência (%) na
     * formação de preço: contingência = valorPercentil ÷ (CD + CI).
     */
    @Transactional
    fun aplicarContingencia(userId: UUID, orcamentoId: UUID, dto: AplicarContingenciaDto): AnaliseRiscoDto {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        checkMutavel(orcamento)
        val riscos = riscoRepository.findAllByOrcamentoIdOrderByCreatedAtAsc(orcamentoId)
        val analise = montarAnalise(orcamento, riscos, dto.iteracoes, dto.seed)
        val simulacao = analise.simulacao
            ?: throw IllegalArgumentException("Cadastre riscos antes de aplicar a contingência.")

        val valor = when (dto.percentil.uppercase()) {
            "P50" -> simulacao.p50
            "P80" -> simulacao.p80
            "P90" -> simulacao.p90
            "P95" -> simulacao.p95
            else -> throw IllegalArgumentException("Percentil inválido: ${dto.percentil}. Use P50, P80, P90 ou P95.")
        }

        val custoIndireto = totalCustosIndiretos(orcamentoId)
        val base = orcamento.custoDireto.add(custoIndireto)
        if (base.signum() <= 0) {
            throw IllegalArgumentException("Orçamento sem custo — adicione itens à EAP antes de aplicar a contingência.")
        }
        val percentual = valor.divide(base, 4, RoundingMode.HALF_UP)

        val params = formacaoPrecoRepository.findById(orcamentoId).orElse(FormacaoPreco(orcamentoId = orcamentoId))
        params.contingencia = percentual
        formacaoPrecoRepository.save(params)

        return analise.copy(contingenciaAtual = percentual)
    }

    // ---------- Internos ----------

    private fun montarAnalise(orcamento: Orcamento, riscos: List<Risco>, iteracoes: Int, seed: Long?): AnaliseRiscoDto {
        val custoDireto = orcamento.custoDireto
        val riscosDto = riscos.map { it.toDto(custoDireto) }

        val simulacao = if (riscos.isEmpty()) null else {
            val resultado = AnaliseRiscoCalculadora.simular(
                riscos.map {
                    AnaliseRiscoCalculadora.RiscoSimulavel(
                        probabilidade = it.probabilidade.toDouble(),
                        impactoMin = it.impactoMin?.toDouble(),
                        impactoProvavel = it.impactoProvavel.toDouble(),
                        impactoMax = it.impactoMax?.toDouble()
                    )
                },
                iteracoes = iteracoes,
                seed = seed
            )
            SimulacaoDto(
                iteracoes = resultado.iteracoes,
                seed = resultado.seed,
                media = resultado.media,
                desvioPadrao = resultado.desvioPadrao,
                minimo = resultado.minimo,
                maximo = resultado.maximo,
                p10 = resultado.p10,
                p50 = resultado.p50,
                p80 = resultado.p80,
                p90 = resultado.p90,
                p95 = resultado.p95,
                icInferior95 = resultado.icInferior95,
                icSuperior95 = resultado.icSuperior95,
                histograma = resultado.histograma.map { HistogramaFaixaDto(it.inicio, it.fim, it.contagem) }
            )
        }

        val contingenciaAtual = formacaoPrecoRepository.findById(orcamento.id)
            .map { it.contingencia }.orElse(BigDecimal.ZERO)

        return AnaliseRiscoDto(
            riscos = riscosDto,
            custoDireto = custoDireto,
            valorEsperadoTotal = riscos.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.valorEsperado) },
            simulacao = simulacao,
            contingenciaAtual = contingenciaAtual
        )
    }

    private fun Risco.toDto(custoReferencia: BigDecimal): RiscoDto {
        val classificacao = AnaliseRiscoCalculadora.classificar(probabilidade, impactoProvavel, custoReferencia)
        return RiscoDto(
            id = id,
            descricao = descricao,
            categoria = categoria,
            probabilidade = probabilidade,
            impactoMin = impactoMin,
            impactoProvavel = impactoProvavel,
            impactoMax = impactoMax,
            resposta = resposta,
            valorEsperado = valorEsperado,
            probabilidadeScore = classificacao.probabilidadeScore,
            probabilidadeLabel = classificacao.probabilidadeLabel,
            impactoScore = classificacao.impactoScore,
            impactoLabel = classificacao.impactoLabel,
            severidade = classificacao.severidade,
            nivel = classificacao.nivel.name
        )
    }

    private fun totalCustosIndiretos(orcamentoId: UUID): BigDecimal =
        custoIndiretoRepository.findAllByOrcamentoIdOrderByCategoriaAscDescricaoAsc(orcamentoId)
            .fold(BigDecimal.ZERO) { acc, c -> acc.add(c.total) }

    private fun carregarOrcamentoDoDono(userId: UUID, orcamentoId: UUID): Orcamento {
        val orcamento = orcamentoRepository.findById(orcamentoId)
            .orElseThrow { EntityNotFoundException("Orçamento não encontrado") }
        if (orcamento.ownerId != userId) {
            throw DomainSecurityException("Você não tem permissão para acessar este orçamento.")
        }
        return orcamento
    }

    private fun carregarRiscoDoOrcamento(riscoId: UUID, orcamentoId: UUID): Risco {
        val risco = riscoRepository.findById(riscoId)
            .orElseThrow { EntityNotFoundException("Risco não encontrado") }
        if (risco.orcamentoId != orcamentoId) {
            throw DomainSecurityException("Risco não pertence a este orçamento.")
        }
        return risco
    }

    private fun checkMutavel(orcamento: Orcamento) {
        if (orcamento.status.isImutavel) {
            throw DomainSecurityException("Orçamento em status ${orcamento.status} é imutável. Edição bloqueada.")
        }
    }
}
