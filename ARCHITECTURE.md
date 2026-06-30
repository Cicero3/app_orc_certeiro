# Arquitetura do Software

> Como este starter (e as apps construídas sobre ele) é organizado. Stack:
> **Kotlin + Spring Boot 3.3 + PostgreSQL + Redis**. Leia junto com [CLAUDE.md](CLAUDE.md)
> (regras) e os [ADRs](docs/decisions/) (decisões e seus porquês).

## 1. Estilo: Monólito Modular

Um único deployable, organizado em **features** independentes. Não é microsserviço
(sem rede entre módulos, uma transação por caso de uso), mas cada feature tem fronteira
clara e poderia ser extraída no futuro.

**Por quê:** simplicidade operacional de um monólito + disciplina de acoplamento dos
módulos. Detalhes em [ADR-0001](docs/decisions/0001-arquitetura-monolito-modular.md).

## 2. Camadas (dentro de cada feature)

```
api/             Controllers REST + DTOs de entrada/saída. Sem regra de negócio.
application/     Casos de uso / services. Orquestra domínio + infra. @Transactional aqui.
domain/          Entities, enums, regras puras. Não conhece Spring Web nem outras features.
infrastructure/  Repositories (Spring Data), clients externos, adapters de portas.
```

Regras:
- **Controller fino**: valida entrada (`@Valid`), chama o service, devolve DTO no envelope. Nunca retorna entity JPA.
- **Service** carrega a regra e a transação. Uma operação que escreve em N tabelas = **uma** `@Transactional`.
- **Domain** não importa Spring Web nem outra feature.

## 3. Features e fronteiras

```
com.example.api
├── auth/                # autenticação, conta, perfil (feature de exemplo, completa)
│   ├── api/ application/ domain/ infrastructure/ security/
├── common/              # COMPARTILHADO entre features (não é uma feature)
│   ├── config/          # OpenAPI, JwtProperties, AuthCookieProperties
│   ├── dto/             # ApiResponse {data,meta}, ErrorResponse, PageMeta
│   ├── exceptions/      # GlobalExceptionHandler + exceções
│   ├── ratelimit/       # Bucket4j (rate limiting de auth)
│   ├── util/            # PasswordHasher, PasswordPolicy
│   └── learner/         # PORTA de exemplo (LearnerProfilePort)
└── <sua-feature>/       # adicione o seu domínio aqui
```

**Regra de ouro:** uma feature **não importa classes de outra feature** diretamente.
Quando A precisa de B, defina uma **porta** (interface) em `common/<contexto>` e implemente
um **adapter** na feature dona do dado. Exemplo vivo no starter: `common/learner/LearnerProfilePort`
é implementado por `auth/application/LearnerProfileAdapter`. Isso mantém o acoplamento
unidirecional e testável, e deixa a extração futura barata.

## 4. Fluxo de uma requisição

```
HTTP → SecurityFilterChain (CORS, headers, JwtAuthenticationFilter, RateLimitingFilter)
     → Controller (@Valid, @AuthenticationPrincipal)
     → Service (@Transactional, regra de negócio, portas)
     → Repository (Spring Data / Postgres) | Redis (tokens)
     → DTO  → ApiResponse{data,meta}  → JSON
Erro → GlobalExceptionHandler → ErrorResponse{error:{code,message,path}} + status adequado
```

Convenções de API:
- Versionamento: `/api/v1/...`; recursos por **UUID**.
- Listas: envelope `{ data: [...], meta: {...} }` + paginação `?page=&size=`.
- Timestamps em UTC (ISO-8601).
- Spec OpenAPI em `/api-docs`; geração de client TS em [docs/frontend-api-client.md](docs/frontend-api-client.md).

## 5. Cross-cutting (transversal)

| Preocupação | Onde |
|---|---|
| AuthN/Z | `auth/security` (JWT, filtro, RBAC, refresh em cookie httpOnly, denylist) |
| Rate limiting | `common/ratelimit` (Bucket4j) |
| Erros | `common/exceptions/GlobalExceptionHandler` |
| Validação | Bean Validation (`@Valid`) + checagens no service |
| Config | `common/config` + `application*.yml` por profile |
| Segurança HTTP | `SecurityConfig` (HSTS/CSP/frame/CORS) — ver [docs/security-deploy.md](docs/security-deploy.md) |

## 6. Dados

- **PostgreSQL** com IDs **UUID**; **JSONB** para payloads flexíveis (validar no service).
- **Flyway**: DDL em `db/migration/V{n}__*.sql` (imutável após aplicada — nova migration para mudar);
  dados de **seed** separados em `db/seed/` (não vão para produção por padrão).
- **Redis**: refresh tokens (opacos, rotação) e denylist de access token.

## 7. Testes (pirâmide)

- **Unit (maioria)**: services, validadores, mappers — **MockK** (não Mockito).
- **Integração**: controllers + Postgres/Redis reais via **Testcontainers** (padrão **singleton**:
  containers sobem 1x por JVM — ver `AbstractIntegrationTest`).
- **CI**: `.github/workflows/ci.yml` roda tudo em Linux (onde o Testcontainers é confiável).

## 8. Como adicionar uma nova feature (passo a passo)

1. Crie `com.example.api.<feature>/{api,application,domain,infrastructure}`.
2. Modele a(s) entity(ies) em `domain` + migration `V{n}__create_<feature>.sql`.
3. `infrastructure`: repositórios Spring Data.
4. `application`: caso(s) de uso `@Transactional`; se precisar de outra feature, crie **porta** em `common`.
5. `api`: controller fino + DTOs; agrupe com `@Tag` (OpenAPI).
6. Testes: unit (MockK) do service + IT do fluxo.
7. Atualize docs se mudar contrato/escopo (ver [docs/DOCUMENTOS-BASE.md](docs/DOCUMENTOS-BASE.md)).
