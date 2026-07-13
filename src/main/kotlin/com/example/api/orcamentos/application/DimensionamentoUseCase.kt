package com.example.api.orcamentos.application

import com.example.api.orcamentos.api.dto.DimensionamentoDto
import com.example.api.orcamentos.api.dto.DimensionamentoItemDto
import com.example.api.orcamentos.domain.DomainSecurityException
import com.example.api.orcamentos.domain.EapItem
import com.example.api.orcamentos.domain.TipoInsumo
import com.example.api.orcamentos.infrastructure.OrcamentoRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.UUID

/**
 * Dimensionamento de equipes e prazos (aba "DIM EQP" da planilha):
 * Dias da equipe básica = quantidade ÷ (jornada ÷ índice), onde o índice (h/unidade)
 * vem dos coeficientes de mão de obra da CPU de cada item — mesma fonte do custo.
 * Arredondamento sempre para cima (ROUNDUP), sem #DIV/0!.
 */
@Service
class DimensionamentoUseCase(
    private val orcamentoRepository: OrcamentoRepository
) {
    private val mc = MathContext(20, RoundingMode.HALF_UP)

    @Transactional(readOnly = true)
    fun calcular(userId: UUID, orcamentoId: UUID, jornadaHorasDia: BigDecimal): DimensionamentoDto {
        if (jornadaHorasDia <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Jornada (h/dia) deve ser maior que zero.")
        }

        val orcamento = orcamentoRepository.findById(orcamentoId)
            .orElseThrow { EntityNotFoundException("Orçamento não encontrado") }
        if (orcamento.ownerId != userId) {
            throw DomainSecurityException("Você não tem permissão para acessar este orçamento.")
        }

        val horasPorFuncao = mutableMapOf<String, BigDecimal>()
        val itensDto = mutableListOf<DimensionamentoItemDto>()

        orcamento.modulos.forEach { modulo ->
            modulo.eapItens.flatMap { coletarFolhas(it) }.forEach { item ->
                val insumosMo = item.composicoes.filter { it.tipoInsumo == TipoInsumo.MAO_DE_OBRA }
                val indice = insumosMo.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.coeficiente) }
                val horasTotais = item.quantidade.multiply(indice)
                val dias = if (horasTotais.signum() == 0) 0
                else horasTotais.divide(jornadaHorasDia, mc).setScale(0, RoundingMode.CEILING).toInt()

                insumosMo.forEach { insumo ->
                    val horas = insumo.coeficiente.multiply(item.quantidade)
                    horasPorFuncao.merge(insumo.descricao, horas) { a, b -> a.add(b) }
                }

                itensDto.add(
                    DimensionamentoItemDto(
                        itemId = item.id,
                        moduloNome = modulo.nome,
                        codigoItem = item.codigoItem,
                        descricao = item.descricao,
                        unidade = item.unidade,
                        quantidade = item.quantidade,
                        indiceHorasPorUnidade = indice,
                        horasTotais = horasTotais,
                        diasEquipeBasica = dias,
                        temCpuDeMo = insumosMo.isNotEmpty()
                    )
                )
            }
        }

        return DimensionamentoDto(
            jornadaHorasDia = jornadaHorasDia,
            itens = itensDto,
            totalHoras = itensDto.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.horasTotais) },
            totalDiasSequenciais = itensDto.sumOf { it.diasEquipeBasica },
            horasPorFuncao = horasPorFuncao.toSortedMap()
        )
    }

    private fun coletarFolhas(item: EapItem): List<EapItem> =
        if (item.subItens.isEmpty()) listOf(item)
        else item.subItens.flatMap { coletarFolhas(it) }
}
