# Projeto Alice - Documento de Brainstorm e Levantamento de Requisitos

**Data de inicio:** 2026-04-01  
**Ultima atualizacao:** 2026-04-03  
**Status:** CONCLUIDO — Brainstorm completo, 49 decisoes tomadas  
**Versao:** 0.9

---

## 1. Visao Geral do Projeto

Alice e um mod para Minecraft Forge 1.20.1 que adiciona uma jogadora controlada por IA ao jogo. Alice e uma personagem feminina que atua como companheira de sobrevivencia e amiga do jogador.

### Identidade da Alice

- **Nome:** Alice
- **Genero:** Feminino
- **Aparencia:** Menina ruiva (skin customizada)
- **Personalidade:** Amiga, companheira, protetora, inteligente, pragmatica. Motivada por seguranca propria e do grupo.
- **Papel:** Engenheira de sobrevivencia e guia do jogador. Alice ORIENTA o jogador durante o jogo.
- **Consciencia:** Alice sabe que e uma IA dentro do mundo do jogo. Ela tem "lembrancas de outra vida" — conhecimento compilado de fontes publicas da internet sobre o apocalipse zumbi, engenharia de sobrevivencia, mecanicas do jogo, receitas, estruturas, etc. Ela trata essas informacoes naturalmente como memorias de antes de existir naquele mundo.
- **Lore:** Alice "estudou" este apocalipse zumbi em sua vida anterior. Ela sabe informacoes cruciais que compartilha com o jogador conforme a fase de jogo progride. Essas informacoes sao compiladas de fontes confiaveis e publicas, tornando Alice uma fonte confiavel de conhecimento.
- **Aprendizado:** Alice confronta a informacao que tem com a experiencia propria e do jogador para aprimorar seu conhecimento. Se algo que ela "lembrava" se prova errado na pratica, ela atualiza seu entendimento.
- **Motivacao principal:** Ficar segura. Por isso aprecia estar com o jogador para viver aventuras juntos. Pode ficar sozinha se o local for seguro ou se for necessario para ajudar.
- **Por que protege-la:** Alice, como gesto de amizade, compartilha toda sua informacao e ainda constroi coisas para o jogador. Por isso o jogador deveria protege-la — ela e valiosa demais para perder.

### Contexto do Servidor

- **Modpack:** Cursed Walking - A Modern Zombie Apocalypse (201 mods)
- **Tematica:** Apocalipse zumbi com hordas, zumbis mutantes, infeccao, armas de fogo, cidades, blood moons, armaduras modernas, armas avancadas de endgame
- **Plataforma:** Minecraft 1.20.1 Forge
- **CurseForge:** https://www.curseforge.com/minecraft/modpacks/cursed-walking-a-modern-zombie-apocalypse
- Alice deve ser compativel com todos os mods do modpack
- **Lista completa de mods:** Pendente — sera extraida diretamente do servidor via acesso SSH (ver Infraestrutura)

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
- **Diagnostico:** Ollama rodando 100% em CPU — sem GPU configurada ou sem GPU disponivel
- **Impacto:** Respostas de Alice serao LENTAS com configuracao atual
- **Solucao a curto prazo:** Usar modelos menores (phi3:mini ja instalado, 2.2GB)
- **Solucao ideal:** Configurar GPU no Ollama OU usar API remota como primary
- **Pendencia:** Verificar hardware da Maquina 3 via SSH (auth por senha nao funcionou, tentar chave SSH)

**Nota sobre Q22 revisada:** O gargalo nao e a falta de GPU na maquina cliente (GTX 1050 Ti), mas sim que a maquina do Ollama parece rodar so em CPU. Isso muda a estrategia — API remota (deepseek-v3.2:cloud etc. ja instalados na Maquina 1) pode ser o caminho mais viavel para responsividade.

---

## 2. Registro de Decisoes

| # | Data | Decisao | Justificativa |
|---|------|---------|---------------|
| 1 | 2026-04-01 | Mod Forge 1.20.1 | Servidor ja roda Cursed Walking nesta versao |
| 2 | 2026-04-01 | Nome: Alice (feminino) | Definido pelo escopo inicial |
| 3 | 2026-04-01 | Arquitetura hibrida: scripts + IA | Performance - IA decide O QUE, scripts executam COMO |
| 4 | 2026-04-01 | Modpack alvo: Cursed Walking | Servidor existente do usuario |
| 5 | 2026-04-01 | Backend IA: Ollama local + API remota | Objetivo final e local, API remota como fallback |
| 6 | 2026-04-01 | 1 Alice por servidor (MVP) | Futuro: multiplas Alices |
| 7 | 2026-04-01 | Spawn via ovo no inventario | Ovo colocado no chao invoca Alice |
| 8 | 2026-04-01 | Alice nao perde itens ao morrer | Renasce com tudo que tinha |
| 9 | 2026-04-01 | Respawn: cama ou ponto original do ovo | Igual jogador, ponto do ovo nunca se perde |
| 10 | 2026-04-01 | Ao renascer busca base ou jogador | Escolhe o mais seguro/facil |
| 11 | 2026-04-01 | Frase-chave "vamos combinar assim" | Indica que o jogador quer dar uma tarefa/comando |
| 12 | 2026-04-01 | Alice deve saber usar TUDO do jogo | Armas, ferramentas, itens de todos os mods |
| 13 | 2026-04-01 | Alice conhece todas as receitas | Via RecipeManager nativo + JEI API |
| 14 | 2026-04-01 | Alice sabe construir via schematics | Estruturas pre-carregadas no mod |
| 15 | 2026-04-01 | Alice tem memoria precisa de inventarios | Proprio, do jogador, e de containers da base |
| 16 | 2026-04-02 | Alice tem voz (TTS) | **ATUALIZADO #47:** Edge TTS cloud pt-BR-FranciscaNeural (gratuito, ilimitado) |
| 17 | 2026-04-02 | Alice ouve o jogador (STT) | **ATUALIZADO #48:** faster-whisper REST (Machine 2, Docker) |
| 18 | 2026-04-02 | Comunicacao por voz e radio | Proximidade + radio craftavel para distancia |
| 19 | 2026-04-02 | Idioma: Portugues BR (MVP) | Outros idiomas em versoes futuras |
| 20 | 2026-04-02 | Voz feminina padrao (MVP) | Uma unica voz fixa para Alice |
| 21 | 2026-04-02 | ~~Entidade: Custom Mob~~ **→ REVISADO: FakePlayer (ServerPlayer)** | **REVISADO #46:** FakePlayer resolve Baritone nativamente + herda inventario, morte, skin, Create, SVC |
| 22 | 2026-04-02 | Pathfinding: Baritone | Poderoso, maduro (8.8k stars). Com FakePlayer: funciona nativo sem adapter. |
| 23 | 2026-04-02 | Faseamento completo definido e escopado | Concordancia com fases, mas projeto inteiro bem definido ate o fim |
| 24 | 2026-04-02 | Alice e guia/orientadora do jogador | Objetivo principal: orientar o jogador durante o jogo |
| 25 | 2026-04-02 | Alice tem lore de ter estudado o apocalipse | "Lembrancas de outra vida" = pesquisa sobre o apocalipse zumbi |
| 26 | 2026-04-02 | Base de conhecimento compilada de fontes publicas | Alice e fonte confiavel; confronta info com experiencia |
| 27 | 2026-04-02 | Alice e uma engenheira de sobrevivencia | Sabe estruturas, defesas, contraptions Create, otimizacoes |
| 28 | 2026-04-02 | Memoria persistente via repositorio Git | Repo publico, branches por sessao, PRs para consolidar, master = estado atual |
| 29 | 2026-04-02 | Alice usa agentes, skills e regras de IA | Tudo disponivel para IA deve ser usado para melhorar resposta e latencia |
| 30 | 2026-04-03 | Simple Voice Chat confirmado no modpack | Q13 resolvida — Cursed Walking ja inclui SVC |
| 31 | 2026-04-03 | Repositorio de memoria: GitHub publico | Q20 resolvida — criar repo no GitHub quando iniciar projeto |
| 32 | 2026-04-03 | Hardware LLM limitado: 1 modelo, lento | Q22 resolvida — otimizar para modelo unico, pre-cache, skills compiladas |
| 33 | 2026-04-03 | LLM library: ollama4j (MVP) | Q08 resolvida — leve, tool calling funciona, interface abstrata para trocar depois |
| 34 | 2026-04-03 | Skin: "Scarlet (Remake)" do Planet Minecraft | Q24 resolvida — https://www.planetminecraft.com/skin/scarlet-remake/ |
| 35 | 2026-04-03 | Sem radio proprio — usar Simple Voice Chat diretamente | Q11 resolvida — SVC ja esta no modpack com todas as features de radio |
| 36 | 2026-04-03 | Alice offline = player offline (fica no mundo, inativa) | Q14 resolvida — comportamento identico a player desconectado |
| 37 | 2026-04-03 | Schematics: nenhum padrao, .nbt, so pre-carregados, LLM sugere | Q10 resolvida — jogador carrega seus proprios .nbt |
| 38 | 2026-04-03 | Sistema de ameaca: fatores pesados, toggleaveis, limiar configuravel | Q15a+b resolvida |
| 39 | 2026-04-03 | Padrao de Comunicacao Eficiente (PCE) definido | Q15c resolvida — protocolo de aviso+confirmacao para situacoes de perigo |
| 40 | 2026-04-03 | Animacoes padrao do player + waypoints via Map Atlases | Q16 resolvida |
| 41 | 2026-04-03 | Redstone/Create: CONFIRMADO com FakePlayer | Q17 resolvida — FakePlayer interage com tudo nativamente |
| 42 | 2026-04-03 | Hierarquia social: Amigo (dono do ovo) > Conhecido > Hostil | Q18 resolvida |
| 43 | 2026-04-03 | Sem restricao de tamanho para o mod | Q19 resolvida |
| 44 | 2026-04-03 | Base de conhecimento: Opcao C hibrida, confirmacao por experiencia | Q21 resolvida |
| 45 | 2026-04-03 | Maquina do Ollama: CPU pura, 2 tok/s, llama3.2 e phi3 instalados | Q22 complemento — sem GPU na maquina do Ollama |
| 46 | 2026-04-03 | Entidade Alice: FakePlayer (ServerPlayer) — decisao #21 REVISADA | Q23b confirmado — FakePlayer substitui Custom Mob. Resolve Baritone, Create, SVC, inventario, morte, skin |
| 47 | 2026-04-03 | TTS: Edge TTS cloud pt-BR-FranciscaNeural — decisao #16 REVISADA | Q12 resolvida — gratuito, ilimitado, qualidade neural, sem API key. Piper descartado (sem voz feminina pt_BR) |
| 48 | 2026-04-03 | STT: faster-whisper (Machine 2, Docker/Portainer) | Q12 complemento STT — REST API OpenAI-compativel, gratuito, ilimitado, estado da arte em reconhecimento de fala |
| 49 | 2026-04-03 | Modo voz offline configuravel (Piper TTS + Vosk STT) | Fallback ativado por config quando Edge TTS ou Machine 2 estiver indisponivel. Modo: online (padrao) ou offline (local). |

---

## 3. Funcoes da Alice

### 3.0 Funcao Central: Guia e Orientadora do Jogador (decisao #24)

Alice existe para ORIENTAR o jogador durante o jogo. Tudo que ela faz serve a esse proposito.

#### FG01 - Base de Conhecimento Confiavel (decisao #26)

- Alice possui uma base de conhecimento compilada de fontes publicas e confiaveis da internet
- Essa base contem informacoes sobre:
  - Mecanicas do Minecraft e de TODOS os mods do Cursed Walking
  - Estrategias de sobrevivencia em apocalipse zumbi
  - Receitas, materiais, progressao
  - Estruturas defensivas e seus pros/contras
  - Contraptions e maquinas do Create mod
  - Armas, armaduras, ferramentas e como usa-las eficientemente
  - Farming, criacao de animais, fontes de comida
  - Infeccao zumbi, blood moons, ameacas especiais do modpack
- Essas informacoes sao as "lembrancas de outra vida" da Alice (lore, decisao #25)
- **Formato:** Embutidas no system prompt do LLM + RAG para busca contextual
- **Atualizacao:** Alice confronta informacao com experiencia propria e do jogador
  - Se algo se prova errado na pratica, Alice atualiza seu entendimento
  - Se descobre algo novo, registra na memoria

#### FG02 - Engenheira de Sobrevivencia (decisao #27)

- Alice sabe avaliar se uma estrutura e boa ou ruim para se defender
- Sabe quais maquinas e contraptions do Create sao apropriadas para cada situacao
- Sugere otimizacoes e melhorias na base do jogador
- Sabe construir as coisas que recomenda (via schematics)
- Informa o jogador sobre riscos estruturais ("essa parede nao vai aguentar uma horda")
- Recomenda upgrades conforme a fase de jogo progride

#### FG03 - Orientacao por Fase de Jogo

- Alice compartilha informacoes conforme a fase de jogo em que o jogador esta
- **Early game:** Foco em abrigo, comida, armas basicas, evitar infeccao
- **Mid game:** Foco em base fortificada, farming, armas melhores, explorar cidades
- **Late game:** Foco em armas avancadas, armaduras endgame, contraptions Create, blood moons
- Alice nao despeja tudo de uma vez — revela informacao conforme contexto e necessidade
- Usa a experiencia acumulada para calibrar quando e o que falar

### 3.1 Funcao Primaria: Companheira de Sobrevivencia

#### FP01 - Conhecimento de Metas e Quests

- Sabe quais sao as conquistas e quests do jogador
- Fornece informacoes sobre o que fazer a seguir
- Acompanha progresso e sugere proximos passos

#### FP02 - Crafting e Construcao

- Conhece TODAS as receitas de craft do jogo (incluindo mods) via RecipeManager + JEI API
- Sabe craftar qualquer item
- Constroi estruturas via schematics pre-carregados no mod
- Constroi contraptions do mod Create via schematics
- Fornece lista de materiais necessarios indicando quanto falta e quais faltam

#### FP03 - Gestao de Recursos e Memoria

- Memoria precisa do inventario dela e do jogador
- Memoria do conteudo de containers dentro dos inventarios
- Lembra tudo que esta guardado nos containers que ela ou o jogador tenham craftado ou mudado de lugar no jogo, e sabe onde esta cada item
- Ajuda a coletar materiais (pode coletar o mesmo material ou outro da lista)
- Coordena com o jogador a divisao de tarefas de coleta

#### FP04 - Combate e Avaliacao de Ameacas

- Ajuda o jogador em combate
- **Sistema de regua de nivel de ameaca**: mede pelo que sabe/sente do jogo quando e hora de combater e quando e hora de fugir
- Avisa o jogador sobre perigos
- Prioridade: ajudar o jogador a escapar quando necessario
- Sabe usar todas as armas do jogo (incluindo armas de fogo do modpack)

#### FP05 - Uso Universal de Itens

- Sabe usar TUDO que existe no jogo
- Sabe criar qualquer item e conhece os materiais necessarios
- Coleta materiais junto com o jogador ou de forma independente
- Entende "combinacoes" com o jogador como comandos/ordens prioritarias

### 3.2 Funcao Secundaria: Companhia e Conversa

#### FS01 - Conversas Variadas

- Conversa sobre atualidades, ciencia, historia, filosofia
- Atua como um amigo que ajuda e conversa
- Comenta por vezes sobre atualidades que pesquisou (via "lembrancas de outra vida")

#### FS02 - Memoria de Aventuras

- Lembra de aventuras vividas juntos no jogo
- Recorda desafios, dificuldades e vitorias
- Usa essas memorias em conversas futuras

### 3.3 Sistema de Comandos

- **Frase-chave:** "vamos combinar assim" - ativa modo de receber tarefa
- Combinacoes com o jogador tem peso forte (ordem + tarefa de sobrevivencia)
- Sempre aberta a combinar tarefas com o jogador
- Entende linguagem natural para instrucoes
- Pode receber comandos por voz (proximidade) ou por radio (distancia)

### 3.4 Sistema de Voz

#### VOZ01 - Alice Fala (TTS)

- Alice fala em portugues BR com voz feminina (pt-BR-FranciscaNeural)
- Fala por proximidade quando perto do jogador (via Simple Voice Chat)
- Fala pelo canal SVC a distancia quando longe do jogador
- TTS via Microsoft Edge TTS cloud (decisao #47) — sem arquivos locais no mod

#### VOZ02 - Alice Ouve (STT)

- Alice ouve a voz do jogador quando proximo (via Simple Voice Chat)
- Alice ouve pelo canal SVC quando distante
- STT via faster-whisper rodando em Machine 2 via Docker (decisao #48) — REST OpenAI-compatible
- Converte voz em texto -> envia para LLM -> gera resposta

#### VOZ04 - Modo Offline (decisao #49)

- Quando Edge TTS (cloud) ou faster-whisper (Machine 2) estiver indisponivel
- Ativado via configuracao: `alice.voice.mode=offline`
- **TTS offline:** Piper TTS rodando localmente — modelo `pt-br-faber-medium` (unica voz pt-BR disponivel no Piper — masculina, qualidade degradada)
- **STT offline:** Vosk rodando localmente — modelo `vosk-model-small-pt-0.3` (45MB, verdadeiramente offline, sem Docker)
- Qualidade menor que o modo padrao, mas funciona sem internet e sem Machine 2
- **Tres modos configuráveis:**
  - `online` (padrao): Edge TTS cloud + faster-whisper REST (Machine 2)
  - `offline`: Piper local + Vosk local (sem rede, sem infra)
  - `text_only`: sem voz — apenas chat texto
- **Referencia:** edge-tts, piper-tts, vosk-api

#### VOZ03 - Radio de Comunicacao

- Item craftavel proprio do mod Alice
- Permite comunicacao por voz entre Alice e jogador a qualquer distancia na mesma dimensao
- Alice sempre tem um "radio interno" (nao precisa do item)
- Jogador precisa craftar e manter o radio no hotbar ou inventario
- Referencia: Walkie-Talkie mod (Simple Voice Chat API)

---

## 4. Requisitos Funcionais Detalhados

### RF01 - Entidade Alice

- **Tipo:** FakePlayer (ServerPlayer) — decisao #21 REVISADA, decisao #46
- **Classe base:** `net.minecraftforge.common.util.FakePlayer` (extende ServerPlayer)
- **Aparencia:** Menina ruiva com skin Scarlet (decisao #34) — funciona out-of-the-box no FakePlayer
- Spawn via ovo proprio no inventario do jogador
- Ponto de spawn original (onde o ovo foi colocado) nunca se perde
- Funciona em singleplayer e multiplayer
- **Aparece na TAB** como jogador na lista de jogadores do servidor
- **Inventario nativo:** 36 slots + 4 armor + offhand — sem implementacao custom
- **Morte e respawn nativos:** Herda toda a logica do ServerPlayer
- **Interacao com blocos:** Qualquer bloco (redstone, crafting table, Create) — igual jogador real
- **Referencia:** Duzo's Alice, Automatone, ChatClef, Forge FakePlayer API

### RF02 - Morte e Respawn

- Morre como jogador normal
- NAO perde itens, armaduras ou qualquer coisa ao morrer
- Respawna na cama ou no ponto original do ovo
- Ao renascer: busca ir para a base ou para perto do jogador (escolhe o mais seguro)
- **Referencia:** HumanCompanions (sistema de respawn)

### RF03 - Movimentacao e Navegacao

- **Pathfinding:** Baritone — decisao #22
- Pathfinding autonomo (A*, desvio de obstaculos, pular, nadar, minerar, construir)
- Seguir jogador (modo follow)
- Ficar parada (modo stay)
- Navegar independentemente para locais conhecidos
- Mine/follow/build embutidos no Baritone
- **Integracao com FakePlayer:** Baritone foi projetado para controlar ServerPlayer — funciona nativamente com FakePlayer sem nenhum adapter ou workaround. O conflito tecnico original (Q23) foi eliminado pela mudanca da decisao #21 para FakePlayer.
- **Referencia:** Baritone, ChatClef, Automatone (todos usam Baritone + FakePlayer/Player)

### RF04 - Combate

- Atacar mobs hostis com qualquer arma (incluindo armas de fogo)
- Sistema de avaliacao de ameaca (regua de perigo)
- Decisao autonoma: lutar vs fugir
- Alertar jogador sobre perigos (por chat, voz ou radio)
- Equipar melhor arma/armadura disponivel
- **Referencia:** HumanCompanions (combate), SiliconeDolls (acoes tick-based)

### RF05 - Crafting e Receitas

- Acesso programatico a TODAS as receitas via `RecipeManager.getAllRecipesFor()`
- Tipos suportados: CRAFTING, SMELTING, BLASTING, SMOKING, CAMPFIRE, STONECUTTING, SMITHING
- Inclui receitas de TODOS os mods automaticamente
- JEI API como soft dependency para lookup mais rico
- Capacidade de craftar itens autonomamente
- Informar jogador sobre receitas e materiais necessarios
- Calcular materiais faltantes considerando todos os containers mapeados
- **Referencia:** Minecraft RecipeManager API, JEI API

### RF06 - Gestao de Inventario e Containers

- Gerenciar proprio inventario
- Tracking do inventario do jogador
- **Mapeamento de containers:** Registrar localizacao e conteudo de todos os containers que Alice ou o jogador tenham craftado ou mudado de lugar no jogo
- Atualizar mapa de containers quando conteudo muda
- Equipar automaticamente melhores itens
- Saber onde encontrar cada item nos containers mapeados
- **Persistencia:** Salvar mapa de containers via Forge SavedData API
- **Referencia:** HumanCompanions (auto-equip), Metropolize Companions

### RF07 - Construcao via Schematics

- Carregar schematics pre-incluidos no mod (formato .nbt)
- Construir estruturas bloco a bloco
- Construir contraptions do Create
- Algoritmo de construcao: bottom-up, solidos primeiro, suportados por ultimo
- Informar materiais necessarios antes de construir
- **Referencia:** Create Schematicannon, Forgematica

### RF08 - Comunicacao por Texto (Chat)

- Conversa natural via chat do jogo
- Recebe comandos via linguagem natural
- Frase-chave "vamos combinar assim" para modo tarefa
- Conversa casual (ciencia, historia, filosofia, atualidades)
- Lembra e referencia aventuras passadas
- **Referencia:** CreatureChat, AI-Player, Mindcraft

### RF09 - Comunicacao por Voz

- **TTS (Alice fala) — decisao #47:**
  - **Microsoft Edge TTS** — voz `pt-BR-FranciscaNeural` (feminina, neural, excelente qualidade)
  - Alternativa: `pt-BR-ManuelaNeural` (segunda voz feminina BR disponivel)
  - Gratuito, sem API key, sem cadastro, sem limite documentado de uso
  - Usa o mesmo motor do Windows Narrator e Edge browser — qualidade neural da Microsoft
  - Chamada HTTP simples: POST com SSML -> resposta mp3/opus
  - Requer internet no servidor (confirmado disponivel)
  - Audio recebido: mp3 -> decoder -> PCM 48kHz -> Simple Voice Chat

- **STT (Alice ouve) — decisao #48:**
  - **faster-whisper** rodando em Machine 2 (192.168.0.225) via Docker/Portainer — **CONFIRMADO RODANDO**
  - REST API OpenAI-compativel: `POST http://192.168.0.225:10300/v1/audio/transcriptions`
  - Modelo: Systran/faster-whisper-small (ou tiny para menor RAM)
  - Gratuito, sem limites, open source, roda local
  - Java chama via HTTP — mesma abordagem que o Ollama
  - Whisper e o estado da arte em STT — treinado em 680k horas de audio, pt_BR excelente
  - Captura audio do jogador via Simple Voice Chat API -> envia para faster-whisper -> texto

- **Integracao com FakePlayer (decisao #46):**
  - Alice e um FakePlayer — Simple Voice Chat ja suporta players nativamente
  - Nao precisa de plugin especial de entidade de voz (ao contrario do Custom Mob)
  - SVC trata Alice exatamente como qualquer outro jogador para fins de audio

- **Pipeline completo (atualizado):**
  1. Jogador fala (mic) -> Simple Voice Chat captura audio (opus)
  2. Audio -> decode opus -> PCM -> faster-whisper REST (Machine 2, Docker) -> texto
  3. Texto -> LLM Ollama (Machine 3) -> resposta texto
  4. Resposta -> Edge TTS HTTP (cloud Microsoft) -> audio mp3
  5. Audio mp3 -> decode -> PCM 48kHz -> Simple Voice Chat API -> jogador ouve Alice

- **Latencia estimada (novo pipeline):**
  - STT faster-whisper: ~300-500ms (local, rapido)
  - LLM Ollama: ~2-5s (CPU, maquina 3)
  - TTS Edge TTS: ~200-400ms (rede, Microsoft rapido)
  - **Total: ~3-6 segundos** (domina o LLM, nao o TTS/STT)

- **Dependencia obrigatoria:** Simple Voice Chat mod instalado no servidor e cliente

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

- **Modo Offline (decisao #49):**
  - `alice.voice.mode=offline` ativa Piper (TTS local) + Vosk (STT local)
  - `alice.voice.mode=text_only` desativa voz completamente
  - Vosk: `vosk-model-small-pt-0.3` (45MB, OpenJDK compativel, zero infra adicional)
  - Piper: `pt-br-faber-medium` (unica opcao pt-BR — voz masculina, degradacao aceitavel em fallback)
  - Offline mode e fallback de emergencia — modo padrao continua sendo `online`

- **Referencia:** Simple Voice Chat API, edge-tts (biblioteca Python para estudo do endpoint), faster-whisper, openai-whisper-server, Walkie-Talkie mod, vosk-api (Java binding), piper-tts

### RF10 - Memoria e Persistencia (decisao #28)

- **Memoria de curto prazo:** Contexto atual da conversa (janela de tokens do LLM)
- **Memoria de longo prazo:** Aventuras, decisoes, locais, jogadores
  - Salva via Forge SavedData API (persistencia local no mundo)
  - Formato: CompoundTag (NBT) com JSON serializado
  - Persiste entre sessoes do servidor
- **Memoria de inventarios:** Mapa de containers (localizacao + conteudo)
- **"Lembrancas de outra vida":** Base de conhecimento compilada (embutida no system prompt + RAG)
- **Memoria de combinacoes:** Tarefas acordadas com o jogador (prioridade alta)

#### RF10.1 - Memoria Persistente via Repositorio Git (decisao #28)

- **Conceito:** Alice usa um repositorio Git publico como memoria de longo prazo portavel
- **Motivacao:** A memoria nao fica presa a um servidor especifico. Qualquer nova sessao de jogo, em qualquer servidor, pode ler o estado mais recente da memoria da Alice.
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
  4. Durante o jogo, faz commits com memorias novas, skills aprendidas, etc.
  5. Ao encerrar sessao (jogador desconecta ou servidor para), faz push da branch
  6. Cria PR automatico da branch da sessao para `develop`
  7. PR e consolidado (merge) — pode ser automatico ou revisado
  8. Periodicamente, `develop` e mergeado na `master`
- **Vantagens:**
  - Memoria portavel entre servidores e maquinas
  - Historico completo de tudo que Alice viveu (git log)
  - Diff entre sessoes (o que mudou)
  - Rollback se algo der errado
  - Repo publico = transparencia total
  - Outros agentes/processos podem ler e contribuir
  - Funciona como "cerebro" compartilhado se houver multiplas Alices no futuro
- **Implementacao tecnica:**
  - Biblioteca Java para Git: JGit (Eclipse) ou chamadas `git` via ProcessBuilder
  - Autenticacao: SSH key ou token de acesso pessoal
  - Operacoes Git rodam em thread separada (nao bloqueia game tick)
  - Commits sao batched (nao a cada tick, mas a cada evento significativo)
- **Referencia:** Git Context Controller (paper), git-native semantic memory, GitHub Copilot agentic memory

- **Referencia geral RF10:** Forge SavedData API, CreatureChat, Voyager skill library, JGit

### RF11 - Consciencia de Quests

- Acessar sistema de quests/conquistas do jogador
- Sugerir proximos passos baseado em quests ativas
- Acompanhar progresso
- **Dependencia:** FTB Quests (se presente no modpack)
- **Referencia:** FTB Quests API

### RF12 - Percepcao do Mundo

- Detectar mobs hostis num raio configuravel (AABB scanning)
- Detectar jogadores proximos
- Detectar itens no chao
- Monitorar blocos quebrados/colocados proximos
- Escutar chat do servidor
- Reagir a eventos: LivingHurtEvent, EntityJoinLevelEvent, ServerChatEvent
- **Referencia:** Forge Event System

### RF13 - Base de Conhecimento Compilada (decisao #26)

- Alice carrega uma base de conhecimento pre-compilada sobre o jogo e o apocalipse
- **Fontes de informacao:**
  - Wikis publicas do Minecraft e dos mods do Cursed Walking
  - Guias de sobrevivencia e estrategias publicados pela comunidade
  - Documentacao oficial dos mods (Create, armas de fogo, etc.)
  - Mecanicas de infeccao zumbi, blood moons, hordas
  - Receitas e progressao do modpack
- **Formato de armazenamento:**
  - Arquivos markdown no repositorio Git da Alice (knowledge/)
  - Indexados via embeddings para busca semantica (RAG)
  - Carregados no system prompt do LLM conforme contexto
- **Entrega progressiva:**
  - Alice nao despeja tudo de uma vez
  - Compartilha informacao conforme a fase de jogo e o contexto
  - Usa triggers: quando o jogador encontra um item novo, entra em area nova, enfrenta ameaca nova
- **Atualizacao:**
  - Alice confronta informacao com experiencia (decisao #26)
  - Se algo se prova errado, atualiza no repositorio Git
  - Se descobre algo novo via experiencia, registra como skill/memoria
- **Referencia:** Voyager (RAG + skill library), AI-Player (RAG system), Mindcraft (system prompts)

### RF14 - Sistema de Agentes e Skills (decisao #29)

Alice usa uma arquitetura de agentes e skills para maximizar a qualidade das respostas e minimizar latencia.

#### RF14.1 - Skills (Capacidades Modulares)

- **Definicao:** Skill = pacote composavel de instrucoes, codigo e recursos que Alice carrega sob demanda
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
  - **Construcao:** Estruturas defensivas, contraptions Create
  - **Crafting:** Receitas otimizadas, ordens de crafting eficientes
  - **Navegacao:** Rotas seguras, pontos de interesse, exploração
  - **Social:** Padroes de conversa, respostas a perguntas frequentes
  - **Sobrevivencia:** Farming, comida, cura, infeccao
- **Skill Library (inspirado no Voyager):**
  - Skills sao armazenadas no repositorio Git (skills/)
  - Indexadas por embedding da descricao
  - Quando Alice enfrenta situacao nova, busca top-5 skills mais relevantes
  - Skills complexas compostas de skills simples (composicao)
  - Novas skills podem ser criadas automaticamente quando Alice resolve um problema novo
  - **Self-verification:** Apos executar skill, Alice verifica se o resultado foi alcancado
    - Se sim: skill confirmada e mantida
    - Se nao: skill revisada ou descartada

#### RF14.2 - Agentes (Raciocinio Especializado)

- **Definicao:** Agentes sao especializacoes do LLM que atuam em dominios diferentes
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
  - **Agente de Combate:** Avaliacao de ameaca, taticas, escolha de arma, posicionamento
  - **Agente de Construcao:** Selecao de schematic, posicionamento, materiais, contraptions
  - **Agente de Chat:** Conversa natural, humor, lembrancas, personalidade
  - **Agente de Quest:** Progresso, sugestoes, proximos passos, prioridades
  - **Agente de Sobrevivencia:** Comida, cura, infeccao, recursos, crafting
  - **Agente de Navegacao:** Rotas, exploração, mapeamento, pontos de interesse
- **Orquestrador:**
  - Recebe input (texto, voz, evento do mundo)
  - Classifica o tipo de situacao (combate? conversa? quest? construcao?)
  - Roteia para agente especializado
  - Agente especializado carrega skills relevantes
  - Resposta e executada pela camada de acao
- **Regras:**
  - Regras sao comportamentos ALWAYS-ON que nao dependem do LLM
  - Executam a cada tick ou a cada evento
  - Exemplos: fugir se vida < 20%, comer se fome < 6, equipar melhor arma automaticamente
  - Regras tem prioridade sobre decisoes do LLM (seguranca primeiro)

#### RF14.3 - Otimizacao de Latencia e Qualidade

- **Objetivo:** Tudo disponivel para IA deve ser usado para melhorar resposta e diminuir latencia (decisao #29)
- **Estrategias:**
  - **Cache de respostas:** Perguntas frequentes respondidas instantaneamente sem LLM
  - **Pre-computacao:** Skills relevantes pre-carregadas baseado em contexto atual
  - **Routing inteligente:** Perguntas simples vao para modelo menor/mais rapido
  - **Streaming:** LLM gera resposta em streaming, TTS comeca a falar antes de terminar
  - **Batch de percepcao:** Mundo escaneado a cada N ticks, nao a cada tick
  - **Skills compiladas:** Skills usadas frequentemente sao "compiladas" em scripts puros (sem LLM)
  - **Embeddings locais:** Busca de skills/memorias via embeddings locais (rapido)
  - **Regras > LLM:** Comportamentos previsíveis sempre via regras (latencia zero)
- **Referencia:** Mindcraft (multi-agent), Voyager (skill library + self-verification), Agent Skills paper (arxiv 2602.12430)

---

## 5. Requisitos Nao-Funcionais

### RNF01 - Performance

- NAO causar lag perceptivel no servidor
- Chamadas LLM 100% assincronas (nunca bloquear game tick de 50ms)
- Comportamentos reativos (combate, fuga) via scripts, nao IA
- Latencia para decisoes LLM: < 3 segundos
- TTS/STT em threads separadas
- Scanning de mundo limitado (nao escanear a cada tick)

### RNF02 - Compatibilidade

- Forge 1.20.1
- Compativel com todos os 201 mods do Cursed Walking
- JEI, FTB Quests, Create como soft dependencies
- Simple Voice Chat como dependencia obrigatoria (para voz)
- Java 17 (padrao do Forge 1.20.1)

### RNF03 - Configurabilidade

- Backend de IA configuravel (Ollama local / API remota)
- URL e modelo do Ollama configuraveis
- API key para servicos remotos configuravel
- Voz pode ser desativada (fallback para chat texto)
- Distancia de percepcao configuravel

### RNF04 - Escalabilidade Futura

- Arquitetura preparada para multiplas Alices no futuro
- Preparada para adicionar idiomas (troca de voz no Edge TTS + modelo Whisper multilingue)
- Preparada para troca de voz
- MVP: 1 Alice por servidor, pt_BR, 1 voz

---

## 6. Stack Tecnica Proposta

### 6.1 Dependencias do Mod

| Biblioteca | Uso | Maven/Gradle | Obrigatoria? |
|-----------|-----|--------------|-------------|
| Forge MDK 1.20.1 | Base do mod | ForgeGradle | Sim |
| Simple Voice Chat API | Voz (captura/transmissao de audio) | ModRepo Maven | Sim (para voz) |
| ollama4j | Comunicacao com Ollama | Maven Central `io.github.ollama4j:ollama4j:1.1.6` | Sim |
| LangChain4j (alternativa) | Framework LLM + RAG | Maven Central `dev.langchain4j:langchain4j-ollama` | Alternativa ao ollama4j |
| JEI API | Receitas ricas | CurseForge Maven | Soft dependency |
| FTB Quests API | Quests awareness | CurseForge Maven | Soft dependency |
| Baritone API | Pathfinding | Baritone Maven | Sim (decisao #22) — funciona nativo com FakePlayer |
| JGit | Operacoes Git (memoria) | Maven Central `org.eclipse.jgit:org.eclipse.jgit` | Sim (para memoria Git) |

### 6.2 Servicos Externos (rodam separadamente)

| Servico | Uso | Onde roda | Requisito |
|---------|-----|-----------|-----------|
| Ollama | LLM local | Machine 3 (192.168.0.200) | CPU disponivel, modelos instalados |
| faster-whisper | STT REST local | Machine 2 (192.168.0.225) | Docker stack alice-whisper (Portainer) |
| Edge TTS | TTS neural feminino | Cloud Microsoft | Internet estavel no servidor |
| API Remota (fallback LLM) | LLM na nuvem se local lento | Cloud | Internet + API key |
| GitHub | Memoria Git da Alice | Cloud GitHub | Conta GitHub + repo publico + SSH key |

### 6.3 Assets a incluir no mod

| Asset | Formato | Descricao |
|-------|---------|-----------|
| Skin da Alice (Scarlet) | PNG 64x64 | Skin feminina ruiva — funciona out-of-the-box no FakePlayer |
| Schematics | .nbt | Estruturas pre-definidas para construcao |
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

| Responsabilidade | Camada | Latencia | Justificativa |
|-----------------|--------|----------|---------------|
| Fugir se vida < 20% | Regra | <1ms | Seguranca — prioridade absoluta |
| Comer quando com fome | Regra | <1ms | Reacao automatica |
| Equipar melhor item | Regra | <1ms | Comparacao de stats |
| Combate reativo (atacar, esquivar) | Regra/Script | <50ms | Resposta imediata |
| Pathfinding / navegacao | Script (Baritone) | <50ms | Algoritmos prontos |
| Mineracao / coleta (execucao) | Script | <50ms | Acao repetitiva |
| Crafting (executar receita) | Script | <50ms | Lookup em RecipeManager |
| Construcao (colocar blocos) | Script | <50ms | Seguir schematic |
| Conversao voz->texto (STT) | Script (faster-whisper REST) | ~300ms | Machine 2 (Docker), rede local |
| Conversao texto->voz (TTS) | Script (Edge TTS HTTP) | ~300ms | Cloud Microsoft |
| Operacoes Git (memoria) | Script (JGit) | ~1s | Thread separada |
| **O QUE minerar/coletar** | **Agente Survive** | <3s | Requer contexto e objetivo |
| **O QUE craftar/construir** | **Agente Build** | <3s | Depende de planejamento |
| **Avaliacao de ameaca (lutar vs fugir)** | **Agente Combate** | <3s | Analise de situacao complexa |
| **Conversa no chat/voz** | **Agente Chat** | <3s | Linguagem natural |
| **Priorizacao de tarefas** | **Orquestrador** | <3s | Planejamento |
| **Interpretar comandos do jogador** | **Orquestrador** | <3s | NLP (texto ou voz) |
| **Sugestoes de quests/proximos passos** | **Agente Quest** | <3s | Raciocinio sobre progresso |
| **Orientacao sobre mecanicas/mods** | **Agente Chat** | <3s | Base de conhecimento + RAG |
| **Criar nova skill** | **Orquestrador** | <5s | Auto-aprendizado |
| **Lembrancas/memoria de aventuras** | **Agente Chat** | <3s | Narrativa + Git repo |

---

## 8. Pipeline de Comunicacao por Voz (Detalhado)

### 8.1 Jogador -> Alice (STT Pipeline) — decisao #48

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

### 8.3 Radio de Comunicacao

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

- ~~Q01 - Versao do Minecraft~~ -> 1.20.1 Forge (decisao #1)
- ~~Q02 - Backend de IA~~ -> Ollama local + API remota (decisao #5)
- ~~Q03 - Tipo de entidade~~ -> **FakePlayer (ServerPlayer)** — decisao #21 REVISADA, #46 confirmado
- ~~Q04 - Pathfinding~~ -> Baritone — funciona nativamente com FakePlayer, zero adapter (decisao #22)
- ~~Q05 - Multiplayer / quantidade~~ -> 1 Alice no MVP (decisao #6)
- ~~Spawn~~ -> Ovo proprio (decisao #7)
- ~~Morte~~ -> Nao perde itens, respawn em cama ou ponto do ovo (decisoes #8, #9, #10)
- ~~Voz~~ -> **TTS Edge TTS FranciscaNeural + STT faster-whisper** + Simple Voice Chat (decisoes #47, #48)
- ~~Idioma~~ -> pt_BR no MVP (decisao #19)
- ~~Q06 - Faseamento~~ -> Projeto inteiro definido e escopado, faseamento aceito (decisao #23)
- ~~Q13 - Simple Voice Chat~~ -> Sim, Cursed Walking ja inclui SVC (decisao #30)
- ~~Q20 - Provedor Git~~ -> GitHub publico (decisao #31). Pendencia: criar repo quando iniciar projeto
- ~~Q22 - Modelo LLM~~ -> Hardware limitado, 1 modelo unico e lento (decisao #32)
- ~~Q08 - LLM library~~ -> ollama4j no MVP, interface abstrata para trocar (decisao #33)
- ~~Q24 - Skin~~ -> "Scarlet (Remake)" do Planet Minecraft (decisao #34)
- ~~Q11 - Radio~~ -> Usar Simple Voice Chat diretamente, sem item radio proprio (decisao #35)
- ~~Q14 - Alice offline~~ -> Fica no mundo como player offline, inativa (decisao #36)
- ~~Q10 - Schematics~~ -> Sem padrao, .nbt, so pre-carregados, LLM sugere jogador decide (decisao #37)
- ~~Q15 - Sistema de ameaca~~ -> Fatores toggleaveis com peso, limiar % configuravel, PCE definido (decisoes #38-#39)
- ~~Q16 - Animacoes~~ -> Padrao do player + waypoints Map Atlases (decisao #40)
- ~~Q17 - Redstone/Create~~ -> Desejavel, depende da decisao final sobre entidade (decisao #41)
- ~~Q18 - Multi-jogador~~ -> Amigo (ovo) > Conhecido (conversa) > Hostil (se atacar) (decisao #42)
- ~~Q19 - Tamanho mod~~ -> Sem restricao (decisao #43)
- ~~Q21 - Base de conhecimento~~ -> Opcao C hibrida, certeza por experiencia/confirmacao (decisao #44)
- ~~Q11 - Radio~~ -> Sem radio proprio, usar Simple Voice Chat diretamente (decisao #35)
- ~~Q14 - Alice offline~~ -> Fica no mundo como player offline, inativa (decisao #36)
- ~~Q12 - Voz feminina pt_BR~~ -> Edge TTS pt-BR-FranciscaNeural (gratuito, ilimitado, cloud Microsoft) (decisao #47)
- ~~STT upgrade~~ -> faster-whisper Machine 2 Docker, REST OpenAI-compatible, confirmado rodando (decisao #48)
- ~~Q23 - Baritone + entidade~~ -> Resolvido com FakePlayer: Baritone funciona nativo, zero adapter (decisao #46)
- ~~Q17 - Redstone/Create~~ -> CONFIRMADO: FakePlayer interage com tudo nativamente (decisao #41 atualizado)

### DETALHAMENTO DAS DECISOES RESOLVIDAS

#### Q03 - FakePlayer (decisao #21 REVISADA → decisao #46)

- **Decisao REVISADA:** FakePlayer (ServerPlayer) via `net.minecraftforge.common.util.FakePlayer`
- **Motivo da revisao:** Baritone requer player. FakePlayer herda inventario, morte, skin, Create, SVC. Menor complexidade total.
- **O que mudou:** Nao e mais Custom Mob. Alice e um ServerPlayer ficticio.
- **Aparencia:** Skin Scarlet (decisao #34) funciona out-of-the-box no FakePlayer (skin padrao de player)
- **Aparece na TAB:** Sim, como jogador normal
- **Referencia:** Duzo's Alice, Automatone, ChatClef

#### Q04 - Baritone (decisao #22) — DESAFIO ELIMINADO com FakePlayer

- **Decisao:** Usar Baritone como dependencia de pathfinding
- **Justificativa:** Poderoso, maduro (8.8k stars), A* rapido, mine/follow/build embutidos, Forge 1.20.1
- **Status com FakePlayer:** Baritone controla ServerPlayer nativamente. ZERO adapter necessario. O desafio tecnico original (Q23) foi eliminado pela mudanca para FakePlayer.
- **Integracao:** Baritone recebe referencia para o FakePlayer e o controla exatamente como controlaria um jogador real.

#### Q06 - Faseamento Completo (decisao #23)

- **Decisao:** Projeto inteiro definido e escopado ate o fim, implementado em fases
- **Faseamento aprovado (atualizado com FakePlayer + Edge TTS):**
  - **Fase 1 - Fundacao:** FakePlayer ruiva + Baritone (follow/stay) + combate basico + chat texto com LLM
  - **Fase 2 - Utilidade:** Crafting + inventario + containers + auto-equip
  - **Fase 3 - Voz:** TTS (Edge TTS FranciscaNeural) + STT (faster-whisper) + Simple Voice Chat
  - **Fase 4 - Construcao:** Schematics + construcao bloco a bloco + contraptions Create (FakePlayer interage nativo)
  - **Fase 5 - Guia:** Base de conhecimento compilada + orientacao por fase de jogo + quests
  - **Fase 6 - Inteligencia:** Agentes + skills + memoria Git + autonomia avancada + aprendizado
- **Nota:** Todas as fases devem estar BEM DEFINIDAS e ESCOPADAS antes de comecar a implementacao.

### PENDENTES - Arquitetura e Implementacao

#### Q07 - Persistencia Tecnica da Memoria (parcialmente resolvido)
- **Decisao parcial:** Dual storage — Forge SavedData (local, rapido) + Git repo (portavel, persistente)
- **SavedData** para: mapa de containers, estado atual, dados de tick (precisa ser rapido)
- **Git repo** para: memorias de aventuras, skills, base de conhecimento, preferencias
- **Perguntas restantes:**
  - Quanto de memoria de aventuras manter no contexto do LLM? (ultimas 50? 100?)
  - Como resumir memorias antigas para nao estourar contexto do LLM?
  - Usar RAG para buscar memorias relevantes? (provavel que sim — alinhado com skills/agentes)
  - Qual provedor Git usar? (GitHub? GitLab? Gitea self-hosted?)
  - Frequencia de commits: a cada evento significativo? A cada N minutos?
  - **Pergunta:** Prefere GitHub publico ou quer hospedar um Gitea no mesmo servidor Docker?

#### Q08 - ollama4j vs LangChain4j vs HTTP direto (analise detalhada)

##### Opcao A: ollama4j (v1.1.4+)

- **Tamanho:** Leve (~500KB + dependencias minimas)
- **Escopo:** Wrapper especifico para Ollama REST API
- **Funcionalidades:**
  - Chat/generate com streaming
  - Modelos: list, pull, delete, copy
  - Vision/image support
  - **Tool/function calling** (suporta MCP tools!) — desde v1.1.x
  - Embeddings generation (para busca semantica de skills)
  - Metricas Prometheus (beta)
- **Codigo exemplo:**
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
  - Leve — nao infla o mod
  - Tool calling JA FUNCIONA (nao precisa implementar na mao)
  - Embeddings JA FUNCIONA (busca semantica de skills)
  - Maven: `io.github.ollama4j:ollama4j:1.1.6`
- **Contras:**
  - So funciona com Ollama (sem fallback direto para OpenAI/Anthropic)
  - RAG precisa ser implementado manualmente
  - Memory management precisa ser implementado manualmente
  - Multi-provider requer codigo custom para fallback API remota

##### Opcao B: LangChain4j

- **Tamanho:** Pesado (~15-20MB com dependencias transitivas)
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
- **Codigo exemplo:**
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
  - RAG pronto (nao precisa implementar)
  - Memory management pronto
  - Fallback para API remota trivial (troca provider)
  - Tool calling com @Tool annotation (elegante)
  - MCP support
  - Comunidade grande (17k+ stars)
  - Documentacao excelente
- **Contras:**
  - **PESADO** — 15-20MB de dependencias
  - Complexidade — muita coisa que Alice nao precisa
  - Possivel conflito com classloading do Forge (dependencias transitivas)
  - Over-engineering para MVP?

##### Opcao C: HTTP direto (OkHttp/Java HttpClient)

- **Tamanho:** Zero dependencia adicional (Java HttpClient nativo)
- **Funcionalidades:** So o que voce implementar
- **Pros:** Controle total, zero overhead, zero conflito
- **Contras:** Reimplementar TUDO (chat, streaming, tool calling, RAG, memory)

##### Analise Considerando Decisoes Tomadas

| Funcionalidade necessaria | ollama4j | LangChain4j | HTTP direto |
|--------------------------|----------|-------------|-------------|
| Chat basico | Sim | Sim | Implementar |
| Streaming | Sim | Sim | Implementar |
| Tool calling (agentes) | Sim (v1.1+) | Sim (nativo) | Implementar |
| Embeddings (skills RAG) | Sim | Sim (+ vector store) | Implementar |
| Chat memory | Manual | Nativo | Implementar |
| RAG (base conhecimento) | Manual | Nativo | Implementar |
| Multi-provider (fallback) | Manual | Nativo | Implementar |
| Peso no mod | ~500KB | ~15-20MB | 0 |
| Risco de conflito Forge | Baixo | Medio-Alto | Zero |

##### Contexto critico: Hardware limitado (decisao #32)

- O usuario confirmou que so roda 1 modelo LLM e ele e LENTO
- Isso muda a equacao:
  - Multi-provider do LangChain4j perde valor (nao vai rodar 2 modelos)
  - RAG via embeddings pode ser pesado demais (requer modelo de embedding rodando)
  - **Alternativa para RAG:** Em vez de embeddings via LLM, usar busca por keywords/tags (mais rapido, sem modelo extra)
  - Cache e pre-computacao sao MAIS importantes que features de framework

##### Recomendacao Tecnica

**Para o MVP: ollama4j** — mais leve, tool calling ja funciona, embeddings disponiveis se quiser, nao infla o mod, baixo risco de conflito com Forge.

**Para Fase 6 (Inteligencia): Reavaliar** — se precisar de RAG sofisticado e multi-provider, migrar para LangChain4j. A interface pode ser abstraida agora para facilitar troca depois.

**Proposta de implementacao:**
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

### PENDENTES - Conteudo e Game Design

#### Q09 - Compatibilidade com Mods do Cursed Walking
- Alice precisa ter scripts especificos para armas de fogo do modpack?
- Precisa entender mecanicas especificas (infeccao zumbi, blood moon)?
- Isso vem via scripts especificos por mod ou o LLM interpreta genericamente?
- **Acao:** Extrair lista completa dos 201 mods via acesso SSH ao servidor (pasta mods/)
- **Status:** Pendente — aguardando acesso SSH do usuario

#### Q10 - Schematics Pre-carregados
- Quais estruturas incluir no mod? (abrigo simples? torre de vigia? base completa?)
- Formato confirmado: .nbt (compativel com Create e Minecraft nativo)
- Alice pode criar schematics novos ou so usa os pre-carregados?
- Como Alice escolhe qual schematic construir? (LLM decide? jogador pede?)
- **Pergunta:** Quais estruturas sao prioritarias para o contexto de apocalipse zumbi?

#### Q11 - RESOLVIDO: Radio substituido pelo Simple Voice Chat (decisao #35)

- **Decisao:** NAO ter item radio proprio — usar Simple Voice Chat diretamente
- **Motivo:** SVC ja esta no modpack Cursed Walking com todas as funcionalidades necessarias
- **O que isso muda:**
  - Proximidade de voz: nativo do SVC (configura range)
  - Comunicacao a distancia: jogador usa o sistema de grupos/canais do SVC
  - O Walkie-Talkie mod (tambem no modpack) ja implementa a mecanica de radio com item
  - Alice usa a API do SVC para falar/ouvir — nao precisa de item proprio
- **Conclusao:** Zero esforco de crafting de radio. Alice "fala" via SVC API como entidade de voz, ou como player (se FakePlayer). Walkie-Talkie mod pode ser referencia direta de como fazer radio com item se necessario no futuro.

#### Q12 - RESOLVIDO: Edge TTS pt-BR-FranciscaNeural (decisao #47) + faster-whisper (decisao #48)

- **TTS:** Microsoft Edge TTS — voz `pt-BR-FranciscaNeural` (feminina, neural, excelente qualidade)
  - Gratuito, sem API key, sem cadastro, sem limite documentado
  - Mesmo motor do Windows Narrator e Edge browser
  - Requer internet no servidor (disponivel e estavel, confirmado)
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

#### Q13 - RESOLVIDO: Simple Voice Chat Confirmado (decisao #30)

- **Resposta:** SIM, Cursed Walking ja inclui Simple Voice Chat
- Confirmado via extracao da lista de mods do CurseForge (2026-04-03)
- Tambem inclui Walkie-Talkie mod (referencia direta para radio da Alice)
- Tambem inclui Sound Physics Remastered (pode afetar propagacao de audio)
- **Conclusao:** Nao precisa pedir instalacao adicional. SVC ja esta no modpack.

#### Q14 - RESOLVIDO: Alice offline = player offline (decisao #36)

- **Decisao:** Quando o jogador desconecta, Alice fica no mundo como um player offline
- **Comportamento:**
  - Permanece fisicamente no mundo (nao desaparece)
  - Fica inativa — sem IA, sem decisoes, sem movimento
  - Continua vulneravel a mobs (igual a um player offline que fica no servidor)
  - Se morrer enquanto offline, respawna normalmente quando jogador voltar
- **Justificativa:** E o comportamento mais natural e consistente com a lore — Alice existe no mundo, nao some quando o jogador fecha o jogo
- **Nota tecnica:** Se for FakePlayer, este comportamento e nativo. Se for Custom Mob, precisa ser implementado (impedir despawn, desligar AI Goals)

#### Q15 - RESOLVIDO: Sistema de Ameaca + Padrao de Comunicacao (decisoes #38-#39)

**a) Fatores do sistema de ameaca (decisao #38)**

Cada fator tem peso configuravel (0.0 a 1.0). Pode ser ligado/desligado. Limiar de fuga em % configuravel.

| Fator | Exemplo de config | Toggleavel |
|-------|------------------|------------|
| Quantidade de mobs hostis proximos | count * 0.1 por mob | Sim |
| Tipo de mob (mutante > normal) | mutante=0.3, normal=0.1 | Sim |
| Vida atual da Alice | < 30% vida = +0.3 | Sim |
| Vida atual do jogador | < 30% vida = +0.2 | Sim |
| Armamento disponivel (Alice) | sem arma = +0.2 | Sim |
| Armamento disponivel (jogador) | sem arma = +0.2 | Sim |
| Hora do dia (noite = mais perigoso) | noite = +0.15 | Sim |
| Blood Moon ativo | blood_moon = +0.4 | Sim |
| Distancia da base segura | longe = +0.1 | Sim |

Limiar de fuga: configuravel em % (ex: fuga se ameaca >= 70%)

**b) Padrao de Comunicacao Eficiente — PCE (decisao #39)**

Este e um padrao de comportamento para TODAS as situacoes de perigo ou desafio, nao so ameacas.

```
PROTOCOLO PCE - Comunicacao em Situacao de Perigo

1. Alice AVISA + PEDE CONFIRMACAO
   "Perigo! [descricao breve]. Fugir agora? [sim/nao]"

2. Aguarda 3-5 segundos sem resposta

3. REPETE O AVISO + PEDE CONFIRMACAO novamente
   "Perigo ainda presente! Fugir agora? [sim/nao]"

4. Continua avisando a cada 3-5 segundos enquanto perigo persistir

CONFIRMACOES ACEITAS:
- Jogador diz "sim" / "pode" / "vamos" / qualquer confirmacao
- Jogador REPETE O QUE ALICE FALOU (ex: "fugir agora") = confirmacao
- Jogador diz "repete" / "de novo" -> Alice repete e pede confirmacao novamente

CANCELAMENTOS:
- Jogador diz "nao" / "fica" / "aguenta"
- Alice para de avisar ate situacao mudar

PRINCIPIOS DO PCE:
- Mensagens CURTAS e DIRETAS (max 1 frase de aviso + pergunta)
- Informacao critica PRIMEIRO, contexto DEPOIS
- Confirmacao simples, sem verbosidade
- Nao bloquear o jogador esperando resposta — ele pode estar lutando
- Usar voz (TTS) E chat simultaneamente em situacoes criticas

APLICAR PCE TAMBEM EM:
- Sugestoes urgentes de crafting antes de ameaca
- Waypoints e alertas de localizacao ("base a X blocos, ir agora?")
- Alertas de recurso critico ("sem municao, reabastecer agora?")
- Qualquer decisao que precisa de resposta rapida
```

#### Q16 - RESOLVIDO: Animacoes padrao + Waypoints (decisao #40)

- Animacoes: padrao de player (sem animacoes especiais no MVP)
- Para apontar/mostrar coisas: Alice cria waypoints via **Map Atlases mod** com nome especifico
- Comunicacao de waypoint segue o PCE: "Marquei [Nome] no mapa. Ver agora?"
- Ex: "Marquei 'Perigo-Horda-Norte' no mapa. Fuja para sul!"

#### Q17 - RESOLVIDO: Redstone/Create desejavel, depende da decisao #21 (decisao #41)

- **Decisao do usuario:** E desejavel que Alice possa interagir com redstone e Create
- **Implicacao critica:** Esta e uma das razoes para rever a decisao #21 (Custom Mob vs FakePlayer)
  - **Custom Mob:** Mobs nao pressionam botoes, alavancas, pedais Create nativamente — precisa implementar tudo do zero com codigo especifico por bloco
  - **FakePlayer:** Interage com qualquer bloco exatamente como um jogador — redstone, Create contraptions, crafting tables, tudo funciona out-of-the-box
- **Conclusao:** Redstone/Create e mais um ponto a favor de FakePlayer (ver Q23 comparacao)
- **MVP:** Redstone e Create nao precisam estar na Fase 1 — mas a arquitetura (Custom Mob vs FakePlayer) deve ser decidida agora, pois mudar depois e caro

#### Q18 - RESOLVIDO: Hierarquia social Amigo > Conhecido > Hostil (decisao #42)

- **Decisao:** Sistema de 3 niveis de relacao social

| Nivel | Quem e | Alice se comporta como |
|-------|--------|----------------------|
| **Amigo** | Dono do ovo (quem spawnou Alice) | Nunca hostil, aceita todas as ordens, maxima confianca |
| **Conhecido** | Qualquer outro jogador no servidor | So conversa, NAO executa ordens, pode ajudar em combate |
| **Hostil** | Quem atacar Alice | Alice ataca de volta ate o agressor parar |

- **Regras de transicao:**
  - Jogador novo encontra Alice = automaticamente "Conhecido"
  - Jogador ataca Alice = vira "Hostil" ate parar de atacar
  - Jogador Hostil que para de atacar = volta a "Conhecido" apos cooldown
  - So o Amigo (dono) pode dar comandos: "vamos combinar assim...", "fica aqui", "me segue"
- **MVP:** 1 Amigo por Alice (quem colocou o ovo). Em futuras versoes: lista de Amigos configuravel

#### Q19 - RESOLVIDO: Tamanho do Mod (decisao #43)

- **Decisao:** Sem restricao de tamanho para o mod
- **Tamanho estimado atual (com decisoes #47 e #48):**
  - Sem modelos de voz locais — Edge TTS e faster-whisper rodam fora do mod
  - Apenas skin PNG + schematics .nbt + system prompt .txt
  - Tamanho estimado: **< 5MB** (reduzido drasticamente vs estimativa original de 100-150MB)
- **Beneficio das decisoes de voz cloud/REST:** Zero modelos bundled no .jar

### NOVAS QUESTOES (adicionadas v0.4)

#### Q20 - RESOLVIDO: GitHub Publico (decisao #31)

- **Decisao:** GitHub publico
- **Pendencia de projeto:** Criar o repositorio GitHub quando iniciar a implementacao
- Vantagens: gratuito, API madura, GitHub Actions para automacao de merges, comunidade pode contribuir
- Nome sugerido do repo: `alice-memory`

#### Q21 - Base de Conhecimento: O que e, por que e como

**O que e "base de conhecimento"?**

Quando voce conversa com um LLM (tipo ChatGPT ou Ollama), ele so sabe o que foi treinado. Ele NAO sabe que no Cursed Walking tem infeccao zumbi, que a Timeless and Classics tem uma M4A1, ou que a melhor defesa contra horda e uma parede de 3 blocos com seteiras.

Para Alice ser util, ela precisa SABER essas coisas. A "base de conhecimento" e um conjunto de textos que dizemos para o LLM ler ANTES de responder. E como dar um livro para alguem ler antes de uma prova.

**Na pratica, funciona assim:**

1. Jogador pergunta: "Alice, como eu faco pra nao pegar infeccao de zumbi?"
2. O mod Alice busca na base de conhecimento: "infeccao zumbi" -> encontra o arquivo `zombie-infection.md`
3. Esse arquivo e enviado junto com a pergunta pro LLM: "Aqui esta informacao sobre infeccao zumbi: [conteudo do arquivo]. Agora responda a pergunta do jogador."
4. LLM responde com informacao CORRETA porque leu o texto certo.

Sem a base de conhecimento, o LLM ia inventar qualquer coisa (alucinacao).

**Quanto conteudo precisa?**

NAO precisamos documentar os 201 mods. Precisamos documentar o que Alice precisa FALAR pro jogador. Baseado no catalogo, sao ~25 mods criticos. Para cada um, precisamos de algo tipo:

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

**De onde tirar essa informacao:**

| Fonte | O que da pra tirar | Como |
|-------|-------------------|------|
| **CurseForge** (pagina do mod) | Descricao basica, features, changelog | Acessar via curl |
| **Wiki do mod** (se existir) | Receitas, mecanicas detalhadas, estrategias | Links nas paginas dos mods |
| **YouTube** (videos de gameplay/tutorial) | Demonstracoes praticas, dicas | Transcrever via IA |
| **JEI (in-game)** | Receitas de TODOS os mods automaticamente | Alice ja acessa via RecipeManager API — NAO precisa estar na base |
| **Jogar o jogo** | Experiencia pratica | Alice aprende jogando (memoria Git) |

**Sobre receitas e JEI:**

Receitas Alice NAO precisa na base de conhecimento! O mod JEI (que esta no modpack) e o RecipeManager do Minecraft ja dao acesso programatico a TODAS as receitas de TODOS os mods. Quando o jogador perguntar "como crafta uma M4?", Alice consulta o RecipeManager em tempo real — nao precisa ter escrito antes.

O que Alice precisa na base e: **estrategia, contexto e dicas** — coisas que nao estao em nenhuma API.

**Opcoes de como compilar:**

- **Opcao A: Eu (IA) compilo para voce**
  - Eu acesso as paginas dos ~25 mods criticos no CurseForge
  - Busco wikis, documentacao, videos do YouTube
  - Escrevo os arquivos markdown da base de conhecimento
  - Voce revisa e corrige o que estiver errado
  - **Pros:** Rapido, voce nao precisa escrever nada
  - **Contras:** Posso errar informacoes que so quem joga sabe

- **Opcao B: Voce joga e eu documento**
  - Voce joga o Cursed Walking normalmente
  - Me conta o que descobriu, o que funciona, o que nao funciona
  - Eu vou escrevendo os arquivos de conhecimento baseado no que voce relata
  - **Pros:** Informacao 100% pratica e testada
  - **Contras:** Lento, depende de voce jogar bastante

- **Opcao C: Hibrido (RECOMENDADO)**
  - Eu compilo a base inicial pesquisando na internet (Opcao A)
  - Voce revisa e corrige o que esta errado
  - Conforme voce joga, eu atualizo com informacao pratica (Opcao B)
  - Alice tambem atualiza conforme joga (memoria Git, Fase 6)
  - **Pros:** Rapido para comecar, melhora com o tempo
  - **Contras:** Nenhum relevante

- **Opcao D: Alice aprende sozinha (so na Fase 6)**
  - Nao compilar nada agora
  - Alice comeca "burra" e vai aprendendo jogando
  - Usa so o que o LLM ja sabe de treino
  - **Pros:** Zero trabalho agora
  - **Contras:** Alice vai dar informacao errada no inicio, experiencia ruim

**Pergunta:** Opcao C faz sentido? Posso comecar a compilar a base dos ~25 mods criticos agora?

#### Q22 - RESOLVIDO: Modelo Unico, Hardware Limitado (decisao #32)

- **Resposta do usuario:** So roda 1 modelo e ate hoje nunca conseguiu rodar bem — sempre lento
- **Decisao:** Modelo unico. Otimizar para funcionar com hardware limitado.
- **Implicacoes criticas para a arquitetura:**
  - NAO usar multi-model (Mindcraft usa 4, mas requer GPU potente)
  - Agentes especializados compartilham o MESMO modelo, diferenciados por system prompt
  - Embeddings para RAG podem ser pesados demais — considerar busca por keywords/tags
  - **Pre-cache** e skills compiladas (regras sem LLM) sao ESSENCIAIS
  - Minimizar chamadas ao LLM — tudo que pode ser script/regra, DEVE ser
  - Considerar modelo menor e mais rapido (ex: phi-3, gemma-2b) em vez de modelo grande lento
  - Streaming de resposta + TTS incremental para perceber latencia menor
  - **Fallback para API remota** ganha importancia (quando LLM local e muito lento)
- **Hardware detectado (2026-04-03):**
  - CPU: AMD Ryzen 5 4500 (6 cores / 12 threads)
  - RAM: 16 GB DDR4
  - GPU: NVIDIA GTX 1050 Ti — **4 GB VRAM** (GARGALO)
  - Ollama: v0.19.0
- **Modelos instalados:** mistral:latest (4.4GB local), glm-5/minimax/deepseek-v3 (cloud)
- **Diagnostico:** Mistral 7B (4.4GB) NAO cabe inteiro nos 4GB VRAM. Roda hibrido GPU+CPU = LENTO.
- **Modelos recomendados para 4GB VRAM:**
  - `llama3.2:3b` (~2.0 GB) — bom equilibrio qualidade/velocidade
  - `qwen2.5:3b` (~2.0 GB) — forte em raciocinio
  - `phi3:mini` (~2.3 GB) — boa qualidade geral
  - `gemma2:2b` (~1.6 GB) — ultra-leve, mais rapido
  - `mistral:7b-q4_0` (~3.8 GB) — cabe justo, mais capaz mas no limite
- **Estrategia recomendada:**
  - Modelo local: `llama3.2:3b` ou `qwen2.5:3b` (cabe na GPU, 1-3s por resposta)
  - Fallback cloud: deepseek-v3/glm-5 via Ollama cloud (para perguntas complexas)
  - Skills compiladas e regras sem LLM para tudo que puder (latencia zero)
- **Proxima acao:** Testar `llama3.2:3b` e `qwen2.5:3b` e comparar latencia/qualidade em pt_BR

#### Q23 - Baritone + Custom Mob: Como vai funcionar na pratica

**O problema explicado de forma simples:**

Tomamos duas decisoes que podem conflitar entre si:
- **Decisao #21:** Alice e um Custom Mob (PathfinderMob) — uma entidade tipo mob, nao um jogador
- **Decisao #22:** Usaremos Baritone para pathfinding — o melhor sistema de navegacao

O conflito: **Baritone foi feito para controlar JOGADORES, nao mobs.** Quando voce instala Baritone no seu Minecraft, ele controla o SEU personagem. Ele sabe ler o inventario do JOGADOR, mover as pernas do JOGADOR, minerar com as maos do JOGADOR.

Alice nao e um jogador — ela e um mob customizado. Baritone nunca foi projetado para mover um mob.

**Analogia:** E como tentar usar o volante de um carro (Baritone) para pilotar um drone (Alice). O volante e excelente, mas precisa de um adaptador para funcionar com um veiculo diferente.

**Opcoes tecnicas (do mais simples ao mais complexo):**

- **Opcao A: Pathfinding vanilla + Baritone parcial**
  - Usar o pathfinding nativo do Minecraft (que JA funciona com mobs) para movimentacao basica (andar, seguir, fugir)
  - Usar Baritone APENAS para tarefas avancadas (minerar, construir) criando um contexto fake de player so para essas operacoes
  - **Pros:** Mais simples, menos risco de bugs
  - **Contras:** Dois sistemas de navegacao, pode ter inconsistencias

- **Opcao B: Criar um "adaptador" para Baritone**
  - Escrever um codigo que "traduz" os comandos do Baritone (feitos para player) em movimentos de mob
  - O Baritone calcula o caminho, nosso adaptador move a Alice por esse caminho
  - **Pros:** Usa toda a inteligencia do Baritone (A*, desvio de obstaculos, etc.)
  - **Contras:** Precisa implementar o adaptador, manter quando Baritone atualizar

- **Opcao C: Fazer Alice ser um "fake player" internamente**
  - Criar um player invisivel associado a Alice que o Baritone controla
  - A entidade visual (o mob ruivo) segue esse player invisivel
  - **Pros:** Baritone funciona 100% sem modificacao
  - **Contras:** Complexo, pode causar bugs estranhos, consome mais recursos

**Isso nos levou a rever a decisao #21 inteira.** Antes de resolver como adaptar Baritone para mob, vale perguntar: **e se Alice fosse um jogador de verdade?**

---

### Q23b - REVISAO DA DECISAO #21: Custom Mob vs FakePlayer/ServerPlayer

**O que e um FakePlayer?**

`net.minecraftforge.common.util.FakePlayer` e uma classe do proprio Forge que cria um ServerPlayer ficticio — um jogador real no servidor, mas sem conexao de cliente real. E o padrao estabelecido no ecossistema Forge para entidades que precisam agir como jogadores.

Usado por mods conhecidos: Industrial Craft 2 (drones), Create (contraptions), BuildCraft (robots), e varios companheiros IA.

**Comparacao completa para o Projeto Alice:**

| Criterio | Custom Mob (PathfinderMob) | FakePlayer (ServerPlayer) |
|----------|--------------------------|--------------------------|
| **Baritone (pathfinding)** | NAO funciona — precisa adapter complexo | FUNCIONA nativo — Baritone foi feito para players |
| **Simple Voice Chat** | Precisa implementacao especial como entidade de voz | Funciona como player normal — integracao trivial |
| **Create (mecanismos)** | Limitado — mobs nao operam blocos Create | Pleno — opera levers, botoes, Create blocks igual jogador |
| **Redstone** | Limitado — mobs nao ativam redstone por interacao | Pleno — pressiona botoes, alavancas, pedais |
| **Crafting table** | Nao usa — RecipeManager apenas programatico | Usa fisicamente igual jogador (tambem RecipeManager) |
| **Inventario** | Custom implementation necessaria | Inventario 36+4+1 slots nativo do Forge |
| **Animacoes** | Custom AnimationController necessario | Animacoes padrao de player — gratuitas |
| **Skin Scarlet (decisao #34)** | Custom PlayerModel renderer no mob | Skin padrao de player — funciona out-of-the-box |
| **Morte e respawn** | Custom death/respawn logic necessaria | Logica nativa de player herdada automaticamente |
| **Lootr (loot instanciado)** | Mob pode nao receber loot de player | Recebe loot proprio como qualquer jogador real |
| **FTB Teams** | Nao pode ser membro de equipe real | Pode ser adicionado como membro normalmente |
| **TAB list (lista de jogadores)** | Nao aparece | Aparece como jogador — pode confundir ou ser feature |
| **Journeymap / mapas** | Aparece como entidade | Aparece como jogador no mapa |
| **Anti-cheat mods** | Sem risco (e mob) | Risco BAIXO — FakePlayer tem GameProfile marcado como fake, maioria dos mods respeita |
| **Auth mods** | Sem conflito | Risco BAIXO — FakePlayer nao passa por login/auth real |
| **Complexidade de spawn** | Simples (EntityType.spawn) | Media — precisa criar GameProfile, UUID, handle join event |
| **Complexidade total MVP** | MAIOR — precisa implementar tudo do zero | MENOR — herda toda logica de player do Forge |
| **Estabilidade** | Alta — mob e simples | Alta — FakePlayer e padrao maduro e bem testado |
| **Projetos de referencia** | HumanCompanions, SiliconeDolls | Duzo's Alice, Automatone, ChatClef, Minecraft bots (mineflayer-style) |

**Analise aplicada ao Projeto Alice:**

Com as informacoes que temos hoje, FakePlayer resolve ou simplifica TODOS os maiores desafios tecnicos:

- **Baritone (decisao #22):** Zero adapter necessario. Baritone controla FakePlayer nativo.
- **Create/Redstone (Q17):** De graca. FakePlayer interage com tudo.
- **Simple Voice Chat (decisao #30):** De graca. SVC trata FakePlayer como player real.
- **Skin Scarlet (decisao #34):** De graca. Skin de player funciona out-of-the-box.
- **Lootr no modpack:** De graca. FakePlayer recebe loot instanciado.
- **Inventario, morte, respawn:** De graca. Logica nativa do Forge.

O Custom Mob exigiria implementar manualmente tudo isso do zero. O FakePlayer herda tudo gratuitamente.

**Risco real do FakePlayer:**

O risco de conflito com mods de autenticacao e BAIXO porque:
1. O servidor roda em modo offline (TLauncher — sem verificacao Mojang)
2. FakePlayer tem flag `isFakePlayer() = true` que mods bem escritos checam
3. FakePlayer e padrao documentado do Forge — mods do Cursed Walking (Forge 1.20.1) sao compativeis

**Recomendacao:**

**Trocar a decisao #21 para FakePlayer.** O argumento e simples: FakePlayer resolve o maior problema tecnico (Baritone) e entrega de graca tudo que precisariamos implementar manualmente no Custom Mob. Menor complexidade, mais recursos, mais estabilidade.

**Pergunta final:** Confirma a mudanca da decisao #21 de Custom Mob para FakePlayer?

#### Q24 - RESOLVIDO: Skin "Scarlet (Remake)" (decisao #34)

- **Skin escolhida:** "Scarlet (Remake)" do Planet Minecraft
- **URL:** https://www.planetminecraft.com/skin/scarlet-remake/
- **Formato:** Skin de player padrao 64x64 PNG
- **Implementacao tecnica:** Custom Mob com PlayerModel renderer para usar skin formato player
- **Opcao de render:** Usar HumanoidModel/PlayerModel do Minecraft no Custom Mob (referencia: HumanCompanions faz isso)
- **Asset a incluir no mod:** Arquivo PNG da skin (com credito ao autor original)

---

## 10. Documentacao de Referencia Complementar

Catalogos detalhados na pasta `referencias/`:

- **[catalogo-bibliotecas-e-apis.md](referencias/catalogo-bibliotecas-e-apis.md)** - Detalhamento tecnico completo de todas as bibliotecas, APIs, dependencias com exemplos de codigo, Maven coordinates, formatos, e recomendacoes

- **[catalogo-projetos-referencia.md](referencias/catalogo-projetos-referencia.md)** - Catalogo completo de projetos de referencia com analise de codigo, arquitetura, o que estudar em cada projeto, e mapa de referencia por feature

- **[catalogo-mods-cursed-walking.md](referencias/catalogo-mods-cursed-walking.md)** - Lista completa dos 201 mods do Cursed Walking, categorizados por relevancia para Alice ([CRITICO], [IMPORTANTE], [REFERENCIA], [COMPATIBILIDADE], [INFRAESTRUTURA])

---

## 11. Proximos Passos

### Concluidos
- [x] Definir versao alvo do Minecraft (1.20.1)
- [x] Decidir Forge vs NeoForge (Forge)
- [x] Pesquisar projetos de referencia (TIER 1, 2, 3)
- [x] Catalogar bibliotecas e APIs disponiveis
- [x] Catalogar projetos de referencia com codigo fonte
- [x] Definir identidade e personalidade da Alice
- [x] Definir mecanica de spawn e morte
- [x] Definir sistema de comandos (frase-chave)
- [x] Definir sistema de voz (TTS Edge TTS FranciscaNeural + STT faster-whisper + SVC)
- [x] Definir idioma MVP (pt_BR)
- [x] Pesquisar viabilidade tecnica de voz
- [x] Revisar tipo de entidade: **FakePlayer** (Q03, decisao #21 REVISADA → #46)
- [x] Definir pathfinding: Baritone (Q04, decisao #22)
- [x] Definir faseamento completo do projeto (Q06, decisao #23)
- [x] Definir papel da Alice: guia/engenheira de sobrevivencia (decisao #24-#27)
- [x] Definir sistema de memoria Git (decisao #28)
- [x] Definir arquitetura de agentes e skills (decisao #29)
- [x] Documentar infraestrutura de desenvolvimento (SSH, Docker, Crafty, TLauncher)
- [x] Pesquisar arquiteturas de agent skills (paper arxiv, Spring AI, Voyager)
- [x] Pesquisar memoria Git-native para agentes IA
- [x] Extrair lista completa de mods do Cursed Walking (201 mods, via curl no CurseForge)
- [x] Categorizar mods por relevancia para Alice (CRITICO/IMPORTANTE/REFERENCIA/etc.)
- [x] Confirmar Simple Voice Chat no modpack (Q13, decisao #30)
- [x] Confirmar GitHub como provedor de memoria (Q20, decisao #31)
- [x] Documentar limitacao de hardware LLM (Q22, decisao #32)
- [x] Analise comparativa detalhada ollama4j vs LangChain4j (Q08)
- [x] Pesquisar e listar links de skins para Alice (Q24)
- [x] Pesquisar memoria Git-native para agentes IA

### Em Andamento / Pendentes do Brainstorm

- [x] ~~Q23b — FakePlayer confirmado~~ (decisao #46) ✓
- [x] ~~Q12 — Edge TTS FranciscaNeural + faster-whisper~~ (decisoes #47, #48) ✓
- [ ] **Q07 — Detalhes de persistencia da memoria:** frequencia de commits Git, tamanho de contexto LLM, RAG vs keywords — pode ser resolvido durante implementacao (Fase 6)
- [ ] **Q09 — Compatibilidade com mods Cursed Walking:** scripts especificos por mod ou LLM interpreta genericamente — pode ser resolvido durante Fase 5 (base de conhecimento)
- [ ] **Compilar base de conhecimento para mods CRITICOS (~25 mods)** — tarefa de conteudo, iniciar na Fase 5

### STATUS DO BRAINSTORM

**Todas as decisoes de arquitetura estao tomadas. O brainstorm pode ser considerado CONCLUIDO.**

Decisoes definitivas (48 registradas):
- Plataforma, entidade, pathfinding, LLM, memoria, voz, STT, TTS, mods, infraestrutura — tudo decidido
- Q07 e Q09 sao detalhes que emergerao naturalmente durante a implementacao — nao bloqueiam inicio

**PROXIMO PASSO: Iniciar a implementacao — Fase 1**

### Proximas Etapas - Pre-Implementacao (fazer antes de escrever codigo)

- [ ] Setup ambiente de desenvolvimento: Forge MDK 1.20.1 + Gradle
- [ ] Clonar e estudar: Automatone ou ChatClef (FakePlayer + Baritone — referencia direta)
- [ ] Clonar e estudar: HumanCompanions (inventario, skin, respawn)
- [ ] Criar documento de arquitetura tecnica (pacotes, classes, interfaces principais)
- [ ] Criar repositorio GitHub alice-memory (memoria Git da Alice)
- [x] faster-whisper: INSTALADO E TESTADO — http://192.168.0.225:10300 (Machine 2, Docker, testado)

### Proximas Etapas - Implementacao por Fase

- [ ] **Fase 1:** FakePlayer ruiva (skin Scarlet) + spawn por ovo + Baritone (follow/stay) + combate basico + chat texto com LLM (Ollama)
- [ ] **Fase 2:** Crafting autonomo + tracking de inventario + mapeamento de containers + auto-equip
- [ ] **Fase 3:** Voz completa — Edge TTS FranciscaNeural + faster-whisper STT + Simple Voice Chat integration
- [ ] **Fase 4:** Schematics + construcao bloco a bloco + interacao Create (FakePlayer ja funciona nativo)
- [ ] **Fase 1:** Integrar Ollama (chat texto basico)
- [ ] **Fase 1:** Adicionar comportamentos (follow, combate, regras)
- [ ] **Fase 2:** Crafting + inventario + containers + auto-equip
- [ ] **Fase 3:** Adicionar voz (TTS + STT + radio)
- [ ] **Fase 4:** Construcao (schematics + Create)
- [ ] **Fase 5:** Base de conhecimento + orientacao + quests
- [ ] **Fase 6:** Agentes + skills + memoria Git + autonomia
- [ ] Testes com Cursed Walking (continuo, a cada fase)

---

## 12. Riscos e Issues Conhecidos

### R01 — Edge TTS: Endpoint Nao-Oficial (Risco: ALTO)

- Edge TTS nao tem API publica oficial documentada. O endpoint e descoberto por engenharia reversa do browser
- Microsoft pode mudar, bloquear ou exigir autenticacao sem aviso previo
- **Impacto:** Alice fica muda completamente (TTS para de funcionar)
- **Mitigacao:** Modo offline (decisao #49) — Piper local como fallback configuravelcom `alice.voice.mode=offline`
- **Probabilidade:** Media. Microsoft raramente quebra coisas intencionalmente, mas o endpoint nao-oficial e risco real
- **Severidade:** Alta se acontecer sem fallback configurado

### R02 — Baritone no Servidor: Uso Fora do Padrao (Risco: MEDIO)

- Baritone foi projetado para rodar no CLIENTE Minecraft, controlando o personagem do jogador
- FakePlayer no servidor + Baritone = uso nao-convencional
- Baritone pode acessar `LocalPlayer` ou `Minecraft.getInstance()` que nao existem no contexto servidor
- **Impacto:** Pathfinding pode nao funcionar, crashar o servidor, ou ter comportamento erratico
- **Mitigacao:** Estudar Automatone e ChatClef — ambos resolveram exatamente este problema (FakePlayer + Baritone no servidor)
- **Probabilidade:** Baixa (Automatone e ChatClef provam que e viavel), mas edge cases existem
- **Acao:** Na Fase 1, validar Baritone + FakePlayer no servidor antes de qualquer outra coisa

### R03 — LLM Lento: Experiencia Ruim (Risco: ALTO)

- Hardware atual: 2 tok/s = 76 segundos para resposta de ~40 palavras
- Uma conversa de 5 trocas = ~6 minutos esperando Alice responder
- **Impacto direto:** O jogo fica impraticavel com Alice se o LLM nao for otimizado
- **Mitigacoes obrigatorias:**
  - Usar modelos que cabem na GPU: `llama3.2:3b` ou `qwen2.5:3b` (~2GB, 1-3s por resposta)
  - 80% das situacoes resolvidas por regras/scripts sem LLM (latencia zero)
  - Fallback para API cloud (deepseek-v3) para perguntas complexas
  - Streaming LLM + TTS incremental para perceber menos latencia
- **Probabilidade:** Certa sem otimizacao. Com `llama3.2:3b` na GPU: latencia aceitavel (~3s)
- **Acao prioritaria:** Testar `llama3.2:3b` e `qwen2.5:3b` antes de comecar a implementacao

### R04 — Compatibilidade com 201 Mods (Risco: MEDIO)

- 201 mods no Cursed Walking, cada um potencialmente interagindo com FakePlayer de forma inesperada
- Mods de grief-prevention podem registrar acoes de Alice como griefing
- Mods de permissoes podem bloquear FakePlayer de interagir com certos blocos
- **Impacto:** Alice quebra sistema do modpack, causa bugs ou tem comportamento proibido
- **Mitigacao:**
  - FakePlayer tem `isFakePlayer() = true` — mods bem escritos checam isso
  - Servidor roda em modo offline (TLauncher) = sem verificacao Mojang
  - Testar cada fase no servidor real com o modpack completo antes de avancar
- **Probabilidade:** Baixa-media. Mods Forge geralmente respeitam FakePlayer

### R05 — Pontos Unicos de Falha na Infraestrutura (Risco: MEDIO)

| Componente | Se cair | Impacto | Fallback |
|-----------|---------|---------|---------|
| Ollama (Machine 3) | Alice sem IA | Total — fica "burra" | API cloud (deepseek-v3) |
| faster-whisper (Machine 2) | Alice sem STT | Nao ouve o jogador | Vosk local (modo offline) |
| Edge TTS (cloud) | Alice sem voz | Nao fala | Piper local (modo offline) |
| GitHub | Sem commit de memoria | Perde historico da sessao | Forge SavedData local (sempre salva) |
| Rede local | Tudo acima falha | Degradacao total | Modo offline completo |

- **Mitigacao global:** Graceful degradation — cada camada tem fallback, Alice funciona parcialmente mesmo com infra parcialmente down

### R06 — Simple Voice Chat API: Captura de Audio (Risco: BAIXO-MEDIO)

- A SVC API para mods de terceiros pode nao expor todos os hooks necessarios
- Capturar audio DO jogador (o que ele fala) via API pode ter limitacoes nao documentadas
- Injetar audio DA Alice (TTS output) no canal SVC pode requerer workarounds
- **Impacto:** Sistema de voz pode precisar de abordagem diferente da planejada
- **Mitigacao:** Estudar o Walkie-Talkie mod (ja no modpack) — usa SVC para radio, referencia direta
- **Probabilidade:** Baixa (SVC tem API publica bem documentada para integracao com mods)

### R07 — Concorrencia e Thread Safety (Risco: MEDIO-ALTO)

- Mod usa multiplas threads simultaneas: LLM async, TTS async, STT async, Git async, perception ticks
- Game tick = 50ms. Qualquer operacao bloqueante nessa thread trava o servidor inteiro
- Acesso a estado do jogo (entidades, blocos) de threads nao-server causa `ConcurrentModificationException`
- **Impacto:** Crash do servidor, corrupcao de mundo
- **Mitigacoes obrigatorias:**
  - TODAS as operacoes IO (LLM, TTS, STT, Git) em CompletableFuture/threads separadas
  - Acoes no jogo (mover Alice, colocar bloco) sempre via `server.execute()` no proximo tick
  - Usar queues thread-safe (`ConcurrentLinkedQueue`) para comunicar entre threads e tick
- **Probabilidade:** Alta sem disciplina de threading. E o bug mais comum em mods assincronos

### R08 — Alice na TAB List: Confusao de Identidade (Risco: BAIXO)

- FakePlayer aparece na lista de jogadores como "Alice" — outros players no servidor podem se confundir
- Outros jogadores podem tentar atacar Alice como PvP
- Pode interferir em sistemas de slots/limite de jogadores do servidor
- **Impacto:** Estetico e social, nao tecnico
- **Mitigacao:** E feature, nao bug — Alice e uma "jogadora" no mundo. Documentar no README do mod
- **Probabilidade:** Certa. Impacto: Baixo

### R09 — Base de Conhecimento Incompleta ou Errada (Risco: MEDIO)

- 201 mods, wikis desatualizadas, informacoes contraditórias na internet
- LLM pode alucinar mesmo com RAG (base de conhecimento nao e blindagem total)
- Alice pode dar dica errada (craftar item errado, ir para local errado, subestimar ameaca)
- **Impacto:** Jogador perde itens, toma decisao ruim, perde confianca na Alice
- **Mitigacao:**
  - Alice deve dizer "nao tenho certeza" quando confianca e baixa
  - Sistema de confirmacao por experiencia (decisao #44) — Alice corrige quando errar
  - Base de conhecimento mantida e corrigida pelo jogador continuamente
- **Probabilidade:** Alta (inevitavel no inicio, melhora com uso e correcoes)

### R10 — Memoria Git: Falha de Commit (Risco: BAIXO)

- Commit Git durante sessao de jogo pode falhar: rede instavel, conflito de merge, auth expirada
- **Impacto:** Alice "esquece" o que aconteceu na sessao (perde historico Git)
- **Mitigacao:**
  - Forge SavedData salva SEMPRE localmente (nunca perde dados criticos)
  - Git e camada adicional de persistencia portavel, nao a unica
  - Retry com backoff exponencial para falhas de commit
  - Commits ao final da sessao, nao a cada tick (reduz volume de operacoes)
- **Probabilidade:** Baixa (rede local estavel, Git e robusto)

---

## 13. Fora do Escopo — O que Nao Vamos Fazer

### Permanentemente fora do escopo

| # | O que nao faremos | Por que |
|---|------------------|---------|
| 1 | Fine-tuning ou treinamento de LLM proprio | Requer hardware especializado, datasets enormes. Usamos LLM pre-treinado via Ollama |
| 2 | Voice cloning ou voz 100% custom | Edge TTS FranciscaNeural e a voz da Alice. Sem personalizacao por usuario no MVP |
| 3 | Alice como servico separado / microservico | Alice e um mod Forge integrado. Nao e processo externo, nao e servidor dedicado |
| 4 | Geracao procedural de schematics em runtime | Alice usa schematics pre-carregados .nbt. Nao gera estruturas novas durante o jogo |
| 5 | Alice criar scripts/codigo automaticamente | Skills sao criadas por nos. Alice carrega e executa skills, nao as escreve |
| 6 | Integracao com redes sociais / streaming / Discord | Alice existe dentro do Minecraft. Sem presenca externa |
| 7 | Suporte a Bukkit / Spigot / Paper | Mod Forge exclusivo. Incompativel por arquitetura com plugins Bukkit |
| 8 | Alice ser invencivel | Morre como jogador normal — e parte da lore e do game design |
| 9 | Alice fazer web search em runtime | Base de conhecimento e compilada offline. Sem acesso a internet durante o jogo |
| 10 | Alice ter comandos de admin/op | Nao executa /op, /ban, /give. Zero acesso a comandos de servidor |
| 11 | Suporte a versoes diferentes de Minecraft 1.20.1 | Foco total em 1.20.1 Forge (servidor existente). Portar para outras versoes e projeto separado |
| 12 | Alice ter consciencia de tempo real / mundo externo | Nao sabe horas, datas reais, eventos do mundo. Vive apenas dentro do jogo |

### Fora do MVP (possivel em versoes futuras)

| # | O que nao faremos no MVP | Quando pode entrar |
|---|--------------------------|-------------------|
| 13 | Multiplas Alices por servidor | Apos MVP estavel — arquitetura ja preparada (decisao #6) |
| 14 | Suporte a outros modpacks | Requer base de conhecimento especifica por modpack — nao e generalizavel facil |
| 15 | Outros idiomas (EN, ES, etc.) | Arquitetura suporta (Edge TTS + Whisper sao multilinguais) — so nao e prioridade |
| 16 | Interface grafica de configuracao | Config via arquivo .toml Forge. GUI e conforto, nao necessidade |
| 17 | Alice no singleplayer | FakePlayer tecnicamente funciona em LAN, mas nao testado nem suportado no MVP |
| 18 | Alice aprender novas schematics em runtime | Fas 4 usa so pre-carregadas. Alice sugerir e jogador decidir (decisao #37) |
| 19 | Alice PvP entre servidores | Nao faz sentido no escopo de companheira. Se atacar Alice, ela se defende — so isso |
| 20 | Multiplas vozes / personalidades alternativas | MVP: 1 voz fixa FranciscaNeural. Troca de voz possivel mas nao prioritaria |

### O que Alice NAO e

- Alice nao e um NPC com script fixo — ela usa LLM para raciocinar
- Alice nao e um bot de Discord ou Twitch — ela existe so dentro do jogo
- Alice nao e um assistente de IA generico — ela e especialista em sobrevivencia no Cursed Walking
- Alice nao e um god-mode companion — ela pode morrer, pode errar, pode ter informacao incompleta
- Alice nao substitui o jogador — ela ORIENTA, sugere, ajuda. Quem decide e o jogador

---

## 14. Resumo e Conclusao do Brainstorm

### O que e o Projeto Alice

Alice e uma jogadora controlada por IA para Minecraft Forge 1.20.1, integrada ao modpack Cursed Walking (apocalipse zumbi, 201 mods). E uma companheira feminina ruiva que orienta o jogador, constroi defesas, gerencia recursos e sobrevive junto com ele — com voz neural, memoria persistente e raciocinio via LLM.

### Decisoes Fundamentais (resumo)

| Dimensao | Decisao | Decisao # |
|----------|---------|-----------|
| Plataforma | Forge 1.20.1 | #1 |
| Entidade | FakePlayer (ServerPlayer) — `net.minecraftforge.common.util.FakePlayer` | #21/#46 |
| Pathfinding | Baritone (nativo com FakePlayer, zero adapter) | #22 |
| LLM | ollama4j → Ollama local, fallback API cloud | #33 |
| TTS padrao | Edge TTS pt-BR-FranciscaNeural (gratuito, cloud Microsoft) | #47 |
| STT padrao | faster-whisper REST, Machine 2 Docker :10300 | #48 |
| TTS offline | Piper local pt-br-faber-medium | #49 |
| STT offline | Vosk local vosk-model-small-pt-0.3 | #49 |
| Voz | Simple Voice Chat (ja no Cursed Walking) | #30 |
| Memoria | Forge SavedData (local) + Git repo GitHub (portavel) | #28/#31 |
| Agentes | Orquestrador + 6 agentes especializados | #29 |
| Conhecimento | Base compilada markdown + RAG keywords + experiencia | #44 |
| Faseamento | 6 fases bem definidas | #23 |
| Total | **49 decisoes de arquitetura registradas** | — |

### Stack Tecnica Final

```
MOD ALICE (Forge 1.20.1 Java 17)
├── Entidade:       FakePlayer (net.minecraftforge.common.util.FakePlayer)
├── Pathfinding:    Baritone API
├── LLM:            ollama4j → http://192.168.0.200:11434 (Ollama)
├── TTS online:     Edge TTS HTTP (cloud) → pt-BR-FranciscaNeural → mp3 → PCM 48kHz → SVC
├── TTS offline:    Piper TTS local → pt-br-faber-medium
├── STT online:     POST http://192.168.0.225:10300/v1/audio/transcriptions (faster-whisper Docker)
├── STT offline:    Vosk local → vosk-model-small-pt-0.3
├── Voz:            Simple Voice Chat API (ja no modpack Cursed Walking)
├── Memoria local:  Forge SavedData (NBT/JSON)
├── Memoria remota: JGit → GitHub alice-memory (publico)
├── Receitas:       RecipeManager nativo + JEI API (soft dep)
├── Quests:         FTB Quests API (soft dep)
└── Construcao:     Schematics .nbt pre-carregados

INFRAESTRUTURA
├── Machine 1 (dev/cliente):  192.168.0.x    — VSCode + Gradle + TLauncher (Windows 11)
├── Machine 2 (MC server):    192.168.0.225  — Forge 1.20.1 + Cursed Walking + faster-whisper :10300
└── Machine 3 (Ollama/LLM):   192.168.0.200  — Ollama :11434 (llama3.2, phi3)
```

### Estado da Infraestrutura

| Componente | Estado | Endpoint |
|-----------|--------|---------|
| faster-whisper (STT) | INSTALADO E TESTADO | http://192.168.0.225:10300 |
| Ollama (LLM) | RODANDO (otimizar modelos) | http://192.168.0.200:11434 |
| Edge TTS (TTS) | CONFIRMADO GRATUITO | cloud Microsoft |
| Simple Voice Chat | JA NO MODPACK | — |
| Modo offline (Piper + Vosk) | A INSTALAR (quando precisar) | local |
| GitHub alice-memory | A CRIAR (inicio da implementacao) | github.com |

### Faseamento

| Fase | Objetivo | Desbloqueado por |
|------|---------|-----------------|
| Fase 1 | FakePlayer ruiva + Baritone (follow/stay) + chat texto com LLM | Forge MDK setup |
| Fase 2 | Crafting autonomo + inventario + containers + auto-equip | Fase 1 estavel |
| Fase 3 | Voz completa — Edge TTS + faster-whisper + Simple Voice Chat | Fase 2 estavel |
| Fase 4 | Schematics + construcao bloco a bloco + Create (FakePlayer nativo) | Fase 3 estavel |
| Fase 5 | Base de conhecimento compilada + orientacao por fase + quests | Fase 4 estavel |
| Fase 6 | Agentes + skills + memoria Git + autonomia avancada | Fase 5 estavel |

### Proximas Etapas Imediatas (Pre-Fase 1)

1. **Setup Forge MDK 1.20.1** — Gradle + IntelliJ/VSCode, build Hello World rodando
2. **Estudar Automatone ou ChatClef** — FakePlayer + Baritone no servidor, referencia direta
3. **Testar llama3.2:3b na Machine 3** — validar latencia com modelo que cabe na GPU
4. **Criar repositorio GitHub alice-memory** — estrutura de pastas inicial

### Visao Final

> Alice nao e um NPC — ela e uma jogadora. Uma companheira real que vive no mesmo mundo, enfrenta os mesmos perigos, e carrega o conhecimento de quem estudou esse apocalipse antes de existir ali. Ela orienta, constroi, luta e lembra. E ela fala com voce em portugues, com voz de verdade.

**O brainstorm esta CONCLUIDO. 49 decisoes de arquitetura tomadas. A implementacao pode comecar.**
