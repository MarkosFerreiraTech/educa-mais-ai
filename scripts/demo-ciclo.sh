#!/usr/bin/env bash
# =============================================================================
# demo-ciclo.sh - dispara o ciclo autonomo completo contra a API local.
#
# Pre-requisitos:
#   1. API rodando com o perfil demo (makes api-run ou docker compose up)
#   2. curl e openssl instalados
#
# O script cadastra um aluno, registra uma falta assinada (HMAC) na aula de
# fracoes semeada, aguarda a missao ser planejada e imprime missao, notificacoes
# e gamificacao. Nenhum dado real de aluno e usado.
# =============================================================================
set -euo pipefail

API="${API:-http://localhost:8080/api/v1}"
API_KEY="${SCHOOL_API_KEY:-dev-school-api-key}"
HMAC_SECRET="${SCHOOL_HMAC_SECRET:-dev-school-hmac-secret}"
LESSON_ID="${LESSON_ID:-33333333-3333-3333-3333-333333333333}"
# Escola e turma de demonstracao: sem estes IDs o cadastro viola a FK de students
# (school_id/classroom_id). Os UUIDs vem das migrations V2 (esquema de referencia).
SCHOOL_ID="${SCHOOL_ID:-11111111-1111-1111-1111-111111111111}"
CLASSROOM_ID="${CLASSROOM_ID:-22222222-2222-2222-2222-222222222222}"
EMAIL="aluno.demo+$(date +%s)@escola.br"
PASSWORD='Senha@2026'

say() { printf '\n\033[36m== %s\033[0m\n' "$1"; }

say "1/5 Cadastrando aluno ($EMAIL)"
REGISTER=$(curl -sS -X POST "$API/auth/register" -H 'Content-Type: application/json' -d @- <<JSON
{"name":"Ana Demo","email":"$EMAIL","phone":"+5581999998888","birthDate":"2012-04-10",
 "password":"$PASSWORD","interests":["futebol","games"],"whatsAppOptIn":true,
 "schoolId":"$SCHOOL_ID","classroomId":"$CLASSROOM_ID","guardianConsent":"ACEITE"}
JSON
)
TOKEN=$(printf '%s' "$REGISTER" | python3 -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])')
STUDENT_ID=$(curl -sS "$API/students/me" -H "Authorization: Bearer $TOKEN" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')
echo "aluno: $STUDENT_ID"

say "2/5 Enviando webhook de falta assinado (HMAC-SHA256)"
EVENT_ID="evt-demo-$(date +%s)"
TIMESTAMP=$(date +%s)
CANONICAL="$EVENT_ID|$STUDENT_ID|$LESSON_ID"
SIGNATURE=$(printf '%s' "$TIMESTAMP.$CANONICAL" | openssl dgst -sha256 -hmac "$HMAC_SECRET" -hex | sed 's/^.*= //')
curl -sS -X POST "$API/integrations/school/attendance" \
  -H 'Content-Type: application/json' \
  -H "X-Api-Key: $API_KEY" -H "X-Signature: $SIGNATURE" -H "X-Timestamp: $TIMESTAMP" \
  -d "{\"eventId\":\"$EVENT_ID\",\"studentId\":\"$STUDENT_ID\",\"lessonId\":\"$LESSON_ID\",\"reason\":\"Falta demo\"}"
echo

say "3/5 Aguardando o agente planejar a missao"
for _ in $(seq 1 20); do
  MISSIONS=$(curl -sS "$API/missions" -H "Authorization: Bearer $TOKEN")
  COUNT=$(printf '%s' "$MISSIONS" | python3 -c 'import json,sys; print(len(json.load(sys.stdin)))')
  [ "$COUNT" -ge 1 ] && break
  sleep 1
done
printf '%s' "$MISSIONS" | python3 -m json.tool | head -40

say "4/5 Notificacoes (in-app + WhatsApp)"
curl -sS "$API/notifications" -H "Authorization: Bearer $TOKEN" | python3 -m json.tool | head -30

say "5/5 Gamificacao inicial"
curl -sS "$API/students/me/gamification" -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

printf '\n\033[32mCiclo concluido. Token do aluno: %s\033[0m\n' "${TOKEN:0:32}..."
printf 'Abra http://localhost:4200 e entre com %s / %s\n' "$EMAIL" "$PASSWORD"
