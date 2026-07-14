package com.example.api.orcamentos.api.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

data class RiscoUpsertDto(
    @field:NotBlank(message = "Descrição do risco é obrigatória")
    @field:Size(max = 500)
    val descricao: String,

    @field:Size(max = 100)
    val categoria: String? = null,

    @field:DecimalMin(value = "0", message = "Probabilidade deve estar entre 0 e 1")
    @field:DecimalMax(value = "1", message = "Probabilidade deve estar entre 0 e 1")
    val probabilidade: BigDecimal,

    @field:DecimalMin(value = "0", message = "Impacto mínimo não pode ser negativo")
    val impactoMin: BigDecimal? = null,

    @field:DecimalMin(value = "0", message = "Impacto provável não pode ser negativo")
    val impactoProvavel: BigDecimal,

    @field:DecimalMin(value = "0", message = "Impacto máximo não pode ser negativo")
    val impactoMax: BigDecimal? = null,

    @field:Size(max = 500)
    val resposta: String? = null
)

data class RiscoDto(
    val id: UUID,
    val descricao: String,
    val categoria: String?,
    val probabilidade: BigDecimal,
    val impactoMin: BigDecimal?,
    val impactoProvavel: BigDecimal,
    val impactoMax: BigDecimal?,
    val resposta: String?,
    val valorEsperado: BigDecimal,
    // Classificação qualitativa (matriz P×I)
    val probabilidadeScore: Int,
    val probabilidadeLabel: String,
    val impactoScore: Int,
    val impactoLabel: String,
    val severidade: Int,
    val nivel: String
)

data class HistogramaFaixaDto(
    val inicio: BigDecimal,
    val fim: BigDecimal,
    val contagem: Int
)

data class SimulacaoDto(
    val iteracoes: Int,
    val seed: Long,
    val media: BigDecimal,
    val desvioPadrao: BigDecimal,
    val minimo: BigDecimal,
    val maximo: BigDecimal,
    val p10: BigDecimal,
    val p50: BigDecimal,
    val p80: BigDecimal,
    val p90: BigDecimal,
    val p95: BigDecimal,
    val icInferior95: BigDecimal,
    val icSuperior95: BigDecimal,
    val histograma: List<HistogramaFaixaDto>
)

data class AnaliseRiscoDto(
    val riscos: List<RiscoDto>,
    /** Custo direto do orçamento — referência da escala de impacto da matriz. */
    val custoDireto: BigDecimal,
    /** Σ probabilidade × impacto provável (matriz quantitativa). */
    val valorEsperadoTotal: BigDecimal,
    /** Resultado da Simulação de Monte Carlo (null quando não há riscos). */
    val simulacao: SimulacaoDto?,
    /** Contingência % atualmente aplicada na formação de preço (para comparação). */
    val contingenciaAtual: BigDecimal
)

data class AplicarContingenciaDto(
    /** Percentil da simulação a adotar como contingência: P50, P80, P90 ou P95. */
    @field:NotBlank val percentil: String,
    val iteracoes: Int = 10_000,
    val seed: Long? = null
)
