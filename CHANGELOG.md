# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/);
versionamento [SemVer](https://semver.org/lang/pt-BR/). Cada release tem **tag anotada**
no Git (`git tag -n` lista as mensagens) e corresponde a um marco funcional do MVP.

## [1.0.0] — 2026-11-12 · MVP hackathon AKCIT Camp

Ciclo autônomo completo, do webhook ao relatório do professor.

### Adicionado

- **Agente de planejamento** com Gemini 2.5 Flash (`LlmPort` + `GeminiAgentAdapter`), grounding na
  ementa (`GroundedContentValidator`) e fallback determinístico (`FallbackPlanner`).
- **Escada pedagógica adaptativa**: `EXPLICIT → ANALOGY → VISUAL → STEP_BY_STEP`, no máximo 3
  tentativas por passo, encaminhando ao professor em `NEEDS_REINFORCEMENT`.
- **Notificações** in-app e WhatsApp plugável (`DEMO`, `TWILIO`, `EVOLUTION`) com consentimento e
  degradação graciosa (`SKIPPED`) quando não há credenciais.
- **Gamificação**: +50 XP e +1 💎 por missão, 1 💎 = 10 moedas, ofensiva de dias, loja com compra
  idempotente; recompensa por chave idempotente (`XP:<eventId>:<studentId>`).
- **Relatório automático ao professor** (resumo, taxa de acerto, lacunas, recomendação via LLM com
  fallback) e dashboard agregado da turma.
- **Frontend Angular 22 + Clarity** (zoneless, signals): shell acessível, jornada da missão, loja,
  perfil e dashboard do professor.
- **Infraestrutura**: `docker-compose.yml` (API + Web + Postgres, perfis `sso` e `observability`),
  Authelia/OIDC, Prometheus + Grafana, Dockerfiles multi-stage, `Makefile`.
- **DevSecOps**: Gitleaks (com allowlist justificada), OWASP Dependency-Check, Trivy e CodeQL
  (`security-extended` com exclusões documentadas).
- **Documentação**: matriz de rastreabilidade requisito → issue → teste, OpenAPI 3.1 (18 rotas),
  índice de diagramas Mermaid, SECURITY.md com modelo de ameaças, CHANGELOG.
- **17 issues** rastreadas, cada uma amarrando requisitos, critérios de aceite em Gherkin e testes.

### Segurança

- PII cifrada com AES-256-GCM por coluna + índice cego HMAC para busca por e-mail.
- Caminho de requisição sanitizado antes de entrar em log (`LogSanitizer`, `safePath`) — defesa
  contra *log injection*.
- Perfis `dev`/`test` sem senha literal (`${DB_PASSWORD:sa}`).
- CSRF desabilitado por design (API stateless, credencial em `Authorization: Bearer`) e decisão
  documentada em ADR-004 e no `SECURITY.md`.

### Qualidade

- 141 testes automatizados (unitários + ITs), cobertura **73,6% de linhas / 72,1% de instruções**
  com gate de 60% no JaCoCo.
- Regras de arquitetura como teste (ArchUnit): domínio isolado, adapters implementam ports,
  controllers não acessam JPA.
- Todos os workflows verdes: Backend CI, Frontend CI, DevSecOps e CodeQL.

## [0.2.0] — 2026-11-11 · Agente autônomo

### Adicionado

- `LlmPort`, `GeminiAgentAdapter` e `GeminiRequestFactory` (structured outputs).
- `RecoveryPlanner` (prompt → LLM → grounding → fallback) e `MissionOrchestrator`.
- `MissionStep` com escada pedagógica e limite de tentativas; `AssessmentService`.
- Webhook escolar com HMAC, anti-replay por timestamp e idempotência por `eventId`.
- Notificação in-app e WhatsApp com templates por status.
- Primeiro teste de integração do ciclo (`AutonomousCycleIT`).

## [0.1.0] — 2026-11-10 · Fundação

### Adicionado

- Documentação de planejamento: charter (ODS 4), requisitos RF01–RF40 / RNF01–RNF24, 11 casos de
  uso, user stories em Gherkin e ADRs 001–006.
- Arquitetura hexagonal: domínio puro, ports e adapters, exceptions de domínio.
- Segurança: JWT access/refresh, `JwtAuthenticationFilter`, RBAC, rate limit configurável,
  `ApiError` como contrato único de erro.
- Cifragem de PII (AES-256-GCM) e índice cego HMAC.
- ArchUnit, Checkstyle, JaCoCo e o primeiro workflow de CI.
