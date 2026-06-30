package com.example.api.auth.application

import com.example.api.auth.api.dto.ConsentData
import com.example.api.auth.api.dto.PersonalDataResponse
import com.example.api.auth.api.dto.ProfileData
import com.example.api.auth.infrastructure.UserConsentRepository
import com.example.api.auth.infrastructure.UserProfileRepository
import com.example.api.auth.infrastructure.UserRepository
import jakarta.persistence.EntityNotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Direitos do titular (LGPD Art. 18): acesso aos dados e exclusão por anonimização.
 */
@Service
class UserAccountService(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val userConsentRepository: UserConsentRepository
) {

    @Transactional(readOnly = true)
    fun getPersonalData(userId: UUID): PersonalDataResponse {
        val user = userRepository.findById(userId).orElseThrow {
            EntityNotFoundException("User $userId not found")
        }
        val profile = userProfileRepository.findById(userId).orElse(null)?.let {
            ProfileData(
                displayName = it.displayName,
                currentLevel = it.currentLevel,
                totalXp = it.totalXp,
                streakDays = it.streakDays,
                lastActiveAt = it.lastActiveAt,
                createdAt = it.createdAt
            )
        }
        val consents = userConsentRepository.findByUserIdOrderByConsentedAtDesc(userId).map {
            ConsentData(termsVersion = it.termsVersion, consentedAt = it.consentedAt)
        }
        return PersonalDataResponse(
            userId = user.id,
            email = user.email,
            createdAt = user.createdAt,
            profile = profile,
            consents = consents
        )
    }

    /**
     * Exclusão por ANONIMIZAÇÃO (LGPD Art. 18, VI). Remove os dados que identificam
     * o titular, mas preserva o registro (e o progresso pseudonimizado) para integridade.
     * A conta deixa de autenticar (deletedAt setado; o JwtAuthenticationFilter barra).
     */
    @Transactional
    fun anonymizeAccount(userId: UUID) {
        val user = userRepository.findById(userId).orElseThrow {
            EntityNotFoundException("User $userId not found")
        }
        if (user.deletedAt != null) return // já anonimizada (idempotente)

        user.email = "anonimizado-${UUID.randomUUID()}@deleted.local"
        user.googleId = null
        user.passwordHash = "ANONYMIZED" // não corresponde a nenhum hash BCrypt -> login impossível
        user.deletedAt = Instant.now()
        userRepository.save(user)

        userProfileRepository.findById(userId).ifPresent { profile ->
            profile.displayName = null
            userProfileRepository.save(profile)
        }
    }
}
