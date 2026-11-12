#!/usr/bin/env bash
# =============================================================================
# ci-doctor.sh - fecha o laco de feedback do CI sem depender dos logs brutos.
#
# Le as annotations (::error) publicadas pelo workflow backend-ci.yml e imprime
# apenas as linhas relevantes. Uso:
#   ./scripts/ci-doctor.sh            # ultima execucao
#   ./scripts/ci-doctor.sh <run-id>   # execucao especifica
# =============================================================================
set -euo pipefail

REPO="${REPO:-MarkosFerreiraTech/educa-mais-ai}"
RUN_ID="${1:-}"

if [[ -z "$RUN_ID" ]]; then
  RUN_ID=$(gh api "repos/$REPO/actions/runs" --jq '.workflow_runs[0].id')
fi

echo "== Execucao: $RUN_ID"
gh api "repos/$REPO/actions/runs/$RUN_ID" --jq '"status=\(.status) conclusion=\(.conclusion) attempt=\(.run_attempt)"'

JOB_ID=$(gh api "repos/$REPO/actions/runs/$RUN_ID/jobs" --jq '.jobs[0].id')
[[ -z "$JOB_ID" || "$JOB_ID" == "null" ]] && { echo "Nenhum job encontrado (workflow invalido?)"; exit 1; }

echo "== Etapas"
gh api "repos/$REPO/actions/runs/$RUN_ID/jobs" --jq '.jobs[0].steps[] | "\(.conclusion)\t\(.name)"' || true

echo "== Diagnostico"
gh api "repos/$REPO/check-runs/$JOB_ID/annotations" \
  --jq '.[] | select(.annotation_level == "failure") | .message' | head -60
