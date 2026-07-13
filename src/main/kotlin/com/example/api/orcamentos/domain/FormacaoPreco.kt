package com.example.api.orcamentos.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

/**
 * Parâmetros da formação de preço de venda (aba "0. P. VENDA" da planilha).
 * Percentuais como fração (0.05 = 5%). Tributos, comissão e lucro incidem sobre o
 * PREÇO DE VENDA, por isso entram no divisor do markup: PV = CustoTotal ÷ (1 − Σ%).
 */
@Entity
@Table(name = "formacoes_preco")
class FormacaoPreco(
    @Id
    @Column(name = "orcamento_id")
    val orcamentoId: UUID,

    @Column(name = "adm_central", nullable = false, precision = 5, scale = 4)
    var admCentral: BigDecimal = BigDecimal.ZERO,

    @Column(name = "custo_financeiro", nullable = false, precision = 5, scale = 4)
    var custoFinanceiro: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 5, scale = 4)
    var contingencia: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 5, scale = 4)
    var comissao: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 5, scale = 4)
    var lucro: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 5, scale = 4)
    var cofins: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 5, scale = 4)
    var pis: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 5, scale = 4)
    var icms: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 5, scale = 4)
    var iss: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 5, scale = 4)
    var irpj: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 5, scale = 4)
    var csll: BigDecimal = BigDecimal.ZERO
) {
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
        protected set

    val aliquotaTributos: BigDecimal
        get() = cofins.add(pis).add(icms).add(iss).add(irpj).add(csll)
}

/** Resultado do cálculo de formação de preço — todos os valores derivados, nada digitado. */
data class ResultadoFormacaoPreco(
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
    /** BDI sobre o custo total (informativo — como a célula C28 da planilha). */
    val bdiSobreCustoTotal: BigDecimal,
    /** BDI a aplicar sobre o CUSTO DIRETO para que a planilha orçamentária feche no preço de venda. */
    val bdiSobreCustoDireto: BigDecimal
)

/** Calculadora pura do markup divisor — espelha (e corrige) a aba "0. P. VENDA". */
object FormacaoPrecoCalculadora {

    private val MC = MathContext(20, RoundingMode.HALF_UP)

    fun calcular(params: FormacaoPreco, custoDireto: BigDecimal, custoIndireto: BigDecimal): ResultadoFormacaoPreco {
        val base = custoDireto.add(custoIndireto)
        val admValor = base.multiply(params.admCentral)
        val finValor = base.multiply(params.custoFinanceiro)
        val contingenciaValor = base.multiply(params.contingencia)
        val custoTotal = base.add(admValor).add(finValor).add(contingenciaValor)

        val aliquota = params.aliquotaTributos
        val percentuaisSobrePreco = aliquota.add(params.lucro).add(params.comissao)
        if (percentuaisSobrePreco >= BigDecimal.ONE) {
            throw DomainSecurityException(
                "Tributos + lucro + comissão (${percentuaisSobrePreco.multiply(BigDecimal(100))}%) devem somar menos de 100% do preço de venda."
            )
        }

        val divisor = BigDecimal.ONE.subtract(percentuaisSobrePreco)
        val precoVenda = if (custoTotal.signum() == 0) BigDecimal.ZERO
        else custoTotal.divide(divisor, MC).setScale(4, RoundingMode.HALF_UP)

        val bdiTotal = if (custoTotal.signum() == 0) BigDecimal.ZERO
        else precoVenda.divide(custoTotal, MC).subtract(BigDecimal.ONE).setScale(4, RoundingMode.HALF_UP)

        val bdiDireto = if (custoDireto.signum() == 0) BigDecimal.ZERO
        else precoVenda.divide(custoDireto, MC).subtract(BigDecimal.ONE).setScale(4, RoundingMode.HALF_UP)

        return ResultadoFormacaoPreco(
            custoDireto = custoDireto,
            custoIndireto = custoIndireto,
            baseCdCi = base,
            admCentralValor = admValor,
            custoFinanceiroValor = finValor,
            contingenciaValor = contingenciaValor,
            custoTotal = custoTotal,
            aliquotaTributos = aliquota,
            tributosValor = precoVenda.multiply(aliquota).setScale(4, RoundingMode.HALF_UP),
            comissaoValor = precoVenda.multiply(params.comissao).setScale(4, RoundingMode.HALF_UP),
            lucroValor = precoVenda.multiply(params.lucro).setScale(4, RoundingMode.HALF_UP),
            precoVenda = precoVenda,
            bdiSobreCustoTotal = bdiTotal,
            bdiSobreCustoDireto = bdiDireto
        )
    }
}
