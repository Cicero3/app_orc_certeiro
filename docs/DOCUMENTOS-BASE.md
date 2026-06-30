# Documentos Base

> Conjunto mínimo de documentos que toda app construída sobre este starter deve ter.
> Os marcados ✅ já vêm prontos (template/exemplo) — basta preencher/adaptar; os ⬜
> você cria conforme o projeto cresce.

| # | Documento | Para quê | Quando atualizar | Status |
|---|-----------|----------|------------------|--------|
| 1 | [CLAUDE.md](../CLAUDE.md) | Fonte única de regras para humanos e agentes de IA (stack, padrões, proibições, armadilhas). | Sempre que uma regra/decisão de processo mudar. | ✅ template |
| 2 | [REQUIREMENTS.md](../REQUIREMENTS.md) | Escopo do produto: o que está **IN** e **OUT**. Evita scope creep. | A cada mudança de escopo. | ✅ template |
| 3 | [ARCHITECTURE.md](../ARCHITECTURE.md) | Estilo arquitetural, camadas, fronteiras, fluxo de request, como adicionar feature. | Ao introduzir um padrão estrutural novo. | ✅ pronto |
| 4 | [docs/GLOSSARY.md](GLOSSARY.md) | Linguagem ubíqua do domínio (termos = nomes no código). | Ao nomear um conceito novo do domínio. | ✅ esqueleto |
| 5 | [docs/decisions/](decisions/) (ADRs) | Decisões arquiteturais e **o porquê**. Uma decisão = um arquivo, imutável. | A cada decisão relevante (nova ADR; não editar antigas). | ✅ template + ADR-0001 |
| 6 | [SKILLS.md](../SKILLS.md) | Habilidades/competências e como o time/agentes operam. | Quando o modo de trabalho mudar. | ✅ template |
| 7 | [README.md](../README.md) | Porta de entrada: o que é, como rodar, primeiros passos. | A cada mudança de setup/onboarding. | ✅ pronto |
| 8 | [docs/security-deploy.md](security-deploy.md) | Segurança em trânsito/repouso e como fazer deploy (TLS no proxy, env/segredos). | Ao mudar infra/segurança de deploy. | ✅ pronto |
| 9 | [docs/frontend-api-client.md](frontend-api-client.md) | Como gerar o client TS do OpenAPI e o fluxo de auth para o frontend. | Ao mudar contrato de API/auth. | ✅ pronto |
| 10 | docs/runbook.md | Operação: como diagnosticar incidentes, logs, métricas, rollback. | Conforme a operação amadurece. | ⬜ criar quando houver prod |

## Ordem sugerida ao iniciar uma app nova

1. **REQUIREMENTS.md** — feche o escopo (IN/OUT) antes de codar.
2. **GLOSSARY.md** — nomeie os conceitos do domínio (vira nome de classe/tabela).
3. **CLAUDE.md** — preencha os `[placeholders]` (nome, stack confirmada, proibições).
4. **ARCHITECTURE.md** — ajuste se divergir do padrão do starter; registre divergências como **ADR**.
5. Codifique a 1ª feature seguindo o passo a passo da ARCHITECTURE §8.
6. Abra **ADRs** sempre que decidir algo não-óbvio (banco, auth, integração externa…).

> Princípio: documento é contrato vivo. Se o código contradiz o doc, um dos dois está errado —
> conserte na mesma PR.
