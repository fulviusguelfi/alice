# Plan B — Alice-Service (projeto paralelo)

**Projeto:** Alice (mod Minecraft Forge 1.20.1)
**Status:** pré-registro. Não está em execução. Só será disparado se o kill agregado dos spikes de porte do Baritone acionar (ver [spikes/pathfinding-port/README.md](../spikes/pathfinding-port/README.md)).
**Criado em:** 2026-04-11

---

## Natureza

Este documento **não** é um fallback runtime. É um **pivot de projeto** definido em tempo de decisão, com olhos abertos, respeitando a regra No-Fallback do projeto. Se o kill agregado dos spikes disparar, o projeto Alice principal **pausa integralmente**, e Alice-Service passa a ser o projeto ativo até entregar seu MVP. Só então o projeto principal retoma, agora como cliente magro do serviço.

Existir este 1-pager **antes** de começar o Spike A é pré-requisito: se o pivot acontecer, a equipe não começa plano B em pânico do zero.

---

## Motivação

A Opção 4 do brainstorm (vanilla A* customizado do zero) foi descartada como Plan B porque seus resultados conhecidos não atendem os requisitos de Alice: navegação complexa, mineração guiada, construção passo-a-passo e decisão de alto nível. Trabalhar mirando um resultado sabidamente insuficiente gera frustração e leva ao abandono.

O Plan B precisa ser uma alternativa **tecnicamente viável e equivalente em potência** ao Baritone portado. A escolha é deslocar pathfinding e decisão para fora da JVM do Minecraft, rodando em um serviço independente na rede local.

---

## Arquitetura proposta

```
+---------------------------+        WebSocket         +----------------------------+
|   Minecraft Forge Server  |  <===================>   |      Alice-Service         |
|   (mod Alice cliente-magro)|                          |  (pathfinding + decisão)   |
|                           |                          |                            |
|  - FakePlayer             |  estado do mundo  --->   |  - mundo parcial           |
|  - Executor de ação       |  <---  comandos          |  - pathfinder              |
|  - Log / telemetria       |                          |  - plan. longo prazo       |
+---------------------------+                          +----------------------------+
```

**Responsabilidades do mod (cliente magro):**
- Manter o FakePlayer vivo e sincronizado com o server.
- Enviar estado periódico via WebSocket: posição, rotação, inventário, entidades próximas, blocos próximos relevantes.
- Receber comandos e executar **no tick do server**: mover, quebrar bloco, colocar bloco, usar item, atacar.
- Logs detalhados de request/response com correlação ID e latência.

**Responsabilidades do Alice-Service:**
- Manter modelo parcial do mundo baseado no que o mod envia.
- Rodar pathfinder (stack a definir — ver candidatos abaixo).
- Tomar decisões de alto nível (goal selection, plano de construção, resposta a ameaças).
- Opcionalmente conversar com LLM (Groq/Maritaca) para diálogo com o jogador.
- Retornar comandos atômicos que o mod consegue executar em 1 tick.

---

## Protocolo (esboço inicial)

**Transporte:** WebSocket (bidirecional, baixa latência, stateful, já mapeado no brainstorm como escolha preferida).

**Formato:** JSON com schema versionado. Ex:

```json
// mod -> service
{ "v": 1, "id": "corr-abc", "kind": "state", "t": 123456,
  "pos": [x, y, z], "yaw": y, "inv": [...], "nearby": {...} }

// service -> mod
{ "v": 1, "id": "corr-abc", "kind": "cmd",
  "action": "move", "args": { "dx": 0.2, "dy": 0, "dz": 0.1 } }
```

**Contrato mínimo:**
- Mod envia `state` a cada N ticks (N a definir — 5? 10?).
- Service responde com `cmd` ou `noop`.
- Mod envia `event` em transições relevantes (block broken, item picked up, damage taken).
- Heartbeat a cada 1s para detectar queda.
- **Falha do serviço é falha visível:** se a conexão cai, o FakePlayer para, loga erro e avisa jogador. Não há fallback runtime (regra No-Fallback).

O schema detalhado é produzido no primeiro dia do Plan B, antes de qualquer código.

---

## Stack candidata (a decidir se pivot acontecer)

| Opção | Prós | Contras |
|---|---|---|
| **Node.js + mineflayer-pathfinder** | Maduro, comunidade ativa, exemplos de AI+Minecraft | Depende do protocolo Minecraft; queremos usar só o pathfinder, não o bot inteiro — pode ser difícil desacoplar |
| **Python + py-trees + custom pathfinder** | Stack confortável, integração LLM natural | Pathfinding from scratch = mesmo problema da Opção 4 |
| **Java standalone + biblioteca A\* genérica** | Reuso de tipos com o mod | Reintroduz o problema de acoplamento que queríamos evitar |
| **Rust + crate pathfinding** | Performance, binários pequenos | Curva de aprendizado, integração LLM mais trabalhosa |

**Decisão provisória (a validar no momento do pivot):** Node.js + mineflayer-pathfinder, usando o pathfinder como biblioteca isolada (sem o bot). Fallback de stack: Python com pathfinder customizado se mineflayer-pathfinder resistir ao desacoplamento.

Não confundir com regra No-Fallback — aqui "fallback" significa escolha alternativa em tempo de decisão, não fallback runtime.

---

## Topologia de deploy

- Alice-Service roda em **uma das máquinas da rede local** (topologia de deploy do projeto já prevê cliente+servidor em máquinas distintas; o service pode ficar em qualquer uma, tipicamente junto do Minecraft server).
- Mod Alice continua gratuito e open. O serviço também. Documentação clara para jogadores de como rodar o serviço.
- Rede local apenas no MVP. Exposição via internet (para multi-jogador distribuído) fica para fora do escopo do MVP.

---

## Cronograma e cap

**Cap duro: 6 semanas** para MVP funcional (navegação básica em servidor de teste, protocolo WS operacional, ciclo completo state→cmd→ação).

Breakdown estimado:
- **Semana 1:** pré-registro final (este doc expandido), decisão de stack, schema WS detalhado, setup de repo do Alice-Service.
- **Semana 2-3:** implementação do serviço (pathfinder + WS server).
- **Semana 4:** adaptação do mod para modo cliente-magro (troca do código relacionado a Baritone por cliente WS).
- **Semana 5:** integração ponta-a-ponta em servidor de teste.
- **Semana 6:** estabilização, logs, telemetria, documentação de deploy.

Depois do MVP, projeto principal retoma e tarefas adicionais do Alice-Service (combate, construção guiada, diálogo) entram no backlog normal do projeto.

---

## Kill criteria do próprio Plan B

Para evitar recursão (o Plan B também pode ter seus riscos), Alice-Service tem seus próprios kill criteria:

- **Semana 2:** se o pathfinder escolhido não consegue calcular path simples (10 blocos, 1 obstáculo) isoladamente em ambiente de teste, troca de stack sem hesitar.
- **Semana 4:** se a latência WS end-to-end (estado enviado → comando recebido → ação executada) fica acima de 100ms p95 em rede local, arquitetura é revista antes de seguir.
- **Semana 6:** se o MVP não estiver funcional, **o projeto Alice como um todo entra em revisão estratégica** — não se trata mais de escolher entre A e B, mas de questionar o escopo do projeto.

---

## Custo do pré-registro (antes de disparar)

- Este doc: **feito** (pré-registro 1-pager).
- Schema WebSocket inicial detalhado: 1 dia, produzido **se** o pivot disparar (não precisa estar pronto antes).
- Stack decision final: 1 dia, produzido **se** o pivot disparar.

Total antes do pivot: **apenas este documento**. Total no dia do pivot: **2 dias** antes de começar código do Alice-Service.

---

## Precedentes e honestidade sobre maturidade do padrão

Esta seção é intencionalmente honesta: o Plan B do Alice seria **inovador, não consolidado** no ecossistema Minecraft. Pesquisa dirigida encontrou o seguinte panorama:

### Precedente acadêmico validado (padrão arquitetural provado)

- **Microsoft Project Malmo** — https://github.com/microsoft/malmo
  Mod Forge (Java) + agentes externos em Python/C++/C#/Java via **TCP sockets + XML**. Mantido pela Microsoft Research, base de dezenas de papers, fundação do MineRL. **Valida academicamente o padrão mod+serviço externo.** Limitação: Minecraft antigo (originalmente 1.11.2), não Forge 1.20.1.
- **MineRL** — https://github.com/minerllabs/minerl
  Extensão de Malmo em produção acadêmica; reforça validação indireta do padrão.

### Precedente moderno direto (mas imaturo)

- **Minecraft-OpenClaw-Controller** — https://github.com/fangbm/Minecraft-OpenClaw-Controller
  Mods Forge + NeoForge + Fabric (1.21.1) + servidor **Node.js via WebSocket**. Arquitetura quase idêntica ao Plan B do Alice. Porém é projeto pequeno (~43 commits, 2 estrelas) — **prova de conceito existe, mas não há maturidade de produção**.

### Precedente parcial recente

- **mc-mcp (lafkpages)** — https://github.com/lafkpages/mc-mcp
  Fabric 1.21.8 expõe estado/ações via HTTP (Model Context Protocol), LLM externo decide, pathfinding delegado a Baritone in-process. Mostra que composição mod+HTTP+pathfinding é viável, mesmo sem externalizar o pathfinder.

### Contra-exemplos (por que a comunidade geralmente NÃO escolhe mod+service)

- **Voyager (NVIDIA/MineDojo)** — https://github.com/MineDojo/Voyager — Python + mineflayer (bot protocolo puro), **não é mod**.
- **Mindcraft (kolbytn)** — https://github.com/kolbytn/mindcraft — 5.1k estrelas, bot mineflayer puro, zero mod instalado.

A maioria do ecossistema "AI Minecraft" consolidado escolheu **bot mineflayer** (cliente externo falando protocolo Minecraft direto) em vez de mod+service, fugindo da complexidade de mod Java. Alice escolhe o caminho oposto porque:

1. **FakePlayer server-side real** é requisito para jogabilidade integrada (jogador humano vê a Alice no servidor como um player de verdade, não como bot externo).
2. **Integração com TTS/STT local** precisa rodar junto da JVM do mod.
3. **Mundo singleplayer opcional** requer mod dentro do jogo, não cliente externo.
4. **Deploy topology do projeto** já prevê mod em cliente e servidor, acréscimo de um serviço na rede é natural.

### Conclusão honesta

**Não existe um precedente ideal** (mod Forge 1.20.1 maduro delegando pathfinding/AI a serviço externo via WebSocket). O Plan B do Alice se inspira em **Malmo** (padrão arquitetural provado academicamente) e em **OpenClaw-Controller** (precedente direto moderno mas imaturo). O risco de ser "o primeiro projeto Forge 1.20.1 em produção com esse padrão" é real e deve ser considerado no momento do pivot.

### Referências internas do projeto

- Baritone (rota principal): https://github.com/cabaletta/baritone
- Mineflayer-pathfinder (candidato de stack se pivotar): https://github.com/PrismarineJS/mineflayer-pathfinder
- Regra No-Fallback do projeto: ver memória `feedback_no_fallback.md`
- Topologia de deploy do projeto: ver memória `deploy_topology.md`
- Spikes que podem disparar este plano: [../spikes/pathfinding-port/README.md](../spikes/pathfinding-port/README.md)
