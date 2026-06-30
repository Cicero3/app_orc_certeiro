package com.example.api.auth.api.dto

/**
 * Resposta de autenticação. O refresh token NÃO vai aqui — é entregue em cookie
 * httpOnly (P1.5). Só o access token (curto) é exposto, para o SPA manter em memória.
 */
data class AuthResponse(
    val userId: String,
    val email: String,
    val accessToken: String,
    val expiresIn: Long
)
