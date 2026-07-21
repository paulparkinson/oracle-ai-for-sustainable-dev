#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

# shellcheck disable=SC1091
source ./load-database-env.sh

if [ "${APP_DATA_MODE:-mcp}" = "mcp" ] && [ -z "${ORACLE_MCP_TOOLKIT_JAR:-}" ]; then
  ORACLE_MCP_TOOLKIT_JAR="$(./prepare-mcp-toolkit.sh)"
  export ORACLE_MCP_TOOLKIT_JAR
fi

if command -v mvn >/dev/null 2>&1; then
  maven_command="mvn"
elif [ -x /opt/homebrew/opt/maven/bin/mvn ]; then
  maven_command="/opt/homebrew/opt/maven/bin/mvn"
else
  echo "Maven 3.9+ is required." >&2
  exit 1
fi

"$maven_command" -q -DskipTests package
java -Dweb.root=../web-client -jar target/interactive-ai-agent-service-0.1.0-SNAPSHOT.jar
