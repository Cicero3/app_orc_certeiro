package com.example.api.orcamentos.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class CondicaoClima { BOM, NUBLADO, CHUVOSO, IMPRATICAVEL }

/**
 * Relatório Diário de Obra — RDO (planilha de referência 006): clima por turno,
 * efetivo de MO, equipamentos, atividades executadas e ocorrências do dia.
 * Um registro por orçamento+data.
 */
@Entity
@Table(name = "diarios_obra")
class DiarioObra(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "orcamento_id", nullable = false)
    val orcamentoId: UUID,

    @Column(nullable = false)
    val data: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "clima_manha")
    var climaManha: CondicaoClima? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "clima_tarde")
    var climaTarde: CondicaoClima? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "clima_noite")
    var climaNoite: CondicaoClima? = null,

    /** JSON: [{"funcao":"Pedreiro","qtde":3}, ...]. */
    @Column(name = "mao_de_obra", columnDefinition = "text")
    var maoDeObra: String? = null,

    /** JSON: [{"descricao":"Betoneira 400L","qtde":1}, ...]. */
    @Column(columnDefinition = "text")
    var equipamentos: String? = null,

    @Column(columnDefinition = "text")
    var atividades: String? = null,

    @Column(columnDefinition = "text")
    var ocorrencias: String? = null,

    @Column(columnDefinition = "text")
    var observacoes: String? = null
) {
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
        protected set

    fun atualizar(
        novoClimaManha: CondicaoClima?,
        novoClimaTarde: CondicaoClima?,
        novoClimaNoite: CondicaoClima?,
        novaMaoDeObra: String?,
        novosEquipamentos: String?,
        novasAtividades: String?,
        novasOcorrencias: String?,
        novasObservacoes: String?
    ) {
        this.climaManha = novoClimaManha
        this.climaTarde = novoClimaTarde
        this.climaNoite = novoClimaNoite
        this.maoDeObra = novaMaoDeObra
        this.equipamentos = novosEquipamentos
        this.atividades = novasAtividades
        this.ocorrencias = novasOcorrencias
        this.observacoes = novasObservacoes
    }
}
