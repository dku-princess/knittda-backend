#!/usr/bin/env bash
# knittda — 배포 중 오탐 방지용 임시 Silence 생성기 (수동 배포 직전에 실행)
#
# 왜: Blue/Green 전환·재배포 시 컨테이너가 잠깐 죽어 TargetDown / Http5xxErrors(critical)가
#     오탐으로 발화 → @here 페이지. 배포 직전에 '시간제한 silence' 를 걸면 그 시간 동안만 무음.
#     자동 만료라 해제를 잊어도 시간이 지나면 알림이 되살아난다.
#
# 사전 준비: Grafana 서비스 계정 토큰(alerting 쓰기 권한).
#   Grafana Cloud → Administration → Users and access → Service accounts
#     → Add service account (Role: Editor) → Add token → 값 복사
#     → 환경변수 GRAFANA_TOKEN 로 전달 (토큰 값은 코드/깃에 넣지 말 것).
#
# 사용:
#   GRAFANA_TOKEN=xxx ./deploy-silence.sh              # 20분, env=prod 의 TargetDown|Http5xxErrors 무음
#   GRAFANA_TOKEN=xxx ./deploy-silence.sh 30           # 30분
#   GRAFANA_TOKEN=xxx ./deploy-silence.sh 30 green     # 30분 + slot=green 한정
#   조기 해제: Grafana → Alerting → Silences → 해당 항목 Expire.
#
# 권장 흐름:  ./deploy-silence.sh 20   →  (배포/전환 수행)  →  자동 만료.
set -euo pipefail

GRAFANA_URL="${GRAFANA_URL:-https://tteuda.grafana.net}"
: "${GRAFANA_TOKEN:?GRAFANA_TOKEN(서비스계정 토큰, alerting 쓰기) 환경변수가 필요합니다}"

MINUTES="${1:-20}"
SLOT="${2:-}"

# 시작/종료 시각(UTC, RFC3339) — 이식성 위해 python3 로 계산
read -r START END <<<"$(python3 - "$MINUTES" <<'PY'
import sys, datetime
m=int(sys.argv[1])
now=datetime.datetime.now(datetime.timezone.utc)
end=now+datetime.timedelta(minutes=m)
f="%Y-%m-%dT%H:%M:%S.000Z"
print(now.strftime(f), end.strftime(f))
PY
)"

# matchers: env=prod AND alertname=~TargetDown|Http5xxErrors (배포 오탐만 무음, Heap/external 은 유지)
MATCHERS='[{"name":"env","value":"prod","isRegex":false,"isEqual":true},{"name":"alertname","value":"TargetDown|Http5xxErrors","isRegex":true,"isEqual":true}'
if [ -n "$SLOT" ]; then
  MATCHERS="$MATCHERS,{\"name\":\"slot\",\"value\":\"$SLOT\",\"isRegex\":false,\"isEqual\":true}"
fi
MATCHERS="$MATCHERS]"

BODY="{\"matchers\":$MATCHERS,\"startsAt\":\"$START\",\"endsAt\":\"$END\",\"createdBy\":\"deploy-silence.sh\",\"comment\":\"manual deploy silence (${MINUTES}m)\"}"

RESP=$(curl -sS -X POST \
  "$GRAFANA_URL/api/alertmanager/grafana/api/v2/silences" \
  -H "Authorization: Bearer $GRAFANA_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$BODY")

ID=$(printf '%s' "$RESP" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("silenceID",""))' 2>/dev/null || true)
if [ -n "$ID" ]; then
  echo "✅ silence 생성: id=$ID"
  echo "   대상: env=prod, alertname=~TargetDown|Http5xxErrors${SLOT:+, slot=$SLOT}"
  echo "   만료: $END (UTC, ${MINUTES}분 후 자동 해제)"
else
  echo "⚠️ silenceID 없음 — 응답: $RESP"
  echo "   토큰 권한(alerting 쓰기)·GRAFANA_URL 확인."
  exit 1
fi
