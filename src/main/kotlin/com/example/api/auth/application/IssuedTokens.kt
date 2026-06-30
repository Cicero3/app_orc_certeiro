package com.example.api.auth.application

/**
 * Par de tokens recém-emitido. O access token vai no corpo da resposta; o refresh token
 * é colocado pelo controller em cookie httpOnly (nunca exposto no corpo).
 */
data class IssuedTokens(
    val userId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)
