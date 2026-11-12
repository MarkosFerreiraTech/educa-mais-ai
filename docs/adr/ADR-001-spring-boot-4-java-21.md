# ADR-001 — Adotar Spring Boot 4.0 sobre Java 21

- **Status:** Aceito
- **Data:** 2026-11-12
- **Decisores:** Tech Lead, Arquiteto
- **Requisitos relacionados:** RNF24, RNF33

## Contexto
O backend precisa de produtividade, maturidade de ecossistema e suporte de longo prazo para um MVP
corporativo com testes e DevSecOps. A instituição exige Java 21 (LTS) e Spring Boot 4.

## Decisão
Construir a API em **Java 21** com **Spring Boot 4.0.x** (Spring Framework 7, Spring Security 7),
Maven 3.9, Flyway, JUnit 5 + Mockito + AssertJ, JaCoCo e ArchUnit.

## Alternativas consideradas
| Alternativa | Motivo da rejeição |
| --- | --- |
| Spring Boot 3.5 | Fim de linha mais próximo; requisito explícito do hackathon é Boot 4 |
| Quarkus | Menor familiaridade do time; ecossistema de exemplos menor para o domínio |
| Node/NestJS | Requisito do projeto é Java no backend |

## Consequências
- ✅ Baseline Java 17+ mantida; Java 21 LTS é o alvo de execução.
- ⚠️ Boot 4 traz Jackson 3 e Security 7 com mudanças de API — o time deve conferir as *release notes*.
- ⚠️ Algumas bibliotecas de terceiros ainda não publicaram versão compatível com Boot 4; cada dependência
  nova passa por avaliação no PR.
