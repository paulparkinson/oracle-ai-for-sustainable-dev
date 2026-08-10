#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SOURCE_REPOSITORY="https://github.com/oracle-devrel/oracle-autonomous-database-samples.git"
SOURCE_COMMIT="7c61fc86ffb7f0f548bdba32ae53ce46ea876fa2"
SOURCE_SUBDIR="google-gemini-marketplace-agents/oracle_ai_database_agent"
DESTINATION="$REPO_ROOT/.runtime/oracle-ai-database-agent-official-${SOURCE_COMMIT:0:12}"

command -v git >/dev/null 2>&1 || {
  echo "git is required." >&2
  exit 1
}
command -v sha256sum >/dev/null 2>&1 || {
  echo "sha256sum is required." >&2
  exit 1
}

if [[ ! -d "$DESTINATION/.git" ]]; then
  if [[ -e "$DESTINATION" ]]; then
    echo "Refusing to overwrite non-repository path: $DESTINATION" >&2
    exit 1
  fi
  mkdir -p "$(dirname "$DESTINATION")"
  git clone --filter=blob:none --no-checkout "$SOURCE_REPOSITORY" "$DESTINATION"
  git -C "$DESTINATION" sparse-checkout init --cone
  git -C "$DESTINATION" sparse-checkout set "$SOURCE_SUBDIR"
  git -C "$DESTINATION" checkout --detach "$SOURCE_COMMIT"
fi

if [[ "$(git -C "$DESTINATION" rev-parse HEAD)" != "$SOURCE_COMMIT" ]]; then
  echo "Existing official-sample checkout is not at the pinned commit." >&2
  exit 1
fi

SOURCE_DIR="$DESTINATION/$SOURCE_SUBDIR"
(
  cd "$SOURCE_DIR"
  printf '%s  %s\n' \
    'e1cb47df96bc85601c83159ec1a2fe364d86ee5d692f7cbbe5521fbcea550b35' \
    'oracle_ai_database_agent.sql' \
    'd6a144a736adb1a08dfa7e3cae3c8b46215c90374a7ec34b7a9001a1b48ca54d' \
    'oracle_ai_database_agent_tool.sql' \
    '5cac89bbaa3fe597d12685b3d075a020dc44c16053cc53a1df5d808d4ec57600' \
    'README.md' | sha256sum --check --strict
)

echo "$SOURCE_DIR"
