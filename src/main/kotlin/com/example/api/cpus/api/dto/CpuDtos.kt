package com.example.api.cpus.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

// ---------- Funções salariais ----------

data class FuncaoSalarialUpsertDto(
    @field:NotBlank(message = "Nome da função é obrigatório")
    @field:Size(max = 100)
    val nome: String,

    @field:DecimalMin(value = "0", message = "Valor-hora não pode ser negativo")
    val valorHora: BigDecimal
)

data class FuncaoSalarialDto(
    val id: UUID,
    val nome: String,
    val valorHora: BigDecimal
)

// ---------- CPUs próprias ----------

data class CpuInsumoUpsertDto(
    @field:NotBlank(message = "Tipo do insumo é obrigatório")
    val tipoInsumo: String,

    @field:NotBlank(message = "Descrição do insumo é obrigatória")
    @field:Size(max = 500)
    val descricao: String,

    @field:NotBlank(message = "Unidade do insumo é obrigatória")
    @field:Size(max = 20)
    val unidade: String,

    @field:DecimalMin(value = "0", message = "Coeficiente não pode ser negativo")
    val coeficiente: BigDecimal = BigDecimal.ZERO,

    @field:DecimalMin(value = "0", message = "Custo unitário não pode ser negativo")
    val custoUnitario: BigDecimal = BigDecimal.ZERO,

    /** Insumo de MO ligado à tabela salarial (custo-hora vivo). */
    val funcaoSalarialId: UUID? = null,

    /** Insumo que referencia uma composição auxiliar (ex.: argamassa). */
    val cpuReferenciaId: UUID? = null
)

data class CpuUpsertDto(
    @field:NotBlank(message = "Código é obrigatório")
    @field:Size(max = 50)
    val codigo: String,

    @field:NotBlank(message = "Descrição é obrigatória")
    @field:Size(max = 500)
    val descricao: String,

    @field:NotBlank(message = "Unidade é obrigatória")
    @field:Size(max = 20)
    val unidade: String,

    val isAuxiliar: Boolean = false,

    @field:Valid
    val insumos: List<CpuInsumoUpsertDto> = emptyList()
)

data class CpuInsumoDetailDto(
    val id: UUID,
    val tipoInsumo: String,
    val descricao: String,
    val unidade: String,
    val coeficiente: BigDecimal,
    /** Custo resolvido: CPU auxiliar > função salarial > custo digitado. */
    val custoUnitarioEfetivo: BigDecimal,
    val custoTotal: BigDecimal,
    val funcaoSalarialId: UUID?,
    val funcaoSalarialNome: String?,
    val cpuReferenciaId: UUID?,
    val cpuReferenciaCodigo: String?
)

data class CpuDetailDto(
    val id: UUID,
    val codigo: String,
    val descricao: String,
    val unidade: String,
    val isAuxiliar: Boolean,
    val valorMo: BigDecimal,
    val valorMat: BigDecimal,
    val valorSrv: BigDecimal,
    val valorUnitario: BigDecimal,
    val insumos: List<CpuInsumoDetailDto>
)

data class CpuSummaryDto(
    val id: UUID,
    val codigo: String,
    val descricao: String,
    val unidade: String,
    val isAuxiliar: Boolean,
    val valorUnitario: BigDecimal
)
