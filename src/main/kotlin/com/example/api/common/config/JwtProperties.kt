package com.example.api.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "jwt")
class JwtProperties {
    var secret: String = "change-me-in-production-must-be-at-least-256-bits-long-xxxxxxxxxx"
    var expirationMs: Long = 86400000 // access token (curto em prod)
    var refreshTokenExpirationMs: Long = 604800000 // refresh token: 7 dias
    var issuer: String = "english-app"
}