# Segurança no Deploy — TLS em trânsito e dados em repouso

> Decisões e procedimentos de segurança que **não vivem no código da aplicação**, mas são
> obrigatórios antes de produção. Complementa o hardening já feito no app (BCrypt, headers, CORS,
> rate limit, RBAC, consentimento/LGPD).

---

## 1. TLS / HTTPS em trânsito (OBRIGATÓRIO em produção)

**Por quê:** login sobre HTTP puro trafega **email + senha em texto claro** na rede — qualquer
intermediário (Wi-Fi, proxy, ISP) lê. Sem TLS não há cadastro/login seguro, ponto final.

**Arquitetura:** o TLS é **terminado em um reverse proxy** (Caddy, Nginx, Traefik) ou no load
balancer da nuvem (ALB, Cloud Load Balancing). A app roda em HTTP **atrás** do proxy.

**O que já está no código:**
- `application-prod.yml` → `server.forward-headers-strategy: framework`: a app respeita
  `X-Forwarded-Proto/Host` do proxy (URLs corretas; HSTS enviado só sob HTTPS real).
- Security headers (no `SecurityConfig`): **HSTS**, CSP, X-Frame-Options, Referrer-Policy.
- O proxy faz o **redirect HTTP → HTTPS** (não forçamos `requiresSecure()` na app para evitar
  loops de redirect atrás de proxy; o proxy + HSTS cuidam disso).

**Local (HTTPS de verdade para testar):**
```bash
# 1) suba o banco e a app (perfil dev)
docker compose up -d
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
# 2) suba o proxy TLS (Caddy) na frente
docker compose -f docker-compose.tls.yml up -d
# 3) acesse via HTTPS (cert interno -> use -k)
curl -k https://localhost/actuator/health
```

**Produção (Caddy):** no `deploy/Caddyfile`, troque `localhost` pelo seu domínio
(`api.seudominio.com`). O Caddy emite e renova **Let's Encrypt automaticamente** e já redireciona
HTTP→HTTPS. Em ALB/Cloud LB: anexe o certificado (ACM/Managed) ao listener 443 e redirecione 80→443.

**Checklist de produção:**
- [ ] Certificado TLS válido (Let's Encrypt/ACM), renovação automática.
- [ ] Redirect 80 → 443 ativo.
- [ ] HSTS chegando ao navegador (conferir header `Strict-Transport-Security` numa resposta HTTPS).
- [ ] `forward-headers-strategy` ativo (perfil prod).
- [ ] Nenhuma porta da app (8080) exposta publicamente sem ser via proxy.

---

## 2. Dados em repouso

### 2.1 Senha — já resolvido (no código)
Hash **BCrypt cost 12** (one-way). Nem o time recupera a senha. Não é criptografia reversível — é o
correto para senha. (Comprovado: a coluna `password_hash` guarda `$2a$12$...`, nunca a senha.)

### 2.2 PII (email, nome) — criptografia de **disco** do banco (decisão consciente)
**Decisão:** NÃO criptografamos email/nome em coluna. Em vez disso, dependemos da **criptografia de
disco e backup do banco gerenciado**. Cobre o cenário real "roubaram o dump/disco/backup".

**Por que não cripto de coluna no email:** o email é chave de busca do login; criptografia segura
(IV aleatório) impede a busca, exigindo um *blind index* (HMAC) — complexidade alta e ônus de
gestão/rotação de chave, com ganho marginal sobre o disco criptografado para um MVP. Cripto de
coluna se justifica em dados ultra-sensíveis (CPF, saúde, pagamento), não em email.

**O que fazer no provedor (ligar disco criptografado — geralmente 1 clique/flag):**
- **AWS RDS:** "Encryption" habilitado na criação (KMS). Backups/snapshots herdam.
- **Google Cloud SQL:** criptografado em repouso por padrão (CMEK opcional).
- **Azure DB for Postgres:** criptografado em repouso por padrão.
- **Railway / Render / Fly.io / Supabase:** verificar que o volume/instância usa disco criptografado
  (na maioria é padrão) e que **backups são criptografados**.

**Checklist:**
- [ ] Disco do banco criptografado (KMS/CMEK ou padrão do provedor).
- [ ] Backups automáticos **e criptografados**.
- [ ] Acesso ao banco restrito (rede privada/VPC, sem porta pública; credenciais via secret manager).
- [ ] Se um dia entrar PII ultra-sensível (CPF, saúde): aí sim avaliar cripto de coluna (blind index).

---

## 3. Resumo do que protege o quê

| Ameaça | Proteção | Onde |
|--------|----------|------|
| Sniff de rede no login | TLS/HTTPS | reverse proxy (deploy) |
| Vazamento da senha | Hash BCrypt | app (✅ feito) |
| Roubo de dump/disco/backup | Criptografia de disco do banco | provedor (deploy) |
| Leitura do banco vivo sem a chave do app | (cripto de coluna — **não feito**, decisão consciente) | — |
| Força bruta no login | Rate limiting | app (✅ feito) |
| Token vazado | Expiração curta; conta excluída barrada por `deleted_at` | app (revogação real = backlog) |
