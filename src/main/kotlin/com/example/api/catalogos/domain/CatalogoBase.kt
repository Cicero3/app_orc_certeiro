package com.example.api.catalogos.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "catalogos_bases")
class CatalogoBase(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val nome: String,

    @Column(name = "mes_ano", nullable = false)
    val mesAno: String,

    @Column(nullable = false)
    val estado: String,

    @Column(name = "is_desonerado", nullable = false)
    val isDesonerado: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
)
