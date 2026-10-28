#!/usr/bin/env bash
set -euo pipefail
PROJETO="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJETO"
bash scripts/dependencias.sh
mkdir -p build/classes/banco
find build/classes -type f -name '*.class' -delete
find src/main/java -name '*.java' -print > build/fontes.txt
javac --release 17 -encoding UTF-8 -Xlint:all -d build/classes @build/fontes.txt
cp banco/schema.sql build/classes/banco/schema.sql
jar --create --file build/chat.jar -C build/classes .
echo 'Compilação concluída: build/classes e build/chat.jar'
