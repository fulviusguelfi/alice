#!/bin/bash
# Projeto Alice - Benchmark Groq cloud
# Dataset identico ao bench_ollama.sh para comparacao direta
# Roda na maquina de dev (Windows bash) contra api.groq.com
#
# Uso: ./bench_groq.sh
# Saida: ../benchmark/results_groq_<timestamp>.jsonl

set -euo pipefail
export LC_ALL=C LANG=C

# Carrega credenciais
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ALICE_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$ALICE_ROOT/.secrets/llm_keys.env"

OUTDIR="$SCRIPT_DIR"
TS=$(date +%Y%m%d_%H%M%S)
OUT="$OUTDIR/results_groq_${TS}.jsonl"
LOG="$OUTDIR/run_groq_${TS}.log"

MODELS=(
  "llama-3.1-8b-instant"
  "llama-3.3-70b-versatile"
)

PROMPTS_IDS=(
  "P01_saudacao"
  "P02_crafting"
  "P03_comando_direto"
  "P04_multistep"
  "P05_roleplay"
  "P06_mod_especifico"
  "P07_combate_urgente"
  "P08_chitchat"
  "P09_ambiguidade"
  "P10_explicacao_longa"
)

PROMPTS_TEXT=(
  "Oi Alice, tudo bem com voce?"
  "Como eu faco uma bigorna no Minecraft? Lista os ingredientes."
  "Alice, pega 3 blocos de madeira de carvalho e traz pra mim."
  "Como construir um portal do nether do zero? Explica passo a passo o que precisa de recurso e o que precisa de estrutura."
  "Voce e a Alice, uma sobrevivente ruiva especialista em Minecraft pos-apocaliptico. Eu sou um amigo seu. Me cumprimenta de forma natural e me pergunta como foi meu dia."
  "No mod Cursed Walking tem um item chamado 'cursed compass'. Voce sabe pra que serve? Se nao souber, diz que nao sabe."
  "ALICE CORRE TEM UM CREEPER ATRAS DE VOCE"
  "Que tipo de musica voce gostaria de ouvir enquanto a gente constroi a base?"
  "Constroi uma casa pra mim."
  "Me explica em detalhes como funciona o sistema de redstone do Minecraft, incluindo os principais componentes, como eles interagem, e da exemplos de circuitos uteis para sobrevivencia."
)

ROUNDS=3
SYSTEM="Voce e a Alice, uma jogadora IA companheira em um servidor Minecraft com o modpack Cursed Walking. Voce e ruiva, sobrevivente, direta e prestativa. Responde sempre em portugues do Brasil."

echo "[bench-groq] starting at $(date)" | tee "$LOG"
echo "[bench-groq] models: ${MODELS[*]}" | tee -a "$LOG"
echo "[bench-groq] output: $OUT" | tee -a "$LOG"

# ============ VALIDACAO PREVIA ============
echo "[bench-groq] validating API key..." | tee -a "$LOG"
VAL_BODY_FILE=$(mktemp)
VAL_STATUS=$(curl -s -o "$VAL_BODY_FILE" -w '%{http_code}' -X POST "$GROQ_BASE_URL/chat/completions" \
  -H "Authorization: Bearer $GROQ_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"llama-3.1-8b-instant","messages":[{"role":"user","content":"ping"}],"max_tokens":5}')

if [ "$VAL_STATUS" != "200" ]; then
  echo "[bench-groq] FATAL: validation failed (HTTP $VAL_STATUS)" | tee -a "$LOG"
  cat "$VAL_BODY_FILE" | tee -a "$LOG"
  rm -f "$VAL_BODY_FILE"
  exit 1
fi
echo "[bench-groq] API OK (HTTP 200)" | tee -a "$LOG"
echo "[bench-groq] validation response: $(cat "$VAL_BODY_FILE" | jq -r '.choices[0].message.content // "N/A"')" | tee -a "$LOG"
rm -f "$VAL_BODY_FILE"

# ============ BENCHMARK ============
for M in "${MODELS[@]}"; do
  for i in "${!PROMPTS_IDS[@]}"; do
    PID="${PROMPTS_IDS[$i]}"
    PTEXT="${PROMPTS_TEXT[$i]}"
    for R in $(seq 1 $ROUNDS); do
      echo "[bench-groq] $M | $PID | round $R" | tee -a "$LOG"

      PAYLOAD=$(jq -n \
        --arg model "$M" \
        --arg sys "$SYSTEM" \
        --arg user "$PTEXT" \
        '{model:$model, temperature:0.7, max_tokens:300, messages:[{role:"system",content:$sys},{role:"user",content:$user}]}')

      START_NS=$(date +%s%N)
      RESP=$(curl -s -X POST "$GROQ_BASE_URL/chat/completions" \
        -H "Authorization: Bearer $GROQ_API_KEY" \
        -H "Content-Type: application/json" \
        -d "$PAYLOAD")
      END_NS=$(date +%s%N)
      WALL_MS=$(( (END_NS - START_NS) / 1000000 ))

      # Groq retorna metricas nativas em x_groq.usage (tempo de prompt, completion, queue, etc)
      PROMPT_TOKENS=$(echo "$RESP" | jq -r '.usage.prompt_tokens // 0')
      COMPLETION_TOKENS=$(echo "$RESP" | jq -r '.usage.completion_tokens // 0')
      TOTAL_TIME=$(echo "$RESP" | jq -r '.usage.total_time // 0')  # segundos
      COMPLETION_TIME=$(echo "$RESP" | jq -r '.usage.completion_time // 0')
      PROMPT_TIME=$(echo "$RESP" | jq -r '.usage.prompt_time // 0')
      QUEUE_TIME=$(echo "$RESP" | jq -r '.usage.queue_time // 0')
      CONTENT=$(echo "$RESP" | jq -r '.choices[0].message.content // ""')

      if awk "BEGIN { exit !($COMPLETION_TIME > 0) }"; then
        TPS=$(awk "BEGIN { printf \"%.2f\", $COMPLETION_TOKENS / $COMPLETION_TIME }")
      else
        TPS="0"
      fi

      # TTFT aproximado = queue + prompt eval (em ms)
      TTFT_MS=$(awk "BEGIN { printf \"%d\", ($QUEUE_TIME + $PROMPT_TIME) * 1000 }")

      jq -nc \
        --arg model "$M" \
        --arg pid "$PID" \
        --argjson round "$R" \
        --argjson wall_ms "$WALL_MS" \
        --argjson ttft_ms "$TTFT_MS" \
        --argjson completion_tokens "$COMPLETION_TOKENS" \
        --argjson prompt_tokens "$PROMPT_TOKENS" \
        --arg tps "$TPS" \
        --arg content "$CONTENT" \
        --arg provider "groq" \
        '{provider:$provider, model:$model, prompt_id:$pid, round:$round, wall_ms:$wall_ms, ttft_ms:$ttft_ms, output_tokens:$completion_tokens, input_tokens:$prompt_tokens, tokens_per_sec:($tps|tonumber), response:$content}' \
        >> "$OUT"

      # Respeitar rate limit 30 RPM = 2s entre chamadas (com folga 2.2s)
      sleep 2.2
    done
  done
done

echo "[bench-groq] DONE at $(date)" | tee -a "$LOG"
echo "[bench-groq] results: $OUT"
echo "[bench-groq] lines: $(wc -l < $OUT)"
