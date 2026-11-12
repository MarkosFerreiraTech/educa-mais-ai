# ADR-006 — Angular 22 + VMware Clarity, Mobile-First

- **Status:** Aceito
- **Data:** 2026-11-12
- **Decisores:** Tech Lead, UX
- **Requisitos relacionados:** RNF17, RNF18, RNF19, RNF21

## Contexto
O público é adolescente, majoritariamente em celular. O hackathon exige aderência ao design system
VMware Clarity e comportamento responsivo real.

## Decisão
Usar **Angular 22** (standalone components, signals, `HttpClient` com interceptor JWT) e
**@clr/angular 18 + @clr/ui + @cds/core**, construindo primeiro em 360 px e escalando até desktop
(Mobile-First). Nenhuma biblioteca visual concorrente (Material/Bootstrap) é permitida.

## Consequências
- ✅ Consistência visual e acessibilidade herdada do design system.
- ✅ Componentes standalone reduzem boilerplate e facilitam testes.
- ⚠️ Clarity 18 acompanha o ciclo do Angular — upgrades do Angular exigem validação de compatibilidade.
- ⚠️ Layouts complexos de dashboard exigem composição manual com grid do Clarity.
