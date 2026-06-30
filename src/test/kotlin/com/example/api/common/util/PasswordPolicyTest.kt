package com.example.api.common.util

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class PasswordPolicyTest {

    private val policy = PasswordPolicy(minLength = 12)

    @Test
    fun `should accept a sufficiently long uncommon password`() {
        assertDoesNotThrow { policy.validate("uma-senha-bem-comprida-2024") }
    }

    @Test
    fun `should reject password below minimum length`() {
        assertThrows(IllegalArgumentException::class.java) { policy.validate("curta123") }
    }

    @Test
    fun `should reject common password even when long enough`() {
        // 12+ chars mas trivial: a blocklist precisa pegar antes do critério de tamanho
        assertThrows(IllegalArgumentException::class.java) { policy.validate("password1234") }
        assertThrows(IllegalArgumentException::class.java) { policy.validate("123456789012") }
    }
}
