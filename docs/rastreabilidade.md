# Matriz de rastreabilidade

Cadeia completa: **requisito → caso de uso / user story → issue → código → teste → evidência**.

Como ler: cada linha começa num requisito numerado de [`requisitos.md`](requisitos.md) e termina na prova de que ele existe — um teste automatizado que roda no CI ou um artefato versionado.

## Requisitos funcionais

| RF | Descrição curta | UC / US | Issue | Implementação | Teste / evidência |
|---|---|---|---|---|---|
| RF01 | Cadastro de aluno com interesses | UC01, US-01 | #1 | `AuthService`, `Student` | `AuthFlowIT`, `StudentTest` |
| RF02 | Login com JWT (access + refresh) | UC01 | #1 | `JwtService`, `AuthController` | `AuthFlowIT`, `JwtServiceTest` |
| RF03 | Renovação de token | UC01 | #1 | `AuthService.refresh` | `AuthFlowIT.refreshRenovaSessao` |
| RF04 | Controle de acesso por papel | UC09 | #1 | `SecurityConfig`, `JwtAuthenticationFilter` | `AuthFlowIT` (401/403), `JwtAuthenticationFilterTest` |
| RF05 | Bloqueio por tentativas de login | UC01 | #1 | `LoginAttemptGuard` | `LoginAttemptGuardTest` |
| RF06 | Receber falta do diário de classe | UC03 | #2 | `SchoolIntegrationController` | `AutonomousCycleIT`, `SchoolIntegrationIT` |
| RF07 | Autenticar origem por API key + HMAC | UC03 | #2 | `HmacSignatureValidator` | `HmacSignatureValidatorTest` |
| RF08 | Rejeitar replay por timestamp | UC03 | #2 | `HmacSignatureValidator` | `HmacSignatureValidatorTest.timestampAntigo` |
| RF09 | Idempotência por `eventId` | UC03 | #2 | `AttendanceService` | `AutonomousCycleIT.eventoDuplicadoEIdempotente` |
| RF10 | Publicar evento após commit | UC03 | #2 | `AttendanceRegisteredEvent`, `ObserverConfig` | `AutonomousCycleIT` |
| RF12 | Planejar missão a partir da ementa | UC04, US-05 | #3 | `RecoveryPlanner` | `RecoveryPlannerTest` |
| RF13 | Personalizar pelo interesse do aluno | UC04, US-05 | #3 | `PromptFactory`, `PersonalizationContext` | `RecoveryPlannerTest`, `AutonomousCycleIT` |
| RF14 | Grounding: só conteúdo da ementa | UC04 | #3 | `GroundedContentValidator` | `GroundedContentValidatorTest` |
| RF15 | Fallback quando o LLM falha | UC04 | #3 | `FallbackPlanner` | `RecoveryPlannerTest.llmIndisponivel` |
| RF16 | Escada de estratégias pedagógicas | UC05, US-06 | #4 | `PedagogicalStrategyCatalog`, `AdaptiveStrategySelector` | `PedagogicalStrategyTest` |
| RF17 | Nova explicação após erro | UC05 | #4 | `AssessmentService` | `AssessmentServiceTest` |
| RF18 | Máximo de 3 tentativas por passo | UC05 | #4 | `MissionStep.registerFailure` | `MissionTest` |
| RF19 | Encaminhar ao professor em reforço | UC06 | #4 | `MissionStep` → `NEEDS_REINFORCEMENT` | `MissionTest`, `AutonomousCycleIT` |
| RF21 | Auditoria das decisões do agente | — | #3 | `AgentAuditLogPersistenceAdapter` | `AutonomousCycleIT` (audit) |
| RF22 | Notificação in-app | UC04, US-04 | #5 | `InAppNotificationAdapter` | `NotificationServiceTest`, `AutonomousCycleIT` |
| RF23 | Notificação WhatsApp com consentimento | UC04, US-04 | #5 | `WhatsAppNotificationAdapter` | `NotificationServiceTest` |
| RF24 | Provider plugável (DEMO/TWILIO/EVOLUTION) | UC04 | #5 | `WhatsAppProperties` | `NotificationServiceTest`, `.env.example` |
| RF25 | Degradação graciosa sem credenciais | UC04 | #5 | `WhatsAppNotificationAdapter` (SKIPPED) | `NotificationServiceTest` |
| RF26 | Listar notificações do aluno | US-04 | #5 | `NotificationController` | `AutonomousCycleIT`, `StudentAreaIT` |
| RF27 | Iniciar e responder missão | UC05, US-05 | #12 | `MissionController`, `MissionService` | `MissionServiceTest`, `AutonomousCycleIT` |
| RF28 | Consultar progresso da missão | US-06 | #12 | `MissionService.find` | `MissionServiceTest` |
| RF29 | XP por missão concluída | UC08, US-02 | #6 | `GamificationProfile.grantMissionReward` | `GamificationProfileTest` |
| RF30 | Diamantes e moedas | UC08, US-07 | #6 | `GamificationProfile.convertDiamondsToCoins` | `GamificationProfileTest` |
| RF31 | Ofensiva (streak) de dias | US-02 | #6 | `registerActiveDay` | `GamificationProfileTest` |
| RF32 | Loja de itens | UC08, US-07 | #6 | `ShopService` | `ShopServiceTest`, `StudentAreaIT` |
| RF33 | Relatório automático ao professor | UC07, US-08 | #7 | `TeacherReportService` | `TeacherReportServiceTest` |
| RF34 | Dashboard agregado da turma | UC10 | #7, #13 | `TeacherDashboardService` | `TeacherDashboardServiceTest` |
| RF35 | Recomendação pedagógica (LLM + fallback) | UC07 | #7 | `TeacherReportService` | `TeacherReportServiceTest` |
| RF37 | Cifrar PII e permitir anonimização | UC09 | #8 | `AesGcmEncryptor`, `PrivacyService` | `AesGcmEncryptorTest`, `PrivacyServiceTest` |
| RF38 | Shell responsivo e acessível | US-05 | #11 | `app.html`, `styles.scss` | `ng build`, teste do `AuthService` |
| RF39 | Design system Clarity | US-05 | #11 | componentes + tokens | `ng build` (budget) |
| RF40 | Estados vazios e erros humanizados | US-05 | #11, #12 | `errorInterceptor`, empty states | inspeção + testes de serviço |

## Requisitos não funcionais

| RNF | Descrição curta | Issue | Como é verificado |
|---|---|---|---|
| RNF01–RNF02 | Mobile-first e acessibilidade | #11 | revisão de layout 360px, skip-link, `aria-*`, foco visível |
| RNF05 | Páginas em < 2s no piloto | #11 | build de produção com budget de bundle |
| RNF06–RNF08 | Senhas fortes, JWT, cifragem | #1, #8 | `StudentTest`, `JwtServiceTest`, `AesGcmEncryptorTest` |
| RNF09–RNF10 | LGPD: minimização e anonimização | #8 | `PrivacyServiceTest` |
| RNF11 | Webhook hardened (HMAC + replay) | #2 | `HmacSignatureValidatorTest` |
| RNF12–RNF16 | Docker, Authelia/OIDC, métricas | #10 | `docker-compose.yml`, `infra/**`, `Makefile` |
| RNF17–RNF21 | DevSecOps (4 scanners) | #9 | run do workflow `DevSecOps` verde |
| RNF22–RNF23 | GitFlow, SemVer, Conventional Commits | #14 | `.github/workflows/**`, tags anotadas, histórico |
| RNF24 | Regras de arquitetura automatizadas | #16 | `ArchitectureTest`, `PortsAreInterfacesTest` |
| RNF19 | Cobertura e suíte automatizada | #17 | JaCoCo: 73,6% linhas (gate 60%), 141 testes |

## Rastreabilidade inversa (issue → requisitos)

| Issue | Requisitos |
|---|---|
| #1 | RF01–RF05, RNF06–RNF08, RNF24 |
| #2 | RF06–RF10, RNF11 |
| #3 | RF12–RF15, RF21, RNF18 |
| #4 | RF16–RF19, RN05 |
| #5 | RF22–RF26, RNF12 |
| #6 | RF29–RF32, RN01–RN04 |
| #7 | RF33–RF35 |
| #8 | RF37, RNF09, RNF10 |
| #9 | RNF17–RNF21 |
| #10 | RNF12–RNF16 |
| #11 | RF38–RF40, RNF01, RNF02, RNF05 |
| #12 | RF27, RF28 |
| #13 | RF33, RF34 |
| #14 | RNF22–RNF24 |
| #15 | RNF22, RNF23 (todos os RF documentados) |
| #16 | RNF24 |
| #17 | RNF19, RF06–RF35 |

## Regras de negócio

| RN | Regra | Issue | Teste |
|---|---|---|---|
| RN01 | +50 XP e +1 💎 por missão concluída | #6 | `GamificationProfileTest` |
| RN02 | 1 💎 = 10 moedas | #6 | `GamificationProfileTest` |
| RN03 | Missão exige iniciar antes de responder | #12 | `MissionServiceTest` |
| RN04 | Compra só com saldo suficiente | #6 | `ShopServiceTest` |
| RN05 | Máximo de 3 tentativas por passo | #4 | `MissionTest` |
