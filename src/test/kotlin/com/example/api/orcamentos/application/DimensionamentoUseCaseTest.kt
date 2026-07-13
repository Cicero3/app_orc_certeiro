package com.example.api.orcamentos.application

import com.example.api.orcamentos.domain.ComposicaoPreco
import com.example.api.orcamentos.domain.DomainSecurityException
import com.example.api.orcamentos.domain.EapItem
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.domain.OrcamentoModulo
import com.example.api.orcamentos.domain.TipoInsumo
import com.example.api.orcamentos.domain.TipoModulo
import com.example.api.orcamentos.infrastructure.OrcamentoRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class DimensionamentoUseCaseTest {

    private lateinit var orcamentoRepository: OrcamentoRepository
    private lateinit var useCase: DimensionamentoUseCase

    private val donoId = UUID.randomUUID()
    private lateinit var orcamento: Orcamento

    @BeforeEach
    fun setup() {
        orcamentoRepository = mockk()
        useCase = DimensionamentoUseCase(orcamentoRepository)
        orcamento = Orcamento(tenantId = donoId, ownerId = donoId, titulo = "Reforma")
        every { orcamentoRepository.findById(orcamento.id) } returns Optional.of(orcamento)
    }

    /**
     * Espelha a linha "Demolição de alvenaria" da aba DIM EQP:
     * 100 m² × índice 0,5825 h/m² = 58,25 h ÷ jornada 8 h/dia = 7,28 → ROUNDUP = 8 dias.
     */
    @Test
    fun `deve calcular dias da equipe basica com roundup como a planilha`() {
        val modulo = OrcamentoModulo(tipoModulo = TipoModulo.DEMOLICAO_CONSTRUCAO, nome = "Demolição")
        orcamento.adicionarModulo(modulo)
        val item = EapItem(codigoItem = "2.1", descricao = "Demolição de alvenaria", unidade = "M2", quantidade = BigDecimal("100"))
        modulo.adicionarEapItem(item)
        item.adicionarComposicao(ComposicaoPreco(tipoInsumo = TipoInsumo.MAO_DE_OBRA, descricao = "Pedreiro", unidade = "H", coeficiente = BigDecimal("0.2553"), custoUnitarioInsumo = BigDecimal("25.38")))
        item.adicionarComposicao(ComposicaoPreco(tipoInsumo = TipoInsumo.MAO_DE_OBRA, descricao = "Servente", unidade = "H", coeficiente = BigDecimal("0.3272"), custoUnitarioInsumo = BigDecimal("20.42")))
        // Material não entra no índice de MO
        item.adicionarComposicao(ComposicaoPreco(tipoInsumo = TipoInsumo.MATERIAL, descricao = "Sacos de entulho", unidade = "UN", coeficiente = BigDecimal("1"), custoUnitarioInsumo = BigDecimal("2.50")))

        val resultado = useCase.calcular(donoId, orcamento.id, BigDecimal("8"))
        val linha = resultado.itens.single()

        // Índice = 0.2553 + 0.3272 = 0.5825 h/m²
        assertEquals(0, BigDecimal("0.5825").compareTo(linha.indiceHorasPorUnidade))
        // Horas totais = 100 × 0.5825 = 58.25 h
        assertEquals(0, BigDecimal("58.25").compareTo(linha.horasTotais))
        // Dias = ceil(58.25 / 8) = ceil(7.28) = 8
        assertEquals(8, linha.diasEquipeBasica)
        // Histograma por função
        assertEquals(0, BigDecimal("25.53").compareTo(resultado.horasPorFuncao["Pedreiro"]))
        assertEquals(0, BigDecimal("32.72").compareTo(resultado.horasPorFuncao["Servente"]))
    }

    @Test
    fun `item sem CPU de MO aparece sem estimativa e sem divisao por zero`() {
        val modulo = OrcamentoModulo(tipoModulo = TipoModulo.MARMORARIA, nome = "Marmoraria")
        orcamento.adicionarModulo(modulo)
        modulo.adicionarEapItem(EapItem(codigoItem = "10.1", descricao = "Bancada em granito", quantidade = BigDecimal("1"), valorSrv = BigDecimal("1500")))

        val resultado = useCase.calcular(donoId, orcamento.id, BigDecimal("8"))
        val linha = resultado.itens.single()

        assertFalse(linha.temCpuDeMo)
        assertEquals(0, linha.diasEquipeBasica)
        assertEquals(0, BigDecimal.ZERO.compareTo(linha.horasTotais))
    }

    @Test
    fun `deve rejeitar jornada invalida`() {
        assertThrows<IllegalArgumentException> {
            useCase.calcular(donoId, orcamento.id, BigDecimal.ZERO)
        }
    }

    @Test
    fun `deve bloquear quem nao e dono`() {
        assertThrows<DomainSecurityException> {
            useCase.calcular(UUID.randomUUID(), orcamento.id, BigDecimal("8"))
        }
    }
}
