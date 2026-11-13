# Design system — Educa Mais (identidade lúdica)

Este documento descreve o sistema visual da SPA e **como ele se relaciona com o Clarity**.
Ele existe para que a identidade "gamificada" do mockup não vire dívida técnica: cada regra
tem um motivo, e a acessibilidade é verificável.

## 1. Fonte da verdade

- **Tokens**: `frontend/src/styles.scss` (bloco `:root`). Toda cor/raio/sombra vem de lá —
  componentes **não** declaram hex soltos, exceto ilustrações SVG (mascote e avatar).
- **Componentes de marca**: `shared/components/orbi.ts` (mascote), `shared/components/avatar.ts`
  (avatar em camadas que reflete os itens da loja), `shared/layout/app-shell.ts` (navegação).

## 2. Tokens

| Grupo | Token | Valor | Uso |
| --- | --- | --- | --- |
| Marca | `--primary` | `#58CC02` | realces, preenchimentos de progresso |
| | `--primary-strong` | `#2B6F00` | superfície do botão primário (texto branco 6.2:1) |
| | `--primary-ink` | `#2A6E00` | texto verde sobre superfícies claras (5.6:1 em `--primary-soft`) |
| | `--secondary` / `--secondary-ink` | `#1CB0F6` / `#0F6E9C` | destaque azul e texto azul (5.6:1) |
| | `--accent-purple` / `--accent-purple-ink` | `#CE82FF` / `#7B33C6` | cards de personalização (5.9:1) |
| | `--gold` / `--gold-ink` | `#FFC800` / `#7A5800` | moedas, avisos (6.0:1) |
| | `--diamond` / `--diamond-ink` | `#00CD9C` / `#00775C` | diamantes (5.0:1) |
| | `--danger` / `--danger-ink` | `#FF4B4B` / `#B32020` | erros (5.8:1) |
| Superfícies | `--bg-page`, `--bg-card`, `--bg-soft` | `#F4F7FA`, `#FFFFFF`, `#F8FAFC` | fundo, cards, campos |
| Texto | `--text-main`, `--text-muted` | `#1E293B`, `#64748B` | 14.6:1 e 4.8:1 |
| Bordas | `--border-light`, `--border-3d` | `#E2E8F0`, `#CBD5E1` | contorno e "pé" do botão 3D |
| Formas | `--radius-sm/md/lg/pill` | `10 / 14 / 20 / 999px` | chips, cards, pílulas |
| Profundidade | `--shadow-3d`, `--shadow-card` | `0 4px 0`, `0 2px 0` | botões e cards |

**Regra de contraste.** O verde/azul "cheios" (`#58CC02`, `#1CB0F6`) têm contraste < 4.5:1 com
branco; por isso eles são usados **como superfície decorativa** e o texto sobre eles é branco em
peso ≥ 800 e tamanho ≥ 15 px (critério de "texto grande" da WCAG). Texto colorido sempre usa a
variante `-ink`. Script de conferência: qualquer par novo precisa chegar a 4.5:1.

## 3. Botões 3D

```css
.box-3d { background: var(--primary-strong); box-shadow: 0 4px 0 var(--primary-strong-shadow); }
.box-3d:active { transform: translateY(3px); box-shadow: 0 1px 0 var(--primary-strong-shadow); }
```

O `:active` "afunda" o botão em vez de apenas mudar a cor — é o gesto do mockup e funciona em
touch. `prefers-reduced-motion` desliga as animações do mascote (o `translateY` permanece, pois
é feedback de estado, não decoração).

## 4. Relação com o Clarity 18

- O Clarity continua sendo carregado (`@clr/ui`, `@cds/core`) e é usado nos formulários
  (`clr-input-container`, `clr-password-container`, `clr-alert`), que trazem validação,
  `aria-describedby` e mensagens de erro prontas.
- Nós sobrescrevemos **apenas aparência** (cor, raio, altura, tipografia) — nunca comportamento.
- `--cds-alias-primary` aponta para `--primary-dark`, então componentes nativos do Clarity
  (spinners, links, foco) acompanham a paleta.
- Telas sem superfície de formulário (jornada, missão, loja) usam apenas marcação própria, o que
  reduz o CSS do Clarity efetivamente aplicado.
- Manter ambos é uma decisão consciente do [ADR-006](../adr/ADR-006-angular-clarity.md); trocar
  de biblioteca de UI é uma decisão de arquitetura, não de estilo.

## 5. Acessibilidade

- Foco visível global: `:focus-visible` com `3px solid var(--secondary)`.
- `aria-current="page"` nas duas navegações (mobile e desktop) via `ariaCurrentWhenActive`.
- Alvos de toque ≥ 44 px (`min-height: 2.75rem` nos botões, 40 px nos ícones do topo).
- O mascote é decorativo por padrão (`aria-hidden` quando ao lado de texto) ou tem
  `aria-label` descritivo quando é a única informação da área.
- Selects/radios mantêm `fieldset` + `legend` (visualmente ocultos no quiz, presentes no DOM).
- Tabelas do professor mantêm `caption` e `scope="col"`.

## 6. Como estender

1. Adicione o token em `styles.scss` (`:root`), não no componente.
2. Se for texto colorido, crie/usе a variante `-ink` e confira 4.5:1.
3. Reuse `.em-card`, `.em-badge`, `.em-stat`, `.btn` antes de inventar classe nova.
4. Rode `npm test` — `app.smoke.spec.ts` mostra se a tela ainda monta e o shell tem as duas
   navegações.
