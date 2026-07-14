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
 * Alocação do cronograma físico-financeiro (planilha 005): fração do módulo da EAP
 * prevista e realizada em cada período (semana/mês 1..N).
 */
@Entity
@Table(name = "cronograma_alocacoes")
class CronogramaAlocacao(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "orcamento_id", nullable = false)
    val orcamentoId: UUID,

    @Column(name = "modulo_id", nullable = false)
    val moduloId: UUID,

    @Column(nullable = false)
    val periodo: Int,

    @Column(name = "previsto_pct", nullable = false, precision = 5, scale = 4)
    var previstoPct: BigDecimal = BigDecimal.ZERO,

    @Column(name = "real_pct", nullable = false, precision = 5, scale = 4)
    var realPct: BigDecimal = BigDecimal.ZERO
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

    fun atualizar(novoPrevisto: BigDecimal, novoReal: BigDecimal) {
        this.previstoPct = novoPrevisto
        this.realPct = novoReal
        validar()
    }

    private fun validar() {
        if (periodo < 1 || periodo > 120) {
            throw DomainSecurityException("Período deve estar entre 1 e 120.")
        }
        if (previstoPct < BigDecimal.ZERO || previstoPct > BigDecimal.ONE ||
            realPct < BigDecimal.ZERO || realPct > BigDecimal.ONE
        ) {
            throw DomainSecurityException("Percentuais do cronograma devem estar entre 0 e 1.")
        }
    }
}
