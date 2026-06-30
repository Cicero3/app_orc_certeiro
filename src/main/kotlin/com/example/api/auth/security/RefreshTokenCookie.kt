package com.example.api.auth.security

import com.example.api.common.config.AuthCookieProperties
import com.example.api.common.config.JwtProperties
import jakarta.servlet.http.HttpServletRequest
import java.time.Duration
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

/**
 * Constrói e lê o cookie httpOnly do refresh token. O cliente nunca acessa o token
 * via JS (proteção contra XSS); o navegador o envia automaticamente para /api/v1/auth.
 */
@Component
class RefreshTokenCookie(
    private val props: AuthCookieProperties,
    private val jwtProperties: JwtProperties
) {
    /** Cookie de emissão, com TTL igual ao do refresh token. */
    fun build(rawToken: String): ResponseCookie =
        base(rawToken).maxAge(Duration.ofMillis(jwtProperties.refreshTokenExpirationMs)).build()

    /** Cookie de expiração imediata (logout). */
    fun clear(): ResponseCookie = base("").maxAge(0).build()

    /** Lê o valor do cookie da requisição, ou null se ausente/vazio. */
    fun read(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == props.name }
            ?.value
            ?.takeIf { it.isNotBlank() }

    private fun base(value: String): ResponseCookie.ResponseCookieBuilder {
        val builder = ResponseCookie.from(props.name, value)
            .httpOnly(true)
            .secure(props.secure)
            .path(props.path)
            .sameSite(props.sameSite)
        if (props.domain.isNotBlank()) builder.domain(props.domain)
        return builder
    }
}
