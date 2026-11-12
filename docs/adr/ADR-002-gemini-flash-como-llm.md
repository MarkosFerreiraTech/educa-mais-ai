# ADR-002 — Gemini Flash como LLM do agente, com Structured Outputs e fallback

- **Status:** Aceito
- **Data:** 2026-11-12
- **Decisores:** Tech Lead, QA
- **Requisitos relacionados:** RF12, RF16, RF21, RNF03

## Contexto
O agente precisa gerar trilhas pedagógicas, exercícios e recomendações em português, com boa relação
custo/latência e saída previsível para consumo pelo backend.

## Decisão
Usar **Google Gemini Flash** via **`generateContent`** com `responseMimeType=application/json` e
`responseSchema` (**Structured Outputs**). O acesso ocorre por `LlmPort` (DIP), com:
- timeout de 20 s, 2 retries com backoff exponencial, circuit breaker (Resilience4j);
- validação de schema no backend (`RecoveryPlanValidator`);
- **`FallbackPlanner` determinístico** quando a saída é inválida ou o serviço está indisponível;
- *grounding* obrigatório na ementa (`GroundedContentValidator`).

## Alternativas consideradas
| Alternativa | Motivo da rejeição |
| --- | --- |
| GPT / Claude | Custo por token maior; camada gratuita menos favorável ao hackathon |
| Modelo local (Ollama) | Exigiria GPU no App Runner; latência e operação inviáveis |
| Prompt livre (texto) | Parsing frágil; quebra o contrato com o domínio |

## Consequências
- ✅ Saída tipada e testável; falhas nunca bloqueiam o aluno.
- ⚠️ Dependência de provedor externo: mitigada por adapter + fallback + métrica `llm_fallback_total`.
- ⚠️ Custo por token: prompts enxutos, cache de ementa e limite de 5 passos.
