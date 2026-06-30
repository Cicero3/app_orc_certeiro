package com.example.api

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Padrão SINGLETON de container (Testcontainers): os containers sobem UMA vez por JVM
 * (bloco init) e NÃO são derrubados por classe. Isso é essencial porque o Spring
 * reaproveita (cacheia) o contexto entre classes de teste — se o container fosse
 * derrubado ao fim de cada classe (@Testcontainers/@Container), o pool do contexto
 * cacheado apontaria para um container morto e toda query falharia (500/timeout).
 * O Ryuk do Testcontainers limpa os containers no encerramento da JVM.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractIntegrationTest {
    companion object {
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test")

        @JvmStatic
        val redis: GenericContainer<*> = GenericContainer("redis:7-alpine")
            .withExposedPorts(6379)

        init {
            postgres.start()
            redis.start()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.flyway.enabled") { true }
            // Inclui db/seed para os testes terem o conteúdo do Módulo 1 (como em dev).
            registry.add("spring.flyway.locations") { "classpath:db/migration,classpath:db/seed" }
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.firstMappedPort }
            registry.add("jwt.secret") { "test-secret-that-is-at-least-256-bits-long-xxxxxxxxxxxxxxx" }
            registry.add("jwt.expiration-ms") { 3600000L }
            registry.add("jwt.issuer") { "english-app-test" }
            // Desliga rate limiting nos ITs (vários register/login do mesmo IP estourariam o limite).
            registry.add("security.rate-limit.auth.enabled") { false }
            // Cinto e suspensório: mesmo se o flag acima não pegasse, capacidade altíssima não limita.
            registry.add("security.rate-limit.auth.capacity") { 1_000_000 }
        }
    }
}
