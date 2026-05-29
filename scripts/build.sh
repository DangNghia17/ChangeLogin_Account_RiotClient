#!/usr/bin/env bash
# Development build for Linux/macOS (compile + run-checks only).
# The application targets Windows at runtime (uses Windows APIs), but this script
# lets developers compile and verify the code on any platform.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

JSON_VERSION="20231013"
LIB_DIR="$ROOT/lib"
JSON_JAR="$LIB_DIR/json-$JSON_VERSION.jar"
mkdir -p "$LIB_DIR"

if [ ! -f "$JSON_JAR" ]; then
  echo "Downloading dependency org.json:$JSON_VERSION ..."
  curl -fsSL -o "$JSON_JAR" \
    "https://repo1.maven.org/maven2/org/json/json/$JSON_VERSION/json-$JSON_VERSION.jar"
fi

OUT="$ROOT/build/classes"
rm -rf "$OUT"
mkdir -p "$OUT"

echo "Compiling..."
find src/main/java -name '*.java' > /tmp/ram_sources.txt
javac --release 11 -encoding UTF-8 -cp "$JSON_JAR" -d "$OUT" @/tmp/ram_sources.txt
cp -r src/main/resources/* "$OUT"/

echo "Build OK -> $OUT"
echo "Tip: use scripts/build.ps1 / scripts/package.ps1 on Windows to produce the JAR/EXE."
