package com.example.api.orcamentos.application

import com.example.api.catalogos.domain.CatalogoBase
import com.example.api.catalogos.domain.CatalogoInsumo
import com.example.api.catalogos.domain.CatalogoItem
import com.example.api.catalogos.infrastructure.CatalogoItemRepository
import com.example.api.orcamentos.domain.EapItem
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.domain.OrcamentoModulo
import com.example.api.orcamentos.domain.TipoModulo
import com.example.api.orcamentos.infrastructure.EapItemRepository
import com.example.api.orcamentos.infrastructure.OrcamentoModuloRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

class CopiarItemCatalogoUseCaseTest {

    private lateinit var catalogoItemRepository: CatalogoItemRepository
    private lateinit var moduloRepository: OrcamentoModuloRepository
    private lateinit var eapItemRepository: EapItemRepository
    private lateinit var useCase: CopiarItemCatalogoUseCase

    @BeforeEach
    fun setup() {
        catalogoItemRepository = mockk()
        moduloRepository = mockk()
        eapItemRepository = mockk()
        useCase = CopiarItemCatalogoUseCase(catalogoItemRepository, moduloRepository, eapItemRepository)
    }

    @Test
    fun `deve copiar item do catalogo para modulo com insumos`() {
        val catalogoId = UUID.randomUUID()
        val base = CatalogoBase(nome = "SINAPI", mesAno = "2026-01", estado = "SP")
        val catalogoItem = CatalogoItem(
            id = catalogoId,
            catalogo = base,
            codigo = "87316",
            descricao = "ALVENARIA",
            unidade = "m2",
            valorMo = BigDecimal("25.50"),
            valorMat = BigDecimal("18.20"),
            valorSrv = BigDecimal.ZERO
        )
        
        val insumoMo = CatalogoInsumo(
            item = catalogoItem,
            tipoInsumo = "MAO_DE_OBRA",
            descricao = "PEDREIRO",
            unidade = "H",
            coeficiente = BigDecimal("1.5"),
            custoUnitario = BigDecimal("17.00")
        )
        catalogoItem.insumos.add(insumoMo)

        val moduloId = UUID.randomUUID()
        val orcamento = Orcamento(tenantId = UUID.randomUUID(), ownerId = UUID.randomUUID(), titulo = "Teste")
        val modulo = OrcamentoModulo(id = moduloId, tipoModulo = TipoModulo.FUNDACAO, nome = "Fundação")
        modulo.orcamento = orcamento

        every { catalogoItemRepository.findByIdWithInsumos(catalogoId) } returns catalogoItem
        every { moduloRepository.findById(moduloId) } returns Optional.of(modulo)
        every { eapItemRepository.save(any()) } answers { firstArg() }

        val quantidadeDesejada = BigDecimal("100.0")
        val eapItem = useCase.executar(catalogoItemId = catalogoId, quantidade = quantidadeDesejada, moduloId = moduloId)

        assertEquals("87316", eapItem.codigoItem)
        assertEquals("ALVENARIA", eapItem.descricao)
        assertEquals(BigDecimal("100.0"), eapItem.quantidade)
        assertEquals(BigDecimal("25.50"), eapItem.valorMo)
        
        assertEquals(1, eapItem.composicoes.size)
        val cpu = eapItem.composicoes[0]
        assertEquals("MAO_DE_OBRA", cpu.tipoInsumo.name)
        assertEquals("PEDREIRO", cpu.descricao)
        assertEquals(BigDecimal("1.5"), cpu.coeficiente)
    }
}
