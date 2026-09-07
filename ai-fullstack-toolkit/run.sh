#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

runtime_jar="runtime/target/ai-fullstack-runtime-0.1.0-SNAPSHOT.jar"

if [[ ! -f "$runtime_jar" ]]; then
  echo "Runtime JAR not found; building it first..."
  mvn -pl runtime -am package
fi

echo "Starting AI Fullstack Runtime at http://localhost:8080"
exec java -jar "$runtime_jar" "$@"
