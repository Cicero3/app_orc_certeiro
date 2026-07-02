package com.example.api.orcamentos.domain

import java.math.BigDecimal
import java.util.UUID
import jakarta.persistence.*

@Entity
@Table(name = "eap_itens")
class EapItem(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "codigo_item", nullable = false)
    val codigoItem: String,

    @Column(nullable = false)
    val descricao: String,

    @Column
    val marca: String? = null,

    @Column
    val unidade: String? = null,

    @Column(nullable = false, precision = 12, scale = 4)
    var quantidade: BigDecimal = BigDecimal.ZERO,

    @Column(name = "valor_mo", nullable = false, precision = 15, scale = 4)
    var valorMo: BigDecimal = BigDecimal.ZERO,

    @Column(name = "valor_mat", nullable = false, precision = 15, scale = 4)
    var valorMat: BigDecimal = BigDecimal.ZERO,

    @Column(name = "valor_srv", nullable = false, precision = 15, scale = 4)
    var valorSrv: BigDecimal = BigDecimal.ZERO,

    @Column
    val observacoes: String? = null
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orcamento_id", nullable = false)
    var orcamento: Orcamento? = null
        internal set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modulo_id")
    var modulo: OrcamentoModulo? = null
        internal set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: EapItem? = null
        internal set

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _subItens: MutableList<EapItem> = mutableListOf()

    val subItens: List<EapItem>
        get() = _subItens.toList()

    @OneToMany(mappedBy = "eapItem", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _composicoes: MutableList<ComposicaoPreco> = mutableListOf()

    val composicoes: List<ComposicaoPreco>
        get() = _composicoes.toList()

    init {
        checkPositiveValues()
    }

    private fun checkPositiveValues() {
        if (quantidade < BigDecimal.ZERO || valorMo < BigDecimal.ZERO || valorMat < BigDecimal.ZERO || valorSrv < BigDecimal.ZERO) {
            throw DomainSecurityException("Valores da EAP (quantidade, mão de obra, material, serviço) não podem ser negativos.")
        }
    }

    val valorUnitario: BigDecimal
        get() = valorMo.add(valorMat).add(valorSrv)

    val custoTotal: BigDecimal
        get() = valorUnitario.multiply(quantidade)

    fun atualizarValores(novaQuantidade: BigDecimal, mo: BigDecimal, mat: BigDecimal, srv: BigDecimal) {
        if (orcamento?.status?.isImutavel == true) {
            throw DomainSecurityException("Orçamento imutável. Edição de EAP bloqueada.")
        }
        this.quantidade = novaQuantidade
        this.valorMo = mo
        this.valorMat = mat
        this.valorSrv = srv
        checkPositiveValues()
    }

    fun adicionarSubItem(subItem: EapItem) {
        if (orcamento?.status?.isImutavel == true) {
            throw DomainSecurityException("Orçamento imutável. Adição de subitens bloqueada.")
        }
        _subItens.add(subItem)
        subItem.parent = this
        subItem.orcamento = this.orcamento
    }

    fun adicionarComposicao(composicao: ComposicaoPreco) {
        if (orcamento?.status?.isImutavel == true) {
            throw DomainSecurityException("Orçamento imutável. Adição de composições (CPU) bloqueada.")
        }
        _composicoes.add(composicao)
        composicao.eapItem = this
    }
}
