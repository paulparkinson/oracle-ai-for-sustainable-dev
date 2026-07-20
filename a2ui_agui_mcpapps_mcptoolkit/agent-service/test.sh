#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
mkdir -p target/test-classes
javac -d target/test-classes $(find src/main/java src/test/java -name '*.java' -print)
java -cp target/test-classes com.oracle.demo.interactiveai.ReferenceTests
