#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="${APP_DIR}/server"

# shellcheck disable=SC1091
source "${APP_DIR}/load-database-env.sh"

if [[ -z "${JAVA_HOME:-}" ]] && [[ -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  export JAVA_HOME
fi

if [[ -z "${JAVA_HOME:-}" ]] || [[ ! -x "${JAVA_HOME}/bin/java" ]]; then
  echo "Java 21 is required. Set JAVA_HOME to a Java 21 installation." >&2
  exit 1
fi

MAVEN_BIN="${MAVEN_BIN:-mvn}"
if ! command -v "${MAVEN_BIN}" >/dev/null 2>&1 \
    && [[ -x /opt/homebrew/opt/maven/bin/mvn ]]; then
  MAVEN_BIN="/opt/homebrew/opt/maven/bin/mvn"
fi

(
  cd "${SERVER_DIR}"
  "${MAVEN_BIN}" -q -DskipTests package
  export MEMORY_WEB_ROOT="${APP_DIR}/web"
  "${JAVA_HOME}/bin/java" -jar target/memories-are-the-magic-0.1.0-SNAPSHOT.jar
)
