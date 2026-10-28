#!/usr/bin/env bash
set -euo pipefail
PROJETO="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJETO"
bash scripts/compilar.sh
mkdir -p build/test-classes
javac --release 17 -encoding UTF-8 -Xlint:all -cp build/classes -d build/test-classes \
  src/test/java/br/edu/chat/demonstracao/DemonstracaoChat.java
exec java --enable-native-access=ALL-UNNAMED -cp 'build/classes:build/test-classes:lib/*' \
  br.edu.chat.demonstracao.DemonstracaoChat
