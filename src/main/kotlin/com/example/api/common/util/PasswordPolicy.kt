package com.example.api.common.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Política de senha (alinhada ao NIST 800-63B): prioriza comprimento e bloqueia
 * senhas notoriamente fracas, em vez de exigir composição (maiúscula/símbolo).
 * minLength configurável via security.password.min-length.
 */
@Component
class PasswordPolicy(
    @Value("\${security.password.min-length:12}") private val minLength: Int = 12
) {
    // Senhas comuns. Inclui entradas com >= 12 chars: são as que passariam pelo
    // critério de comprimento e ainda assim são triviais de adivinhar.
    private val blocklist = setOf(
        "password", "password123", "password1234", "passw0rd1234",
        "123456789012", "123456789", "1234567890", "12345678",
        "qwerty123456", "qwertyuiop", "iloveyou1234", "administrador",
        "senha123456", "trocarsenha1", "mudarsenha123", "welcome123456"
    )

    fun validate(rawPassword: String) {
        val normalized = rawPassword.lowercase()
        if (normalized in blocklist) {
            throw IllegalArgumentException("Senha muito comum. Escolha uma senha mais forte")
        }
        if (rawPassword.length < minLength) {
            throw IllegalArgumentException("Senha deve ter pelo menos $minLength caracteres")
        }
    }
}
