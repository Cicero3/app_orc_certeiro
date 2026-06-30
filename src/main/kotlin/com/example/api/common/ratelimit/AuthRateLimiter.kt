package com.example.api.common.ratelimit

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Rate limiter em memória por chave (IP) para endpoints de autenticação,
 * mitigando brute-force/credential-stuffing.
 *
 * Limitação (MVP): buckets ficam em memória local — não compartilhado entre
 * instâncias e sem expiração de chaves ociosas. Para multi-instância, migrar
 * para Bucket4j + Redis. Por ora, capacidade/refill configuráveis.
 */
@Component
class AuthRateLimiter(
    @Value("\${security.rate-limit.auth.capacity:10}") private val capacity: Long,
    @Value("\${security.rate-limit.auth.refill-period-seconds:60}") private val refillPeriodSeconds: Long
) {
    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun tryConsume(key: String): Boolean = buckets.computeIfAbsent(key) { newBucket() }.tryConsume(1)

    private fun newBucket(): Bucket {
        val limit = Bandwidth.classic(
            capacity,
            Refill.greedy(capacity, Duration.ofSeconds(refillPeriodSeconds))
        )
        return Bucket.builder().addLimit(limit).build()
    }
}
