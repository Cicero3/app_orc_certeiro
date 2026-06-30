# Project Instructions for AI Assistants

> Este arquivo é lido automaticamente por assistentes de IA (Claude Code, etc.).
> É a **fonte única de verdade**. Leia-o inteiro antes de gerar, alterar ou revisar código.
> Template para projetos **Kotlin + Spring Boot**. Substitua os `[placeholders]` e
> mantenha a seção "Armadilhas Conhecidas" viva — ela é o que mais economiza tempo.

---

## 1. Visão Geral

- **Nome:** [NOME DO PROJETO]
- **Descrição:** [1-2 frases sobre o que faz]
- **Estágio:** [MVP / Alpha / Beta / Produção]
- **Stack principal:** [Ex: Kotlin 1.9 + Java 21 + Spring Boot 3.3 + PostgreSQL 16 + Redis 7]
- **Docs complementares:** `requirements.md` (escopo), `design.md` (arquitetura/DDL), `SKILLS.md` (procedimentos), `docs/adr/` (decisões).

---

## 2. Stack (NÃO sugerir alternativas sem ADR)

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Linguagem | Kotlin (+ Java só p/ integrações) | [1.9 / 21] |
| Framework | Spring Boot | [3.3.x] |
| ORM | Spring Data JPA + Hibernate | [6.x] |
| Migrações | Flyway | [10.x] |
| Banco | PostgreSQL (JSONB) | [16] |
| Cache | Redis | [7.x] |
| Testes | JUnit 5 + **MockK** (não Mockito) + Testcontainers | - |
| Build | Gradle (Kotlin DSL) | [8.x] |
| Auth | Spring Security + JWT | - |

**Proibido sem justificativa (ADR):** trocar o banco, ORM, ou framework; Mockito (usar MockK);
logar PII; commitar secrets.

---

## 3. Regras Absolutas (NUNCA quebrar)

1. **IDs são UUID** (`java.util.UUID`), nunca `Long`/`int` para domínio.
2. **DTOs na API** — nunca expor entity JPA direto no controller.
3. **Lógica de negócio em Use Cases/Services**, nunca em controllers ou repositories.
4. **Endpoints versionados** `/api/v1/...`, recursos por UUID, paginação obrigatória em listas.
5. **Timestamps em UTC** (`TIMESTAMPTZ` / `Instant`), ISO 8601 na API.
6. **Transação única** (`@Transactional`) quando a operação escreve em múltiplas tabelas.
7. **Senha sempre hasheada** (BCrypt cost ≥ 12 ou Argon2). Hash ≠ criptografia: é one-way, correto para senha.
8. **Validar toda entrada** com Bean Validation (`@Valid` + `@NotBlank`/`@Email`/`@AssertTrue`...).
9. **Migrations aplicadas são imutáveis** — alterações = nova migration (ver Armadilha #1).
10. **Fronteiras entre features:** uma feature não importa classes de outra. Use **portas (interfaces) em `common/`** + adapters, ou eventos de domínio.

---

## 4. Estrutura de Pastas (Monolito Modular)

```
src/main/kotlin/com/[empresa]/[projeto]/
├── [feature]/
│   ├── api/            # Controllers + DTOs
│   ├── application/    # Use Cases / Services + adapters de portas
│   ├── domain/         # Entities, value objects, lógica pura (ex.: Grader)
│   └── infrastructure/ # Repositories, clients
├── common/             # Compartilhado: portas entre features, DTOs, exceptions, config, security
src/main/resources/
├── db/migration/       # Flyway: schema (roda em TODOS os ambientes)
├── db/seed/            # Flyway: dados de exemplo (carregar SÓ em dev/test — ver Armadilha #8)
└── application*.yml
src/test/kotlin/        # Espelha main; *Test = unitário, *IT = integração
```

---

## 5. Convenções de Código

- **Constructor injection** sempre (nunca `@Autowired` em field).
- `val` por padrão em services/domain; `var` só com justificativa (acumuladores, campos mutáveis de entity).
- **Null-safety:** tipos não-nuláveis + early return + `orElseThrow`. Evitar `!!` em produção.
- **DTOs = `data class`** com validação. **Entities JPA = `class`** (não `data class` — equals/hashCode com lazy).
- **Logs:** SLF4J com parâmetros (`log.info("x={}", x)`), nunca concatenar nem logar PII/senha/token.
- **Exceptions:** específicas + `GlobalExceptionHandler` (mapeie 400/404/409/429/500). Nunca `catch (Exception)` genérico que engole a causa.
- **JSONB (Postgres + Kotlin):** dependência `hypersistence-utils-hibernate-6x` + `@Type(JsonType::class)`; mapear como `JsonNode` para traversal fácil (ver Armadilha #9).

---

## 6. Banco de Dados

- Tabelas no plural, colunas `snake_case`. FKs com `ON DELETE CASCADE/RESTRICT` explícito.
- `created_at`/`updated_at` em toda tabela. CHECK constraints para enums (`level IN (...)`).
- **JSONB** para payloads flexíveis (não para o que precisa de FK/índice). Validar no service.
- **Particionamento** (tabelas de alto volume / séries temporais): `PARTITION BY RANGE (created_at)`;
  a PK precisa **incluir a chave de partição**; criar partições futuras por migration **antes** de virar o mês.
- **Migrations idempotentes** (`IF NOT EXISTS`, `CREATE OR REPLACE`); separe DDL de DML.

---

## 7. Testes

| Tipo | Foco | Ferramenta | Sufixo |
|------|------|-----------|--------|
| Unitário (maioria) | Use Cases, validadores, lógica pura | MockK | `*Test` |
| Integração | Controllers + DB real | Testcontainers | `*IT` |
| E2E | Fluxos críticos | [Playwright/REST] | - |

- Nomes em BDD: `` `should award XP when score above 80 percent`() ``.
- **Container singleton** no `AbstractIntegrationTest` (ver Armadilha #2).
- Em ITs, **desligar rate limiting** por propriedade (ver Armadilha #3).
- **Diagnóstico de CI:** configurar `testLogging` para `FULL` + `showStandardStreams` (ver Armadilha #4).
- Comando: `./gradlew test` (ou `build` para incluir empacotamento).

---

## 8. Segurança (não-negociável)

- BCrypt cost ≥ 12; JWT secret ≥ 256 bits **via env var** em prod.
- Política de senha (NIST): comprimento ≥ 12 + blocklist de comuns; **não** forçar composição.
- Rate limiting em login/register (Bucket4j). RBAC mínimo (ROLE_USER/ROLE_ADMIN) no token + `UserPrincipal`.
- Security headers (HSTS, CSP, X-Frame-Options, Referrer-Policy) + CORS explícito (fail-closed em prod).
- **HTTPS/TLS obrigatório em produção** (login sobre HTTP puro vaza credenciais). Geralmente no reverse proxy.
- **Swagger/Actuator** restritos/desabilitados em prod (por profile).
- **LGPD/GDPR:** consentimento explícito no cadastro (registro auditável: versão + timestamp);
  direitos do titular (acesso, exportação/portabilidade, exclusão por **anonimização** — não soft-delete eterno);
  conta excluída não autentica nem com token válido (checar `deleted_at` no filtro JWT).
  ⚠️ Conformidade plena tem parte **não-código** (política de privacidade, base legal, DPO, ANPD) — sinalizar ao dono.

---

## 9. Armadilhas Conhecidas (lições reais — leia antes de debugar)

> Atualize sempre que descobrir uma nova. Cada linha aqui já custou horas a alguém.

| # | Problema (sintoma) | Causa raiz | Solução |
|---|---|---|---|
| 1 | `Flyway checksum mismatch` / `migration not applied` no boot | Migration já aplicada foi **editada ou movida** (ex.: V999 mudou de pasta/conteúdo) | Nunca editar migration aplicada. Em DEV: `docker compose down -v` e suba do zero. Separar `db/migration` (schema) de `db/seed` desde o início. |
| 2 | **500 nos ITs da 2ª classe em diante** (`Connection refused`, porta diferente do container vivo); 1ª classe passa | `@Testcontainers/@Container` derruba o container ao fim de cada classe, mas o Spring **cacheia o contexto** entre classes → pool aponta p/ container morto | **Padrão singleton:** subir os containers **1x por JVM** em bloco `init {}` no companion, **sem** `@Container`/`@Testcontainers`. O Ryuk limpa no fim da JVM. |
| 3 | ITs falham com 429, ou register/login retorna inesperado em testes | Rate limiter por IP + MockMvc sempre usa `127.0.0.1` + contexto compartilhado → bucket esgota entre classes | Desligar rate limiting nos ITs via `@DynamicPropertySource` (`security.rate-limit.auth.enabled=false`) + capacidade alta como reforço. |
| 4 | CI mostra só `AssertionError at File:linha`, sem a mensagem nem a causa do 500 | Reporter padrão do Gradle omite a mensagem da asserção e o stdout/stderr do app | `tasks.withType<Test> { testLogging { exceptionFormat = FULL; showExceptions = true; showCauses = true; showStackTraces = true; showStandardStreams = true } }` |
| 5 | Testcontainers não conecta no **Docker Desktop (Windows)** (`HTTP 400` em `/info`), mas `docker` CLI funciona | Incompatibilidade docker-java × proxy do named pipe do Docker Desktop | Rodar os ITs no **CI Linux** (Docker nativo, unix socket). Localmente: expor TCP 2375 e `DOCKER_HOST=tcp://localhost:2375`; se o engine travar no boot, `wsl --shutdown` + relançar. |
| 6 | Testes com colisão de e-mail/`duplicate` aleatória | `System.currentTimeMillis()` repete dentro do mesmo milissegundo | Gerar valores únicos com `UUID.randomUUID()`. |
| 7 | **500 no registro** com `@MapsId` | ID setado manualmente ao usar `@MapsId` | Não passar o ID — o `@MapsId` deriva da entidade pai. |
| 8 | Dados de **seed aparecem em produção** | `V999__seed.sql` em `db/migration` (que prod carrega) | Seeds em `db/seed`; `flyway.locations` inclui `db/seed` **só** em dev/test. |
| 9 | JSONB não mapeia / erro de tipo no Hibernate | Falta suporte a JSONB no Kotlin/Hibernate | Dependência `io.hypersistence:hypersistence-utils-hibernate-6x` + `@Type(JsonType::class)` na coluna `columnDefinition = "jsonb"`. |
| 10 | Insert falha em tabela particionada | PK não inclui a chave de partição, ou não há partição p/ a data | PK = `(id, created_at)`; criar partições futuras por migration. `GenerationType.IDENTITY` com `BIGSERIAL` funciona no PG16. |
| 11 | Acham que "dados estão criptografados" porque a senha é segura | Confundir **hash** (senha) com **criptografia** (PII/coluna) e com **TLS** (trânsito) | São três coisas. Senha = hash (ok). PII em coluna = texto puro a menos que cripto de coluna/disco. Trânsito = HTTPS no deploy. |

---

## 10. Anti-Padrões (não fazer)

❌ Field injection · ❌ retornar entity no controller · ❌ lógica em controller/repository ·
❌ `catch (Exception)` genérico · ❌ `System.out.println` · ❌ `!!` em produção ·
❌ `SELECT *` em prod · ❌ UPDATE em conteúdo imutável/versionado · ❌ N+1 (usar `@EntityGraph`/JOIN FETCH) ·
❌ endpoints sem paginação · ❌ 200 OK para erro · ❌ stack trace em resposta de prod ·
❌ IDs sequenciais na URL · ❌ misturar idiomas em campos JSON · ❌ editar migration aplicada.

---

## 11. Fluxo de Trabalho

1. Branch: `git checkout -b feature/[descricao]`.
2. Ler este arquivo (seções 3, 9, 10) + ADR relevante.
3. **TDD:** escrever testes primeiro (seção 7).
4. Implementar seguindo seções 5/6.
5. `./gradlew build` (deve passar). Em DEV, resetar banco quando mexer em migration (`down -v`).
6. Commit (Conventional Commits). PR com review antes de merge em `main`.
7. Atualizar docs/Armadilhas se descobrir algo novo.

---

## 12. Backlog Técnico

- [ ] [Item 1: ex. refresh token + revogação no Redis]
- [ ] [Item 2: ex. observabilidade (OpenTelemetry + logs JSON)]
- [ ] [Item 3: ex. HTTPS/TLS no deploy]

---
_Última atualização: [DATA] · Mantenedor: [NOME]_
