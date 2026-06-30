package com.example.api.auth.api

import com.example.api.AbstractIntegrationTest
import com.example.api.auth.api.dto.LoginRequest
import com.example.api.auth.api.dto.RegisterRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class AuthControllerIT : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should register user successfully`() {
        val request = RegisterRequest(
            email = "new-user-${java.util.UUID.randomUUID()}@test.com",
            password = "test-user-password-2024",
            acceptedTerms = true
        )

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.userId").isNotEmpty)
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.data.email").value(request.email))
    }

    @Test
    fun `should reject duplicate email`() {
        val email = "duplicate-${java.util.UUID.randomUUID()}@test.com"
        val request = RegisterRequest(email = email, password = "test-user-password-2024", acceptedTerms = true)

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should login with valid credentials`() {
        val email = "login-${java.util.UUID.randomUUID()}@test.com"
        val password = "test-user-password-2024"

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, password, acceptedTerms = true)))
        )

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequest(email, password)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
    }

    @Test
    fun `should reject invalid login`() {
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    LoginRequest("nonexistent@test.com", "wrong")
                ))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should access protected endpoint with valid token`() {
        val email = "protected-${java.util.UUID.randomUUID()}@test.com"
        val password = "test-user-password-2024"

        val registerResult = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RegisterRequest(email, password, acceptedTerms = true)))
        ).andReturn()

        val responseBody = registerResult.response.contentAsString
        val token = objectMapper.readTree(responseBody).get("data").get("accessToken").asText()

        mockMvc.perform(
            get("/api/v1/auth/me")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.email").value(email))
    }

    @Test
    fun `should reject protected endpoint without token`() {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isForbidden)
    }
}