package com.example.api.orcamentos.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.UUID

class OrcamentoTest {

    private val tenantId = UUID.randomUUID()
    private val ownerId = UUID.randomUUID()
    private val aprovadorId = UUID.randomUUID()

    @Test
    fun `should create orcamento with status RASCUNHO`() {
        val orcamento = Orcamento(tenantId = tenantId, ownerId = ownerId, titulo = "Teste")
        assertEquals(OrcamentoStatus.RASCUNHO, orcamento.status)
        assertEquals(BigDecimal.ZERO, orcamento.bdi)
    }

    @Test
    fun `should calculate total value correctly`() {
        val orcamento = Orcamento(tenantId = tenantId, ownerId = ownerId, titulo = "Teste", bdi = BigDecimal("0.1000")) // 10% BDI
        
        val modulo = OrcamentoModulo(tipoModulo = TipoModulo.FUNDACAO, nome = "Fundação")
        orcamento.adicionarModulo(modulo)

        val item1 = EapItem(codigoItem = "1.1", descricao = "Sapata", quantidade = BigDecimal("10"), valorMo = BigDecimal("30"), valorMat = BigDecimal("20")) // Unit = 50, Total = 500
        val item2 = EapItem(codigoItem = "1.2", descricao = "Concreto", quantidade = BigDecimal("100"), valorMo = BigDecimal("1"), valorMat = BigDecimal("1")) // Unit = 2, Total = 200
        
        modulo.adicionarEapItem(item1)
        modulo.adicionarEapItem(item2)

        // Total itens = 700. BDI 10% = 70. Total = 770.
        assertEquals(0, BigDecimal("770.0").compareTo(orcamento.valorTotal))
    }

    @Test
    fun `should not allow negative values in EAP items`() {
        assertThrows<DomainSecurityException> {
            EapItem(codigoItem = "1.1", descricao = "Erro", quantidade = BigDecimal("-5"), valorMo = BigDecimal("10"))
        }
    }

    @Test
    fun `should allow adding items when status is RASCUNHO`() {
        val orcamento = Orcamento(tenantId = tenantId, ownerId = ownerId, titulo = "Teste")
        val modulo = OrcamentoModulo(tipoModulo = TipoModulo.FUNDACAO, nome = "Fundação")
        orcamento.adicionarModulo(modulo)
        modulo.adicionarEapItem(EapItem(codigoItem = "1.1", descricao = "Ok", quantidade = BigDecimal("1"), valorMo = BigDecimal("10")))
        assertEquals(1, modulo.eapItens.size)
    }

    @Test
    fun `should not allow adding modules when status is imutavel`() {
        val orcamento = Orcamento(tenantId = tenantId, ownerId = ownerId, titulo = "Teste")
        
        // Simular transição para aprovado para testar a imutabilidade
        orcamento.solicitarAprovacao(ownerId)
        orcamento.aprovar(aprovadorId, true)

        assertThrows<DomainSecurityException> {
            orcamento.adicionarModulo(OrcamentoModulo(tipoModulo = TipoModulo.FUNDACAO, nome = "Fundação"))
        }
    }

    @Test
    fun `should not allow updating EAP values when orcamento is imutavel`() {
        val orcamento = Orcamento(tenantId = tenantId, ownerId = ownerId, titulo = "Teste")
        val modulo = OrcamentoModulo(tipoModulo = TipoModulo.FUNDACAO, nome = "Fundação")
        val item = EapItem(codigoItem = "1.1", descricao = "Ok", quantidade = BigDecimal("1"), valorMo = BigDecimal("10"))
        orcamento.adicionarModulo(modulo)
        modulo.adicionarEapItem(item)
        
        orcamento.solicitarAprovacao(ownerId)
        orcamento.aprovar(aprovadorId, true)

        assertThrows<DomainSecurityException> {
            item.atualizarValores(BigDecimal("2"), BigDecimal("20"), BigDecimal.ZERO, BigDecimal.ZERO)
        }
    }

    @Test
    fun `should follow valid state transitions`() {
        val orcamento = Orcamento(tenantId = tenantId, ownerId = ownerId, titulo = "Teste")
        
        orcamento.solicitarAprovacao(ownerId)
        assertEquals(OrcamentoStatus.EM_REVISAO, orcamento.status)

        orcamento.aprovar(aprovadorId, true)
        assertEquals(OrcamentoStatus.APROVADO, orcamento.status)

        orcamento.enviarAoCliente()
        assertEquals(OrcamentoStatus.ENVIADO_CLIENTE, orcamento.status)

        orcamento.registrarRespostaCliente(true)
        assertEquals(OrcamentoStatus.ACEITO, orcamento.status)
    }

    @Test
    fun `should block unauthorized transitions`() {
        val orcamento = Orcamento(tenantId = tenantId, ownerId = ownerId, titulo = "Teste")
        
        // Dono solicitando aprovação (ok)
        orcamento.solicitarAprovacao(ownerId)
        
        // Aprovador não admin não pode aprovar
        assertThrows<DomainSecurityException> {
            orcamento.aprovar(aprovadorId, false)
        }
    }
}
