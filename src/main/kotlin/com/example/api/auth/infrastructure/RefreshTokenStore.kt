package com.example.api.auth.infrastructure

import com.example.api.common.config.JwtProperties
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64
import java.util.UUID
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

/**
 * Refresh tokens OPACOS guardados no Redis. O cliente recebe o token bruto; nós só
 * guardamos o hash (SHA-256). TTL = expiração do refresh. Um set por usuário permite
 * revogar todas as sessões no logout.
 *
 * Rotação (uso único): cada /refresh consome o token apresentado e emite um novo.
 */
@Repository
class RefreshTokenStore(
    private val redis: StringRedisTemplate,
    private val jwtProperties: JwtProperties
) {
    private val random = SecureRandom()
    private val ttl: Duration get() = Duration.ofMillis(jwtProperties.refreshTokenExpirationMs)

    private fun tokenKey(hash: String) = "refresh:token:$hash"
    private fun userKey(userId: UUID) = "refresh:user:$userId"

    /** Emite um novo refresh token (bruto) e o associa ao usuário. */
    fun issue(userId: UUID): String {
        val raw = newRawToken()
        val hash = sha256(raw)
        redis.opsForValue().set(tokenKey(hash), userId.toString(), ttl)
        redis.opsForSet().add(userKey(userId), hash)
        redis.expire(userKey(userId), ttl)
        return raw
    }

    /**
     * Valida e CONSOME o refresh token (rotação). Retorna o userId se válido, ou null
     * se inexistente/expirado/já usado. O chamador deve emitir um novo via [issue].
     */
    fun consume(rawToken: String): UUID? {
        val hash = sha256(rawToken)
        val userId = redis.opsForValue().get(tokenKey(hash)) ?: return null
        redis.delete(tokenKey(hash))
        redis.opsForSet().remove(userKey(UUID.fromString(userId)), hash)
        return UUID.fromString(userId)
    }

    /** Revoga TODAS as sessões (refresh tokens) do usuário — usado no logout. */
    fun revokeAllForUser(userId: UUID) {
        val hashes = redis.opsForSet().members(userKey(userId)) ?: emptySet()
        if (hashes.isNotEmpty()) {
            redis.delete(hashes.map { tokenKey(it) })
        }
        redis.delete(userKey(userId))
    }

    private fun newRawToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
