#!/usr/bin/env python3
"""
Projeto Alice - Benchmark Maritaca AI (Sabiazinho-4 / Sabia-3)
Dataset identico ao bench_ollama.sh e bench_groq.py para comparacao direta.
Endpoint OpenAI-compatible: https://chat.maritaca.ai/api/chat/completions

Pre-requisito: creditos ativos em MARITACA_API_KEY (.secrets/llm_keys.env)
Saida: docs/benchmark/results_maritaca_<timestamp>.jsonl
"""
import json
import sys
import time
import urllib.request
import urllib.error
from datetime import datetime
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
ALICE_ROOT = SCRIPT_DIR.parent.parent
SECRETS = ALICE_ROOT / ".secrets" / "llm_keys.env"

env = {}
for line in SECRETS.read_text(encoding="utf-8").splitlines():
    line = line.strip()
    if not line or line.startswith("#") or "=" not in line:
        continue
    k, v = line.split("=", 1)
    env[k.strip()] = v.strip()

MARITACA_API_KEY = env["MARITACA_API_KEY"]
MARITACA_BASE_URL = env.get("MARITACA_BASE_URL", "https://chat.maritaca.ai/api")

MODELS = [
    "sabiazinho-4",
    "sabia-3",
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
SLEEP_BETWEEN = 1.5

TS = datetime.now().strftime("%Y%m%d_%H%M%S")
OUT_FILE = SCRIPT_DIR / f"results_maritaca_{TS}.jsonl"
LOG_FILE = SCRIPT_DIR / f"run_maritaca_{TS}.log"


def log(msg: str):
    line = f"[bench-maritaca] {msg}"
    print(line, flush=True)
    with LOG_FILE.open("a", encoding="utf-8") as f:
        f.write(line + "\n")


def call_maritaca(model: str, user_msg: str, max_tokens: int = 300) -> tuple[int, dict]:
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
        f"{MARITACA_BASE_URL}/chat/completions",
        data=payload,
        headers={
            "Authorization": f"Bearer {MARITACA_API_KEY}",
            "Content-Type": "application/json",
            "User-Agent": "Mozilla/5.0 (compatible; alice-bench/0.1)",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
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

    log("validating API key...")
    status, body = call_maritaca("sabiazinho-4", "ping", max_tokens=5)
    if status != 200:
        log(f"FATAL: validation failed (HTTP {status})")
        log(json.dumps(body)[:500])
        sys.exit(1)
    sample = body.get("choices", [{}])[0].get("message", {}).get("content", "")[:50]
    log(f"API OK (HTTP 200) - sample: {sample}")

    with OUT_FILE.open("w", encoding="utf-8") as out:
        for model in MODELS:
            for pid, ptext in PROMPTS:
                for r in range(1, ROUNDS + 1):
                    log(f"{model} | {pid} | round {r}")
                    t0 = time.monotonic_ns()
                    status, body = call_maritaca(model, ptext)
                    t1 = time.monotonic_ns()
                    wall_ms = (t1 - t0) // 1_000_000

                    if status != 200:
                        log(f"ERROR HTTP {status}: {json.dumps(body)[:300]}")
                        time.sleep(SLEEP_BETWEEN)
                        continue

                    usage = body.get("usage", {})
                    pt = usage.get("prompt_tokens", 0)
                    ct = usage.get("completion_tokens", 0)
                    content = body["choices"][0]["message"]["content"]

                    tps = (ct / (wall_ms / 1000.0)) if wall_ms > 0 else 0.0

                    out.write(json.dumps({
                        "provider": "maritaca",
                        "model": model,
                        "prompt_id": pid,
                        "round": r,
                        "wall_ms": wall_ms,
                        "ttft_ms": None,
                        "output_tokens": ct,
                        "input_tokens": pt,
                        "tokens_per_sec": round(tps, 2),
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
