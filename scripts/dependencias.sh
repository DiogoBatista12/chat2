#!/usr/bin/env bash
set -euo pipefail
PROJETO="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJETO"
SQLITE_VERSAO='3.53.4.0'
SQLITE_SHA256='bcb1f51e36f940867e83342f9efbf5968ac44a6bef4d397bb4af7b17b45cd2fb'
ARQUIVO="lib/sqlite-jdbc-${SQLITE_VERSAO}.jar"

validar() {
    local resultado
    if command -v sha256sum > /dev/null 2>&1; then
        resultado="$(sha256sum "$1")"
    else
        resultado="$(shasum -a 256 "$1")"
    fi
    if [[ "${resultado%% *}" != "$SQLITE_SHA256" ]]; then
        echo "Falha na verificação SHA-256 do driver SQLite: $1" >&2
        exit 1
    fi
}

mkdir -p lib
if [[ ! -f "$ARQUIVO" ]]; then
    echo "Baixando o driver SQLite JDBC ${SQLITE_VERSAO} do Maven Central..."
    curl -fSL --retry 2 --connect-timeout 10 --max-time 120 \
        "https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/${SQLITE_VERSAO}/sqlite-jdbc-${SQLITE_VERSAO}.jar" \
        --output "${ARQUIVO}.part"
    validar "${ARQUIVO}.part"
    mv -- "${ARQUIVO}.part" "$ARQUIVO"
fi
validar "$ARQUIVO"
