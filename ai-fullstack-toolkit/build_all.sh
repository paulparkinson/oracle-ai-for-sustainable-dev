#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

echo "Building and testing AI Fullstack Toolkit..."
mvn clean package

echo
echo "Build complete. Runtime JAR: runtime/target/ai-fullstack-runtime-0.1.0-SNAPSHOT.jar"
