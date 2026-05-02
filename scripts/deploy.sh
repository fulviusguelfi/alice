#!/usr/bin/env bash
# scripts/deploy.sh — Compila o mod e copia o JAR para test-server/mods/
# Uso: ./scripts/deploy.sh [--skip-build]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$SCRIPT_DIR/.."
MOD_DIR="$ROOT/mod"
MODS_DIR="$ROOT/test-server/mods"

SKIP_BUILD=false
for arg in "$@"; do
  [[ "$arg" == "--skip-build" ]] && SKIP_BUILD=true
done

echo "[deploy] Projeto Alice — deploy para test-server"

if [[ "$SKIP_BUILD" == false ]]; then
  echo "[deploy] Compilando mod..."
  cd "$MOD_DIR"
  ./gradlew build --info 2>&1 | tail -20
  cd "$ROOT"
fi

JAR=$(find "$MOD_DIR/build/libs" -name "alice-*.jar" ! -name "*-sources.jar" | sort -V | tail -1)
if [[ -z "$JAR" ]]; then
  echo "[deploy] ERRO: JAR não encontrado em $MOD_DIR/build/libs/"
  exit 1
fi

echo "[deploy] JAR: $JAR"

# Remove versão antiga
OLD_JARS=$(find "$MODS_DIR" -name "alice-*.jar" 2>/dev/null || true)
if [[ -n "$OLD_JARS" ]]; then
  echo "[deploy] Removendo JAR antigo..."
  rm -f "$MODS_DIR"/alice-*.jar
fi

cp "$JAR" "$MODS_DIR/"
echo "[deploy] Copiado para $MODS_DIR/$(basename "$JAR")"
echo "[deploy] Pronto. Reinicie o test-server para carregar a nova versão."
