package com.example.api.orcamentos.api.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class LevantamentoCreateDto(
    @field:NotBlank(message = "Tipo é obrigatório")
    @field:Size(max = 50)
    val tipo: String,

    @field:NotBlank(message = "Descrição é obrigatória")
    @field:Size(max = 500)
    val descricao: String,

    @field:NotBlank(message = "Unidade é obrigatória")
    @field:Size(max = 20)
    val unidade: String,

    @field:DecimalMin(value = "0", message = "Resultado não pode ser negativo")
    val resultado: BigDecimal,

    /** Inputs da calculadora serializados (JSON) — memória de cálculo. */
    val payload: String? = null,

    /** Item da EAP que receberá o resultado como quantidade (opcional). */
    val eapItemId: UUID? = null
)

data class LevantamentoDto(
    val id: UUID,
    val orcamentoId: UUID,
    val eapItemId: UUID?,
    val tipo: String,
    val descricao: String,
    val unidade: String,
    val resultado: BigDecimal,
    val payload: String?,
    val createdAt: Instant
)
