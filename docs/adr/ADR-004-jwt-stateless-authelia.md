# ADR-004 — JWT stateless para a API e Authelia (OIDC/SSO) para o back-office

- **Status:** Aceito
- **Data:** 2026-11-12
- **Decisores:** Tech Lead, Segurança
- **Requisitos relacionados:** RF02, RF36, RNF06, RNF07, RNF09, RNF16

## Contexto
A API roda em App Runner com múltiplas réplicas e não deve depender de sessão em memória. Professores e
administradores já usam SSO institucional (Authelia).

## Decisão
- **Alunos:** login local com BCrypt e emissão de **JWT HS256** (access 60 min, refresh 7 dias), validado
  em cada requisição pelo `JwtAuthenticationFilter`.
- **Professores/Admins:** login via **Authelia (OIDC Authorization Code + PKCE)**; o backend valida o
  `id_token` (JWKS, `iss`, `aud`, `nonce`, `exp`), mapeia `groups` para papéis e emite o JWT interno.
- **Webhook escolar:** única rota pública, autenticada por **HMAC-SHA256 + API Key** dedicada.
- CORS por allowlist explícita; headers de segurança; rate limit por usuário/IP.

## Consequências
- ✅ Escala horizontal trivial (stateless) e integração SSO com o ecossistema da escola.
- ⚠️ Revogação imediata de JWT exige lista de bloqueio (`jti`) — prevista para a fase 2.
- ⚠️ Rotação de chave de assinatura precisa ser documentada (key id no header).
