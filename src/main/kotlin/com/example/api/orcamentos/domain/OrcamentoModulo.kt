package com.example.api.orcamentos.domain

import java.util.UUID
import jakarta.persistence.*

enum class TipoModulo {
    DEMOLICAO_CONSTRUCAO,
    FUNDACAO,
    ELETRICA_DISTRIBUICAO,
    ELETRICA_PRUMADAS,
    ESGOTO_PLUVIAL,
    HIDRAULICA,
    ILUMINACAO,
    EQUIPAMENTOS,
    FORRO,
    REVESTIMENTO,
    MARMORARIA,
    PINTURA,
    MARCENARIA,
    AR_CONDICIONADO,
    EXAUSTAO,
    RENOVACAO_AR,
    MEMORIAL_DESCRITIVO,
    OUTROS
}

@Entity
@Table(name = "orcamentos_modulos")
class OrcamentoModulo(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_modulo", nullable = false)
    val tipoModulo: TipoModulo,

    @Column(nullable = false)
    val nome: String
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orcamento_id", nullable = false)
    var orcamento: Orcamento? = null
        internal set

    @OneToMany(mappedBy = "modulo", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _eapItens: MutableList<EapItem> = mutableListOf()

    val eapItens: List<EapItem>
        get() = _eapItens.toList()

    fun adicionarEapItem(item: EapItem) {
        if (orcamento?.status?.isImutavel == true) {
            throw DomainSecurityException("Orçamento imutável. Adição de itens bloqueada.")
        }
        _eapItens.add(item)
        item.modulo = this
        item.orcamento = this.orcamento
    }
}
