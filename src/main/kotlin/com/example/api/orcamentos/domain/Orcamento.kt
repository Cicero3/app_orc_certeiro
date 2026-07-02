package com.example.api.orcamentos.domain

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp

@Entity
@Table(name = "orcamentos")
class Orcamento(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "owner_id", nullable = false)
    val ownerId: UUID,

    @Column(name = "parent_id")
    val parentId: UUID? = null, // Para revisões

    @Column(nullable = false)
    val titulo: String,

    @Column(nullable = false, precision = 5, scale = 4)
    var bdi: BigDecimal = BigDecimal.ZERO // Ex: 0.2500 para 25%
) {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrcamentoStatus = OrcamentoStatus.RASCUNHO
        protected set // Hibernate precisa setar via reflexão, mas a lógica de domínio restringe chamadas diretas

    @OneToMany(mappedBy = "orcamento", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _modulos: MutableList<OrcamentoModulo> = mutableListOf()

    val modulos: List<OrcamentoModulo>
        get() = _modulos.toList()

    @OneToMany(mappedBy = "orcamento", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _ambientes: MutableList<AmbienteProjeto> = mutableListOf()

    val ambientes: List<AmbienteProjeto>
        get() = _ambientes.toList()

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
        protected set

    // Calcula o valor total com base nos itens da EAP e BDI
    val valorTotal: BigDecimal
        get() {
            // Soma de todos os itens folha da EAP em todos os módulos
            val somaEap = _modulos.flatMap { it.eapItens }
                                  .filter { it.subItens.isEmpty() } // Apenas itens folha
                                  .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.custoTotal) }
            val fatorBdi = BigDecimal.ONE.add(bdi)
            return somaEap.multiply(fatorBdi)
        }

    // --- GUARD CLAUSES & COMPORTAMENTOS DO DOMÍNIO ---

    private fun checkMutabilidade() {
        if (status.isImutavel) {
            throw DomainSecurityException("Orçamento em status $status é imutável. Edição bloqueada.")
        }
    }

    fun adicionarModulo(modulo: OrcamentoModulo) {
        checkMutabilidade()
        _modulos.add(modulo)
        modulo.orcamento = this
    }

    fun adicionarAmbiente(ambiente: AmbienteProjeto) {
        checkMutabilidade()
        _ambientes.add(ambiente)
        ambiente.orcamento = this
    }

    fun atualizarBdi(novoBdi: BigDecimal) {
        checkMutabilidade()
        if (novoBdi < BigDecimal.ZERO) {
            throw DomainSecurityException("BDI não pode ser negativo.")
        }
        this.bdi = novoBdi
    }

    // --- MÁQUINA DE ESTADOS ---

    fun solicitarAprovacao(solicitanteId: UUID) {
        if (this.status != OrcamentoStatus.RASCUNHO) {
            throw DomainSecurityException("Apenas orçamentos em RASCUNHO podem ser enviados para revisão.")
        }
        if (this.ownerId != solicitanteId) {
            throw DomainSecurityException("Apenas o dono do orçamento pode solicitar aprovação.")
        }
        this.status = OrcamentoStatus.EM_REVISAO
    }

    fun aprovar(aprovadorId: UUID, isAdmin: Boolean) {
        if (this.status != OrcamentoStatus.EM_REVISAO) {
            throw DomainSecurityException("Apenas orçamentos EM_REVISAO podem ser aprovados.")
        }
        if (!isAdmin) {
            throw DomainSecurityException("Apenas um administrador pode aprovar o orçamento internamente.")
        }
        this.status = OrcamentoStatus.APROVADO
    }

    fun rejeitarInternamente(aprovadorId: UUID, isAdmin: Boolean) {
        if (this.status != OrcamentoStatus.EM_REVISAO) {
            throw DomainSecurityException("Apenas orçamentos EM_REVISAO podem ser rejeitados internamente.")
        }
        if (!isAdmin) {
            throw DomainSecurityException("Apenas um administrador pode rejeitar o orçamento internamente.")
        }
        this.status = OrcamentoStatus.REJEITADO
    }

    fun enviarAoCliente() {
        if (this.status != OrcamentoStatus.APROVADO) {
            throw DomainSecurityException("Apenas orçamentos APROVADOS podem ser enviados ao cliente.")
        }
        this.status = OrcamentoStatus.ENVIADO_CLIENTE
    }

    fun registrarRespostaCliente(aceito: Boolean) {
        if (this.status != OrcamentoStatus.ENVIADO_CLIENTE) {
            throw DomainSecurityException("Apenas orçamentos ENVIADO_CLIENTE podem receber resposta.")
        }
        this.status = if (aceito) OrcamentoStatus.ACEITO else OrcamentoStatus.REJEITADO
    }
    
    fun cancelar() {
        if (this.status.isImutavel && this.status != OrcamentoStatus.APROVADO) {
             throw DomainSecurityException("Não é possível cancelar um orçamento que já foi enviado ao cliente ou finalizado.")
        }
        this.status = OrcamentoStatus.CANCELADO
    }
}
