FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src

# Dá permissão de execução ao gradlew
RUN chmod +x ./gradlew

# Faz o build do projeto, gerando o jar executável
RUN ./gradlew bootJar --no-daemon

# Segundo estágio: a imagem final bem mais leve
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copia apenas o jar final da etapa de build
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
