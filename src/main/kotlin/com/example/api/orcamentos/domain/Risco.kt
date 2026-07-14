package com.example.api.orcamentos.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Risco de um orçamento (planilhas de referência 008/009: matriz de risco + SMC).
 * Impacto em 3 pontos (mín / mais provável / máx) para amostragem triangular na
 * Simulação de Monte Carlo; se só o provável for informado, o impacto é fixo.
 */
@Entity
@Table(name = "riscos")
class Risco(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "orcamento_id", nullable = false)
    val orcamentoId: UUID,

    @Column(nullable = false)
    var descricao: String,

    @Column
    var categoria: String? = null,

    /** Probabilidade de ocorrência (0..1). */
    @Column(nullable = false, precision = 5, scale = 4)
    var probabilidade: BigDecimal,

    @Column(name = "impacto_min", precision = 15, scale = 4)
    var impactoMin: BigDecimal? = null,

    @Column(name = "impacto_provavel", nullable = false, precision = 15, scale = 4)
    var impactoProvavel: BigDecimal,

    @Column(name = "impacto_max", precision = 15, scale = 4)
    var impactoMax: BigDecimal? = null,

    /** Plano de resposta/mitigação. */
    @Column
    var resposta: String? = null
) {
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
        protected set

    init {
        validar()
    }

    fun atualizar(
        novaDescricao: String,
        novaCategoria: String?,
        novaProbabilidade: BigDecimal,
        novoImpactoMin: BigDecimal?,
        novoImpactoProvavel: BigDecimal,
        novoImpactoMax: BigDecimal?,
        novaResposta: String?
    ) {
        this.descricao = novaDescricao
        this.categoria = novaCategoria
        this.probabilidade = novaProbabilidade
        this.impactoMin = novoImpactoMin
        this.impactoProvavel = novoImpactoProvavel
        this.impactoMax = novoImpactoMax
        this.resposta = novaResposta
        validar()
    }

    private fun validar() {
        if (probabilidade < BigDecimal.ZERO || probabilidade > BigDecimal.ONE) {
            throw DomainSecurityException("Probabilidade deve estar entre 0 e 1.")
        }
        if (impactoProvavel < BigDecimal.ZERO) {
            throw DomainSecurityException("Impacto provável não pode ser negativo.")
        }
        val min = impactoMin
        val max = impactoMax
        if ((min == null) != (max == null)) {
            throw DomainSecurityException("Impacto mínimo e máximo devem ser informados juntos (estimativa de 3 pontos).")
        }
        if (min != null && max != null && !(min <= impactoProvavel && impactoProvavel <= max)) {
            throw DomainSecurityException("Estimativa de 3 pontos inválida: exige mín ≤ provável ≤ máx.")
        }
        if (min != null && min < BigDecimal.ZERO) {
            throw DomainSecurityException("Impacto mínimo não pode ser negativo.")
        }
    }

    /** Valor esperado (matriz quantitativa): probabilidade × impacto provável. */
    val valorEsperado: BigDecimal
        get() = probabilidade.multiply(impactoProvavel)
}
