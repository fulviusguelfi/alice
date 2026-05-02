#!/usr/bin/env bash
# scripts/check-infra.sh — Valida pré-requisitos antes de rodar pytest
# Uso: ./scripts/check-infra.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$SCRIPT_DIR/.."

ERRORS=0

check() {
  local label="$1"
  local ok="$2"
  if [[ "$ok" == "true" ]]; then
    echo "  [OK] $label"
  else
    echo "  [FAIL] $label"
    ERRORS=$((ERRORS + 1))
  fi
}

echo "[check-infra] Validando pré-requisitos do Projeto Alice..."
echo ""

# 1. server.properties — enable-rcon=true
PROPS="$ROOT/test-server/server.properties"
if grep -q "^enable-rcon=true" "$PROPS" 2>/dev/null; then
  check "RCON habilitado em server.properties" "true"
else
  check "RCON habilitado em server.properties (enable-rcon=true)" "false"
fi

# 2. RCON password definida
if grep -q "^rcon.password=" "$PROPS" 2>/dev/null; then
  PASS=$(grep "^rcon.password=" "$PROPS" | cut -d= -f2)
  if [[ -n "$PASS" ]]; then
    check "rcon.password definida" "true"
  else
    check "rcon.password definida (está vazia)" "false"
  fi
else
  check "rcon.password no server.properties" "false"
fi

# 3. JAR presente em test-server/mods/
JAR=$(find "$ROOT/test-server/mods" -name "alice-*.jar" 2>/dev/null | head -1)
if [[ -n "$JAR" ]]; then
  check "JAR alice presente em test-server/mods/ ($(basename "$JAR"))" "true"
else
  check "JAR alice presente em test-server/mods/ (rode scripts/deploy.sh)" "false"
fi

# 4. RCON acessível (test de conexão TCP)
RCON_HOST="${ALICE_RCON_HOST:-127.0.0.1}"
RCON_PORT="${ALICE_RCON_PORT:-25575}"
if timeout 2 bash -c "echo > /dev/tcp/$RCON_HOST/$RCON_PORT" 2>/dev/null; then
  check "RCON porta $RCON_PORT acessível (server-side)" "true"
else
  check "RCON porta $RCON_PORT acessível — servidor não está rodando?" "false"
fi

# 5. API key LLM configurada
CONFIG="$ROOT/test-server/config/alice-common.toml"
if grep -q 'apiKey\s*=\s*"[^"]\+' "$CONFIG" 2>/dev/null; then
  check "LLM apiKey configurada em alice-common.toml" "true"
else
  check "LLM apiKey configurada em alice-common.toml (vazia ou ausente)" "false"
fi

# 6. Python + pytest disponíveis
if command -v python3 >/dev/null 2>&1; then
  check "python3 disponível ($(python3 --version 2>&1))" "true"
else
  check "python3 disponível" "false"
fi

if python3 -m pytest --version >/dev/null 2>&1; then
  check "pytest disponível" "true"
else
  check "pytest disponível (pip install pytest)" "false"
fi

echo ""
if [[ $ERRORS -eq 0 ]]; then
  echo "[check-infra] Tudo OK — pode rodar: cd test-server && pytest tests/ -m 'not slow' -v"
else
  echo "[check-infra] $ERRORS problema(s) encontrado(s). Corrija antes de rodar os testes."
  exit 1
fi
