import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    kotlin("plugin.jpa") version "1.9.24"
}

group = "com.example.api"
version = "0.0.1-SNAPSHOT"

// Sobe o Testcontainers acima do gerenciado pelo Spring Boot 3.3 (1.19.8 -> docker-java 3.3.6),
// cujo docker-java é incompatível com o proxy de named-pipe do Docker Desktop recente (HTTP 400 em /info).
// 1.20.6 ainda falhava contra o engine do Docker Desktop 29 (API 1.54); 1.21.x traz docker-java
// novo o suficiente para negociar a API e completar o handshake do named pipe.
extra["testcontainers.version"] = "1.21.3"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // ===== Spring Boot Starters =====
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    
    // ===== Database =====
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    
    // ===== Kotlin =====
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // ===== JSONB (Hibernate 6.3 / Spring Boot 3.3) =====
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.8.3")

    // ===== Rate limiting (login/register brute-force) =====
    implementation("com.bucket4j:bucket4j-core:8.7.0")
    
    // ===== JWT (JJWT 0.12.x) =====
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    
    // ===== API Documentation =====
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
    
    // ===== Testes =====
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Permite apontar o Testcontainers para um daemon específico em ambientes onde a
    // autodetecção falha (ex.: Docker Desktop no Windows): ./gradlew test -PdockerHost=tcp://localhost:2375
    ((project.findProperty("dockerHost") as String?) ?: System.getenv("DOCKER_HOST"))?.let {
        environment("DOCKER_HOST", it)
    }
    // Imprime a mensagem e o stack completo das falhas no console (essencial para CI:
    // o reporter padrão omite "expected:<201> but was:<...>").
    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        // Mostra o log do app (Logback) no console: necessário para ver a stack do 500
        // que o GlobalExceptionHandler registra no servidor durante o teste.
        showStandardStreams = true
    }
}

// Configuração JPA (para Kotlin data classes funcionarem com Hibernate)
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}