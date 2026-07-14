package com.example.api.orcamentos.domain

import com.example.api.orcamentos.domain.AnaliseRiscoCalculadora.NivelSeveridade
import com.example.api.orcamentos.domain.AnaliseRiscoCalculadora.RiscoSimulavel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class AnaliseRiscoCalculadoraTest {

    // ---------- Matriz qualitativa P×I ----------

    @Test
    fun `deve classificar probabilidade nas faixas da matriz da planilha 008`() {
        assertEquals(1 to "Muito Baixa", AnaliseRiscoCalculadora.scoreProbabilidade(BigDecimal("0.09")))
        assertEquals(2 to "Baixa", AnaliseRiscoCalculadora.scoreProbabilidade(BigDecimal("0.16")))
        assertEquals(3 to "Média", AnaliseRiscoCalculadora.scoreProbabilidade(BigDecimal("0.40")))
        assertEquals(4 to "Alta", AnaliseRiscoCalculadora.scoreProbabilidade(BigDecimal("0.60")))
        assertEquals(5 to "Quase Certa", AnaliseRiscoCalculadora.scoreProbabilidade(BigDecimal("0.70")))
    }

    @Test
    fun `deve classificar impacto relativo ao custo de referencia na escala 1-2-4-8-16`() {
        val custo = BigDecimal("100000")
        assertEquals(1 to "Muito Baixo", AnaliseRiscoCalculadora.scoreImpacto(BigDecimal("500"), custo))    // 0,5%
        assertEquals(2 to "Baixo", AnaliseRiscoCalculadora.scoreImpacto(BigDecimal("3000"), custo))         // 3%
        assertEquals(4 to "Médio", AnaliseRiscoCalculadora.scoreImpacto(BigDecimal("8000"), custo))         // 8%
        assertEquals(8 to "Alto", AnaliseRiscoCalculadora.scoreImpacto(BigDecimal("15000"), custo))         // 15%
        assertEquals(16 to "Muito Alto", AnaliseRiscoCalculadora.scoreImpacto(BigDecimal("30000"), custo))  // 30%
    }

    @Test
    fun `severidade e nivel devem combinar probabilidade e impacto`() {
        // p=0,19 (Baixa=2) × impacto 16% do custo (Alto=8) → severidade 16 → ALTO
        val c = AnaliseRiscoCalculadora.classificar(BigDecimal("0.19"), BigDecimal("16000"), BigDecimal("100000"))
        assertEquals(16, c.severidade)
        assertEquals(NivelSeveridade.ALTO, c.nivel)

        // p=0,05 (Muito Baixa=1) × impacto 0,5% (Muito Baixo=1) → 1 → BAIXO
        val baixo = AnaliseRiscoCalculadora.classificar(BigDecimal("0.05"), BigDecimal("500"), BigDecimal("100000"))
        assertEquals(NivelSeveridade.BAIXO, baixo.nivel)

        // p=0,80 (Quase Certa=5) × impacto 30% (Muito Alto=16) → 80 → CRÍTICO
        val critico = AnaliseRiscoCalculadora.classificar(BigDecimal("0.80"), BigDecimal("30000"), BigDecimal("100000"))
        assertEquals(80, critico.severidade)
        assertEquals(NivelSeveridade.CRITICO, critico.nivel)
    }

    // ---------- Simulação de Monte Carlo ----------

    @Test
    fun `risco certo com impacto fixo deve produzir distribuicao degenerada`() {
        val resultado = AnaliseRiscoCalculadora.simular(
            listOf(RiscoSimulavel(probabilidade = 1.0, impactoMin = null, impactoProvavel = 100.0, impactoMax = null)),
            iteracoes = 1_000, seed = 42
        )
        assertEquals(0, BigDecimal("100.00").compareTo(resultado.media))
        assertEquals(0, BigDecimal("0.00").compareTo(resultado.desvioPadrao))
        assertEquals(0, BigDecimal("100.00").compareTo(resultado.p50))
        assertEquals(0, BigDecimal("100.00").compareTo(resultado.p95))
    }

    @Test
    fun `bernoulli deve convergir para o valor esperado probabilidade vezes impacto`() {
        val resultado = AnaliseRiscoCalculadora.simular(
            listOf(RiscoSimulavel(probabilidade = 0.5, impactoMin = null, impactoProvavel = 100.0, impactoMax = null)),
            iteracoes = 20_000, seed = 42
        )
        // E[X] = 0,5 × 100 = 50 (tolerância estatística de ±3)
        assertTrue(resultado.media.toDouble() in 47.0..53.0, "média foi ${resultado.media}")
        // Só há dois resultados possíveis: 0 ou 100
        assertEquals(0, BigDecimal("0.00").compareTo(resultado.minimo))
        assertEquals(0, BigDecimal("100.00").compareTo(resultado.maximo))
    }

    @Test
    fun `distribuicao triangular deve respeitar limites e media teorica`() {
        val resultado = AnaliseRiscoCalculadora.simular(
            listOf(RiscoSimulavel(probabilidade = 1.0, impactoMin = 80.0, impactoProvavel = 100.0, impactoMax = 140.0)),
            iteracoes = 20_000, seed = 7
        )
        // Média teórica da triangular = (min + moda + max) / 3 = 106,67 (tolerância ±2)
        assertTrue(resultado.media.toDouble() in 104.5..108.5, "média foi ${resultado.media}")
        assertTrue(resultado.minimo.toDouble() >= 80.0)
        assertTrue(resultado.maximo.toDouble() <= 140.0)
        // Percentis em ordem
        assertTrue(resultado.p10 <= resultado.p50 && resultado.p50 <= resultado.p80 && resultado.p80 <= resultado.p95)
    }

    @Test
    fun `mesma seed deve produzir resultados identicos - reprodutibilidade`() {
        val riscos = listOf(
            RiscoSimulavel(0.16, null, 72137.09, null),
            RiscoSimulavel(0.32, null, 159233.80, null),
            RiscoSimulavel(0.19, 1000000.0, 1628793.92, 2200000.0)
        )
        val a = AnaliseRiscoCalculadora.simular(riscos, iteracoes = 5_000, seed = 123)
        val b = AnaliseRiscoCalculadora.simular(riscos, iteracoes = 5_000, seed = 123)
        assertEquals(a.media, b.media)
        assertEquals(a.p80, b.p80)
        assertEquals(a.histograma, b.histograma)
    }

    @Test
    fun `histograma deve cobrir todas as amostras`() {
        val resultado = AnaliseRiscoCalculadora.simular(
            listOf(RiscoSimulavel(0.5, 50.0, 100.0, 200.0)),
            iteracoes = 2_000, seed = 99
        )
        assertEquals(2_000, resultado.histograma.sumOf { it.contagem })
    }

    @Test
    fun `deve rejeitar numero de iteracoes fora dos limites`() {
        assertThrows<IllegalArgumentException> {
            AnaliseRiscoCalculadora.simular(emptyList(), iteracoes = 10, seed = 1)
        }
    }
}
