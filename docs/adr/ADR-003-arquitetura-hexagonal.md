# ADR-003 — Arquitetura em camadas com ports & adapters (hexagonal)

- **Status:** Aceito
- **Data:** 2026-11-12
- **Decisores:** Tech Lead
- **Requisitos relacionados:** RNF24, RNF22

## Contexto
O núcleo do produto (regras pedagógicas e ciclo do agente) não pode depender de Spring, JPA, Gemini ou
Twilio — isso inviabilizaria testes rápidos e a troca de providers.

## Decisão
Separar em `api` (apresentação) → `service`/`agent` (aplicação) → `domain` (modelo + ports) ←
`infrastructure` (adapters). Dependências apontam sempre para o domínio. Regras verificadas por
**ArchUnit** no build.

## Consequências
- ✅ Testes de serviço sem contexto Spring; troca de provider sem tocar em regra de negócio.
- ✅ Mapeamento explícito DTO × domínio × entity (sem vazamento de JPA na API).
- ⚠️ Mais classes (mappers, ports), mitigado por padrões de geração assistida e testes de arquitetura.
- ⚠️ Risco de "domínio anêmico" — mitigado por comportamentos nas entidades (`Mission.advance()`).
