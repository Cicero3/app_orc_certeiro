package com.example.api.orcamentos.domain

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Análise de risco e contingência (planilhas de referência 008/009), em três camadas:
 *
 * 1. Matriz qualitativa P×I — probabilidade (1..5) × impacto (1,2,4,8,16), como a
 *    "Matriz de Risco Qualitativa" da planilha 008.
 * 2. Valor esperado — Σ probabilidade × impacto provável (matriz quantitativa).
 * 3. Simulação de Monte Carlo — em cada iteração, cada risco "dispara" se U < p
 *    (Bernoulli, como a aba contingência-SMC-hipóteses); quando disparado, o impacto
 *    é amostrado da distribuição triangular (mín/provável/máx, como a SMC-serviço)
 *    ou é o valor fixo. Saída: média, desvio, percentis (P10..P95), IC 95% e histograma.
 *
 * A planilha rodava 314 cenários com RAND() voláteis e irreprodutíveis; aqui são
 * N iterações (default 10.000) com seed opcional para reprodutibilidade.
 */
object AnaliseRiscoCalculadora {

    // ---------- Classificação qualitativa ----------

    enum class NivelSeveridade { BAIXO, MODERADO, ALTO, CRITICO }

    data class ClassificacaoRisco(
        val probabilidadeScore: Int,      // 1..5
        val probabilidadeLabel: String,   // Muito Baixa..Quase Certa
        val impactoScore: Int,            // 1,2,4,8,16
        val impactoLabel: String,         // Muito Baixo..Muito Alto
        val severidade: Int,              // P × I (1..80)
        val nivel: NivelSeveridade
    )

    /** Faixas de probabilidade da planilha 008 (Muito Baixa=1 .. Quase Certa=5). */
    fun scoreProbabilidade(p: BigDecimal): Pair<Int, String> {
        val v = p.toDouble()
        return when {
            v < 0.10 -> 1 to "Muito Baixa"
            v < 0.30 -> 2 to "Baixa"
            v < 0.50 -> 3 to "Média"
            v < 0.70 -> 4 to "Alta"
            else -> 5 to "Quase Certa"
        }
    }

    /** Impacto relativo ao custo de referência do orçamento (escala 1,2,4,8,16 da planilha). */
    fun scoreImpacto(impacto: BigDecimal, custoReferencia: BigDecimal): Pair<Int, String> {
        if (custoReferencia.signum() <= 0) return 16 to "Muito Alto"
        val razao = impacto.divide(custoReferencia, 6, RoundingMode.HALF_UP).toDouble()
        return when {
            razao < 0.01 -> 1 to "Muito Baixo"
            razao < 0.05 -> 2 to "Baixo"
            razao < 0.10 -> 4 to "Médio"
            razao < 0.20 -> 8 to "Alto"
            else -> 16 to "Muito Alto"
        }
    }

    fun classificar(probabilidade: BigDecimal, impactoProvavel: BigDecimal, custoReferencia: BigDecimal): ClassificacaoRisco {
        val (pScore, pLabel) = scoreProbabilidade(probabilidade)
        val (iScore, iLabel) = scoreImpacto(impactoProvavel, custoReferencia)
        val severidade = pScore * iScore
        val nivel = when {
            severidade <= 4 -> NivelSeveridade.BAIXO
            severidade <= 12 -> NivelSeveridade.MODERADO
            severidade <= 32 -> NivelSeveridade.ALTO
            else -> NivelSeveridade.CRITICO
        }
        return ClassificacaoRisco(pScore, pLabel, iScore, iLabel, severidade, nivel)
    }

    // ---------- Simulação de Monte Carlo ----------

    data class RiscoSimulavel(
        val probabilidade: Double,
        val impactoMin: Double?,
        val impactoProvavel: Double,
        val impactoMax: Double?
    )

    data class FaixaHistograma(val inicio: BigDecimal, val fim: BigDecimal, val contagem: Int)

    data class ResultadoSimulacao(
        val iteracoes: Int,
        val seed: Long,
        val media: BigDecimal,
        val desvioPadrao: BigDecimal,
        val minimo: BigDecimal,
        val maximo: BigDecimal,
        val p10: BigDecimal,
        val p50: BigDecimal,
        val p80: BigDecimal,
        val p90: BigDecimal,
        val p95: BigDecimal,
        /** IC 95% pela aproximação normal (média ± 1,96σ), como a planilha 009. */
        val icInferior95: BigDecimal,
        val icSuperior95: BigDecimal,
        val histograma: List<FaixaHistograma>
    )

    fun simular(
        riscos: List<RiscoSimulavel>,
        iteracoes: Int = 10_000,
        seed: Long? = null,
        bins: Int = 20
    ): ResultadoSimulacao {
        require(iteracoes in 100..1_000_000) { "Iterações devem estar entre 100 e 1.000.000." }
        val seedEfetiva = seed ?: System.nanoTime()
        val rnd = Random(seedEfetiva)

        val amostras = DoubleArray(iteracoes)
        for (i in 0 until iteracoes) {
            var total = 0.0
            for (r in riscos) {
                if (rnd.nextDouble() < r.probabilidade) {
                    total += amostrarImpacto(r, rnd)
                }
            }
            amostras[i] = total
        }
        amostras.sort()

        val media = amostras.average()
        val variancia = amostras.sumOf { (it - media) * (it - media) } / (iteracoes - 1)
        val desvio = sqrt(variancia)

        val min = amostras.first()
        val max = amostras.last()
        val histograma = montarHistograma(amostras, min, max, bins)

        fun bd(v: Double) = BigDecimal(v).setScale(2, RoundingMode.HALF_UP)

        return ResultadoSimulacao(
            iteracoes = iteracoes,
            seed = seedEfetiva,
            media = bd(media),
            desvioPadrao = bd(desvio),
            minimo = bd(min),
            maximo = bd(max),
            p10 = bd(percentil(amostras, 0.10)),
            p50 = bd(percentil(amostras, 0.50)),
            p80 = bd(percentil(amostras, 0.80)),
            p90 = bd(percentil(amostras, 0.90)),
            p95 = bd(percentil(amostras, 0.95)),
            icInferior95 = bd((media - 1.96 * desvio).coerceAtLeast(0.0)),
            icSuperior95 = bd(media + 1.96 * desvio),
            histograma = histograma
        )
    }

    /** Triangular quando há 3 pontos; valor fixo caso contrário. */
    private fun amostrarImpacto(r: RiscoSimulavel, rnd: Random): Double {
        val min = r.impactoMin
        val max = r.impactoMax
        if (min == null || max == null || max <= min) return r.impactoProvavel
        val moda = r.impactoProvavel.coerceIn(min, max)
        val u = rnd.nextDouble()
        val fc = (moda - min) / (max - min)
        return if (u < fc) min + sqrt(u * (max - min) * (moda - min))
        else max - sqrt((1 - u) * (max - min) * (max - moda))
    }

    /** Percentil por interpolação linear sobre a amostra ordenada. */
    private fun percentil(ordenado: DoubleArray, p: Double): Double {
        if (ordenado.isEmpty()) return 0.0
        val pos = p * (ordenado.size - 1)
        val lo = pos.toInt()
        val hi = (lo + 1).coerceAtMost(ordenado.size - 1)
        val frac = pos - lo
        return ordenado[lo] * (1 - frac) + ordenado[hi] * frac
    }

    private fun montarHistograma(ordenado: DoubleArray, min: Double, max: Double, bins: Int): List<FaixaHistograma> {
        if (max <= min) {
            return listOf(
                FaixaHistograma(
                    BigDecimal(min).setScale(2, RoundingMode.HALF_UP),
                    BigDecimal(max).setScale(2, RoundingMode.HALF_UP),
                    ordenado.size
                )
            )
        }
        val largura = (max - min) / bins
        val contagens = IntArray(bins)
        for (v in ordenado) {
            val idx = (((v - min) / largura).toInt()).coerceIn(0, bins - 1)
            contagens[idx]++
        }
        return (0 until bins).map { i ->
            FaixaHistograma(
                inicio = BigDecimal(min + i * largura).setScale(2, RoundingMode.HALF_UP),
                fim = BigDecimal(min + (i + 1) * largura).setScale(2, RoundingMode.HALF_UP),
                contagem = contagens[i]
            )
        }
    }
}
