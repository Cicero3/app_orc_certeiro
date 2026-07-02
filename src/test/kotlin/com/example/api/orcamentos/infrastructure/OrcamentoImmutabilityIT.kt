package com.example.api.orcamentos.infrastructure

import com.example.api.AbstractIntegrationTest
import com.example.api.auth.domain.User
import com.example.api.auth.infrastructure.UserRepository
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.domain.OrcamentoModulo
import com.example.api.orcamentos.domain.TipoModulo
import com.example.api.orcamentos.domain.EapItem
import com.example.api.orcamentos.domain.OrcamentoStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.math.BigDecimal
import java.util.UUID

class OrcamentoImmutabilityIT : AbstractIntegrationTest() {

    @Autowired
    private lateinit var orcamentoRepository: OrcamentoRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testUser: User
    private val tenantId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        // Precisa criar um usuário real para respeitar a foreign key
        val user = User(
            email = "test${UUID.randomUUID()}@example.com",
            passwordHash = "hash",
            role = "USER"
        )
        testUser = userRepository.save(user)
    }

    @Test
    fun `database trigger should prevent financial updates on immutable budget`() {
        // 1. Criar orçamento e aprovar via código (simulando estado já no banco)
        val orcamento = Orcamento(
            tenantId = tenantId,
            ownerId = testUser.id,
            titulo = "Orçamento Teste",
            bdi = BigDecimal("0.1000")
        )
        val modulo = OrcamentoModulo(tipoModulo = TipoModulo.FUNDACAO, nome = "Fundação")
        orcamento.adicionarModulo(modulo)
        modulo.adicionarEapItem(EapItem(codigoItem = "1", descricao = "Item 1", quantidade = BigDecimal("10"), valorMo = BigDecimal("50")))
        
        // Salva primeiro como RASCUNHO (status inicial) para inserir os itens
        var savedOrcamento = orcamentoRepository.saveAndFlush(orcamento)
        
        savedOrcamento.solicitarAprovacao(testUser.id)
        savedOrcamento.aprovar(testUser.id, true) // Forçando aprovação para o teste
        
        // Salva a alteração de status para APROVADO
        savedOrcamento = orcamentoRepository.saveAndFlush(savedOrcamento)
        
        // 2. Tentar alterar o BDI diretamente no banco ignorando o domínio
        // (Isso simula uma query maliciosa, um bug no código que ignorou as Guard Clauses, etc)
        val orcamentoFromDb = orcamentoRepository.findById(savedOrcamento.id).get()
        
        // Usamos reflection para driblar a proteção do domínio só para forçar o UPDATE no Hibernate
        val bdiField = Orcamento::class.java.getDeclaredField("bdi")
        bdiField.isAccessible = true
        bdiField.set(orcamentoFromDb, BigDecimal("0.9000")) // Tentando roubar 90%
        
        // 3. Verifica se o Postgres bloqueia
        val exception = assertThrows<DataIntegrityViolationException> {
            orcamentoRepository.saveAndFlush(orcamentoFromDb)
        }
        
        assertTrue(exception.message?.contains("FRAUD PREVENTION") == true || exception.cause?.message?.contains("FRAUD PREVENTION") == true, 
            "A trigger deve levantar uma exceção de FRAUD PREVENTION. Exception: ${exception.message} - Cause: ${exception.cause?.message}")
    }
}
