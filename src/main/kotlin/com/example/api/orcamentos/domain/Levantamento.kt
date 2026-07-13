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
 * Levantamento de quantitativo (aba "2. LEV QNT" + calculadoras do app).
 * Guarda os inputs da calculadora (payload JSON) como memória de cálculo auditável
 * e pode alimentar a quantidade de um item da EAP.
 */
@Entity
@Table(name = "levantamentos")
class Levantamento(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "orcamento_id", nullable = false)
    val orcamentoId: UUID,

    @Column(name = "eap_item_id")
    var eapItemId: UUID? = null,

    @Column(nullable = false)
    val tipo: String,

    @Column(nullable = false)
    var descricao: String,

    @Column(nullable = false)
    var unidade: String,

    @Column(nullable = false, precision = 12, scale = 4)
    var resultado: BigDecimal = BigDecimal.ZERO,

    @Column(columnDefinition = "text")
    var payload: String? = null
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
        if (resultado < BigDecimal.ZERO) {
            throw DomainSecurityException("Resultado do levantamento não pode ser negativo.")
        }
    }
}
