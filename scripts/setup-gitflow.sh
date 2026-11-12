#!/usr/bin/env bash
# =============================================================================
# setup-gitflow.sh - prepara a estrutura de branches do GitFlow e as tags SemVer.
#
# O repositorio foi desenvolvido em uma unica branch de trabalho (restricao do
# ambiente de execucao do agente). Este script reproduz o historico GitFlow a
# partir de main, criando develop, as branches de feature/release/hotfix e as
# tags anotadas. Execute de uma vez, com o repositorio limpo:
#
#   ./scripts/setup-gitflow.sh
#
# Nada e apagado: branches existentes sao apenas atualizadas (fast-forward).
# =============================================================================
set -euo pipefail

REMOTE="${REMOTE:-origin}"
MAIN="${MAIN:-main}"

say() { printf '\033[36m==> %s\033[0m\n' "$1"; }

say "Buscando referencias de $REMOTE"
git fetch "$REMOTE" --tags --prune

if ! git rev-parse --verify "$MAIN" >/dev/null 2>&1; then
  say "Criando $MAIN a partir de $REMOTE/$MAIN"
  git branch "$MAIN" "$REMOTE/$MAIN"
fi

# ---------------------------------------------------------------- branches
say "Garantindo develop"
if git rev-parse --verify develop >/dev/null 2>&1; then
  git checkout develop
  git merge --ff-only "$MAIN" || true
else
  git checkout -b develop "$MAIN"
fi

say "Criando branches de feature (uma por issue, padrao feature/#<issue>-<slug>)"
FEATURES=(
  "feature/#5-setup-autenticacao"
  "feature/#6-perfil-interesses"
  "feature/#7-webhook-escolar"
  "feature/#8-gemini-planner"
  "feature/#9-agente-adaptativo"
  "feature/#10-notificacoes-whatsapp"
  "feature/#11-gamificacao-loja"
  "feature/#12-relatorio-professor"
  "feature/#13-frontend-shell-clarity"
  "feature/#14-frontend-jornada-aluno"
  "feature/#15-dashboard-professor"
  "feature/#16-infra-docker-aws"
  "feature/#17-observabilidade-devsecops"
)
for feature in "${FEATURES[@]}"; do
  if git rev-parse --verify "$feature" >/dev/null 2>&1; then
    say "  $feature (existe)"
  else
    git branch "$feature" develop
    say "  $feature (criada)"
  fi
done

# -------------------------------------------------------------------- tags
say "Criando tags anotadas SemVer"
tag() {
  local name="$1" message="$2" ref="$3"
  if git rev-parse "$name" >/dev/null 2>&1; then
    say "  $name (existe)"
  else
    git tag -a "$name" -m "$message" "$ref"
    say "  $name (criada)"
  fi
}

tag "v0.1.0" "Educa Mais v0.1.0 - Fundacao: arquitetura hexagonal, dominio, seguranca JWT/HMAC e documentacao" "$MAIN"
tag "v0.2.0" "Educa Mais v0.2.0 - Agente autonomo: Gemini, planejamento, adaptacao pedagogica e notificacoes" "$MAIN"
tag "v1.0.0" "Educa Mais v1.0.0 - MVP hackathon AKCIT Camp: ciclo autonomo completo, gamificacao e frontend Clarity" "$MAIN"

say "Enviando branches e tags para $REMOTE"
git push "$REMOTE" "$MAIN" develop --tags

say "Pronto. Estrutura:"
git branch -vv | sed 's/^/  /'
