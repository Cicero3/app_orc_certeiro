package com.example.api.orcamentos.api.dto

import java.math.BigDecimal

data class OrcamentoCreateDto(
    val titulo: String,
    val bdi: BigDecimal? = null
)
