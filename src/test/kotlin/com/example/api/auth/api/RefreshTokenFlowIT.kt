package com.example.api.auth.api

import com.example.api.AbstractIntegrationTest
import com.example.api.auth.api.dto.RegisterRequest
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class RefreshTokenFlowIT : AbstractIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    private val cookieName = "refresh_token"

    private fun register(): MvcResult {
        val email = "refresh-${java.util.UUID.randomUUID()}@test.com"
        return mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "test-user-password-2024", acceptedTerms = true)))
        ).andExpect(status().isCreated).andReturn()
    }

    private fun accessTokenOf(result: MvcResult): String =
        objectMapper.readTree(result.response.contentAsString).get("data").get("accessToken").asText()

    /** Valor do refresh token a partir do header Set-Cookie (cookie httpOnly não é lido por JS). */
    private fun refreshCookieValue(result: MvcResult): String {
        val setCookie = result.response.getHeaders("Set-Cookie").first { it.startsWith("$cookieName=") }
        return setCookie.substringAfter("$cookieName=").substringBefore(";")
    }

    private fun refreshWith(token: String) =
        post("/api/v1/auth/refresh").cookie(Cookie(cookieName, token))

    @Test
    fun `register issues access token in body and refresh token in httpOnly cookie`() {
        val result = register()
        assertThat(accessTokenOf(result)).isNotBlank()
        // refresh NÃO vai no corpo
        assertThat(result.response.contentAsString).doesNotContain("refreshToken")
        // refresh vai em cookie httpOnly
        val setCookie = result.response.getHeaders("Set-Cookie").first { it.startsWith("$cookieName=") }
        assertThat(setCookie).contains("HttpOnly")
        assertThat(refreshCookieValue(result)).isNotBlank()
    }

    @Test
    fun `refresh should rotate the cookie and invalidate the used refresh token`() {
        val refresh1 = refreshCookieValue(register())

        val refreshed = mockMvc.perform(refreshWith(refresh1)).andExpect(status().isOk).andReturn()
        val refresh2 = refreshCookieValue(refreshed)
        assertNotEquals(refresh1, refresh2) // rotacionou

        // reusar o refresh token antigo -> 401 (uso único)
        mockMvc.perform(refreshWith(refresh1)).andExpect(status().isUnauthorized)

        // o novo refresh token ainda funciona
        mockMvc.perform(refreshWith(refresh2)).andExpect(status().isOk)
    }

    @Test
    fun `refresh without cookie is unauthorized`() {
        mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `logout should revoke refresh and immediately block the access token`() {
        val result = register()
        val access = accessTokenOf(result)
        val refresh = refreshCookieValue(result)

        // antes do logout: o access token funciona
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer $access"))
            .andExpect(status().isOk)

        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer $access"))
            .andExpect(status().isNoContent)

        // access token revogado imediatamente (denylist) -> 403
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer $access"))
            .andExpect(status().isForbidden)

        // refresh token também foi revogado -> 401
        mockMvc.perform(refreshWith(refresh)).andExpect(status().isUnauthorized)
    }
}
