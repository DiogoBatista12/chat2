#!/usr/bin/env bash
set -euo pipefail
PROJETO="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJETO"
bash scripts/compilar.sh
exec java -cp build/classes br.edu.chat.cliente.ClienteChat "$@"
