package com.example.api.common.ratelimit

import com.example.api.common.dto.ErrorResponse
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Aplica rate limiting apenas em POST /api/v1/auth/login e /register.
 * Roda antes da cadeia de segurança (não depende de autenticação).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RateLimitingFilter(
    private val rateLimiter: AuthRateLimiter,
    private val objectMapper: ObjectMapper,
    @Value("\${security.rate-limit.auth.enabled:true}") private val enabled: Boolean
) : OncePerRequestFilter() {

    private val limitedPaths = setOf("/api/v1/auth/login", "/api/v1/auth/register")

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !enabled || !(request.method == "POST" && request.servletPath in limitedPaths)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (rateLimiter.tryConsume(clientIp(request))) {
            filterChain.doFilter(request, response)
        } else {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            val body = ErrorResponse(
                error = ErrorResponse.ErrorDetail(
                    code = "RATE_LIMIT_EXCEEDED",
                    message = "Muitas tentativas. Tente novamente em instantes.",
                    path = request.requestURI
                )
            )
            objectMapper.writeValue(response.writer, body)
        }
    }

    private fun clientIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        return if (!forwarded.isNullOrBlank()) forwarded.split(",")[0].trim() else request.remoteAddr
    }
}
