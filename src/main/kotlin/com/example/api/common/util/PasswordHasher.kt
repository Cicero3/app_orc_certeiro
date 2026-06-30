package com.example.api.common.util

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class PasswordHasher {
    // Cost 12: ~250ms por hash, mais resistente a brute-force offline que o 10.
    private val encoder = BCryptPasswordEncoder(12)

    fun hash(rawPassword: String): String = encoder.encode(rawPassword)

    fun verify(rawPassword: String, hashedPassword: String): Boolean =
        encoder.matches(rawPassword, hashedPassword)
}