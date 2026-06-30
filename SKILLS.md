# Skills — Procedimentos Operacionais Padronizados

> "Receitas" para tarefas recorrentes. Cada skill tem código `SK-XX`.
> Só vira skill o que você faria de novo. Comandos abaixo assumem stack Kotlin/Spring/Gradle/Postgres/Docker.

**Índice:** SK-01 Iniciar ambiente · SK-02 Nova migration · SK-03 Novo endpoint · SK-04 Resetar banco ·
SK-05 Investigar 500 · SK-06 Nova dependência · SK-07 Configurar Testcontainers (singleton) ·
SK-08 Diagnosticar falha de teste no CI · SK-09 Verificar auth manualmente + inspecionar banco.

---

## SK-01: Iniciar ambiente de desenvolvimento

```bash
# 1. Subir Postgres + Redis
docker compose up -d
# 2. Esperar o Postgres aceitar conexão
docker exec [container-postgres] pg_isready -U [user]
# 3. Rodar a app (perfil dev)
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
# 4. Confirmar saúde
curl -s http://localhost:8080/actuator/health   # -> {"status":"UP"}
```
**Confirmar:** containers `Up`, `health` = `UP`, log "Started ... in Xs".

---

## SK-02: Criar nova migration

> **Regra de ouro:** migration aplicada NUNCA é editada (ver Armadilha #1 do CLAUDE.md).
> **Schema** vai em `db/migration` (roda em todos os ambientes). **Seed/exemplo** vai em `db/seed` (só dev/test).

```bash
ls src/main/resources/db/migration/      # achar o próximo V[N]
# criar V[N]__descricao.sql com SQL idempotente
```
```sql
-- V[N]__descricao.sql
CREATE TABLE IF NOT EXISTS [tabela] (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- ...
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_[tabela]_[coluna] ON [tabela]([coluna]);
```
**Confirmar:** reiniciar app → log "Successfully applied"; `docker exec ... psql -c "\dt"`.
**Se der checksum mismatch em DEV:** use SK-04 (reset).

---

## SK-03: Adicionar endpoint REST (ordem exata)

1. **DTOs** (`api/dto/`): `XxxRequest` (com Bean Validation) + `XxxResponse` (só o necessário).
2. **Entity** (`domain/`) + **migration** (SK-02) se novo.
3. **Repository** (`infrastructure/`): interface `JpaRepository`.
4. **Use Case** (`application/`): `@Service`, `@Transactional` se escreve. Uma classe por caso de uso.
5. **Controller** (`api/`): `@Valid`, chama o Use Case, retorna `ResponseEntity<ApiResponse<...>>`.
6. **Testes:** unitário do Use Case (MockK) + IT do controller (Testcontainers).

**Confirmar:** `curl` no endpoint + `./gradlew test --tests "*[Controller]IT"`.

---

## SK-04: Resetar banco (dados limpos) — ⚠️ só em DEV

> Use quando: Flyway checksum mismatch, dados corrompidos, ou para começar do zero.

```bash
# Parar a app primeiro
docker compose down -v          # apaga containers E volumes
docker volume ls                # não deve listar volumes do projeto
docker compose up -d            # sobe do zero -> migrations aplicam limpas
```
**Confirmar:** `docker compose ps` mostra containers criados há segundos; log "Successfully applied N migrations".

---

## SK-05: Investigar erro 500

1. Ver o log do servidor (terminal do app ou, no CI, com SK-08). Procurar `ERROR`/`Exception`/`Caused by`.
2. Classificar pela causa raiz:

| Sintoma no stack | Causa provável | Ação |
|---|---|---|
| `relation "x" does not exist` | Migration não aplicada | Conferir `flyway_schema_history` / SK-04 |
| `Connection refused` / porta ≠ container vivo (em teste) | **Testcontainers lifecycle** (Armadilha #2) | Aplicar SK-07 (singleton) |
| `CannotCreateTransactionException` / `Connection is not available` | Pool sem conexão (banco fora / container morto) | Conferir banco no ar; SK-07 em teste |
| `checksum mismatch` no boot | Migration editada/movida | SK-04 em dev; nova migration em prod |
| `NullPointer` no Use Case | Dado ausente no DB | Conferir seed/migration |
| `DataIntegrityViolation` | Duplicidade/FK | Revisar regra de negócio/unique |
| `@MapsId` + 500 no registro | ID setado manual | Não setar o ID (Armadilha #7) |

3. Reproduzir com `curl`. 4. Corrigir. 5. **Escrever teste que captura o bug.** 6. Atualizar Armadilhas no CLAUDE.md.

---

## SK-06: Adicionar dependência

1. Preferir starter oficial do framework. 2. `implementation("[group]:[artifact]:[version]")` no `build.gradle.kts`.
3. `./gradlew dependencies --configuration runtimeClasspath | grep [artifact]` para confirmar resolução.
4. Rodar todos os testes. 5. Documentar na seção Stack do CLAUDE.md.
**Critérios:** mantida ativamente, licença permissiva (MIT/Apache/BSD), sem CVEs conhecidas.

---

## SK-07: Configurar Testcontainers (padrão singleton) — evita o 500 fantasma

> **Por que:** com `@Testcontainers/@Container`, o container morre ao fim de cada classe, mas o Spring
> **cacheia o contexto** entre classes → a 2ª classe usa um pool apontando p/ container morto → 500.
> (Armadilha #2). Solução: subir os containers **uma vez por JVM**.

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractIntegrationTest {
    companion object {
        @JvmStatic val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("test_db").withUsername("test").withPassword("test")
        @JvmStatic val redis = GenericContainer("redis:7-alpine").withExposedPorts(6379)

        init { postgres.start(); redis.start() }   // sobe 1x; Ryuk limpa no fim da JVM

        @DynamicPropertySource @JvmStatic
        fun props(r: DynamicPropertyRegistry) {
            r.add("spring.datasource.url") { postgres.jdbcUrl }
            r.add("spring.datasource.username") { postgres.username }
            r.add("spring.datasource.password") { postgres.password }
            r.add("spring.flyway.locations") { "classpath:db/migration,classpath:db/seed" }
            r.add("spring.data.redis.host") { redis.host }
            r.add("spring.data.redis.port") { redis.firstMappedPort }
            r.add("security.rate-limit.auth.enabled") { false }   // ver Armadilha #3
        }
    }
}
```
**Não use** `@Testcontainers` nem `@Container` com esse padrão.
**Confirmar:** rodar 2+ classes de IT; todas passam (antes, só a 1ª passava).

---

## SK-08: Diagnosticar falha de teste no CI

> O console do Gradle esconde a mensagem da asserção e o log do app. Para ver a causa:

1. **No `build.gradle.kts`** (deixar permanente):
```kotlin
tasks.withType<Test> {
    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true; showCauses = true; showStackTraces = true
        showStandardStreams = true   // mostra o log do app (stack do 500) no console
    }
}
```
2. **Artefato:** publicar `build/reports/tests/test` no workflow; abrir `index.html` →
   classe falha → aba **Standard error/output** tem o stack do servidor.
3. Procurar no log por `but was`, `Caused by`, `Unexpected error`.

**Dica:** se os ITs não rodam localmente (Docker Desktop/Windows, Armadilha #5), o **CI Linux é a fonte de verdade**.

---

## SK-09: Verificar auth manualmente + inspecionar o banco

> Prova concreta, sem frontend, de que cadastro/login/acesso funcionam e a senha não fica em texto puro.

```bash
# 1. Subir ambiente (SK-01). 2. Cadastrar:
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@teste.com","password":"minha-senha-bem-forte-2026","acceptedTerms":true}'
# -> guarde o accessToken da resposta

# 3. Acessar os próprios dados:
curl -s http://localhost:8080/api/v1/users/me/data -H "Authorization: Bearer $TOKEN"

# 4. PROVA no banco — senha vira hash BCrypt, não texto puro:
docker exec [container-postgres] psql -U [user] -d [db] -x \
  -c "SELECT email, password_hash, deleted_at FROM users WHERE email LIKE 'demo%';"
# password_hash esperado: $2a$12$....  (NUNCA a senha original)
```
**Confirmar:** `password_hash` começa com `$2a$12$`; a senha original não existe em lugar nenhum;
`/users/me/data` retorna os dados do próprio usuário.

---

## Como adicionar nova skill
Fez a mesma tarefa 2+ vezes? Crie `SK-[N+1]` com: Quando usar · Passos · Como Confirmar. Atualize o índice.
