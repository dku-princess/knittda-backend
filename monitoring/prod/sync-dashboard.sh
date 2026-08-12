#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
# knittda 대시보드를 Grafana Cloud 에 동기화 (dashboards-as-code).
#
# 원칙: git 의 knittda.json 이 원본(source of truth). UI 에서 직접 수정 금지(drift 방지).
#       수정은 항상 JSON → 이 스크립트로 push. uid(knittda-overview) 기준 upsert.
#
# 사용:
#   GRAFANA_TOKEN=glsa_xxx ./monitoring/prod/sync-dashboard.sh
#   GRAFANA_TOKEN=glsa_xxx GRAFANA_FOLDER_UID=abc ./monitoring/prod/sync-dashboard.sh path/to/dash.json
#   DRY_RUN=1 GRAFANA_TOKEN=x ./monitoring/prod/sync-dashboard.sh   # POST 없이 payload 만 출력
#
# 필요 키: GRAFANA_TOKEN = Grafana Cloud "서비스 계정 토큰"(glsa_...), Editor 권한.
#   생성: Grafana Cloud → Administration → Users and access → Service accounts
#         → Add service account(role: Editor) → Add token → glsa_... 복사.
#   ⚠️ M2 의 GRAFANA_CLOUD_TOKEN(metrics:write, remote_write용)과 다른 토큰이다.
#   토큰은 절대 커밋하지 말 것(.env / CI Secret 로 주입).
# ─────────────────────────────────────────────────────────────
set -euo pipefail

GRAFANA_URL="${GRAFANA_URL:-https://tteuda.grafana.net}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DASHBOARD_FILE="${1:-$SCRIPT_DIR/../local/grafana/dashboards/knittda.json}"
FOLDER_UID="${GRAFANA_FOLDER_UID:-}"
MESSAGE="${GRAFANA_MESSAGE:-synced from git ($(date +%F\ %T))}"

command -v jq >/dev/null || { echo "ERROR: jq 가 필요합니다 (brew install jq)." >&2; exit 1; }
[[ -f "$DASHBOARD_FILE" ]] || { echo "ERROR: 대시보드 파일 없음: $DASHBOARD_FILE" >&2; exit 1; }
if [[ -z "${GRAFANA_TOKEN:-}" ]]; then
  echo "ERROR: GRAFANA_TOKEN 이 필요합니다 (Grafana Cloud 서비스 계정 토큰 glsa_...)." >&2
  exit 1
fi

# {dashboard: <model, id=null>, overwrite:true, message, folderUid?}
# id=null 로 두면 uid 기준 생성/갱신(upsert). folderUid 없으면 General 폴더.
payload="$(jq -c \
  --arg msg "$MESSAGE" \
  --arg folder "$FOLDER_UID" \
  '{dashboard: (. + {id: null}), overwrite: true, message: $msg}
   + (if $folder == "" then {} else {folderUid: $folder} end)' \
  "$DASHBOARD_FILE")"

uid="$(jq -r '.uid' "$DASHBOARD_FILE")"
echo "→ 대시보드 uid=$uid  →  $GRAFANA_URL  (folder=${FOLDER_UID:-General})"

if [[ "${DRY_RUN:-}" == "1" ]]; then
  echo "$payload" | jq '{overwrite, message, folderUid, dashboard: {uid: .dashboard.uid, title: .dashboard.title, panels: (.dashboard.panels|length)}}'
  echo "(DRY_RUN — POST 생략)"
  exit 0
fi

resp="$(curl -sS -w '\n%{http_code}' -X POST "$GRAFANA_URL/api/dashboards/db" \
  -H "Authorization: Bearer $GRAFANA_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$payload")"
code="$(echo "$resp" | tail -n1)"
body="$(echo "$resp" | sed '$d')"

if [[ "$code" == "200" ]]; then
  echo "$body" | jq -r '"✅ 동기화 완료: status=\(.status) version=\(.version)  \(.url)"'
else
  echo "❌ 실패 (HTTP $code):" >&2
  echo "$body" | jq . 2>/dev/null || echo "$body" >&2
  exit 1
fi

# ── 알림 룰은 별도 (Prometheus YAML → Grafana Cloud Mimir ruler) ──
# mimirtool 사용 예 (참고, 이 스크립트 범위 밖):
#   mimirtool rules load monitoring/prod/alerting/knittda-alerts.yml \
#             monitoring/prod/alerting/knittda-business-alerts.yml \
#     --address="$GRAFANA_URL" --id=<tenant> --key="$GRAFANA_TOKEN"
