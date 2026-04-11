#!/bin/bash
# Projeto Alice - Benchmark Ollama local
# Roda em bill@192.168.0.200 (Ryzen 5 2500U, 7.4 GiB, sem GPU)
# Mede latencia, throughput e qualidade subjetiva (avaliacao humana posterior)
#
# Uso: ./bench_ollama.sh
# Saida: ~/alice_bench/results_<timestamp>.jsonl

set -euo pipefail
export LC_ALL=C LANG=C  # forca ponto como separador decimal (pt_BR usa virgula)

OUTDIR="$HOME/alice_bench"
mkdir -p "$OUTDIR"
TS=$(date +%Y%m%d_%H%M%S)
OUT="$OUTDIR/results_${TS}.jsonl"
LOG="$OUTDIR/run_${TS}.log"

MODELS=(
  "llama3.2:latest"
  "phi3:3.8b-mini-4k-instruct-q4_K_M"
  "qwen2.5:3b"
  "gemma2:2b"
)

# 10 prompts representativos do que Alice recebe no jogo
# IDs estaveis para correlacao posterior
PROMPTS_IDS=(
  "P01_saudacao"
  "P02_crafting"
  "P03_comando_direto"
  "P04_multistep"
  "P05_roleplay"
  "P06_mod_specifico"
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

echo "[bench] starting at $(date)" | tee "$LOG"
echo "[bench] models: ${MODELS[*]}" | tee -a "$LOG"
echo "[bench] prompts: ${#PROMPTS_IDS[@]}, rounds: $ROUNDS" | tee -a "$LOG"
echo "[bench] output: $OUT" | tee -a "$LOG"

# Warm up cada modelo (carrega na RAM)
for M in "${MODELS[@]}"; do
  echo "[bench] warming up $M ..." | tee -a "$LOG"
  ollama run "$M" "ok" >/dev/null 2>&1 || echo "[bench] WARN warmup failed for $M" | tee -a "$LOG"
done

for M in "${MODELS[@]}"; do
  for i in "${!PROMPTS_IDS[@]}"; do
    PID="${PROMPTS_IDS[$i]}"
    PTEXT="${PROMPTS_TEXT[$i]}"
    for R in $(seq 1 $ROUNDS); do
      echo "[bench] $M | $PID | round $R" | tee -a "$LOG"

      # Mede RAM antes
      RAM_BEFORE=$(free -m | awk '/^Mem.:|^Mem:/ {print $3}')

      # Chamada via API HTTP nao-streaming, captura tudo
      START_NS=$(date +%s%N)
      RESP=$(curl -s -X POST http://127.0.0.1:11434/api/chat \
        -H "Content-Type: application/json" \
        -d "$(jq -n \
              --arg model "$M" \
              --arg sys "$SYSTEM" \
              --arg user "$PTEXT" \
              '{model:$model, stream:false, options:{temperature:0.7, num_predict:300}, messages:[{role:"system",content:$sys},{role:"user",content:$user}]}')")
      END_NS=$(date +%s%N)

      RAM_AFTER=$(free -m | awk '/^Mem.:|^Mem:/ {print $3}')

      WALL_MS=$(( (END_NS - START_NS) / 1000000 ))

      # Extrai metricas nativas do Ollama (em ns)
      EVAL_COUNT=$(echo "$RESP" | jq -r '.eval_count // 0')
      EVAL_DUR_NS=$(echo "$RESP" | jq -r '.eval_duration // 0')
      PROMPT_EVAL_COUNT=$(echo "$RESP" | jq -r '.prompt_eval_count // 0')
      PROMPT_EVAL_DUR_NS=$(echo "$RESP" | jq -r '.prompt_eval_duration // 0')
      LOAD_DUR_NS=$(echo "$RESP" | jq -r '.load_duration // 0')
      TOTAL_DUR_NS=$(echo "$RESP" | jq -r '.total_duration // 0')
      CONTENT=$(echo "$RESP" | jq -r '.message.content // ""')

      # Tokens por segundo (output)
      if [ "$EVAL_DUR_NS" -gt 0 ] && [ "$EVAL_COUNT" -gt 0 ]; then
        TPS=$(awk "BEGIN { printf \"%.2f\", $EVAL_COUNT / ($EVAL_DUR_NS / 1000000000) }")
      else
        TPS="0"
      fi

      # TTFT estimado (load + prompt eval)
      TTFT_MS=$(( (LOAD_DUR_NS + PROMPT_EVAL_DUR_NS) / 1000000 ))

      jq -nc \
        --arg model "$M" \
        --arg pid "$PID" \
        --argjson round "$R" \
        --argjson wall_ms "$WALL_MS" \
        --argjson ttft_ms "$TTFT_MS" \
        --argjson eval_count "$EVAL_COUNT" \
        --argjson prompt_eval_count "$PROMPT_EVAL_COUNT" \
        --arg tps "$TPS" \
        --argjson ram_delta_mb "$((RAM_AFTER - RAM_BEFORE))" \
        --arg content "$CONTENT" \
        '{model:$model, prompt_id:$pid, round:$round, wall_ms:$wall_ms, ttft_ms:$ttft_ms, output_tokens:$eval_count, input_tokens:$prompt_eval_count, tokens_per_sec:($tps|tonumber), ram_delta_mb:$ram_delta_mb, response:$content}' \
        >> "$OUT"
    done
  done
done

echo "[bench] DONE at $(date)" | tee -a "$LOG"
echo "[bench] results: $OUT"
echo "[bench] lines: $(wc -l < $OUT)"
