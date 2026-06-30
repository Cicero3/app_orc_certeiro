package com.example.api.common.ratelimit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class RateLimitingTest {

    // Mapper com suporte a java.time (como o do Spring), para serializar o Instant do ErrorResponse.
    private val mapper = ObjectMapper().registerModule(JavaTimeModule())

    @Test
    fun `limiter should allow up to capacity then block, per key`() {
        val limiter = AuthRateLimiter(capacity = 3, refillPeriodSeconds = 60)

        repeat(3) { assertTrue(limiter.tryConsume("ip-A")) }
        assertFalse(limiter.tryConsume("ip-A"))

        // chave diferente é independente
        assertTrue(limiter.tryConsume("ip-B"))
    }

    private fun loginRequest(): MockHttpServletRequest =
        MockHttpServletRequest("POST", "/api/v1/auth/login").apply {
            servletPath = "/api/v1/auth/login"
            remoteAddr = "203.0.113.7"
        }

    @Test
    fun `filter should pass under limit and return 429 over limit`() {
        val filter = RateLimitingFilter(AuthRateLimiter(capacity = 2, refillPeriodSeconds = 60), mapper, enabled = true)

        repeat(2) {
            val res = MockHttpServletResponse()
            val chain = MockFilterChain()
            filter.doFilter(loginRequest(), res, chain)
            assertEquals(200, res.status)
            assertNotNull(chain.request) // passou adiante
        }

        val res = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(loginRequest(), res, chain)
        assertEquals(429, res.status)
        assertNull(chain.request) // bloqueado, não seguiu a cadeia
        assertTrue(res.contentAsString.contains("RATE_LIMIT_EXCEEDED"))
    }

    @Test
    fun `filter should ignore non-auth paths`() {
        val filter = RateLimitingFilter(AuthRateLimiter(capacity = 1, refillPeriodSeconds = 60), mapper, enabled = true)
        val req = MockHttpServletRequest("GET", "/api/v1/modules").apply { servletPath = "/api/v1/modules" }

        repeat(5) {
            val res = MockHttpServletResponse()
            val chain = MockFilterChain()
            filter.doFilter(req, res, chain)
            assertEquals(200, res.status)
        }
    }
}
