package com.example.api.orcamentos.api.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

data class ModuloCreateDto(
    @field:NotBlank(message = "Nome do módulo é obrigatório")
    @field:Size(max = 255)
    val nome: String,
    val tipoModulo: String? = null
)

data class EapItemCreateDto(
    @field:NotBlank(message = "Código do item é obrigatório")
    @field:Size(max = 50)
    val codigoItem: String,

    @field:NotBlank(message = "Descrição é obrigatória")
    @field:Size(max = 500)
    val descricao: String,

    @field:Size(max = 255)
    val marca: String? = null,

    @field:Size(max = 20)
    val unidade: String? = null,

    @field:DecimalMin(value = "0", message = "Quantidade não pode ser negativa")
    val quantidade: BigDecimal = BigDecimal.ZERO,

    @field:DecimalMin(value = "0", message = "Valor de mão de obra não pode ser negativo")
    val valorMo: BigDecimal = BigDecimal.ZERO,

    @field:DecimalMin(value = "0", message = "Valor de material não pode ser negativo")
    val valorMat: BigDecimal = BigDecimal.ZERO,

    @field:DecimalMin(value = "0", message = "Valor de serviço não pode ser negativo")
    val valorSrv: BigDecimal = BigDecimal.ZERO,

    val observacoes: String? = null,

    /** Destino: módulo (item de primeiro nível) OU item pai (subitem). Exatamente um deve ser informado. */
    val moduloId: UUID? = null,
    val parentId: UUID? = null
)

data class EapItemUpdateDto(
    @field:DecimalMin(value = "0", message = "Quantidade não pode ser negativa")
    val quantidade: BigDecimal,

    @field:DecimalMin(value = "0", message = "Valor de mão de obra não pode ser negativo")
    val valorMo: BigDecimal,

    @field:DecimalMin(value = "0", message = "Valor de material não pode ser negativo")
    val valorMat: BigDecimal,

    @field:DecimalMin(value = "0", message = "Valor de serviço não pode ser negativo")
    val valorSrv: BigDecimal
)

data class EapItemFromCatalogoDto(
    val catalogoItemId: UUID,

    @field:DecimalMin(value = "0", message = "Quantidade não pode ser negativa")
    val quantidade: BigDecimal,

    /** Destino: módulo OU item pai. Exatamente um deve ser informado. */
    val moduloId: UUID? = null,
    val parentId: UUID? = null
)

data class EapItemFromCpuDto(
    val cpuId: UUID,

    @field:DecimalMin(value = "0", message = "Quantidade não pode ser negativa")
    val quantidade: BigDecimal,

    /** Destino: módulo OU item pai. Exatamente um deve ser informado. */
    val moduloId: UUID? = null,
    val parentId: UUID? = null
)

data class BdiUpdateDto(
    @field:DecimalMin(value = "0", message = "BDI não pode ser negativo")
    val bdi: BigDecimal
)

data class StatusActionDto(
    @field:NotBlank(message = "Ação é obrigatória")
    val acao: String
)
