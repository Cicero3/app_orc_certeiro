package com.example.api.orcamentos.api.dto

import java.math.BigDecimal
import java.util.UUID

data class DimensionamentoItemDto(
    val itemId: UUID,
    val moduloNome: String,
    val codigoItem: String,
    val descricao: String,
    val unidade: String?,
    val quantidade: BigDecimal,
    /** Σ coeficientes de MO da CPU do item (h por unidade). */
    val indiceHorasPorUnidade: BigDecimal,
    val horasTotais: BigDecimal,
    /** ROUNDUP(horasTotais ÷ jornada). */
    val diasEquipeBasica: Int,
    /** false quando o item não tem insumos de MO na CPU (sem estimativa). */
    val temCpuDeMo: Boolean
)

data class DimensionamentoDto(
    val jornadaHorasDia: BigDecimal,
    val itens: List<DimensionamentoItemDto>,
    val totalHoras: BigDecimal,
    /** Soma dos dias item a item (execução sequencial, 1 equipe por serviço). */
    val totalDiasSequenciais: Int,
    /** Histograma de horas por função (descrição do insumo de MO). */
    val horasPorFuncao: Map<String, BigDecimal>
)
