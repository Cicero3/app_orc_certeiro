package com.example.api.orcamentos.api.dto

import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class DiarioObraUpsertDto(
    @field:NotNull(message = "Data é obrigatória")
    val data: LocalDate,
    /** BOM, NUBLADO, CHUVOSO ou IMPRATICAVEL (por turno, opcionais). */
    val climaManha: String? = null,
    val climaTarde: String? = null,
    val climaNoite: String? = null,
    /** JSON: [{"funcao":"Pedreiro","qtde":3}]. */
    val maoDeObra: String? = null,
    /** JSON: [{"descricao":"Betoneira 400L","qtde":1}]. */
    val equipamentos: String? = null,
    val atividades: String? = null,
    val ocorrencias: String? = null,
    val observacoes: String? = null
)

data class DiarioObraDto(
    val id: UUID,
    val data: LocalDate,
    val climaManha: String?,
    val climaTarde: String?,
    val climaNoite: String?,
    val maoDeObra: String?,
    val equipamentos: String?,
    val atividades: String?,
    val ocorrencias: String?,
    val observacoes: String?,
    val createdAt: Instant
)
