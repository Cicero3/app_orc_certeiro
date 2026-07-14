package com.example.api.orcamentos.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.math.BigDecimal
import java.util.UUID

// ---------- Escrita ----------

data class CronogramaAlocacaoUpsertDto(
    val moduloId: UUID,

    @field:Min(1) @field:Max(120)
    val periodo: Int,

    @field:DecimalMin("0") @field:DecimalMax("1")
    val previstoPct: BigDecimal = BigDecimal.ZERO,

    @field:DecimalMin("0") @field:DecimalMax("1")
    val realPct: BigDecimal = BigDecimal.ZERO
)

data class CronogramaSalvarDto(
    @field:Valid
    val alocacoes: List<CronogramaAlocacaoUpsertDto>
)

// ---------- Leitura ----------

data class CronogramaCelulaDto(
    val periodo: Int,
    val previstoPct: BigDecimal,
    val realPct: BigDecimal,
    val previstoValor: BigDecimal,
    val realValor: BigDecimal
)

data class CronogramaLinhaDto(
    val moduloId: UUID,
    val moduloNome: String,
    /** Preço do módulo com BDI — base financeira da linha, como a coluna 'Vlr Total' da planilha 005. */
    val valorTotal: BigDecimal,
    val celulas: List<CronogramaCelulaDto>,
    val totalPrevistoPct: BigDecimal,
    val totalRealPct: BigDecimal,
    /** Avanço real em R$: total real % × valor do módulo. */
    val avancoRealValor: BigDecimal
)

data class CronogramaPeriodoDto(
    val periodo: Int,
    // Avanço físico ponderado por valor (fração do total do orçamento)
    val fisicoPrevistoPct: BigDecimal,
    val fisicoRealPct: BigDecimal,
    val fisicoPrevistoAcumuladoPct: BigDecimal,
    val fisicoRealAcumuladoPct: BigDecimal,
    // Curva de desembolso (R$)
    val financeiroPrevisto: BigDecimal,
    val financeiroReal: BigDecimal,
    val financeiroPrevistoAcumulado: BigDecimal,
    val financeiroRealAcumulado: BigDecimal
)

data class CronogramaDto(
    val periodos: Int,
    val valorTotal: BigDecimal,
    val linhas: List<CronogramaLinhaDto>,
    /** Agregados por período — a curva S vem dos acumulados. */
    val porPeriodo: List<CronogramaPeriodoDto>,
    /** Desvio físico acumulado no último período com medição real (real − previsto). */
    val desvioFisicoPct: BigDecimal
)
