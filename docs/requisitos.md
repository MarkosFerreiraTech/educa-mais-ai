# 📋 Especificação de Requisitos — Educa Mais

| Campo | Valor |
| --- | --- |
| Versão | 1.0.0 |
| Status | ✅ Baseline congelada (escopo do MVP) |
| Última atualização | 12/11/2026 |
| Responsável | Engenharia de Requisitos / PM |
| Rastreabilidade | RF/RNF → Casos de Uso → User Stories → Issues → Código → Testes |

> **Convenções**
> - `RF##` — Requisito Funcional (o que o sistema **faz**) · prioridade MoSCoW: `M` Must, `S` Should, `C` Could, `W` Won't
> - `RNF##` — Requisito Não Funcional (como o sistema **é/serve**) · categoria: Segurança, Desempenho, Portabilidade,
>   Usabilidade, Confiabilidade, Manutenibilidade, Observabilidade, Conformidade
> - Cada requisito declara **origem** (stakeholder/ODS), **critério de verificação** e **componente** que o implementa,
>   garantindo o ciclo de rastreabilidade exigido pelo projeto.

---

## 1. Atores do Sistema

| Ator | Tipo | Descrição |
| --- | --- | --- |
| **Aluno** | Humano | Estudante que faltou e recebe a missão de recuperação |
| **Professor** | Humano | Docente que recebe o diagnóstico automático |
| **Administrador** | Humano | Gestor da escola/plataforma (turmas, ementas, usuários) |
| **Agente Autônomo** | Sistema (IA) | Planeja, executa, avalia e adapta a trilha pedagógica |
| **Sistema Escolar** | Sistema externo | ERP/diário de classe que publica os eventos de falta |
| **Authelia (IdP)** | Sistema externo | Provedor OIDC/SSO para professores e administradores |
| **WhatsApp Gateway** | Sistema externo | Twilio Sandbox ou Evolution API para entrega de mensagens |
| **Gemini Flash API** | Sistema externo | LLM que sustenta o raciocínio do agente |

---

## 2. Requisitos Funcionais

### 2.1 Cadastro, Perfil e Personalização

| ID | Requisito | Prioridade | Origem | Verificação | Componente |
| --- | --- | --- | --- | --- | --- |
| **RF01** | O sistema deve permitir que o aluno se cadastre informando nome, e-mail/e-mail escolar, data de nascimento, telefone (WhatsApp) e senha, atribuindo o papel `ALUNO`. | M | Aluno / ODS 4 | Teste de integração `AuthControllerIT` + BDD US-01 | `AuthController`, `StudentService` |
| **RF02** | O sistema deve autenticar-se exclusivamente por **JWT stateless**, devolvendo `accessToken` com expiração de 60 min e refresh de 7 dias. | M | Segurança | `JwtServiceTest`, `JwtAuthenticationFilterIT` | `security/token` |
| **RF03** | O aluno deve escolher **interesses pessoais** (futebol, games, música, ciência, moda, natureza, etc.) no onboarding e editá-los depois. | M | Aluno | Teste `InterestServiceTest` + US-02 | `StudentService`, `Interest` |
| **RF04** | O aluno deve personalizar o **avatar** (tom de pele, cabelo, roupa, acessório) usando a moeda virtual. | S | Aluno | `AvatarServiceTest` + US-03 | `AvatarService`, `ShopService` |
| **RF05** | O sistema deve permitir atualização de perfil e preferências de notificação, incluindo *opt-in* de WhatsApp. | M | Aluno / LGPD | `StudentServiceTest` | `StudentController` |

### 2.2 Ingestão de Faltas (integração com o sistema escolar)

| ID | Requisito | Prioridade | Origem | Verificação | Componente |
| --- | --- | --- | --- | --- | --- |
| **RF06** | O sistema deve expor um endpoint de webhook `POST /api/v1/integrations/school/attendance` para receber o evento de falta **sem intervenção do professor**. | M | Sistema Escolar | `SchoolWebhookIT` | `SchoolIntegrationController` |
| **RF07** | O webhook deve ser autenticado por **assinatura HMAC-SHA256** (`X-Signature`) + **API Key** dedicada (`X-Api-Key`), rejeitando payloads inválidos com `401`. | M | Segurança | `HmacSignatureValidatorTest` | `security/hmac` |
| **RF08** | A assinatura deve ser comparada em tempo constante e o timestamp do evento validado com janela de **5 minutos** para impedir *replay attack*. | M | Segurança | `HmacSignatureValidatorTest#rejeitaReplay` | `security/hmac` |
| **RF09** | O evento deve ser **idempotente** pelo `eventId` do sistema escolar: reprocessamentos não podem criar missões duplicadas. | M | Confiabilidade | `AttendanceIngestionServiceTest#idempotencia` | `AttendanceIngestionService` |
| **RF10** | A ingestão deve ser **assíncrona**: o webhook responde `202 Accepted` e o processamento ocorre em fila (SQS/EventBridge em nuvem; executor interno em dev). | M | Desempenho | `AsyncAttendanceProcessorTest` | `AttendanceEventPublisher` |
| **RF11** | O sistema deve registrar a falta vinculando aluno, aula e origem (`WEBHOOK`, `MANUAL`, `IMPORT`), permitindo ao admin corrigir a situação para `JUSTIFIED`. | M | Escola | `AttendanceServiceTest` | `AttendanceService` |

### 2.3 Agente Autônomo (núcleo de IA)

| ID | Requisito | Prioridade | Origem | Verificação | Componente |
| --- | --- | --- | --- | --- | --- |
| **RF12** | Ao ingerir uma falta, o agente deve **planejar autonomamente** uma trilha de recuperação a partir da ementa da aula perdida. | M | ODS 4 | `RecoveryPlannerTest` + US-06 | `RecoveryPlanner`, `GeminiAgentAdapter` |
| **RF13** | O plano deve conter entre 2 e 5 passos, cada um com objetivo de aprendizagem, explicação, exemplo e pergunta de verificação. | M | Pedagógico | `RecoveryPlanValidatorTest` | `domain/agent/RecoveryPlan` |
| **RF14** | As explicações e exemplos devem ser **contextualizados nos interesses do aluno** (ex.: futebol), usando o interesse de maior prioridade. | M | Aluno | `ExplanationPersonalizationTest` | `PromptFactory` |
| **RF15** | O conteúdo deve ser **ancorado na ementa** (`grounding`): tópicos fora dos objetivos da aula devem ser descartados pelo validador. | M | Professor | `GroundedContentValidatorTest` | `GroundedContentValidator` |
| **RF16** | O agente deve gerar **exercícios diagnósticos adaptativos** (múltipla escolha + resposta curta) com gabarito e justificativa. | M | Pedagógico | `DiagnosticQuestionFactoryTest` | `DiagnosticService` |
| **RF17** | O agente deve **avaliar** cada resposta, com feedback imediato e classificação `CORRECT`/`PARTIAL`/`INCORRECT`. | M | Aluno | `AnswerEvaluationServiceTest` | `AssessmentService` |
| **RF18** | Se o aluno errar, o agente deve **adaptar a estratégia pedagógica** para uma abordagem alternativa (ex.: `EXPLICIT → ANALOGY → VISUAL → STEP_BY_STEP`), no máximo 3 tentativas por passo. | M | Pedagógico | `AdaptiveStrategySelectorTest` + US-07 | `PedagogicalStrategy` (Strategy) |
| **RF19** | Se o aluno acertar, o agente **avança** para o próximo passo automaticamente. | M | Aluno | `MissionProgressTest` | `MissionOrchestrator` |
| **RF20** | O agente deve **decidir e registrar** cada ação (planejar, notificar, ensinar, adaptar, recompensar, reportar) em um *audit trail* do ciclo autônomo. | M | Rastreabilidade | `AgentAuditTrailTest` | `AgentAuditLog` |
| **RF21** | A saída do LLM deve ser validada por **Structured Outputs** (JSON Schema) e, em falha de parsing/validação, o agente deve usar plano de contingência determinístico. | M | Confiabilidade | `GeminiAgentAdapterTest#fallback` | `GeminiAgentAdapter`, `FallbackPlanner` |
| **RF22** | O agente deve encerrar o ciclo aplicando recompensas e disparando o relatório ao professor, sem ação manual. | M | ODS 4 | `MissionCompletionIT` | `MissionCompletionService` |

### 2.4 Notificação Proativa

| ID | Requisito | Prioridade | Origem | Verificação | Componente |
| --- | --- | --- | --- | --- | --- |
| **RF23** | O sistema deve criar uma **notificação in-app** imediatamente após o planejamento da missão. | M | Aluno | `NotificationServiceTest` | `InAppNotificationAdapter` |
| **RF24** | O sistema deve realizar **disparo real de WhatsApp** (Twilio Sandbox ou Evolution API) com mensagem personalizada e *deep link* da missão. | M | Aluno | `WhatsAppAdapterTest` (WireMock) | `WhatsAppNotificationAdapter` |
| **RF25** | Se o provedor de WhatsApp falhar ou não houver credencial, o sistema deve aplicar *fallback* (mock/log), registrar a falha e **não interromper** o fluxo. | M | Confiabilidade | `NotificationFallbackTest` + US-08 | `NotificationRouter` (Adapter) |
| **RF26** | O sistema deve registrar o status de entrega por canal (`PENDING`, `SENT`, `FAILED`, `SKIPPED`) e permitir reenvio. | S | Operação | `NotificationRepositoryTest` | `NotificationService` |

### 2.5 Jornada de Recuperação e Gamificação

| ID | Requisito | Prioridade | Origem | Verificação | Componente |
| --- | --- | --- | --- | --- | --- |
| **RF27** | O aluno deve visualizar suas missões com status e progresso e iniciar a missão a partir do app. | M | Aluno | `MissionControllerIT` | `MissionController` |
| **RF28** | A missão só pode ser acessada pelo próprio aluno (ou pelo professor da turma); acesso indevido retorna `403`. | M | Segurança | `AuthorizationRulesTest` | `MissionAccessPolicy` |
| **RF29** | Ao concluir a missão, o aluno recebe **+50 XP e +1 Diamante**; cada Diamante equivale a **10 Moedas**. | M | Aluno | `RewardCalculatorTest` + US-09 | `RewardCalculator` |
| **RF30** | O sistema deve manter a **ofensiva (streak)** de dias consecutivos sem falta, incrementando-a em dia letivo cumprido e zerando-a em falta não recuperada. | M | Aluno | `StreakServiceTest` | `StreakService` |
| **RF31** | O aluno deve trocar Moedas por itens de avatar em uma **loja** (`GET /shop/items`, `POST /shop/purchase`), com saldo insuficiente retornando `422`. | S | Aluno | `ShopServiceTest` | `ShopService` |
| **RF32** | O sistema deve calcular **nível** a partir do XP acumulado (curva 100 × nível) e expor progresso ao frontend. | C | Aluno | `LevelCalculatorTest` | `LevelCalculator` |

### 2.6 Relatório ao Professor e Administração

| ID | Requisito | Prioridade | Origem | Verificação | Componente |
| --- | --- | --- | --- | --- | --- |
| **RF33** | Ao concluir a missão, o sistema deve gerar e **entregar automaticamente** ao professor um relatório com resumo, lacunas identificadas, nº de tentativas e recomendação pedagógica. | M | Professor | `TeacherReportServiceTest` + US-10 | `TeacherReportService` |
| **RF34** | O professor deve visualizar um **dashboard** com missões da turma, taxa de conclusão, média de acerto e alunos em risco. | M | Professor | `TeacherDashboardIT` | `TeacherDashboardService` |
| **RF35** | O administrador deve cadastrar turmas, aulas/ementas e vincular alunos e professores. | S | Escola | `AdminControllerIT` | `ClassroomService`, `LessonService` |

### 2.7 Segurança, Privacidade e Operação

| ID | Requisito | Prioridade | Origem | Verificação | Componente |
| --- | --- | --- | --- | --- | --- |
| **RF36** | O sistema deve aplicar **RBAC** (`ALUNO`, `PROFESSOR`, `ADMIN`), restringindo endpoints por papel e por vínculo (escola/turma). | M | Segurança | `SecurityConfigTest`, `AuthorizationRulesTest` | `SecurityConfig`, `AccessPolicy` |
| **RF37** | Dados sensíveis (nome, telefone, e-mail, data de nascimento) devem ser **cifrados em repouso com AES-256-GCM**, com IV e tag de autenticação por registro. | M | LGPD | `AesEncryptionServiceTest` + `CryptoConverterTest` | `crypto/AesGcmEncryptor` |
| **RF38** | O sistema deve permitir **anonimização/exclusão** dos dados do aluno a pedido do responsável (*right to be forgotten*). | S | LGPD | `PrivacyServiceTest` | `PrivacyService` |
| **RF39** | O sistema deve registrar logs **sem PII**, usando identificadores pseudonimizados (`studentRef`). | M | LGPD | `PiiLogGuardTest` | `logging/PiiMasker` |
| **RF40** | O sistema deve expor endpoints de saúde e métricas (`/actuator/health`, `/actuator/metrics`) para operação em AWS App Runner. | M | Operação | `ActuatorIT` | Spring Boot Actuator |

---

## 3. Requisitos Não Funcionais

### 3.1 Desempenho e Escalabilidade

| ID | Requisito | Categoria | Métrica / Alvo | Verificação |
| --- | --- | --- | --- | --- |
| **RNF01** | Latência p95 do webhook escolar deve ser **≤ 300 ms** (processamento assíncrono). | Desempenho | p95 ≤ 300 ms | Teste de carga k6/Gatling no CI manual |
| **RNF02** | Tempo entre falta ingerida e notificação enviada deve ser **≤ 60 s** (p95). | Desempenho | p95 ≤ 60 s | Métrica Micrometer `attendance_to_notification` |
| **RNF03** | Chamadas ao Gemini devem ter *timeout* de **20 s**, 2 retries com *exponential backoff* e *circuit breaker*. | Resiliência | timeout 20 s | `ResilienceConfigTest` |
| **RNF04** | Endpoints de leitura devem responder em **≤ 500 ms** (p95) com base de 10 k alunos. | Desempenho | p95 ≤ 500 ms | Índices + `EXPLAIN ANALYZE` |
| **RNF05** | A aplicação deve ser **stateless** e escalar horizontalmente (mín. 2 instâncias App Runner). | Escalabilidade | 2+ réplicas | Docker Compose / App Runner config |

### 3.2 Segurança e Conformidade

| ID | Requisito | Categoria | Métrica / Alvo | Verificação |
| --- | --- | --- | --- | --- |
| **RNF06** | Toda transação autenticada usa **JWT assinado (HS256/RS256)** com validação de assinatura, emissor, audiência e expiração. | Segurança | 100% dos endpoints | `JwtAuthenticationFilterIT` |
| **RNF07** | Política **CORS estrita**: apenas *origins* em allowlist explícita, métodos e headers mínimos, sem `*` em produção. | Segurança | Allowlist | `CorsConfigTest` |
| **RNF08** | Apenas o webhook escolar é público, protegido por **HMAC + API Key dedicada**; todos os demais exigem JWT. | Segurança | 1 rota pública | `SecurityHeadersIT` |
| **RNF09** | Professores e administradores devem autenticar via **Authelia (OIDC/SSO)** com *claim* de papel mapeada. | Segurança | OIDC PKCE | `AutheliaOidcConfigTest` + doc |
| **RNF10** | Senhas armazenadas com **BCrypt (cost ≥ 12)**; nenhum segredo em código (Gitleaks no CI). | Segurança | cost ≥ 12 / 0 leaks | `PasswordEncoderConfig`, Gitleaks |
| **RNF11** | **Headers de segurança** obrigatórios: HSTS, CSP, X-Content-Type-Options, X-Frame-Options, Referrer-Policy. | Segurança | 5 headers | `SecurityHeadersIT` |
| **RNF12** | PII cifrada com **AES-256-GCM**; chaves gerenciadas por **AWS KMS/Secrets Manager** (env var em dev). | Conformidade (LGPD) | AES-256-GCM | `AesEncryptionServiceTest` |
| **RNF13** | Dados em trânsito somente **TLS 1.2+**; RDS com criptografia habilitada. | Conformidade | TLS 1.2+ | IaC review |
| **RNF14** | Rate limiting de **60 req/min/usuário** e **10 req/min/IP** no login (proteção a força bruta). | Segurança | 429 ao exceder | Bucket4j + `RateLimitFilterTest` |
| **RNF15** | Zero vulnerabilidades **High/Critical** em dependências, imagem e segredos (OWASP Dependency-Check, Trivy, Gitleaks). | Segurança | 0 High/Critical | Pipeline `security.yml` |
| **RNF16** | O sistema deve negar acesso a dados de aluno de outro professor/escola (isolamento multi-tenant lógico). | Segurança | 403 | `AccessPolicyTest` |

### 3.3 Usabilidade, Acessibilidade e Responsividade

| ID | Requisito | Categoria | Métrica / Alvo | Verificação |
| --- | --- | --- | --- | --- |
| **RNF17** | Interface **Mobile-First** responsiva (320 px → 1440 px), sem scroll horizontal em 360×640. | Usabilidade | 320–1440 px | Teste Playwright em viewports |
| **RNF18** | UI **100% aderente ao VMware Clarity Design System** (`@clr/angular`, `@clr/ui`, `@cds/core`), sem bibliotecas visuais concorrentes. | Usabilidade | 0 libs concorrentes | Code review + `package.json` |
| **RNF19** | Contraste mínimo **4.5:1**, navegação por teclado, `aria-label` em ícones e foco visível (WCAG 2.2 AA). | Acessibilidade | AA | axe-core no CI |
| **RNF20** | Idioma padrão **pt-BR**, com texto simples adequado a 12–18 anos (legibilidade ≥ 60 Flesch-PT). | Usabilidade | ≥ 60 | Revisão de conteúdo |
| **RNF21** | Feedback de estado obrigatório: carregando, vazio, erro e sucesso em toda tela. | Usabilidade | 100% das telas | Checklist UX |

### 3.4 Manutenibilidade e Qualidade

| ID | Requisito | Categoria | Métrica / Alvo | Verificação |
| --- | --- | --- | --- | --- |
| **RNF22** | **Cobertura de testes ≥ 80%** (linhas e branches) em `domain` e `service`; ≥ 70% global. | Manutenibilidade | ≥ 80% | JaCoCo + `jacoco:check` |
| **RNF23** | **Quality Gate SonarQube verde**: 0 bugs, 0 vulnerabilidades, ≤ 3% duplicação, *rating* A em maintainability, complexidade cognitiva ≤ 15/função. | Manutenibilidade | Gate verde | `sonar-project.properties` + CI |
| **RNF24** | Arquitetura em camadas com **SOLID**, **DIP** (ports/adapters), **DI** e separação **DTO × Entity**; dependências apontam para o domínio. | Manutenibilidade | 0 violações ArchUnit | `ArchitectureTest` (ArchUnit) |
| **RNF25** | **SemVer** + **Conventional Commits** + GitFlow; cada issue rastreável em PR (`Closes #n`). | Processo | 100% dos PRs | Convenção + revisão |
| **RNF26** | Pipeline CI/CD deve rodar build, testes, análise estática e scans em **≤ 12 min**. | Manutenibilidade | ≤ 12 min | Tempo do workflow |

### 3.5 Portabilidade, Observabilidade e Disponibilidade

| ID | Requisito | Categoria | Métrica / Alvo | Verificação |
| --- | --- | --- | --- | --- |
| **RNF27** | Execução local one-command via **Docker Compose** (API + PostgreSQL + frontend + Authelia), sem dependência de nuvem. | Portabilidade | `make up` | `docker-compose.yml` |
| **RNF28** | Deploy em **AWS**: App Runner (API), RDS PostgreSQL 16, Amplify (web), EventBridge/SQS (eventos), Secrets Manager/KMS. | Portabilidade | IaC documentada | `infra/` + docs |
| **RNF29** | Configuração por **variáveis de ambiente** (12-factor), com `.env.example` versionado e segredos fora do Git. | Portabilidade | 12-factor | `.env.example` |
| **RNF30** | **Observabilidade**: logs JSON estruturados com `traceId`, métricas Micrometer e health checks. | Observabilidade | 100% requests | `logback-spring.xml`, Actuator |
| **RNF31** | Disponibilidade alvo **99,5%/mês** com *health check* e reinício automático. | Disponibilidade | 99,5% | App Runner health config |
| **RNF32** | Migrações de banco versionadas com **Flyway**, sempre *forward-only* e idempotentes. | Confiabilidade | 100% versionado | `db/migration` |
| **RNF33** | Tempo de *boot* da API **≤ 25 s** (frio) para o App Runner. | Desempenho | ≤ 25 s | Medição de startup |

---

## 4. Matriz de Rastreabilidade

### 4.1 RF → Caso de Uso → User Story → Issue

| RF | Caso de Uso | User Story | Issue |
| --- | --- | --- | --- |
| RF01–RF05 | UC01, UC02 | US-01, US-02, US-03 | #5, #6, #14 |
| RF06–RF11 | UC03 | US-04, US-05 | #7 |
| RF12–RF15 | UC04 | US-06 | #8, #9 |
| RF16–RF19 | UC05, UC06 | US-07 | #9 |
| RF20–RF22 | UC04, UC06, UC07 | US-06, US-10 | #9, #12 |
| RF23–RF26 | UC04 | US-08 | #10 |
| RF27–RF32 | UC05, UC08 | US-09, US-11 | #9, #11, #14 |
| RF33–RF35 | UC07 | US-10, US-12 | #12, #15 |
| RF36–RF40 | UC09 | US-13 | #4 |

### 4.2 RNF → Estratégia de Verificação → Issue

| RNF | Verificação | Issue |
| --- | --- | --- |
| RNF01–RNF05 | Testes de carga + métricas | #9, #16 |
| RNF06–RNF16 | Testes de segurança + pipeline DevSecOps | #3, #4 |
| RNF17–RNF21 | Playwright + axe + revisão UX | #13, #14 |
| RNF22–RNF26 | JaCoCo + Sonar + ArchUnit + convenções | #3, #4, #5 |
| RNF27–RNF33 | Docker Compose + IaC + Actuator + Flyway | #6, #16, #17 |

---

## 5. Regras de Negócio

| ID | Regra | Origem |
| --- | --- | --- |
| **RN01** | 1 Diamante = 10 Moedas; Diamante não é convertido automaticamente (o aluno decide na loja). | Gamificação |
| **RN02** | Missão concluída = todos os passos com resposta `CORRECT` (ou *step* esgotado após 3 tentativas com revisão obrigatória). | Pedagógico |
| **RN03** | Recompensa é concedida **uma única vez** por missão (idempotência por `missionId`). | Gamificação |
| **RN04** | Ofensiva incrementa apenas em **dia letivo** com falta recuperada; falta não recuperada zera a ofensiva. | Gamificação |
| **RN05** | Máximo de **3 tentativas** por passo antes de o agente mudar de estratégia e seguir com reforço. | Pedagógico |
| **RN06** | Uma falta gera **no máximo uma missão** ativa por aluno/aula (idempotência por `eventId`). | Integração |
| **RN07** | Somente o professor da turma ou o admin acessa o relatório de um aluno. | Segurança |
| **RN08** | Interesse do aluno é usado apenas para **exemplos/analogias**, nunca para alterar o objetivo curricular. | Pedagógico |
| **RN09** | Mensagem de WhatsApp só é enviada com *opt-in* registrado (`notification_opt_in = true`). | LGPD |
| **RN10** | Aluno menor de 16 anos não pode alterar e-mail sem confirmação do responsável registrada no admin. | LGPD |

---

## 6. Glossário

| Termo | Definição |
| --- | --- |
| **Missão de recuperação** | Unidade de trabalho pedagógico que representa o conteúdo de uma aula perdida |
| **Trilha (plan)** | Conjunto ordenado de passos de aprendizagem gerados pelo agente |
| **Estratégia pedagógica** | Abordagem de explicação (explícita, analogia, visual, passo a passo) escolhida dinamicamente |
| **Ofensiva (streak)** | Sequência de dias letivos sem falta pendente |
| **Grounding** | Ancoragem obrigatória da resposta do LLM na ementa da aula |
| **Structured Output** | Resposta do LLM obrigada a um JSON Schema validado no backend |
| **Tool Calling** | Capacidade do agente de invocar ferramentas internas (buscar ementa, consultar aluno, enviar mensagem) |

---

🔗 [Charter](project-charter.md) · [Casos de Uso](casos-de-uso.md) · [User Stories](user-stories.md) ·
[Arquitetura](arquitetura-agente.md)
