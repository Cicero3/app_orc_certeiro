package com.example.api.auth.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    // var: pode ser anonimizado no exercício do direito ao apagamento (LGPD Art. 18).
    @Column(nullable = false, unique = true)
    var email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    // RBAC: 'USER' por padrão, 'ADMIN' para administração. var pois pode ser promovido.
    @Column(nullable = false)
    var role: String = "USER",

    @Column(name = "google_id", unique = true)
    var googleId: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
)