#!/usr/bin/env python3
"""
Projeto Alice — Benchmark Groq cloud
Dataset identico ao bench_ollama.sh para comparacao direta.
Roda na maquina de dev contra api.groq.com.

Saida: docs/benchmark/results_groq_<timestamp>.jsonl
"""
import json
import os
import sys
import time
import urllib.request
import urllib.error
from datetime import datetime
from pathlib import Path

# --- Configuracao ---
SCRIPT_DIR = Path(__file__).resolve().parent
ALICE_ROOT = SCRIPT_DIR.parent.parent
SECRETS = ALICE_ROOT / ".secrets" / "llm_keys.env"

# Parse simples de .env
env = {}
for line in SECRETS.read_text(encoding="utf-8").splitlines():
    line = line.strip()
    if not line or line.startswith("#") or "=" not in line:
        continue
    k, v = line.split("=", 1)
    env[k.strip()] = v.strip()

GROQ_API_KEY = env["GROQ_API_KEY"]
GROQ_BASE_URL = env.get("GROQ_BASE_URL", "https://api.groq.com/openai/v1")

MODELS = [
    "llama-3.1-8b-instant",
    "llama-3.3-70b-versatile",
]

PROMPTS = [
    ("P01_saudacao", "Oi Alice, tudo bem com voce?"),
    ("P02_crafting", "Como eu faco uma bigorna no Minecraft? Lista os ingredientes."),
    ("P03_comando_direto", "Alice, pega 3 blocos de madeira de carvalho e traz pra mim."),
    ("P04_multistep", "Como construir um portal do nether do zero? Explica passo a passo o que precisa de recurso e o que precisa de estrutura."),
    ("P05_roleplay", "Voce e a Alice, uma sobrevivente ruiva especialista em Minecraft pos-apocaliptico. Eu sou um amigo seu. Me cumprimenta de forma natural e me pergunta como foi meu dia."),
    ("P06_mod_especifico", "No mod Cursed Walking tem um item chamado 'cursed compass'. Voce sabe pra que serve? Se nao souber, diz que nao sabe."),
    ("P07_combate_urgente", "ALICE CORRE TEM UM CREEPER ATRAS DE VOCE"),
    ("P08_chitchat", "Que tipo de musica voce gostaria de ouvir enquanto a gente constroi a base?"),
    ("P09_ambiguidade", "Constroi uma casa pra mim."),
    ("P10_explicacao_longa", "Me explica em detalhes como funciona o sistema de redstone do Minecraft, incluindo os principais componentes, como eles interagem, e da exemplos de circuitos uteis para sobrevivencia."),
]

ROUNDS = 3
SYSTEM = ("Voce e a Alice, uma jogadora IA companheira em um servidor Minecraft "
          "com o modpack Cursed Walking. Voce e ruiva, sobrevivente, direta e "
          "prestativa. Responde sempre em portugues do Brasil.")
SLEEP_BETWEEN = 2.2  # rate limit 30 RPM + folga

TS = datetime.now().strftime("%Y%m%d_%H%M%S")
OUT_FILE = SCRIPT_DIR / f"results_groq_{TS}.jsonl"
LOG_FILE = SCRIPT_DIR / f"run_groq_{TS}.log"


def log(msg: str):
    line = f"[bench-groq] {msg}"
    print(line, flush=True)
    with LOG_FILE.open("a", encoding="utf-8") as f:
        f.write(line + "\n")


def call_groq(model: str, user_msg: str, max_tokens: int = 300) -> tuple[int, dict]:
    payload = json.dumps({
        "model": model,
        "temperature": 0.7,
        "max_tokens": max_tokens,
        "messages": [
            {"role": "system", "content": SYSTEM},
            {"role": "user", "content": user_msg},
        ],
    }).encode("utf-8")

    req = urllib.request.Request(
        f"{GROQ_BASE_URL}/chat/completions",
        data=payload,
        headers={
            "Authorization": f"Bearer {GROQ_API_KEY}",
            "Content-Type": "application/json",
            "User-Agent": "Mozilla/5.0 (compatible; alice-bench/0.1)",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            raw = resp.read().decode("utf-8")
            return resp.status, json.loads(raw)
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        if not raw:
            return e.code, {"error": "empty body", "headers": dict(e.headers)}
        try:
            return e.code, json.loads(raw)
        except json.JSONDecodeError:
            return e.code, {"error": "non-json body", "raw": raw[:500]}
    except urllib.error.URLError as e:
        return 0, {"error": f"URLError: {e.reason}"}


def main():
    log(f"starting at {datetime.now().isoformat()}")
    log(f"models: {MODELS}")
    log(f"output: {OUT_FILE}")

    # Validacao previa
    log("validating API key...")
    status, body = call_groq("llama-3.1-8b-instant", "ping", max_tokens=5)
    if status != 200:
        log(f"FATAL: validation failed (HTTP {status})")
        log(json.dumps(body))
        sys.exit(1)
    log(f"API OK (HTTP 200) — sample: {body['choices'][0]['message']['content'][:50]}")

    with OUT_FILE.open("w", encoding="utf-8") as out:
        for model in MODELS:
            for pid, ptext in PROMPTS:
                for r in range(1, ROUNDS + 1):
                    log(f"{model} | {pid} | round {r}")
                    t0 = time.monotonic_ns()
                    status, body = call_groq(model, ptext)
                    t1 = time.monotonic_ns()
                    wall_ms = (t1 - t0) // 1_000_000

                    if status != 200:
                        log(f"ERROR HTTP {status}: {json.dumps(body)[:200]}")
                        time.sleep(SLEEP_BETWEEN)
                        continue

                    usage = body.get("usage", {})
                    pt = usage.get("prompt_tokens", 0)
                    ct = usage.get("completion_tokens", 0)
                    total_t = usage.get("total_time", 0) or 0
                    compl_t = usage.get("completion_time", 0) or 0
                    prompt_t = usage.get("prompt_time", 0) or 0
                    queue_t = usage.get("queue_time", 0) or 0
                    content = body["choices"][0]["message"]["content"]

                    tps = (ct / compl_t) if compl_t > 0 else 0.0
                    ttft_ms = int((queue_t + prompt_t) * 1000)

                    out.write(json.dumps({
                        "provider": "groq",
                        "model": model,
                        "prompt_id": pid,
                        "round": r,
                        "wall_ms": wall_ms,
                        "ttft_ms": ttft_ms,
                        "output_tokens": ct,
                        "input_tokens": pt,
                        "tokens_per_sec": round(tps, 2),
                        "groq_total_time_s": round(total_t, 3),
                        "groq_completion_time_s": round(compl_t, 3),
                        "groq_queue_time_s": round(queue_t, 3),
                        "response": content,
                    }, ensure_ascii=False) + "\n")
                    out.flush()

                    time.sleep(SLEEP_BETWEEN)

    log(f"DONE at {datetime.now().isoformat()}")
    log(f"results: {OUT_FILE}")
    with OUT_FILE.open(encoding="utf-8") as f:
        n = sum(1 for _ in f)
    log(f"lines: {n}")


if __name__ == "__main__":
    main()
