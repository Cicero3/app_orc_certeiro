package com.example.api.common.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Metadados do OpenAPI usados para gerar o client TS do frontend
 * (ver docs/frontend-api-client.md). Declara o esquema de auth Bearer JWT
 * globalmente, para que o spec e o client tratem o Authorization header.
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        val securitySchemeName = "bearer-jwt"
        return OpenAPI()
            .info(
                Info()
                    .title("Spring Kotlin API Starter")
                    .version("v1")
                    .description(
                        """
                        API base (starter Kotlin + Spring Boot). Substitua pela descrição do seu domínio.

                        Convenções:
                        - Listas usam envelope `{ "data": [...], "meta": {...} }`.
                        - Recursos únicos usam `{ "data": {...} }`.
                        - Erros usam `{ "error": { "code", "message", "path" } }`.
                        - Autenticação via Bearer JWT (access token de /auth/login ou /auth/register).
                        """.trimIndent()
                    )
            )
            // Aplica auth Bearer a todas as operações; endpoints públicos (register/login/refresh)
            // simplesmente ignoram o header.
            .addSecurityItem(SecurityRequirement().addList(securitySchemeName))
            .components(
                Components().addSecuritySchemes(
                    securitySchemeName,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token JWT obtido em POST /api/v1/auth/login ou /register.")
                )
            )
    }
}
