#!/usr/bin/env bash
set -euo pipefail
PROJETO="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJETO"
bash scripts/compilar.sh
exec java --enable-native-access=ALL-UNNAMED -cp 'build/classes:lib/*' br.edu.chat.servidor.ServidorChat "$@"
