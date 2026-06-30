package com.example.api.auth.api.dto

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "Email é obrigatório")
    @field:Email(message = "Email inválido")
    val email: String,

    @field:NotBlank(message = "Senha é obrigatória")
    @field:Size(min = 12, max = 128, message = "Senha deve ter entre 12 e 128 caracteres")
    val password: String,

    // LGPD: consentimento explícito é obrigatório. @AssertTrue reprova se ausente/false.
    @field:AssertTrue(message = "É necessário aceitar os termos de uso e a política de privacidade")
    val acceptedTerms: Boolean = false
)
