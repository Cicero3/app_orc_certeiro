package com.example.api.orcamentos.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.UUID

class FormacaoPrecoCalculadoraTest {

    /**
     * Reproduz exatamente a aba "0. P. VENDA" da planilha de referência:
     * CD = 28.880,24 | CI = 0 | ADM 1% | Custo financeiro 1% | Imprevistos 1%
     * Tributos 5% (COFINS 1,5% + ISS 2% + CSLL 1,5%) | Lucro 20%
     * → Total custo 29.746,6472 | PV = 29.746,6472 ÷ (1 − 0,25) = 39.662,1963 | BDI 33,33%
     */
    @Test
    fun `deve reproduzir o preco de venda da planilha com markup divisor`() {
        val params = FormacaoPreco(
            orcamentoId = UUID.randomUUID(),
            admCentral = BigDecimal("0.01"),
            custoFinanceiro = BigDecimal("0.01"),
            contingencia = BigDecimal("0.01"),
            lucro = BigDecimal("0.20"),
            cofins = BigDecimal("0.015"),
            iss = BigDecimal("0.02"),
            csll = BigDecimal("0.015")
        )

        val resultado = FormacaoPrecoCalculadora.calcular(
            params,
            custoDireto = BigDecimal("28880.24"),
            custoIndireto = BigDecimal.ZERO
        )

        assertEquals(0, BigDecimal("28880.24").compareTo(resultado.baseCdCi))
        assertEquals(0, BigDecimal("288.8024").compareTo(resultado.admCentralValor))
        assertEquals(0, BigDecimal("29746.6472").compareTo(resultado.custoTotal))
        assertEquals(0, BigDecimal("0.05").compareTo(resultado.aliquotaTributos))
        // PV = 29746.6472 / 0.75 = 39662.1963 (mesma célula C27 da planilha)
        assertEquals(0, BigDecimal("39662.1963").compareTo(resultado.precoVenda))
        // BDI sobre custo total = 33,33% (célula C28)
        assertEquals(0, BigDecimal("0.3333").compareTo(resultado.bdiSobreCustoTotal))
        // Lucro em valor: 39662.1963 × 0.20 = 7932.4393 (célula D24)
        assertEquals(0, BigDecimal("7932.4393").compareTo(resultado.lucroValor))
    }

    @Test
    fun `bdi sobre custo direto deve fechar a planilha orcamentaria no preco de venda`() {
        val params = FormacaoPreco(
            orcamentoId = UUID.randomUUID(),
            lucro = BigDecimal("0.20"),
            iss = BigDecimal("0.05")
        )
        val custoDireto = BigDecimal("10000")
        val custoIndireto = BigDecimal("2000")

        val resultado = FormacaoPrecoCalculadora.calcular(params, custoDireto, custoIndireto)

        // PV = 12000 / 0.75 = 16000; BDI sobre CD = 16000/10000 − 1 = 0.60
        assertEquals(0, BigDecimal("16000").compareTo(resultado.precoVenda))
        assertEquals(0, BigDecimal("0.6000").compareTo(resultado.bdiSobreCustoDireto))
        // Aplicando: CD × (1 + BDI) = 10000 × 1.60 = 16000 = PV ✓
        assertEquals(
            0,
            resultado.precoVenda.compareTo(custoDireto.multiply(BigDecimal.ONE.add(resultado.bdiSobreCustoDireto)))
        )
    }

    @Test
    fun `deve bloquear percentuais sobre preco que somem 100 por cento ou mais`() {
        val params = FormacaoPreco(
            orcamentoId = UUID.randomUUID(),
            lucro = BigDecimal("0.60"),
            iss = BigDecimal("0.40")
        )
        assertThrows<DomainSecurityException> {
            FormacaoPrecoCalculadora.calcular(params, BigDecimal("1000"), BigDecimal.ZERO)
        }
    }

    @Test
    fun `orcamento sem custo deve resultar em preco zero sem divisao por zero`() {
        val params = FormacaoPreco(orcamentoId = UUID.randomUUID(), lucro = BigDecimal("0.20"))
        val resultado = FormacaoPrecoCalculadora.calcular(params, BigDecimal.ZERO, BigDecimal.ZERO)
        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.precoVenda))
        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.bdiSobreCustoDireto))
    }
}
