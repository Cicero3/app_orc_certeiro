# Glossário (Linguagem Ubíqua)

> Os termos do domínio e sua definição. Cada termo aqui deve aparecer **com o mesmo nome**
> no código (classes, tabelas, endpoints) — é o que mantém código e negócio falando a mesma língua.
> Preencha com os conceitos da SUA aplicação; abaixo só os termos genéricos que o starter já traz.

## Convenções

- Nomes de domínio em **inglês** no código (classes, colunas, campos JSON).
- Tabelas no **plural** (`users`), colunas em `snake_case`, IDs são **UUID**.

## Termos (starter)

| Termo | Definição |
|-------|-----------|
| **User** | Pessoa autenticável. Tem credenciais e papel (`role`). |
| **UserProfile** | Dados não-credenciais do usuário (ex.: `displayName`). Campos `level/xp/streak` são **exemplo** — adapte ao seu domínio. |
| **UserConsent** | Registro de consentimento (termos/privacidade) com versão e data — base legal (LGPD). |
| **Access token** | JWT curto, enviado no header `Authorization: Bearer`. Identifica o usuário em cada request. |
| **Refresh token** | Token opaco, longo, em cookie httpOnly. Troca por novos tokens (rotação/uso-único). |
| **Role** | Papel do usuário para autorização (RBAC), ex.: `USER`, `ADMIN`. |

## Termos do SEU domínio

<!-- Adicione aqui. Ex.:
| **Order** | Pedido de compra de um Customer. Tem itens e um status. |
-->
