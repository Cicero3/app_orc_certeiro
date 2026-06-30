package com.example.api.auth.infrastructure

import java.time.Duration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

/**
 * Denylist de access tokens por `jti`, no Redis. Permite que o logout invalide
 * IMEDIATAMENTE um access token (que é stateless) antes da sua expiração natural.
 * A entrada expira junto com o token (TTL = vida restante), então não acumula lixo.
 */
@Repository
class TokenDenylist(
    private val redis: StringRedisTemplate
) {
    private fun key(jti: String) = "denylist:jti:$jti"

    fun denylist(jti: String, ttlMillis: Long) {
        if (ttlMillis <= 0) return
        redis.opsForValue().set(key(jti), "1", Duration.ofMillis(ttlMillis))
    }

    fun isDenylisted(jti: String): Boolean = redis.hasKey(key(jti))
}
