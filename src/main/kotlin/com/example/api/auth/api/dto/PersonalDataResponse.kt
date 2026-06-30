package com.example.api.auth.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant
import java.util.UUID

/**
 * Todos os dados pessoais que o sistema guarda do titular (LGPD Art. 18, II/V).
 * Serve tanto para acesso (/me/data) quanto para portabilidade (/me/export).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonalDataResponse(
    val userId: UUID,
    val email: String,
    val createdAt: Instant,
    val profile: ProfileData?,
    val consents: List<ConsentData>
)

data class ProfileData(
    val displayName: String?,
    val currentLevel: String,
    val totalXp: Int,
    val streakDays: Int,
    val lastActiveAt: Instant?,
    val createdAt: Instant
)

data class ConsentData(
    val termsVersion: String,
    val consentedAt: Instant
)
