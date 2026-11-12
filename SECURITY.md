# 🔐 Política de Segurança — Educa Mais

## Versões suportadas

| Versão | Suporte |
| --- | --- |
| 1.0.x | ✅ Correções de segurança ativas |
| < 1.0 | ❌ Ambiente de hackathon (sem garantia) |

## Como reportar uma vulnerabilidade

**Não abra issue pública.** Envie um e-mail para `seguranca@educa-mais.com` com:

- descrição do problema e impacto potencial (ex.: vazamento de PII de menor);
- passos para reprodução (payloads, requisições) **sem dados reais de alunos**;
- versão/commit afetado e, se possível, sugestão de correção.

Prazo de resposta: **até 72 h** para confirmação e **7 dias** para plano de correção.
Pedimos divulgação responsável (prazo de 90 dias antes de publicação).

## Controles implementados

| Área | Controle |
| --- | --- |
| Autenticação | JWT stateless (access + refresh), bcrypt custo 12, bloqueio após 10 falhas em 5 min |
| Autorização | RBAC (`ALUNO`, `PROFESSOR`, `ADMIN`) + verificação de dono do recurso |
| Webhook escolar | API Key + HMAC-SHA256 + timestamp (tolerância 5 min) + idempotência |
| Dados pessoais | AES-256-GCM em nome, e-mail, telefone e nascimento; índice cego (HMAC) para busca |
| Transporte | HSTS, CSP restritiva, `X-Frame-Options: DENY`, `no-referrer` |
| CORS | Allowlist explícita por ambiente (nunca `*` com credenciais) |
| Rate limiting | 60 req/min por IP; 10/min no login |
| Pipeline | Gitleaks, OWASP Dependency-Check, Trivy, CodeQL |
| Auditoria | `agent_audit_log` + `security_audit_log` (sem PII) |

## Segredos

Nunca comite chaves. Em produção, use **AWS Secrets Manager** (+ KMS) e as variáveis descritas no
[README](README.md#-variáveis-de-ambiente). Em desenvolvimento, `.env` (ignorado pelo Git) ou o perfil `demo`.
