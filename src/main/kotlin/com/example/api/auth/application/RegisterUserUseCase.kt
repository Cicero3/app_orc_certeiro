package com.example.api.auth.application

import com.example.api.auth.domain.User
import com.example.api.auth.domain.UserConsent
import com.example.api.auth.domain.UserProfile
import com.example.api.auth.infrastructure.UserConsentRepository
import com.example.api.auth.infrastructure.UserProfileRepository
import com.example.api.auth.infrastructure.UserRepository
import com.example.api.common.util.PasswordHasher
import com.example.api.common.util.PasswordPolicy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val userConsentRepository: UserConsentRepository,
    private val passwordHasher: PasswordHasher,
    private val passwordPolicy: PasswordPolicy,
    @Value("\${app.legal.terms-version:1.0}") private val termsVersion: String
) {
    @Transactional
    fun execute(email: String, password: String, acceptedTerms: Boolean): User {
        val normalizedEmail = email.trim().lowercase()

        // LGPD: sem consentimento explícito, não há base legal por consentimento.
        if (!acceptedTerms) {
            throw IllegalArgumentException("É necessário aceitar os termos e a política de privacidade")
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw IllegalArgumentException("Email já registrado")
        }

        passwordPolicy.validate(password)

        val user = User(
            email = normalizedEmail,
            passwordHash = passwordHasher.hash(password)
        )
        val savedUser = userRepository.save(user)

        // Não passar userId explicitamente - o @MapsId cuida disso
        val profile = UserProfile(
            user = savedUser
        )
        userProfileRepository.save(profile)

        userConsentRepository.save(
            UserConsent(userId = savedUser.id, termsVersion = termsVersion)
        )

        return savedUser
    }
}
