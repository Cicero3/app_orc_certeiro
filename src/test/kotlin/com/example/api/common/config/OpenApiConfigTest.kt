package com.example.api.common.config

import io.swagger.v3.oas.models.security.SecurityScheme
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpenApiConfigTest {

    private val openApi = OpenApiConfig().customOpenAPI()

    @Test
    fun `expoe info da API`() {
        assertThat(openApi.info.title).isEqualTo("Spring Kotlin API Starter")
        assertThat(openApi.info.version).isEqualTo("v1")
    }

    @Test
    fun `declara o esquema de auth bearer JWT`() {
        val scheme = openApi.components.securitySchemes["bearer-jwt"]
        assertThat(scheme).isNotNull
        assertThat(scheme!!.type).isEqualTo(SecurityScheme.Type.HTTP)
        assertThat(scheme.scheme).isEqualTo("bearer")
        assertThat(scheme.bearerFormat).isEqualTo("JWT")
    }

    @Test
    fun `aplica o requisito de seguranca globalmente`() {
        assertThat(openApi.security).anyMatch { it.containsKey("bearer-jwt") }
    }
}
