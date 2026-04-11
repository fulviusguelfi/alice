# Projeto Alice - Documento de Brainstorm e Levantamento de Requisitos

**Data de inicio:** 2026-04-01  
**Ultima atualização:** 2026-04-03  
**Status:** CONCLUIDO — Brainstorm completo, 49 decisões tomadas  
**Versão:** 0.9

---

## 1. Visão Geral do Projeto

Alice é um mod para Minecraft Forge 1.20.1 que adiciona uma jogadora controlada por IA ao jogo. Alice é uma personagem feminina que atua como companheira de sobrevivência e amiga do jogador.

### Identidade da Alice

- **Nome:** Alice
- **Gênero:** Feminino
- **Aparência:** Menina ruiva (skin customizada)
- **Personalidade:** Amiga, companheira, protetora, inteligente, pragmática. Motivada por segurança própria e do grupo.
- **Papel:** Engenheira de sobrevivência e guia do jogador. Alice ORIENTA o jogador durante o jogo.
- **Consciência:** Alice sabe que é uma IA dentro do mundo do jogo. Ela tem "lembranças de outra vida" — conhecimento compilado de fontes públicas da internet sobre o apocalipse zumbi, engenharia de sobrevivência, mecânicas do jogo, receitas, estruturas, etc. Ela trata essas informações naturalmente como memórias de antes de existir naquele mundo.
- **Lore:** Alice "estudou" este apocalipse zumbi em sua vida anterior. Ela sabe informações cruciais que compartilha com o jogador conforme a fase de jogo progride. Essas informações são compiladas de fontes confiáveis e públicas, tornando Alice uma fonte confiável de conhecimento.
- **Aprendizado:** Alice confronta a informação que tem com a experiência própria e do jogador para aprimorar seu conhecimento. Se algo que ela "lembrava" se prova errado na prática, ela atualiza seu entendimento.
- **Motivação principal:** Ficar segura. Por isso aprecia estar com o jogador para viver aventuras juntos. Pode ficar sozinha se o local for seguro ou se for necessário para ajudar.
- **Por que protege-la:** Alice, como gesto de amizade, compartilha toda sua informação e ainda constrói coisas para o jogador. Por isso o jogador deveria protege-la — ela é valiosa demais para perder.

### Contexto do Servidor

- **Modpack:** Cursed Walking - A Modern Zombie Apocalypse (201 mods)
- **Tematica:** Apocalipse zumbi com hordas, zumbis mutantes, infeccao, armas de fogo, cidades, blood moons, armaduras modernas, armas avancadas de endgame
- **Plataforma:** Minecraft 1.20.1 Forge
- **CurseForge:** https://www.curseforge.com/minecraft/modpacks/cursed-walking-a-modern-zombie-apocalypse
- Alice deve ser compatível com todos os mods do modpack
- **Lista completa de mods:** Pendente — será extraida diretamente do servidor via acesso SSH (ver Infraestrutura)

### Infraestrutura de Desenvolvimento — 3 Maquinas

```
MAQUINA 1 (CLIENTE)          MAQUINA 2 (SERVIDOR MC)       MAQUINA 3 (OLLAMA/IA)
========================     ========================      ========================
CPU:  AMD Ryzen 5 4500       IP: 192.168.0.225             IP: 192.168.0.200
RAM:  16GB (13GB pro MC)     Portainer (Docker)            Usuario: bill
GPU:  GTX 1050 Ti 4GB        Crafty Controller             Ollama v0.18.2
OS:   Windows 11             Forge 1.20.1 + Cursed Walking CPU pura: 2 tok/s
                             SSH disponivel                Modelos: llama3.2:latest
Usos:                                                               phi3:3.8b-mini
- TLauncher (jogar)          Usos:                         API: http://192.168.0.200:11434
- Desenvolvimento do mod     - Rodar o servidor MC
- Claude Code (VSCode)       - Hospedar mundos
- Git/build/testes           - Logs e administracao
                             - faster-whisper (Docker)
                               :10300 CONFIRMADO RODANDO
```

**Fluxo de trabalho:**
1. Desenvolvimento do mod na Maquina 1 (VSCode + Claude Code)
2. Build do mod na Maquina 1 (Gradle)
3. Deploy do .jar no servidor: copiar para Maquina 2 via SSH/Portainer
4. Testar: Maquina 1 (TLauncher) conecta no servidor da Maquina 2
5. Alice chama LLM: mod faz HTTP para Maquina 3 (Ollama :11434)
6. Alice transcreve voz: mod faz HTTP para Maquina 2 (faster-whisper :10300)
7. Alice sintetiza voz: mod faz HTTP para Edge TTS (cloud Microsoft)

**Performance do Ollama (Maquina 3) — Medido em 2026-04-03:**
- Modelo: llama3.2:latest (3.2B, Q4_K_M, 1.9GB)
- Velocidade: **2.0 tokens/segundo** (CPU pura, sem GPU)
- Tempo por resposta curta (~40 palavras): **~76 segundos**
- **Diagnostico:** Ollama rodando 100% em CPU — sem GPU configurada ou sem GPU disponível
- **Impacto:** Respostas de Alice serão LENTAS com configuração atual
- **Solução a curto prazo:** Usar modelos menores (phi3:mini ja instalado, 2.2GB)
- **Solução ideal:** Configurar GPU no Ollama OU usar API remota como primary
- **Pendencia:** Verificar hardware da Maquina 3 via SSH (auth por senha não funcionou, tentar chave SSH)

**Nota sobre Q22 revisada:** O gargalo não é a falta de GPU na maquina cliente (GTX 1050 Ti), mas sim que a maquina do Ollama parece rodar só em CPU. Isso muda a estrategia — API remota (deepseek-v3.2:cloud etc. ja instalados na Maquina 1) pode ser o caminho mais viavel para responsividade.

---

## 2. Registro de Decisões

| # | Data | Decisão | Justificativa |
|---|------|---------|---------------|
| 1 | 2026-04-01 | Mod Forge 1.20.1 | Servidor ja roda Cursed Walking nesta versão |
| 2 | 2026-04-01 | Nome: Alice (feminino) | Definido pelo escopo inicial |
| 3 | 2026-04-01 | Arquitetura hibrida: scripts + IA | Performance - IA decide O QUE, scripts executam COMO |
| 4 | 2026-04-01 | Modpack alvo: Cursed Walking | Servidor existente do usuario |
| 5 | 2026-04-01 | Backend IA: Ollama local + API remota | Objetivo final e local, API remota como fallback |
| 6 | 2026-04-01 | 1 Alice por servidor (MVP) | Futuro: multiplas Alices |
| 7 | 2026-04-01 | Spawn via ovo no inventário | Ovo colocado no chao invoca Alice |
| 8 | 2026-04-01 | Alice não perde itens ao morrer | Renasce com tudo que tinha |
| 9 | 2026-04-01 | Respawn: cama ou ponto original do ovo | Igual jogador, ponto do ovo nunca se perde |
| 10 | 2026-04-01 | Ao renascer busca base ou jogador | Escolhe o mais seguro/fácil |
| 11 | 2026-04-01 | Frase-chave "vamos combinar assim" | Indica que o jogador quer dar uma tarefa/comando |
| 12 | 2026-04-01 | Alice deve saber usar TUDO do jogo | Armas, ferramentas, itens de todos os mods |
| 13 | 2026-04-01 | Alice conhece todas as receitas | Via RecipeManager nativo + JEI API |
| 14 | 2026-04-01 | Alice sabe construir via schematics | Estruturas pré-carregadas no mod |
| 15 | 2026-04-01 | Alice tem memória precisa de inventários | Próprio, do jogador, e de containers da base |
| 16 | 2026-04-02 | Alice tem voz (TTS) | **ATUALIZADO #47:** Edge TTS cloud pt-BR-FranciscaNeural (gratuito, ilimitado) |
| 17 | 2026-04-02 | Alice ouve o jogador (STT) | **ATUALIZADO #48:** faster-whisper REST (Machine 2, Docker) |
| 18 | 2026-04-02 | Comunicação por voz e radio | Proximidade + radio craftavel para distância |
| 19 | 2026-04-02 | Idioma: Portugues BR (MVP) | Outros idiomas em versões futuras |
| 20 | 2026-04-02 | Voz feminina padrão (MVP) | Uma única voz fixa para Alice |
| 21 | 2026-04-02 | ~~Entidade: Custom Mob~~ **→ REVISADO: FakePlayer (ServerPlayer)** | **REVISADO #46:** FakePlayer resolve Baritone nativamente + herda inventário, morte, skin, Create, SVC |
| 22 | 2026-04-02 | ~~Pathfinding: Baritone nativo via FakePlayer~~ **→ REVISADO #50** | Descoberto em 2026-04-11 que Baritone é client-side e FakePlayer não resolve o acoplamento nativamente. Automatone e PlayerEngine são Fabric-only. Ver decisão #50. |
| 23 | 2026-04-02 | Faseamento completo definido e escopado | Concordancia com fases, mas projeto inteiro bem definido até o fim |
| 24 | 2026-04-02 | Alice é guia/orientadora do jogador | Objetivo principal: orientar o jogador durante o jogo |
| 25 | 2026-04-02 | Alice tem lore de ter estudado o apocalipse | "Lembranças de outra vida" = pesquisa sobre o apocalipse zumbi |
| 26 | 2026-04-02 | Base de conhecimento compilada de fontes públicas | Alice é fonte confiável; confronta info com experiência |
| 27 | 2026-04-02 | Alice é uma engenheira de sobrevivência | Sabe estruturas, defesas, contraptions Create, otimizacoes |
| 28 | 2026-04-02 | memória persistente via repositorio Git | Repo público, branches por sessao, PRs para consolidar, master = estado atual |
| 29 | 2026-04-02 | Alice usa agentes, skills e regras de IA | Tudo disponível para IA deve ser usado para melhorar resposta e latência |
| 30 | 2026-04-03 | Simple Voice Chat confirmado no modpack | Q13 resolvida — Cursed Walking ja inclui SVC |
| 31 | 2026-04-03 | Repositorio de memória: GitHub público | Q20 resolvida — criar repo no GitHub quando iniciar projeto |
| 32 | 2026-04-03 | Hardware LLM limitado: 1 modelo, lento | Q22 resolvida — otimizar para modelo único, pré-cache, skills compiladas |
| 33 | 2026-04-03 | LLM library: ollama4j (MVP) | Q08 resolvida — leve, tool calling funciona, interface abstrata para trocar depois |
| 34 | 2026-04-03 | Skin: "Scarlet (Remake)" do Planet Minecraft | Q24 resolvida — https://www.planetminecraft.com/skin/scarlet-remake/ |
| 35 | 2026-04-03 | Sem radio próprio — usar Simple Voice Chat diretamente | Q11 resolvida — SVC ja está no modpack com todas as features de radio |
| 36 | 2026-04-03 | Alice offline = player offline (fica no mundo, inativa) | Q14 resolvida — comportamento idêntico a player desconectado |
| 37 | 2026-04-03 | Schematics: nenhum padrão, .nbt, só pré-carregados, LLM sugere | Q10 resolvida — jogador carrega seus próprios .nbt |
| 38 | 2026-04-03 | Sistema de ameaça: fatores pesados, toggleaveis, limiar configurável | Q15a+b resolvida |
| 39 | 2026-04-03 | Padrão de Comunicação Eficiente (PCE) definido | Q15c resolvida — protocolo de aviso+confirmacao para situações de perigo |
| 40 | 2026-04-03 | Animações padrão do player + waypoints via Map Atlases | Q16 resolvida |
| 41 | 2026-04-03 | Redstone/Create: CONFIRMADO com FakePlayer | Q17 resolvida — FakePlayer interage com tudo nativamente |
| 42 | 2026-04-03 | Hierarquia social: Amigo (dono do ovo) > Conhecido > Hostil | Q18 resolvida |
| 43 | 2026-04-03 | Sem restrição de tamanho para o mod | Q19 resolvida |
| 44 | 2026-04-03 | Base de conhecimento: Opção C hibrida, confirmacao por experiência | Q21 resolvida |
| 45 | 2026-04-03 | Maquina do Ollama: CPU pura, 2 tok/s, llama3.2 e phi3 instalados | Q22 complemento — sem GPU na maquina do Ollama |
| 46 | 2026-04-03 | Entidade Alice: FakePlayer (ServerPlayer) — decisão #21 REVISADA | Q23b confirmado — FakePlayer substitui Custom Mob. Resolve Baritone, Create, SVC, inventário, morte, skin |
| 47 | 2026-04-03 | TTS: Edge TTS cloud pt-BR-FranciscaNeural — decisão #16 REVISADA | Q12 resolvida — gratuito, ilimitado, qualidade neural, sem API key. Piper descartado (sem voz feminina pt_BR) |
| 48 | 2026-04-03 | STT: faster-whisper (Machine 2, Docker/Portainer) | Q12 complemento STT — REST API OpenAI-compatível, gratuito, ilimitado, estado da arte em reconhecimento de fala |
| 49 | 2026-04-03 | Modo voz offline configurável (Piper TTS + Vosk STT) | Fallback ativado por config quando Edge TTS ou Machine 2 estiver indisponivel. Modo: online (padrão) ou offline (local). |
| 50 | 2026-04-11 | Pathfinding: port manual Baritone para Forge server-side; Plan B = Alice-Service (projeto paralelo) | Revisa #22. Rota principal = porte manual via spikes timeboxed (2 semanas hard cap, 6 spikes com kill individual + agregado). Plan B NÃO é fallback runtime: é pivot de projeto que pausa Alice e inicia projeto paralelo Alice-Service (serviço WebSocket em máquina da rede, cap 6 semanas). Ver [docs/spikes/pathfinding-port/README.md](../spikes/pathfinding-port/README.md) e [docs/planejamento/plano-b-alice-service.md](../planejamento/plano-b-alice-service.md). Precedente acadêmico: Microsoft Malmo. Honestidade registrada: sem precedente industrial maduro em Forge 1.20.1. |

---

## 3. Funções da Alice

### 3.0 Função Central: Guia e Orientadora do Jogador (decisão #24)

Alice existe para ORIENTAR o jogador durante o jogo. Tudo que ela faz serve a esse proposito.

#### FG01 - Base de Conhecimento Confiável (decisão #26)

- Alice possui uma base de conhecimento compilada de fontes públicas e confiáveis da internet
- Essa base contém informações sobre:
  - Mecânicas do Minecraft e de TODOS os mods do Cursed Walking
  - Estrategias de sobrevivência em apocalipse zumbi
  - Receitas, materiais, progressao
  - Estruturas defensivas e seus pros/contras
  - Contraptions e maquinas do Create mod
  - Armas, armaduras, ferramentas e como usa-las eficientemente
  - Farming, criação de animais, fontes de comida
  - Infeccao zumbi, blood moons, ameaças especiais do modpack
- Essas informações são as "lembranças de outra vida" da Alice (lore, decisão #25)
- **Formato:** Embutidas no system prompt do LLM + RAG para busca contextual
- **Atualização:** Alice confronta informação com experiência própria e do jogador
  - Se algo se prova errado na prática, Alice atualiza seu entendimento
  - Se descobre algo novo, registra na memória

#### FG02 - Engenheira de Sobrevivência (decisão #27)

- Alice sabe avaliar se uma estrutura e boa ou ruim para se defender
- Sabe quais maquinas e contraptions do Create são apropriadas para cada situação
- Sugere otimizacoes e melhorias na base do jogador
- Sabe construir as coisas que recomenda (via schematics)
- Informa o jogador sobre riscos estruturais ("essa parede não vai aguentar uma horda")
- Recomenda upgrades conforme a fase de jogo progride

#### FG03 - Orientacao por Fase de Jogo

- Alice compartilha informações conforme a fase de jogo em que o jogador está
- **Early game:** Foco em abrigo, comida, armas básicas, evitar infeccao
- **Mid game:** Foco em base fortificada, farming, armas melhores, explorar cidades
- **Late game:** Foco em armas avancadas, armaduras endgame, contraptions Create, blood moons
- Alice não despeja tudo de uma vez — revela informação conforme contexto e necessidade
- Usa a experiência acumulada para calibrar quando é o que falar

### 3.1 Função Primaria: Companheira de Sobrevivência

#### FP01 - Conhecimento de Metas e Quests

- Sabe quais são as conquistas e quests do jogador
- Fornece informações sobre o que fazer a seguir
- Acompanha progresso e sugere próximos passos

#### FP02 - Crafting e Construção

- Conhece TODAS as receitas de craft do jogo (incluindo mods) via RecipeManager + JEI API
- Sabe craftar qualquer item
- Constrói estruturas via schematics pré-carregados no mod
- Constrói contraptions do mod Create via schematics
- Fornece lista de materiais necessários indicando quanto falta e quais faltam

#### FP03 - Gestao de Recursos e memória

- memória precisa do inventário dela e do jogador
- memória do conteúdo de containers dentro dos inventários
- Lembra tudo que está guardado nos containers que ela ou o jogador tenham craftado ou mudado de lugar no jogo, e sabe onde está cada item
- Ajuda a coletar materiais (pode coletar o mesmo material ou outro da lista)
- Coordena com o jogador a divisao de tarefas de coleta

#### FP04 - Combate e Avaliação de Ameaças

- Ajuda o jogador em combate
- **Sistema de regua de nível de ameaça**: mede pelo que sabe/sente do jogo quando e hora de combater e quando e hora de fugir
- Avisa o jogador sobre perigos
- Prioridade: ajudar o jogador a escapar quando necessário
- Sabe usar todas as armas do jogo (incluindo armas de fogo do modpack)

#### FP05 - Uso Universal de Itens

- Sabe usar TUDO que existe no jogo
- Sabe criar qualquer item e conhece os materiais necessários
- Coleta materiais junto com o jogador ou de forma independente
- Entende "combinações" com o jogador como comandos/ordens prioritarias

### 3.2 Função Secundaria: Companhia e Conversa

#### FS01 - Conversas Variadas

- Conversa sobre atualidades, ciencia, história, filosofia
- Atua como um amigo que ajuda e conversa
- Comenta por vezes sobre atualidades que pesquisou (via "lembranças de outra vida")

#### FS02 - memória de Aventuras

- Lembra de aventuras vividas juntos no jogo
- Recorda desafios, dificuldades e vitorias
- Usa essas memórias em conversas futuras

### 3.3 Sistema de Comandos

- **Frase-chave:** "vamos combinar assim" - ativa modo de receber tarefa
- Combinações com o jogador tem peso forte (ordem + tarefa de sobrevivência)
- Sempre aberta a combinar tarefas com o jogador
- Entende linguagem natural para instrucoes
- Pode receber comandos por voz (proximidade) ou por radio (distância)

### 3.4 Sistema de Voz

#### VOZ01 - Alice Fala (TTS)

- Alice fala em portugues BR com voz feminina (pt-BR-FranciscaNeural)
- Fala por proximidade quando perto do jogador (via Simple Voice Chat)
- Fala pelo canal SVC a distância quando longe do jogador
- TTS via Microsoft Edge TTS cloud (decisão #47) — sem arquivos locais no mod

#### VOZ02 - Alice Ouve (STT)

- Alice ouve a voz do jogador quando próximo (via Simple Voice Chat)
- Alice ouve pelo canal SVC quando distante
- STT via faster-whisper rodando em Machine 2 via Docker (decisão #48) — REST OpenAI-compatible
- Converte voz em texto -> envia para LLM -> gera resposta

#### VOZ03 - Comunicação a Distância (decisão #35)

- **NÃO ha item radio próprio do mod Alice** — decisão #35 cancelou item customizado
- Comunicação a distância usa o sistema de **grupos e canais do Simple Voice Chat** diretamente
- SVC ja está no Cursed Walking e ja tem todas as funcionalidades necessárias
- O mod **Walkie-Talkie** (também no Cursed Walking) ja implementa mecânica de radio com item — jogador pode usar ele
- Alice usa a API do SVC para falar/ouvir de qualquer distância via canal/grupo
- Zero esforço de implementação de item: zero crafting recipe, zero item GUI, zero texture
- **Referência:** Simple Voice Chat API (grupos/canais), Walkie-Talkie mod (comportamento esperado)

#### VOZ04 - Modo Offline / Fallback de Voz (decisão #49)

- Ativado quando Edge TTS (cloud) ou faster-whisper (Machine 2) estiver indisponivel
- Configurado via: `alice.voice.mode=online|offline|text_only`
- **IMPORTANTE:** No modo offline, Alice usa voz MASCULINA (não ha voz feminina pt-BR no Piper)
  - Piper TTS so tem `pt-br-faber-medium` para portugues brasileiro — voz masculina
  - Isso é uma degradacao intencional e aceitavel para um fallback de emergencia
  - No modo `online` (padrão) Alice tem voz feminina neural (FranciscaNeural)
- **TTS offline:** Piper TTS rodando localmente — modelo `pt-br-faber-medium`
- **STT offline:** Vosk rodando localmente — modelo para pt-BR (verificar versão atual em alphacephei.com/vosk/models — nomes de modelos mudam entre releases; buscar "vosk-model-pt" ou "vosk-model-small-pt")
- Ambos (Piper e Vosk) rodam 100% local: zero rede, zero Docker, zero dependência de infra externa
- Qualidade reduzida mas funcionalidade preservada — voz funciona mesmo sem internet e sem Machines 2/3
- **Tres modos:**
  - `online` (padrão): Edge TTS cloud (feminina, neural) + faster-whisper REST (Machine 2)
  - `offline`: Piper local (masculino, básico) + Vosk local (reconhecimento local)
  - `text_only`: sem voz alguma — apenas chat texto no jogo
- **Referência:** piper-tts (rhasspy/piper), vosk-api (alphacephei/vosk)

---

## 4. Requisitos Funcionais Detalhados

### RF01 - Entidade Alice

- **Tipo:** FakePlayer (ServerPlayer) — decisão #21 REVISADA, decisão #46
- **Classe base:** `net.minecraftforge.common.útil.FakePlayer` (extende ServerPlayer)
- **Aparência:** Menina ruiva com skin Scarlet (decisão #34) — funciona out-of-the-box no FakePlayer
- Spawn via ovo próprio no inventário do jogador
- Ponto de spawn original (onde o ovo foi colocado) nunca se perde
- Funciona em singleplayer e multiplayer
- **Aparece na TAB** como jogador na lista de jogadores do servidor
- **Inventário nativo:** 36 slots + 4 armor + offhand — sem implementação custom
- **Morte e respawn nativos:** Herda toda a lógica do ServerPlayer
- **Interação com blocos:** Qualquer bloco (redstone, crafting table, Create) — igual jogador real
- **Referência:** Duzo's Alice, Automatone, ChatClef, Forge FakePlayer API

### RF02 - Morte e Respawn

- Morre como jogador normal
- NÃO perde itens, armaduras ou qualquer coisa ao morrer
- Respawna na cama ou no ponto original do ovo
- Ao renascer: busca ir para a base ou para perto do jogador (escolhe o mais seguro)
- **Referência:** HumanCompanions (sistema de respawn)

### RF03 - Movimentacao e Navegação

- **Pathfinding:** Baritone — decisão #22
- Pathfinding autonomo (A*, desvio de obstaculos, pular, nadar, minerar, construir)
- Seguir jogador (modo follow)
- Ficar parada (modo stay)
- Navegar independentemente para locais conhecidos
- Mine/follow/build embutidos no Baritone
- **Integração com FakePlayer:** Baritone foi projetado para controlar ServerPlayer — funciona nativamente com FakePlayer sem nenhum adapter ou workaround. O conflito técnico original (Q23) foi eliminado pela mudança da decisão #21 para FakePlayer.
- **Referência:** Baritone, ChatClef, Automatone (todos usam Baritone + FakePlayer/Player)

### RF04 - Combate

- Atacar mobs hostis com qualquer arma (incluindo armas de fogo)
- Sistema de avaliação de ameaça (regua de perigo)
- Decisão autonoma: lutar vs fugir
- Alertar jogador sobre perigos (por chat, voz ou radio)
- Equipar melhor arma/armadura disponível
- **Referência:** HumanCompanions (combate), SiliconeDolls (ações tick-based)

### RF05 - Crafting e Receitas

- Acesso programatico a TODAS as receitas via `RecipeManager.getAllRecipesFor()`
- Tipos suportados: CRAFTING, SMELTING, BLASTING, SMOKING, CAMPFIRE, STONECUTTING, SMITHING
- Inclui receitas de TODOS os mods automaticamente
- JEI API como soft dependency para lookup mais rico
- Capacidade de craftar itens autonomamente
- Informar jogador sobre receitas e materiais necessários
- Calcular materiais faltantes considerando todos os containers mapeados
- **Referência:** Minecraft RecipeManager API, JEI API

### RF06 - Gestao de Inventário e Containers

- Gerenciar próprio inventário
- Tracking do inventário do jogador
- **Mapeamento de containers:** Registrar localização e conteúdo de todos os containers que Alice ou o jogador tenham craftado ou mudado de lugar no jogo
- Atualizar mapa de containers quando conteúdo muda
- Equipar automaticamente melhores itens
- Saber onde encontrar cada item nos containers mapeados
- **Persistência:** Salvar mapa de containers via Forge SavedData API
- **Referência:** HumanCompanions (auto-equip), Metropolize Companions

### RF07 - Construção via Schematics

- Carregar schematics pré-incluidos no mod (formato .nbt)
- Construir estruturas bloco a bloco
- Construir contraptions do Create
- Algoritmo de construção: bottom-up, sólidos primeiro, suportados por ultimo
- Informar materiais necessários antes de construir
- **Referência:** Create Schematicannon, Forgematica

### RF08 - Comunicação por Texto (Chat)

- Conversa natural via chat do jogo
- Recebe comandos via linguagem natural
- Frase-chave "vamos combinar assim" para modo tarefa
- Conversa casual (ciencia, história, filosofia, atualidades)
- Lembra e referência aventuras passadas
- **Referência:** CreatureChat, AI-Player, Mindcraft

### RF09 - Comunicação por Voz

- **TTS (Alice fala) — decisão #47:**
  - **Microsoft Edge TTS** — voz `pt-BR-FranciscaNeural` (feminina, neural, excelente qualidade)
  - Alternativa: `pt-BR-ManuelaNeural` (segunda voz feminina BR disponível)
  - Gratuito, sem API key, sem cadastro, sem limite documentado de uso
  - Usa o mesmo motor do Windows Narrator e Edge browser — qualidade neural da Microsoft
  - Chamada HTTP simples: POST com SSML -> resposta mp3/opus
  - Requer internet no servidor (confirmado disponível)
  - Audio recebido: mp3 -> decoder -> PCM 48kHz -> Simple Voice Chat

- **STT (Alice ouve) — decisão #48:**
  - **faster-whisper** rodando em Machine 2 (192.168.0.225) via Docker/Portainer — **CONFIRMADO RODANDO**
  - REST API OpenAI-compatível: `POST http://192.168.0.225:10300/v1/audio/transcriptions`
  - Modelo: Systran/faster-whisper-small (ou tiny para menor RAM)
  - Gratuito, sem limites, open source, roda local
  - Java chama via HTTP — mesma abordagem que o Ollama
  - Whisper é o estado da arte em STT — treinado em 680k horas de audio, pt_BR excelente
  - Captura audio do jogador via Simple Voice Chat API -> envia para faster-whisper -> texto

- **Integração com FakePlayer (decisão #46):**
  - Alice é um FakePlayer — Simple Voice Chat ja suporta players nativamente
  - Não precisa de plugin especial de entidade de voz (ao contrario do Custom Mob)
  - SVC trata Alice exatamente como qualquer outro jogador para fins de audio

- **Pipeline completo (atualizado):**
  1. Jogador fala (mic) -> Simple Voice Chat captura audio (opus)
  2. Audio -> decode opus -> PCM -> faster-whisper REST (Machine 2, Docker) -> texto
  3. Texto -> LLM Ollama (Machine 3) -> resposta texto
  4. Resposta -> Edge TTS HTTP (cloud Microsoft) -> audio mp3
  5. Audio mp3 -> decode -> PCM 48kHz -> Simple Voice Chat API -> jogador ouve Alice

- **Latência estimada (novo pipeline):**
  - STT faster-whisper: ~300-500ms (local, rápido)
  - LLM Ollama: ~2-5s (CPU, maquina 3)
  - TTS Edge TTS: ~200-400ms (rede, Microsoft rápido)
  - **Total: ~3-6 segundos** (domina o LLM, não o TTS/STT)

- **Dependência obrigatória:** Simple Voice Chat mod instalado no servidor e cliente

```java
// Plugin SVC — FakePlayer funciona como player real, sem codigo especial de entidade
@ForgeVoicechatPlugin
public class AliceVoicechatPlugin implements VoicechatPlugin {
    public String getPluginId() { return "alice_voice"; }
    public void initialize(VoicechatApi api) { /* setup channels */ }
}

// TTS via Edge TTS — chamada HTTP simples
String ssml = "<speak version='1.0' xml:lang='pt-BR'>"
            + "<voice name='pt-BR-FranciscaNeural'>" + texto + "</voice></speak>";
// POST https://[edge-tts-endpoint] -> mp3 bytes -> PCM -> SVC

// STT via faster-whisper — OpenAI-compatible endpoint
// POST http://192.168.0.225:10300/v1/audio/transcriptions
// multipart: file=audio.wav, model=whisper-large-v3, language=pt
```

- **Modo Offline (decisão #49):**
  - `alice.voice.mode=offline` ativa Piper (TTS local) + Vosk (STT local)
  - `alice.voice.mode=text_only` desativa voz completamente
  - Vosk: `vosk-model-small-pt-0.3` (45MB, OpenJDK compatível, zero infra adicional)
  - Piper: `pt-br-faber-medium` (única opção pt-BR — voz masculina, degradacao aceitavel em fallback)
  - Offline mode e fallback de emergencia — modo padrão contínua sendo `online`

- **Referência:** Simple Voice Chat API, edge-tts (biblioteca Python para estudo do endpoint), faster-whisper, openai-whisper-server, Walkie-Talkie mod, vosk-api (Java binding), piper-tts

### RF10 - memória e Persistência (decisão #28)

- **memória de curto prazo:** Contexto atual da conversa (janela de tokens do LLM)
- **memória de longo prazo:** Aventuras, decisões, locais, jogadores
  - Salva via Forge SavedData API (persistência local no mundo)
  - Formato: CompoundTag (NBT) com JSON serializado
  - Persiste entre sessoes do servidor
- **memória de inventários:** Mapa de containers (localização + conteúdo)
- **"Lembranças de outra vida":** Base de conhecimento compilada (embutida no system prompt + RAG)
- **memória de combinações:** Tarefas acordadas com o jogador (prioridade alta)

#### RF10.1 - memória Persistente via Repositorio Git (decisão #28)

- **Conceito:** Alice usa um repositorio Git público como memória de longo prazo portável
- **Motivação:** A memória não fica presa a um servidor específico. Qualquer nova sessao de jogo, em qualquer servidor, pode ler o estado mais recente da memória da Alice.
- **Estrutura do repositorio:**
  ```
  alice-memory/
  ├── README.md                    # Estado atual resumido da Alice
  ├── knowledge/                   # Base de conhecimento compilada
  │   ├── game-mechanics.md        # Mecanicas do Minecraft e mods
  │   ├── survival-strategies.md   # Estrategias de sobrevivencia
  │   ├── recipes-tips.md          # Dicas de receitas e crafting
  │   └── structures.md            # Estruturas e defesas
  ├── memories/                    # Memorias de experiencia
  │   ├── adventures.md            # Aventuras vividas
  │   ├── locations.md             # Locais conhecidos e mapeados
  │   ├── players.md               # Jogadores conhecidos e relacoes
  │   └── lessons-learned.md       # Licoes aprendidas na pratica
  ├── skills/                      # Biblioteca de skills aprendidas
  │   ├── combat/                  # Skills de combate
  │   ├── building/                # Skills de construcao
  │   └── crafting/                # Skills de crafting otimizadas
  ├── sessions/                    # Logs de sessoes
  │   └── YYYY-MM-DD-session.md    # Resumo de cada sessao
  └── config/                      # Configuracoes e personalidade
      ├── personality.md           # Tracos de personalidade atuais
      └── preferences.md           # Preferencias aprendidas do jogador
  ```
- **Fluxo de branches:**
  ```
  master (estado consolidado mais recente)
    └── develop (integracao)
          ├── session/2026-04-02-server1 (sessao atual)
          ├── session/2026-04-03-server1 (proxima sessao)
          └── session/2026-04-02-server2 (outro servidor)
  ```
- **Ciclo de vida de uma sessao:**
  1. Alice inicia sessao de jogo
  2. Faz checkout da `master` (estado mais recente consolidado)
  3. Cria branch `session/<data>-<servidor>`
  4. Durante o jogo, faz commits com memórias novas, skills aprendidas, etc.
  5. Ao encerrar sessao (jogador desconecta ou servidor para), faz push da branch
  6. Cria PR automático da branch da sessao para `develop`
  7. PR e consolidado (merge) — pode ser automático ou revisado
  8. Periodicamente, `develop` e mergeado na `master`
- **Vantagens:**
  - memória portável entre servidores e maquinas
  - Histórico completo de tudo que Alice viveu (git log)
  - Diff entre sessoes (o que mudou)
  - Rollback se algo der errado
  - Repo público = transparencia total
  - Outros agentes/processos podem ler e contribuir
  - Funciona como "cerebro" compartilhado se houver multiplas Alices no futuro
- **Implementação técnica:**
  - Biblioteca Java para Git: JGit (Eclipse) ou chamadas `git` via ProcessBuilder
  - Autenticação: SSH key ou token de acesso pessoal
  - Operações Git rodam em thread separada (não bloqueia game tick)
  - Commits são batched (não a cada tick, mas a cada evento significativo)
- **Referência:** Git Context Controller (paper), git-native semantic memory, GitHub Copilot agentic memory

- **Referência geral RF10:** Forge SavedData API, CreatureChat, Voyager skill library, JGit

### RF11 - Consciência de Quests

- Acessar sistema de quests/conquistas do jogador
- Sugerir próximos passos baseado em quests ativas
- Acompanhar progresso
- **Dependência:** FTB Quests (se presente no modpack)
- **Referência:** FTB Quests API

### RF12 - Percepção do Mundo

- Detectar mobs hostis num raio configurável (AABB scanning)
- Detectar jogadores próximos
- Detectar itens no chao
- Monitorar blocos quebrados/colocados próximos
- Escutar chat do servidor
- Reagir a eventos: LivingHurtEvent, EntityJoinLevelEvent, ServerChatEvent
- **Referência:** Forge Event System

### RF13 - Base de Conhecimento Compilada (decisão #26)

- Alice carrega uma base de conhecimento pré-compilada sobre o jogo é o apocalipse
- **Fontes de informação:**
  - Wikis públicas do Minecraft e dos mods do Cursed Walking
  - Guias de sobrevivência e estrategias publicados pela comunidade
  - Documentacao oficial dos mods (Create, armas de fogo, etc.)
  - Mecânicas de infeccao zumbi, blood moons, hordas
  - Receitas e progressao do modpack
- **Formato de armazenamento:**
  - Arquivos markdown no repositorio Git da Alice (knowledge/)
  - Indexados via embeddings para busca semantica (RAG)
  - Carregados no system prompt do LLM conforme contexto
- **Entrega progressiva:**
  - Alice não despeja tudo de uma vez
  - Compartilha informação conforme a fase de jogo é o contexto
  - Usa triggers: quando o jogador encontra um item novo, entra em area nova, enfrenta ameaça nova
- **Atualização:**
  - Alice confronta informação com experiência (decisão #26)
  - Se algo se prova errado, atualiza no repositorio Git
  - Se descobre algo novo via experiência, registra como skill/memória
- **Referência:** Voyager (RAG + skill library), AI-Player (RAG system), Mindcraft (system prompts)

### RF14 - Sistema de Agentes e Skills (decisão #29)

Alice usa uma arquitetura de agentes e skills para maximizar a qualidade das respostas e minimizar latência.

#### RF14.1 - Skills (Capacidades Modulares)

- **Definição:** Skill = pacote composável de instrucoes, código e recursos que Alice carrega sob demanda
- **Estrutura de uma skill:**
  ```
  skills/
  └── combat-zombie-horde/
      ├── SKILL.md              # Metadata + instrucoes
      ├── scripts/              # Codigo executavel (acoes no jogo)
      │   └── formation.java    # Logica de formacao defensiva
      ├── references/           # Documentacao de referencia
      │   └── zombie-types.md   # Tipos de zumbi e fraquezas
      └── assets/               # Recursos adicionais
          └── retreat-points.json  # Pontos de recuo pre-definidos
  ```
- **Metadata da skill (SKILL.md):**
  ```yaml
  ---
  name: combat-zombie-horde
  description: Taticas de combate contra hordas de zumbis
  triggers:
    - hostile_count > 5
    - event: blood_moon
  priority: high
  phase: [early, mid, late]
  ---
  
  # Combate Contra Horda de Zumbis
  
  ## Quando Usar
  - Quando ha mais de 5 zumbis hostis no raio de deteccao
  - Durante blood moons
  - Quando o jogador esta em area aberta sem cobertura
  
  ## Instrucoes
  1. Avaliar numero e tipo de zumbis
  2. Verificar armamento disponivel (Alice e jogador)
  3. Se ameaca > 70%: recomendar fuga e guiar jogador
  4. Se ameaca < 70%: assumir posicao de combate
  5. Priorizar zumbis mutantes/especiais
  6. Manter distancia se usando armas de fogo
  ```
- **Tipos de skills:**
  - **Combate:** Taticas contra diferentes inimigos
  - **Construção:** Estruturas defensivas, contraptions Create
  - **Crafting:** Receitas otimizadas, ordens de crafting eficientes
  - **Navegação:** Rotas seguras, pontos de interesse, exploração
  - **Social:** Padrões de conversa, respostas a perguntas frequentes
  - **Sobrevivência:** Farming, comida, cura, infeccao
- **Skill Library (inspirado no Voyager):**
  - Skills são armazenadas no repositorio Git (skills/)
  - Indexadas por embedding da descricao
  - Quando Alice enfrenta situação nova, busca top-5 skills mais relevantes
  - Skills complexas compostas de skills simples (composição)
  - Novas skills podem ser criadas automaticamente quando Alice resolve um problema novo
  - **Self-verification:** Após executar skill, Alice verifica se o resultado foi alcancado
    - Se sim: skill confirmada e mantida
    - Se não: skill revisada ou descartada

#### RF14.2 - Agentes (Raciocinio Especializado)

- **Definição:** Agentes são especializacoes do LLM que atuam em dominios diferentes
- **Arquitetura multi-agente (inspirada em Mindcraft):**
  ```
  +--------------------------------------------------+
  |              AGENTE ORQUESTRADOR                   |
  |  (Recebe input, decide qual agente usar)           |
  +-----+--------+--------+--------+--------+---------+
        |        |        |        |        |
        v        v        v        v        v
  +---------+ +------+ +------+ +------+ +---------+
  | Agente  | |Agente| |Agente| |Agente| | Agente  |
  | Combate | |Build | |Chat  | |Quest | | Survival|
  +---------+ +------+ +------+ +------+ +---------+
  ```
- **Agentes especializados:**
  - **Agente de Combate:** Avaliação de ameaça, taticas, escolha de arma, posicionamento
  - **Agente de Construção:** Seleção de schematic, posicionamento, materiais, contraptions
  - **Agente de Chat:** Conversa natural, humor, lembranças, personalidade
  - **Agente de Quest:** Progresso, sugestões, próximos passos, prioridades
  - **Agente de Sobrevivência:** Comida, cura, infeccao, recursos, crafting
  - **Agente de Navegação:** Rotas, exploração, mapeamento, pontos de interesse
- **Orquestrador:**
  - Recebe input (texto, voz, evento do mundo)
  - Classifica o tipo de situação (combate? conversa? quest? construção?)
  - Roteia para agente especializado
  - Agente especializado carrega skills relevantes
  - Resposta e executada pela camada de ação
- **Regras:**
  - Regras são comportamentos ALWAYS-ON que não dependem do LLM
  - Executam a cada tick ou a cada evento
  - Exemplos: fugir se vida < 20%, comer se fome < 6, equipar melhor arma automaticamente
  - Regras tem prioridade sobre decisões do LLM (segurança primeiro)

#### RF14.3 - Otimização de Latência e Qualidade

- **Objetivo:** Tudo disponível para IA deve ser usado para melhorar resposta e diminuir latência (decisão #29)
- **Estrategias:**
  - **Cache de respostas:** Perguntas frequentes respondidas instantaneamente sem LLM
  - **Pré-computação:** Skills relevantes pré-carregadas baseado em contexto atual
  - **Routing inteligente:** Perguntas simples vão para modelo menor/mais rápido
  - **Streaming:** LLM gera resposta em streaming, TTS comeca a falar antes de terminar
  - **Batch de percepção:** Mundo escaneado a cada N ticks, não a cada tick
  - **Skills compiladas:** Skills usadas frequentemente são "compiladas" em scripts puros (sem LLM)
  - **Embeddings locais:** Busca de skills/memórias via embeddings locais (rápido)
  - **Regras > LLM:** Comportamentos previsíveis sempre via regras (latência zero)
- **Referência:** Mindcraft (multi-agent), Voyager (skill library + self-verification), Agent Skills paper (arxiv 2602.12430)

---

## 5. Requisitos Não-Funcionais

### RNF01 - Performance

- NÃO causar lag perceptível no servidor
- Chamadas LLM 100% assíncronas (nunca bloquear game tick de 50ms)
- Comportamentos reativos (combate, fuga) via scripts, não IA
- Latência para decisões LLM: < 3 segundos
- TTS/STT em threads separadas
- Scanning de mundo limitado (não escanear a cada tick)

### RNF02 - Compatibilidade

- Forge 1.20.1
- Compatível com todos os 201 mods do Cursed Walking
- JEI, FTB Quests, Create como soft dependencies
- Simple Voice Chat como dependência obrigatória (para voz)
- Java 17 (padrão do Forge 1.20.1)

### RNF03 - Configurabilidade

- Backend de IA configurável (Ollama local / API remota)
- URL e modelo do Ollama configuraveis
- API key para serviços remotos configurável
- Modo de voz configurável: `online` (padrão), `offline` (Piper + Vosk local) ou `text_only` (sem voz) — decisão #49
- Distância de percepção configurável

### RNF04 - Escalabilidade Futura

- Arquitetura preparada para multiplas Alices no futuro
- Preparada para adicionar idiomas (troca de voz no Edge TTS + modelo Whisper multilingue)
- Preparada para troca de voz
- MVP: 1 Alice por servidor, pt_BR, 1 voz

---

## 6. Stack Técnica Proposta

### 6.1 Dependências do Mod

| Biblioteca | Uso | Maven/Gradle | Obrigatória? |
|-----------|-----|--------------|-------------|
| Forge MDK 1.20.1 | Base do mod | ForgeGradle | Sim |
| Simple Voice Chat API | Voz (captura/transmissão de audio) | ModRepo Maven | Sim (para voz) |
| ollama4j | Comunicação com Ollama | Maven Central `io.github.ollama4j:ollama4j:1.1.6` | Sim |
| LangChain4j (alternativa) | Framework LLM + RAG | Maven Central `dev.langchain4j:langchain4j-ollama` | Alternativa ao ollama4j |
| JEI API | Receitas ricas | CurseForge Maven | Soft dependency |
| FTB Quests API | Quests awareness | CurseForge Maven | Soft dependency |
| Baritone API | Pathfinding | Baritone Maven | Sim (decisão #22) — funciona nativo com FakePlayer |
| JGit | Operações Git (memória) | Maven Central `org.eclipse.jgit:org.eclipse.jgit` | Sim (para memória Git) |

### 6.2 Serviços Externos (rodam separadamente)

| Serviço | Uso | Onde roda | Requisito |
|---------|-----|-----------|-----------|
| Ollama | LLM local | Machine 3 (192.168.0.200) | CPU disponível, modelos instalados |
| faster-whisper | STT REST local | Machine 2 (192.168.0.225) | Docker stack alice-whisper (Portainer) |
| Edge TTS | TTS neural feminino | Cloud Microsoft | Internet estável no servidor |
| API Remota (fallback LLM) | LLM na nuvem se local lento | Cloud | Internet + API key |
| GitHub | memória Git da Alice | Cloud GitHub | Conta GitHub + repo público + SSH key |

### 6.3 Assets a incluir no mod

| Asset | Formato | Descricao |
|-------|---------|-----------|
| Skin da Alice (Scarlet) | PNG 64x64 | Skin feminina ruiva — funciona out-of-the-box no FakePlayer |
| Schematics | .nbt | Estruturas pré-definidas para construção |
| System prompt | .txt | Personalidade e instrucoes da Alice |

---

## 7. Arquitetura Proposta (Revisada v3)

```
+------------------------------------------------------------------------------+
|                      MINECRAFT SERVER 1.20.1 (Forge)                          |
|                                                                               |
|  +---------+                +---------------------------------------------+  |
|  | Jogador |<-- chat/voz -->|                MOD ALICE                     |  |
|  | Real    |                |                                              |  |
|  +---------+                |  +---------------------------------------+   |  |
|       |                     |  |         CAMADA DE PERCEPCAO           |   |  |
|       | mic                 |  | - AABB scan mobs/players/itens        |   |  |
|       v                     |  | - Forge Events (hurt, chat, join)     |   |  |
|  +---------+                |  | - Inventarios tracking                |   |  |
|  | Simple  |-- audio PCM ->|  | - Container mapping                   |   |  |
|  | Voice   |                |  | - Quest progress                      |   |  |
|  | Chat    |<- audio PCM --|  +-------------------+-------------------+   |  |
|  +---------+                |                     |                       |  |
|       ^                     |  +------------------v-------------------+   |  |
|       |                     |  |          CAMADA DE VOZ               |   |  |
|       | radio               |  | - STT: faster-whisper (audio->texto) |   |  |
|       |                     |  | - TTS: Edge TTS (texto -> audio)     |   |  |
|  +---------+                |  | - EntityAudioChannel (proximidade)   |   |  |
|  | Radio   |                |  | - StaticAudioChannel (radio)         |   |  |
|  | Item    |                |  +-------------------+-------------------+   |  |
|  +---------+                |                     |                       |  |
|                              |  +------------------v-------------------+   |  |
|                              |  |         CAMADA DE REGRAS            |   |  |
|                              |  |         (Always-on, <50ms)          |   |  |
|                              |  | - Fugir se vida < 20%              |   |  |
|                              |  | - Comer se fome < 6                |   |  |
|                              |  | - Auto-equip melhor arma/armadura  |   |  |
|                              |  | - Combate reativo (atacar/esquivar)|   |  |
|                              |  +-------------------+-------------------+  |  |
|                              |                      |                      |  |
|                              |  +-------------------v-------------------+  |  |
|                              |  |     CAMADA DE AGENTES (Orquestrador)  |  |  |
|                              |  |     (LLM - Async, <3s)                |  |  |
|                              |  |                                       |  |  |
|                              |  |  +--------+ +-------+ +--------+     |  |  |
|                              |  |  |Agente  | |Agente | |Agente  |     |  |  |
|                              |  |  |Combate | |Build  | |Chat    |     |  |  |
|                              |  |  +--------+ +-------+ +--------+     |  |  |
|                              |  |  +--------+ +-------+ +--------+     |  |  |
|                              |  |  |Agente  | |Agente | |Agente  |     |  |  |
|                              |  |  |Quest   | |Survive| |Navegar |     |  |  |
|                              |  |  +--------+ +-------+ +--------+     |  |  |
|                              |  |                                       |  |  |
|                              |  | - Roteia input para agente certo     |  |  |
|                              |  | - Agente carrega skills relevantes   |  |  |
|                              |  | - Task decomposition                 |  |  |
|                              |  | - Self-verification                  |  |  |
|                              |  +-------------------+-------------------+  |  |
|                              |                      |                      |  |
|                              |  +-------------------v-------------------+  |  |
|                              |  |       CAMADA DE SKILLS               |  |  |
|                              |  | - Skill library (composable modules)|  |  |
|                              |  | - Indexadas por embedding           |  |  |
|                              |  | - Top-5 skills por contexto (RAG)   |  |  |
|                              |  | - Auto-criacao de novas skills      |  |  |
|                              |  +-------------------+-------------------+  |  |
|                              |                      |                      |  |
|                              |  +-------------------v-------------------+  |  |
|                              |  |       CAMADA DE ACAO                 |  |  |
|                              |  |       (Scripts/Comportamentos)       |  |  |
|                              |  | - Pathfinding (Baritone)            |  |  |
|                              |  | - Crafting (RecipeManager)          |  |  |
|                              |  | - Construcao (schematics)           |  |  |
|                              |  | - Coleta de recursos                |  |  |
|                              |  +-------------------+-------------------+  |  |
|                              |                      |                      |  |
|                              |  +-------------------v-------------------+  |  |
|                              |  |       CAMADA DE MEMORIA              |  |  |
|                              |  | LOCAL: Forge SavedData (NBT/JSON)   |  |  |
|                              |  | - Mapa de containers e itens        |  |  |
|                              |  | - Combinacoes com jogador           |  |  |
|                              |  | REMOTO: Repositorio Git             |  |  |
|                              |  | - Memorias de aventuras             |  |  |
|                              |  | - Skills aprendidas                 |  |  |
|                              |  | - Base de conhecimento              |  |  |
|                              |  | - Locais e jogadores conhecidos     |  |  |
|                              |  +--------------------------------------+  |  |
|                              +---------------------------------------------+  |
+------------------------------------------------------------------------------+
         |                                    |
         | HTTP async                         | Git (JGit, async)
         v                                    v
+------------------+  +------------------+  +------------------+
| Ollama Local     |  | API Remota       |  | Git Repo         |
| (rede local)     |  | (fallback)       |  | (GitHub publico) |
| llama3 / mistral |  | OpenAI/Anthropic |  | alice-memory     |
+------------------+  +------------------+  +------------------+
```

### Divisao Regras vs Agentes vs Scripts (Revisada v3)

| Responsabilidade | Camada | Latência | Justificativa |
|-----------------|--------|----------|---------------|
| Fugir se vida < 20% | Regra | <1ms | Segurança — prioridade absoluta |
| Comer quando com fome | Regra | <1ms | Reacao automática |
| Equipar melhor item | Regra | <1ms | Comparação de stats |
| Combate reativo (atacar, esquivar) | Regra/Script | <50ms | Resposta imediata |
| Pathfinding / navegação | Script (Baritone) | <50ms | Algoritmos prontos |
| Mineracao / coleta (execução) | Script | <50ms | Ação repetitiva |
| Crafting (executar receita) | Script | <50ms | Lookup em RecipeManager |
| Construção (colocar blocos) | Script | <50ms | Seguir schematic |
| Conversao voz->texto (STT) | Script (faster-whisper REST) | ~300ms | Machine 2 (Docker), rede local |
| Conversao texto->voz (TTS) | Script (Edge TTS HTTP) | ~300ms | Cloud Microsoft |
| Operações Git (memória) | Script (JGit) | ~1s | Thread separada |
| **O QUE minerar/coletar** | **Agente Survive** | <3s | Requer contexto e objetivo |
| **O QUE craftar/construir** | **Agente Build** | <3s | Depende de planejamento |
| **Avaliação de ameaça (lutar vs fugir)** | **Agente Combate** | <3s | Análise de situação complexa |
| **Conversa no chat/voz** | **Agente Chat** | <3s | Linguagem natural |
| **Priorizacao de tarefas** | **Orquestrador** | <3s | Planejamento |
| **Interpretar comandos do jogador** | **Orquestrador** | <3s | NLP (texto ou voz) |
| **Sugestões de quests/próximos passos** | **Agente Quest** | <3s | Raciocinio sobre progresso |
| **Orientacao sobre mecânicas/mods** | **Agente Chat** | <3s | Base de conhecimento + RAG |
| **Criar nova skill** | **Orquestrador** | <5s | Auto-aprendizado |
| **Lembranças/memória de aventuras** | **Agente Chat** | <3s | Narrativa + Git repo |

---

## 8. Pipeline de Comunicação por Voz (Detalhado)

### 8.1 Jogador -> Alice (STT Pipeline) — decisão #48

```
Jogador fala no microfone
        |
        v
Simple Voice Chat captura audio (opus encoded)
  - Alice e FakePlayer: SVC trata como player real, captura nativa
        |
        v
Mod Alice intercepta via SVC API (OnSoundPacketEvent ou similar)
        |
        v
Decode opus -> PCM 16kHz mono
        |
        v
POST http://192.168.0.225:10300/v1/audio/transcriptions
  - faster-whisper rodando em Machine 2 via Docker/Portainer
  - Modelo: whisper-medium ou whisper-large-v3
  - language=pt (forcado para pt_BR)
  - Thread separada (nao bloqueia game tick)
        |
        v
Texto reconhecido (JSON OpenAI-compatible: {"text": "..."})
        |
        v
Envia para camada de decisao (LLM)
```

### 8.2 Alice -> Jogador (TTS Pipeline)

```
LLM gera resposta texto
        |
        v
Edge TTS HTTP (cloud Microsoft)
  - Voz: pt-BR-FranciscaNeural (feminina, neural)
  - POST SSML -> resposta mp3
  - Thread separada (async)
  - Gratuito, sem API key
        |
        v
Decode mp3 -> PCM 48kHz
(Simple Voice Chat requer 48kHz — ja no formato certo)
        |
        v
Encode PCM -> opus
        |
        v
Transmitir via Simple Voice Chat API (FakePlayer = player real no SVC):
  - Proximidade (~32 blocos range padrao SVC)
  - OU grupo/canal SVC para distancia ilimitada
        |
        v
Jogador ouve a Alice
```

### 8.3 Radio de Comunicação

```
CENARIO: Jogador longe da Alice (fora do range de voz)

Jogador segura/tem radio no hotbar
        |
        v
Jogador fala no microfone
        |
        v
Simple Voice Chat captura audio
        |
        v
Mod Alice detecta que jogador esta fora de range
mas tem radio no inventario
        |
        v
Audio e direcionado para Alice via StaticAudioChannel reverso
        |
        v
[STT Pipeline normal]
        |
        v
[LLM processa]
        |
        v
[TTS Pipeline normal]
        |
        v
Alice responde via StaticAudioChannel
(audio vai direto pro jogador, qualquer distancia)
```

---

## 9. Questoes em Aberto (Para Brainstorm)

### RESOLVIDAS

- ~~Q01 - Versão do Minecraft~~ -> 1.20.1 Forge (decisão #1)
- ~~Q02 - Backend de IA~~ -> Ollama local + API remota (decisão #5)
- ~~Q03 - Tipo de entidade~~ -> **FakePlayer (ServerPlayer)** — decisão #21 REVISADA, #46 confirmado
- ~~Q04 - Pathfinding~~ -> Baritone — funciona nativamente com FakePlayer, zero adapter (decisão #22)
- ~~Q05 - Multiplayer / quantidade~~ -> 1 Alice no MVP (decisão #6)
- ~~Spawn~~ -> Ovo próprio (decisão #7)
- ~~Morte~~ -> Não perde itens, respawn em cama ou ponto do ovo (decisões #8, #9, #10)
- ~~Voz~~ -> **TTS Edge TTS FranciscaNeural + STT faster-whisper** + Simple Voice Chat (decisões #47, #48)
- ~~Idioma~~ -> pt_BR no MVP (decisão #19)
- ~~Q06 - Faseamento~~ -> Projeto inteiro definido e escopado, faseamento aceito (decisão #23)
- ~~Q13 - Simple Voice Chat~~ -> Sim, Cursed Walking ja inclui SVC (decisão #30)
- ~~Q20 - Provedor Git~~ -> GitHub público (decisão #31). Pendencia: criar repo quando iniciar projeto
- ~~Q22 - Modelo LLM~~ -> Hardware limitado, 1 modelo único é lento (decisão #32)
- ~~Q08 - LLM library~~ -> ollama4j no MVP, interface abstrata para trocar (decisão #33)
- ~~Q24 - Skin~~ -> "Scarlet (Remake)" do Planet Minecraft (decisão #34)
- ~~Q11 - Radio~~ -> Usar Simple Voice Chat diretamente, sem item radio próprio (decisão #35)
- ~~Q14 - Alice offline~~ -> Fica no mundo como player offline, inativa (decisão #36)
- ~~Q10 - Schematics~~ -> Sem padrão, .nbt, só pré-carregados, LLM sugere jogador decide (decisão #37)
- ~~Q15 - Sistema de ameaça~~ -> Fatores toggleaveis com peso, limiar % configurável, PCE definido (decisões #38-#39)
- ~~Q16 - Animações~~ -> Padrão do player + waypoints Map Atlases (decisão #40)
- ~~Q17 - Redstone/Create~~ -> Desejavel, depende da decisão final sobre entidade (decisão #41)
- ~~Q18 - Multi-jogador~~ -> Amigo (ovo) > Conhecido (conversa) > Hostil (se atacar) (decisão #42)
- ~~Q19 - Tamanho mod~~ -> Sem restrição (decisão #43)
- ~~Q21 - Base de conhecimento~~ -> Opção C hibrida, certeza por experiência/confirmacao (decisão #44)
- ~~Q11 - Radio~~ -> Sem radio próprio, usar Simple Voice Chat diretamente (decisão #35)
- ~~Q14 - Alice offline~~ -> Fica no mundo como player offline, inativa (decisão #36)
- ~~Q12 - Voz feminina pt_BR~~ -> Edge TTS pt-BR-FranciscaNeural (gratuito, ilimitado, cloud Microsoft) (decisão #47)
- ~~STT upgrade~~ -> faster-whisper Machine 2 Docker, REST OpenAI-compatible, confirmado rodando (decisão #48)
- ~~Q23 - Baritone + entidade~~ -> Resolvido com FakePlayer: Baritone funciona nativo, zero adapter (decisão #46)
- ~~Q17 - Redstone/Create~~ -> CONFIRMADO: FakePlayer interage com tudo nativamente (decisão #41 atualizado)

### DETALHAMENTO DAS DECISÕES RESOLVIDAS

#### Q03 - FakePlayer (decisão #21 REVISADA → decisão #46)

- **Decisão REVISADA:** FakePlayer (ServerPlayer) via `net.minecraftforge.common.útil.FakePlayer`
- **Motivo da revisao:** Baritone requer player. FakePlayer herda inventário, morte, skin, Create, SVC. Menor complexidade total.
- **O que mudou:** Não e mais Custom Mob. Alice é um ServerPlayer ficticio.
- **Aparência:** Skin Scarlet (decisão #34) funciona out-of-the-box no FakePlayer (skin padrão de player)
- **Aparece na TAB:** Sim, como jogador normal
- **Referência:** Duzo's Alice, Automatone, ChatClef

#### Q04 - Baritone (decisão #22) — DESAFIO ELIMINADO com FakePlayer

- **Decisão:** Usar Baritone como dependência de pathfinding
- **Justificativa:** Poderoso, maduro (8.8k stars), A* rápido, mine/follow/build embutidos, Forge 1.20.1
- **Status com FakePlayer:** Baritone controla ServerPlayer nativamente. ZERO adapter necessário. O desafio técnico original (Q23) foi eliminado pela mudança para FakePlayer.
- **Integração:** Baritone recebe referência para o FakePlayer é o controla exatamente como controlaria um jogador real.

#### Q06 - Faseamento Completo (decisão #23)

- **Decisão:** Projeto inteiro definido e escopado até o fim, implementado em fases
- **Faseamento aprovado (atualizado com FakePlayer + Edge TTS):**
  - **Fase 1 - Fundacao:** FakePlayer ruiva + Baritone (follow/stay) + combate básico + chat texto com LLM
  - **Fase 2 - Utilidade:** Crafting + inventário + containers + auto-equip
  - **Fase 3 - Voz:** TTS (Edge TTS FranciscaNeural) + STT (faster-whisper) + Simple Voice Chat
  - **Fase 4 - Construção:** Schematics + construção bloco a bloco + contraptions Create (FakePlayer interage nativo)
  - **Fase 5 - Guia:** Base de conhecimento compilada + orientacao por fase de jogo + quests
  - **Fase 6 - Inteligência:** Agentes + skills + memória Git + autonomia avancada + aprendizado
- **Nota:** Todas as fases devem estar BEM DEFINIDAS e ESCOPADAS antes de comecar a implementação.

### PENDENTES - Arquitetura e Implementação

#### Q07 - Persistência Técnica da memória (parcialmente resolvido)
- **Decisão parcial:** Dual storage — Forge SavedData (local, rápido) + Git repo (portável, persistente)
- **SavedData** para: mapa de containers, estado atual, dados de tick (precisa ser rápido)
- **Git repo** para: memórias de aventuras, skills, base de conhecimento, preferencias
- **Perguntas restantes:**
  - Quanto de memória de aventuras manter no contexto do LLM? (ultimas 50? 100?)
  - Como resumir memórias antigas para não estourar contexto do LLM?
  - Usar RAG para buscar memórias relevantes? (provavel que sim — alinhado com skills/agentes)
  - Qual provedor Git usar? (GitHub? GitLab? Gitea self-hosted?)
  - Frequência de commits: a cada evento significativo? A cada N minutos?
  - **Pergunta:** Prefere GitHub público ou quer hospedar um Gitea no mesmo servidor Docker?

#### Q08 - ollama4j vs LangChain4j vs HTTP direto (análise detalhada)

##### Opção A: ollama4j (v1.1.4+)

- **Tamanho:** Leve (~500KB + dependências minimas)
- **Escopo:** Wrapper específico para Ollama REST API
- **Funcionalidades:**
  - Chat/generate com streaming
  - Modelos: list, pull, delete, copy
  - Vision/image support
  - **Tool/function calling** (suporta MCP tools!) — desde v1.1.x
  - Embeddings generation (para busca semantica de skills)
  - Metricas Prometheus (beta)
- **Código exemplo:**
  ```java
  OllamaAPI api = new OllamaAPI("http://localhost:11434");
  OllamaChatResult result = api.chat(
      OllamaChatRequestBuilder.getInstance("llama3")
          .withMessage(OllamaChatMessageRole.USER, "Quantos zumbis vc ve?")
          .build()
  );
  ```
- **Pros:**
  - Simples, direto, pouca curva de aprendizado
  - Leve — não infla o mod
  - Tool calling JA FUNCIONA (não precisa implementar na mao)
  - Embeddings JA FUNCIONA (busca semantica de skills)
  - Maven: `io.github.ollama4j:ollama4j:1.1.6`
- **Contras:**
  - So funciona com Ollama (sem fallback direto para OpenAI/Anthropic)
  - RAG precisa ser implementado manualmente
  - Memory management precisa ser implementado manualmente
  - Multi-provider requer código custom para fallback API remota

##### Opção B: LangChain4j

- **Tamanho:** Pesado (~15-20MB com dependências transitivas)
- **Escopo:** Framework completo de LLM — equivalente Java do LangChain Python
- **Funcionalidades nativas:**
  - **RAG completo** (document loader, splitter, embedding store, retriever)
  - **Memory** (chat memory, message window, token window, persistent)
  - **Tool calling** nativo com @Tool annotation
  - **Multi-provider** (Ollama, OpenAI, Anthropic, Gemini, Mistral, etc.)
  - **MCP support** (Model Context Protocol)
  - **Agents** (sequencial, iterativo)
  - **Streaming** support
  - **AiServices** interface — define interface Java, LLM implementa
- **Código exemplo:**
  ```java
  // Definir interface
  interface AliceAssistant {
      @SystemMessage("Voce e Alice, uma engenheira de sobrevivencia...")
      String chat(@MemoryId String sessionId, @UserMessage String message);
  }
  
  // Criar com RAG + memory
  AliceAssistant alice = AiServices.builder(AliceAssistant.class)
      .chatLanguageModel(OllamaChatModel.builder()
          .baseUrl("http://localhost:11434")
          .modelName("llama3")
          .build())
      .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
      .contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore))
      .tools(new CraftingTool(), new NavigationTool(), new CombatTool())
      .build();
  
  String response = alice.chat("session1", "O que devo fazer?");
  ```
- **Pros:**
  - RAG pronto (não precisa implementar)
  - Memory management pronto
  - Fallback para API remota trivial (troca provider)
  - Tool calling com @Tool annotation (elegante)
  - MCP support
  - Comunidade grande (17k+ stars)
  - Documentacao excelente
- **Contras:**
  - **PESADO** — 15-20MB de dependências
  - Complexidade — muita coisa que Alice não precisa
  - Possível conflito com classloading do Forge (dependências transitivas)
  - Over-engineering para MVP?

##### Opção C: HTTP direto (OkHttp/Java HttpClient)

- **Tamanho:** Zero dependência adicional (Java HttpClient nativo)
- **Funcionalidades:** Só o que você implementar
- **Pros:** Controle total, zero overhead, zero conflito
- **Contras:** Reimplementar TUDO (chat, streaming, tool calling, RAG, memory)

##### Análise Considerando Decisões Tomadas

| Funcionalidade necessária | ollama4j | LangChain4j | HTTP direto |
|--------------------------|----------|-------------|-------------|
| Chat básico | Sim | Sim | Implementar |
| Streaming | Sim | Sim | Implementar |
| Tool calling (agentes) | Sim (v1.1+) | Sim (nativo) | Implementar |
| Embeddings (skills RAG) | Sim | Sim (+ vector store) | Implementar |
| Chat memory | Manual | Nativo | Implementar |
| RAG (base conhecimento) | Manual | Nativo | Implementar |
| Multi-provider (fallback) | Manual | Nativo | Implementar |
| Peso no mod | ~500KB | ~15-20MB | 0 |
| Risco de conflito Forge | Baixo | Medio-Alto | Zero |

##### Contexto crítico: Hardware limitado (decisão #32)

- O usuario confirmou que so roda 1 modelo LLM e ele e LENTO
- Isso muda a equacao:
  - Multi-provider do LangChain4j perde valor (não vai rodar 2 modelos)
  - RAG via embeddings pode ser pesado demais (requer modelo de embedding rodando)
  - **Alternativa para RAG:** Em vez de embeddings via LLM, usar busca por keywords/tags (mais rápido, sem modelo extra)
  - Cache e pré-computação são MAIS importantes que features de framework

##### Recomendação Técnica

**Para o MVP: ollama4j** — mais leve, tool calling ja funciona, embeddings disponíveis se quiser, não infla o mod, baixo risco de conflito com Forge.

**Para Fase 6 (Inteligência): Reavaliar** — se precisar de RAG sofisticado e multi-provider, migrar para LangChain4j. A interface pode ser abstraida agora para facilitar troca depois.

**Proposta de implementação:**
```java
// Interface abstrata — permite trocar ollama4j por LangChain4j depois
public interface AliceLLMProvider {
    String chat(String systemPrompt, List<Message> history, String userMessage);
    String chatWithTools(String systemPrompt, List<Message> history, 
                         String userMessage, List<Tool> tools);
    float[] generateEmbedding(String text);  // para busca de skills
    CompletableFuture<String> chatAsync(...);  // async para nao bloquear tick
}

// Implementacao ollama4j (MVP)
public class OllamaProvider implements AliceLLMProvider { ... }

// Futura implementacao LangChain4j (Fase 6)
public class LangChain4jProvider implements AliceLLMProvider { ... }
```

- **Pergunta:** Essa abordagem (ollama4j agora, interface abstrata, LangChain4j depois se precisar) faz sentido?

### PENDENTES - Conteúdo e Game Design

#### Q09 - Compatibilidade com Mods do Cursed Walking
- Alice precisa ter scripts específicos para armas de fogo do modpack?
- Precisa entender mecânicas específicas (infeccao zumbi, blood moon)?
- Isso vem via scripts específicos por mod ou o LLM interpreta genericamente?
- **Ação:** Extrair lista completa dos 201 mods via acesso SSH ao servidor (pasta mods/)
- **Status:** Pendente — aguardando acesso SSH do usuario

#### Q10 - Schematics Pré-carregados
- Quais estruturas incluir no mod? (abrigo simples? torre de vigia? base completa?)
- Formato confirmado: .nbt (compatível com Create e Minecraft nativo)
- Alice pode criar schematics novos ou só usa os pré-carregados?
- Como Alice escolhe qual schematic construir? (LLM decide? jogador pede?)
- **Pergunta:** Quais estruturas são prioritarias para o contexto de apocalipse zumbi?

#### Q11 - RESOLVIDO: Radio substituido pelo Simple Voice Chat (decisão #35)

- **Decisão:** NÃO ter item radio próprio — usar Simple Voice Chat diretamente
- **Motivo:** SVC ja está no modpack Cursed Walking com todas as funcionalidades necessárias
- **O que isso muda:**
  - Proximidade de voz: nativo do SVC (configura range)
  - Comunicação a distância: jogador usa o sistema de grupos/canais do SVC
  - O Walkie-Talkie mod (também no modpack) ja implementa a mecânica de radio com item
  - Alice usa a API do SVC para falar/ouvir — não precisa de item próprio
- **Conclusão:** Zero esforço de crafting de radio. Alice "fala" via SVC API como entidade de voz, ou como player (se FakePlayer). Walkie-Talkie mod pode ser referência direta de como fazer radio com item se necessário no futuro.

#### Q12 - RESOLVIDO: Edge TTS pt-BR-FranciscaNeural (decisão #47) + faster-whisper (decisão #48)

- **TTS:** Microsoft Edge TTS — voz `pt-BR-FranciscaNeural` (feminina, neural, excelente qualidade)
  - Gratuito, sem API key, sem cadastro, sem limite documentado
  - Mesmo motor do Windows Narrator e Edge browser
  - Requer internet no servidor (disponível é estável, confirmado)
  - Chamada HTTP simples com SSML -> resposta mp3

```java
String ssml = "<speak version='1.0' xml:lang='pt-BR'>"
            + "<voice name='pt-BR-FranciscaNeural'>" + texto + "</voice></speak>";
// POST endpoint Edge TTS -> mp3 -> PCM 48kHz -> SVC
```

- **STT:** faster-whisper rodando em Machine 2 (192.168.0.225) via Docker/Portainer
  - REST OpenAI-compatible: `POST /v1/audio/transcriptions`
  - Gratuito, ilimitado, estado da arte em reconhecimento de fala
  - pt_BR excelente (Whisper treinado em 680k horas de audio multilingue)

#### Q13 - RESOLVIDO: Simple Voice Chat Confirmado (decisão #30)

- **Resposta:** SIM, Cursed Walking ja inclui Simple Voice Chat
- Confirmado via extração da lista de mods do CurseForge (2026-04-03)
- Também inclui Walkie-Talkie mod (referência direta para radio da Alice)
- Também inclui Sound Physics Remastered (pode afetar propagação de audio)
- **Conclusão:** Não precisa pedir instalacao adicional. SVC ja está no modpack.

#### Q14 - RESOLVIDO: Alice offline = player offline (decisão #36)

- **Decisão:** Quando o jogador desconecta, Alice fica no mundo como um player offline
- **Comportamento:**
  - Permanece fisicamente no mundo (não desaparece)
  - Fica inativa — sem IA, sem decisões, sem movimento
  - Contínua vulneravel a mobs (igual a um player offline que fica no servidor)
  - Se morrer enquanto offline, respawna normalmente quando jogador voltar
- **Justificativa:** É o comportamento mais natural e consistente com a lore — Alice existe no mundo, não some quando o jogador fecha o jogo
- **Nota técnica:** Com FakePlayer (decisão #46), este comportamento é nativo — um FakePlayer simplesmente para de receber comandos quando o servidor para de enviá-los, permanecendo no mundo como qualquer player offline

#### Q15 - RESOLVIDO: Sistema de Ameaça + Padrão de Comunicação (decisões #38-#39)

**a) Fatores do sistema de ameaça (decisão #38)**

Cada fator tem peso configurável (0.0 a 1.0). Pode ser ligado/desligado. Limiar de fuga em % configurável.

| Fator | Exemplo de config | Toggleavel |
|-------|------------------|------------|
| Quantidade de mobs hostis próximos | count * 0.1 por mob | Sim |
| Tipo de mob (mutante > normal) | mutante=0.3, normal=0.1 | Sim |
| Vida atual da Alice | < 30% vida = +0.3 | Sim |
| Vida atual do jogador | < 30% vida = +0.2 | Sim |
| Armamento disponível (Alice) | sem arma = +0.2 | Sim |
| Armamento disponível (jogador) | sem arma = +0.2 | Sim |
| Hora do dia (noite = mais perigoso) | noite = +0.15 | Sim |
| Blood Moon ativo | blood_moon = +0.4 | Sim |
| Distância da base segura | longe = +0.1 | Sim |

Limiar de fuga: configurável em % (ex: fuga se ameaça >= 70%)

**b) Padrão de Comunicação Eficiente — PCE (decisão #39)**

Este é um padrão de comportamento para TODAS as situações de perigo ou desafio, não so ameaças.

```
PROTOCOLO PCE - Comunicação em Situação de Perigo

1. Alice AVISA + PEDE CONFIRMAÇÃO
   "Perigo! [descrição breve]. Fugir agora? [sim/não]"

2. Aguarda 3-5 segundos sem resposta

3. REPETE O AVISO + PEDE CONFIRMAÇÃO novamente
   "Perigo ainda presente! Fugir agora? [sim/não]"

4. Continua avisando a cada 3-5 segundos enquanto perigo persistir

CONFIRMAÇÕES ACEITAS:
- Jogador diz "sim" / "pode" / "vamos" / qualquer confirmação
- Jogador REPETE O QUE ALICE FALOU (ex: "fugir agora") = confirmação
- Jogador diz "repete" / "de novo" -> Alice repete e pede confirmação novamente

CANCELAMENTOS:
- Jogador diz "não" / "fica" / "aguenta"
- Alice para de avisar até a situação mudar

PRINCÍPIOS DO PCE:
- Mensagens CURTAS e DIRETAS (máx. 1 frase de aviso + pergunta)
- Informação crítica PRIMEIRO, contexto DEPOIS
- Confirmação simples, sem verbosidade
- Não bloquear o jogador esperando resposta — ele pode estar lutando
- Usar voz (TTS) E chat simultaneamente em situações críticas

APLICAR PCE TAMBÉM EM:
- Sugestões urgentes de crafting antes de ameaça
- Waypoints e alertas de localização ("base a X blocos, ir agora?")
- Alertas de recurso crítico ("sem munição, reabastecer agora?")
- Qualquer decisão que precisa de resposta rápida
```

#### Q16 - RESOLVIDO: Animações padrão + Waypoints (decisão #40)

- Animações: padrão de player (sem animações especiais no MVP)
- Para apontar/mostrar coisas: Alice cria waypoints via **Map Atlases mod** com nome específico
- Comunicação de waypoint segue o PCE: "Marquei [Nome] no mapa. Ver agora?"
- Ex: "Marquei 'Perigo-Horda-Norte' no mapa. Fuja para sul!"

#### Q17 - RESOLVIDO: Redstone/Create confirmado com FakePlayer (decisão #41)

- **Decisão do usuário:** É desejável que Alice possa interagir com redstone e Create
- **Foi uma das razões para revisar a decisão #21** de Custom Mob para FakePlayer:
  - **Custom Mob (descartado):** Mobs não pressionam botões, alavancas, pedais Create nativamente — precisaria implementar tudo do zero com código específico por bloco
  - **FakePlayer (adotado):** Interage com qualquer bloco exatamente como um jogador — redstone, Create contraptions, crafting tables, tudo funciona out-of-the-box
- **Conclusão:** ✅ Resolvido — FakePlayer (decisão #46) entrega interação com redstone e Create gratuitamente
- **MVP:** Redstone e Create ficam para a Fase 4, mas a arquitetura já suporta desde a Fase 1

#### Q18 - RESOLVIDO: Hierarquia social Amigo > Conhecido > Hostil (decisão #42)

- **Decisão:** Sistema de 3 níveis de relação social

| Nível | Quem e | Alice se comporta como |
|-------|--------|----------------------|
| **Amigo** | Dono do ovo (quem spawnou Alice) | Nunca hostil, aceita todas as ordens, máxima confiança |
| **Conhecido** | Qualquer outro jogador no servidor | So conversa, NÃO executa ordens, pode ajudar em combate |
| **Hostil** | Quem atacar Alice | Alice ataca de volta até o agressor parar |

- **Regras de transicao:**
  - Jogador novo encontra Alice = automaticamente "Conhecido"
  - Jogador ataca Alice = vira "Hostil" até parar de atacar
  - Jogador Hostil que para de atacar = volta a "Conhecido" após cooldown
  - Só o Amigo (dono) pode dar comandos: "vamos combinar assim...", "fica aqui", "me segue"
- **MVP:** 1 Amigo por Alice (quem colocou o ovo). Em futuras versões: lista de Amigos configurável

#### Q19 - RESOLVIDO: Tamanho do Mod (decisão #43)

- **Decisão:** Sem restrição de tamanho para o mod
- **Tamanho estimado atual (com decisões #47 e #48):**
  - Sem modelos de voz locais — Edge TTS e faster-whisper rodam fora do mod
  - Apenas skin PNG + schematics .nbt + system prompt .txt
  - Tamanho estimado: **< 5MB** (reduzido drasticamente vs estimativa original de 100-150MB)
- **Beneficio das decisões de voz cloud/REST:** Zero modelos bundled no .jar

### NOVAS QUESTOES (adicionadas v0.4)

#### Q20 - RESOLVIDO: GitHub Público (decisão #31)

- **Decisão:** GitHub público
- **Pendencia de projeto:** Criar o repositorio GitHub quando iniciar a implementação
- Vantagens: gratuito, API madura, GitHub Actions para automacao de merges, comunidade pode contribuir
- Nome sugerido do repo: `alice-memory`

#### Q21 - Base de Conhecimento: O que é, por que e como

**O que é "base de conhecimento"?**

Quando você conversa com um LLM (tipo ChatGPT ou Ollama), ele so sabe o que foi treinado. Ele NÃO sabe que no Cursed Walking tem infeccao zumbi, que a Timeless and Classics tem uma M4A1, ou que a melhor defesa contra horda é uma parede de 3 blocos com seteiras.

Para Alice ser útil, ela precisa SABER essas coisas. A "base de conhecimento" é um conjunto de textos que dizemos para o LLM ler ANTES de responder. E como dar um livro para alguem ler antes de uma prova.

**Na prática, funciona assim:**

1. Jogador pergunta: "Alice, como eu faco pra não pegar infeccao de zumbi?"
2. O mod Alice busca na base de conhecimento: "infeccao zumbi" -> encontra o arquivo `zombie-infection.md`
3. Esse arquivo e enviado junto com a pergunta pro LLM: "Aqui está informação sobre infeccao zumbi: [conteúdo do arquivo]. Agora responda a pergunta do jogador."
4. LLM responde com informação CORRETA porque leu o texto certo.

Sem a base de conhecimento, o LLM ia inventar qualquer coisa (alucinacao).

**Quanto conteúdo precisa?**

NÃO precisamos documentar os 201 mods. Precisamos documentar o que Alice precisa FALAR pro jogador. Baseado no catálogo, são ~25 mods críticos. Para cada um, precisamos de algo tipo:

```markdown
# Zombie Infection Vaccine
## O que e
O mod adiciona sistema de infeccao por zumbi. Se um zumbi te acerta, voce pode ser infectado.
## Como saber se esta infectado
Icone de infeccao aparece na tela. Barra de infeccao sobe.
## Como curar
- Usar a vacina (craftar com X + Y + Z)
- Usar antidoto do Medical Remedies
## Como prevenir
- Armadura completa reduz chance
- Evitar contato corpo-a-corpo com zumbis
- Usar armas de longo alcance
## Receita da vacina
[descricao da receita]
```

**De onde tirar essa informação:**

| Fonte | O que da pra tirar | Como |
|-------|-------------------|------|
| **CurseForge** (pagina do mod) | Descricao básica, features, changelog | Acessar via curl |
| **Wiki do mod** (se existir) | Receitas, mecânicas detalhadas, estrategias | Links nas paginas dos mods |
| **YouTube** (videos de gameplay/tutorial) | Demonstracoes práticas, dicas | Transcrever via IA |
| **JEI (in-game)** | Receitas de TODOS os mods automaticamente | Alice ja acessa via RecipeManager API — NÃO precisa estar na base |
| **Jogar o jogo** | Experiência prática | Alice aprende jogando (memória Git) |

**Sobre receitas e JEI:**

Receitas Alice NÃO precisa na base de conhecimento! O mod JEI (que está no modpack) é o RecipeManager do Minecraft ja dao acesso programatico a TODAS as receitas de TODOS os mods. Quando o jogador perguntar "como crafta uma M4?", Alice consulta o RecipeManager em tempo real — não precisa ter escrito antes.

O que Alice precisa na base e: **estrategia, contexto e dicas** — coisas que não estão em nenhuma API.

**Opções de como compilar:**

- **Opção A: Eu (IA) compilo para você**
  - Eu acesso as paginas dos ~25 mods críticos no CurseForge
  - Busco wikis, documentacao, videos do YouTube
  - Escrevo os arquivos markdown da base de conhecimento
  - Você revisa e corrige o que estiver errado
  - **Pros:** Rápido, você não precisa escrever nada
  - **Contras:** Posso errar informações que so quem joga sabe

- **Opção B: Você joga e eu documento**
  - Você joga o Cursed Walking normalmente
  - Me conta o que descobriu, o que funciona, o que não funciona
  - Eu vou escrevendo os arquivos de conhecimento baseado no que você relata
  - **Pros:** Informação 100% prática e testada
  - **Contras:** Lento, depende de você jogar bastante

- **Opção C: Hibrido (RECOMENDADO)**
  - Eu compilo a base inicial pesquisando na internet (Opção A)
  - Você revisa e corrige o que está errado
  - Conforme você joga, eu atualizo com informação prática (Opção B)
  - Alice também atualiza conforme joga (memória Git, Fase 6)
  - **Pros:** Rápido para comecar, melhora com o tempo
  - **Contras:** Nenhum relevante

- **Opção D: Alice aprende sozinha (só na Fase 6)**
  - Não compilar nada agora
  - Alice comeca "burra" e vai aprendendo jogando
  - Usa só o que o LLM ja sabe de treino
  - **Pros:** Zero trabalho agora
  - **Contras:** Alice vai dar informação errada no inicio, experiência ruim

**Pergunta:** Opção C faz sentido? Posso comecar a compilar a base dos ~25 mods críticos agora?

#### Q22 - RESOLVIDO: Modelo Único, Hardware Limitado (decisão #32)

- **Resposta do usuario:** So roda 1 modelo e até hoje nunca conseguiu rodar bem — sempre lento
- **Decisão:** Modelo único. Otimizar para funcionar com hardware limitado.
- **Implicações críticas para a arquitetura:**
  - NÃO usar multi-model (Mindcraft usa 4, mas requer GPU potente)
  - Agentes especializados compartilham o MESMO modelo, diferenciados por system prompt
  - Embeddings para RAG podem ser pesados demais — considerar busca por keywords/tags
  - **Pré-cache** e skills compiladas (regras sem LLM) são ESSENCIAIS
  - Minimizar chamadas ao LLM — tudo que pode ser script/regra, DEVE ser
  - Considerar modelo menor e mais rápido (ex: phi-3, gemma-2b) em vez de modelo grande lento
  - Streaming de resposta + TTS incremental para perceber latência menor
  - **Fallback para API remota** ganha importância (quando LLM local é muito lento)
- **Hardware detectado (2026-04-03):**
  - CPU: AMD Ryzen 5 4500 (6 cores / 12 threads)
  - RAM: 16 GB DDR4
  - GPU: NVIDIA GTX 1050 Ti — **4 GB VRAM** (GARGALO)
  - Ollama: v0.19.0
- **Modelos instalados:** mistral:latest (4.4GB local), glm-5/minimax/deepseek-v3 (cloud)
- **Diagnostico:** Mistral 7B (4.4GB) NÃO cabe inteiro nos 4GB VRAM. Roda hibrido GPU+CPU = LENTO.
- **Modelos recomendados para 4GB VRAM:**
  - `llama3.2:3b` (~2.0 GB) — bom equilibrio qualidade/velocidade
  - `qwen2.5:3b` (~2.0 GB) — forte em raciocinio
  - `phi3:mini` (~2.3 GB) — boa qualidade geral
  - `gemma2:2b` (~1.6 GB) — ultra-leve, mais rápido
  - `mistral:7b-q4_0` (~3.8 GB) — cabe justo, mais capaz mas no limite
- **Estrategia recomendada:**
  - Modelo local: `llama3.2:3b` ou `qwen2.5:3b` (cabe na GPU, 1-3s por resposta)
  - Fallback cloud: deepseek-v3/glm-5 via Ollama cloud (para perguntas complexas)
  - Skills compiladas e regras sem LLM para tudo que puder (latência zero)
- **Próxima ação (pré-Fase 1):** Primeiro investigar GPU da Machine 3 via SSH — se tiver GPU, ativar CUDA no Ollama e então testar `llama3.2:3b` / `qwen2.5:3b`. Se não tiver GPU, decidir entre mover Ollama para Machine 1 ou usar API cloud como primary

#### Q23 - ~~Baritone + Custom Mob~~ → RESOLVIDO por FakePlayer (decisão #46)

> **Contexto histórico:** Esta questão levantou o conflito entre Custom Mob e Baritone, que foi o principal motivador para revisar a decisão #21 e adotar FakePlayer. Mantida aqui para documentar o raciocínio.

**O problema que existia:**

Tínhamos duas decisões que conflitavam entre si:
- **Decisão #21 (original):** Alice seria um Custom Mob (PathfinderMob) — uma entidade tipo mob, não um jogador
- **Decisão #22:** Usaremos Baritone para pathfinding — o melhor sistema de navegação

O conflito: **Baritone foi feito para controlar JOGADORES, não mobs.** Quando você instala Baritone no seu Minecraft, ele controla o SEU personagem. Ele sabe ler o inventário do JOGADOR, mover as pernas do JOGADOR, minerar com as maos do JOGADOR.

Alice não é um jogador — ela é um mob customizado. Baritone nunca foi projetado para mover um mob.

**Analogia:** E como tentar usar o volante de um carro (Baritone) para pilotar um drone (Alice). O volante e excelente, mas precisa de um adaptador para funcionar com um veiculo diferente.

**Opções técnicas (do mais simples ao mais complexo):**

- **Opção A: Pathfinding vanilla + Baritone parcial**
  - Usar o pathfinding nativo do Minecraft (que JA funciona com mobs) para movimentacao básica (andar, seguir, fugir)
  - Usar Baritone APENAS para tarefas avancadas (minerar, construir) criando um contexto fake de player só para essas operações
  - **Pros:** Mais simples, menos risco de bugs
  - **Contras:** Dois sistemas de navegação, pode ter inconsistências

- **Opção B: Criar um "adaptador" para Baritone**
  - Escrever um código que "traduz" os comandos do Baritone (feitos para player) em movimentos de mob
  - O Baritone calcula o caminho, nosso adaptador move a Alice por esse caminho
  - **Pros:** Usa toda a inteligência do Baritone (A*, desvio de obstaculos, etc.)
  - **Contras:** Precisa implementar o adaptador, manter quando Baritone atualizar

- **Opção C: Fazer Alice ser um "fake player" internamente**
  - Criar um player invisivel associado a Alice que o Baritone controla
  - A entidade visual (o mob ruivo) segue esse player invisivel
  - **Pros:** Baritone funciona 100% sem modificacao
  - **Contras:** Complexo, pode causar bugs estranhos, consome mais recursos

**Isso nos levou a rever a decisão #21 inteira.** Antes de resolver como adaptar Baritone para mob, vale perguntar: **e se Alice fosse um jogador de verdade?**

---

### Q23b - REVISAO DA DECISÃO #21: Custom Mob vs FakePlayer/ServerPlayer

**O que é um FakePlayer?**

`net.minecraftforge.common.útil.FakePlayer` é uma classe do próprio Forge que cria um ServerPlayer ficticio — um jogador real no servidor, mas sem conexão de cliente real. É o padrão estabelecido no ecossistema Forge para entidades que precisam agir como jogadores.

Usado por mods conhecidos: Industrial Craft 2 (drones), Create (contraptions), BuildCraft (robots), e varios companheiros IA.

**Comparação completa para o Projeto Alice:**

| Criterio | Custom Mob (PathfinderMob) | FakePlayer (ServerPlayer) |
|----------|--------------------------|--------------------------|
| **Baritone (pathfinding)** | NÃO funciona — precisa adapter complexo | FUNCIONA nativo — Baritone foi feito para players |
| **Simple Voice Chat** | Precisa implementação especial como entidade de voz | Funciona como player normal — integração trivial |
| **Create (mecanismos)** | Limitado — mobs não operam blocos Create | Pleno — opera levers, botoes, Create blocks igual jogador |
| **Redstone** | Limitado — mobs não ativam redstone por interação | Pleno — pressiona botoes, alavancas, pedais |
| **Crafting table** | Não usa — RecipeManager apenas programatico | Usa fisicamente igual jogador (também RecipeManager) |
| **Inventário** | Custom implementation necessária | Inventário 36+4+1 slots nativo do Forge |
| **Animações** | Custom AnimationController necessário | Animações padrão de player — gratuitas |
| **Skin Scarlet (decisão #34)** | Custom PlayerModel renderer no mob | Skin padrão de player — funciona out-of-the-box |
| **Morte e respawn** | Custom death/respawn logic necessária | Lógica nativa de player herdada automaticamente |
| **Lootr (loot instanciado)** | Mob pode não receber loot de player | Recebe loot próprio como qualquer jogador real |
| **FTB Teams** | Não pode ser membro de equipe real | Pode ser adicionado como membro normalmente |
| **TAB list (lista de jogadores)** | Não aparece | Aparece como jogador — pode confundir ou ser feature |
| **Journeymap / mapas** | Aparece como entidade | Aparece como jogador no mapa |
| **Anti-cheat mods** | Sem risco (e mob) | Risco BAIXO — FakePlayer tem GameProfile marcado como fake, maioria dos mods respeita |
| **Auth mods** | Sem conflito | Risco BAIXO — FakePlayer não passa por login/auth real |
| **Complexidade de spawn** | Simples (EntityType.spawn) | Media — precisa criar GameProfile, UUID, handle join event |
| **Complexidade total MVP** | MAIOR — precisa implementar tudo do zero | MENOR — herda toda lógica de player do Forge |
| **Estabilidade** | Alta — mob é simples | Alta — FakePlayer é padrão maduro e bem testado |
| **Projetos de referência** | HumanCompanions, SiliconeDolls | Duzo's Alice, Automatone, ChatClef, Minecraft bots (mineflayer-style) |

**Análise aplicada ao Projeto Alice:**

Com as informações que temos hoje, FakePlayer resolve ou simplifica TODOS os maiores desafios técnicos:

- **Baritone (decisão #22):** Zero adapter necessário. Baritone controla FakePlayer nativo.
- **Create/Redstone (Q17):** De graca. FakePlayer interage com tudo.
- **Simple Voice Chat (decisão #30):** De graca. SVC trata FakePlayer como player real.
- **Skin Scarlet (decisão #34):** De graca. Skin de player funciona out-of-the-box.
- **Lootr no modpack:** De graca. FakePlayer recebe loot instanciado.
- **Inventário, morte, respawn:** De graca. Lógica nativa do Forge.

O Custom Mob exigiria implementar manualmente tudo isso do zero. O FakePlayer herda tudo gratuitamente.

**Risco real do FakePlayer:**

O risco de conflito com mods de autenticação e BAIXO porque:
1. O servidor roda em modo offline (TLauncher — sem verificação Mojang)
2. FakePlayer tem flag `isFakePlayer() = true` que mods bem escritos checam
3. FakePlayer é padrão documentado do Forge — mods do Cursed Walking (Forge 1.20.1) são compatíveis

**Recomendação:**

**Trocar a decisão #21 para FakePlayer.** O argumento é simples: FakePlayer resolve o maior problema técnico (Baritone) e entrega gratuitamente tudo que precisaríamos implementar manualmente no Custom Mob. Menor complexidade, mais recursos, mais estabilidade.

**✅ CONFIRMADO — Decisão #46 (2026-04-03):** Mudança da decisão #21 de Custom Mob para FakePlayer confirmada. Ver decisão #46 na tabela de decisões.

#### Q24 - RESOLVIDO: Skin "Scarlet (Remake)" (decisão #34)

- **Skin escolhida:** "Scarlet (Remake)" do Planet Minecraft
- **URL:** https://www.planetminecraft.com/skin/scarlet-remake/
- **Formato:** Skin de player padrão 64x64 PNG
- **Implementação técnica (FakePlayer — decisão #46):** Skin funciona out-of-the-box — FakePlayer é um ServerPlayer real, usa o mesmo sistema de skin de qualquer jogador. Basta definir o `GameProfile` com o UUID correto é a skin é carregada automaticamente.
- **Asset a incluir no mod:** Arquivo PNG da skin (com crédito ao autor original no README)

---

## 10. Documentacao de Referência Complementar

Catálogos detalhados na pasta `referências/`:

- **[catálogo-bibliotecas-e-apis.md](referências/catálogo-bibliotecas-e-apis.md)** - Detalhamento técnico completo de todas as bibliotecas, APIs, dependências com exemplos de código, Maven coordinates, formatos, e recomendações

- **[catálogo-projetos-referência.md](referências/catálogo-projetos-referência.md)** - Catálogo completo de projetos de referência com análise de código, arquitetura, o que estudar em cada projeto, e mapa de referência por feature

- **[catálogo-mods-cursed-walking.md](referências/catálogo-mods-cursed-walking.md)** - Lista completa dos 201 mods do Cursed Walking, categorizados por relevancia para Alice ([CRÍTICO], [IMPORTANTE], [REFERÊNCIA], [COMPATIBILIDADE], [INFRAESTRUTURA])

---

## 11. Próximos Passos

### Concluidos
- [x] Definir versão alvo do Minecraft (1.20.1)
- [x] Decidir Forge vs NeoForge (Forge)
- [x] Pesquisar projetos de referência (TIER 1, 2, 3)
- [x] Catalogar bibliotecas e APIs disponíveis
- [x] Catalogar projetos de referência com código fonte
- [x] Definir identidade e personalidade da Alice
- [x] Definir mecânica de spawn e morte
- [x] Definir sistema de comandos (frase-chave)
- [x] Definir sistema de voz (TTS Edge TTS FranciscaNeural + STT faster-whisper + SVC)
- [x] Definir idioma MVP (pt_BR)
- [x] Pesquisar viabilidade técnica de voz
- [x] Revisar tipo de entidade: **FakePlayer** (Q03, decisão #21 REVISADA → #46)
- [x] Definir pathfinding: Baritone (Q04, decisão #22)
- [x] Definir faseamento completo do projeto (Q06, decisão #23)
- [x] Definir papel da Alice: guia/engenheira de sobrevivência (decisão #24-#27)
- [x] Definir sistema de memória Git (decisão #28)
- [x] Definir arquitetura de agentes e skills (decisão #29)
- [x] Documentar infraestrutura de desenvolvimento (SSH, Docker, Crafty, TLauncher)
- [x] Pesquisar arquiteturas de agent skills (paper arxiv, Spring AI, Voyager)
- [x] Pesquisar memória Git-native para agentes IA
- [x] Extrair lista completa de mods do Cursed Walking (201 mods, via curl no CurseForge)
- [x] Categorizar mods por relevancia para Alice (CRÍTICO/IMPORTANTE/REFERÊNCIA/etc.)
- [x] Confirmar Simple Voice Chat no modpack (Q13, decisão #30)
- [x] Confirmar GitHub como provedor de memória (Q20, decisão #31)
- [x] Documentar limitação de hardware LLM (Q22, decisão #32)
- [x] Análise comparativa detalhada ollama4j vs LangChain4j (Q08)
- [x] Pesquisar e listar links de skins para Alice (Q24)
- [x] Pesquisar memória Git-native para agentes IA

### Em Andamento / Pendentes do Brainstorm

- [x] ~~Q23b — FakePlayer confirmado~~ (decisão #46) ✓
- [x] ~~Q12 — Edge TTS FranciscaNeural + faster-whisper~~ (decisões #47, #48) ✓
- [ ] **Q07 — Detalhes de persistência da memória:** frequência de commits Git, tamanho de contexto LLM, RAG vs keywords — pode ser resolvido durante implementação (Fase 6)
- [ ] **Q09 — Compatibilidade com mods Cursed Walking:** scripts específicos por mod ou LLM interpreta genericamente — pode ser resolvido durante Fase 5 (base de conhecimento)
- [ ] **Compilar base de conhecimento para mods CRÍTICOS (~25 mods)** — tarefa de conteúdo, iniciar na Fase 5

### STATUS DO BRAINSTORM

**Todas as decisões de arquitetura estão tomadas. O brainstorm pode ser considerado CONCLUIDO.**

Decisões definitivas (48 registradas):
- Plataforma, entidade, pathfinding, LLM, memória, voz, STT, TTS, mods, infraestrutura — tudo decidido
- Q07 e Q09 são detalhes que emergerao naturalmente durante a implementação — não bloqueiam inicio

**PRÓXIMO PASSO: Iniciar a implementação — Fase 1**

### Próximas Etapas - Pré-Implementação (fazer antes de escrever código)

- [ ] Setup ambiente de desenvolvimento: Forge MDK 1.20.1 + Gradle
- [ ] Clonar e estudar: Automatone ou ChatClef (FakePlayer + Baritone — referência direta)
- [ ] Clonar e estudar: HumanCompanions (inventário, skin, respawn)
- [ ] Criar documento de arquitetura técnica (pacotes, classes, interfaces principais)
- [ ] Criar repositorio GitHub alice-memory (memória Git da Alice)
- [x] faster-whisper: INSTALADO E TESTADO — http://192.168.0.225:10300 (Machine 2, Docker, testado)

### Próximas Etapas - Implementação por Fase

- [ ] **Fase 1:** FakePlayer ruiva (skin Scarlet) + spawn por ovo + Baritone (follow/stay) + combate básico + chat texto com LLM (Ollama)
- [ ] **Fase 2:** Crafting autonomo + tracking de inventário + mapeamento de containers + auto-equip
- [ ] **Fase 3:** Voz completa — Edge TTS FranciscaNeural + faster-whisper STT + Simple Voice Chat integration
- [ ] **Fase 4:** Schematics + construção bloco a bloco + interação Create (FakePlayer ja funciona nativo)
- [ ] **Fase 5:** Base de conhecimento compilada + orientacao por fase + quests awareness
- [ ] **Fase 6:** Arquitetura de agentes autonomos + memória Git + skills library + auto-aprendizado
- [ ] Testes com Cursed Walking (contínuo, validar a cada fase antes de avancar)

---

## 12. Riscos e Issues Conhecidos

### R01 — Edge TTS: Endpoint Não-Oficial (Risco: ALTO)

- Edge TTS não tem API pública oficial documentada. O endpoint e descoberto por engenharia reversa do browser
- Microsoft pode mudar, bloquear ou exigir autenticação sem aviso previo
- **Impacto:** Alice fica muda completamente (TTS para de funcionar)
- **Mitigacao:** Modo offline (decisão #49) — Piper local como fallback configurável com `alice.voice.mode=offline`
- **Probabilidade:** Media. Microsoft raramente quebra coisas intencionalmente, mas o endpoint não-oficial e risco real
- **Severidade:** Alta se acontecer sem fallback configurado

### R02 — Baritone no Servidor: Uso Fora do Padrão (Risco: MEDIO)

- Baritone foi projetado para rodar no **cliente** Minecraft, controlando o personagem local do jogador
- FakePlayer no **servidor** + Baritone = uso não-convencional, nunca foi o caso de uso primario
- **O problema técnico específico:**
  - Baritone acessa `Minecraft.getInstance()`, `LocalPlayer`, `ClientLevel` — classes que NÃO EXISTEM no contexto servidor
  - O código de Baritone assume que está rodando com acesso ao game loop do cliente
  - Usar Baritone em um FakePlayer server-side requer ou (a) uma versão/fork adaptada para servidor, ou (b) usar apenas a API de pathfinding do Baritone (IBaritone interface) sem as partes cliente
- **Como Automatone e ChatClef resolveram:**
  - Automatone (Fabric/1.18+) usa a API `IBaritone` do Baritone sem depender do contexto cliente
  - O `FakePlayer` e passado ao Baritone via `BaritoneAPI.getProvider().getPrimaryBaritone(player)`
  - Alice deve seguir o mesmo padrão: instanciar Baritone com o FakePlayer como contexto
- **Impacto se não resolver:** Pathfinding não funciona, `NullPointerException` em `Minecraft.getInstance()`, crash do servidor
- **Mitigacao:** Estudar e replicar o padrão exato de Automatone (GitHub: Ladysnake/Automatone)
- **Probabilidade de problema:** Baixa se implementação seguir Automatone, Alta se tentar usar Baritone da forma padrão
- **Ação crítica — Fase 1:** Validar integração Baritone + FakePlayer no servidor ANTES de qualquer outra feature

### R03 — LLM Lento: Experiência Ruim (Risco: ALTO)

- Hardware atual confirmado: Machine 3 roda Ollama em **CPU pura** — 2 tok/s = ~76s para resposta de 40 palavras
- **CONTRADICAO CRÍTICA A RESOLVER (pré-Fase 1):**
  - Machine 1 (dev/cliente) TEM GPU (GTX 1050 Ti, 4GB VRAM)
  - Machine 3 (Ollama) está em CPU pura — **sem GPU confirmada ou GPU não configurada no Ollama**
  - Recomendações de "usar llama3.2:3b que cabe na GPU" so são validas SE a GPU for ativada
  - Até isso ser resolvido, qualquer modelo vai ser lento (~2 tok/s)
- **Opções para resolver (decidir antes da Fase 1):**
  1. **Investigar/ativar GPU na Machine 3** via SSH — verificar se tem GPU, configurar Ollama com CUDA
  2. **Mover Ollama para Machine 1** (que TEM GTX 1050 Ti) — Ollama server rodando na mesma maquina do cliente
  3. **Usar API cloud como primary** (deepseek-v3, já instalado na Machine 1) — não local mas rápido
- **Com GPU ativa (4GB VRAM):** `llama3.2:3b` (~2GB) cabe inteiro, ~1-3s por resposta — aceitavel
- **Sem GPU:** qualquer modelo = ~30-80s por resposta — inaceitavel para gameplay
- **Mitigacoes complementares (independente da GPU):**
  - 80% das situações resolvidas por regras/scripts sem LLM (latência zero)
  - Streaming LLM + TTS incremental (usuario comeca a ouvir antes de terminar de gerar)
  - Cache de perguntas frequentes (resposta instantanea sem LLM)
- **Ação obrigatória pré-Fase 1:** Testar e validar latência LLM ANTES de implementar Alice

### R04 — Compatibilidade com 201 Mods (Risco: MEDIO)

- 201 mods no Cursed Walking, cada um potencialmente interagindo com FakePlayer de forma inesperada
- Mods de grief-prevention podem registrar ações de Alice como griefing
- Mods de permissões podem bloquear FakePlayer de interagir com certos blocos
- **Impacto:** Alice quebra sistema do modpack, causa bugs ou tem comportamento proibido
- **Mitigacao:**
  - FakePlayer tem `isFakePlayer() = true` — mods bem escritos checam isso
  - Servidor roda em modo offline (TLauncher) = sem verificação Mojang
  - Testar cada fase no servidor real com o modpack completo antes de avancar
- **Probabilidade:** Baixa-media. Mods Forge geralmente respeitam FakePlayer

### R05 — Pontos Únicos de Falha na Infraestrutura (Risco: MEDIO)

| Componente | Onde roda | Se cair | Impacto | Fallback |
|-----------|----------|---------|---------|---------|
| Ollama (Machine 3) | Rede local | Alice sem IA | Total — respostas paradas | API cloud (deepseek-v3, ja no Machine 1) |
| faster-whisper | Machine 2, Docker | Alice surda | Não transcreve voz | Vosk local (zero rede) |
| Edge TTS | Cloud Microsoft | Alice muda | Não sintetiza voz | Piper local (zero rede, voz masculina) |
| GitHub alice-memory | Cloud GitHub | Sem memória portável | Perde histórico da sessao | Forge SavedData local (SEMPRE salva — primario) |
| Rede local inteira | Infraestrutura | Degradacao total | Ollama + faster-whisper inacessiveis | Modo offline completo |

- **IMPORTANTE — Piper e Vosk NÃO são "fallbacks de rede":** eles rodam LOCALMENTE e estão sempre disponíveis (desde que instalados). A pergunta é apenas "quando ativar modo offline", não "se o fallback está disponível".
- **Hierarquia de degradacao graceful:**
  1. Tudo ok: voz feminina neural + STT preciso + LLM local + memória Git
  2. Cloud cai: Piper (voz masculina) + LLM local ainda funciona
  3. Machine 2 cai: Vosk STT local + restante funciona
  4. Machine 3 cai: API cloud LLM como primary
  5. Rede local cai: modo texto apenas (sem voz, sem LLM) — Alice no "modo regras" básico
- **Forge SavedData é sempre o primario** — dados críticos nunca dependem do Git

### R06 — Simple Voice Chat API: Captura de Audio (Risco: BAIXO-MEDIO)

- A SVC API para mods de terceiros pode não expor todos os hooks necessários
- Capturar audio DO jogador (o que ele fala) via API pode ter limitações não documentadas
- Injetar audio DA Alice (TTS output) no canal SVC pode requerer workarounds
- **Impacto:** Sistema de voz pode precisar de abordagem diferente da planejada
- **Mitigacao:** Estudar o Walkie-Talkie mod (ja no modpack) — usa SVC para radio, referência direta
- **Probabilidade:** Baixa (SVC tem API pública bem documentada para integração com mods)

### R07 — Concorrência e Thread Safety (Risco: MEDIO-ALTO)

- Mod usa multiplas threads simultaneas: LLM async, TTS async, STT async, Git async, perception ticks
- Game tick = 50ms. Qualquer operação bloqueante nessa thread trava o servidor inteiro
- Acesso a estado do jogo (entidades, blocos) de threads não-server causa `ConcurrentModificationException`
- **Impacto:** Crash do servidor, corrupcao de mundo
- **Mitigacoes obrigatórias:**
  - TODAS as operações IO (LLM, TTS, STT, Git) em CompletableFuture/threads separadas
  - Ações no jogo (mover Alice, colocar bloco) sempre via `server.execute()` no próximo tick
  - Usar queues thread-safe (`ConcurrentLinkedQueue`) para comunicar entre threads e tick
- **Probabilidade:** Alta sem disciplina de threading. É o bug mais comum em mods assíncronos

### R08 — Alice na TAB List: Confusao de Identidade (Risco: BAIXO)

- FakePlayer aparece na lista de jogadores como "Alice" — outros players no servidor podem se confundir
- Outros jogadores podem tentar atacar Alice como PvP
- Pode interferir em sistemas de slots/limite de jogadores do servidor
- **Impacto:** Estetico e social, não técnico
- **Mitigacao:** E feature, não bug — Alice é uma "jogadora" no mundo. Documentar no README do mod
- **Probabilidade:** Certa. Impacto: Baixo

### R09 — Base de Conhecimento Incompleta ou Errada (Risco: MEDIO)

- 201 mods, wikis desatualizadas, informações contraditórias na internet
- LLM pode alucinar mesmo com RAG (base de conhecimento não é blindagem total)
- Alice pode dar dica errada (craftar item errado, ir para local errado, subestimar ameaça)
- **Impacto:** Jogador perde itens, toma decisão ruim, perde confiança na Alice
- **Mitigacao:**
  - Alice deve dizer "não tenho certeza" quando confiança é baixa
  - Sistema de confirmacao por experiência (decisão #44) — Alice corrige quando errar
  - Base de conhecimento mantida e corrigida pelo jogador continuamente
- **Probabilidade:** Alta (inevitavel no inicio, melhora com uso e correcoes)

### R10 — memória Git: Falha de Commit (Risco: BAIXO)

- Commit Git durante sessao de jogo pode falhar: rede instável, conflito de merge, auth expirada
- **Impacto:** Alice "esquece" o que aconteceu na sessao (perde histórico Git)
- **Mitigacao:**
  - Forge SavedData salva SEMPRE localmente (nunca perde dados críticos)
  - Git e camada adicional de persistência portável, não a única
  - Retry com backoff exponencial para falhas de commit
  - Commits ao final da sessao, não a cada tick (reduz volume de operações)
- **Probabilidade:** Baixa (rede local estável, Git é robusto)

### R11 — Modo Offline Muda Identidade de Voz (Risco: MEDIO)

- No modo online, Alice tem **voz feminina** (pt-BR-FranciscaNeural, neural, natural)
- No modo offline, Alice passa a ter **voz masculina** (pt-br-faber-medium, única opção pt-BR no Piper)
- **Impacto:** Quebra de imersao — Alice é uma personagem feminina com voz que soa masculina no fallback
- **Natureza do risco:** Não e falha técnica, e degradacao de experiência intencional
- **Mitigacao:**
  - Modo offline e emergencia — so ativa quando Edge TTS cai ou rede some
  - Documentar claramente para o usuario que modo offline = voz masculina
  - Alternativa futura: usar `text_only` em vez de `offline` se imersao for prioridade
  - Acompanhar comunidade Piper — feature request de voz feminina pt-BR existe (GitHub issue #766), pode ser adicionada
- **Probabilidade de ativar:** Baixa (Edge TTS é estável, internet do servidor confirmada)
- **Impacto quando ativa:** Medio (funcional, mas Alice soa errada)

---

## 13. Fora do Escopo — O que Não Vamos Fazer

### Permanentemente fora do escopo

| # | O que não faremos | Por que |
|---|------------------|---------|
| 1 | Fine-tuning ou treinamento de LLM próprio | Requer hardware especializado, datasets enormes. Usamos LLM pré-treinado via Ollama |
| 2 | Voice cloning ou voz 100% custom | Edge TTS FranciscaNeural é a voz da Alice. Sem personalização por usuario no MVP |
| 3 | Alice como serviço separado / microservico | Alice é um mod Forge integrado. Não e processo externo, não é servidor dedicado |
| 4 | Geração procedural de schematics em runtime | Alice usa schematics pré-carregados .nbt. Não gera estruturas novas durante o jogo |
| 5 | Alice criar scripts/código automaticamente | Skills são criadas por nos. Alice carrega e executa skills, não as escreve |
| 6 | Integração com redes sociais / streaming / Discord | Alice existe dentro do Minecraft. Sem presença externa |
| 7 | Suporte a Bukkit / Spigot / Paper | Mod Forge exclusivo. Incompativel por arquitetura com plugins Bukkit |
| 8 | Alice ser invencivel | Morre como jogador normal — é parte da lore e do game design |
| 9 | Alice fazer web search em runtime | Base de conhecimento e compilada offline. Sem acesso a internet durante o jogo |
| 10 | Alice ter comandos de admin/op | Não executa /op, /ban, /give. Zero acesso a comandos de servidor |
| 11 | Suporte a versões diferentes de Minecraft 1.20.1 | Foco total em 1.20.1 Forge (servidor existente). Portar para outras versões e projeto separado |
| 12 | Alice ter consciência de tempo real / mundo externo | Não sabe horas, datas reais, eventos do mundo. Vive apenas dentro do jogo |

### Fora do MVP (possível em versões futuras)

| # | O que não faremos no MVP | Quando pode entrar |
|---|--------------------------|-------------------|
| 13 | Multiplas Alices por servidor | Após MVP estável — arquitetura ja preparada (decisão #6) |
| 14 | Suporte a outros modpacks | Requer base de conhecimento específica por modpack — não é generalizavel fácil |
| 15 | Outros idiomas (EN, ES, etc.) | Arquitetura suporta (Edge TTS + Whisper são multilinguais) — so não é prioridade |
| 16 | Interface gráfica de configuração | Config via arquivo .toml Forge. GUI e conforto, não necessidade |
| 17 | Alice no singleplayer | FakePlayer tecnicamente funciona em LAN, mas não testado nem suportado no MVP |
| 18 | Alice aprender novas schematics em runtime | Fase 4 usa só pré-carregadas. Alice sugerir e jogador decidir (decisão #37) |
| 19 | Alice PvP entre servidores | Não faz sentido no escopo de companheira. Se atacar Alice, ela se defende — só isso |
| 20 | Multiplas vozes / personalidades alternativas | MVP: 1 voz fixa FranciscaNeural. Troca de voz possível mas não prioritaria |

### O que Alice NÃO e

- Alice não é um NPC com script fixo — ela usa LLM para raciocinar
- Alice não é um bot de Discord ou Twitch — ela existe so dentro do jogo
- Alice não é um assistente de IA generico — ela é especialista em sobrevivência no Cursed Walking
- Alice não é um god-mode companion — ela pode morrer, pode errar, pode ter informação incompleta
- Alice não substitui o jogador — ela ORIENTA, sugere, ajuda. Quem decide é o jogador

---

## 14. Resumo e Conclusão do Brainstorm

### O que é o Projeto Alice

Alice é uma jogadora controlada por IA para Minecraft Forge 1.20.1, integrada ao modpack Cursed Walking (apocalipse zumbi, 201 mods). É uma companheira feminina ruiva que orienta o jogador, constrói defesas, gerencia recursos e sobrevive junto com ele — com voz neural, memória persistente e raciocinio via LLM.

### Decisões Fundamentais (resumo)

| Dimensão | Decisão | Decisão # |
|----------|---------|-----------|
| Plataforma | Forge 1.20.1 | #1 |
| Entidade | FakePlayer (ServerPlayer) — `net.minecraftforge.common.útil.FakePlayer` | #21/#46 |
| Pathfinding | Baritone (nativo com FakePlayer, zero adapter) | #22 |
| LLM | ollama4j → Ollama local, fallback API cloud | #33 |
| TTS padrão | Edge TTS pt-BR-FranciscaNeural (gratuito, cloud Microsoft) | #47 |
| STT padrão | faster-whisper REST, Machine 2 Docker :10300 | #48 |
| TTS offline | Piper local pt-br-faber-medium | #49 |
| STT offline | Vosk local vosk-model-small-pt-0.3 | #49 |
| Voz | Simple Voice Chat (ja no Cursed Walking) | #30 |
| memória | Forge SavedData (local) + Git repo GitHub (portável) | #28/#31 |
| Agentes | Orquestrador + 6 agentes especializados | #29 |
| Conhecimento | Base compilada markdown + RAG keywords + experiência | #44 |
| Faseamento | 6 fases bem definidas | #23 |
| Total | **49 decisões de arquitetura registradas** | — |

### Stack Técnica Final

```
MOD ALICE (Forge 1.20.1 Java 17)
├── Entidade:       FakePlayer (net.minecraftforge.common.util.FakePlayer)
│                   [Forge built-in, sem dependencia adicional]
├── Pathfinding:    Baritone API — via IBaritone interface (server-side)
│                   [seguir padrao Automatone: Ladysnake/Automatone]
├── LLM client:     ollama4j 1.1.6 — io.github.ollama4j:ollama4j:1.1.6
│                   → http://192.168.0.200:11434 (Ollama local)
│                   [fallback: API cloud via interface AliceLLMProvider]
├── TTS online:     Edge TTS HTTP (cloud) → pt-BR-FranciscaNeural
│                   mp3 → decode → PCM 48kHz → Simple Voice Chat
├── TTS offline:    Piper TTS local → pt-br-faber-medium (voz MASCULINA — unica opcao pt-BR)
├── STT online:     POST http://192.168.0.225:10300/v1/audio/transcriptions
│                   (faster-whisper Docker, Machine 2) → {"text":"..."}
├── STT offline:    Vosk API local → modelo pt-BR (verificar versao em alphacephei.com/vosk/models)
│                   vosk-java binding: com.alphacephei:vosk:0.3.45
├── Voz:            Simple Voice Chat API (ja incluido no Cursed Walking)
│                   de.maxhenkel.voicechat:voicechat-api:2.5.x
├── Memoria local:  Forge SavedData (NBT/JSON) — primario, sempre disponivel
├── Memoria remota: JGit — org.eclipse.jgit:org.eclipse.jgit:6.x
│                   → GitHub alice-memory (repositorio publico)
├── Receitas:       RecipeManager nativo (zero dependencia) + JEI API (soft dep)
├── Quests:         FTB Quests API (soft dep)
└── Construcao:     Schematics .nbt pre-carregados no resources/ do mod

INFRAESTRUTURA
├── Machine 1 (dev/cliente):  192.168.0.x    — VSCode + Claude Code + Gradle + TLauncher (Win 11)
│                              GPU: GTX 1050 Ti 4GB — candidata para Ollama se Machine 3 for CPU-only
├── Machine 2 (MC server):    192.168.0.225  — Forge 1.20.1 + Cursed Walking
│                              Docker/Portainer: faster-whisper fedirz/faster-whisper-server :10300 ✅
└── Machine 3 (Ollama/LLM):   192.168.0.200  — Ollama :11434
                               ⚠️ PENDENTE: verificar GPU via SSH (atualmente CPU pura = 2 tok/s)
```

### Estado da Infraestrutura

| Componente | Estado | Endpoint |
|-----------|--------|---------|
| faster-whisper (STT) | INSTALADO E TESTADO | http://192.168.0.225:10300 |
| Ollama (LLM) | RODANDO (otimizar modelos) | http://192.168.0.200:11434 |
| Edge TTS (TTS) | CONFIRMADO GRATUITO | cloud Microsoft |
| Simple Voice Chat | JA NO MODPACK | — |
| Modo offline (Piper + Vosk) | A INSTALAR (quando precisar) | local |
| GitHub alice-memory | A CRIAR (inicio da implementação) | github.com |

### Faseamento

| Fase | Objetivo | Desbloqueado por |
|------|---------|-----------------|
| Fase 1 | FakePlayer ruiva + Baritone (follow/stay) + chat texto com LLM | Forge MDK setup |
| Fase 2 | Crafting autonomo + inventário + containers + auto-equip | Fase 1 estável |
| Fase 3 | Voz completa — Edge TTS + faster-whisper + Simple Voice Chat | Fase 2 estável |
| Fase 4 | Schematics + construção bloco a bloco + Create (FakePlayer nativo) | Fase 3 estável |
| Fase 5 | Base de conhecimento compilada + orientacao por fase + quests | Fase 4 estável |
| Fase 6 | Agentes + skills + memória Git + autonomia avancada | Fase 5 estável |

### Próximas Etapas Imediatas (Pré-Fase 1)

1. **Setup Forge MDK 1.20.1** — Gradle + IntelliJ/VSCode, build Hello World rodando no servidor
2. **Resolver gargalo LLM (CRÍTICO)** — investigar Machine 3 via SSH: tem GPU? Ollama com CUDA?
   - Se tem GPU: configurar Ollama + testar `llama3.2:3b` (deve dar ~1-3s por resposta)
   - Se não tem GPU: decidir entre mover Ollama para Machine 1 OU usar API cloud como primary
   - **Não comecar Fase 1 sem latência LLM aceitavel (~3-5s)**
3. **Estudar Automatone (GitHub: Ladysnake/Automatone)** — FakePlayer + Baritone no servidor, padrão exato a replicar
4. **Criar repositorio GitHub alice-memory** — estrutura de pastas inicial (knowledge/, memories/, skills/, sessions/)

### Visão Final

> Alice não é um NPC — ela é uma jogadora. Uma companheira real que vive no mesmo mundo, enfrenta os mesmos perigos, e carrega o conhecimento de quem estudou esse apocalipse antes de existir ali. Ela orienta, constrói, luta e lembra. E ela fala com você em portugues, com voz de verdade.

**O brainstorm está CONCLUIDO. 49 decisões de arquitetura tomadas. A implementação pode comecar.**
