package com.example.api.catalogos.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "catalogos_itens")
class CatalogoItem(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalogo_id", nullable = false)
    val catalogo: CatalogoBase,

    @Column(nullable = false)
    val codigo: String,

    @Column(nullable = false)
    val descricao: String,

    @Column(nullable = false)
    val unidade: String,

    @Column(name = "valor_mo", nullable = false, precision = 19, scale = 4)
    val valorMo: BigDecimal = BigDecimal.ZERO,

    @Column(name = "valor_mat", nullable = false, precision = 19, scale = 4)
    val valorMat: BigDecimal = BigDecimal.ZERO,

    @Column(name = "valor_srv", nullable = false, precision = 19, scale = 4)
    val valorSrv: BigDecimal = BigDecimal.ZERO,

    @OneToMany(mappedBy = "item", cascade = [CascadeType.ALL], orphanRemoval = true)
    val insumos: MutableList<CatalogoInsumo> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
)
