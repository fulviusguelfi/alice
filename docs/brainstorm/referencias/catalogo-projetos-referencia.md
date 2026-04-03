# Catalogo de Projetos de Referencia - Codigo Fonte

**Atualizado em:** 2026-04-02

---

## PRIORIDADE 1 - Analisar codigo fonte em detalhe

### 1. Forge-AI-Player (dongge0210)
- **URL:** https://github.com/dongge0210/Forge-AI-Player
- **Branch:** 1.20.1
- **Licenca:** (verificar)
- **Plataforma:** Forge 1.20.1
- **Descricao:** Fork do AI-Player portado para Forge. O projeto MAIS PROXIMO do que Alice pretende ser.
- **Dependencias:** Carpet mod internals + ollama4j
- **Requer:** Java 21
- **Arquitetura identificada:**
  - Fake player via Carpet (ServerPlayer estendido)
  - Comunicacao com Ollama via ollama4j
  - Deep-Q learning para decisoes reativas (reflex actions)
  - Movimentacao customizada (nao usa Carpet's server-sided movement)
- **O que estudar:**
  - Como cria o fake player no Forge
  - Como traduz respostas do LLM em acoes do jogo
  - Como implementa percepcao do mundo
  - Pipeline LLM -> acao
  - Sistema de reinforcement learning para reflexos
- **Limitacoes conhecidas:**
  - Requer Java 21 (pode conflitar com mods que usam Java 17)
  - Depende de internals do Carpet (fragil)

### 2. HumanCompanions (justinwon777)
- **URL:** https://github.com/justinwon777/HumanCompanions
- **Licenca:** GPL-3.0
- **Plataforma:** Forge 1.16.5 / 1.18.1 / 1.18.2 / 1.19.2 / 1.20.1
- **Stars:** 4 | Forks: 18
- **Descricao:** Companion NPCs (knight, archer, axeguard, arbalist) com AI de seguir, combate, inventario.
- **O que estudar:**
  - **Sistema de AI Goals** (como registra goals de seguir/atacar/patrulhar)
  - **Auto-equip** (logica para equipar melhor arma/armadura)
  - **Inventario pessoal** (como gerencia inventario do companion)
  - **Healing** (come food para se curar)
  - **Modos** (follow/patrol/guard - toggle de stances)
  - **Spawning** (como spawna companions nas vilas)
  - **Fugir de creepers** (comportamento de fuga)
- **Codigo interessante para Alice:**
```
Provavelmente:
  - entity/CompanionEntity.java - classe principal
  - entity/ai/ - goals de IA (FollowOwnerGoal, etc.)
  - inventory/ - sistema de inventario
  - network/ - pacotes de rede
```

### 3. Duzo's FakePlayer
- **URL:** https://github.com/duzos/fakeplayer
- **Plataforma:** Forge + Fabric 1.20.1
- **Commits:** 125
- **Descricao:** NPCs com aparencia de jogador, skins via Mojang API
- **O que estudar:**
  - Implementacao de fake player entity para 1.20.1
  - Sistema de skin retrieval via Mojang API
  - Interacao via right-click (diferentes itens = diferentes acoes)
  - Combate defensivo
  - Wandering behavior
- **Bug conhecido:** Crash no servidor Forge por carregamento de codigo client-only
- **Licao:** Separar bem codigo client/server

### 4. Baritone
- **URL:** https://github.com/cabaletta/baritone
- **Branch:** 1.20.1 disponivel
- **Licenca:** LGPL 3.0
- **Stars:** 8.8k
- **O que estudar:**
  - API publica (`baritone.api` package)
  - IBaritone, IBaritoneProvider, IGoalProcess
  - GoalBlock, GoalXZ, GoalNear, GoalGetToBlock
  - MineProcess, FollowProcess, BuilderProcess
  - Como integrar via API jar
  - Settings configuracoes

---

## PRIORIDADE 2 - Estudar arquitetura

### 5. AI-Player Original (shasankp000)
- **URL:** https://github.com/shasankp000/AI-Player
- **Plataforma:** Fabric 1.21.1
- **Stars:** 109 | Forks: 18
- **Descricao:** "Second player" com RAG, task decomposition, multi-LLM
- **Arquitetura estudar:**
  - **RAG System:** Como constroi base de conhecimento Minecraft
    - Database + web search para informacoes factuais
    - Reduz alucinacoes do LLM
  - **Task Decomposition:** Instrucao complexa -> subtarefas -> execucao passo a passo
  - **Multi-LLM Provider:** Abstrai OpenAI/Anthropic/Gemini/Grok/Ollama
    - Qual interface usa?
    - Como troca entre providers?
  - **NLP-to-Action Pipeline:**
    - Quais acoes primitivas existem?
    - Como compoe acoes complexas de primitivas?
  - **Reinforcement Learning:** Aprendizado para acoes reflexas

### 6. Mindcraft (mindcraft-bots)
- **URL:** https://github.com/mindcraft-bots/mindcraft
- **Stars:** 5000+ | Forks: 737
- **Licenca:** MIT
- **Plataforma:** JavaScript (Mineflayer)
- **Descricao:** Multi-agente LLM jogando Minecraft. Melhor arquitetura LLM-to-action.
- **Arquitetura estudar:**
  - **Sistema de Prompts:**
    - Perfis JSON definindo personalidades
    - Como serializa estado do jogo para o LLM
    - System prompt structure
  - **"Collaborating Action by Action":** Raciocinio sequencial
  - **Multi-Model:**
    - Chat model: conversa
    - Code model: gerar codigo de acoes
    - Vision model: entender visuais
    - Embeddings model: memoria
  - **Skill/Action Definitions:**
    - Quais acoes disponibiliza
    - Como adiciona novas skills
    - Formato de skill definition
  - **17+ LLM providers:** Configuracao e troca entre providers

### 7. Voyager (MineDojo)
- **URL:** https://github.com/MineDojo/Voyager
- **Stars:** 6800 | Forks: 663
- **Licenca:** MIT
- **Descricao:** Primeiro agente LLM lifelong learning em Minecraft
- **Conceitos estudar:**
  - **Skill Library:** Armazenar skills executaveis que crescem com o tempo
    - Formato da skill
    - Como busca skill relevante
    - Como verifica se skill funcionou
  - **Automatic Curriculum:** Auto-definicao de objetivos progressivos
  - **Iterative Prompting:** Feedback do ambiente -> correcao -> retry
  - **Self-Verification:** Verificar se acao teve sucesso
  - **Resultados:** 3.3x mais itens, 15.3x mais rapido na tech tree vs baselines

### 8. ChatClef (elefant-ai)
- **URL:** https://github.com/elefant-ai/chatclef
- **Stars:** 12
- **Descricao:** AI copilot que joga Minecraft. Consegue zerar o jogo solo.
- **Arquitetura 3 camadas:**
  1. **LLM (Player2):** Interpreta instrucoes, decide acoes
  2. **Task Planner (AltoClef):** Decompoe tarefas em subtarefas
  3. **Pathfinding (Baritone):** Executa movimentacao
- **O que estudar:**
  - Interface entre camadas (o que cada camada recebe/retorna)
  - Como AltoClef decompoe tarefas
  - Como Baritone e chamado a partir de AltoClef

### 9. Player2NPC (Goodbird-git)
- **URL:** https://github.com/Goodbird-git/Player2NPC
- **Stars:** 10 | Forks: 11
- **Descricao:** Framework PlayerEngine - componentes modulares de player
- **Conceito-chave: Component Architecture**
  - Capacidades de player sao "building blocks" modulares
  - Qualquer entidade pode receber capacidades de player
  - Player2 LLM + AltoClef + Baritone como pipeline
- **O que estudar:**
  - Como funciona o sistema de componentes
  - Interface para implementar capacidades
  - Como qualquer mob ganha comportamento de player

### 10. CreatureChat
- **URL:** https://github.com/CreatureChat/creature-chat
- **Descricao:** IA para conversas com mobs. 100+ modelos via LiteLLM.
- **O que estudar:**
  - **LiteLLM Proxy:** Suporte a 100+ modelos via interface OpenAI-compatible
  - **Sistema de memoria:** Como persiste conversas entre sessoes
  - **Relationship tracking:** Sistema amigo-inimigo
  - **Behaviors reativos:** follow, flee, attack, protect baseados em relacao
  - **Multiplayer sync:** Como sincroniza estado entre jogadores

---

## PRIORIDADE 3 - Referencia pontual

### 11. SiliconeDolls (Anvil-Dev)
- **URL:** https://github.com/Anvil-Dev/SiliconeDolls
- **Plataforma:** NeoForge 1.21
- **O que estudar:** Sistema de comandos tick-based (sprint, crouch, jump, attack, use, look)
  - Acoes continuas medidas em ticks
  - Acoes por intervalo
  - Gerenciamento de grupo de bots

### 12. MC-Experiment-AI (marloncalleja)
- **URL:** https://github.com/marloncalleja/MC-Experiment-AI
- **Plataforma:** Forge 1.8.9
- **O que estudar:** Implementacao GOAP (Goal-Oriented Action Planning)
  - Action pools
  - Decision trees
  - Comparacao A* vs BFS vs DFS para planning
  - Aplicacao de GOAP para NPCs Minecraft

### 13. Automatone (Ladysnake)
- **URL:** https://github.com/Ladysnake/Automatone
- **Fork:** https://github.com/minefortress-mod/automatone
- **Plataforma:** Fabric
- **O que estudar:** Como Baritone foi adaptado para server-side
  - Quais mudancas foram necessarias
  - Como funciona com fake players
  - API de componentes (Cardinal Components)

### 14. Nations & Villagers AI Reborn
- **URL:** https://modrinth.com/mod/nations-villagers-ai-reborn
- **O que estudar:**
  - Integracao Piper TTS + eSpeak NG no Minecraft
  - Pipeline TTS completo
  - NPCs autonomos com planejamento
  - Sistema de economia e diplomacia
  - Fully offline com Ollama

### 15. Walkie-Talkie Mod (Flaton1)
- **URL:** https://github.com/Flaton1/walkie-talkie-mod
- **O que estudar:**
  - Como hook no Simple Voice Chat API
  - Sistema de frequencias/canais
  - Transmissao de audio sem limite de distancia
  - Implementacao de item craftavel

---

## MAPA DE REFERENCIA POR FEATURE

| Feature da Alice | Projeto principal | Projetos complementares |
|-----------------|-------------------|------------------------|
| Fake player entity | Duzo's fakeplayer | Forge-AI-Player, Carpet |
| Pathfinding | Baritone | Automatone, CoroUtil |
| Combate AI | HumanCompanions | SiliconeDolls |
| Inventario/auto-equip | HumanCompanions | Metropolize Companions |
| Comunicacao LLM | ollama4j/LangChain4j | Forge-AI-Player, CreatureChat |
| Chat natural | CreatureChat | AI-Player, Mindcraft |
| Task decomposition | AI-Player | ChatClef, Voyager |
| Memoria/persistencia | CreatureChat | Voyager (skill library) |
| TTS (voz) | Piper + piper-jni | Nations & Villagers AI |
| STT (ouvir) | Vosk | whisper-jni |
| Audio no jogo | Simple Voice Chat API | Walkie-Talkie mod |
| Receitas/crafting | RecipeManager nativo | JEI API |
| Construcao schematics | Create Schematicannon | Forgematica |
| GOAP/planning | MC-Experiment-AI | Voyager |
| Radio comunicacao | Walkie-Talkie mod | Simple Voice Chat API |
