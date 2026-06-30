# Requirements — [NOME DO PROJETO]

> **Status:** [Draft / Aprovado / Em execução / Completo] · **Versão:** [X.Y] · **Owner:** [NOME]
> Foco: validar a **hipótese principal** do produto com o mínimo necessário. Se não valida a hipótese, é OUT.

---

## 1. Visão do Produto

- **Problema:** [1-2 frases. Se não dá para explicar simples, ainda não entendeu o problema.]
- **Evidência:** [dado/pesquisa/entrevistas que comprovam o problema]
- **Proposta de valor:** "Permitimos que [público] faça [ação] em [tempo/custo] em vez de [alternativa atual]."
- **Público-alvo do MVP:** [perfil · contexto de uso · objetivo]
- **Hipótese principal:**
  > Se [construirmos X], então [público Y] fará [comportamento Z], e saberemos que é verdade quando [métrica M = valor V].

---

## 2. Escopo do MVP

### 2.1 IN (obrigatórias) — regra: só o essencial para a hipótese

| ID | Funcionalidade | Prioridade | Critério de Aceitação (como saber que funciona) |
|----|---------------|------------|--------------------------------------------------|
| F-01 | [Nome] | P0 | [Verificável — idealmente um teste automatizado] |
| F-02 | [Nome] | P0 | [...] |
| F-03 | [Nome] | P1 | [...] |

### 2.2 OUT (adiadas) — o que NÃO faremos, mesmo parecendo óbvio

- ❌ [Feature]: [motivo — complexidade/baixo impacto/dependência]

### 2.3 Anti-Goals (o que NÃO queremos ser)

- ❌ [Ex: não queremos ser rede social] · ❌ [Ex: sem gamificação complexa no MVP]

---

## 3. Casos de Uso Principais

### UC-01: [caso de uso mais importante]
- **Ator:** [quem] · **Pré-condição:** [estado inicial]
- **Fluxo:** 1. [...] 2. [...] 3. **Resultado:** [o que o usuário obtém]
- **Alternativos:** A1 [erro/recuperação] · A2 [...]

### UC-02: [segundo caso de uso]
[repetir]

---

## 4. Restrições Técnicas Críticas (não-negociáveis no MVP)

### 4.1 Stack
- Backend: [linguagem/framework] · Banco: [SGBD] · Cache: [-] · Deploy: [plataforma] · Frontend: [se houver]

### 4.2 Não-funcionais

| Aspecto | Meta | Justificativa |
|---------|------|---------------|
| Latência | [p95 < 500ms] | UX |
| Disponibilidade | [99%] | MVP tolera downtime |
| Custo unitário | [< R$ X/user/mês] | Viabilidade |
| Equipe | [1 dev] | Realidade |

### 4.3 Compliance e Segurança (LGPD/GDPR)

> Marque o que é requisito. Itens técnicos têm teste; itens legais dependem do dono/advogado.

**Técnico (código — dá para testar):**
- [ ] Senha hasheada (BCrypt cost ≥ 12 / Argon2) — *hash, não criptografia*
- [ ] Validação de entrada em todas as APIs públicas
- [ ] Rate limiting em login/register
- [ ] JWT secret via env var; CORS explícito; security headers (HSTS/CSP/X-Frame)
- [ ] Logs sem PII (nunca senha/token/email em log)
- [ ] **Consentimento** explícito no cadastro (registro auditável: versão + timestamp)
- [ ] **Direitos do titular (Art. 18):** acesso aos dados · exportação/portabilidade · exclusão por **anonimização**
- [ ] Conta excluída não autentica (checar `deleted_at` no filtro de auth)
- [ ] HTTPS/TLS em produção (login sobre HTTP puro vaza credenciais)

**Legal/organizacional (NÃO é código — responsabilidade do dono):**
- [ ] Política de Privacidade + Termos de Uso publicados
- [ ] Base legal definida (Art. 7 — ex.: execução de contrato)
- [ ] Encarregado (DPO) designado · Processo de notificação à ANPD
- [ ] (Opcional MVP) Criptografia de PII em repouso / no disco do banco gerenciado

---

## 5. Premissas e Riscos

| ID | Premissa | Como validar | Data limite |
|----|----------|--------------|-------------|
| A-01 | [...] | [pesquisa/entrevista] | [data] |

| ID | Risco | Prob. | Impacto | Mitigação |
|----|-------|-------|---------|-----------|
| R-01 | [...] | A/M/B | A/M/B | [plano B] |

---

## 6. Métricas de Sucesso

- **North Star:** [métrica única de valor] — meta [valor em X semanas].

| Métrica | Meta | Como medir | Frequência |
|---------|------|------------|------------|
| Ativação | [X% fazem ação Y] | [evento] | Semanal |
| Retenção D1 / D7 / D30 | [≥ X%] | [cohort] | Diária/Semanal/Mensal |

- **Critério de pivotagem:** se após [X semanas] não atingir [meta mínima], pivotar para [plano B].

---

## 7. Critério Técnico de "Pronto" (Definition of Done)

> Uma funcionalidade só está pronta quando:
- [ ] Tem teste unitário (lógica) e/ou de integração (endpoint + DB) passando.
- [ ] **CI verde** (build + todos os testes) no commit correspondente.
- [ ] Sem regredir segurança (seção 4.3) nem quebrar migrations.
- [ ] Documentação/contrato de API atualizado (`design.md`) se mudou a API/schema.

---

## 8. Roadmap Pós-MVP

- **Fase 2:** [feature A] · [feature B]
- **Fase 3:** [feature C]

---

## 9. Decisões-Chave (ADRs resumidos)

| Decisão | Opções | Escolha | Motivo |
|---------|--------|---------|--------|
| Autenticação | JWT / Session / OAuth | [JWT] | [stateless, mobile-friendly] |
| Banco | Postgres / Mongo / Dynamo | [Postgres] | [ACID + JSONB] |

> Detalhes em `docs/adr/`.

---

## Histórico de Alterações

| Versão | Data | Autor | Mudança |
|--------|------|-------|---------|
| 1.0 | [data] | [nome] | Versão inicial |
