package com.example.api.cpus.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class TipoContratacao { HORISTA, MENSALISTA }

/**
 * Salário-hora por função (Pedreiro, Servente, Encanador...) — tabela da aba "3. CPU",
 * com encargos sociais por tipo de contratação (planilha 004: ex. encargos desonerados
 * horista 88,28% / mensalista 49,82%).
 */
@Entity
@Table(name = "funcoes_salariais")
class FuncaoSalarial(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "owner_id", nullable = false)
    val ownerId: UUID,

    @Column(nullable = false)
    var nome: String,

    /** Salário-hora BASE (sem encargos). */
    @Column(name = "valor_hora", nullable = false, precision = 15, scale = 4)
    var valorHora: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contratacao", nullable = false)
    var tipoContratacao: TipoContratacao = TipoContratacao.HORISTA,

    /** Encargos sociais como fração (0.8828 = 88,28%). */
    @Column(name = "encargos_pct", nullable = false, precision = 6, scale = 4)
    var encargosPct: BigDecimal = BigDecimal.ZERO
) {
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
        protected set

    /** Custo-hora efetivo para as CPUs: salário base × (1 + encargos). */
    val valorHoraComEncargos: BigDecimal
        get() = valorHora.multiply(BigDecimal.ONE.add(encargosPct))

    fun atualizar(novoNome: String, novoValorHora: BigDecimal, novoTipo: TipoContratacao, novosEncargos: BigDecimal) {
        require(novoValorHora >= BigDecimal.ZERO) { "Valor-hora não pode ser negativo." }
        require(novosEncargos >= BigDecimal.ZERO) { "Encargos não podem ser negativos." }
        this.nome = novoNome
        this.valorHora = novoValorHora
        this.tipoContratacao = novoTipo
        this.encargosPct = novosEncargos
    }

    fun aplicarEncargos(pct: BigDecimal) {
        require(pct >= BigDecimal.ZERO) { "Encargos não podem ser negativos." }
        this.encargosPct = pct
    }
}
