package com.example.api.orcamentos.domain

import java.math.BigDecimal
import java.util.UUID
import jakarta.persistence.*

enum class TipoInsumo {
    MAO_DE_OBRA,
    MATERIAL,
    EQUIPAMENTO,
    SERVICO
}

@Entity
@Table(name = "composicoes_preco")
class ComposicaoPreco(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_insumo", nullable = false)
    val tipoInsumo: TipoInsumo,

    @Column(nullable = false)
    val descricao: String,

    @Column(nullable = false)
    val unidade: String,

    @Column(nullable = false, precision = 12, scale = 6)
    var coeficiente: BigDecimal = BigDecimal.ZERO,

    @Column(name = "custo_unitario_insumo", nullable = false, precision = 15, scale = 4)
    var custoUnitarioInsumo: BigDecimal = BigDecimal.ZERO
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eap_item_id", nullable = false)
    var eapItem: EapItem? = null
        internal set

    init {
        checkPositiveValues()
    }

    private fun checkPositiveValues() {
        if (coeficiente < BigDecimal.ZERO || custoUnitarioInsumo < BigDecimal.ZERO) {
            throw DomainSecurityException("Coeficiente e custo unitário do insumo não podem ser negativos.")
        }
    }

    val custoTotalInsumo: BigDecimal
        get() = coeficiente.multiply(custoUnitarioInsumo)

    fun atualizarValores(novoCoeficiente: BigDecimal, novoCustoUnitario: BigDecimal) {
        if (eapItem?.orcamento?.status?.isImutavel == true) {
            throw DomainSecurityException("Orçamento imutável. Edição de CPU bloqueada.")
        }
        this.coeficiente = novoCoeficiente
        this.custoUnitarioInsumo = novoCustoUnitario
        checkPositiveValues()
    }
}
