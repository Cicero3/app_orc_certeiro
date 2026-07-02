package com.example.api.orcamentos.domain

import java.math.BigDecimal
import java.util.UUID
import jakarta.persistence.*

@Entity
@Table(name = "ambientes_projeto")
class AmbienteProjeto(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "nome_ambiente", nullable = false)
    var nomeAmbiente: String,

    @Column(nullable = false, precision = 10, scale = 4)
    var largura: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 10, scale = 4)
    var comprimento: BigDecimal = BigDecimal.ZERO,

    @Column(name = "pe_direito", nullable = false, precision = 10, scale = 4)
    var peDireito: BigDecimal = BigDecimal.ZERO
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orcamento_id", nullable = false)
    var orcamento: Orcamento? = null
        internal set

    init {
        checkPositiveValues()
    }

    private fun checkPositiveValues() {
        if (largura < BigDecimal.ZERO || comprimento < BigDecimal.ZERO || peDireito < BigDecimal.ZERO) {
            throw DomainSecurityException("Dimensões do ambiente não podem ser negativas.")
        }
    }

    // Cálculos automáticos de área
    val areaPisoForro: BigDecimal
        get() = largura.multiply(comprimento)

    val areaParede: BigDecimal
        get() {
            val perimetro = largura.add(comprimento).multiply(BigDecimal("2"))
            return perimetro.multiply(peDireito)
        }

    fun atualizarDimensoes(novaLargura: BigDecimal, novoComprimento: BigDecimal, novoPeDireito: BigDecimal) {
        if (orcamento?.status?.isImutavel == true) {
            throw DomainSecurityException("Orçamento imutável. Alteração de ambientes bloqueada.")
        }
        this.largura = novaLargura
        this.comprimento = novoComprimento
        this.peDireito = novoPeDireito
        checkPositiveValues()
    }
}
