#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
mkdir -p target/classes
javac -d target/classes $(find src/main/java -name '*.java' -print)
java -Dweb.root=../web-client -cp target/classes com.oracle.demo.interactiveai.Main
