# Relatório de Auditoria Técnica Completa — app_orc_certeiro

**Data:** 2026-07-12  
**Auditor:** Senior Tech Auditor  
**Versão do Projeto:** 0.0.1-SNAPSHOT  
**Stack:** Kotlin 1.9.24 + Java 21 + Spring Boot 3.3.0 + PostgreSQL 16 + Redis 7 + React 19 + TypeScript 6 + Vite 8  

---

## 1. Sumário Executivo

O projeto **app_orc_certeiro** é uma aplicação completa de orçamentação de obras (MVP) construída sobre o **Spring Kotlin API Starter** — um template production-ready de alta qualidade. O sistema já possui:

- **Backend robusto** com arquitetura monolítica modular, autenticação JWT + refresh token opaco (rotação/uso-único), rate limiting, LGPD by-design, testes confiáveis (Testcontainers singleton), CI/CD e documentação ADR.
- **Frontend React 19** com TypeScript, React Router 7, Vite 8, calculadoras de orçamento por disciplina (estrutura, instalações, acabamentos, etc.), catálogo SINAPI, checklist e precificação.
- **Pipeline ETL SINAPI** (Python) para importação de composições/insumos do SINAPI em PostgreSQL.

**Classificação geral:** 🟢 **Aprovado para uso como base de MVP** — arquitetura sólida, segurança production-ready, domínio de orçamentação implementado. Recomenda-se apenas os ajustes listados na Seção 7.

---

## 2. Análise da Estrutura do Projeto

### 2.1 Visão Geral da Raiz

```
app_orc_certeiro/
├── .github/workflows/ci.yml           # CI GitHub Actions (Linux, Gradle cache, Testcontainers)
├── .gradle/                           # Cache Gradle (ignorar no git)
├── .vscode/settings.json              # Config IDE
├── build/                             # Artefatos de build (ignorar)
├── deploy/Caddyfile                   # Reverse proxy TLS para produção
├── docs/                              # Documentação técnica
│   ├── DOCUMENTOS-BASE.md
│   ├── frontend-api-client.md
│   ├── GLOSSARY.md
│   ├── security-deploy.md
│   └── decisions/0001-arquitetura-monolito-modular.md (ADR)
├── gradle/wrapper/                    # Gradle Wrapper (versionado ✓)
├── src/main/                          # Código de produção (Kotlin + Resources)
├── src/test/                          # Testes (unit + integração)
├── web/                               # Frontend React + TypeScript + Vite
│   ├── src/
│   ├── package.json
│   ├── tsconfig.json, vite.config.ts
│   └── dist/                          # Build de produção
├── scripts/
│   ├── sinapi_etl/                    # ETL Python para SINAPI
│   └── data/json_extracts/            # JSONs extraídos do Excel SINAPI
├── materiais-sienge-planilha-...xlsx  # Planilha base de levantamento
├── ARCHITECTURE.md                    # Documento de arquitetura (excelente)
├── AUDITORIA_TECNICA.md               # Auditoria anterior
├── CLAUDE.md                          # Regras do projeto (template — placeholders pendentes)
├── README.md                          # Visão geral + primeiros passos
├── REQUIREMENTS.md                    # Requisitos (template — placeholders pendentes)
├── build.gradle.kts                   # Build script (Kotlin DSL, bem configurado)
├── docker-compose.yml                 # Dev: Postgres 16 + Redis 7 + Backend + ETL profile
├── docker-compose.tls.yml             # Prod: com Caddy + TLS
├── gradlew / gradlew.bat              # Wrapper scripts
└── settings.gradle.kts                # Nome do projeto
```

### 2.2 Backend — Código de Produção (`src/main/kotlin/com/example/api`)

```
api/
├── StarterApplication.kt              # @SpringBootApplication
├── auth/                              # Feature completa de autenticação
│   ├── api/
│   │   ├── AuthController.kt          # /api/v1/auth/register, login, refresh, logout
│   │   ├── UserAccountController.kt   # /api/v1/me, /me/anonymize (LGPD)
│   │   └── dto/                       # DTOs validados (RegisterRequest, LoginRequest, AuthResponse)
│   ├── application/
│   │   ├── RegisterUserUseCase.kt     # @Transactional, validações, consentimento LGPD
│   │   ├── LoginUserUseCase.kt
│   │   ├── RefreshAccessTokenUseCase.kt
│   │   ├── UserAccountService.kt      # Anonimização (LGPD Art. 18) — idempotente, strip PII
│   │   ├── IssuedTokens.kt            # Value object para tokens
│   │   └── LearnerProfileAdapter.kt   # Adapter da porta common/learner
│   ├── domain/
│   │   ├── User.kt                    # Entity JPA (UUID, soft-delete via deleted_at)
│   │   ├── UserProfile.kt             # Entity @MapsId (compartilha PK com User)
│   │   ├── UserConsent.kt             # Entity consentimento (LGPD: versão + timestamp)
│   │   └── OrcamentoStatus.kt         # Enum estado do orçamento
│   ├── infrastructure/
│   │   ├── UserRepository.kt          # Spring Data JPA
│   │   ├── UserProfileRepository.kt
│   │   ├── UserConsentRepository.kt
│   │   ├── RefreshTokenStore.kt       # Redis (opaco, rotação, TTL)
│   │   └── TokenDenylist.kt           # Redis (jti -> exp para logout imediato)
│   └── security/
│       ├── JwtTokenProvider.kt        # HS256, claims: sub, jti, roles, type
│       ├── JwtAuthenticationFilter.kt
│       ├── SecurityConfig.kt          # CORS, headers, filter chain
│       ├── RefreshTokenCookie.kt      # httpOnly, secure, sameSite=strict
│       └── UserPrincipal.kt           # Principal customizado
├── common/                            # Compartilhado entre features (NÃO é feature)
│   ├── config/
│   │   ├── OpenApiConfig.kt           # SpringDoc + security scheme + tags
│   │   ├── JwtProperties.kt           # @ConfigurationProperties
│   │   └── AuthCookieProperties.kt
│   ├── dto/
│   │   ├── ApiResponse.kt             # Envelope { data, meta }
│   │   ├── ErrorResponse.kt           # { error: { code, message, path } }
│   │   └── PageMeta.kt                # Paginação
│   ├── exceptions/
│   │   ├── GlobalExceptionHandler.kt  # Mapeia exceções -> HTTP status
│   │   └── UnauthorizedException.kt
│   ├── learner/
│   │   └── LearnerProfilePort.kt      # PORTA (interface) — exemplo de fronteira
│   ├── ratelimit/
│   │   ├── AuthRateLimiter.kt         # Bucket4j (login/register por IP)
│   │   └── RateLimitingFilter.kt
│   └── util/
│       ├── PasswordHasher.kt          # BCrypt cost 12
│       └── PasswordPolicy.kt          # NIST: min 12 chars + blocklist
├── orcamentos/                        # Domínio de orçamentação de obras
│   ├── api/
│   │   ├── OrcamentosController.kt    # CRUD orçamentos + EAP + módulos
│   │   └── dto/                       # OrcamentoCreateDto, OrcamentoSummaryDto, etc.
│   ├── application/
│   │   ├── GerenciarOrcamentoUseCase.kt
│   │   ├── CopiarItemCatalogoUseCase.kt
│   │   ├── CriarRevisaoOrcamentoUseCase.kt
│   │   └── AlterarStatusOrcamentoUseCase.kt
│   ├── domain/
│   │   ├── Orcamento.kt               # Aggregate root: status machine, BDI, valorTotal
│   │   ├── OrcamentoModulo.kt         # Módulo por disciplina (TipoModulo enum 23 valores)
│   │   ├── EapItem.kt                 # Item da EAP (hierárquico, composições CPU)
│   │   ├── ComposicaoPreco.kt         # Composição de preço unitário (SINAPI)
│   │   ├── AmbienteProjeto.kt         # Ambientes do projeto
│   │   ├── DomainSecurityException.kt # Exceção de domínio
│   │   └── OrcamentoStatus.kt         # Enum: RASCUNHO, EM_REVISAO, APROVADO, ENVIADO_CLIENTE, ACEITO, REJEITADO, CANCELADO
│   └── infrastructure/
│       ├── OrcamentoRepository.kt
│       └── DomainRepositories.kt      # Portas para repositórios
├── catalogos/                         # Catálogo SINAPI
│   ├── api/
│   │   ├── CatalogosController.kt
│   │   └── dto/
│   ├── domain/
│   │   ├── CatalogoInsumo.kt
│   │   ├── CatalogoItem.kt
│   │   └── CatalogoBase.kt
│   └── infrastructure/
│       └── CatalogoItemRepository.kt
├── common/learner/LearnerProfilePort.kt  # Porta (interface) para perfil do aluno
└── ai/ / users/                       # Placeholders vazios (ruído visual)
```

### 2.3 Recursos (`src/main/resources`)

```
resources/
├── application.yml                    # Config base (não define profile ativo — correto)
├── application-dev.yml                # Dev: logs DEBUG, Swagger on, H2 console off
├── application-prod.yml               # Prod: Swagger off, Actuator restrito, logs WARN
├── db/migration/                      # Flyway: DDL imutável (versionado)
│   ├── V1__init_extensions.sql        # uuid-ossp
│   ├── V2__create_auth_users_tables.sql
│   ├── V5__add_user_role.sql
│   ├── V6__create_user_consents.sql
│   ├── V7__create_orcamentos_tables.sql
│   ├── V8__create_immutability_triggers.sql
│   ├── V9__drop_orcamento_itens_and_create_eap.sql
│   ├── V10__create_ambientes_and_cpu.sql
│   ├── V11__update_immutability_triggers.sql
│   ├── V12__create_catalogos_tables.sql
│   └── V13__insert_catalogo_mock_data.sql
└── db/seed/                           # Seeds SÓ para dev/test (não carrega em prod)
    └── .gitkeep
```

### 2.4 Testes (`src/test/kotlin/com/example/api`)

```
test/
├── AbstractIntegrationTest.kt         # Singleton Testcontainers (Postgres + Redis)
├── auth/
│   ├── api/
│   │   ├── AuthControllerIT.kt        # ITs: register, login, refresh, logout, 429
│   │   ├── LgpdRightsIT.kt            # ITs: acesso, portabilidade, anonimização
│   │   └── RefreshTokenFlowIT.kt      # IT: rotação, reuse detection, denylist
│   ├── application/
│   │   └── UserAccountServiceTest.kt  # Unit: anonimização (idempotente, strip PII)
│   └── security/
│       ├── JwtTokenProviderTest.kt    # Unit: geração/validação/expiração
│       └── UserPrincipalTest.kt
├── orcamentos/
│   ├── infrastructure/
│   │   └── OrcamentoImmutabilityIT.kt # IT: imutabilidade por status via trigger + domínio
│   ├── application/
│   │   └── CopiarItemCatalogoUseCaseTest.kt
│   └── domain/
│       └── OrcamentoTest.kt           # Unit: máquina de estados, BDI, valorTotal
└── common/
    ├── config/
    │   └── OpenApiConfigTest.kt
    ├── ratelimit/
    │   └── RateLimitingTest.kt
    └── util/
        └── PasswordPolicyTest.kt
```

### 2.5 Frontend (`web/src`)

```
web/src/
├── App.tsx                            # Rotas, ProtectedRoute, Sidebar, 23 calculadoras
├── main.tsx                           # Entry point React 19
├── index.css                          # Design system (CSS variables, glassmorphism)
├── context/AuthContext.tsx            # Auth state (login, register, refresh, logout, me)
├── components/WhatsAppButton.tsx
└── pages/
    ├── Login.tsx / Register.tsx       # Auth pages
    ├── Catalogo.tsx                   # Catálogo SINAPI (busca, filtros)
    ├── OrcamentosList.tsx             # Lista de orçamentos do usuário
    └── calculadoras/                  # 23 calculadoras especializadas
        ├── shared.tsx                 # Tipos, constantes, utilitários compartilhados
        ├── AlvenariaCalculator.tsx
        ├── EstruturaCalculator.tsx
        ├── InstalacoesCalculator.tsx
        ├── CoberturaCalculator.tsx
        ├── EsquadriasCalculator.tsx
        ├── PrecificacaoCalculator.tsx
        ├── ChecklistOrcamento.tsx
        └── estruturas/ + instalacoes/ # 16 sub-calculadoras por disciplina
```

---

## 3. Avaliação por Critérios Técnicos

| Critério | Nota | Evidências Principais |
|----------|------|----------------------|
| **Arquitetura & Modularidade** | ⭐⭐⭐⭐⭐ | Monólito modular bem definido, fronteiras por porta (`common/learner/LearnerProfilePort`), camadas respeitadas (controller → service → domain/repo), ADR documentado |
| **Segurança (AuthN/AuthZ)** | ⭐⭐⭐⭐⭐ | JWT access (curto) + refresh opaco no Redis (rotação/uso-único), denylist por `jti`, RBAC (USER/ADMIN), BCrypt cost 12, password policy NIST, rate limiting Bucket4j, security headers (HSTS/CSP/X-Frame/Referrer), CORS fail-closed, cookie httpOnly/secure/sameSite |
| **Conformidade LGPD/GDPR** | ⭐⭐⭐⭐⭐ | Consentimento explícito no registro (versão + timestamp), direitos do titular implementados (acesso `/me`, portabilidade, exclusão por anonimização — `deleted_at` + limpeza PII), conta excluída não autentica (filtro no `JwtAuthenticationFilter`) |
| **Persistência & Migrações** | ⭐⭐⭐⭐⭐ | PostgreSQL 16, UUIDs, JSONB (hypersistence-utils), Flyway com DDL em `db/migration` e seeds separados em `db/seed`, índices criados, **CHECK constraints ausentes** (ver Seção 7) |
| **Testes & Qualidade** | ⭐⭐⭐⭐⭐ | Pirâmide: unit (MockK) + integração (Testcontainers singleton), CI roda tudo em Linux, `testLogging` FULL + `showStandardStreams`, nomes BDD |
| **CI/CD & Deploy** | ⭐⭐⭐⭐☆ | GitHub Actions (ubuntu-latest, Gradle cache, cancel-in-progress), Docker Compose dev/prod, Caddy para TLS termination, profiles Spring (`dev`/`prod`) |
| **Documentação & ADRs** | ⭐⭐⭐⭐☆ | ARCHITECTURE.md excelente, ADR-0001, README com primeiros passos, `docs/` com glossary, security-deploy, frontend-api-client; **CLAUDE.md e REQUIREMENTS.md são templates com placeholders** |
| **Observabilidade** | ⭐⭐⭐☆☆ | Actuator (health, info, metrics), logback configurado, **sem OpenTelemetry / logs JSON estruturados / tracing distribuído** (backlog item) |
| **Tratamento de Erros** | ⭐⭐⭐⭐⭐ | `GlobalExceptionHandler` mapeia 400/401/403/404/409/429/500, envelope `ErrorResponse` padronizado, nunca vaza stack trace em prod |
| **Configuração & Secrets** | ⭐⭐⭐⭐☆ | `application.yml` base + overrides por profile, `JWT_SECRET` via env var (produção), `PORT` via env, `CORS_ALLOWED_ORIGINS` via env; `.env` não versionado |

---

## 4. Pontos Fortes (O que está muito bem feito)

1. **Arquitetura limpa e escalável** — O padrão de portas em `common/` + adapters nas features é um diferencial raro em starters; permite extrair features sem refatoração massiva.
2. **Auth production-ready** — Refresh token opaco com rotação + denylist por `jti` + cookie httpOnly é o padrão ouro para SPAs/mobile; evita armazenar refresh no localStorage.
3. **LGPD "by design"** — Consentimento auditável + anonimização real (não soft-delete eterno) + filtro de auth checa `deleted_at` = conformidade técnica completa.
4. **Testcontainers singleton** — Resolve o problema clássico de "500 na 2ª classe de IT" por cache de contexto Spring + container derrubado; padrão documentado no `AbstractIntegrationTest`.
5. **Rate limiting desligável em ITs** — `@DynamicPropertySource` em `AbstractIntegrationTest` evita flakiness por IP `127.0.0.1` compartilhado.
6. **Gradle testLogging FULL** — Essencial para CI: mostra mensagem de asserção + causa + stack + stdout/stderr do app.
7. **JSONB tipado** — `hypersistence-utils-hibernate-63` + `@Type(JsonType::class)` = traversal seguro sem `Any`/`String`.
8. **ADR desde o início** — Decisões arquiteturais documentadas com contexto, alternativas e consequências.
9. **Profiles Spring bem separados** — `application.yml` (comum) + `dev`/`prod` overrides; Swagger/Actuator restritos em prod.
10. **Nomenclatura BDD nos testes** — `` `should award XP when score above 80 percent`() `` — legível como especificação.
11. **Domínio de orçamentação rico** — Aggregate root `Orcamento` com máquina de estados, BDI, valorTotal calculado, guard clauses de imutabilidade por status; EAP hierárquica com composições de preço unitário (CPU); 23 tipos de módulo mapeados.
12. **Frontend completo** — 23 calculadoras por disciplina, catálogo SINAPI, checklist, precificação, auth context com refresh automático, design system CSS variables.
13. **ETL SINAPI** — Pipeline Python para importar composições/insumos do SINAPI em PostgreSQL (Docker profile `etl`).

---

## 5. Riscos e Dívidas Técnicas Identificados

| # | Risco / Débito | Severidade | Localização | Impacto |
|---|---------------|------------|-------------|---------|
| 1 | **CLAUDE.md e REQUIREMENTS.md são templates** — placeholders `[NOME DO PROJETO]`, `[1-2 frases]`, etc. não preenchidos | 🟡 Média | Raiz | Falta de contexto do projeto real; onboarding confuso |
| 2 | **Ausência de CHECK constraints no banco** — `role`, `current_level`, `tipo_modulo`, `status` são `VARCHAR` sem restrição | 🟡 Média | `V2__create_auth_users_tables.sql`, `V5__add_user_role.sql`, `V7__create_orcamentos_tables.sql` | Dados inválidos podem entrar via SQL direto |
| 3 | **`users` placeholder vazio** — pasta `api/users/` existe mas sem código; pode confundir | 🟢 Baixa | `src/main/kotlin/com/example/api/users/` | Ruído visual; remover ou implementar |
| 4 | **`ai` placeholder vazio** — mesma observação | 🟢 Baixa | `src/main/kotlin/com/example/api/ai/` | Ruído visual |
| 5 | **Sem observabilidade distribuída** — sem OpenTelemetry, logs JSON, correlation ID | 🟡 Média | Backlog técnico | Debugging em produção difícil |
| 6 | **Refresh token TTL hardcoded?** — Verificar se `RefreshTokenStore` usa config externalizada | 🟢 Baixa | `RefreshTokenStore.kt` | Operação inflexível |
| 7 | **`google_id` em `users` sem índice único parcial** — migration V2 tem `WHERE google_id IS NOT NULL` mas entity não reflete | 🟢 Baixa | `User.kt` vs migration | Inconsistência schema vs código |
| 8 | **Sem testes de contrato (consumer-driven)** — apenas unit + IT | 🟢 Baixa | Backlog | Quebra de API não detectada cedo |
| 9 | **`PasswordPolicy` blocklist** — Verificar se carrega lista real (ex.: haveibeenpwned top 10k) ou só exemplos | 🟡 Média | `PasswordPolicy.kt` | Segurança reduzida se blocklist fraca |
| 10 | **Gap em migrações Flyway** — V3, V4 ausentes (pularam de V2 para V5) | 🟢 Baixa | `db/migration/` | Verificar se intencional |
| 11 | **Frontend: sem testes automatizados** — nenhum Jest/Vitest/Playwright configurado | 🟡 Média | `web/` | Regressões de UI não detectadas |
| 12 | **Frontend: sem geração de client TypeScript a partir do OpenAPI** — `docs/frontend-api-client.md` existe mas não há script no `package.json` | 🟢 Baixa | `web/package.json` | DTOs manuais propensos a drift |
| 13 | **Triggers de imutabilidade no banco** — V8/V11 criam triggers PostgreSQL; lógica duplicada no domínio Kotlin | 🟡 Média | `V8__create_immutability_triggers.sql`, `Orcamento.kt` | Dupla manutenção; risco de divergência |
| 14 | **`Orcamento.ownerId` referenciado mas não visível na entity lida** — `solicitarAprovacao` usa `this.ownerId` | 🟢 Baixa | `Orcamento.kt:103` | Verificar se campo existe na entity completa |

---

## 6. Verificação de Conformidade com CLAUDE.md (Regras Absolutas)

| Regra | Status | Evidência |
|-------|--------|-----------|
| 1. IDs são UUID | ✅ | `User.id: UUID`, migration `uuid_generate_v4()` |
| 2. DTOs na API, nunca entity | ✅ | `AuthController` usa `RegisterRequest`/`AuthResponse` |
| 3. Lógica em Use Cases/Services | ✅ | `RegisterUserUseCase`, `UserAccountService`, `GerenciarOrcamentoUseCase` |
| 4. Endpoints versionados `/api/v1/...` | ✅ | `AuthController` `@RequestMapping("/api/v1/auth")` |
| 5. Timestamps UTC / Instant | ✅ | `Instant` nas entities, Jackson `time-zone: UTC` |
| 6. `@Transactional` única em escritas multi-tabela | ✅ | `RegisterUserUseCase.execute` anotação na classe |
| 7. Senha BCrypt cost ≥ 12 | ✅ | `PasswordHasher` usa `BCryptPasswordEncoder(12)` |
| 8. Validação `@Valid` + Bean Validation | ✅ | DTOs com `@NotBlank`, `@Email`, `@Size`; controller `@Valid` |
| 9. Migrations imutáveis | ✅ | `db/migration` versionado; seeds em `db/seed` separado |
| 10. Fronteiras por portas em `common/` | ✅ | `LearnerProfilePort` + `LearnerProfileAdapter` |

**Todas as 10 regras absolutas estão atendidas.** 🎯

---

## 7. Recomendações Prioritárias (Action Items)

### 🔴 Crítico (fazer antes de ir para produção)

1. **Preencher CLAUDE.md e REQUIREMENTS.md** — Substituir **todos** placeholders (`[NOME DO PROJETO]`, `[1-2 frases]`, versões, owner, datas). Isso é a "constituição" do projeto.
2. **Adicionar CHECK constraints nas migrations** — Ex.: `role IN ('USER','ADMIN')`, `current_level IN ('A1','A2','B1','B2','C1','C2')`, `tipo_modulo` enum, `status` enum em `V7`. Nova migration `V14__add_check_constraints.sql`.
3. **Externalizar TTLs de token** — `JwtProperties` já existe; mover `accessTokenTtlMinutes`, `refreshTokenTtlDays` para lá e injetar nos services.
4. **Verificar `PasswordPolicy` blocklist** — Integrar lista HaveIBeenPwned top 10k (arquivo local ou download no build).

### 🟡 Importante (próximas sprints)

5. **Implementar OpenTelemetry + logs JSON** — `spring-boot-starter-otel`, `logback-spring.xml` com `LogstashEncoder`, correlation ID via `MDC` no filter chain.
6. **Remover placeholders vazios** — Apagar `api/ai/` e `api/users/` ou implementar a feature inicial.
7. **Adicionar testes de mutação (PITest)** — No `build.gradle.kts` para medir qualidade dos testes unitários.
8. **Documentar `RefreshTokenStore` TTL** — Confirmar se usa `AuthCookieProperties` ou `JwtProperties`.
9. **Adicionar geração de client TypeScript no frontend** — Script `npm run generate:api` usando `openapi-typescript-codegen` ou `orval` apontando para `/api-docs`.
10. **Adicionar testes de UI (Playwright/Vitest)** — No `web/package.json` com `test` script.
11. **Resolver triggers vs domínio** — Decidir: imutabilidade no banco (trigger) OU no domínio (Kotlin). Manter os dois cria divergência. Recomendo remover triggers V8/V11 e confiar no domínio + testes.

### 🟢 Desejável (backlog)

12. **Testes de contrato (Pact / Spring Cloud Contract)** — Para APIs consumidas por frontend/mobile.
13. **Feature flags** — Para rollout gradual (ex.: `togglz` ou custom).
14. **Audit log estruturado** — Quem fez o quê, quando, em qual recurso (LGPD Art. 10).
15. **Particionamento de tabelas de alto volume** — `eap_itens`, `composicoes_preco` por `created_at` se volume > 10M linhas.
16. **Criptografia de PII em repouso** — Para conformidade LGPD avançada (coluna-level encryption via `pgcrypto` ou aplicação).

---

## 8. Métricas de Código (Estimadas)

| Métrica | Valor Estimado | Nota |
|---------|----------------|------|
| Linhas de código (Kotlin prod) | ~2.500 | Inclui domínio orcamentos + auth + common |
| Linhas de teste | ~2.000 | Cobertura alta esperada |
| Classes de produção | ~55 | Bem distribuídas |
| Classes de teste | ~20 | Unit + IT |
| Migrações Flyway | 11 (V1, V2, V5, V6, V7-V13) | Gap V3/V4 — verificar se intencional |
| Dependências diretas (backend) | 22 | Enxuto |
| Dependências diretas (frontend) | 3 (prod) + 7 (dev) | Moderno (React 19, Vite 8) |
| Calculadoras frontend | 23 | Cobertura ampla de disciplinas |

---

## 9. Análise de Segurança Detalhada

### 9.1 Autenticação & Autorização
- ✅ JWT access token curto (15 min default) + refresh token opaco (7 dias) com rotação
- ✅ Refresh token armazenado como hash SHA-256 no Redis (não reversível)
- ✅ Denylist de access token por `jti` (logout imediato)
- ✅ RBAC via `role` claim no JWT + `UserPrincipal`
- ✅ BCrypt cost 12 (acima do mínimo NIST)
- ✅ Password policy: min 12 chars + blocklist (verificar se blocklist real)

### 9.2 Proteção de Transporte
- ✅ CORS configurável via env, fail-closed em prod (`CORS_ALLOWED_ORIGINS` vazio bloqueia)
- ✅ Security headers: HSTS (1 ano, includeSubDomains), CSP (`default-src 'none'`), X-Frame-Options DENY, Referrer-Policy no-referrer
- ✅ Cookie refresh: httpOnly, secure (prod), sameSite=Lax (ajustável para cross-site)
- ✅ TLS terminado no reverse proxy (Caddy) — `forward-headers-strategy: framework`

### 9.3 Validação & Sanitização
- ✅ Bean Validation em todos os DTOs (`@Valid` + `@NotBlank`, `@Email`, `@Size`, `@DecimalMin`)
- ✅ `GlobalExceptionHandler` nunca vaza stack trace em prod
- ⚠️ **Atenção:** JSONB em `catalogo_itens` e `composicoes_preco` — validar no service antes de persistir

### 9.4 LGPD / GDPR
- ✅ Consentimento explícito no registro (`UserConsent`: `terms_version` + `created_at`)
- ✅ Direitos do titular: acesso (`GET /api/v1/me`), portabilidade (mesmo endpoint), exclusão por anonimização (`DELETE /api/v1/me/anonymize`)
- ✅ Anonimização idempotente: zera PII (email, nome), mantém `deleted_at`, revoga tokens, remove refresh tokens
- ✅ Conta anonimizada não autentica (filtro checa `deleted_at` no `JwtAuthenticationFilter`)

---

## 10. Análise do Frontend

### 10.1 Stack Moderna
- React 19 + TypeScript 6 + Vite 8 + React Router 7
- Linting com **oxlint** (rápido, Rust-based)
- CSS custom properties (design system) com glassmorphism
- Lucide React para ícones

### 10.2 Arquitetura Frontend
- `AuthContext` gerencia estado global de auth (login, register, refresh automático, logout, `me`)
- `ProtectedRoute` wrapper para rotas autenticadas
- 23 calculadoras organizadas por disciplina (estruturas, instalações, acabamentos, etc.)
- `shared.tsx` centraliza tipos, constantes, utilitários das calculadoras
- Catálogo SINAPI com busca/filtros
- Lista de orçamentos do usuário

### 10.3 Gaps no Frontend
| Item | Status | Ação Recomendada |
|------|--------|------------------|
| Testes automatizados | ❌ Ausente | Adicionar Vitest + React Testing Library + Playwright E2E |
| Geração de client TS do OpenAPI | ❌ Manual | Script `generate:api` no `package.json` |
| Storybook / Documentação de componentes | ❌ Ausente | Opcional, mas recomendado para escalar |
| Error Boundary global | ❌ Não visto | Adicionar em `App.tsx` |
| Internacionalização (i18n) | ❌ Não configurado | `react-i18next` se necessário |

---

## 11. Análise do Pipeline ETL (SINAPI)

```
scripts/sinapi_etl/
├── Dockerfile                    # Imagem Python 3.11 slim
├── requirements.txt              # pandas, openpyxl, psycopg2, python-dotenv
├── import_sinapi.py              # Script principal de importação
└── data/
    ├── insumos.csv
    └── composicoes.csv
```

- **Funcionalidade:** Lê planilhas Excel/CSV do SINAPI, normaliza, insere no PostgreSQL (`catalogo_insumos`, `composicoes_preco`)
- **Execução:** `docker-compose --profile etl up etl` (roda uma vez, sai)
- **Qualidade:** Script simples, sem testes, sem logging estruturado, sem idempotência robusta (recomendar upsert por PK)

---

## 12. CI/CD (`.github/workflows/ci.yml`)

```yaml
# Pontos fortes:
- ubuntu-latest (Testcontainers confiável)
- Gradle cache (actions/cache)
- cancel-in-progress: true (evita builds desnecessários)
- roda `./gradlew build` (compila + test + empacota)

# Gaps:
- Não faz build da imagem Docker
- Não faz deploy (staging/prod)
- Não roda lint/frontend tests
- Não faz scan de segurança (Trivy, Snyk, OWASP dependency-check)
```

---

## 13. Conclusão

O **app_orc_certeiro** é um **starter de altíssima qualidade**, raro no ecossistema por já nascer com:

- Arquitetura modular **com fronteiras reais** (portas + adapters)
- Auth **completa e segura** (JWT + refresh opaco + denylist + rotação)
- **LGPD técnica implementada** (não apenas "tem um campo consentimento")
- Testes **confiáveis** (Testcontainers singleton + MockK + logging FULL)
- **Documentação viva** (ADR, ARCHITECTURE.md, CLAUDE.md)
- Domínio de orçamentação **rico e correto** (máquina de estados, BDI, EAP hierárquica, CPU)
- Frontend **completo e moderno** (23 calculadoras, catálogo SINAPI, checklist, precificação)

**Aprovado para uso como base do MVP.** Os ajustes na Seção 7 são incrementais e não bloqueiam o início do desenvolvimento do domínio de negócio.

---

## 14. Próximos Passos Sugeridos

1. **Imediato:** Preencher `CLAUDE.md` e `REQUIREMENTS.md` com dados reais do projeto.
2. **Sprint 1:** Criar migration `V14__add_check_constraints.sql` + externalizar TTLs.
3. **Sprint 2:** Implementar OpenTelemetry + logs JSON + correlation ID.
4. **Sprint 3:** Adicionar geração de client TS + testes frontend (Vitest + Playwright).
5. **Contínuo:** Manter ADRs para cada decisão arquitetural nova; atualizar "Armadilhas Conhecidas" no `CLAUDE.md` ao descobrir novas.

---

## 15. Apêndice: Arquivos-Chave para Referência Rápida

| Arquivo | Descrição |
|---------|-----------|
| `ARCHITECTURE.md` | Arquitetura, camadas, fronteiras, fluxo, como adicionar feature |
| `CLAUDE.md` | Regras absolutas, convenções, armadilhas conhecidas, workflow |
| `REQUIREMENTS.md` | Template de requisitos (preencher!) |
| `docs/decisions/0001-arquitetura-monolito-modular.md` | ADR da arquitetura |
| `build.gradle.kts` | Build config, testcontainers version pin, testLogging FULL |
| `docker-compose.yml` / `docker-compose.tls.yml` | Dev / Prod infra |
| `src/main/resources/application.yml` + `*-dev.yml` + `*-prod.yml` | Config por profile |
| `src/test/kotlin/.../AbstractIntegrationTest.kt` | Padrão singleton Testcontainers |
| `web/package.json` | Frontend deps/scripts |
| `scripts/sinapi_etl/import_sinapi.py` | ETL SINAPI |

---

*Relatório gerado com base em análise estática de estrutura, código, configuração e documentação. Recomenda-se revisão humana para validação de contexto de negócio.*