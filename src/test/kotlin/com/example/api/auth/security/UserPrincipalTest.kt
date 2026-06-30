package com.example.api.auth.security

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserPrincipalTest {

    @Test
    fun `should expose role as ROLE_ prefixed authority`() {
        val principal = UserPrincipal(UUID.randomUUID(), "u@test.com", "ADMIN")
        val authorities = principal.authorities.map { it.authority }

        assertEquals(1, authorities.size)
        assertTrue(authorities.contains("ROLE_ADMIN"))
    }

    @Test
    fun `should default to USER role authority`() {
        val principal = UserPrincipal(UUID.randomUUID(), "u@test.com")
        assertTrue(principal.authorities.map { it.authority }.contains("ROLE_USER"))
    }
}
