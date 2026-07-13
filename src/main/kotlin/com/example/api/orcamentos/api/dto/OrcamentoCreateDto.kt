package com.example.api.orcamentos.api.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class OrcamentoCreateDto(
    @field:NotBlank(message = "Título é obrigatório")
    @field:Size(max = 255)
    val titulo: String,

    @field:DecimalMin(value = "0", message = "BDI não pode ser negativo")
    val bdi: BigDecimal? = null
)
