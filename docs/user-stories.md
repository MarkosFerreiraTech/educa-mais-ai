# 🧩 Histórias de Usuário e Critérios de Aceite — Educa Mais

| Campo | Valor |
| --- | --- |
| Versão | 1.0.0 |
| Formato | `Como [ator], quero [ação] para que [valor de negócio]` + BDD/Gherkin |
| Base | [Requisitos](requisitos.md) · [Casos de Uso](casos-de-uso.md) |
| Total | 12 histórias · 38 cenários Gherkin |

> **Definition of Ready (DoR):** história possui ator, valor, critérios de aceite testáveis, requisitos
> vinculados e issue no GitHub.
> **Definition of Done (DoD):** código + testes automatizados + cobertura ≥ 80% no módulo + Quality Gate
> SonarQube verde + PR revisado com `Closes #n` + documentação atualizada.

---

## Épico 1 — Onboarding e Personalização

### US-01 · Cadastro do aluno com interesses pessoais

> **Como** aluno do Ensino Fundamental/Médio,
> **quero** me cadastrar escolhendo meus interesses pessoais,
> **para que** meu tutor me ensine usando exemplos que fazem sentido para mim.

| Campo | Valor |
| --- | --- |
| Requisitos | RF01, RF03, RNF06, RNF12 |
| Caso de uso | UC01 |
| Issue | #14 |
| Prioridade | Must |
| Estimativa | 5 SP |

**Critérios de aceite (BDD)**

```gherkin
Cenário: Cadastro bem-sucedido com interesses
  Dado que não estou autenticado
  E informo nome "Ana Souza", e-mail "ana@escola.br", data de nascimento "2012-04-10", telefone "+5581999998888" e senha "S3nh@Forte"
  E seleciono os interesses "futebol" e "games"
  Quando envio a requisição POST /api/v1/auth/register
  Então recebo HTTP 201 com accessToken e refreshToken
  E meu perfil é criado com papel "ALUNO" e status "ACTIVE"
  E meus interesses são persistidos na ordem escolhida

Cenário: PII armazenada cifrada
  Dado que me cadastrei com telefone "+5581999998888"
  Quando um administrador consulta a tabela "students" diretamente
  Então o campo "email" e o campo "phone" estão cifrados com AES-256-GCM
  E o valor em claro não aparece em logs da aplicação

Cenário: E-mail duplicado
  Dado que já existe um aluno cadastrado com "ana@escola.br"
  Quando tento me cadastrar com o mesmo e-mail
  Então recebo HTTP 409 com o código "EMAIL_ALREADY_REGISTERED"
  E nenhum novo registro é criado

Cenário: Payload inválido
  Dado que informo uma senha sem número e sem símbolo
  Quando envio o cadastro
  Então recebo HTTP 400 com a lista de violações de validação
```

---

### US-02 · Personalizar o avatar com moedas

> **Como** aluno,
> **quero** personalizar meu avatar comprando roupas e acessórios com Moedas,
> **para que** eu me sinta representado e motivado a voltar à plataforma.

| Campo | Valor |
| --- | --- |
| Requisitos | RF04, RF31, RF29, RN01 |
| Caso de uso | UC02, UC08 |
| Issue | #14 |
| Prioridade | Should |
| Estimativa | 3 SP |

```gherkin
Cenário: Compra de item com saldo suficiente
  Dado que estou autenticado como aluno
  E possuo 30 Moedas
  E existe o item "Camisa do time" no valor de 20 Moedas
  Quando envio POST /api/v1/shop/purchase com o itemId "camisa-time"
  Então recebo HTTP 200 com o avatar atualizado
  E meu saldo passa a ser 10 Moedas
  E o item é marcado como possuído

Cenário: Saldo insuficiente
  Dado que possuo 5 Moedas
  E o item "Camisa do time" custa 20 Moedas
  Quando tento comprar o item
  Então recebo HTTP 422 com "INSUFFICIENT_COINS"
  E o campo missingCoins é 15
  E nenhum saldo é debitado

Cenário: Conversão de Diamante em Moedas
  Dado que possuo 1 Diamante e 0 Moedas
  Quando solicito a conversão de 1 Diamante
  Então recebo 10 Moedas
  E meu saldo de Diamantes passa a 0

Cenário: Item já possuído não é cobrado novamente
  Dado que já possuo o item "Camisa do time"
  Quando solicito a compra novamente
  Então recebo HTTP 200 sem débito
  E o item permanece aplicado ao avatar
```

---

## Épico 2 — Detecção Autônoma da Falta

### US-03 · Receber faltas do sistema escolar sem trabalho manual

> **Como** professor,
> **quero** que o Educa Mais receba automaticamente as faltas do sistema escolar,
> **para que** eu não precise registrar ausências ou acionar ninguém manualmente.

| Campo | Valor |
| --- | --- |
| Requisitos | RF06, RF07, RF08, RF10, RF11, RNF08 |
| Caso de uso | UC03 |
| Issue | #7 |
| Prioridade | Must |
| Estimativa | 8 SP |

```gherkin
Cenário: Webhook autenticado é aceito
  Dado que o cliente escolar possui API Key válida e shared secret HMAC
  E envio um POST para /api/v1/integrations/school/attendance com corpo assinado
  Quando a assinatura HMAC-SHA256 confere e o timestamp está dentro de 5 minutos
  Então recebo HTTP 202 com o eventId
  E o evento é publicado na fila de processamento assíncrono
  E a resposta ocorre em menos de 300 ms (p95)

Cenário: Assinatura HMAC inválida
  Dado que envio o corpo com uma assinatura incorreta
  Quando o webhook processa a requisição
  Então recebo HTTP 401 com "SECURITY_HMAC_MISMATCH"
  E um log de segurança é emitido sem dados pessoais
  E nenhum evento é publicado

Cenário: Proteção contra replay
  Dado que a assinatura é válida mas o timestamp tem 10 minutos
  Quando o webhook processa a requisição
  Então recebo HTTP 401 com "SECURITY_REPLAY_DETECTED"

Cenário: Evento duplicado é idempotente
  Dado que o evento "evt-001" já foi processado
  Quando o mesmo evento é reenviado
  Então recebo HTTP 200 com "IGNORED_DUPLICATE"
  E nenhuma nova missão é criada

Cenário: API Key desconhecida
  Dado que envio a requisição com "X-Api-Key" inexistente
  Quando o webhook processa
  Então recebo HTTP 401
  E a tentativa é contabilizada para bloqueio por rate limit
```

---

### US-04 · Ser avisado proativamente no WhatsApp

> **Como** aluno que faltou,
> **quero** receber uma mensagem no WhatsApp com a minha missão,
> **para que** eu saiba exatamente o que perdi e possa recuperar rapidamente.

| Campo | Valor |
| --- | --- |
| Requisitos | RF23, RF24, RF25, RF26, RNF02, RN09 |
| Caso de uso | UC04, UC04.1, UC04.2 |
| Issue | #10 |
| Prioridade | Must |
| Estimativa | 8 SP |

```gherkin
Cenário: Envio bem-sucedido pelo provedor configurado
  Dado que sou aluno com opt-in de WhatsApp ativo
  E ocorreu uma falta registrada na primeira aula
  Quando o agente planeja a missão
  Então uma notificação in-app é criada com status "SENT"
  E uma mensagem de WhatsApp é enviada com meu primeiro nome, o conteúdo perdido e o deep link da missão
  E o registro da notificação fica com status "SENT" e providerMessageId preenchido

Cenário: Falha do provedor não interrompe o fluxo
  Dado que o provedor de WhatsApp responde HTTP 500
  Quando o agente tenta o disparo
  Então o sistema realiza 2 retries com backoff exponencial
  E a notificação de WhatsApp é marcada "FAILED" com o motivo
  E a notificação in-app permanece disponível
  E a missão continua em "NOTIFIED"

Cenário: Sem credencial configurada (modo demonstração)
  Dado que as variáveis TWILIO_* não estão definidas
  Quando o agente tenta o disparo
  Então o adapter opera em modo "DEMO"
  E a notificação fica "SKIPPED" com motivo "NO_CREDENTIALS"
  E nenhuma exceção é propagada ao fluxo principal

Cenário: Aluno sem opt-in
  Dado que revoguei o consentimento de WhatsApp
  Quando uma falta é registrada
  Então o canal WhatsApp é "SKIPPED" com motivo "NO_OPT_IN"
  E recebo apenas a notificação no aplicativo

Cenário: Mensagem dentro do prazo
  Dado que o evento de falta foi ingerido às 08:00:00
  Quando a notificação é entregue com sucesso
  Então o intervalo registrado na métrica é menor ou igual a 60 segundos
```

---

## Épico 3 — Agente Pedagógico Adaptativo

### US-05 · Aprender o conteúdo perdido com exemplos do meu interesse

> **Como** aluno,
> **quero** aprender o conteúdo que perdi com exemplos ligados ao futebol,
> **para que** a matéria deixe de ser abstrata e eu entenda de verdade.

| Campo | Valor |
| --- | --- |
| Requisitos | RF12, RF13, RF14, RF15, RF21 |
| Caso de uso | UC04, UC05 |
| Issue | #8, #9 |
| Prioridade | Must |
| Estimativa | 13 SP |

```gherkin
Cenário: Plano gerado e ancorado na ementa
  Dado que a aula perdida tem o objetivo "Comparar frações com denominadores diferentes"
  E meu interesse principal é "futebol"
  Quando o agente planeja a missão
  Então o plano possui entre 2 e 5 passos
  E cada passo tem objetivo, explicação, exemplo e pergunta de verificação
  E todos os passos referenciam o objetivo da ementa
  E pelo menos uma explicação usa um exemplo de futebol

Cenário: Conteúdo fora da ementa é descartado
  Dado que o LLM retornou um passo sobre "números complexos"
  Quando o validador de grounding executa
  Então o passo é removido do plano
  E o descarte é registrado no audit trail do agente

Cenário: Fallback determinístico quando o LLM falha
  Dado que a API do Gemini retorna timeout em todas as tentativas
  Quando o agente planeja a missão
  Então um plano determinístico é gerado a partir da ementa
  E a missão é persistida com planSource "FALLBACK"
  E o aluno recebe a missão normalmente

Cenário: Structured Output inválido é reparado
  Dado que o LLM devolveu um JSON que não adere ao schema
  Quando o agente valida a resposta
  Então uma retentativa com prompt de reparo é executada
  E, se falhar novamente, o fallback determinístico é aplicado
```

---

### US-06 · Receber adaptação quando eu erro

> **Como** aluno com dificuldade,
> **quero** que o tutor mude a forma de explicar quando eu erro,
> **para que** eu tenha a chance de entender por outro caminho em vez de desistir.

| Campo | Valor |
| --- | --- |
| Requisitos | RF16, RF17, RF18, RF19, RF20, RN05 |
| Caso de uso | UC05, UC06 |
| Issue | #9 |
| Prioridade | Must |
| Estimativa | 13 SP |

```gherkin
Cenário: Avanço automático quando acerto
  Dado que estou no passo 1 da missão
  E a estratégia atual é "EXPLICIT"
  Quando respondo o exercício corretamente
  Então recebo feedback com classificação "CORRECT"
  E o agente registra o passo como "COMPLETED"
  E o passo 2 é apresentado automaticamente

Cenário: Adaptação pedagógica quando erro
  Dado que estou no passo 1 com a estratégia "EXPLICIT"
  Quando respondo o exercício incorretamente
  Então recebo feedback com a explicação do meu erro
  E o agente seleciona uma estratégia diferente ("ANALOGY")
  E uma nova explicação é apresentada mantendo o mesmo objetivo curricular
  E o contador de tentativas do passo passa a 1
  E a adaptação é registrada em "adaptation_applied"

Cenário: Limite de três tentativas com reforço
  Dado que já errei três vezes no mesmo passo
  Quando envio a quarta resposta incorreta
  Então o passo é marcado como "NEEDS_REINFORCEMENT"
  E o agente apresenta a versão "STEP_BY_STEP" com andaimes
  E o professor é sinalizado no relatório final
  E o aluno não é bloqueado de concluir a missão

Cenário: Nunca ficar sem resposta
  Dado que o LLM não conseguiu gerar uma explicação alternativa
  Quando a adaptação é solicitada
  Então o agente usa um template determinístico da biblioteca pedagógica
  E o aluno recebe a nova explicação em menos de 3 segundos

Cenário: Acesso indevido à missão de outro aluno
  Dado que estou autenticado como outro aluno
  Quando tento acessar a missão "mission-123" de outro estudante
  Então recebo HTTP 403
  E o evento "ACCESS_DENIED_MISSION" é registrado na auditoria
```

---

### US-07 · Receber recompensas e manter minha ofensiva

> **Como** aluno,
> **quero** ganhar XP, Diamantes e manter minha ofensiva ao concluir a missão,
> **para que** eu sinta progresso real e queira continuar sem faltar.

| Campo | Valor |
| --- | --- |
| Requisitos | RF29, RF30, RF32, RN01, RN03, RN04 |
| Caso de uso | UC08 |
| Issue | #11 |
| Prioridade | Must |
| Estimativa | 5 SP |

```gherkin
Cenário: Recompensa concedida na conclusão
  Dado que concluí todos os passos da missão "mission-123"
  E a missão ainda não foi recompensada
  Quando o agente finaliza o ciclo
  Então recebo +50 XP
  E recebo +1 Diamante
  E minha ofensiva é incrementada em 1 dia letivo
  E meu nível é recalculado pela curva 100 x nível
  E recebo o resumo visual da conquista

Cenário: Recompensa não é duplicada
  Dado que a missão "mission-123" já foi recompensada
  Quando a missão é finalizada novamente por reprocessamento
  Então nenhum XP ou Diamante adicional é concedido
  E o estado retornado é o mesmo da conclusão original

Cenário: Ofensiva zerada por falta não recuperada
  Dado que minha ofensiva era de 7 dias
  E registrei uma falta que não foi recuperada em 3 dias
  Quando o job diário de ofensiva executa
  Então minha ofensiva passa a 0
  E recebo uma mensagem motivacional para reiniciar

Cenário: Rollback em falha transacional
  Dado que ocorre erro ao atualizar o saldo de Moedas
  Quando a transação de recompensa é revertida
  Então nenhum XP nem Diamante é gravado parcialmente
```

---

## Épico 4 — Feedback ao Professor

### US-08 · Receber o relatório de conclusão automaticamente

> **Como** professor,
> **quero** receber um relatório automático da recuperação de cada aluno,
> **para que** eu saiba quem precisa de atenção sem gastar tempo montando diagnósticos.

| Campo | Valor |
| --- | --- |
| Requisitos | RF22, RF33, RNF04, RN07 |
| Caso de uso | UC07 |
| Issue | #12 |
| Prioridade | Must |
| Estimativa | 8 SP |

```gherkin
Cenário: Relatório gerado ao concluir a missão
  Dado que o aluno concluiu a missão da aula "Frações equivalentes"
  Quando o agente encerra o ciclo autônomo
  Então um relatório é persistido com conteúdo perdido, acertos, tentativas, lacunas e tempo
  E uma recomendação pedagógica é anexada
  E o relatório aparece no dashboard do professor da turma
  E o professor recebe uma notificação in-app

Cenário: Recomendação por fallback
  Dado que a geração da recomendação pelo LLM falhou
  Quando o relatório é montado
  Então uma recomendação baseada em regras é aplicada
  E o relatório é entregue mesmo assim
  E o evento "REPORT_FALLBACK" é registrado

Cenário: Isolamento entre professores
  Dado que sou professor da turma "8A"
  Quando tento acessar o relatório de um aluno da turma "8B"
  Então recebo HTTP 403 com "ACCESS_DENIED_SCHOOL"
```

---

### US-09 · Acompanhar o dashboard da turma

> **Como** professor,
> **quero** visualizar a taxa de conclusão e os alunos em risco da minha turma,
> **para que** eu possa intervir pedagogicamente antes da reprovação.

| Campo | Valor |
| --- | --- |
| Requisitos | RF34, RNF04, RNF21 |
| Caso de uso | UC10 |
| Issue | #12, #13 |
| Prioridade | Must |
| Estimativa | 5 SP |

```gherkin
Cenário: Dashboard com indicadores
  Dado que sou professor da turma "8A" com 30 alunos
  E 12 alunos receberam missões nesta semana
  Quando acesso /api/v1/teacher/dashboard?classroomId=8A
  Então vejo total de alunos, missões pendentes, concluídas e taxa de conclusão
  E vejo a lista de alunos em risco ordenada por faltas não recuperadas
  E a resposta ocorre em menos de 500 ms (p95)

Cenário: Estado vazio orientativo
  Dado que a turma não possui missões no período
  Quando acesso o dashboard
  Então a interface mostra um estado vazio com orientação de próximos passos
  E nenhum gráfico é renderizado com dados falsos
```

---

## Épico 5 — Segurança, Privacidade e Operação

### US-10 · Autenticação segura com JWT e SSO Authelia

> **Como** professor,
> **quero** entrar com o SSO da escola (Authelia) e ter sessões seguras,
> **para que** meus dados e os dos alunos fiquem protegidos.

| Campo | Valor |
| --- | --- |
| Requisitos | RF02, RF36, RNF06, RNF07, RNF09, RNF11, RNF14 |
| Caso de uso | UC09 |
| Issue | #4 |
| Prioridade | Must |
| Estimativa | 13 SP |

```gherkin
Cenário: Login de aluno com JWT
  Dado que sou um aluno cadastrado e ativo
  Quando envio credenciais válidas para /api/v1/auth/login
  Então recebo HTTP 200 com accessToken (60 min) e refreshToken (7 dias)
  E o token contém sub, roles, schoolId, iss e aud
  E o token é assinado e verificável pelo backend

Cenário: Login de professor via Authelia (OIDC)
  Dado que acesso a área do professor sem sessão
  Quando sou redirecionado ao Authelia e autentico com sucesso
  Então o backend valida o id_token (assinatura, nonce, iss, aud)
  E mapeia o grupo "professores" para ROLE_PROFESSOR
  E recebo um JWT interno stateless

Cenário: Token inválido
  Dado que envio um token com assinatura adulterada
  Quando acesso /api/v1/missions
  Então recebo HTTP 401 com "invalid_token"
  E o acesso é registrado como SECURITY_TOKEN_INVALID

Cenário: Token expirado
  Dado que meu accessToken expirou
  Quando faço uma requisição autenticada
  Então recebo HTTP 401 com código "TOKEN_EXPIRED"
  E consigo renovar com o refreshToken válido

Cenário: Papel insuficiente
  Dado que sou aluno autenticado
  Quando tento acessar /api/v1/admin/classrooms
  Então recebo HTTP 403 com "ACCESS_DENIED_ROLE"

Cenário: CORS estrito
  Dado que a origem "https://malicioso.example" não está na allowlist
  Quando ela envia uma requisição para a API
  Então o navegador recebe bloqueio de CORS
  E o backend registra "CORS_REJECTED"

Cenário: Proteção contra força bruta
  Dado que faço 11 tentativas de login inválidas em 1 minuto
  Quando envio a 11ª tentativa
  Então recebo HTTP 429 Too Many Requests
```

---

### US-11 · Exercer o direito à privacidade (LGPD)

> **Como** responsável por um aluno menor de idade,
> **quero** solicitar a anonimização dos dados pessoais,
> **para que** o direito à proteção de dados seja respeitado.

| Campo | Valor |
| --- | --- |
| Requisitos | RF37, RF38, RF39, RNF12, RNF13 |
| Caso de uso | UC11 |
| Issue | #4 |
| Prioridade | Should |
| Estimativa | 5 SP |

```gherkin
Cenário: Solicitação de anonimização
  Dado que sou administrador da escola
  E o responsável solicitou a anonimização do aluno "Ana Souza"
  Quando executo POST /api/v1/privacy/anonymize com o studentId
  Então nome, e-mail e telefone são substituídos por pseudônimos
  E os dados de desempenho permanecem anonimizados para estatística
  E a operação é registrada em auditoria com data, autor e escopo
  E o evento "privacy.anonymized" é emitido

Cenário: Logs sem dados pessoais
  Dado que o aluno acessou a missão
  Quando inspeciono os logs da aplicação
  Então encontro apenas "studentRef" pseudonimizado
  E não encontro nome, e-mail, telefone ou data de nascimento

Cenário: Cifragem em repouso
  Dado que um registro de aluno foi persistido
  Quando leio a coluna "phone" diretamente no banco
  Então o valor está cifrado (base64 com IV + tag GCM)
  E a decifragem exige a chave gerenciada (KMS/env)
```

---

### US-12 · Rodar o projeto localmente com um comando

> **Como** desenvolvedor do time,
> **quero** subir toda a stack localmente com um comando,
> **para que** eu possa desenvolver e testar sem depender da AWS.

| Campo | Valor |
| --- | --- |
| Requisitos | RNF27, RNF29, RNF30, RNF32 |
| Caso de uso | — (habilitador técnico) |
| Issue | #6, #16 |
| Prioridade | Must |
| Estimativa | 3 SP |

```gherkin
Cenário: Subida completa da stack
  Dado que tenho Docker e Docker Compose instalados
  E copiei .env.example para .env
  Quando executo "make up"
  Então os containers postgres, backend, frontend e authelia ficam "healthy"
  E a API responde em http://localhost:8080/actuator/health com {"status":"UP"}
  E o frontend responde em http://localhost:4200
  E as migrações Flyway são aplicadas automaticamente

Cenário: Modo demonstração sem credenciais externas
  Dado que não defini GEMINI_API_KEY nem TWILIO_*
  Quando executo o fluxo completo de falta até relatório
  Então o plano é gerado pelo planner de fallback
  E a notificação de WhatsApp é registrada como "SKIPPED"
  E o relatório é gerado e visível ao professor

Cenário: Logs estruturados e rastreáveis
  Dado que enviei uma requisição ao webhook
  Quando consulto os logs do container backend
  Então encontro JSON com "traceId", "level", "logger" e "message"
  E o mesmo traceId aparece no processamento assíncrono do evento
```

---

## 5. Matriz de Rastreabilidade — User Stories

| US | RF | RNF | UC | Issue | Épico |
| --- | --- | --- | --- | --- | --- |
| US-01 | RF01, RF03, RF05 | RNF06, RNF10, RNF12 | UC01 | #14 | Onboarding |
| US-02 | RF04, RF29, RF31 | RNF06, RNF08 | UC02, UC08 | #14 | Onboarding |
| US-03 | RF06–RF11 | RNF01, RNF07, RNF08, RNF14 | UC03 | #7 | Detecção |
| US-04 | RF23–RF26 | RNF02, RNF09 | UC04 | #10 | Detecção |
| US-05 | RF12–RF15, RF21 | RNF03 | UC04, UC05 | #8, #9 | Agente |
| US-06 | RF16–RF20 | RNF03, RNF17 | UC05, UC06 | #9 | Agente |
| US-07 | RF29, RF30, RF32 | RNF04 | UC08 | #11 | Gamificação |
| US-08 | RF22, RF33 | RNF04, RNF16 | UC07 | #12 | Professor |
| US-09 | RF34 | RNF04, RNF21 | UC10 | #12, #13 | Professor |
| US-10 | RF02, RF36 | RNF06–RNF14 | UC09 | #4 | Segurança |
| US-11 | RF37–RF39 | RNF12, RNF13 | UC11 | #4 | Segurança |
| US-12 | RF40 | RNF27–RNF32 | — | #6, #16 | Operação |

---

🔗 [Requisitos](requisitos.md) · [Casos de Uso](casos-de-uso.md) · [Arquitetura](arquitetura-agente.md) ·
[Backlog no GitHub](https://github.com/MarkosFerreiraTech/educa-mais-ai/issues)
