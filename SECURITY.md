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

## Análise estática (CodeQL) — decisões documentadas

O pipeline roda CodeQL com `security-extended` para Java/Kotlin e TypeScript. Resultados que **não**
são vulnerabilidades neste contexto são excluídos de forma justificada em
[`.github/codeql/codeql-config.yml`](.github/codeql/codeql-config.yml), nunca silenciados:

| Alerta | Situação | Justificativa |
| --- | --- | --- |
| `java/spring-disabled-csrf-protection` | excluído | API stateless com credencial em `Authorization: Bearer`; não há cookie de sessão para o navegador enviar sozinho (ADR-004) |
| Log injection (`ApiExceptionHandler`, `JwtAuthenticationFilter`) | **corrigido** | o caminho da requisição passa por sanitização (`LogSanitizer` / `safePath`): remove caracteres de controle e trunca em 200 caracteres |
| Senha vazia em arquivo de configuração | **corrigido** | perfis `dev`/`test` passaram a usar `${DB_PASSWORD:sa}` em vez de string vazia |
| `replaceAll` que substitui um termo por ele mesmo | **corrigido** | `mission.page.ts` não faz mais a substituição inútil |

## Modelo de ameaças (resumo)

| Ativo | Ameaça | Controle |
| --- | --- | --- |
| Dados de menores (nome, e-mail, telefone, nascimento) | leitura direta do banco | AES-256-GCM por coluna + índice cego HMAC para busca |
| Credenciais do aluno | vazamento em log/erro | senha só em hash Argon2id; `ApiError` nunca devolve stacktrace |
| Webhook da escola | forjar falta / replay | API key + HMAC do payload e timestamp + janela de tolerância |
| Sessão | roubo de token | access token curto, refresh rotativo, sem estado no servidor |
| Pipeline | segredo versionado / dependência vulnerável | Gitleaks + Dependency-Check + Trivy + CodeQL em todo push e semanalmente |

## Reportar uma vulnerabilidade

Envie um e-mail para **security@educamais.example** (ou abra um *private security advisory* no
GitHub) com: versão/commit afetado, passos de reprodução e impacto estimado. Respondemos em até
**72 horas** e publicamos a correção com crédito ao pesquisador, se desejado.
