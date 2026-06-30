package com.example.api.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuração do cookie httpOnly que carrega o refresh token (P1.5).
 *
 * `secure=false` por padrão para funcionar em dev sob http; em produção deve ser true
 * (override em application-prod.yml). `sameSite=Lax` mitiga CSRF no /auth/refresh quando
 * frontend e backend são same-site. Para deploy cross-site (domínios diferentes), use
 * sameSite=None + secure=true e adicione proteção CSRF.
 */
@Configuration
@ConfigurationProperties(prefix = "app.auth.refresh-cookie")
class AuthCookieProperties {
    var name: String = "refresh_token"
    var path: String = "/api/v1/auth"
    var secure: Boolean = false
    var sameSite: String = "Lax"
    var domain: String = ""
}
