#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${MEMORY_BASE_URL:-http://127.0.0.1:${MEMORY_PORT:-8091}}"

curl -fsS "${BASE_URL}/api/health"
for action in reset retain recall correct expire dream approve next-day; do
  if [[ "${action}" == "recall" ]]; then
    body='{"guestId":"AVA","query":"Build a rain-safe evening plan"}'
  elif [[ "${action}" == "approve" ]]; then
    body='{"approver":"demo.presenter@example.com"}'
  else
    body='{}'
  fi
  curl -fsS \
    -H 'Content-Type: application/json' \
    -d "${body}" \
    "${BASE_URL}/api/actions/${action}"
done
curl -fsS "${BASE_URL}/api/state"
printf '\nMemory demo smoke test passed.\n'
