package com.example.api.auth.security

import com.example.api.common.config.JwtProperties
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(
        jwtProperties.secret.toByteArray(StandardCharsets.UTF_8)
    )

    fun generateToken(userId: UUID, email: String, role: String): String {
        val now = Instant.now()
        val expiry = now.plusMillis(jwtProperties.expirationMs)

        return Jwts.builder()
            .id(UUID.randomUUID().toString()) // jti: identifica o token p/ denylist (logout)
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role)
            .issuer(jwtProperties.issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String): Boolean = try {
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
        true
    } catch (e: JwtException) {
        // assinatura inválida, token expirado, malformado, etc.
        false
    } catch (e: IllegalArgumentException) {
        // token nulo/vazio
        false
    }

    fun extractUserId(token: String): UUID {
        return UUID.fromString(parseClaims(token).subject)
    }

    fun extractRole(token: String): String {
        return parseClaims(token).get("role", String::class.java) ?: "USER"
    }

    fun extractJti(token: String): String? = parseClaims(token).id

    /** Milissegundos restantes até o token expirar (>= 0). Usado como TTL da denylist. */
    fun remainingMillis(token: String): Long {
        val exp = parseClaims(token).expiration?.toInstant() ?: return 0
        return (exp.toEpochMilli() - Instant.now().toEpochMilli()).coerceAtLeast(0)
    }

    private fun parseClaims(token: String) =
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

    fun getExpirationMs(): Long = jwtProperties.expirationMs
}