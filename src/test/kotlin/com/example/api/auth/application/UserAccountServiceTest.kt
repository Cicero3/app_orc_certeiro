package com.example.api.auth.application

import com.example.api.auth.domain.User
import com.example.api.auth.infrastructure.UserConsentRepository
import com.example.api.auth.infrastructure.UserProfileRepository
import com.example.api.auth.infrastructure.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserAccountServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val userProfileRepository = mockk<UserProfileRepository>(relaxed = true)
    private val userConsentRepository = mockk<UserConsentRepository>()
    private val service = UserAccountService(userRepository, userProfileRepository, userConsentRepository)

    @Test
    fun `anonymizeAccount should strip PII and mark deleted`() {
        val user = User(email = "joao@test.com", passwordHash = "\$2a\$12\$realhash")
        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { userRepository.save(any()) } answers { firstArg() }
        every { userProfileRepository.findById(user.id) } returns Optional.empty()

        service.anonymizeAccount(user.id)

        assertTrue(user.email.startsWith("anonimizado-"))
        assertTrue(user.email.endsWith("@deleted.local"))
        assertEquals("ANONYMIZED", user.passwordHash)
        assertNotNull(user.deletedAt)
        verify(exactly = 1) { userRepository.save(user) }
    }

    @Test
    fun `anonymizeAccount should be idempotent when already deleted`() {
        val user = User(email = "anonimizado-x@deleted.local", passwordHash = "ANONYMIZED")
            .apply { deletedAt = java.time.Instant.now() }
        every { userRepository.findById(user.id) } returns Optional.of(user)

        service.anonymizeAccount(user.id)

        verify(exactly = 0) { userRepository.save(any()) }
    }
}
