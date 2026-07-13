package com.example.api.orcamentos.application

import com.example.api.orcamentos.domain.EapItem
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.domain.OrcamentoModulo
import com.example.api.orcamentos.domain.TipoModulo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

/**
 * Valida as fórmulas da "Planilha Orçamentária" (aba PLAN ORÇA da planilha de referência):
 * totais MO/MAT/SERV por módulo, custo direto, preço com BDI e % de participação.
 */
class PlanilhaMapperTest {

    private fun novoOrcamento(bdi: String = "0.25") = Orcamento(
        tenantId = UUID.randomUUID(),
        ownerId = UUID.randomUUID(),
        titulo = "Reforma Teste",
        bdi = BigDecimal(bdi)
    )

    @Test
    fun `deve decompor totais em MO MAT SERV por modulo como a PLAN ORCA`() {
        val orcamento = novoOrcamento()
        val demolicao = OrcamentoModulo(tipoModulo = TipoModulo.DEMOLICAO_CONSTRUCAO, nome = "Demolição")
        orcamento.adicionarModulo(demolicao)

        // Espelha o item 2.1 da planilha: MO=30, MAT=2.50 por m², 10 m²
        demolicao.adicionarEapItem(
            EapItem(
                codigoItem = "2.1", descricao = "Demolição de alvenaria", unidade = "M2",
                quantidade = BigDecimal("10"),
                valorMo = BigDecimal("30"), valorMat = BigDecimal("2.50"), valorSrv = BigDecimal.ZERO
            )
        )
        // Item com serviço terceirizado (como o 2.9 da planilha)
        demolicao.adicionarEapItem(
            EapItem(
                codigoItem = "2.9", descricao = "Remoção de entulho", unidade = "UN",
                quantidade = BigDecimal("1"),
                valorMo = BigDecimal("80"), valorMat = BigDecimal.ZERO, valorSrv = BigDecimal("120")
            )
        )

        val dto = PlanilhaMapper.toDetailDto(orcamento)
        val modulo = dto.modulos.single()

        assertEquals(0, BigDecimal("380").compareTo(modulo.totalMo))   // 30×10 + 80×1
        assertEquals(0, BigDecimal("25").compareTo(modulo.totalMat))   // 2.5×10
        assertEquals(0, BigDecimal("120").compareTo(modulo.totalSrv))  // 120×1
        assertEquals(0, BigDecimal("525").compareTo(modulo.totalCusto))
        // Preço com BDI 25%: 525 × 1.25 = 656.25
        assertEquals(0, BigDecimal("656.25").compareTo(modulo.totalPreco))
        assertEquals(0, BigDecimal("525").compareTo(dto.totais.custoDireto))
        assertEquals(0, BigDecimal("656.25").compareTo(dto.totais.precoComBdi))
    }

    @Test
    fun `deve calcular percentual de participacao de cada modulo no custo direto`() {
        val orcamento = novoOrcamento()
        val m1 = OrcamentoModulo(tipoModulo = TipoModulo.DEMOLICAO_CONSTRUCAO, nome = "Demolição")
        val m2 = OrcamentoModulo(tipoModulo = TipoModulo.PINTURA, nome = "Pintura")
        orcamento.adicionarModulo(m1)
        orcamento.adicionarModulo(m2)

        m1.adicionarEapItem(EapItem(codigoItem = "1.1", descricao = "A", quantidade = BigDecimal("1"), valorMo = BigDecimal("750")))
        m2.adicionarEapItem(EapItem(codigoItem = "2.1", descricao = "B", quantidade = BigDecimal("1"), valorMo = BigDecimal("250")))

        val dto = PlanilhaMapper.toDetailDto(orcamento)

        assertEquals(0, BigDecimal("0.7500").compareTo(dto.modulos[0].percentual))
        assertEquals(0, BigDecimal("0.2500").compareTo(dto.modulos[1].percentual))
    }

    @Test
    fun `percentual deve ser zero quando orcamento esta vazio, sem divisao por zero`() {
        val orcamento = novoOrcamento()
        orcamento.adicionarModulo(OrcamentoModulo(tipoModulo = TipoModulo.OUTROS, nome = "Vazio"))

        val dto = PlanilhaMapper.toDetailDto(orcamento)

        assertEquals(0, BigDecimal.ZERO.compareTo(dto.modulos.single().percentual))
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.totais.custoDireto))
    }

    @Test
    fun `deve somar apenas itens folha quando ha subitens aninhados`() {
        val orcamento = novoOrcamento(bdi = "0")
        val modulo = OrcamentoModulo(tipoModulo = TipoModulo.HIDRAULICA, nome = "Hidráulica")
        orcamento.adicionarModulo(modulo)

        // Pai é agrupador (valores próprios não contam); folhas somam
        val pai = EapItem(codigoItem = "4", descricao = "Instalações", quantidade = BigDecimal("1"), valorMo = BigDecimal("999"))
        modulo.adicionarEapItem(pai)
        pai.adicionarSubItem(EapItem(codigoItem = "4.1", descricao = "Ponto de água", quantidade = BigDecimal("10"), valorMo = BigDecimal("50"), valorMat = BigDecimal("20.72")))
        pai.adicionarSubItem(EapItem(codigoItem = "4.2", descricao = "Ponto de esgoto", quantidade = BigDecimal("1"), valorMo = BigDecimal("70"), valorMat = BigDecimal("265.14")))

        val dto = PlanilhaMapper.toDetailDto(orcamento)
        val modDto = dto.modulos.single()

        // Folhas: (50+20.72)×10 + (70+265.14)×1 = 707.20 + 335.14 = 1042.34
        assertEquals(0, BigDecimal("1042.34").compareTo(modDto.totalCusto))
        // A árvore aparece aninhada no DTO
        assertEquals(1, modDto.itens.size)
        assertEquals(2, modDto.itens[0].subItens.size)
    }
}
