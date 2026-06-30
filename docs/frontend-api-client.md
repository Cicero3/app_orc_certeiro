# Client TypeScript da API (frontend)

> Como gerar um client TS **tipado** a partir do OpenAPI do backend, mantendo os tipos
> em sincronia com a API (sem drift). O spec é produzido pelo springdoc; o client é
> gerado pelo [openapi-generator](https://openapi-generator.tech/).

## Pré-requisitos do spec

- O backend expõe o spec em **`/api-docs`** (JSON) e **`/api-docs.yaml`** (YAML).
- Swagger UI em `/swagger-ui.html` (dev).
- **Em produção o spec é desabilitado** (`springdoc.api-docs.enabled=false`) — gere sempre
  contra o ambiente local/dev.
- Auth: esquema `bearer-jwt` (HTTP bearer) declarado globalmente (ver
  [`OpenApiConfig`](../src/main/kotlin/com/englishapp/common/config/OpenApiConfig.kt)).
  Endpoints são agrupados por `@Tag` (`Auth`, `Curriculum`, `Progress`, `UserAccount`),
  então o generator cria `AuthApi`, `CurriculumApi`, etc.

## Passo a passo

```bash
# 1. Sobe Postgres + Redis (necessários para o app iniciar)
docker-compose up -d

# 2. Roda o backend em dev (swagger/api-docs habilitados)
./gradlew bootRun

# 3. Exporta o spec (em outro terminal). Escolha JSON ou YAML:
curl http://localhost:8080/api-docs      > frontend/openapi.json
# curl http://localhost:8080/api-docs.yaml > frontend/openapi.yaml

# 4. Gera o client TS (generator typescript-fetch, sem runtime extra)
npx @openapitools/openapi-generator-cli generate \
  -i frontend/openapi.json \
  -g typescript-fetch \
  -o frontend/src/api/generated \
  --additional-properties=supportsES6=true,typescriptThreePlus=true
```

> `typescript-fetch` usa o `fetch` nativo (sem dependências). Se o frontend já usar
> axios, troque por `-g typescript-axios`.

## Scripts npm (adicionar quando o `frontend/` existir)

```jsonc
// frontend/package.json -> "scripts"
{
  "api:spec": "curl http://localhost:8080/api-docs > openapi.json",
  "api:gen":  "openapi-generator-cli generate -i openapi.json -g typescript-fetch -o src/api/generated --additional-properties=supportsES6=true,typescriptThreePlus=true"
}
```

## Uso do client (exemplo)

```ts
import { Configuration, CurriculumApi } from "@/api/generated";

// O access token (curto) é mantido em memória pelo SPA.
let accessToken = "";

const config = new Configuration({
  basePath: import.meta.env.VITE_API_BASE_URL, // ex.: http://localhost:8080
  accessToken: () => accessToken,
  // ESSENCIAL: envia/recebe o cookie httpOnly do refresh token.
  credentials: "include",
});

const curriculum = new CurriculumApi(config);
const { data } = await curriculum.listModules({ page: 0, size: 20 });
//      ^ tipado: ApiResponseListModuleSummary { data: ModuleSummary[]; meta }
```

### Fluxo de autenticação (P1.5)

- `POST /auth/login` e `/register` retornam o **access token no corpo** (guardar só em
  memória) e setam o **refresh token em cookie `httpOnly`** (o JS não lê — proteção XSS).
- Ao receber `401`, chamar `POST /auth/refresh` **sem corpo** (o navegador envia o cookie):
  retorna um novo access token e rotaciona o cookie. Refresh reutilizado → `401`.
- `POST /auth/logout` revoga as sessões e limpa o cookie.
- Todas as chamadas precisam de `credentials: "include"` para o cookie viajar; o backend
  já responde com `Access-Control-Allow-Credentials: true`. O cookie usa `SameSite=Lax`
  por padrão (mitiga CSRF same-site); deploy cross-domain exige `SameSite=None` + CSRF.

## Regras de manutenção

- **Regerar o client sempre que a API mudar** (novo endpoint, campo de DTO, etc.).
  Como os tipos vêm do spec, o `tsc` do frontend acusa qualquer uso incompatível.
- **Versionar o client gerado** (`src/api/generated`) no repo para o typecheck rodar no CI
  do frontend sem precisar do backend no ar. Tratar como artefato gerado (não editar à mão).
- Manter os `operationId` estáveis: hoje vêm dos nomes dos métodos dos controllers
  (`listModules`, `submitAttempts`, ...). Renomear um método renomeia a função do client.
