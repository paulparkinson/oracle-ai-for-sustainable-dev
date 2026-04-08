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

if ! command -v java >/dev/null 2>&1; then
  echo "Java is required but was not found on PATH."
  exit 1
fi

cd "$SCRIPT_DIR"

JAR_PATH="$(find "$SCRIPT_DIR/target" -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' | head -n 1)"
if [[ -z "$JAR_PATH" ]]; then
  echo "No built jar found. Running build first..."
  "$SCRIPT_DIR/build.sh"
  JAR_PATH="$(find "$SCRIPT_DIR/target" -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' | head -n 1)"
fi

if [[ -z "$JAR_PATH" ]]; then
  echo "Unable to find a runnable jar under $SCRIPT_DIR/target after build."
  exit 1
fi

GRAPH_PORT="${GRAPH_AGENT_PORT:-${PORT:-8080}}"

echo "Starting Oracle Graph Agent on port $GRAPH_PORT"
exec java -Djava.awt.headless=true -jar "$JAR_PATH" \
  --server.port="$GRAPH_PORT" \
  --server.address="${BIND_HOST:-0.0.0.0}"
