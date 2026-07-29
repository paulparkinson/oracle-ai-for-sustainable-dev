#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${MEMORY_PYTHON_BASE_URL:-http://127.0.0.1:${MEMORY_PYTHON_PORT:-8092}}"

curl -fsS "${BASE_URL}/api/health"
for action in reset retain recall correct expire dream approve next-day; do
  curl -fsS \
    -H 'Content-Type: application/json' \
    -d '{}' \
    "${BASE_URL}/api/actions/${action}"
done
curl -fsS "${BASE_URL}/api/state"
printf '\nPython Oracle AI Agent Memory smoke test passed.\n'
