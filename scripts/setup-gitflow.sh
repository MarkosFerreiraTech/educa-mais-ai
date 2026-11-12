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
  "feature/#1-dominio-seguranca-jwt"
  "feature/#2-webhook-escolar-hmac"
  "feature/#3-agente-gemini-planejamento"
  "feature/#4-exercicios-adaptativos"
  "feature/#5-notificacoes-in-app-whatsapp"
  "feature/#6-gamificacao-loja"
  "feature/#7-relatorio-professor-dashboard"
  "feature/#8-privacidade-lgpd"
  "feature/#9-devsecops-scanners"
  "feature/#10-infra-docker-authelia-observabilidade"
  "feature/#11-frontend-shell-clarity"
  "feature/#12-frontend-jornada-missao"
  "feature/#13-frontend-dashboard-professor"
  "feature/#14-ci-gitflow-semver"
  "feature/#15-documentacao-charter-requisitos"
  "feature/#16-archunit"
  "feature/#17-testes-integracao-ciclo"
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
say "Criando pull request develop -> main (se a CLI gh estiver disponivel)"
if command -v gh >/dev/null 2>&1 && ! gh pr list --head develop --state open | grep -q develop; then
  gh pr create --base "$MAIN" --head develop \
    --title "release: Educa Mais v1.0.0" \
    --body "Release v1.0.0 — ciclo autonomo completo (ver CHANGELOG/historico de tags)." || true
fi

say "Pronto. Estrutura:"
git branch -vv | sed 's/^/  /'
