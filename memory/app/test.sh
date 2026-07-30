#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="${APP_DIR}/server"
LIBRARY_BUILD="${APP_DIR}/../test-utils/build-library.sh"

if [[ -z "${JAVA_HOME:-}" ]] && [[ -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  export JAVA_HOME
fi

MAVEN_BIN="${MAVEN_BIN:-mvn}"
if ! command -v "${MAVEN_BIN}" >/dev/null 2>&1 \
    && [[ -x /opt/homebrew/opt/maven/bin/mvn ]]; then
  MAVEN_BIN="/opt/homebrew/opt/maven/bin/mvn"
fi

"${LIBRARY_BUILD}"
cd "${SERVER_DIR}"
"${MAVEN_BIN}" test
