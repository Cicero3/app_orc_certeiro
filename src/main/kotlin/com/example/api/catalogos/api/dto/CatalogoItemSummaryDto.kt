package com.example.api.catalogos.api.dto

import java.math.BigDecimal
import java.util.UUID

data class CatalogoItemSummaryDto(
    val id: UUID,
    val codigo: String,
    val descricao: String,
    val unidade: String,
    val valorTotal: BigDecimal
)
