package com.example.api.orcamentos.api.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class OrcamentoSummaryDto(
    val id: UUID,
    val titulo: String,
    val status: String,
    val valorTotal: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant
)
