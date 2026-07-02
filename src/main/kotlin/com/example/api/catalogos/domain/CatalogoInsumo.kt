package com.example.api.catalogos.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "catalogos_insumos")
class CatalogoInsumo(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    val item: CatalogoItem,

    @Column(name = "tipo_insumo", nullable = false)
    val tipoInsumo: String,

    @Column
    val codigo: String? = null,

    @Column(nullable = false)
    val descricao: String,

    @Column(nullable = false)
    val unidade: String,

    @Column(nullable = false, precision = 19, scale = 6)
    val coeficiente: BigDecimal,

    @Column(name = "custo_unitario", nullable = false, precision = 19, scale = 4)
    val custoUnitario: BigDecimal,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
)
