package com.example.api.orcamentos.application

import com.example.api.orcamentos.api.dto.CronogramaAlocacaoUpsertDto
import com.example.api.orcamentos.api.dto.CronogramaSalvarDto
import com.example.api.orcamentos.domain.CronogramaAlocacao
import com.example.api.orcamentos.domain.EapItem
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.domain.OrcamentoModulo
import com.example.api.orcamentos.domain.TipoModulo
import com.example.api.orcamentos.infrastructure.CronogramaAlocacaoRepository
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

class CronogramaUseCaseTest {

    private lateinit var orcamentoRepository: OrcamentoRepository
    private lateinit var alocacaoRepository: CronogramaAlocacaoRepository
    private lateinit var useCase: CronogramaUseCase

    private val donoId = UUID.randomUUID()
    private lateinit var orcamento: Orcamento
    private lateinit var moduloA: OrcamentoModulo
    private lateinit var moduloB: OrcamentoModulo

    @BeforeEach
    fun setup() {
        orcamentoRepository = mockk()
        alocacaoRepository = mockk(relaxed = true)
        useCase = CronogramaUseCase(orcamentoRepository, alocacaoRepository)

        orcamento = Orcamento(tenantId = donoId, ownerId = donoId, titulo = "Obra") // BDI 0
        moduloA = OrcamentoModulo(tipoModulo = TipoModulo.DEMOLICAO_CONSTRUCAO, nome = "Demolição")
        moduloB = OrcamentoModulo(tipoModulo = TipoModulo.PINTURA, nome = "Pintura")
        orcamento.adicionarModulo(moduloA)
        orcamento.adicionarModulo(moduloB)
        // Módulo A vale 1000, módulo B vale 3000 → total 4000
        moduloA.adicionarEapItem(EapItem(codigoItem = "1.1", descricao = "A", quantidade = BigDecimal("1"), valorMo = BigDecimal("1000")))
        moduloB.adicionarEapItem(EapItem(codigoItem = "2.1", descricao = "B", quantidade = BigDecimal("1"), valorMo = BigDecimal("3000")))

        every { orcamentoRepository.findById(orcamento.id) } returns Optional.of(orcamento)
    }

    private fun alocacao(modulo: OrcamentoModulo, periodo: Int, previsto: String, real: String) =
        CronogramaAlocacao(
            orcamentoId = orcamento.id, moduloId = modulo.id, periodo = periodo,
            previstoPct = BigDecimal(previsto), realPct = BigDecimal(real)
        )

    @Test
    fun `deve calcular avanco fisico ponderado por valor e acumulados como a planilha 005`() {
        every { alocacaoRepository.findAllByOrcamentoId(orcamento.id) } returns listOf(
            alocacao(moduloA, 1, "0.50", "1.00"),
            alocacao(moduloA, 2, "0.50", "0"),
            alocacao(moduloB, 1, "0.25", "0.25"),
            alocacao(moduloB, 2, "0.75", "0")
        )

        val dto = useCase.obter(donoId, orcamento.id, periodos = 2)

        assertEquals(0, BigDecimal("4000").compareTo(dto.valorTotal))

        val p1 = dto.porPeriodo[0]
        // Financeiro previsto P1 = 0,5×1000 + 0,25×3000 = 1250 → físico 31,25%
        assertEquals(0, BigDecimal("1250").compareTo(p1.financeiroPrevisto))
        assertEquals(0, BigDecimal("0.3125").compareTo(p1.fisicoPrevistoPct))
        // Financeiro real P1 = 1,0×1000 + 0,25×3000 = 1750 → físico 43,75%
        assertEquals(0, BigDecimal("1750").compareTo(p1.financeiroReal))
        assertEquals(0, BigDecimal("0.4375").compareTo(p1.fisicoRealPct))

        val p2 = dto.porPeriodo[1]
        // Acumulado previsto fecha em 100% (curva S completa)
        assertEquals(0, BigDecimal("1.0000").compareTo(p2.fisicoPrevistoAcumuladoPct))
        assertEquals(0, BigDecimal("4000").compareTo(p2.financeiroPrevistoAcumulado))
        // Real não anda no P2 → acumulado permanece 43,75%
        assertEquals(0, BigDecimal("0.4375").compareTo(p2.fisicoRealAcumuladoPct))

        // Desvio no último período com medição real (P1): +12,5 pontos (adiantado)
        assertEquals(0, BigDecimal("0.1250").compareTo(dto.desvioFisicoPct))

        // Linha do módulo A: avanço real 100% × 1000
        val linhaA = dto.linhas.first { it.moduloId == moduloA.id }
        assertEquals(0, BigDecimal("1000").compareTo(linhaA.avancoRealValor))
    }

    @Test
    fun `deve aplicar BDI no valor financeiro dos modulos`() {
        orcamento.atualizarBdi(BigDecimal("0.25"))
        every { alocacaoRepository.findAllByOrcamentoId(orcamento.id) } returns emptyList()

        val dto = useCase.obter(donoId, orcamento.id, periodos = 1)

        // 4000 × 1,25 = 5000
        assertEquals(0, BigDecimal("5000").compareTo(dto.valorTotal))
    }

    @Test
    fun `deve rejeitar alocacao que ultrapasse 100 por cento no modulo`() {
        every { alocacaoRepository.findByOrcamentoIdAndModuloIdAndPeriodo(any(), any(), any()) } returns null
        every { alocacaoRepository.save(any()) } answers { firstArg() }
        every { alocacaoRepository.findAllByOrcamentoId(orcamento.id) } returns listOf(
            alocacao(moduloA, 1, "0.60", "0"),
            alocacao(moduloA, 2, "0.60", "0")
        )

        assertThrows<IllegalArgumentException> {
            useCase.salvar(
                donoId, orcamento.id,
                CronogramaSalvarDto(
                    alocacoes = listOf(
                        CronogramaAlocacaoUpsertDto(moduloId = moduloA.id, periodo = 2, previstoPct = BigDecimal("0.60"))
                    )
                ),
                periodos = null
            )
        }
    }
}
