# Spring Kotlin API Starter

Esqueleto **production-minded** para APIs em **Kotlin + Spring Boot 3.3**, extraído de um
projeto real (CI verde). Já vem com autenticação completa, segurança, convenções de API,
arquitetura modular, testes e CI — só falta o **seu domínio**.

> Pacote base: `com.example.api`. Renomeie para o seu (veja "Primeiros passos").

## O que já vem pronto

- **Auth & sessão**: JWT access curto + refresh **opaco no Redis** (rotação/uso-único),
  refresh em **cookie httpOnly**, denylist de access por `jti` (logout imediato), RBAC por papel.
- **Segurança**: rate limiting (Bucket4j) no login/registro, BCrypt (cost 12), password policy
  (NIST: tamanho mínimo + blocklist), security headers (HSTS/CSP/frame/referrer), CORS
  configurável fail-closed.
- **Convenções de API**: envelope `{ data, meta }`, erros `{ error: { code, message, path } }`,
  `GlobalExceptionHandler`, paginação, **OpenAPI** (`/api-docs`) + pipeline de **client TS**
  (`docs/frontend-api-client.md`).
- **Arquitetura**: monólito modular (`api / application / domain / infrastructure`) com
  **fronteiras entre features via portas em `common`** — exemplo vivo: `LearnerProfilePort`
  (em `common/learner`) implementado por um adapter em `auth`.
- **Persistência**: PostgreSQL + Flyway (migrations separadas de seeds), UUIDs, extensão JSONB.
- **Compliance (LGPD) de exemplo**: consentimento no registro + direitos do titular
  (acesso/portabilidade/exclusão por anonimização).
- **Testes & CI**: unit com **MockK**, integração com **Testcontainers** (padrão singleton),
  GitHub Actions (`.github/workflows/ci.yml`).
- **Deploy**: profile `prod`, TLS terminado no reverse proxy (`deploy/Caddyfile` +
  `docker-compose.tls.yml`).

## Domínio de exemplo (adapte/remova)

`auth/domain/UserProfile` traz campos de exemplo (`currentLevel`, `totalXp`, `streakDays`)
e o par `LearnerProfilePort`/`LearnerProfileAdapter` demonstra o padrão de portas. Troque
por campos do seu domínio. As regras do projeto estão em [CLAUDE.md](CLAUDE.md) (template —
substitua os `[placeholders]`); requisitos em [REQUIREMENTS.md](REQUIREMENTS.md).

## Primeiros passos

1. **Renomear o pacote** `com.example.api` para o seu (IDE refactor, ou buscar/substituir
   `com.example.api` + mover os diretórios `com/example/api`). Ajuste `group` no
   `build.gradle.kts` e `rootProject.name` no `settings.gradle.kts`.
2. **Subir dependências**: `docker-compose up -d` (Postgres + Redis).
3. **Rodar**: `./gradlew bootRun` (Swagger em `/swagger-ui.html`).
4. **Configurar segredos**: `JWT_SECRET` (≥256 bits) e, em produção, `CORS_ALLOWED_ORIGINS`.

## Comandos

```bash
./gradlew test --tests "*Test"   # unit (sem Docker)
./gradlew test                   # tudo (integração precisa de Docker)
./gradlew build -x test          # JAR
```

## Armadilha conhecida (toolchain)

Docker Desktop recente (engine 29) pode quebrar o handshake do Testcontainers com
`docker-java` antigo — por isso `testcontainers.version` está fixado mais alto no
`build.gradle.kts`. Os testes de integração rodam de forma confiável no **CI (Linux)**.
Detalhes e mais armadilhas: seção "Armadilhas Conhecidas" do `CLAUDE.md`.
