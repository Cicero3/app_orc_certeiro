package com.example.api.cpus.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class TipoInsumoCpu { MAO_DE_OBRA, MATERIAL, EQUIPAMENTO, SERVICO }

/**
 * Composição de Preço Unitário própria do usuário, reutilizável entre orçamentos
 * (aba "3. CPU" da planilha). Uma CPU marcada como auxiliar (ex.: argamassa 1:2:8)
 * pode ser usada como insumo de outra CPU — profundidade máxima 1, como na planilha.
 */
@Entity
@Table(name = "cpus_proprias")
class CpuPropria(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "owner_id", nullable = false)
    val ownerId: UUID,

    @Column(nullable = false)
    var codigo: String,

    @Column(nullable = false)
    var descricao: String,

    @Column(nullable = false)
    var unidade: String,

    @Column(name = "is_auxiliar", nullable = false)
    val isAuxiliar: Boolean = false
) {
    @OneToMany(mappedBy = "cpu", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _insumos: MutableList<CpuInsumo> = mutableListOf()

    val insumos: List<CpuInsumo>
        get() = _insumos.toList()

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
        protected set

    fun adicionarInsumo(insumo: CpuInsumo) {
        if (insumo.cpuReferencia != null) {
            check(!isAuxiliar) { "Composição auxiliar não pode referenciar outra composição (profundidade máxima 1)." }
            check(insumo.cpuReferencia!!.isAuxiliar) { "Somente composições marcadas como auxiliares podem ser usadas como insumo." }
            check(insumo.cpuReferencia!!.id != this.id) { "Uma composição não pode referenciar a si mesma." }
        }
        _insumos.add(insumo)
        insumo.cpu = this
    }

    fun limparInsumos() = _insumos.clear()

    /** Custo unitário do insumo resolvido: CPU auxiliar > função salarial > custo digitado. */
    private fun custoResolvido(insumo: CpuInsumo): BigDecimal =
        insumo.cpuReferencia?.valorUnitario
            ?: insumo.funcaoSalarial?.valorHora
            ?: insumo.custoUnitario

    private fun somaPorTipo(vararg tipos: TipoInsumoCpu): BigDecimal =
        _insumos.filter { it.tipoInsumo in tipos }
            .fold(BigDecimal.ZERO) { acc, i -> acc.add(i.coeficiente.multiply(custoResolvido(i))) }

    val valorMo: BigDecimal
        get() = somaPorTipo(TipoInsumoCpu.MAO_DE_OBRA)

    val valorMat: BigDecimal
        get() = somaPorTipo(TipoInsumoCpu.MATERIAL, TipoInsumoCpu.EQUIPAMENTO)

    val valorSrv: BigDecimal
        get() = somaPorTipo(TipoInsumoCpu.SERVICO)

    val valorUnitario: BigDecimal
        get() = valorMo.add(valorMat).add(valorSrv)

    /** Custo unitário efetivo de um insumo (exposto para cópia/snapshot na EAP). */
    fun custoUnitarioEfetivo(insumo: CpuInsumo): BigDecimal = custoResolvido(insumo)
}

@Entity
@Table(name = "cpus_proprias_insumos")
class CpuInsumo(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_insumo", nullable = false)
    val tipoInsumo: TipoInsumoCpu,

    @Column(nullable = false)
    var descricao: String,

    @Column(nullable = false)
    var unidade: String,

    @Column(nullable = false, precision = 12, scale = 6)
    var coeficiente: BigDecimal = BigDecimal.ZERO,

    @Column(name = "custo_unitario", nullable = false, precision = 15, scale = 4)
    var custoUnitario: BigDecimal = BigDecimal.ZERO,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcao_salarial_id")
    var funcaoSalarial: FuncaoSalarial? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cpu_referencia_id")
    var cpuReferencia: CpuPropria? = null
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cpu_id", nullable = false)
    var cpu: CpuPropria? = null
        internal set

    init {
        require(coeficiente >= BigDecimal.ZERO && custoUnitario >= BigDecimal.ZERO) {
            "Coeficiente e custo unitário não podem ser negativos."
        }
    }
}
