#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$REPO_ROOT/.env"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

TARGET_URL="${GRAPH_AGENT_URL:-${PUBLIC_PROTOCOL:-http}://${PUBLIC_HOST:-localhost}:${GRAPH_AGENT_PORT:-${PORT:-8080}}}"

echo "-----------------------------------------------"
echo "STEP 1: Testing Discovery"
echo "-----------------------------------------------"
curl -sS "$TARGET_URL/.well-known/agent-card.json"
echo -e "\n"

echo "-----------------------------------------------"
echo "STEP 2: Testing Action (A2A JSON-RPC)"
echo "-----------------------------------------------"
ACTION_RESPONSE="$(curl -sS -X POST "$TARGET_URL/" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "message/send",
    "params": {
      "message": {
        "kind": "message",
        "messageId": "graph-test-message-1",
        "role": "user",
        "parts": [
          {
            "kind": "text",
            "text": "Show dependencies for SKU-500"
          }
        ]
      }
    },
    "id": 1
  }')"

printf '%s\n' "$ACTION_RESPONSE" | python3 -c '
import json
import sys

payload = json.load(sys.stdin)
if "error" in payload:
    print(json.dumps(payload, indent=2))
    raise SystemExit(0)

result = payload.get("result", {})
artifacts = []
for artifact in result.get("artifacts", []):
    parts = []
    for part in artifact.get("parts", []):
        summary = {"kind": part.get("kind")}
        file_info = part.get("file")
        if isinstance(file_info, dict):
            summary["mimeType"] = file_info.get("mimeType")
            summary["name"] = file_info.get("name")
            if file_info.get("bytes"):
                summary["bytes"] = "<base64 omitted>"
        if part.get("text"):
            summary["text"] = part.get("text")
        parts.append(summary)
    artifacts.append(
        {
            "artifactId": artifact.get("artifactId"),
            "name": artifact.get("name"),
            "parts": parts,
        }
    )

summary = {
    "status": (result.get("status") or {}).get("state"),
    "contextId": result.get("contextId"),
    "artifacts": artifacts,
}

status_message = (result.get("status") or {}).get("message") or {}
if status_message.get("parts"):
    summary["statusMessageParts"] = status_message["parts"]

print(json.dumps(summary, indent=2))
'
echo
