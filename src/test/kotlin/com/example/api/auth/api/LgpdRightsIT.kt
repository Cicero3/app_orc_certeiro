package com.example.api.auth.api

import com.example.api.AbstractIntegrationTest
import com.example.api.auth.api.dto.RegisterRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class LgpdRightsIT : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private fun registerAndGetToken(email: String): String {
        val result = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, "test-user-password-2024", acceptedTerms = true)))
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("data").get("accessToken").asText()
    }

    @Test
    fun `should reject registration without accepting terms`() {
        val body = objectMapper.writeValueAsString(
            RegisterRequest("noconsent-${java.util.UUID.randomUUID()}@test.com", "test-user-password-2024", acceptedTerms = false)
        )
        mockMvc.perform(
            post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should expose personal data with consent record`() {
        val email = "lgpd-data-${java.util.UUID.randomUUID()}@test.com"
        val token = registerAndGetToken(email)

        mockMvc.perform(get("/api/v1/users/me/data").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.email").value(email))
            .andExpect(jsonPath("$.data.consents.length()").value(1))
            .andExpect(jsonPath("$.data.consents[0].termsVersion").value("1.0"))
    }

    @Test
    fun `should export personal data as downloadable file`() {
        val token = registerAndGetToken("lgpd-export-${java.util.UUID.randomUUID()}@test.com")

        mockMvc.perform(get("/api/v1/users/me/export").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"meus-dados.json\""))
            .andExpect(jsonPath("$.consents[0].termsVersion").value("1.0"))
    }

    @Test
    fun `should anonymize account and block further access with same token`() {
        val email = "lgpd-delete-${java.util.UUID.randomUUID()}@test.com"
        val token = registerAndGetToken(email)

        mockMvc.perform(delete("/api/v1/users/me").header("Authorization", "Bearer $token"))
            .andExpect(status().isNoContent)

        // Mesmo com token ainda válido, a conta anonimizada não autentica mais.
        mockMvc.perform(get("/api/v1/users/me/data").header("Authorization", "Bearer $token"))
            .andExpect(status().isForbidden)
    }
}
