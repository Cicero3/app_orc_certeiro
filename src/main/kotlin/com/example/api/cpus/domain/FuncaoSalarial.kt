package com.example.api.cpus.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Salário-hora por função (Pedreiro, Servente, Encanador...) — tabela da aba "3. CPU". */
@Entity
@Table(name = "funcoes_salariais")
class FuncaoSalarial(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "owner_id", nullable = false)
    val ownerId: UUID,

    @Column(nullable = false)
    var nome: String,

    @Column(name = "valor_hora", nullable = false, precision = 15, scale = 4)
    var valorHora: BigDecimal = BigDecimal.ZERO
) {
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
        protected set

    fun atualizar(novoNome: String, novoValorHora: BigDecimal) {
        require(novoValorHora >= BigDecimal.ZERO) { "Valor-hora não pode ser negativo." }
        this.nome = novoNome
        this.valorHora = novoValorHora
    }
}
