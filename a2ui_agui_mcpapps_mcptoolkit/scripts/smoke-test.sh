#!/usr/bin/env bash
set -euo pipefail

base_url="${DEMO_BASE_URL:-http://127.0.0.1:8080}"
curl --fail --silent --show-error "$base_url/api/health" | grep -q '"status":"UP"'
run_file="$(mktemp)"
reject_file="$(mktemp)"
trap 'rm -f "$run_file" "$reject_file"' EXIT

curl --fail --silent --show-error -X POST "$base_url/api/runs" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data 'minimumRisk=90&maximumRows=2' > "$run_file"
grep -q '"type":"RUN_FINISHED"' "$run_file"
approval_id="$(sed -n 's/.*"approvalId":"\([^"]*\)".*/\1/p' "$run_file" | head -1)"
test -n "$approval_id"
curl --fail --silent --show-error -X POST "$base_url/api/approve" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode "approvalId=$approval_id" \
  --data-urlencode 'customerId=1' \
  --data-urlencode 'actionType=REVIEW' \
  --data-urlencode 'actionNotes=Review current risk evidence' | grep -q '"status":"APPROVED"'

curl --fail --silent --show-error -X POST "$base_url/api/runs" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data 'minimumRisk=90&maximumRows=2' > "$reject_file"
approval_id="$(sed -n 's/.*"approvalId":"\([^"]*\)".*/\1/p' "$reject_file" | head -1)"
curl --fail --silent --show-error -X POST "$base_url/api/reject" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode "approvalId=$approval_id" | grep -q '"status":"REJECTED"'
echo "Smoke test passed for $base_url"
