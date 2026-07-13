package com.example.api.orcamentos.application

import com.example.api.orcamentos.api.dto.EapItemCreateDto
import com.example.api.orcamentos.api.dto.EapItemUpdateDto
import com.example.api.orcamentos.api.dto.ModuloCreateDto
import com.example.api.orcamentos.domain.DomainSecurityException
import com.example.api.orcamentos.domain.EapItem
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.domain.OrcamentoModulo
import com.example.api.orcamentos.domain.TipoModulo
import com.example.api.cpus.infrastructure.CpuPropriaRepository
import com.example.api.orcamentos.infrastructure.EapItemRepository
import com.example.api.orcamentos.infrastructure.OrcamentoModuloRepository
import com.example.api.orcamentos.infrastructure.OrcamentoRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class GerenciarEapUseCaseTest {

    private lateinit var orcamentoRepository: OrcamentoRepository
    private lateinit var moduloRepository: OrcamentoModuloRepository
    private lateinit var eapItemRepository: EapItemRepository
    private lateinit var copiarItemCatalogoUseCase: CopiarItemCatalogoUseCase
    private lateinit var cpuPropriaRepository: CpuPropriaRepository
    private lateinit var useCase: GerenciarEapUseCase

    private val donoId = UUID.randomUUID()
    private val intrusoId = UUID.randomUUID()
    private lateinit var orcamento: Orcamento

    @BeforeEach
    fun setup() {
        orcamentoRepository = mockk()
        moduloRepository = mockk()
        eapItemRepository = mockk()
        copiarItemCatalogoUseCase = mockk()
        cpuPropriaRepository = mockk()
        useCase = GerenciarEapUseCase(orcamentoRepository, moduloRepository, eapItemRepository, copiarItemCatalogoUseCase, cpuPropriaRepository)

        orcamento = Orcamento(tenantId = donoId, ownerId = donoId, titulo = "Reforma")
        every { orcamentoRepository.findById(orcamento.id) } returns Optional.of(orcamento)
        every { orcamentoRepository.save(any()) } answers { firstArg() }
        every { eapItemRepository.save(any()) } answers { firstArg() }
    }

    @Test
    fun `deve adicionar modulo ao orcamento do dono`() {
        val modulo = useCase.adicionarModulo(donoId, orcamento.id, ModuloCreateDto(nome = "Demolição", tipoModulo = "DEMOLICAO_CONSTRUCAO"))

        assertEquals(TipoModulo.DEMOLICAO_CONSTRUCAO, modulo.tipoModulo)
        assertEquals(1, orcamento.modulos.size)
    }

    @Test
    fun `deve usar tipo OUTROS quando tipo nao informado`() {
        val modulo = useCase.adicionarModulo(donoId, orcamento.id, ModuloCreateDto(nome = "Diversos"))
        assertEquals(TipoModulo.OUTROS, modulo.tipoModulo)
    }

    @Test
    fun `deve bloquear quem nao e dono do orcamento`() {
        assertThrows<DomainSecurityException> {
            useCase.adicionarModulo(intrusoId, orcamento.id, ModuloCreateDto(nome = "Invasão"))
        }
    }

    @Test
    fun `deve adicionar item de primeiro nivel em modulo`() {
        val modulo = OrcamentoModulo(tipoModulo = TipoModulo.PINTURA, nome = "Pintura")
        orcamento.adicionarModulo(modulo)
        every { moduloRepository.findById(modulo.id) } returns Optional.of(modulo)

        val item = useCase.adicionarItem(
            donoId, orcamento.id,
            EapItemCreateDto(
                codigoItem = "14.1", descricao = "Selador acrílico", unidade = "M2",
                quantidade = BigDecimal("10"), valorMo = BigDecimal("10"), valorMat = BigDecimal("3.30"),
                moduloId = modulo.id
            )
        )

        assertEquals("14.1", item.codigoItem)
        assertEquals(1, modulo.eapItens.size)
        // custo total = (10 + 3.30) × 10 = 133.00
        assertEquals(0, BigDecimal("133.00").compareTo(item.custoTotal))
    }

    @Test
    fun `deve adicionar subitem em item pai`() {
        val modulo = OrcamentoModulo(tipoModulo = TipoModulo.HIDRAULICA, nome = "Hidráulica")
        orcamento.adicionarModulo(modulo)
        val pai = EapItem(codigoItem = "4", descricao = "Instalações", quantidade = BigDecimal.ONE)
        modulo.adicionarEapItem(pai)
        every { eapItemRepository.findById(pai.id) } returns Optional.of(pai)

        val sub = useCase.adicionarItem(
            donoId, orcamento.id,
            EapItemCreateDto(
                codigoItem = "4.1", descricao = "Ponto de água fria", unidade = "UN",
                quantidade = BigDecimal("10"), valorMo = BigDecimal("50"),
                parentId = pai.id
            )
        )

        assertEquals(1, pai.subItens.size)
        assertEquals("4.1", sub.codigoItem)
    }

    @Test
    fun `deve rejeitar quando moduloId e parentId vem juntos ou ambos ausentes`() {
        assertThrows<IllegalArgumentException> {
            useCase.adicionarItem(
                donoId, orcamento.id,
                EapItemCreateDto(codigoItem = "1", descricao = "X", moduloId = UUID.randomUUID(), parentId = UUID.randomUUID())
            )
        }
        assertThrows<IllegalArgumentException> {
            useCase.adicionarItem(donoId, orcamento.id, EapItemCreateDto(codigoItem = "1", descricao = "X"))
        }
    }

    @Test
    fun `deve bloquear item de modulo de outro orcamento`() {
        val outroOrcamento = Orcamento(tenantId = donoId, ownerId = donoId, titulo = "Outro")
        val moduloAlheio = OrcamentoModulo(tipoModulo = TipoModulo.OUTROS, nome = "Alheio")
        outroOrcamento.adicionarModulo(moduloAlheio)
        every { moduloRepository.findById(moduloAlheio.id) } returns Optional.of(moduloAlheio)

        assertThrows<DomainSecurityException> {
            useCase.adicionarItem(
                donoId, orcamento.id,
                EapItemCreateDto(codigoItem = "1", descricao = "X", moduloId = moduloAlheio.id)
            )
        }
    }

    @Test
    fun `deve atualizar valores de item existente`() {
        val modulo = OrcamentoModulo(tipoModulo = TipoModulo.PINTURA, nome = "Pintura")
        orcamento.adicionarModulo(modulo)
        val item = EapItem(codigoItem = "14.1", descricao = "Tinta", quantidade = BigDecimal.ONE, valorMo = BigDecimal.TEN)
        modulo.adicionarEapItem(item)
        every { eapItemRepository.findById(item.id) } returns Optional.of(item)

        val atualizado = useCase.atualizarItem(
            donoId, orcamento.id, item.id,
            EapItemUpdateDto(quantidade = BigDecimal("5"), valorMo = BigDecimal("10"), valorMat = BigDecimal("13.50"), valorSrv = BigDecimal.ZERO)
        )

        // (10 + 13.50) × 5 = 117.50
        assertEquals(0, BigDecimal("117.50").compareTo(atualizado.custoTotal))
    }

    @Test
    fun `deve bloquear remocao de item quando orcamento esta imutavel`() {
        val modulo = OrcamentoModulo(tipoModulo = TipoModulo.PINTURA, nome = "Pintura")
        orcamento.adicionarModulo(modulo)
        val item = EapItem(codigoItem = "14.1", descricao = "Tinta", quantidade = BigDecimal.ONE, valorMo = BigDecimal.TEN)
        modulo.adicionarEapItem(item)
        every { eapItemRepository.findById(item.id) } returns Optional.of(item)

        orcamento.solicitarAprovacao(donoId)
        orcamento.aprovar(UUID.randomUUID(), true)

        assertThrows<DomainSecurityException> {
            useCase.removerItem(donoId, orcamento.id, item.id)
        }
    }
}
