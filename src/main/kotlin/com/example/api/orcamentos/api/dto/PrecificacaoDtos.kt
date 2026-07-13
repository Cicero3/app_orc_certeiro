package com.example.api.orcamentos.api.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

// ---------- Custos indiretos ----------

data class CustoIndiretoUpsertDto(
    @field:NotBlank(message = "Categoria é obrigatória")
    val categoria: String,

    @field:NotBlank(message = "Descrição é obrigatória")
    @field:Size(max = 500)
    val descricao: String,

    @field:DecimalMin(value = "0", message = "Quantidade não pode ser negativa")
    val quantidade: BigDecimal = BigDecimal.ONE,

    @field:DecimalMin(value = "0", message = "Valor unitário não pode ser negativo")
    val valorUnitario: BigDecimal = BigDecimal.ZERO
)

data class CustoIndiretoDto(
    val id: UUID,
    val categoria: String,
    val descricao: String,
    val quantidade: BigDecimal,
    val valorUnitario: BigDecimal,
    val total: BigDecimal
)

data class CustosIndiretosResumoDto(
    val itens: List<CustoIndiretoDto>,
    val totalPorCategoria: Map<String, BigDecimal>,
    val total: BigDecimal
)

// ---------- Formação de preço ----------

data class FormacaoPrecoUpsertDto(
    @field:DecimalMin("0") @field:DecimalMax("1") val admCentral: BigDecimal = BigDecimal.ZERO,
    @field:DecimalMin("0") @field:DecimalMax("1") val custoFinanceiro: BigDecimal = BigDecimal.ZERO,
    @field:DecimalMin("0") @field:DecimalMax("1") val contingencia: BigDecimal = BigDecimal.ZERO,
    @field:DecimalMin("0") @field:DecimalMax("1") val comissao: BigDecimal = BigDecimal.ZERO,
    @field:DecimalMin("0") @field:DecimalMax("1") val lucro: BigDecimal = BigDecimal.ZERO,
    @field:DecimalMin("0") @field:DecimalMax("1") val cofins: BigDecimal = BigDecimal.ZERO,
    @field:DecimalMin("0") @field:DecimalMax("1") val pis: BigDecimal = BigDecimal.ZERO,
    @field:DecimalMin("0") @field:DecimalMax("1") val icms: BigDecimal = BigDecimal.ZERO,
    @field:DecimalMin("0") @field:DecimalMax("1") val iss: BigDecimal = BigDecimal.ZERO,
    @field:DecimalMin("0") @field:DecimalMax("1") val irpj: BigDecimal = BigDecimal.ZERO,
    @field:DecimalMin("0") @field:DecimalMax("1") val csll: BigDecimal = BigDecimal.ZERO
)

data class FormacaoPrecoDto(
    // Parâmetros
    val admCentral: BigDecimal,
    val custoFinanceiro: BigDecimal,
    val contingencia: BigDecimal,
    val comissao: BigDecimal,
    val lucro: BigDecimal,
    val cofins: BigDecimal,
    val pis: BigDecimal,
    val icms: BigDecimal,
    val iss: BigDecimal,
    val irpj: BigDecimal,
    val csll: BigDecimal,
    // Resultado calculado
    val custoDireto: BigDecimal,
    val custoIndireto: BigDecimal,
    val baseCdCi: BigDecimal,
    val admCentralValor: BigDecimal,
    val custoFinanceiroValor: BigDecimal,
    val contingenciaValor: BigDecimal,
    val custoTotal: BigDecimal,
    val aliquotaTributos: BigDecimal,
    val tributosValor: BigDecimal,
    val comissaoValor: BigDecimal,
    val lucroValor: BigDecimal,
    val precoVenda: BigDecimal,
    val bdiSobreCustoTotal: BigDecimal,
    val bdiSobreCustoDireto: BigDecimal,
    /** BDI atualmente aplicado no orçamento (para comparação). */
    val bdiAtualDoOrcamento: BigDecimal
)
