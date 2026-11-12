# 📘 Termo de Abertura do Projeto (Project Charter)

| Campo | Valor |
| --- | --- |
| **Projeto** | Educa Mais — Agente Autônomo de IA para Recuperação de Aprendizagem |
| **Código** | EM-AKCIT-2026 |
| **Versão do documento** | 1.0.0 |
| **Data de abertura** | 12/11/2026 |
| **Patrocinador (Sponsor)** | Coordenação do Hackathon AKCIT Camp |
| **Gestor do Projeto (PM)** | Markos Ferreira |
| **Tech Lead / Arquitetura** | Markos Ferreira (assistido por IA) |
| **Alinhamento estratégico** | ODS 4 — Educação de Qualidade (ONU Agenda 2030), meta 4.1 e 4.c |
| **Status** | ✅ Aprovado — MVP em desenvolvimento |

---

## 1. Sumário Executivo

O **Educa Mais** é um agente autônomo de inteligência artificial que combate a **defasagem escolar
causada por faltas**. Diferente de um chatbot passivo — que espera a pergunta do aluno — ou de uma
agenda gamificada comum, o Educa Mais é um **sistema agêntico orientado a eventos**:

1. **Observa** um fato do mundo real: a falta registrada no sistema escolar.
2. **Planeja** uma trilha de estudo individual a partir da ementa da aula perdida.
3. **Executa** ações externas: notificação in-app e disparo real de lembrete no WhatsApp.
4. **Adapta** a estratégia pedagógica conforme o desempenho do aluno nos exercícios diagnósticos.
5. **Encerra** o ciclo reportando o resultado ao professor — sem exigir trabalho manual do docente.

## 2. Justificativa e Alinhamento com a ODS 4

A evasão e a defasagem escolar raramente começam com uma decisão de abandonar a escola: começam com
**faltas**. Cada ausência rompe a cadeia de pré-requisitos de um conteúdo e, sem intervenção rápida,
o aluno perde o fio da meada — a reprovação e o abandono vêm depois.

| Problema observado | Consequência | Como o Educa Mais atua |
| --- | --- | --- |
| Falta não tratada nas 24 h seguintes | Lacuna de aprendizagem acumulada | Intervenção proativa no mesmo dia, via WhatsApp + app |
| Conteúdo de recuperação genérico | Baixo engajamento do adolescente | Exemplos ancorados em interesses pessoais (ex.: futebol) |
| Professor sem tempo para diagnóstico individual | Nenhum feedback de retorno | Relatório automático de conclusão e lacunas |
| Ferramentas educacionais reativas | Aluno precisa saber pedir ajuda | O agente **inicia** o contato e conduz o ciclo |

**Contribuição direta para a ODS 4:** redução do abandono (meta 4.1), aumento da efetividade docente
(meta 4.c) e equidade no acesso a apoio pedagógico individualizado.

## 3. Visão do Produto

> **Para** estudantes do Ensino Fundamental/Médio que faltam às aulas,
> **que** perdem o encadeamento do conteúdo e não recebem apoio individualizado,
> **o** Educa Mais é um **tutor autônomo proativo**
> **que** detecta a ausência, ensina o conteúdo perdido de forma personalizada e devolve o diagnóstico ao professor.
> **Diferentemente** de plataformas de estudo reativas, o Educa Mais **age sem ser provocado**.

## 4. Objetivos SMART

| # | Objetivo | S | M | A | R | T |
| --- | --- | --- | --- | --- | --- | --- |
| **O1** | Detectar a falta e iniciar o contato com o aluno em **< 60 s** após a ingestão do evento escolar | Escopo: ingestão → notificação | Latência medida no `IncidentNotificationService` (p95) | EventBridge/SQS + Spring assíncrono | Elimina a janela de abandono | Até a entrega do MVP (hackathon) |
| **O2** | Concluir a missão de recuperação com **≥ 85% de acerto** no pós-teste adaptativo | Alunos com trilha concluída | Taxa de acerto agregada | 3 tentativas de adaptação pedagógica | Comprova eficácia pedagógica | Último mês do projeto |
| **O3** | Reduzir o **tempo de produção de diagnóstico** do professor de ~15 min para **0 min (automático)** | Professores da turma piloto | Relatório gerado sem ação manual | Structured Outputs do Gemini Flash | Retorno de valor ao docente | 30 dias após o piloto |
| **O4** | Garantir **cobertura de testes ≥ 80%** com Quality Gate SonarQube verde | Módulos `domain` + `service` | JaCoCo line/branch coverage | JUnit 5 + Mockito | Sustenta evolução corporativa | Em cada merge na `develop` |
| **O5** | Zerar **vulnerabilidades High/Critical** em dependências e segredos no CI | Pipeline DevSecOps | OWASP Dependency-Check + Gitleaks + Trivy | Fail-fast no workflow | Requisito LGPD e reputação | Em cada release |
| **O6** | Entregar **MVP funcional de ponta a ponta** com 3 perfis (aluno, professor, admin) em tela responsiva Mobile-First | Demo do hackathon | Fluxo completo gravado em vídeo | Angular 22 + VMware Clarity 18 | Apresentação ao júri | Até a data do AKCIT Camp |

## 5. Escopo

### 5.1 Dentro do escopo (MVP)

- **Onboarding do aluno:** cadastro, escolha de interesses (futebol, games, música…), personalização do avatar.
- **Ingestão de faltas:** consumer assíncrono de eventos (webhook escolar protegido por HMAC/API Key
  dedicada + fila EventBridge/SQS simulável localmente).
- **Agente autônomo:** consulta de ementa, planejamento de trilha, geração de explicação contextualizada,
  exercícios diagnósticos adaptativos, decisão de avanço/reforço, Structured Outputs.
- **Notificação multicanal:** notificação in-app + disparo WhatsApp (Twilio Sandbox / Evolution API),
  com *fallback* de demonstração quando não há credencial.
- **Gamificação da recuperação:** +50 XP, 1 Diamante (1 💎 = 10 moedas), ofensiva (*streak*) de dias sem falta,
  loja de customização do avatar.
- **Relatório ao professor:** sumário de conclusão, lacunas identificadas e recomendação pedagógica.
- **Segurança:** JWT stateless, CORS estrito, webhook com HMAC, cifragem AES-256-GCM de PII (LGPD),
  RBAC (ALUNO/PROFESSOR/ADMIN), conformidade com Authelia (OIDC/SSO) documentada e integrada.
- **Qualidade:** testes unitários/integração, JaCoCo, SonarQube, OWASP, Gitleaks, Trivy, pipelines CI/CD,
  GitFlow + Conventional Commits + SemVer.

### 5.2 Fora do escopo (expressamente)

- Aplicativo móvel nativo (Android/iOS) — o MVP é **web responsivo Mobile-First**.
- Integração real com diários de classe comerciais (i-Educar, Sponte, Google Classroom) — apenas o
  **contrato** de integração e um *adapter* de referência.
- Múltiplas disciplinas simultâneas e currículo completo da BNCC — o MVP cobre **1 disciplina piloto
  (Matemática)** com ementa semente.
- Vídeo-aulas próprias, avatares 3D, chat síncrono com professor humano.
- Faturamento, licenciamento comercial, multi-tenant com isolamento físico.
- Produção em AWS real com aprovação de orçamento — a infraestrutura é **Infra-as-Code reproduzível**
  e a operação é validada localmente (Docker Compose) + deploy automatizado em App Runner.
- Certificação formal de acessibilidade (WCAG 2.2 AA auditada por terceiros) — buscamos conformidade,
  não o selo nesta fase.

## 6. Stakeholders e Público-Alvo

| Stakeholder | Papel | Interesse / Expectativa |
| --- | --- | --- |
| **Aluno (12–18 anos)** | Usuário primário | Aprender rápido, com linguagem e exemplos que façam sentido |
| **Professor** | Usuário secundário / beneficiário | Diagnóstico pronto, zero trabalho manual |
| **Coordenação pedagógica** | Avaliador institucional | Redução de defasagem, aderência ao currículo |
| **Sistema Escolar (ERP)** | Ator de sistema | Contrato de integração estável, assíncrono e seguro |
| **Equipe de TI da escola** | Operador | Segurança, LGPD, SSO via Authelia |
| **Júri do AKCIT Camp** | Avaliador | Originalidade agêntica, rigor de engenharia, demo funcional |
| **Time de desenvolvimento** | Executor | Requisitos claros, rastreabilidade, dívida técnica controlada |

## 7. Equipe e Governança

| Frente | Responsável | Entregáveis |
| --- | --- | --- |
| Gestão & Documentação | PM | Charter, Requisitos, Casos de Uso, User Stories |
| Arquitetura & Backend | Tech Lead | API Spring Boot 4, agente Gemini, segurança |
| Frontend | Dev Frontend | Angular 22 + VMware Clarity, Mobile-First |
| DevSecOps | DevSecOps | GitHub Actions, Sonar, OWASP, Trivy, Gitleaks, IaC |
| QA | QA | Casos de teste, cobertura, testes de aceite BDD |

**Cadência:** rituais simulados em Sprints de 1 semana, revisão de PR obrigatória, *definition of done*
atrelada ao Quality Gate verde.

## 8. Cronograma e Marcos (Milestones)

| Milestone | SemVer | Conteúdo | Critério de saída |
| --- | --- | --- | --- |
| **M0 — Fundação** | `v0.1.0` | Repositório, docs, CI/CD, segurança base | Pipeline verde + 15 issues |
| **M1 — Domínio & Agente** | `v0.2.0` | Backend núcleo, agente adaptativo, auth | Cobertura ≥ 80% |
| **M2 — Experiência** | `v0.9.0` | Frontend Clarity, gamificação, relatório | Demo navegável |
| **M3 — Release** | `v1.0.0` | Hardening DevSecOps, docs finais | Quality Gate + scan limpos |

## 9. Premissas

1. A escola fornece ementa em formato estruturado (JSON/Markdown) por aula.
2. O aluno possui acesso a WhatsApp ou ao app web.
3. Há disponibilidade de API key do Gemini Flash e de um número Twilio Sandbox para a demo real.
4. A LGPD permite tratar nome, matrícula e desempenho sob base legal de execução de política pública
   educacional + consentimento do responsável.
5. A conta AWS do hackathon permite App Runner, RDS, Amplify, EventBridge, SQS, Secrets Manager e KMS.

## 10. Riscos Principais

| # | Risco | Prob. | Impacto | Mitigação |
| --- | --- | --- | --- | --- |
| R1 | Custo/latência do LLM acima do orçamento | Média | Alto | Prompt enxuto, *structured output*, cache de plano, fallback determinístico |
| R2 | Bloqueio do WhatsApp (sandbox) | Alta | Médio | Evolution API alternativa, notificação in-app garantida |
| R3 | Vazamento de PII de menores | Baixa | **Crítico** | AES-256-GCM, segregação, Gitleaks, logs sem PII, DPO definido |
| R4 | Agente "alucina" conteúdo fora da ementa | Média | Alto | *Grounding* na ementa + validação de tópicos + testes de contrato |
| R5 | Inexperiência do time em Spring Boot 4 / Angular 22 | Média | Médio | Docs, pareamento com IA, testes como rede de segurança |
| R6 | Prazo do hackathon | Alta | Alto | MVP com escopo 5.1 congelado; fora-de-escopo explicitado |

## 11. Critérios de Sucesso (KPIs)

| KPI | Meta | Fonte de medição |
| --- | --- | --- |
| Latência detecção → notificação (p95) | ≤ 60 s | Micrometer / Actuator |
| Taxa de conclusão da missão | ≥ 70% dos alunos notificados | `LearningRecoveryMission.mission_status` |
| Acerto no pós-teste | ≥ 85% | `diagnostic_attempts` |
| Cobertura de testes (domain/service) | ≥ 80% | JaCoCo + SonarQube |
| Vulnerabilidades High/Critical abertas | 0 | OWASP DC + Trivy |
| Avaliação de usabilidade (SUS) | ≥ 80 | Formulário com 10 alunos |

## 12. Orçamento e Restrições Técnicas

- **Infra:** camada gratuita/baixo custo AWS (App Runner 1 vCPU, RDS `db.t4g.micro`), estimativa < US$ 45/mês.
- **LLM:** Gemini Flash (custo por token baixo), orçamento de 1,5 M tokens/mês.
- **WhatsApp:** Twilio Sandbox (gratuito para demo) / Evolution API self-hosted.
- **Stack mandatória:** Java 21 + Spring Boot 4; Angular 22 + VMware Clarity 18; PostgreSQL 16;
  Docker; GitHub Actions.
- **Restrição legal:** nenhum dado de menor pode trafegar sem cifragem em repouso e em trânsito.

## 13. Aprovações

| Nome | Papel | Assinatura | Data |
| --- | --- | --- | --- |
| Coordenação AKCIT Camp | Sponsor | ✔️ (aprovado para o hackathon) | 12/11/2026 |
| Markos Ferreira | PM / Tech Lead | ✔️ | 12/11/2026 |

---

🔗 Documentos relacionados: [Requisitos](requisitos.md) · [Casos de Uso](casos-de-uso.md) ·
[User Stories](user-stories.md) · [Arquitetura do Agente](arquitetura-agente.md) · [ADRs](adr/)
