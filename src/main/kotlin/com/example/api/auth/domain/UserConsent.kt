package com.example.api.auth.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Registro de consentimento do titular (LGPD). Imutável/append-only:
 * cada aceite gera uma linha (permite histórico em novas versões de termos).
 */
@Entity
@Table(name = "user_consents")
class UserConsent(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    val userId: UUID,

    @Column(name = "terms_version", nullable = false)
    val termsVersion: String,

    @Column(name = "consented_at", nullable = false, updatable = false)
    val consentedAt: Instant = Instant.now()
)
