package com.example.api.orcamentos.domain

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

/** Categorias do checklist de custos indiretos (aba "4. CUSTO IND" da planilha). */
enum class CategoriaCustoIndireto {
    EQUIPE_TECNICA,
    EQUIPE_SUPORTE,
    EQUIPE_ADMINISTRATIVA,
    MOBILIZACAO,
    EQP_CANTEIRO,
    EQP_ADMINISTRATIVO,
    PROTECAO_COLETIVA,
    EPI,
    FERRAMENTAS,
    DESPESAS_CORRENTES,
    DESPESAS_PESSOAL,
    SERVICOS_TERCEIROS,
    TAXAS,
    DIVERSOS
}

@Entity
@Table(name = "custos_indiretos")
class CustoIndireto(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "orcamento_id", nullable = false)
    val orcamentoId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var categoria: CategoriaCustoIndireto,

    @Column(nullable = false)
    var descricao: String,

    @Column(nullable = false, precision = 12, scale = 4)
    var quantidade: BigDecimal = BigDecimal.ONE,

    @Column(name = "valor_unitario", nullable = false, precision = 15, scale = 4)
    var valorUnitario: BigDecimal = BigDecimal.ZERO
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
        checkPositive()
    }

    private fun checkPositive() {
        if (quantidade < BigDecimal.ZERO || valorUnitario < BigDecimal.ZERO) {
            throw DomainSecurityException("Quantidade e valor unitário do custo indireto não podem ser negativos.")
        }
    }

    val total: BigDecimal
        get() = quantidade.multiply(valorUnitario)

    fun atualizar(novaCategoria: CategoriaCustoIndireto, novaDescricao: String, novaQuantidade: BigDecimal, novoValor: BigDecimal) {
        this.categoria = novaCategoria
        this.descricao = novaDescricao
        this.quantidade = novaQuantidade
        this.valorUnitario = novoValor
        checkPositive()
    }
}
