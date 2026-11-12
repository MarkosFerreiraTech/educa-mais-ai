# 🏛️ Arquitetura do Educa Mais

| Campo | Valor |
| --- | --- |
| Versão | 1.0.0 |
| Notação | C4 Model (Contexto, Contêiner, Componente) + DER + Fluxo do Agente |
| Stack | Java 21 · Spring Boot 4.0 · PostgreSQL 16 · Angular 22 · VMware Clarity 18 · AWS |
| Decisões | registradas em [`docs/adr/`](adr/) |

> Todos os diagramas estão em **Mermaid** e renderizam nativamente no GitHub.

---

## 1. Princípios Arquiteturais

| # | Princípio | Aplicação concreta |
| --- | --- | --- |
| P1 | **Domínio no centro (Hexagonal / Ports & Adapters)** | `domain` não conhece Spring, JPA nem HTTP. Interfaces `LlmPort`, `WhatsAppPort`, `ClockPort` |
| P2 | **SOLID** | Cada classe com responsabilidade única; Strategy para pedagogia; Adapter para integrações |
| P3 | **DIP** | Serviços dependem de abstrações (`NotificationPort`), nunca de `TwilioClient` |
| P4 | **DI** | Injeção por construtor em 100% dos componentes; zero `@Autowired` em campo |
| P5 | **DTO × Entity** | `api/dto` ↔ `domain/model` ↔ `infrastructure/persistence/entity`, com mappers explícitos |
| P6 | **Eventos de domínio** | `AttendanceRegisteredEvent` → `ApplicationEventPublisher` (Observer) |
| P7 | **Idempotência** | `eventId`, `missionId` como chaves naturais; operações repetíveis |
| P8 | **Fail-safe do agente** | Toda chamada a LLM tem timeout, retry, circuit breaker e fallback determinístico |

---

## 2. C4 — Nível 1: Contexto do Sistema

```mermaid
flowchart TB
    subgraph Pessoas["Pessoas"]
        ALUNO["👦 Aluno<br/>12-18 anos"]
        PROF["👨‍🏫 Professor"]
        ADMIN["🛠️ Administrador"]
    end

    SYS["🎓 <b>Educa Mais</b><br/>Agente autônomo de recuperação<br/>de aprendizagem"]

    subgraph Externos["Sistemas externos"]
        ESCOLA["🏫 Sistema Escolar<br/>ERP / diário de classe"]
        AUTHELIA["🔐 Authelia<br/>OIDC / SSO"]
        WHATSAPP["💬 WhatsApp<br/>Twilio Sandbox / Evolution API"]
        GEMINI["🧠 Google Gemini Flash<br/>LLM + Tool Calling + Structured Outputs"]
    end

    ALUNO -->|usa| SYS
    PROF -->|recebe relatórios| SYS
    ADMIN -->|administra| SYS
    ESCOLA -->|"publica eventos de falta<br/>[HTTPS, HMAC]"| SYS
    SYS -->|autentica| AUTHELIA
    SYS -->|"envia mensagens<br/>[HTTPS REST]"| WHATSAPP
    SYS -->|"gera planos e avaliações<br/>[HTTPS JSON]"| GEMINI
    WHATSAPP -->|entrega| ALUNO
```

---

## 3. C4 — Nível 2: Contêineres

```mermaid
flowchart TB
    subgraph WEB["Camada Web"]
        SPA["🖥️ SPA Angular 22 + Clarity 18<br/>Mobile-First · TypeScript<br/>[Amplify / CDN]"]
    end

    subgraph API["Camada de Aplicação (AWS App Runner)"]
        BACK["⚙️ API Spring Boot 4.0 / Java 21<br/>REST · JWT stateless · CORS estrito<br/>[Docker, 2+ réplicas]"]
        WORKER["🔁 Worker de eventos do agente<br/>Consome SQS · executa o ciclo autônomo<br/>[mesmo artefato, perfil worker]"]
    end

    subgraph DADOS["Dados e Segredos"]
        PG[("🗄️ Amazon RDS PostgreSQL 16<br/>Flyway · TLS · cifrado")]
        S3[("📦 Amazon S3<br/>assets de avatar e relatórios")]
        SM["🔑 Secrets Manager + KMS<br/>chaves AES-256 / API keys"]
    end

    subgraph MSG["Mensageria"]
        EB["📨 EventBridge<br/>barramento de eventos"]
        SQS["📥 SQS + DLQ<br/>faltas e comandos do agente"]
    end

    subgraph SSO["Identidade"]
        AUTHELIA["🔐 Authelia<br/>OIDC Provider"]
    end

    ESCOLA["🏫 Sistema Escolar"]
    GEMINI["🧠 Gemini Flash"]
    WA["💬 WhatsApp Gateway"]

    ESCOLA -->|"POST webhook HMAC"| BACK
    SPA -->|"HTTPS/JSON<br/>Bearer JWT"| BACK
    SPA -->|"Authorization Code + PKCE"| AUTHELIA
    BACK -->|"valida id_token"| AUTHELIA
    BACK -->|"lê segredos"| SM
    BACK --> PG
    BACK --> S3
    WORKER --> PG
    WORKER --> GEMINI
    WORKER --> WA
    SQS --> WORKER
    BACK -->|"202 + enfileira"| SQS
    SQS -.->|"fan-out"| EB
    EB -.->|"reprocessamento / DLQ"| SQS
```

### Responsabilidades dos contêineres

| Contêiner | Responsabilidade | Tecnologia |
| --- | --- | --- |
| **SPA** | Experiência do aluno (missão, avatar, ofensiva) e do professor (dashboard) | Angular 22, Clarity 18, RxJS |
| **API** | Expor REST, autenticar, validar webhook, orquestrar comandos | Spring Boot 4, Spring Security 7, Flyway |
| **Worker** | Executar o ciclo autônomo do agente (planejar, notificar, avaliar, reportar) | Spring Boot 4 + SQS listener |
| **RDS** | Persistência transacional | PostgreSQL 16 |
| **SQS/DLQ** | Desacoplamento e resiliência da ingestão | AWS SQS |
| **Secrets Manager/KMS** | Chaves de cifragem e credenciais de providers | AWS |
| **Authelia** | SSO/OIDC para professores e administradores | Authelia 4.x |

---

## 4. C4 — Nível 3: Componentes do Backend

```mermaid
flowchart TB
    subgraph APRESENTACAO["1. Apresentação (Controllers)"]
        C1["AuthController"]
        C2["StudentController"]
        C3["MissionController"]
        C4["SchoolIntegrationController"]
        C5["TeacherController"]
        C6["ShopController"]
        C7["AdminController"]
    end

    subgraph SEGURANCA["6. Segurança"]
        S1["SecurityConfig<br/>CORS · headers · CSRF off (stateless)"]
        S2["JwtAuthenticationFilter"]
        S3["JwtService (emitir/validar)"]
        S4["HmacSignatureValidator"]
        S5["AccessPolicy (RBAC + vínculo)"]
        S6["RateLimitFilter"]
    end

    subgraph SERVICOS["2. Regra de Negócio (Services)"]
        SV1["StudentService"]
        SV2["AttendanceService"]
        SV3["MissionService"]
        SV4["AssessmentService"]
        SV5["RewardService / StreakService"]
        SV6["TeacherReportService"]
        SV7["ShopService"]
        SV8["NotificationService"]
    end

    subgraph AGENTE["3. Orquestração do Agente"]
        AG1["AttendanceIngestionListener<br/>(Observer)"]
        AG2["RecoveryPlanner"]
        AG3["MissionOrchestrator<br/>(máquina de estados)"]
        AG4["AdaptiveStrategySelector<br/>(Strategy)"]
        AG5["GroundedContentValidator"]
        AG6["AgentAuditTrail"]
        AG7["FallbackPlanner"]
    end

    subgraph DOMINIO["4. Modelo de Domínio"]
        D1["Student · Interest · Avatar"]
        D2["Attendance · Lesson · Syllabus"]
        D3["LearningRecoveryMission · MissionStep"]
        D4["DiagnosticQuestion · DiagnosticAttempt"]
        D5["GamificationProfile · Streak · Reward"]
        D6["TeacherReport · Notification"]
    end

    subgraph PORTS["Ports (interfaces)"]
        P1["LlmPort"]
        P2["NotificationPort"]
        P3["MessagingPort"]
        P4["SyllabusPort"]
        P5["ClockPort"]
        P6["EncryptionPort"]
    end

    subgraph ADAPTERS["5. Acesso a Dados e Adapters"]
        R1["JPA Repositories + Entities"]
        R2["GeminiAgentAdapter"]
        R3["WhatsAppNotificationAdapter<br/>(Twilio / Evolution / Demo)"]
        R4["InAppNotificationAdapter"]
        R5["EventBridgePublisher / SqsConsumer"]
        R6["AesGcmEncryptor"]
    end

    C1 --> SV1
    C2 --> SV1
    C3 --> SV3
    C4 --> SV2
    C5 --> SV6
    C6 --> SV7
    C7 --> SV1
    C3 --> S5
    S1 --> S2 --> S3
    C4 --> S4
    S1 --> S6

    SV2 --> AG1
    SV3 --> AG3
    AG1 --> AG2
    AG3 --> AG2
    AG3 --> AG4
    AG3 --> AG5
    AG3 --> AG6
    AG2 --> P1
    AG2 --> P4
    AG3 --> P2
    AG4 --> P1
    SV8 --> P2
    SV2 --> P3

    P1 -.-> R2
    P2 -.-> R3
    P2 -.-> R4
    P3 -.-> R5
    P4 -.-> R1
    P6 -.-> R6

    SV1 --> D1
    SV2 --> D2
    SV3 --> D3
    SV4 --> D4
    SV5 --> D5
    SV6 --> D6

    SV1 --> R1
    SV3 --> R1
    SV4 --> R1
    SV5 --> R1
    SV6 --> R1
    SV7 --> R1
    SV8 --> R1
```

### 4.1 Mapa de pacotes (implementado)

```text
br.com.educamais
├── EducaMaisApplication.java
├── api/                      # Camada de apresentação
│   ├── controller/           # REST controllers (thin)
│   ├── dto/                  # Records de request/response
│   ├── mapper/               # DTO <-> Domain
│   └── exception/            # @RestControllerAdvice + ApiError
├── config/                   # Configurações (OpenAPI, Jackson, Async, Resilience)
├── domain/
│   ├── model/                # Entidades de domínio puras (sem JPA)
│   ├── enums/
│   ├── event/                # Eventos de domínio (Observer)
│   ├── exception/            # Exceções de negócio
│   └── port/                 # Interfaces (DIP)
├── service/                  # Regras de negócio
├── agent/                    # Orquestração do agente autônomo
│   ├── planner/              # RecoveryPlanner + FallbackPlanner
│   ├── strategy/             # PedagogicalStrategy (Strategy pattern)
│   ├── validation/           # GroundedContentValidator
│   └── audit/                # AgentAuditTrail
├── security/                 # JWT, HMAC, CORS, RBAC, rate limit
├── crypto/                   # AES-256-GCM
└── infrastructure/
    ├── persistence/          # JPA entities, repositories, adapters
    ├── llm/                  # GeminiAgentAdapter + prompt factory
    ├── notification/         # WhatsApp / in-app adapters
    └── messaging/            # SQS / in-memory publisher
```

### 4.2 Regra de dependência (verificada por ArchUnit)

```mermaid
flowchart LR
    API[api] --> SERVICE[service]
    SERVICE --> DOMAIN[domain]
    AGENT[agent] --> DOMAIN
    AGENT --> SERVICE
    SERVICE --> PORTS[domain.port]
    INFRA[infrastructure] --> PORTS
    INFRA --> DOMAIN
    SEC[security] --> SERVICE
    style DOMAIN fill:#d4f4dd,stroke:#2e7d32
    style INFRA fill:#fff3cd,stroke:#b8860b
```

> **Regras:** `domain` não depende de ninguém; `infrastructure` nunca é importado por `domain` ou `service`;
> `api` não acessa repositórios diretamente. Violações quebram o build (`ArchitectureTest`).

---

## 5. Fluxo Autônomo do Agente (ponta a ponta)

```mermaid
sequenceDiagram
    autonumber
    participant ES as 🏫 Sistema Escolar
    participant API as API Spring Boot
    participant SEC as HmacSignatureValidator
    participant Q as Fila (SQS / in-memory)
    participant LIST as AttendanceIngestionListener
    participant AG as MissionOrchestrator
    participant V as GroundedContentValidator
    participant LLM as 🧠 Gemini Flash
    participant N as NotificationPort
    participant WA as 💬 WhatsApp
    participant DB as 🗄️ PostgreSQL
    participant PR as 👨‍🏫 Professor

    ES->>API: POST /integrations/school/attendance (HMAC + ApiKey)
    API->>SEC: valida assinatura + timestamp + idempotência
    SEC-->>API: OK
    API-->>ES: 202 Accepted (eventId)
    API->>Q: publica AttendanceEvent

    Q->>LIST: entrega evento
    LIST->>DB: registra falta (idempotente)
    LIST->>AG: inicia ciclo (AttendanceRegisteredEvent)

    Note over AG: ── 1. OBSERVAR ──
    AG->>DB: carrega aluno, interesses e ementa da aula

    Note over AG: ── 2. PLANEJAR ──
    AG->>LLM: prompt (ementa + interesse + schema JSON)
    LLM-->>AG: plano estruturado (2-5 passos)
    AG->>V: valida grounding nos objetivos da ementa
    V-->>AG: plano aprovado (ou fallback)
    AG->>DB: persiste missão PLANNED

    Note over AG: ── 3. AGIR ──
    AG->>N: notificação in-app
    AG->>N: WhatsApp (deep link da missão)
    N->>WA: envia mensagem
    WA-->>N: SENT / FAILED (fallback registrado)
    AG->>DB: missão NOTIFIED + audit trail

    Note over AG: ── 4. ADAPTAR (interação do aluno) ──
    loop cada passo da trilha
        AG->>LLM: gera exercício diagnóstico
        LLM-->>AG: questão + gabarito
        Note right of AG: aluno responde no app
        AG->>LLM: avalia resposta (structured)
        alt acertou
            AG->>DB: passo COMPLETED → avança
        else errou / parcial
            AG->>AG: AdaptiveStrategySelector escolhe nova abordagem
            AG->>LLM: nova explicação (mesmo objetivo)
            LLM-->>AG: explicação alternativa
            Note over AG: máx. 3 tentativas (RN05)
        end
    end

    Note over AG: ── 5. RECOMPENSAR ──
    AG->>DB: +50 XP, +1 Diamante, streak, nível

    Note over AG: ── 6. REPORTAR ──
    AG->>LLM: gera recomendação pedagógica
    LLM-->>AG: relatório estruturado
    AG->>DB: persiste TeacherReport
    AG->>PR: notificação in-app + dashboard
```

### 5.1 Máquina de estados da missão

```mermaid
stateDiagram-v2
    [*] --> PLANNED: falta ingerida
    PLANNED --> NOTIFIED: notificação enviada
    PLANNED --> BLOCKED_NO_SYLLABUS: ementa ausente
    NOTIFIED --> IN_PROGRESS: aluno abre a missão
    IN_PROGRESS --> IN_PROGRESS: resposta errada (adaptação)
    IN_PROGRESS --> COMPLETED: todos os passos concluídos
    IN_PROGRESS --> COMPLETED_WITH_SUPPORT: passos com reforço
    IN_PROGRESS --> ABANDONED: 3 lembretes sem retorno
    NOTIFIED --> CANCELLED: falta justificada
    COMPLETED --> [*]
    COMPLETED_WITH_SUPPORT --> [*]
    ABANDONED --> [*]
    CANCELLED --> [*]
    BLOCKED_NO_SYLLABUS --> PLANNED: ementa cadastrada
```

### 5.2 Estratégias pedagógicas (padrão Strategy)

```mermaid
flowchart LR
    ERRO[Resposta INCORRECT/PARTIAL] --> SEL{AdaptiveStrategySelector}
    SEL -->|1ª tentativa| E1["EXPLICIT<br/>explicação direta"]
    SEL -->|2ª tentativa| E2["ANALOGY<br/>analogia com futebol"]
    SEL -->|3ª tentativa| E3["VISUAL<br/>diagrama/reto numérico"]
    SEL -->|esgotado| E4["STEP_BY_STEP<br/>andaimes + reforço"]
    E1 --> LLM[Gemini Flash]
    E2 --> LLM
    E3 --> LLM
    E4 --> TEMPL[Template determinístico]
    LLM --> OUT[Nova explicação + exercício calibrado]
    TEMPL --> OUT
```

| Estratégia | Quando usar | Exemplo (frações, aluno fã de futebol) |
| --- | --- | --- |
| `EXPLICIT` | Primeira exposição ao conceito | "Fração é parte de um todo…" |
| `ANALOGY` | Aluno não engajou ou errou por abstração | "Em 4 chutes, 1 gol = 1/4 de aproveitamento" |
| `VISUAL` | Aluno precisa de representação concreta | Reta numérica dividida marcando 1/4 e 2/4 |
| `STEP_BY_STEP` | Erro persistente | Decomposição em 4 micro-passos com verificação |

---

## 6. Arquitetura AWS (produção)

```mermaid
flowchart TB
    subgraph Edge["Borda"]
        R53["Route 53"]
        ACM["ACM (TLS 1.2+)"]
    end

    subgraph Front["Hospedagem Web"]
        AMP["AWS Amplify Hosting<br/>SPA Angular 22"]
    end

    subgraph VPC["VPC (private subnets)"]
        APP["AWS App Runner<br/>Spring Boot 4 · 2+ réplicas<br/>autoscaling por CPU/RPS"]
        WORKER["App Runner (worker)<br/>SQS consumer"]
        RDS[("RDS PostgreSQL 16<br/>Multi-AZ · cifrado · backup 7d")]
        subgraph Secrets["Segurança"]
            KMS["AWS KMS"]
            SM["Secrets Manager"]
        end
    end

    subgraph Async["Assíncrono"]
        EB["EventBridge (custom bus)"]
        SQS["SQS (faltas/agente)"]
        DLQ["SQS DLQ + alarme"]
        CW["CloudWatch<br/>logs + métricas + alarmes"]
    end

    subgraph Ext["Serviços externos"]
        GEM["Gemini Flash"]
        WA["WhatsApp (Twilio/Evolution)"]
        AUT["Authelia (ECS/EC2)"]
    end

    R53 --> AMP
    R53 --> APP
    AMP -->|"API base URL"| APP
    APP --> RDS
    APP --> SM
    SM --> KMS
    APP -->|"publica"| SQS
    EB --> SQS
    SQS --> WORKER
    DLQ -.->|"alarme"| CW
    WORKER --> RDS
    WORKER --> GEM
    WORKER --> WA
    APP --> AUT
    APP --> CW
    WORKER --> CW
    S3[("S3: assets/relatórios")] --> AMP
    APP --> S3
```

### 6.1 Mapeamento requisito → serviço AWS

| Necessidade | Serviço | Configuração |
| --- | --- | --- |
| Backend conteinerizado | **App Runner** | 1 vCPU/2 GB, health `/actuator/health`, 2 instâncias mín. |
| Banco relacional | **RDS PostgreSQL 16** | `db.t4g.micro`, cifrado, multi-AZ em prod |
| Frontend SPA | **Amplify Hosting** | Build Angular, HTTPS, cache CDN |
| Ingestão de faltas | **EventBridge + SQS** | bus custom + fila + DLQ |
| Segredos | **Secrets Manager** | rotação de chaves, IAM restrito |
| Cifragem de PII | **KMS** | CMK por ambiente, envelope encryption |
| Observabilidade | **CloudWatch** | logs JSON, métricas EMF, alarmes |
| Assets | **S3** | bucket privado + URL assinada |
| CI/CD | **GitHub Actions + OIDC** | sem *long-lived keys* (role assumida) |

### 6.2 Estratégia de ambientes

| Ambiente | Banco | LLM | WhatsApp | Custo aprox. |
| --- | --- | --- | --- | --- |
| `local` | Postgres Docker | mock/fallback | demo | R$ 0 |
| `dev` | RDS micro | Gemini real | Twilio Sandbox | ~US$ 20/mês |
| `prod` | RDS Multi-AZ | Gemini real | Twilio/Evolution | ~US$ 45/mês |

---

## 7. Segurança: Authelia (OIDC) + JWT + HMAC

```mermaid
sequenceDiagram
    autonumber
    participant U as 👨‍🏫 Professor
    participant SPA as SPA Angular
    participant AUT as 🔐 Authelia (OIDC)
    participant API as API Spring Boot
    participant DB as PostgreSQL

    U->>SPA: acessa /professor
    SPA->>AUT: /authorize (code + PKCE + state + nonce)
    AUT->>U: tela de login SSO
    U->>AUT: credenciais (MFA opcional)
    AUT-->>SPA: redirect com ?code=...
    SPA->>API: POST /auth/oidc/exchange (code, verifier)
    API->>AUT: /token (code + client_secret)
    AUT-->>API: id_token + access_token
    API->>API: valida assinatura JWKS, iss, aud, nonce, exp
    API->>API: mapeia grupos → ROLE_PROFESSOR / ROLE_ADMIN
    API->>DB: upsert usuário (sub OIDC)
    API-->>SPA: JWT interno (60 min) + refresh (7 d)
    SPA->>API: GET /api/v1/teacher/dashboard (Bearer JWT)
    API-->>SPA: 200 dados da turma
```

### 7.1 Camadas de defesa

```mermaid
flowchart LR
    A["1. TLS 1.2+"] --> B["2. CORS allowlist"]
    B --> C["3. Rate limit 60/min"]
    C --> D["4. JWT signature+claims"]
    D --> E["5. RBAC por papel"]
    E --> F["6. Vínculo escola/turma"]
    F --> G["7. Cifragem AES-256-GCM"]
    G --> H["8. Logs sem PII"]
    style A fill:#e3f2fd
    style H fill:#d4f4dd
```

| Vetor de ataque | Controle | Requisito |
| --- | --- | --- |
| Token forjado | Assinatura HMAC/RSA + validação de claims | RNF06 |
| Replay de webhook | Timestamp 5 min + HMAC + idempotência | RF08, RF09 |
| CSRF | API stateless sem cookies de sessão | RNF06 |
| XSS | CSP estrita, escape padrão do Angular | RNF11 |
| Força bruta | Rate limit + bloqueio progressivo | RNF14 |
| Vazamento de PII em log | `PiiMasker` + revisão por teste | RF39 |
| Acesso horizontal | `AccessPolicy` por vínculo | RNF16 |
| Segredo no repo | Gitleaks no CI + `.gitignore` | RNF15 |
| Dependência vulnerável | OWASP Dependency-Check + Trivy | RNF15 |

---

## 8. Modelo Entidade-Relacionamento (DER)

```mermaid
erDiagram
    SCHOOL ||--o{ CLASSROOM : possui
    SCHOOL ||--o{ STUDENT : matricula
    TEACHER ||--o{ CLASSROOM : leciona
    CLASSROOM ||--o{ STUDENT : contem
    CLASSROOM ||--o{ LESSON : possui
    LESSON ||--|| SYLLABUS : define
    STUDENT ||--o{ INTEREST : escolhe
    STUDENT ||--|| AVATAR : personaliza
    STUDENT ||--|| GAMIFICATION_PROFILE : acumula
    STUDENT ||--o{ ATTENDANCE : registra
    LESSON ||--o{ ATTENDANCE : referencia
    ATTENDANCE ||--o| LEARNING_MISSION : gera
    LEARNING_MISSION ||--o{ MISSION_STEP : contem
    MISSION_STEP ||--o{ DIAGNOSTIC_QUESTION : avalia
    DIAGNOSTIC_QUESTION ||--o{ DIAGNOSTIC_ATTEMPT : recebe
    LEARNING_MISSION ||--o| TEACHER_REPORT : produz
    STUDENT ||--o{ NOTIFICATION : recebe
    STUDENT ||--o{ REWARD_LEDGER : recebe
    SHOP_ITEM ||--o{ AVATAR_ITEM_OWNED : adquirido
    GAMIFICATION_PROFILE ||--o{ AVATAR_ITEM_OWNED : possui
    AGENT_AUDIT_LOG }o--|| LEARNING_MISSION : audita

    SCHOOL {
        uuid id PK
        varchar name
        varchar external_code UK
        timestamptz created_at
    }
    CLASSROOM {
        uuid id PK
        uuid school_id FK
        uuid teacher_id FK
        varchar name
        varchar grade
        varchar shift
    }
    TEACHER {
        uuid id PK
        uuid school_id FK
        varchar name_enc
        varchar email_enc
        varchar oidc_subject UK
        varchar role
    }
    STUDENT {
        uuid id PK
        uuid school_id FK
        uuid classroom_id FK
        varchar name_enc "AES-256-GCM"
        varchar email_enc "AES-256-GCM"
        varchar phone_enc "AES-256-GCM"
        date birth_date_enc "LGPD"
        varchar password_hash "BCrypt"
        boolean notification_opt_in
        varchar status
        bigint version "otimista"
    }
    INTEREST {
        uuid id PK
        uuid student_id FK
        varchar code "FOOTBALL, GAMES..."
        smallint priority
    }
    AVATAR {
        uuid id PK
        uuid student_id FK
        varchar skin_tone
        varchar hair_style
        varchar outfit_code
        varchar accessory_code
    }
    GAMIFICATION_PROFILE {
        uuid id PK
        uuid student_id FK
        int total_xp
        int level
        int diamonds
        int coins
        int current_streak
        int longest_streak
        date last_streak_date
    }
    SHOP_ITEM {
        uuid id PK
        varchar code UK
        varchar name
        varchar category
        int price_coins
        varchar asset_url
    }
    AVATAR_ITEM_OWNED {
        uuid id PK
        uuid profile_id FK
        uuid item_id FK
        timestamptz acquired_at
    }
    LESSON {
        uuid id PK
        uuid classroom_id FK
        date lesson_date
        varchar subject
        varchar topic
    }
    SYLLABUS {
        uuid id PK
        uuid lesson_id FK
        text learning_objectives
        text topics
        text prerequisites
        text source_reference
    }
    ATTENDANCE {
        uuid id PK
        uuid student_id FK
        uuid lesson_id FK
        varchar event_id UK "idempotencia"
        varchar status "ABSENT/JUSTIFIED/PRESENT"
        varchar origin "WEBHOOK/MANUAL/IMPORT"
        timestamptz occurred_at
    }
    LEARNING_MISSION {
        uuid id PK
        uuid attendance_id FK "1:1"
        uuid student_id FK
        varchar mission_status
        varchar plan_source "LLM/FALLBACK"
        smallint progress_percent
        int attempts_total
        timestamptz created_at
        timestamptz completed_at
    }
    MISSION_STEP {
        uuid id PK
        uuid mission_id FK
        smallint step_order
        text objective
        text explanation
        text example
        varchar strategy "EXPLICIT/ANALOGY/VISUAL/STEP_BY_STEP"
        varchar step_status
        smallint attempts
    }
    DIAGNOSTIC_QUESTION {
        uuid id PK
        uuid step_id FK
        varchar question_type "MULTIPLE_CHOICE/SHORT_ANSWER"
        text prompt
        jsonb options
        varchar correct_answer_enc
        text explanation
        smallint difficulty
    }
    DIAGNOSTIC_ATTEMPT {
        uuid id PK
        uuid question_id FK
        uuid student_id FK
        text answer_enc
        varchar classification "CORRECT/PARTIAL/INCORRECT"
        text feedback
        timestamptz answered_at
        int time_spent_seconds
    }
    TEACHER_REPORT {
        uuid id PK
        uuid mission_id FK
        uuid teacher_id FK
        text summary
        text identified_gaps
        text recommendation
        smallint success_rate
        varchar delivery_status
        timestamptz created_at
    }
    NOTIFICATION {
        uuid id PK
        uuid student_id FK
        uuid mission_id FK
        varchar channel "IN_APP/WHATSAPP"
        varchar status "PENDING/SENT/FAILED/SKIPPED"
        varchar provider_message_id
        varchar failure_reason
        text payload
        timestamptz sent_at
    }
    REWARD_LEDGER {
        uuid id PK
        uuid student_id FK
        uuid mission_id FK
        varchar reward_type "XP/DIAMOND/COIN"
        int amount
        varchar idempotency_key UK
        timestamptz granted_at
    }
    AGENT_AUDIT_LOG {
        uuid id PK
        uuid mission_id FK
        varchar action "OBSERVE/PLAN/ACT/ADAPT/REWARD/REPORT"
        varchar decision
        jsonb details
        int latency_ms
        timestamptz created_at
    }
```

### 8.1 Índices e restrições críticas

| Tabela | Índice / Constraint | Justificativa |
| --- | --- | --- |
| `attendance` | `UNIQUE(event_id)` | Idempotência de webhook (RF09) |
| `attendance` | `INDEX(student_id, occurred_at DESC)` | Cálculo de streak e histórico |
| `learning_mission` | `UNIQUE(attendance_id)` | 1 falta → 1 missão (RN06) |
| `learning_mission` | `INDEX(student_id, mission_status)` | Listagem de missões do aluno |
| `diagnostic_attempt` | `INDEX(question_id, student_id)` | Histórico de adaptação |
| `reward_ledger` | `UNIQUE(idempotency_key)` | Recompensa única por missão (RN03) |
| `notification` | `INDEX(status, channel)` | Reprocessamento de falhas |
| `agent_audit_log` | `INDEX(mission_id, created_at)` | Rastreabilidade do ciclo autônomo |

---

## 9. Padrões de Projeto Aplicados

| Padrão | Onde | Problema resolvido | Arquivo de referência |
| --- | --- | --- | --- |
| **Strategy** | `PedagogicalStrategy` (`Explicit`, `Analogy`, `Visual`, `StepByStep`) | Trocar abordagem pedagógica sem `if/else` crescente | `agent/strategy/` |
| **Adapter** | `LlmPort` → `GeminiAgentAdapter`; `NotificationPort` → Twilio/Evolution/Demo | Isolar SDK externo do domínio | `infrastructure/llm`, `infrastructure/notification` |
| **Observer** | `AttendanceRegisteredEvent` → `MissionOrchestrator`, `AuditListener` | Reagir à falta sem acoplamento | `domain/event`, `agent/` |
| **Template Method** | `RecoveryPlanner` (esqueleto) × `FallbackPlanner` | Reaproveitar o fluxo de planejamento | `agent/planner/` |
| **Repository** | `StudentRepository` (port) × JPA | Persistência plugável | `domain/port`, `infrastructure/persistence` |
| **Factory** | `DiagnosticQuestionFactory`, `PromptFactory` | Centralizar construção complexa | `agent/`, `service/` |
| **State (light)** | `MissionStatus` com transições válidas | Impedir transições ilegais | `domain/model/MissionStatus` |
| **Decorator** | `RetryingLlmAdapter` envolvendo o adapter Gemini | Retry + circuit breaker sem duplicação | `infrastructure/llm/` |
| **DTO/Mapper** | `api/dto` + `api/mapper` | Não vazar entidade JPA na API | `api/` |

---

## 10. Observabilidade e Resiliência

| Aspecto | Implementação |
| --- | --- |
| **Logs** | JSON estruturado (`logstash-encoder`), `traceId` via `MDC`, PII mascarada |
| **Métricas** | Micrometer: `attendance_to_notification_seconds`, `agent_plan_duration_seconds`, `llm_fallback_total`, `whatsapp_failure_total` |
| **Tracing** | Propagação de `traceId` do webhook até o worker (header + MDC) |
| **Health** | `/actuator/health` com *liveness* e *readiness* (App Runner) |
| **Resiliência** | Resilience4j: timeout 20 s, retry 2×, circuit breaker (50% falhas / 10 chamadas) |
| **Idempotência** | Chaves naturais + *unique constraints* |
| **DLQ** | Fila morta com alarme CloudWatch e endpoint admin de *replay* |

---

## 11. ADRs (Architecture Decision Records)

| ADR | Título | Status |
| --- | --- | --- |
| [ADR-001](adr/ADR-001-spring-boot-4-java-21.md) | Spring Boot 4 sobre Java 21 | ✅ Aceito |
| [ADR-002](adr/ADR-002-gemini-flash-como-llm.md) | Gemini Flash com Structured Outputs e fallback | ✅ Aceito |
| [ADR-003](adr/ADR-003-arquitetura-hexagonal.md) | Arquitetura hexagonal (ports & adapters) | ✅ Aceito |
| [ADR-004](adr/ADR-004-jwt-stateless-authelia.md) | JWT stateless + Authelia (OIDC) | ✅ Aceito |
| [ADR-005](adr/ADR-005-cifragem-aes-256-gcm.md) | Cifragem de PII com AES-256-GCM | ✅ Aceito |
| [ADR-006](adr/ADR-006-angular-clarity.md) | Angular 22 + VMware Clarity, Mobile-First | ✅ Aceito |

---

🔗 [Charter](project-charter.md) · [Requisitos](requisitos.md) · [Casos de Uso](casos-de-uso.md) ·
[User Stories](user-stories.md) · [README](../README.md)
