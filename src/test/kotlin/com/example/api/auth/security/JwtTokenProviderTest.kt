package com.example.api.auth.security

import com.example.api.common.config.JwtProperties
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JwtTokenProviderTest {

    private val provider = JwtTokenProvider(
        JwtProperties().apply {
            secret = "test-secret-that-is-at-least-256-bits-long-xxxxxxxxxxxxxxx"
            expirationMs = 3_600_000
            issuer = "english-app-test"
        }
    )

    @Test
    fun `should round-trip user id and role in token`() {
        val userId = UUID.randomUUID()
        val token = provider.generateToken(userId, "admin@test.com", "ADMIN")

        assertTrue(provider.validateToken(token))
        assertEquals(userId, provider.extractUserId(token))
        assertEquals("ADMIN", provider.extractRole(token))
    }

    @Test
    fun `should default role to USER when claim missing`() {
        // token sem claim de role continua válido e assume USER
        val token = provider.generateToken(UUID.randomUUID(), "u@test.com", "USER")
        assertEquals("USER", provider.extractRole(token))
    }

    @Test
    fun `should reject malformed token`() {
        assertFalse(provider.validateToken("not-a-jwt"))
    }

    @Test
    fun `should reject token signed with different secret`() {
        val other = JwtTokenProvider(
            JwtProperties().apply { secret = "another-completely-different-secret-key-xxxxxxxxxxxxxxxxx" }
        )
        val foreign = other.generateToken(UUID.randomUUID(), "x@test.com", "USER")
        assertFalse(provider.validateToken(foreign))
    }
}
