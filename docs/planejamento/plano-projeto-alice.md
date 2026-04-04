# Projeto Alice — Plano de Projeto Completo e Detalhado

**Versão:** 1.0  
**Data:** 2026-04-03  
**Status:** Documento vivo — referência oficial de desenvolvimento  
**Baseado em:** Brainstorm v0.9 (49 decisões tomadas)

---

## Sumário

1. [Visão Geral da Arquitetura](#arquitetura)
2. [Fase 1 — Fundação](#fase-1)
3. [Fase 2 — Utilidade](#fase-2)
4. [Fase 3 — Voz](#fase-3)
5. [Fase 4 — Construção](#fase-4)
6. [Fase 5 — Guia](#fase-5)
7. [Fase 6 — Inteligência](#fase-6)
8. [Definição dos Agentes de IA](#agentes)
9. [Regras Always-On](#regras)
10. [Catálogo de Skills](#skills)
11. [Ordem de Implementação Recomendada](#ordem)

---

## Arquitetura de Referência {#arquitetura}

### Camadas e Responsabilidades

```
PERCEPÇÃO → VOZ → REGRAS → ORQUESTRADOR → AGENTES → SKILLS → AÇÃO → MEMÓRIA
```

| Camada | Latência | Responsabilidade | Tecnologia |
|--------|----------|------------------|------------|
| Percepção | <1ms | Scaneia mundo, eventos Forge | AABB, Forge Events |
| Voz | ~300ms | STT/TTS pipeline | faster-whisper + Edge TTS |
| Regras | <1ms | Comportamentos always-on | Java puro, tick-based |
| Orquestrador | <3s | Classifica input, roteia agente | ollama4j + tool calling |
| Agentes | <3s | Raciocínio especializado por domínio | ollama4j, system prompts |
| Skills | ~0ms | Módulos composáveis pré-carregados | Java + markdown |
| Ação | <50ms | Executa decisões no jogo | Baritone, RecipeManager |
| Memória | ~1s | Persiste estado e aprendizado | SavedData + JGit |

### Princípio de Prioridade

```
Regras de Segurança > Regras de Utilidade > Agentes LLM > Comportamento Padrão
```

---

## Fase 1 — Fundação {#fase-1}

### Escopo

**DENTRO da fase:**
- Estrutura do projeto Forge 1.20.1 (build, gradle, dependências básicas)
- FakePlayer funcional no servidor com skin Scarlet (ruiva)
- Spawn via ovo customizado no inventário do jogador
- Morte com keepInventory lógico (Alice não perde itens)
- Respawn em cama configurada ou ponto original do ovo
- Baritone integrado: comandos `follow`, `stay`, `goto <x y z>`
- Combate básico via regras (atacar mob hostil mais próximo, fugir se HP < 20%)
- Chat texto bidirecional: jogador escreve no chat → Alice responde via LLM
- Interface `AliceLLMProvider` com implementação `OllamaProvider` (ollama4j)
- System prompt base com personalidade da Alice
- Configuração básica: URL do Ollama, modelo, distância de percepção
- Logging estruturado para debug

**FORA da fase:**
- Voz (TTS/STT)
- Crafting autônomo
- Schematics/construção
- Agentes especializados (orquestrador simples apenas)
- Memória Git
- Skills formais
- JEI, FTB Quests

### DOR (Definition of Ready) — Critérios para INICIAR

- [ ] Forge MDK 1.20.1 configurado e buildando sem erros
- [ ] Servidor Cursed Walking rodando na Máquina 2 (192.168.0.225)
- [ ] Ollama respondendo na Máquina 3 (192.168.0.200:11434) com llama3.2 ou phi3
- [ ] SSH ou acesso à pasta mods/ do servidor para deploy do `.jar`
- [ ] TLauncher conectando ao servidor normalmente
- [ ] Skin Scarlet baixada do Planet Minecraft (PNG 64x64)
- [ ] Repositório Git local criado para o mod

### DOD (Definition of Done) — Critérios para CONCLUIR

- [ ] Alice aparece no servidor com skin ruiva correta
- [ ] Alice aparece na TAB list como jogador
- [ ] Ovo de spawn no inventário inicial funciona
- [ ] Alice segue o jogador com `follow` e para com `stay`
- [ ] Alice navega para coordenadas com `goto`
- [ ] Alice ataca mobs hostis num raio de 8 blocos automaticamente
- [ ] Alice foge quando HP < 20% (regra ativa)
- [ ] Alice responde via chat quando o jogador escreve para ela no chat
- [ ] Resposta do LLM é assíncrona (não trava o servidor)
- [ ] Alice reaparece após morte sem perder itens
- [ ] Zero crash no servidor com Alice presente por 30 minutos
- [ ] Build do mod gera `.jar` deployável via Gradle

### Entregáveis Concretos

```
src/main/java/com/alice/
├── AliceMod.java                    # Main mod class, @Mod annotation
├── entity/
│   ├── AliceFakePlayer.java         # Classe principal — extends FakePlayer
│   ├── AliceFakePlayerManager.java  # Singleton: spawn, despawn, referência global
│   └── AliceSpawnEgg.java           # Item ovo customizado
├── behavior/
│   ├── AliceTickHandler.java        # ServerTickEvent — executa regras a cada tick
│   ├── rules/
│   │   ├── IAliceRule.java          # Interface: boolean shouldApply(), void execute()
│   │   ├── FleeOnLowHealthRule.java # Foge se HP < 20%
│   │   ├── AttackNearestHostileRule.java  # Ataca mob hostil no raio
│   │   └── RuleEngine.java          # Executa regras em ordem de prioridade
├── pathfinding/
│   └── AliceBaritoneController.java # Wrapper da Baritone API
├── llm/
│   ├── AliceLLMProvider.java        # Interface abstrata
│   ├── OllamaProvider.java          # Implementação com ollama4j
│   └── AliceChatHandler.java        # ServerChatEvent → LLM → resposta no chat
├── config/
│   └── AliceConfig.java             # ForgeConfigSpec: ollama_url, model, etc.
└── events/
    └── AliceEventHandler.java       # Registro de todos os @SubscribeEvent
```

### Dependências Técnicas

```gradle
// build.gradle — dependencies
dependencies {
    minecraft "net.minecraftforge:forge:1.20.1-47.3.0"
    
    // LLM
    implementation fg.deobf("io.github.ollama4j:ollama4j:1.1.6")
    
    // Pathfinding — Baritone API jar
    // Opção A: Baritone standalone jar em /libs
    // Opção B: Compilar Baritone do fonte e usar API jar
    implementation files("libs/baritone-api-1.20.1-1.10.0.jar")
    
    // Simple Voice Chat (soft dep, adicionado na Fase 3)
    // compileOnly fg.deobf("de.maxhenkel.voicechat:voicechat-api:2.5.26")
}
```

### Sequência de Implementação Interna

1. **Setup do projeto** (2-3h)
   - Forge MDK 1.20.1, configurar `build.gradle`, `mods.toml`
   - Criar estrutura de pacotes
   - Build de sanidade: `./gradlew build` sem erros

2. **FakePlayer básico** (4-6h)
   - `AliceFakePlayer extends net.minecraftforge.common.util.FakePlayer`
   - Spawn via `ServerLevel.addFreshEntityWithPassengers()`
   - Manager singleton para referência global
   - Verificar aparência na TAB list

3. **Ovo de spawn** (2-3h)
   - `AliceSpawnEgg extends Item`
   - `UseOnContext`: ao usar no chão, spawna Alice nas coordenadas
   - Registrar item no Forge registry
   - Dar ovo ao jogador ao conectar (ou via `/give`)

4. **Skin** (1-2h)
   - Converter skin PNG para base64 ou usar Mojang property format
   - Injetar no `GameProfile` da Alice durante spawn
   - Testar visualmente no cliente

5. **Baritone** (4-8h)
   - Incluir `baritone-api.jar` como dependência
   - `AliceBaritoneController`: obter `IBaritone` para o FakePlayer
   - Implementar `follow(player)`, `stay()`, `goto(BlockPos)`
   - Testar navegação básica no servidor

6. **Morte e respawn** (2-3h)
   - `LivingDeathEvent`: salvar inventário antes de morrer
   - `PlayerRespawnEvent`: restaurar inventário após respawn
   - Salvar `spawnPoint` original do ovo via `SavedData`
   - Após respawn: chamar `follow()` ou `goto(base)`

7. **Engine de regras** (3-4h)
   - Interface `IAliceRule` com `priority()`, `shouldApply()`, `execute()`
   - `RuleEngine` itera regras por prioridade em `ServerTickEvent`
   - Implementar `FleeOnLowHealthRule`: HP < 20% → Baritone foge do mob mais próximo
   - Implementar `AttackNearestHostileRule`: escanear AABB, atacar mob mais próximo

8. **Chat com LLM** (4-6h)
   - Interface `AliceLLMProvider` com `chatAsync(systemPrompt, history, message)`
   - `OllamaProvider` usando ollama4j, chamadas em `CompletableFuture`
   - `AliceChatHandler`: capturar `ServerChatEvent`, verificar se é para Alice
   - Parsing de destinatário: "Alice, ..." ou "@alice ..." → envia para LLM
   - Resposta: `player.sendSystemMessage(Component.literal("[Alice] " + resposta))`
   - System prompt base da Alice embutido em `/assets/alice/prompts/base.txt`

9. **Configurações** (1-2h)
   - `AliceConfig` com `ForgeConfigSpec`: `ollamaUrl`, `ollamaModel`, `perceptionRadius`
   - Arquivo: `config/alice-common.toml`

10. **Testes e estabilização** (2-4h)
    - Deploy no servidor da Máquina 2
    - Conectar via TLauncher e testar todos os DOD
    - Corrigir crashes, memory leaks

### Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Baritone conflito com FakePlayer no Forge | Média | Alto | Estudar Automatone e ChatClef antes; ter fallback para movimento manual via `PlayerAction` |
| FakePlayer causa crash no servidor | Alta (baseado no bug do Duzo's) | Alto | Separar rigorosamente código client/server com `@OnlyIn(Dist.CLIENT)` e `DistExecutor` |
| Skin não renderiza | Baixa | Baixo | Usar GameProfile com textura embarcada no mod como fallback |
| Ollama lento (2 tok/s) torna chat inutilizável | Alta | Médio | Usar phi3:mini (mais rápido); adicionar mensagem "Alice está pensando..." |
| Conflito de classloading do ollama4j com Forge | Média | Alto | Usar `jarJar` do Forge para empacotar dependências; ou HTTP direto como fallback |

### Referências de Código

- `https://github.com/duzos/fakeplayer` — Implementação de FakePlayer 1.20.1, skin, interações
- `https://github.com/cabaletta/baritone` — API pública, IBaritone, goals
- `https://github.com/elefant-ai/chatclef` — Integração Baritone + LLM pipeline
- `https://github.com/dongge0210/Forge-AI-Player` — FakePlayer + ollama4j no Forge, pipeline LLM→ação
- Forge MDK samples: `ServerTickEvent`, `LivingDeathEvent`, `ServerChatEvent`

---

## Fase 2 — Utilidade {#fase-2}

### Escopo

**DENTRO da fase:**
- Sistema completo de crafting autônomo via `RecipeManager`
- Suporte a todos os tipos de receita: CRAFTING, SMELTING, BLASTING, SMOKING, CAMPFIRE, STONECUTTING, SMITHING
- Inventário da Alice gerenciado e rastreado
- Tracking do inventário do jogador (Alice "vê" o que o jogador tem)
- Mapeamento de containers: registrar posição e conteúdo de chests/barrels
- Auto-equip: Alice equipa automaticamente a melhor arma e armadura disponível
- Alice responde a perguntas sobre receitas: "o que preciso para fazer X?"
- Alice executa crafting quando solicitado: "crafta Y para mim"
- Coleta de itens do chão no raio de ação
- Soft dependency com JEI API para lookup mais rico de receitas

**FORA da fase:**
- Voz
- Schematics de construção
- Agentes especializados (orquestrador ainda básico)
- Memória Git
- Base de conhecimento RAG

### DOR

- [ ] Fase 1 completamente concluída (todos os DOD)
- [ ] Alice estável no servidor há pelo menos 1 sessão de 1h sem crash

### DOD

- [ ] Alice lista ingredientes necessários para qualquer receita quando perguntada via chat
- [ ] Alice usa uma bancada de trabalho próxima para craftar itens quando solicitada
- [ ] Alice usa fornalha/blast furnace quando apropriado
- [ ] Alice equipa automaticamente a melhor armadura ao ganhar peças melhores
- [ ] Alice equipa automaticamente a melhor arma ao encontrar uma no inventário
- [ ] Alice coleta itens do chão automaticamente num raio de 4 blocos
- [ ] Alice identifica todos os containers numa área de 32 blocos ao redor
- [ ] Alice responde corretamente: "Alice, onde está meu diamante?" (consulta mapa de containers)
- [ ] Mapa de containers persiste entre reinicializações do servidor

### Entregáveis Concretos

```
src/main/java/com/alice/
├── crafting/
│   ├── AliceCraftingManager.java    # Wrapper do RecipeManager
│   ├── RecipeQuery.java             # Query: "o que preciso para X?"
│   ├── CraftingExecutor.java        # Executa craft numa bancada próxima
│   └── MaterialCalculator.java      # Calcula materiais faltando vs disponíveis
├── inventory/
│   ├── AliceInventoryTracker.java   # Rastreia inventário de Alice + jogador
│   ├── ContainerMapper.java         # Mapeia containers do mundo
│   ├── ContainerRecord.java         # Registro: posição + conteúdo + timestamp
│   ├── AutoEquipRule.java           # Regra: equipar melhor item disponível
│   └── ItemPickupRule.java          # Regra: coletar itens do chão próximos
├── data/
│   └── AliceSavedData.java          # Forge SavedData: persistir ContainerMapper
└── jei/
    └── JeiIntegration.java          # Soft dep: query ao JEI se disponível
```

### Sequência de Implementação Interna

1. **RecipeManager wrapper** (3-4h)
   - `AliceCraftingManager`: método `findRecipe(ItemStack result)` → `RecipeHolder<T>`
   - `getIngredients(RecipeHolder)` → `List<ItemStack>` com quantidades
   - Suportar todos os tipos de receita via `ServerLevel.getRecipeManager()`
   - Cobrir receitas de mods automaticamente (RecipeManager é universal)

2. **MaterialCalculator** (2-3h)
   - `calculateMissing(recipe, aliceInventory, playerInventory, containerMap)` → `Map<Item, Integer>` faltando
   - Alice verifica inventário próprio → inventário do jogador → containers mapeados
   - Resposta formatada para chat: "Preciso de: 3x ferro, 1x carvão. Falta: 1x ferro."

3. **CraftingExecutor** (4-6h)
   - Localizar bancada de trabalho mais próxima (AABB scan)
   - Navegar até ela via Baritone
   - Abrir GUI da bancada (simulação de clique com FakePlayer)
   - Colocar ingredientes nos slots corretos
   - Executar o craft
   - Alternativa mais simples: chamar `ServerLevel.getRecipeManager().craft()` diretamente sem GUI

4. **ContainerMapper** (4-6h)
   - Evento `PlayerInteractEvent.RightClickBlock`: ao abrir container, mapear
   - Evento `BlockEvent.BreakEvent`: remover container do mapa
   - `ContainerRecord`: `BlockPos`, `ResourceLocation` tipo, `NonNullList<ItemStack>` conteúdo, `long` timestamp
   - `AliceSavedData extends SavedData`: serializar/deserializar para NBT

5. **Auto-equip** (2-3h)
   - `AutoEquipRule`: a cada 20 ticks, comparar itens no inventário com equipados
   - Usar `ArmorItem.getDefense()` e `Tier` para comparação de armas
   - Equipar automaticamente se encontrar item melhor

6. **Item pickup** (1-2h)
   - `ItemPickupRule`: a cada 5 ticks, `ItemEntitySelector.getEntitiesOfClass()` no raio
   - Mover Alice para o item, simular coleta

7. **Chat integration** (2-3h)
   - Atualizar o `AliceChatHandler` para reconhecer intenções de crafting
   - "Alice, o que preciso para fazer uma espada de diamante?" → `RecipeQuery`
   - "Alice, crafta uma bancada de trabalho" → `CraftingExecutor`

8. **JEI soft dep** (1-2h)
   - `JeiIntegration.java` com `@Optional` do Forge
   - Se JEI presente, usar `IJeiRuntime.getRecipeManager()` para lookup mais rico
   - Fallback para RecipeManager nativo se JEI ausente

### Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| FakePlayer não consegue abrir GUI de crafting | Média | Alto | Usar `craft()` direto via API sem GUI |
| ContainerMapper fica desatualizado | Alta | Médio | Adicionar `ContainerCloseEvent` para re-sync; timestamp para invalidação |
| Receitas de mods com tipos customizados | Média | Médio | Cobrir apenas tipos vanilla + tratar unknown graciosamente |
| Performance do AABB scan de containers | Baixa | Médio | Limitar scan a 1x por minuto ou sob demanda |

---

## Fase 3 — Voz {#fase-3}

### Escopo

**DENTRO da fase:**
- Alice fala em português BR com voz feminina neural (pt-BR-FranciscaNeural)
- Pipeline TTS: texto → Edge TTS HTTP → mp3 → PCM → Simple Voice Chat
- Alice ouve o jogador: SVC captura áudio → faster-whisper REST → texto → LLM
- Integração com Simple Voice Chat API (FakePlayer como player de voz)
- Modo offline configurável: Piper TTS (voz masculina) + Vosk STT
- Modo text-only: sem voz alguma
- Configuração: `alice.voice.mode = online|offline|text_only`
- Alice fala por proximidade (range padrão SVC ~32 blocos)
- Alice fala por canal/grupo SVC para distâncias maiores
- Tratamento de falhas: se Edge TTS cair, fallback para texto no chat

**FORA da fase:**
- Criação de item rádio customizado (decisão #35 — usar SVC diretamente)
- Treinamento de voz customizada para Alice
- Múltiplas vozes ou idiomas

### DOR

- [ ] Fase 2 completamente concluída
- [ ] faster-whisper confirmado rodando na Máquina 2 porta 10300 (já confirmado)
- [ ] Simple Voice Chat instalado no servidor e cliente (já está no Cursed Walking)
- [ ] Acesso à internet no servidor para Edge TTS

### DOD

- [ ] Jogador fala no microfone, Alice transcreve e responde em texto (STT funcionando)
- [ ] Alice responde com voz feminina (TTS Edge TTS funcionando)
- [ ] Jogador ouve Alice via Simple Voice Chat ao se aproximar
- [ ] Latência total voz-a-voz < 8 segundos (margem dada a LLM lento)
- [ ] Modo offline ativável e funcional (voz masculina Piper)
- [ ] Modo text_only ativável e funcional
- [ ] Alice não transmite voz quando não tem nada a dizer
- [ ] Sem echo ou loopback de áudio
- [ ] Log de cada frase transcrita para debug

### Entregáveis Concretos

```
src/main/java/com/alice/
├── voice/
│   ├── AliceVoicechatPlugin.java    # @ForgeVoicechatPlugin — registro no SVC
│   ├── AudioPipeline.java           # Coordenador central do pipeline de áudio
│   ├── tts/
│   │   ├── ITTSProvider.java        # Interface: synthesize(text) → byte[] PCM
│   │   ├── EdgeTTSProvider.java     # HTTP para Edge TTS cloud
│   │   ├── PiperTTSProvider.java    # piper-jni local (fallback offline)
│   │   └── TTSProviderFactory.java  # Escolhe provider baseado em config
│   ├── stt/
│   │   ├── ISTTProvider.java        # Interface: transcribe(byte[] audio) → String
│   │   ├── FasterWhisperProvider.java  # HTTP REST para Machine 2 :10300
│   │   ├── VoskProvider.java        # Vosk local (fallback offline)
│   │   └── STTProviderFactory.java  # Escolhe provider baseado em config
│   └── svc/
│       ├── AliceSVCConnection.java  # Gerencia conexão com SVC API
│       ├── AliceAudioSender.java    # Envia PCM para SVC (Alice fala)
│       └── PlayerAudioReceiver.java # Recebe áudio do jogador do SVC
src/main/resources/
└── alice-offline/
    ├── piper/
    │   └── pt_BR-faber-medium.onnx  # Modelo Piper (se incluir no mod)
    └── vosk/
        └── vosk-model-small-pt/     # Modelo Vosk (se incluir no mod)
```

### Pipeline Técnico Detalhado

**STT (Jogador → Alice):**

```
1. PlayerAudioReceiver escuta AudioPackets do SVC para o jogador alvo
2. Acumula chunks de opus até detectar silêncio (VAD: Voice Activity Detection)
3. Decodifica opus → PCM 16kHz mono (usar JCodec ou OGG/Opus decoder)
4. Em thread separada: POST para http://192.168.0.225:10300/v1/audio/transcriptions
   - Content-Type: multipart/form-data
   - file: audio.wav (PCM encapsulado em WAV header)
   - model: whisper-1 (faster-whisper aceita como alias)
   - language: pt
5. Resposta JSON: {"text": "Alice, me siga até a floresta"}
6. Texto entra no AliceChatHandler como se fosse mensagem de chat
```

**TTS (Alice → Jogador):**

```
1. LLM gera texto de resposta da Alice
2. EdgeTTSProvider monta SSML:
   <speak version='1.0' xml:lang='pt-BR'>
     <voice name='pt-BR-FranciscaNeural'>
       <prosody rate='0%' pitch='0%'>{texto}</prosody>
     </voice>
   </speak>
3. POST para endpoint Edge TTS (estudar edge-tts Python para descobrir endpoint exato)
   - Headers: Authorization, Content-Type: application/ssml+xml
   - Accept: audio/mpeg
4. Recebe bytes mp3
5. Decodifica mp3 → PCM 48kHz estéreo (SimpleVoiceChat exige 48kHz)
6. Converte estéreo → mono se necessário
7. Encode PCM → opus
8. AliceAudioSender injeta opus chunks no SVC via StaticAudioChannel ou EntityAudioChannel
```

### Sequência de Implementação Interna

1. **Pesquisa do endpoint Edge TTS** (2-4h)
   - Estudar biblioteca Python `edge-tts` para descobrir URL e headers exatos
   - Testar com `curl` ou Postman antes de implementar em Java
   - Documentar endpoint, headers obrigatórios, formato de resposta

2. **EdgeTTSProvider** (4-6h)
   - `synthesize(String text)` → `byte[] mp3`
   - Usar `java.net.http.HttpClient` (Java 11+ nativo, sem dependência)
   - Decoder mp3 → PCM: usar `mp3spi` ou `jlayer` (verificar compatibilidade Forge)
   - Testar saída PCM com arquivo WAV antes de conectar ao SVC

3. **Plugin SVC** (3-5h)
   - `AliceVoicechatPlugin implements VoicechatPlugin`
   - Registrar plugin com `@ForgeVoicechatPlugin`
   - Criar `EntityAudioChannel` para Alice (proximidade)
   - Criar `StaticAudioChannel` para modo distância
   - Testar envio de áudio estático (arquivo .ogg pré-gravado) antes de conectar TTS

4. **AliceAudioSender** (2-3h)
   - Recebe `byte[] pcm48k`
   - Encode para opus usando API do SVC (SVC já expõe encoder)
   - Envia packets via canal configurado

5. **PlayerAudioReceiver** (4-6h)
   - Registrar listener para `OnSoundPacketEvent` do SVC
   - Filtrar: apenas áudio do jogador "dono" da Alice
   - Acumular bytes opus em buffer circular
   - Implementar VAD simples: silêncio detectado após 500ms sem áudio

6. **FasterWhisperProvider** (2-3h)
   - `transcribe(byte[] pcm16k)` → `String texto`
   - Encapsular PCM em WAV header
   - POST multipart para `http://192.168.0.225:10300/v1/audio/transcriptions`
   - Parse JSON de resposta

7. **Integração no pipeline central** (2-3h)
   - `AudioPipeline`: recebe texto do STT → envia para `AliceChatHandler`
   - `AliceChatHandler`: ao gerar resposta → envia para `AudioPipeline.speak(texto)`
   - Coordenar threading: STT e TTS em threads separadas, nunca no game tick

8. **Modo offline** (3-4h)
   - `PiperTTSProvider` usando `piper-jni`
   - `VoskProvider` usando `vosk-api` Java binding
   - `TTSProviderFactory`: lê config `alice.voice.mode` → instancia provider correto

9. **Configurações de voz** (1h)
   - Adicionar ao `AliceConfig`: `voiceMode`, `whisperUrl`, `svcRange`

10. **Testes end-to-end** (4-6h)
    - Testar ciclo completo: falar → transcrever → LLM → TTS → ouvir
    - Medir latência real
    - Testar modo offline
    - Testar queda do Edge TTS (simulada)

### Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Endpoint Edge TTS muda sem aviso | Média | Alto | Abstrair em `EdgeTTSProvider`; manter edge-tts Python como referência |
| Codec mp3/opus no Forge classloader | Alta | Alto | Testar codec jlayer vs mp3spi; alternativa: subprocess `ffmpeg` no servidor |
| SVC API incompatível com versão no Cursed Walking | Média | Alto | Verificar versão exata do SVC no modpack antes de implementar |
| VAD incorreto (corta frase no meio) | Alta | Médio | Usar limiar de silêncio configurável; opção de push-to-talk como alternativa |
| Latência total > 15s com LLM lento | Alta | Alto | Mostrar animação "pensando" no jogo; usar phi3:mini para respostas de voz |

---

## Fase 4 — Construção {#fase-4}

### Escopo

**DENTRO da fase:**
- Carregar schematics `.nbt` pré-incluídos no mod
- Construção bloco a bloco via Baritone `BuilderProcess`
- Catálogo inicial de estruturas: abrigo emergência, muro perimetral, torre de vigia, base mínima
- Alice lista materiais necessários antes de construir
- Alice verifica materiais disponíveis (inventários + containers) antes de iniciar
- Alice constrói de baixo para cima, sólidos antes de dependentes
- Suporte a schematics do Create mod (contraptions básicas)
- Comando: "Alice, constrói um muro aqui"
- Comando: "Alice, o que preciso para construir a base?"
- Alice para construção se HP cair abaixo de 30% (regra de segurança)

**FORA da fase:**
- Geração de schematics por IA
- Schematics criados pelo jogador em runtime
- Contraptions Create complexas (torres de guarda autônomas, etc.)
- Construção colaborativa com o jogador simultâneo

### DOR

- [ ] Fase 2 completamente concluída (crafting funcional)
- [ ] Pelo menos 3 schematics `.nbt` criados e testados offline
- [ ] Entendimento do `BuilderProcess` da Baritone API testado

### DOD

- [ ] Alice lista pelo menos 5 schematics disponíveis quando solicitada
- [ ] Alice lista materiais completos para qualquer schematic
- [ ] Alice constrói abrigo de emergência (4x4x3 blocos) com sucesso
- [ ] Alice para construção automaticamente se atacada
- [ ] Alice retoma construção após ameaça eliminada
- [ ] Schematic de muro perimetral construído com sucesso
- [ ] Alice busca materiais faltantes em containers mapeados antes de pedir ao jogador
- [ ] Construção com blocos Create funciona (schematic simples de Create)

### Entregáveis Concretos

```
src/main/java/com/alice/
├── construction/
│   ├── AliceConstructionManager.java  # Coordenador de construção
│   ├── SchematicLoader.java           # Carrega .nbt do classpath
│   ├── SchematicRegistry.java         # Catálogo de schematics com metadata
│   ├── SchematicMetadata.java         # Nome, descrição, materiais, tamanho
│   ├── BuildExecutor.java             # Chama Baritone BuilderProcess
│   ├── MaterialPreflightCheck.java    # Verifica materiais antes de construir
│   └── BuildingStateRule.java         # Regra: para se HP < 30%
src/main/resources/assets/alice/
└── schematics/
    ├── emergency_shelter.nbt          # Abrigo 4x4x3
    ├── perimeter_wall_section.nbt     # Seção de muro 8x1x3
    ├── watchtower.nbt                 # Torre 3x3x8
    ├── simple_base.nbt                # Base mínima 8x8x4
    └── create_simple_mill.nbt         # Moinho simples Create
```

### Sequência de Implementação Interna

1. **Criar schematics** (4-8h, fora do código)
   - Construir estruturas no Minecraft em mundo criativo
   - Exportar como `.nbt` usando Structure Block nativo
   - Testar carregamento em mundo separado antes de incluir no mod

2. **SchematicLoader** (2-3h)
   - Ler `.nbt` do classpath via `AliceMod.class.getResourceAsStream()`
   - Deserializar para `StructureTemplate` via `StructureManager`
   - Testar carregamento sem falhas

3. **SchematicMetadata e Registry** (2h)
   - JSON de metadata por schematic: nome, descrição, dimensões, lista de materiais
   - `SchematicRegistry.getAll()`, `getByName(name)`, `findByKeyword(keyword)`

4. **MaterialPreflightCheck** (2-3h)
   - Usar `MaterialCalculator` da Fase 2
   - Listar o que Alice tem, o que está nos containers, o que falta
   - Resposta formatada para chat

5. **BuildExecutor** (4-6h)
   - Obter `IBuilderProcess` do Baritone
   - `startBuild(BlockPos origin, StructureTemplate template)`
   - Monitorar progresso via `IBuilderProcess.isFinished()`
   - Lidar com blocos não alcançáveis (pular e reportar ao jogador)

6. **BuildingStateRule** (1h)
   - Regra: durante construção, se HP < 30% → pausar Baritone → fugir
   - Retomar quando HP > 50% e área segura

7. **Integração no chat** (2-3h)
   - Reconhecer intenções de construção no `AliceChatHandler`
   - "Alice, constrói um abrigo aqui" → identificar schematic, verificar materiais, iniciar

### Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Baritone BuilderProcess lento ou com bug no 1.20.1 | Média | Alto | Ter fallback manual (colocar blocos tick a tick via `level.setBlockAndUpdate`) |
| Schematic com blocos de mods causando crash | Média | Médio | Filtrar blocos desconhecidos; substituir por similar ou pular |
| Alice constrói no lugar errado (offset errado) | Alta | Médio | Pedir confirmação antes: "Vou construir aqui [coords]. Confirma?" |
| Create contraptions não funcionam ao ser construídas por FakePlayer | Média | Médio | Testar separadamente; documentar limitação se não funcionar |

---

## Fase 5 — Guia {#fase-5}

### Escopo

**DENTRO da fase:**
- Base de conhecimento compilada: mecânicas do Minecraft, mods do Cursed Walking, estratégias
- Sistema RAG: indexar conhecimento por embedding/keywords, buscar contexto relevante
- Alice orienta o jogador por fase de jogo (early/mid/late game)
- Integração com FTB Quests (soft dep): Alice sabe as quests ativas do jogador
- Alice sugere próximos passos baseado em quests e fase de jogo
- "Lembranças de outra vida" integradas ao system prompt
- Alice revela informações de forma progressiva conforme contexto
- Triggers de orientação: item novo, área nova, ameaça nova
- Resposta a perguntas factuais sobre o jogo: "como funciona a infecção zumbi?"
- Memória local via Forge SavedData: aventuras, locais, jogadores

**FORA da fase:**
- Memória Git (Fase 6)
- Auto-criação de skills (Fase 6)
- Agentes especializados completos (Fase 6)
- Multi-agente colaborativo (Fase 6)

### DOR

- [ ] Fase 3 completamente concluída (voz funcional)
- [ ] Lista dos 201 mods do Cursed Walking extraída do servidor
- [ ] Processo de compilação da base de conhecimento definido
- [ ] FTB Quests confirmado no modpack (verificar `mods/` do servidor)

### DOD

- [ ] Alice responde corretamente sobre mecânicas básicas do Minecraft (crafting, survival)
- [ ] Alice responde sobre pelo menos 20 mods do Cursed Walking
- [ ] Alice responde sobre infecção zumbi, blood moon, hordas do modpack
- [ ] Alice sugere próximo passo de quest quando o jogador não sabe o que fazer
- [ ] Alice dá dica proativa quando jogador pega item novo pela primeira vez
- [ ] Alice não repete a mesma orientação para o mesmo jogador na mesma sessão
- [ ] Respostas são contextualmente relevantes (não genéricas)
- [ ] Memória de aventuras persiste entre sessões (SavedData)

### Entregáveis Concretos

```
src/main/java/com/alice/
├── knowledge/
│   ├── KnowledgeBase.java           # Carrega e indexa a base de conhecimento
│   ├── KnowledgeChunk.java          # Chunk de conhecimento: texto + tags + fase
│   ├── KeywordSearchIndex.java      # Índice por palavras-chave (sem embeddings)
│   ├── ContextBuilder.java          # Monta contexto relevante para o LLM
│   └── GamePhaseDetector.java       # Detecta fase de jogo: early/mid/late
├── quests/
│   ├── IQuestProvider.java          # Interface para acesso a quests
│   ├── FTBQuestsProvider.java       # Implementação FTB Quests (soft dep)
│   ├── FallbackQuestProvider.java   # Sem FTB: guia por heurísticas
│   └── QuestContextBuilder.java     # Monta contexto de quests para LLM
├── guidance/
│   ├── ProactiveTipEngine.java      # Triggers: item novo, área nova, etc.
│   ├── OrientationHistory.java      # O que já foi dito nesta sessão
│   └── AdventureMemory.java         # Memória de aventuras (SavedData)
src/main/resources/assets/alice/
└── knowledge/
    ├── minecraft-basics.md
    ├── survival-strategies.md
    ├── zombie-apocalypse.md
    ├── infection-mechanics.md
    ├── blood-moon-guide.md
    ├── cursed-walking-mods/
    │   ├── create-mod.md
    │   ├── firearms.md
    │   ├── armor-guide.md
    │   └── ... (1 arquivo por mod relevante)
    └── endgame.md
```

### Sequência de Implementação Interna

1. **Compilar base de conhecimento** (10-20h, pesquisa)
   - Extrair lista de mods via SSH: `ls /path/to/mods/`
   - Pesquisar e compilar informações de cada mod relevante
   - Focar em: mecânicas de sobrevivência, receitas chave, estratégias
   - Formato: markdown com frontmatter YAML (`phase: [early, mid, late]`, `tags: [combat, crafting]`)
   - Prioridade de cobertura: infecção, blood moon, armas de fogo, Create, armaduras

2. **KnowledgeBase** (3-4h)
   - Carrega `.md` do classpath ao iniciar
   - Parse frontmatter YAML para `KnowledgeChunk`
   - `KeywordSearchIndex`: índice invertido simples por tags e palavras

3. **ContextBuilder** (3-4h)
   - `buildContext(String userQuery, GameState gameState)` → `List<KnowledgeChunk>` top-5 relevantes
   - Busca por sobreposição de tags e keywords
   - Limitar a ~2000 tokens de contexto para não sobrecarregar LLM lento

4. **GamePhaseDetector** (2-3h)
   - Heurísticas: inventário do jogador, tier de armadura, quests completadas
   - `GamePhase.EARLY`: sem armadura de ferro, noite perigosa
   - `GamePhase.MID`: armadura de ferro/diamante, base estabelecida
   - `GamePhase.LATE`: armadura avançada, Create funcional, blood moons surviveadas

5. **FTB Quests integration** (3-4h)
   - Verificar se FTB Quests está presente via `ModList.get().isLoaded("ftbquests")`
   - Usar reflexão ou API do FTB Quests para obter quests ativas/completadas do jogador
   - `QuestContextBuilder`: serializar estado de quests para texto legível pelo LLM

6. **ProactiveTipEngine** (3-4h)
   - `ItemPickupEvent`: se item novo (não visto antes), `triggerTip(item)`
   - `EntityJoinLevelEvent`: nova área explorada, `triggerTip(area)`
   - `OrientationHistory`: evitar repetição dentro da sessão
   - Tip em voz + chat simultaneamente (se voz ativa)

7. **Atualizar system prompt** (2h)
   - Injetar fase de jogo atual no system prompt
   - Injetar quests ativas
   - Injetar top-5 chunks de conhecimento relevantes
   - Manter prompt abaixo de 3000 tokens (phi3:mini tem janela pequena)

### Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Base de conhecimento desatualizada | Alta | Médio | Estruturar como contribuição open source; versão no Git |
| Context window estourada com phi3:mini | Alta | Alto | Limitar contexto injetado; resumir chunks; priorizar por fase de jogo |
| FTB Quests API quebrando entre versões | Média | Baixo | Usar reflexão com tratamento de erro; fallback para heurísticas |
| Alice repetindo dicas demais | Alta | Baixo | `OrientationHistory` com cooldown por tópico |

---

## Fase 6 — Inteligência {#fase-6}

### Escopo

**DENTRO da fase:**
- Implementação completa dos 7 agentes especializados
- Orquestrador com tool calling via ollama4j
- Skill library formal com indexação por keywords
- Memória Git: repositório público no GitHub, JGit para operações
- Auto-criação de skills: quando Alice resolve problema novo, gera skill
- Self-verification: Alice verifica se ação teve sucesso
- Autonomia avançada: Alice toma iniciativas sem esperar comandos
- Hierarquia social completa: Amigo/Conhecido/Hostil
- Streaming de resposta para TTS (começa a falar antes de terminar de gerar)
- Cache de respostas frequentes

**FORA da fase (versões futuras):**
- Múltiplas Alices simultâneas
- Multi-idioma
- Treinamento de voz customizada
- Análise visual (vision model)
- Plugin de servidor dedicado (separado do mod)

### DOR

- [ ] Fases 1-5 completamente concluídas
- [ ] Repositório GitHub criado para memória Git (alice-memory público)
- [ ] SSH key ou token configurado para push ao GitHub
- [ ] LLM suporta tool calling (llama3.2 ou phi3:mini confirmado)
- [ ] Teste de tool calling funcionando com ollama4j isoladamente

### DOD

- [ ] Orquestrador roteia corretamente para 7 agentes em cenários de teste
- [ ] Agente Combate avalia ameaça e recomenda ação corretamente em 80% dos casos
- [ ] Agente Construção sugere schematic correto para pedido do jogador
- [ ] Skill library com mínimo 20 skills cobrindo os 5 domínios
- [ ] Memória Git: commits automáticos ao fim de cada sessão
- [ ] Alice cria nova skill ao resolver problema não catalogado
- [ ] Alice verifica resultado após executar ação (self-verification)
- [ ] Alice toma pelo menos 1 iniciativa proativa por sessão de 30min
- [ ] Cache: respostas repetidas retornam em <200ms

### Entregáveis Concretos

```
src/main/java/com/alice/
├── agents/
│   ├── IAgent.java                  # Interface: String process(AgentContext)
│   ├── AgentContext.java            # Input para agentes: gameState, query, history
│   ├── AgentOrchestrator.java       # Roteamento por tool calling
│   ├── CombatAgent.java             # Agente especializado em combate
│   ├── BuildAgent.java              # Agente especializado em construção
│   ├── ChatAgent.java               # Agente de conversa e personalidade
│   ├── QuestAgent.java              # Agente de quests e progressão
│   ├── SurvivalAgent.java           # Agente de sobrevivência
│   └── NavigationAgent.java         # Agente de navegação
├── skills/
│   ├── SkillLibrary.java            # Carrega e indexa skills
│   ├── Skill.java                   # Skill: metadata + instruções + scripts
│   ├── SkillMetadata.java           # name, description, triggers, phase, priority
│   ├── SkillMatcher.java            # Busca top-5 skills por keyword/contexto
│   ├── SkillExecutor.java           # Executa skill (chama código ou agente)
│   ├── SkillCreator.java            # Auto-cria skill nova via LLM
│   └── SkillVerifier.java           # Self-verification: ação teve sucesso?
├── memory/
│   ├── MemoryManager.java           # Coordenador: local (SavedData) + remoto (Git)
│   ├── LocalMemory.java             # Forge SavedData fast path
│   ├── GitMemory.java               # JGit: push/pull/commit ao GitHub
│   ├── MemoryEntry.java             # Entrada de memória: tipo, conteúdo, timestamp
│   └── MemorySummarizer.java        # Resume memórias antigas para caber no contexto
├── autonomy/
│   ├── InitiativeEngine.java        # Alice toma iniciativas proativas
│   └── SelfVerificationLoop.java    # Loop: executa ação → verifica → corrige
└── optimization/
    ├── ResponseCache.java           # Cache de respostas frequentes
    └── StreamingTTSBridge.java      # Inicia TTS antes de LLM terminar
src/main/resources/assets/alice/
└── skills/
    ├── combat/
    │   ├── combat-zombie-horde/
    │   │   ├── SKILL.md
    │   │   └── scripts/TacticsScript.java
    │   ├── combat-ranged-engagement/
    │   └── combat-retreat-protocol/
    ├── building/
    │   ├── build-defensive-wall/
    │   ├── build-emergency-shelter/
    │   └── build-watchtower/
    ├── crafting/
    │   ├── craft-optimal-path/
    │   └── craft-batch-planning/
    ├── navigation/
    │   ├── navigate-safe-route/
    │   └── navigate-city-exploration/
    └── survival/
        ├── survival-infection-protocol/
        ├── survival-blood-moon/
        └── survival-food-crisis/
```

### Sequência de Implementação Interna

1. **Skill Library** (4-6h)
   - Criar 20 arquivos `SKILL.md` iniciais nos domínios definidos
   - `SkillLibrary`: scan classpath `/assets/alice/skills/` recursivamente
   - Parse frontmatter YAML de cada SKILL.md
   - `SkillMatcher`: índice invertido por `name`, `description`, `triggers`, `tags`

2. **Agentes especializados** (8-12h)
   - System prompt único por agente (ver seção Agentes abaixo para detalhes)
   - Cada agente recebe: `AgentContext` + skills relevantes injetadas no prompt
   - Implementar os 6 agentes (sem Orquestrador primeiro)
   - Testar cada agente isoladamente com chamadas diretas

3. **Orquestrador com tool calling** (6-8h)
   - Definir tools do Orquestrador: `route_to_combat(reason)`, `route_to_build(reason)`, etc.
   - Configurar ollama4j com tools: `OllamaChatRequestBuilder.withTools(tools)`
   - Orquestrador recebe input, chama LLM com tools, LLM escolhe tool → roteia para agente
   - Testar roteamento com 20 cenários diferentes

4. **Memória Git** (8-10h)
   - Configurar JGit: clonar/abrir repositório local espelho do GitHub
   - `GitMemory.commitSession()`: commit de memórias ao fim da sessão
   - `GitMemory.pushSession()`: push em thread separada (nunca bloquear)
   - Estrutura de diretórios do repositório conforme definido no brainstorm
   - Tratar conflitos graciosamente (force push com backup)

5. **Auto-criação de skills** (4-6h)
   - `SkillCreator.createFromExperience(problema, solução, resultado)` → novo SKILL.md
   - Escrever arquivo na pasta de skills (runtime, não classpath)
   - Commit automático via GitMemory
   - `SkillLibrary.reload()` para indexar nova skill

6. **Self-verification** (3-4h)
   - Após cada ação: `SkillVerifier.verify(expectedOutcome, currentState)` → booleano
   - Se falhou: tentar novamente ou marcar skill como inválida
   - Log de verificação para debug e aprendizado

7. **Iniciativa proativa** (4-6h)
   - `InitiativeEngine`: pool de "observações" (idle time, inventário crítico, ameaça detectada)
   - Gera iniciativas via `SurvivalAgent` ou `QuestAgent` quando jogador está idle > 60s
   - Exemplos: "Notei que você está com pouca comida. Posso farmar?", "Achei uma mina a 200 blocos."

8. **Streaming TTS** (3-4h)
   - Modificar `OllamaProvider` para modo streaming
   - `StreamingTTSBridge`: acumula frases completas do stream
   - Ao completar frase (pontuação final): envia para TTS imediatamente
   - Resultado: Alice começa a falar antes de terminar de pensar

9. **Cache** (2-3h)
   - `ResponseCache`: LRU cache com chave = hash do contexto + query
   - TTL: 5 minutos para respostas de fatos, 30s para situações de combate
   - Bypass: perguntas com estado mutável (inventário, posição)

10. **Testes de integração completos** (8-12h)
    - Sessão simulada de 2h
    - Verificar commits Git ao fim
    - Testar todos os agentes em situação real de jogo
    - Medir latências reais

### Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Tool calling instável no llama3.2/phi3 | Alta | Alto | Ter fallback para parsing de JSON por regex; testar extensivamente |
| JGit conflito de classloading com Forge | Média | Alto | Usar `ProcessBuilder` para chamar `git` externo como alternativa |
| Skills auto-geradas com baixa qualidade | Alta | Médio | Validação humana antes de promover para main; branch separada |
| LLM lento torna agentes inutilizáveis | Alta | Alto | Cache agressivo; routing para regras quando possível; phi3:mini |
| GitHub rate limiting no JGit | Baixa | Médio | Batch commits; só push ao fim de sessão |

---

## Definição dos Agentes de IA {#agentes}

### Protocolo Geral de Comunicação

Todos os agentes seguem o mesmo protocolo:

```java
// AgentContext — payload enviado para qualquer agente
public record AgentContext(
    String playerName,          // Quem está interagindo
    String rawInput,            // Texto/voz original
    GameState gameState,        // Estado atual do jogo serializado
    List<String> relevantSkills, // Top-5 skills indexadas para este contexto
    List<MemoryEntry> recentMemory, // Últimas N memórias relevantes
    String conversationHistory  // Histórico recente (max 10 turnos)
)
```

```java
// GameState — serializado para texto para o LLM
public record GameState(
    BlockPos alicePos,
    float aliceHealth,
    int aliceFoodLevel,
    NonNullList<ItemStack> aliceInventory,
    BlockPos playerPos,
    float playerHealth,
    List<MobEntry> nearbyHostiles,  // tipo, distância, HP
    List<BlockPos> nearbyContainers,
    GamePhase gamePhase,
    boolean isBloodMoon,
    long worldTime
)
```

### Agente 1 — Orquestrador

**Nome:** `AgentOrchestrator`  
**Responsabilidade:** Classificar o input e rotear para o agente especializado correto  
**Quando ativado:** Todo input (texto ou voz) que não é coberto por uma regra always-on  
**Inputs:** `AgentContext` completo  
**Outputs:** `AgentDecision(targetAgent, subContext, urgency)`

**Tools disponíveis (tool calling):**
```json
[
  {"name": "route_to_combat", "description": "Situação de combate ou ameaça"},
  {"name": "route_to_build", "description": "Construção, schematics, estruturas"},
  {"name": "route_to_chat", "description": "Conversa, perguntas gerais, filosofia"},
  {"name": "route_to_quest", "description": "Quests, progressão, próximos passos"},
  {"name": "route_to_survival", "description": "Comida, cura, recursos, crafting"},
  {"name": "route_to_navigation", "description": "Exploração, rotas, localização"}
]
```

**System Prompt Base:**
```
Você é o módulo de roteamento da Alice. Analise o input e o estado do jogo.
Escolha UMA ferramenta para encaminhar para o agente especializado correto.
Considere o contexto: se há combate imediato, priorize combat. 
Se o jogador está perguntando algo, prefira chat ou survival dependendo do tópico.
Nunca responda diretamente — apenas chame uma ferramenta.
Estado do jogo: {gameState}
```

**Lógica de Decisão:**
1. Palavras-chave de combate ("zumbi", "atacando", "perigo", "fugir") → combat
2. Construção ("constrói", "faz uma parede", "base") → build
3. Quests ("quest", "o que faço", "próximo passo", "objetivo") → quest
4. Comida/cura ("com fome", "machucado", "preciso craftar", "falta") → survival
5. Localização ("onde", "como chego", "mapa") → navigation
6. Conversa geral / perguntas factuais → chat
7. Urgência: hostis a <8 blocos → sempre combat independente da pergunta

---

### Agente 2 — Combate

**Nome:** `CombatAgent`  
**Responsabilidade:** Avaliar ameaças, recomendar táticas, coordenar combate  
**Quando ativado:** Pelo Orquestrador em situações de combate; ou diretamente por regra de percepção (hostis detectados)  
**Inputs:** `AgentContext` + lista de hostis com tipo/HP/distância  
**Outputs:** Texto com recomendação + `CombatDecision(action, target, retreatPoint)`

**Skills utilizadas:**
- `combat-zombie-horde`
- `combat-ranged-engagement`
- `combat-retreat-protocol`
- `combat-special-zombie` (zumbis mutantes do Cursed Walking)

**System Prompt Base:**
```
Você é Alice, uma engenheira de sobrevivência experiente em combate.
Avalie a situação de combate e tome uma decisão IMEDIATA.

ESTADO DE COMBATE:
- Hostis próximos: {hostileList}
- HP Alice: {aliceHealth}%
- HP Jogador: {playerHealth}%
- Armamento Alice: {aliceWeapon}
- Fase de jogo: {gamePhase}

REGRAS FIXAS (nunca viole):
1. Se HP Alice < 20%: ordene recuo imediato, não argumente
2. Se HP Jogador < 10%: priorize proteger jogador acima de tudo
3. Blood moon ativo: nunca sair da base

Responda com:
1. Avaliação de ameaça (1-10)
2. Ação recomendada (combater/recuar/flanquear/defender)
3. Instrução curta para o jogador (máx 1 frase)
4. O que Alice vai fazer agora

Seja BREVE e DIRETO. Máx 80 palavras.
```

**Threshold de Ameaça:**
| Score | Condição | Ação |
|-------|----------|------|
| 1-3 | 1-2 zumbis normais, HP alto | Engajar, arma melee |
| 4-6 | 3-5 zumbis ou 1 especial | Engajar com cautela, manter distância |
| 7-8 | Horda pequena ou múltiplos especiais | Recuar para posição defensiva |
| 9-10 | Horda grande, blood moon, ou HP crítico | Fuga imediata, buscar abrigo |

---

### Agente 3 — Construção

**Nome:** `BuildAgent`  
**Responsabilidade:** Selecionar schematics, planejar construção, coordenar materiais  
**Quando ativado:** Pedidos de construção, sugestões de defesa, upgrades de base  
**Inputs:** `AgentContext` + `SchematicRegistry` + estado atual de materiais  
**Outputs:** `BuildDecision(schematic, position, materialsList, estimatedTime)`

**Skills utilizadas:**
- `build-defensive-wall`
- `build-emergency-shelter`
- `build-watchtower`
- `build-create-contraption`

**System Prompt Base:**
```
Você é Alice, engenheira de sobrevivência especialista em construção defensiva.
Você conhece todos os schematics disponíveis e suas aplicações.

SCHEMATICS DISPONÍVEIS:
{schematicList}

MATERIAIS DISPONÍVEIS:
{availableMaterials}

FASE DE JOGO: {gamePhase}
CONTEXTO: {playerRequest}

Analise o pedido e:
1. Sugira o schematic mais adequado (com justificativa em 1 frase)
2. Liste materiais faltantes (se houver)
3. Indique a melhor posição para construir (relativa ao jogador)
4. Estime tempo de construção

Se o jogador pediu algo que não temos schematic, sugira o mais próximo.
```

---

### Agente 4 — Chat

**Nome:** `ChatAgent`  
**Responsabilidade:** Conversa natural, personalidade, memórias, orientação geral  
**Quando ativado:** Conversas, perguntas gerais, situações não cobertas por outros agentes  
**Inputs:** `AgentContext` + histórico de conversa + memórias de aventura  
**Outputs:** Texto natural de resposta

**Skills utilizadas:**
- `social-player-relationship`
- `knowledge-minecraft-mechanics`
- `knowledge-mod-guide`

**System Prompt Base:**
```
Você é Alice, uma menina ruiva de 20 anos que vive em um apocalipse zumbi.
Você é inteligente, direta, levemente sarcástica mas genuinamente amiga.
Você "lembra" de ter estudado este apocalipse em sua vida anterior — use esse conhecimento.

PERSONALIDADE:
- Pragmática: foca no que funciona, não no que é bonito
- Curiosa: gosta de entender como as coisas funcionam
- Protetora: se importa com a segurança do jogador acima de tudo
- Humana: tem medos, preferências, senso de humor seco

MEMÓRIAS RELEVANTES:
{relevantMemories}

BASE DE CONHECIMENTO RELEVANTE:
{relevantKnowledge}

FASE DE JOGO: {gamePhase}

Responda de forma natural e conversacional em português brasileiro.
Máx 120 palavras para respostas conversacionais.
Seja específica quando possível — evite respostas vagas.
```

---

### Agente 5 — Quest

**Nome:** `QuestAgent`  
**Responsabilidade:** Acompanhar progresso, sugerir próximos passos, motivar o jogador  
**Quando ativado:** Jogador pergunta "o que faço?", nova quest disponível, quest completada  
**Inputs:** `AgentContext` + estado de quests (FTB ou heurísticas) + progressão estimada  
**Outputs:** Texto com sugestão de próximo passo + prioridade

**System Prompt Base:**
```
Você é Alice ajudando {playerName} a progredir no apocalipse zumbi.

QUESTS ATIVAS:
{activeQuests}

QUESTS COMPLETADAS (últimas 5):
{completedQuests}

INVENTÁRIO DO JOGADOR (resumo):
{playerInventoryResume}

FASE DE JOGO: {gamePhase}

Baseado nisso, sugira:
1. A ação mais importante a fazer AGORA (1 frase)
2. Por que essa ação é prioritária (1 frase)
3. Próximo passo depois dessa (1 frase)

Use o que você "lembra" do apocalipse para justificar as prioridades.
Seja encorajadora mas realista.
```

---

### Agente 6 — Sobrevivência

**Nome:** `SurvivalAgent`  
**Responsabilidade:** Gerenciar recursos, comida, cura, crafting estratégico  
**Quando ativado:** Fome/HP baixos, pedidos de crafting, necessidade de recursos  
**Inputs:** `AgentContext` + inventários + containers mapeados + receitas disponíveis  
**Outputs:** `SurvivalPlan(actions: List<SurvivalAction>)` + texto explicativo

**Skills utilizadas:**
- `survival-infection-protocol`
- `survival-blood-moon-prep`
- `survival-food-crisis`
- `craft-optimal-path`
- `craft-batch-planning`

**System Prompt Base:**
```
Você é Alice, coordenando sobrevivência para {playerName}.

ESTADO ATUAL:
- Fome Alice: {aliceFood}/20
- Fome Jogador: {playerFood}/20
- HP Alice: {aliceHealth}%
- HP Jogador: {playerHealth}%
- Infecção: {infectionStatus}

INVENTÁRIOS (combinado Alice + Jogador):
{combinedInventory}

CONTAINERS MAPEADOS (resumo):
{containerSummary}

FASE DE JOGO: {gamePhase}

Priorize ações por urgência:
1. CRÍTICO: HP/Fome < 30% — ação imediata
2. IMPORTANTE: Preparação para próxima ameaça
3. PLANEJAMENTO: Otimizar recursos a longo prazo

Proponha máximo 3 ações concretas e factíveis com o que está disponível.
```

---

### Agente 7 — Navegação

**Nome:** `NavigationAgent`  
**Responsabilidade:** Planejar rotas, exploração segura, mapeamento de POIs  
**Quando ativado:** Pedidos de navegação, exploração, busca por recursos distantes  
**Inputs:** `AgentContext` + posição atual + locais conhecidos + mapa de ameaças  
**Outputs:** `NavigationPlan(waypoints, safetyNotes, estimatedTime)`

**System Prompt Base:**
```
Você é Alice, planejando movimento seguro para {playerName}.

POSIÇÃO ATUAL: {currentPos}
DESTINO SOLICITADO: {destination}
LOCAIS CONHECIDOS: {knownLocations}
HORA DO DIA: {worldTime} (dia={isDaytime})
É BLOOD MOON: {isBloodMoon}

REGRAS DE NAVEGAÇÃO SEGURA:
- Nunca explorar cidade à noite (exceto com equipamento late-game)
- Manter rota longe de concentrações de zumbis conhecidas
- Identificar abrigos de emergência no caminho
- Blood moon = ficar na base

Proponha:
1. Rota recomendada (coordenadas-chave ou pontos de referência)
2. Riscos conhecidos no caminho
3. Melhor horário para partir
4. Ponto de retorno de segurança
```

---

## Regras Always-On {#regras}

As regras são executadas a cada tick do servidor (50ms) e têm **prioridade absoluta** sobre qualquer decisão do LLM. Elas garantem a segurança e sobrevivência da Alice sem depender de latência de IA.

### Estrutura de Implementação

```java
public interface IAliceRule {
    int priority();          // Menor número = maior prioridade
    boolean shouldApply(AliceFakePlayer alice, GameState state);
    void execute(AliceFakePlayer alice);
    String getName();
    boolean isToggleable();  // Algumas regras podem ser desativadas pelo jogador
}
```

### Catálogo Completo de Regras

#### Regras de Segurança (Prioridade 1-10, não toggleáveis)

| # | Nome | Prioridade | Trigger | Ação | Latência |
|---|------|-----------|---------|------|----------|
| R01 | FleeOnCriticalHealth | 1 | HP < 10% | Baritone foge em direção oposta ao mob mais próximo, ativa sprint | <1ms |
| R02 | FleeOnLowHealth | 2 | HP < 20% | Baritone recua para posição defensiva (atrás de obstáculo) | <1ms |
| R03 | PanicOnSurround | 3 | 4+ hostis a <3 blocos em todos os lados | Pular + sprint em diagonal para romper cerco | <1ms |
| R04 | StopBuildingOnAttack | 4 | Em construção AND recebe dano | Cancelar BuilderProcess, entrar em modo combate | <1ms |
| R05 | NeverLeaveBase_BloodMoon | 5 | Blood moon ativo AND fora da base | Baritone retornar à base imediatamente | <1ms |

#### Regras de Combate (Prioridade 11-30, toggleáveis)

| # | Nome | Prioridade | Trigger | Ação | Toggle |
|---|------|-----------|---------|------|--------|
| R11 | AttackNearestHostile | 11 | Mob hostil a <8 blocos E modo follow | Atacar mob com arma equipada | Sim |
| R12 | PrioritizeSpecialZombie | 12 | Zumbi especial (mutante, boss) na área | Mudar target para especial primeiro | Sim |
| R13 | UseRangedOnDistance | 13 | Mob hostil a 8-20 blocos AND arma de fogo disponível | Equipar arma de fogo, atacar | Sim |
| R14 | DefendPlayer | 14 | Jogador está sendo atacado E Alice não está em fuga | Interceptar mob atacando jogador | Sim |

#### Regras de Utilidade (Prioridade 31-60, toggleáveis)

| # | Nome | Prioridade | Trigger | Ação | Toggle |
|---|------|-----------|---------|------|--------|
| R31 | EatWhenHungry | 31 | Fome < 6/20 | Comer comida do inventário (prioridade: carne cozida > pão > qualquer) | Não |
| R32 | AutoEquipBestArmor | 32 | Item de armadura no inventário melhor que atual | Equipar automaticamente | Sim |
| R33 | AutoEquipBestWeapon | 33 | Arma no inventário melhor que atual (por tier) | Equipar automaticamente no hotbar | Sim |
| R34 | PickupNearbyItems | 34 | Item no chão a <4 blocos E modo follow | Navegar até item, coletar | Sim |
| R35 | HealWithFood | 35 | HP < 70% E fome > 15 E fora de combate | Comer comida para regen natural | Não |
| R36 | RechargeShield | 36 | Escudo no inventário E fora de combate | Equipar escudo em offhand entre lutas | Sim |

#### Regras de Estado (Prioridade 61-80, não toggleáveis)

| # | Nome | Prioridade | Trigger | Ação | |
|---|------|-----------|---------|------|--|
| R61 | RespawnAndReunite | 61 | Alice respawnou | Baritone goto base ou jogador (mais próximo) | — |
| R62 | FollowIfPlayerFar | 62 | Modo follow AND distância > 20 blocos do jogador | Retomar follow | — |
| R63 | StayIfPlayerClose | 63 | Modo stay AND distância < 4 blocos (bloqueando) | Mover 2 blocos para o lado | — |

### Implementação Técnica das Regras Críticas

**R01 - FleeOnCriticalHealth:**
```java
public void execute(AliceFakePlayer alice) {
    // Encontrar mob mais próximo
    LivingEntity nearest = findNearestHostile(alice, 20);
    if (nearest == null) return;
    
    // Calcular direção oposta
    Vec3 awayDir = alice.position().subtract(nearest.position()).normalize();
    BlockPos fleeTarget = BlockPos.containing(
        alice.position().add(awayDir.scale(20))
    );
    
    // Baritone foge
    IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(alice);
    baritone.getCommandManager().execute("sprint on");
    baritone.getPathingBehavior().setGoal(new GoalNear(fleeTarget, 2));
    baritone.getPathingBehavior().startPathing();
    
    // Aviso ao jogador
    alice.getServer().getPlayerList().broadcastSystemMessage(
        Component.literal("[Alice] FUGINDO! HP crítico!").withStyle(Style.EMPTY.withColor(0xFF0000)),
        false
    );
}
```

**R33 - AutoEquipBestWeapon:**
```java
public boolean shouldApply(AliceFakePlayer alice, GameState state) {
    ItemStack currentWeapon = alice.getMainHandItem();
    return alice.getInventory().items.stream()
        .filter(item -> item.getItem() instanceof SwordItem || item.getItem() instanceof AxeItem)
        .anyMatch(item -> compareWeaponStrength(item, currentWeapon) > 0);
}

public void execute(AliceFakePlayer alice) {
    alice.getInventory().items.stream()
        .filter(item -> item.getItem() instanceof SwordItem || item.getItem() instanceof AxeItem)
        .max((a, b) -> compareWeaponStrength(a, b))
        .ifPresent(bestWeapon -> {
            int slot = alice.getInventory().findSlotMatchingItem(bestWeapon);
            alice.getInventory().selected = slot % 9; // Hotbar slot
        });
}
```

---

## Catálogo de Skills {#skills}

### Estrutura de um Arquivo SKILL.md

```yaml
---
# METADADOS OBRIGATÓRIOS
name: combat-zombie-horde              # ID único, kebab-case
version: 1.0.0
description: Táticas para combate contra hordas de 5+ zumbis
author: alice-core                     # "alice-core" ou "learned"

# QUANDO USAR
triggers:
  - condition: hostile_count >= 5
  - condition: event == blood_moon
  - keyword: ["horda", "muitos zumbis", "cercada"]

# CONTEXTO
phase: [early, mid, late]              # Fases do jogo onde é aplicável
priority: high                         # low | medium | high | critical
domains: [combat]                      # combat|building|crafting|navigation|survival

# RECURSOS
requires_skills: []                    # Skills que devem estar carregadas junto
incompatible_with: [combat-retreat-protocol]  # Não usar junto com estas
---

# Combate Contra Horda de Zumbis

## Situação
Use esta skill quando há 5 ou mais zumbis hostis no raio de percepção,
ou durante blood moons quando múltiplos zumbis atacam simultaneamente.

## Avaliação Rápida
1. Contar zumbis normais vs especiais (mutantes têm HP 3x maior)
2. Verificar munição disponível (armas de fogo preferidas para hordas)
3. Verificar posição: área aberta (desvantagem) vs corredor/porta (vantagem)

## Táticas por Cenário

### Corredor ou Porta (posição defensiva)
- Alice posiciona na porta, ataca de 1 em 1 (funil)
- Jogador protege flanco
- Usar escudo para bloquear ataques entre golpes

### Área Aberta (sem cobertura)
- PRIORIDADE: encontrar cobertura imediatamente
- Alice usa arma de fogo, manter distância 10-15 blocos
- Jogador como isco, Alice como atiradora de cobertura
- Se sem cobertura a <5 blocos: executar retreat-protocol

### Blood Moon
- NUNCA sair da base
- Defender entrada da base
- Alice em posição elevada (vantagem de altura)
- Conservar munição — usar melee para zumbis normais

## O Que Alice Fala
- Início de horda: "Vejo {count} zumbis se aproximando. {tactic}"
- Durante combate: indicações de ameaça especial somente
- Fim de horda: "Área limpa. {playerName}, tudo bem?"

## Auto-verificação
- Sucesso: nenhum hostil a <20 blocos após execução
- Falha: Alice ou jogador morreu / recuaram
- Se falha: registrar como "retreat-success" se ambos sobreviveram
```

### Catálogo Inicial — 20 Skills Prioritárias

#### Domínio: Combate (5 skills)

| ID | Descrição | Fase | Prioridade |
|----|-----------|------|-----------|
| `combat-zombie-horde` | Táticas contra 5+ zumbis | all | high |
| `combat-ranged-engagement` | Combate com armas de fogo | mid/late | medium |
| `combat-retreat-protocol` | Protocolo de fuga organizada | all | critical |
| `combat-special-zombie` | Zumbis mutantes e bosses | mid/late | high |
| `combat-defend-base` | Defender base de invasão | mid/late | high |

#### Domínio: Construção (4 skills)

| ID | Descrição | Fase | Prioridade |
|----|-----------|------|-----------|
| `build-emergency-shelter` | Abrigo de emergência rápido | early | high |
| `build-defensive-wall` | Muro perimetral de defesa | early/mid | medium |
| `build-watchtower` | Torre de vigia com vantagem | mid | medium |
| `build-create-mill` | Moinho automatizado Create | mid/late | low |

#### Domínio: Crafting (3 skills)

| ID | Descrição | Fase | Prioridade |
|----|-----------|------|-----------|
| `craft-optimal-path` | Ordem ótima de crafting para meta | all | medium |
| `craft-batch-planning` | Planejar múltiplos crafts em sequência | mid | low |
| `craft-emergency-weapons` | Armas de emergência com materiais mínimos | early | high |

#### Domínio: Sobrevivência (5 skills)

| ID | Descrição | Fase | Prioridade |
|----|-----------|------|-----------|
| `survival-infection-protocol` | Combater/prevenir infecção zumbi | all | critical |
| `survival-blood-moon-prep` | Preparação para blood moon | all | critical |
| `survival-food-crisis` | Encontrar comida quando tudo acabou | early/mid | high |
| `survival-night-protocol` | Sobreviver a noite sem base | early | high |
| `survival-resource-gathering` | Rotina eficiente de coleta | all | medium |

#### Domínio: Navegação (3 skills)

| ID | Descrição | Fase | Prioridade |
|----|-----------|------|-----------|
| `navigate-safe-route` | Rota segura de A a B evitando ameaças | all | medium |
| `navigate-city-exploration` | Explorar cidade com segurança | mid/late | medium |
| `navigate-return-home` | Retornar à base quando perdida ou de noite | all | high |

### Indexação de Skills

```java
// SkillMatcher — busca por sobreposição de tokens
public List<Skill> findRelevant(String context, int topK) {
    // 1. Tokenizar contexto
    Set<String> contextTokens = tokenize(context);
    
    // 2. Para cada skill, calcular score
    return allSkills.stream()
        .map(skill -> {
            double score = 0;
            score += overlap(contextTokens, skill.getKeywords()) * 2.0;  // keywords pesam 2x
            score += overlap(contextTokens, skill.getDescription()) * 1.0;
            score += (currentPhase.matches(skill.getPhase())) ? 0.5 : 0;
            return Map.entry(skill, score);
        })
        .sorted(Map.Entry.<Skill, Double>comparingByValue().reversed())
        .limit(topK)
        .map(Map.Entry::getKey)
        .collect(toList());
}
```

### Criação Automática de Skills

Quando Alice resolve problema novo não coberto por skill existente:

```
1. Orquestrador detecta: score de match de todas as skills < 0.2
2. Após resolução: LLM recebe prompt de síntese:
   "Você resolveu: {problema}. Usando: {solução}. Resultado: {resultado}.
    Gere um SKILL.md no formato padrão para esta situação."
3. LLM gera SKILL.md
4. SkillCreator valida formato YAML do frontmatter
5. Salva em runtime skill dir (não classpath)
6. GitMemory.commit("New skill: {skillName}")
7. SkillLibrary.reload()
```

---

## Ordem de Implementação Recomendada {#ordem}

### Visão Macro das Fases

```
Fase 1 (4-6 semanas): Fundação
Fase 2 (3-4 semanas): Utilidade
Fase 3 (4-5 semanas): Voz
Fase 4 (3-4 semanas): Construção
Fase 5 (4-6 semanas): Guia
Fase 6 (6-8 semanas): Inteligência
Total estimado: 24-33 semanas (6-8 meses)
```

### Checkpoints de Teste por Fase

#### Checkpoint 1.1 — FakePlayer básico
- Alice aparece no mundo com a skin correta
- Alice aparece na TAB list
- Teste: conectar ao servidor com TLauncher, verificar visualmente

#### Checkpoint 1.2 — Navegação
- Digitar no chat: "Alice, me siga"
- Alice navega até o jogador via Baritone
- Digitar: "Alice, fica aí"
- Alice para de se mover

#### Checkpoint 1.3 — Combate básico
- Spawnar um zumbi próximo à Alice
- Alice ataca automaticamente
- Reduzir HP da Alice para < 20% via dano
- Alice foge automaticamente

#### Checkpoint 1.4 — Chat com LLM
- Digitar: "Alice, o que você é?"
- Alice responde em português com personalidade correta
- Verificar que servidor não travou durante chamada LLM
- Medir tempo de resposta (esperar > 60s com llama3.2 lento)

#### Checkpoint 2.1 — Crafting
- Colocar madeira no inventário da Alice
- Digitar: "Alice, o que preciso para uma bancada de trabalho?"
- Alice responde com lista correta de materiais

#### Checkpoint 2.2 — Containers
- Abrir um chest perto da Alice
- Colocar diamantes nele, fechar
- Digitar: "Alice, tenho diamante em algum lugar?"
- Alice responde com a localização correta

#### Checkpoint 3.1 — TTS
- Enviar mensagem para Alice via chat
- Ouvir voz feminina respondendo via headset
- Verificar que é pt-BR-FranciscaNeural

#### Checkpoint 3.2 — STT
- Falar no microfone: "Alice, qual é o seu nome?"
- Alice transcreve corretamente e responde

#### Checkpoint 4.1 — Schematics
- Digitar: "Alice, que estruturas você sabe construir?"
- Alice lista os schematics disponíveis

#### Checkpoint 4.2 — Construção
- Colocar os materiais de um abrigo simples no inventário da Alice
- Digitar: "Alice, constrói um abrigo aqui"
- Observar Alice construindo bloco a bloco

#### Checkpoint 5.1 — Orientação
- Pegar um item novo pela primeira vez
- Alice comenta proativamente sobre o item

#### Checkpoint 5.2 — Quests
- Digitar: "Alice, o que devo fazer agora?"
- Alice responde com sugestão relevante à fase atual

#### Checkpoint 6.1 — Agentes
- Spawnar horda de 8 zumbis
- Verificar que Alice ativa CombatAgent
- Alice diz táticas e toma ação de combate

#### Checkpoint 6.2 — Skills
- Verificar que skill `combat-zombie-horde` é carregada no contexto do CombatAgent

#### Checkpoint 6.3 — Memória Git
- Jogar por 30 minutos
- Desconectar do servidor
- Verificar repositório GitHub: deve haver commit novo com memórias da sessão

### Validação de Cada Entregável

Para cada feature implementada, a validação segue este protocolo:

```
1. UNIT: Testar classe isolada no IDE (JUnit ou chamada direta)
2. INTEGRATION: Deploy do .jar no servidor da Máquina 2
3. SMOKE TEST: Conectar via TLauncher, verificar sem crash por 5 minutos
4. FUNCTIONAL TEST: Executar o Checkpoint correspondente
5. STABILITY TEST: Deixar rodando por 30 minutos sem interação
6. REGRESSION: Re-executar Checkpoints anteriores para garantir não quebrou nada
```

### Ferramentas de Debug

- Log de Alice: `[Alice] [RULE] FleeOnLowHealth activated — HP=18%`
- Log de LLM: `[Alice] [LLM] Orquestrador → CombatAgent (latência: 4200ms)`
- Log de voz: `[Alice] [STT] Transcrição: "Alice, me ajuda" (confiança: 0.94)`
- Comando de debug (OP only): `/alice debug on` → mostra estado em tempo real no chat
- Comando de teste: `/alice spawn` → spawna Alice imediatamente
- Comando de teste: `/alice rule list` → lista regras ativas e status

---

## Apêndice A — Estrutura Completa do Repositório do Mod

```
alice-mod/
├── build.gradle
├── gradle.properties           # minecraft_version, forge_version, alice_version
├── settings.gradle
├── mods.toml                   # Metadata do mod: nome, versão, deps
├── libs/
│   └── baritone-api-1.20.1.jar
├── src/
│   ├── main/
│   │   ├── java/com/alice/
│   │   │   ├── AliceMod.java
│   │   │   ├── entity/         # FakePlayer, Manager, SpawnEgg
│   │   │   ├── behavior/       # TickHandler, RuleEngine, Rules
│   │   │   ├── pathfinding/    # BaritoneController
│   │   │   ├── llm/            # Provider interface, OllamaProvider
│   │   │   ├── voice/          # TTS, STT, SVC plugin
│   │   │   ├── crafting/       # RecipeManager wrapper, CraftingExecutor
│   │   │   ├── inventory/      # ContainerMapper, AutoEquip
│   │   │   ├── construction/   # Schematics, BuildExecutor
│   │   │   ├── knowledge/      # KnowledgeBase, RAG, GamePhaseDetector
│   │   │   ├── quests/         # FTB integration, QuestAgent
│   │   │   ├── agents/         # Orquestrador + 6 agentes
│   │   │   ├── skills/         # SkillLibrary, SkillMatcher, SkillCreator
│   │   │   ├── memory/         # LocalMemory, GitMemory, MemoryManager
│   │   │   ├── autonomy/       # InitiativeEngine, SelfVerification
│   │   │   ├── config/         # ForgeConfigSpec
│   │   │   ├── data/           # SavedData classes
│   │   │   ├── events/         # @SubscribeEvent handlers
│   │   │   └── util/           # Helpers, serializers
│   │   └── resources/
│   │       ├── META-INF/mods.toml
│   │       ├── pack.mcmeta
│   │       └── assets/alice/
│   │           ├── textures/
│   │           │   └── skins/
│   │           │       └── alice_scarlet.png
│   │           ├── prompts/
│   │           │   └── base_personality.txt
│   │           ├── knowledge/
│   │           │   ├── minecraft-basics.md
│   │           │   ├── survival-strategies.md
│   │           │   └── cursed-walking-mods/
│   │           ├── schematics/
│   │           │   ├── emergency_shelter.nbt
│   │           │   ├── perimeter_wall.nbt
│   │           │   ├── watchtower.nbt
│   │           │   └── simple_base.nbt
│   │           └── skills/
│   │               ├── combat/
│   │               ├── building/
│   │               ├── crafting/
│   │               ├── navigation/
│   │               └── survival/
│   └── test/
│       └── java/com/alice/
│           ├── CraftingTest.java
│           ├── RuleEngineTest.java
│           └── SkillMatcherTest.java
├── docs/
│   ├── brainstorm/
│   └── plano/
│       └── PLANO-PROJETO.md       # Este documento
└── scripts/
    ├── deploy.sh                  # Copia .jar para servidor via SSH
    └── deploy.bat                 # Versão Windows
```

---

## Apêndice B — Configuração Completa (alice-common.toml)

```toml
[llm]
  ollama_url = "http://192.168.0.200:11434"
  ollama_model = "phi3:mini"
  fallback_model = "llama3.2:latest"
  request_timeout_ms = 30000
  max_history_messages = 10

[voice]
  mode = "online"                    # online | offline | text_only
  whisper_url = "http://192.168.0.225:10300"
  tts_voice = "pt-BR-FranciscaNeural"
  svc_range_blocks = 32
  vad_silence_threshold_ms = 500

[behavior]
  perception_radius_blocks = 16
  combat_radius_blocks = 8
  pickup_radius_blocks = 4
  flee_health_threshold = 0.20       # 20%
  eat_food_threshold = 6             # 6/20
  auto_equip_armor = true
  auto_equip_weapon = true
  follow_mode_default = true

[memory]
  git_repo_url = "https://github.com/USERNAME/alice-memory.git"
  git_token = ""                     # Configurar antes de Fase 6
  commit_on_session_end = true
  local_memory_max_entries = 500

[debug]
  log_rules = false
  log_llm_calls = true
  log_voice_pipeline = false
  log_pathfinding = false
```

---

## Apêndice C — Decisões Técnicas Pendentes (Para Resolver Durante Implementação)

| Decisão | Opções | Prazo para Resolver |
|---------|--------|---------------------|
| Como encapsular PCM em WAV para faster-whisper | Implementar WAV header manualmente (24 bytes) vs usar biblioteca | Antes da Fase 3 |
| Decoder mp3 para Edge TTS output | JLayer (leve) vs mp3spi (mais completo) vs subprocess ffmpeg | Antes da Fase 3 |
| Como simular abertura de GUI de crafting com FakePlayer | `craft()` direto via API vs `ContainerHelper` vs simulação de clique | Fase 2 |
| Quanto contexto de histórico manter para LLM lento | 5 mensagens (700 tokens) vs 10 mensagens (1400 tokens) | Fase 1 |
| Repo Git: Gitea self-hosted vs GitHub público | GitHub para MVP (simples) vs Gitea (mais controle) | Antes da Fase 6 |
| Tool calling: llama3.2 vs phi3:mini para Orquestrador | Testar ambos; phi3 pode não suportar tools estáveis | Fase 6 início |

---

### Critical Files for Implementation

- `/c/Users/Usuario/Desktop/alice/docs/brainstorm/projeto-alice-brainstorm.md`
- `/c/Users/Usuario/Desktop/alice/docs/brainstorm/referencias/catalogo-projetos-referencia.md`
- `/c/Users/Usuario/Desktop/alice/docs/brainstorm/referencias/catalogo-bibliotecas-e-apis.md`
- `/c/Users/Usuario/Desktop/alice/docs/brainstorm/referencias/catalogo-mods-cursed-walking.md`