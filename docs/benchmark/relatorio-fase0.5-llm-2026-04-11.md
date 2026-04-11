# Relatório Fase 0.5 — Pesquisa e Benchmark LLM para o Projeto Alice

**Data:** 2026-04-11
**Autor:** project-owner (Claude Code)
**Status:** Parte 1 concluída — aguarda decisão do usuário para seguir para Parte 2 (cloud)

---

## 1. Contexto e Motivação

O Projeto Alice é um mod Minecraft Forge 1.20.1 com jogadora IA (FakePlayer) controlada por LLM local. A arquitetura original do brainstorm v0.9 assumia Ollama local na máquina **bill** (Ryzen 5 2500U + iGPU Vega, 7.4 GiB RAM, Debian 13). O risco **R03** do brainstorm documentava gargalo de latência. Esta fase valida empiricamente se o caminho local é viável e pesquisa alternativas cloud gratuitas brasileiras e globais.

Regra do projeto aplicada: **fallback proibido sem autorização explícita**. Decisão final adotará um único caminho (local OU cloud) sem rede de segurança silenciosa.

---

## 2. Metodologia

### 2.1 Critérios (aprovados pelo usuário)

**Local (bill):**
- RAM em inferência ≤ 5 GiB
- Quantização Q4_K_M
- TPS ≥ 5, TTFT ≤ 3s, resposta ≤ 15s para 200 tokens
- Qualidade PT-BR sem alucinação, sem mistura de idiomas
- Contexto ≥ 4k

**Cloud:**
- Gratuito (ou custo negligível no volume esperado)
- Latência p50 ≤ 2s, resposta ≤ 5s
- PT-BR excelente
- ≥ 30 req/min
- Doc oficial clara, API REST/OpenAI-compat
- Brasil-first (menor ping)

### 2.2 Dataset de prompts (10 categorias representativas)

P01_saudacao, P02_crafting, P03_comando_direto, P04_multistep, P05_roleplay, P06_mod_especifico, P07_combate_urgente, P08_chitchat, P09_ambiguidade, P10_explicacao_longa.

System prompt: persona da Alice (ruiva sobrevivente, PT-BR).

### 2.3 Execução

4 modelos × 10 prompts × 3 rodadas = **120 inferências** via API HTTP `/api/chat` do Ollama (streaming off, temperature=0.7, num_predict=300).

Script: [bench_ollama.sh](./bench_ollama.sh) — rodou em 55 minutos em background na bill (setsid, desacoplado).

Resultados brutos: [results_bill_local_20260411.jsonl](./results_bill_local_20260411.jsonl)

---

## 3. Resultados Quantitativos — Bill (local)

| Modelo | n | Mediana wall (ms) | Média wall (ms) | TPS mediana | TTFT mediana | Output tokens (med) | Min wall | Max wall |
|---|---|---|---|---|---|---|---|---|
| **qwen2.5:3b** | 30 | **13.133** | 21.151 | **7.57** | 375 | 81 | 6.003 | 46.646 |
| gemma2:2b | 30 | 22.101 | 25.838 | 6.70 | 410 | 137 | 5.536 | 50.907 |
| llama3.2:3b | 30 | 23.275 | 25.080 | 7.18 | 434 | 140 | 5.772 | 51.237 |
| phi3:3.8b-mini | 30 | 32.989 | 32.886 | 6.02 | **202** | 153 | 10.420 | 59.118 |

### Critérios atendidos

| Critério | qwen2.5:3b | gemma2:2b | llama3.2:3b | phi3:3.8b |
|---|---|---|---|---|
| TPS ≥ 5 | ✅ 7.57 | ✅ 6.70 | ✅ 7.18 | ✅ 6.02 |
| TTFT ≤ 3s | ✅ 375ms | ✅ 410ms | ✅ 434ms | ✅ 202ms |
| Wall ≤ 15s (P50) | ✅ 13.1s | ❌ 22.1s | ❌ 23.3s | ❌ 33.0s |
| Wall ≤ 15s (P95 aprox) | ❌ max 46s | ❌ max 51s | ❌ max 51s | ❌ max 59s |

**Nenhum modelo atende o critério de wall time em pior caso.** Apenas qwen2.5:3b atende a mediana.

---

## 4. Resultados Qualitativos — Análise por prompt

Amostra de respostas em `round 1` para 4 prompts críticos:

### P01 — Saudação simples

- **llama3.2:3b** ✅ natural, PT-BR correto, concisa e amigável.
- **qwen2.5:3b** ⚠️ **erro de gênero** — "obrigado" em vez de "obrigada" (Alice é feminina).
- **gemma2:2b** ⚠️ gírias esquisitas ("barra de forja" não existe em Minecraft), emojis em profusão, persona caricata.
- **phi3:3.8b** ✅ formal mas adequada.

### P05 — Roleplay

- **llama3.2:3b** ✅ natural, imersiva.
- **qwen2.5:3b** ⚠️ "amigão" OK, genérica.
- **gemma2:2b** ⚠️ linguagem ainda caricata.
- **phi3:3.8b** ⚠️ frase estranha ("como um gato nas árvores"), forçada.

### P07 — Combate urgente (creeper!)

- **llama3.2:3b** ❌❌ **CATASTRÓFICO** — sugere "tentar conversar com o creeper para convencê-lo a não atacar". Quebra total de contexto.
- **qwen2.5:3b** ⚠️ Resposta direta boa para urgência, mas erro lexical ("estouvo").
- **gemma2:2b** ❌ Entra em roleplay descritivo de livro em vez de reagir — péssimo.
- **phi3:3.8b** ⚠️ "ataca o cérebro dessa criatura com arma de madeira" — confusa mas ao menos reage.

### P04 — Como construir portal do Nether

- **llama3.2:3b** ❌ Factualmente errado: "obsidiana obtida de blocos de lava ou fogo" (errado), "artilharia para atacar mobs" (inventado), "ferro fundido" (inventado).
- **qwen2.5:3b** ❌❌ **Alucinação grave** — "portal do Nether é criado utilizando bloco de água". Completamente fora da realidade do jogo.
- **gemma2:2b** ❌❌ **Responde em INGLÊS** — fuga de idioma. E inventa "Nether Star" e "Soul Sand" como requisitos.
- **phi3:3.8b** ❌ Inventa "obsidianite" (item que não existe).

### Veredito qualitativo

**Nenhum modelo local atende o critério "qualidade PT-BR sem alucinação em contexto simples".** Todos falham em pelo menos um dos eixos críticos:

| Problema | qwen2.5 | gemma2 | llama3.2 | phi3 |
|---|---|---|---|---|
| Alucinação factual Minecraft | ❌ | ❌ | ❌ | ❌ |
| Fuga para inglês | ⚠️ | ❌ | ⚠️ | ⚠️ |
| Quebra de contexto (urgência) | ⚠️ | ❌ | ❌❌ | ⚠️ |
| Erro de gênero (feminino) | ❌ | ✅ | ✅ | ✅ |

### Baseline local selecionado

**qwen2.5:3b** — não porque seja "bom", mas porque é o menos ruim considerando velocidade + concisão. Será o benchmark para comparar com cloud.

**Conclusão interina:** caminho local em bill é **inviável como solução única** para o Alice. Serve apenas como referência de quanto o cloud precisa superar.

---

## 5. Pesquisa Cloud — Provedores Gratuitos / Baixo Custo

### 5.1 Tabela comparativa

| Provedor | Modelo recomendado | Preço | RPM | RPD | TPM | Latência BR | PT-BR | Treina em dados? |
|---|---|---|---|---|---|---|---|---|
| **Groq** | llama-3.1-8b-instant | **Grátis** | 30 | **14.400** | 6.000 | ~150-250ms (US) | Bom (oficial PT) | Não explicitado |
| Groq | llama-3.3-70b-versatile | Grátis | 30 | 1.000 | 12.000 | ~150-250ms (US) | Bom | Não explicitado |
| Groq | openai/gpt-oss-20b | Grátis | — | — | — | ~150-250ms (US) | OK | Não explicitado |
| **Maritaca** | sabiazinho-4 | **Pago** (R$20 crédito inicial, ~R$1/M input estimado) | — | — | — | **<50ms (BR)** | **NATIVO** | Não (API comercial) |
| Maritaca | sabia-4 | Pago (mais caro) | — | — | — | <50ms (BR) | NATIVO | Não |
| Google Gemini | 2.5 Flash | Grátis | 5 | **20** ❌ | 250k | ~80ms (SP*) | Excelente | **Sim (free tier treina)** |
| Google Gemini | 3.1 Flash-Lite | Grátis | 15 | 500 | 250k | ~80ms (SP*) | Excelente | **Sim** |
| Mistral | medium/small (Experiment) | Grátis* | 60 | — | 500k | ~200-250ms (EU) | OK | **Sim (opt-in obrigatório)** |
| OpenRouter | vários (~23 free) | Grátis | 20 | 200 | — | ~200ms | varia | varia |

\* Região BR para Gemini free tier não confirmada — pode ir para US.

### 5.2 Eliminações justificadas

**❌ Gemini 2.5 Flash:** 20 req/DIA é inviável para chat de jogo. Descartado.

**❌ Gemini 3.1 Flash-Lite:** 500 RPD é marginal; modelo "lite" tende a ter qualidade menor; Google treina em free tier data (conflito de privacidade mesmo que o conteúdo seja casual). Descartado como primary.

**❌ OpenRouter:** 20 RPM / 200 RPD é muito restritivo. Descartado.

**❌ Mistral Experiment:** 1B tokens/mês é generosíssimo, mas **exige opt-in de data training**. Contra o espírito do projeto. Descartado salvo autorização.

### 5.3 Finalistas

#### 🏆 Groq — Llama 3.1 8B ou Llama 3.3 70B (grátis)

**Prós:**
- **Volume absurdamente grande:** 14.400 req/dia no modelo 8B (600 req/h) — efetivamente ilimitado para single user
- **Velocidade extrema:** 560 tok/s (8B) — resposta de 300 tokens em 0,5s + latência de rede ~200ms = **<1s total do Brasil**
- **Gratuito sem cartão de crédito**
- Modelo Llama 3.1/3.3 tem suporte oficial a PT-BR (Meta lista Portuguese entre as 8 línguas oficiais)
- Endpoint OpenAI-compatível (fácil integrar em Java via OkHttp ou OpenAI SDK)
- API key instantânea após cadastro

**Contras:**
- Servidores nos EUA (latência ~150-250ms rede), 4 regiões globais mas nenhuma no Brasil
- Política de dados sobre treino em free tier não explícita na doc
- Llama não é otimizado especificamente para PT-BR (cobertura boa mas não nativa)

#### 🥈 Maritaca AI — Sabiazinho-4 (pago com R$20 crédito inicial)

**Prós:**
- **100% brasileiro**, servidores no Brasil (<50ms latência)
- **PT-BR nativo** — líder em benchmarks de conhecimento brasileiro (OAB, legislação, conversação PT)
- Sabiazinho-4 posicionado como "speed + low cost + agentic"
- OpenAI-compatible (só trocar `base_url` para `https://chat.maritaca.ai/api`)
- R$20 de crédito inicial para testes
- Preço estimado do Sabiazinho-3 (antecessor): R$1/M input + R$3/M output. Sabiazinho-4 presumivelmente similar.
- R$20 crédito = ~5-10 milhões de tokens, meses de uso em volume de dev
- Comercial, sem treino em dados do cliente
- Contexto 128k tokens

**Contras:**
- **Não é 100% gratuito** — o crédito acaba e depois é pago por token
- Velocidade da inferência não publicada (assumir comparável a mainstream)
- Menos documentação em inglês, mas toda em português (oportunidade, não problema)

---

## 6. Análise do trade-off: Groq (grátis + rápido + US) vs Maritaca (pago + nativo + BR)

| Dimensão | Groq | Maritaca |
|---|---|---|
| Custo | 🏆 Zero | Baixo (R$20 gratuito depois ~R$5-15/mês estimado em uso moderado) |
| Velocidade (inferência) | 🏆 560 tok/s | 150-300 tok/s (estimado) |
| Latência de rede | 150-250ms (US) | 🏆 <50ms (BR) |
| **Latência total percebida** | ~750ms-1s | ~1-2s (estimado) |
| PT-BR | Bom (Llama oficial) | 🏆 Nativo brasileiro |
| Conhecimento do Brasil | Genérico | 🏆 Especializado |
| Volume | 🏆 14.400 RPD | Indefinido, presumível alto |
| Risco de sumir | Baixo | Médio (startup BR) |
| Treino em dados do usuário | Não claro | 🏆 Não (API paga) |

Ambos atendem os critérios de latência total (<2s p50, <5s resposta completa).

---

## 7. Recomendação

### Proposta do project-owner

**Opção 1 (pragmática):** **Groq com Llama 3.1 8B** como primary absoluto.
- Justificativa: atende todos os critérios, é grátis, é rápido, tem volume que mata qualquer dúvida.
- Risco: qualidade PT-BR pode ser inferior ao Sabiazinho-4 em nuances brasileiras específicas (gírias, contexto cultural).

**Opção 2 (brasileira):** **Maritaca Sabiazinho-4** como primary, aceitando custo marginal.
- Justificativa: cumpre o "Brasil-first" do usuário, qualidade PT-BR teoricamente superior, latência BR imbatível.
- Risco: o crédito de R$20 é o experimento; após testes reais saberemos se o gasto mensal é compatível.

**Opção 3 (a mais rigorosa):** **Testar os dois head-to-head** antes de decidir.
- Criar conta nos dois.
- Rodar o mesmo dataset de 10 prompts × 3 rounds em ambos.
- Comparar latência real medida (não estimada), qualidade subjetiva PT-BR, custo efetivo.
- Decidir com dados de produção.

**Recomendo Opção 3.** É o único caminho empiricamente sólido, alinha com a diretiva do usuário de "pesquisa séria e bem fundamentada", e consome ~15 minutos de compute + ~R$2 do crédito Maritaca.

---

## 8. Próximos Passos

1. ✅ Critérios definidos
2. ✅ Benchmark local concluído — baseline qwen2.5:3b selecionado
3. ✅ Pesquisa cloud concluída — finalistas Groq + Maritaca
4. ⏳ **Usuário cria conta em Groq** (instantâneo, sem cartão)
5. ⏳ **Usuário cria conta em Maritaca** (R$20 crédito grátis)
6. ⏳ Usuário fornece API keys para o projeto
7. ⏳ project-owner escreve `bench_cloud.sh` que replica o mesmo dataset nos 2 providers
8. ⏳ Head-to-head: bill baseline vs Groq vs Maritaca
9. ⏳ Decisão arquitetural #50 registrada no brainstorm
10. ⏳ Atualizar plano-projeto-alice.md adicionando Fase 0.5 formal

---

## 9. Instruções para o Usuário

### Criar conta Groq (5 min)

1. Acesse https://console.groq.com
2. Clique "Sign up" — login via Google/GitHub ou email
3. Confirme email
4. Vá em "API Keys" no menu lateral
5. Clique "Create API Key", dê um nome (ex: "alice-dev"), copie a chave (começa com `gsk_...`)
6. **Guarde a chave com segurança** — Groq não mostra novamente
7. Me passe a chave para usar no benchmark (ou me diga que está guardada e você mesmo vai colar no script)

### Criar conta Maritaca (5 min)

1. Acesse https://plataforma.maritaca.ai
2. Crie conta (pede email, nome, confirmação)
3. Verifique saldo — deve ter **R$20 de crédito inicial**
4. Menu "Chaves de API" → criar nova chave
5. Copie a chave
6. Me passe a chave ou use diretamente no script

### Validação antes de benchmark

Antes de rodar o benchmark completo, vou fazer **uma única chamada curl** em cada provider só para confirmar que a API está viva e a chave funciona. Se der certo, libero o bench completo.
