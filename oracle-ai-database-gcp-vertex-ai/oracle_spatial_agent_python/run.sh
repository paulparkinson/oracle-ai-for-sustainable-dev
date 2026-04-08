#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$REPO_ROOT/.env"
VENV_PYTHON="$SCRIPT_DIR/.venv/bin/python"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

if [[ ! -x "$VENV_PYTHON" ]]; then
  echo "No virtual environment found at $SCRIPT_DIR/.venv"
  echo "Run $SCRIPT_DIR/setup_venv.sh first."
  exit 1
fi

export PORT="${SPATIAL_AGENT_PORT:-${PORT:-8080}}"

cd "$SCRIPT_DIR"

echo "Starting Oracle Spatial Agent on port $PORT"
exec "$VENV_PYTHON" main.py
