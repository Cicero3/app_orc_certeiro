package com.example.api.catalogos.api.dto

import java.math.BigDecimal
import java.util.UUID

data class CatalogoItemDetailDto(
    val id: UUID,
    val codigo: String,
    val descricao: String,
    val unidade: String,
    val valorMo: BigDecimal,
    val valorMat: BigDecimal,
    val valorSrv: BigDecimal,
    val insumos: List<CatalogoInsumoDto>
)

data class CatalogoInsumoDto(
    val id: UUID,
    val tipoInsumo: String,
    val codigo: String?,
    val descricao: String,
    val unidade: String,
    val coeficiente: BigDecimal,
    val custoUnitario: BigDecimal,
    val custoTotal: BigDecimal
)
