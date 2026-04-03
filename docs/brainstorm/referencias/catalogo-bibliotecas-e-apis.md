# Catalogo de Bibliotecas, APIs e Dependencias para o Projeto Alice

**Atualizado em:** 2026-04-02

---

## 1. COMUNICACAO COM LLM

### 1.1 ollama4j - Cliente Java para Ollama

- **Repositorio:** https://github.com/ollama4j/ollama4j
- **Documentacao:** https://ollama4j.github.io/ollama4j/
- **Maven:** `io.github.ollama4j:ollama4j:1.1.6`
- **Licenca:** MIT
- **Funcionalidades:**
  - Chat e geracao de texto
  - Suporte a streaming de respostas
  - Suporte a tool/function calling (modelos compatveis: mistral, llama3.x, qwen)
  - Suporte a modelos de visao/imagem
  - Metricas Prometheus embutidas
  - Configuravel: host padrao `http://localhost:11434`
- **Uso no projeto:** Comunicacao direta com Ollama para decisoes da Alice
- **Exemplo basico:**
```java
OllamaAPI ollamaAPI = new OllamaAPI("http://localhost:11434");
OllamaChatResult result = ollamaAPI.chat(
    OllamaChatRequestBuilder.getInstance("llama3")
        .withMessage(OllamaChatMessageRole.USER, "mensagem")
        .build()
);
String resposta = result.getResponse();
```

### 1.2 LangChain4j - Framework Java para LLM

- **Repositorio:** https://github.com/langchain4j/langchain4j
- **Maven:** `dev.langchain4j:langchain4j-ollama`
- **Funcionalidades:**
  - API unificada para multiplos providers (Ollama, OpenAI, Anthropic, etc.)
  - RAG (Retrieval Augmented Generation) embutido
  - ChatMemory para historico de conversas
  - Tool/Function calling
  - Embeddings e vector stores
  - Chains e agents
- **Vantagem sobre ollama4j:** Abstrai o provider de LLM, facilitando troca entre Ollama/OpenAI/Anthropic
- **Uso no projeto:** Camada de abstracao para multi-provider LLM + RAG para conhecimento do jogo
- **Exemplo basico:**
```java
ChatLanguageModel model = OllamaChatModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("llama3")
    .build();
String answer = model.generate("pergunta");
```

### 1.3 Ollama REST API Direto

- **Documentacao:** https://github.com/ollama/ollama/blob/main/docs/api.md
- **Endpoints principais:**
  - `POST /api/chat` - Chat com historico (streaming por padrao)
  - `POST /api/generate` - Geracao simples
  - `POST /api/embeddings` - Gerar embeddings
- **Parametros uteis:**
  - `stream: false` para resposta completa
  - `keep_alive: "5m"` controla tempo do modelo em memoria
  - `tools: [...]` para function calling
- **Formato de request (chat):**
```json
{
  "model": "llama3",
  "messages": [
    {"role": "system", "content": "Voce e Alice..."},
    {"role": "user", "content": "mensagem do jogador"}
  ],
  "stream": false,
  "tools": [...]
}
```
- **Uso no projeto:** Alternativa leve se nao quisermos dependencia de ollama4j/langchain4j

---

## 2. VOZ - TEXT-TO-SPEECH (Alice fala)

### 2.1 Piper TTS + piper-jni

- **Piper:** https://github.com/rhasspy/piper
- **piper-jni:** https://github.com/GiviMAD/piper-jni
- **Maven:** `io.github.givimad:piper-jni` (Maven Central)
- **Licenca:** MIT
- **Funcionalidades:**
  - TTS neural local e offline
  - Rapido o suficiente para Raspberry Pi
  - JNI wrapper inclui: piper nativo + espeak + piper-phonemize + onnxruntime
  - Saida: audio PCM 22.05 kHz
  - Suporte a Windows x86_64, Linux x86_64/arm64
- **Modelos pt_BR disponiveis:**
  - `pt_BR-faber-medium` - Voz masculina, ~63MB, 22.05 kHz (HuggingFace: Trelis/piper-pt-br-faber-medium)
  - Vozes femininas pt_BR: LIMITADAS - pode ser necessario treinar modelo customizado
- **Treinamento de voz customizada:**
  - Formato de dataset: LJSpeech (audio + transcricao)
  - Ferramenta: piper-recording-studio para coleta
  - Pode fazer fine-tune a partir de modelo existente (ex: pt_PT tugao medium)
  - Requer GPU para treinamento
  - Ferramenta alternativa: TextyMcSpeechy (https://github.com/domesticatedviking/TextyMcSpeechy)
- **Formato de arquivos por voz:** `.onnx` (modelo) + `.onnx.json` (config)
- **Uso no projeto:** Gerar audio da fala da Alice localmente
- **PROBLEMA IDENTIFICADO:** Nao ha voz feminina pronta em pt_BR. Opcoes:
  1. Treinar uma voz feminina pt_BR customizada
  2. Usar voz pt_PT feminina (sotaque diferente)
  3. Usar voz masculina pt_BR faber como placeholder inicial
  4. Iniciar MVP apenas com chat texto e adicionar voz depois

### 2.2 Referencia: Nations & Villagers AI Reborn

- Ja usa Piper TTS + eSpeak NG para dar voz a NPCs no Minecraft
- Prova de que a integracao Piper + Minecraft funciona
- Modrinth: https://modrinth.com/mod/nations-villagers-ai-reborn

---

## 3. VOZ - SPEECH-TO-TEXT (Jogador fala para Alice)

### 3.1 Vosk - STT Offline com Java nativo

- **Repositorio:** https://github.com/alphacep/vosk-api
- **Site:** https://alphacephei.com/vosk/
- **Maven:** `com.alphacep:vosk:0.3.31+` (requer JNA 5.7.0+)
- **Licenca:** Apache 2.0
- **Modelos Portugues:**
  - `vosk-model-small-pt-0.3` - 31MB, leve, para tempo real (Apache 2.0)
  - `vosk-model-pt-fb-v0.1.1` - 1.6GB, alta precisao, FalaBrasil (GPLv3)
- **Funcionalidades:**
  - STT offline completo
  - Streaming API com latencia zero
  - Java 8+ compativel
  - Windows, Linux, macOS
  - 20+ idiomas incluindo Portugues
  - Modelo pequeno (31MB) ou servidor (1.6GB)
- **Exemplo Java:**
```java
Model model = new Model("path/to/vosk-model-small-pt");
Recognizer recognizer = new Recognizer(model, 16000); // 16kHz
// Processar audio em chunks de bytes
recognizer.acceptWaveForm(audioBytes, audioBytes.length);
String resultado = recognizer.getResult(); // JSON com texto reconhecido
```
- **Uso no projeto:** Converter fala do jogador (capturada via Simple Voice Chat) em texto para o LLM
- **RECOMENDACAO:** Vosk e MAIS ADEQUADO que Whisper para este projeto porque:
  1. Tem binding Java nativo (sem JNI complexo)
  2. Modelo pequeno (31MB) funciona em tempo real
  3. API de streaming perfeita para audio continuo
  4. Licenca Apache 2.0 (modelo pequeno)

### 3.2 whisper-jni - Alternativa com Whisper

- **Repositorio:** https://github.com/GiviMAD/whisper-jni
- **Maven:** `io.github.givimad:whisper-jni` (Maven Central)
- **Funcionalidades:**
  - JNI wrapper para whisper.cpp
  - Suporta Windows x86_64 e Linux x86_64/arm64
  - Mesmo autor do piper-jni (GiviMAD)
- **Modelos:** tiny (75MB), base (142MB), small (466MB), medium (1.5GB)
- **Portugues:** Bom suporte, melhor com modelo small+
- **Uso no projeto:** Alternativa ao Vosk se precisarmos de maior precisao
- **Desvantagem:** Modelos maiores, mais lento que Vosk para streaming

### 3.3 whisper-cpp-server - Alternativa via REST

- **Repositorio:** https://github.com/litongjava/whisper-cpp-server
- **Funcionalidades:** Servidor REST separado para STT
- **Uso no projeto:** Rodar como servico separado (igual Ollama)

---

## 4. INFRAESTRUTURA DE AUDIO NO MINECRAFT

### 4.1 Simple Voice Chat API (Forge)

- **Repositorio:** https://github.com/henkelmax/simple-voice-chat
- **API Docs:** https://modrepo.de/minecraft/voicechat/api/overview
- **API Examples:** https://modrepo.de/minecraft/voicechat/api/examples
- **Template Forge:** https://github.com/henkelmax/voicechat-api-forge
- **Javadoc:** https://voicechat.modrepo.de/
- **Versao:** Forge 1.20.1 compativel
- **Funcionalidades criticas para Alice:**

#### 4.1.1 EntityAudioChannel - Alice FALA por proximidade
```java
// Criar canal de audio vinculado a entidade Alice
EntityAudioChannel channel = api.createEntityAudioChannel(
    UUID.randomUUID(),
    api.fromEntity(aliceEntity)
);
channel.setCategory("alice_voice"); // categoria registrada
channel.setDistance(32); // raio de audicao em blocos
// Enviar audio: 48kHz, 16-bit, PCM mono
api.createAudioPlayer(channel, encoderInfo, audioSupplier);
```

#### 4.1.2 AudioSender - Capturar audio do jogador
```java
// Registrar audio sender para capturar voz do jogador
// Funciona para jogadores SEM o mod instalado tambem
// Audio recebido em formato opus-encoded
```

#### 4.1.3 StaticAudioChannel - Radio a distancia
```java
// Canal estatico que envia audio para um jogador especifico
// Independente de distancia - perfeito para radio
StaticAudioChannel radio = api.createStaticAudioChannel(
    UUID.randomUUID(),
    api.fromServerLevel(level),
    playerConnection
);
```

- **Registro do plugin Forge:**
```java
@ForgeVoicechatPlugin
public class AliceVoicechatPlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() { return "alice_voice"; }
    
    @Override
    public void initialize(VoicechatApi api) {
        // setup channels, listeners, etc.
    }
}
```

- **Formato de audio aceito:** 48kHz, 16-bit PCM mono (opus-encoded para transmissao)
- **NOTA:** Piper gera 22.05kHz -> precisamos converter para 48kHz antes de enviar

### 4.2 Voice Chat Recording

- **CurseForge:** https://www.curseforge.com/minecraft/mc-mods/voice-chat-recording
- **Funcionalidade:** Grava audio de jogadores via Simple Voice Chat em formato .pcm
- **API:** Expoe metodos para capturar e reproduzir audio gravado
- **Uso no projeto:** Referencia para como capturar audio de jogadores

### 4.3 Walkie-Talkie Mod (referencia para radio)

- **Repositorio:** https://github.com/Flaton1/walkie-talkie-mod
- **CurseForge:** https://www.curseforge.com/minecraft/mc-mods/walkie-talkie
- **Funcionalidade:** Radios com frequencias, upgrades, alcance ilimitado
- **Depende de:** Simple Voice Chat + Architectury API
- **Uso no projeto:** Referencia para implementacao do radio Alice-Jogador

---

## 5. PATHFINDING E NAVEGACAO

### 5.1 Baritone

- **Repositorio:** https://github.com/cabaletta/baritone
- **API Javadoc:** https://baritone.leijurv.com/
- **DeepWiki:** https://deepwiki.com/cabaletta/baritone
- **Stars:** 8.8k
- **Versao 1.20.1:** Disponivel (Forge API jar)
- **Licenca:** LGPL 3.0 (pode usar como biblioteca)
- **API principal:**
```java
// Obter instancia do Baritone
IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();

// Navegar para coordenadas
baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(x, y, z));

// Minerar bloco especifico
baritone.getMineProcess().mine(Blocks.DIAMOND_ORE);

// Seguir entidade
baritone.getFollowProcess().follow(entity -> entity == targetPlayer);

// Construir de schematic
baritone.getBuilderProcess().build("schematic.nbt", origin);
```
- **Interfaces principais:**
  - `IBaritoneProvider` - ponto de entrada
  - `IBaritone` - instancia principal
  - `ICustomGoalProcess` - navegacao por goals
  - `IMineProcess` - mineracao automatica
  - `IFollowProcess` - seguir entidades
  - `IBuilderProcess` - construir de schematics
- **Goals disponiveis:**
  - `GoalBlock(x, y, z)` - ir para bloco exato
  - `GoalXZ(x, z)` - ir para coluna (qualquer Y)
  - `GoalNear(x, y, z, range)` - ir para perto de
  - `GoalGetToBlock(x, y, z)` - ir para bloco adjacente
- **PROBLEMA para Alice:** Baritone e client-side. Para fake players server-side, precisamos do Automatone ou adaptar.

### 5.2 Automatone (Fork server-side do Baritone)

- **Repositorio:** https://github.com/Ladysnake/Automatone
- **Fork alternativo:** https://github.com/minefortress-mod/automatone
- **Descricao:** Fork do Baritone para rodar server-side, projetado para NPCs/fake players
- **Funcionalidades extras:**
  - Pathfinding server-side (nao precisa de cliente)
  - Quebragem de blocos considerando ferramentas no hotbar
  - Colocacao de blocos como parte do caminho (pillar, sneak-back)
  - 30x mais rapido que MineBot
- **Plataforma:** Fabric (NAO Forge)
- **PROBLEMA:** Nao tem versao Forge. Precisamos portar conceitos ou usar Baritone adaptado.
- **DECISAO NECESSARIA:** Baritone (client-side, Forge pronto) vs portar Automatone vs pathfinding proprio

### 5.3 CoroUtil

- **Repositorio:** https://github.com/Tencao/CoroUtil
- **Funcionalidades:**
  - Pathfinder threaded (nao bloqueia game tick)
  - Fila com prioridades e timeout (5 segundos)
  - Callback interface (IPFCallback) para execucao thread-safe
  - Alcance adaptativo quando alvo excede maximo
- **Uso no projeto:** Referencia para pathfinding assincrono se nao usarmos Baritone

---

## 6. RECEITAS E CRAFTING

### 6.1 Minecraft RecipeManager (API nativa)

- **Classe:** `net.minecraft.world.item.crafting.RecipeManager`
- **Acesso:** `ServerLevel.getRecipeManager()` ou `MinecraftServer.getRecipeManager()`
- **Metodos principais:**
```java
RecipeManager rm = server.getRecipeManager();

// Todas as receitas de um tipo
List<RecipeHolder<CraftingRecipe>> todas = rm.getAllRecipesFor(RecipeType.CRAFTING);

// Buscar receita para um input especifico
Optional<RecipeHolder<CraftingRecipe>> receita = rm.getRecipeFor(
    RecipeType.CRAFTING, craftingContainer, level
);

// Inclui receitas de TODOS os mods carregados
```
- **Tipos de receita:** CRAFTING, SMELTING, BLASTING, SMOKING, CAMPFIRE_COOKING, STONECUTTING, SMITHING
- **Uso no projeto:** Alice acessa todas as receitas do jogo programaticamente SEM depender do JEI

### 6.2 JEI API (complementar)

- **Repositorio:** https://github.com/mezz/JustEnoughItems
- **Uso:** Soft dependency - se JEI estiver presente, pode usar sua API para lookup mais rico
- **Vantagem sobre RecipeManager nativo:** Interface de busca mais amigavel, categorias visuais

---

## 7. PERSISTENCIA E MEMORIA

### 7.1 Forge SavedData API

- **Documentacao oficial:** https://docs.minecraftforge.net/en/latest/datastorage/saveddata/
- **Classe base:** `net.minecraft.world.level.saveddata.SavedData`
- **Acesso:** `ServerLevel.getDataStorage()` ou `ServerChunkCache.getDataStorage()`
- **Metodos:**
```java
public class AliceMemoryData extends SavedData {
    // Salvar dados em NBT
    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putString("aventuras", aventurasJson);
        tag.putString("containers_mapeados", containersJson);
        tag.putString("tarefas", tarefasJson);
        return tag;
    }
    
    // Carregar dados de NBT
    public static AliceMemoryData load(CompoundTag tag) {
        AliceMemoryData data = new AliceMemoryData();
        data.aventurasJson = tag.getString("aventuras");
        data.containersJson = tag.getString("containers_mapeados");
        return data;
    }
    
    // Registrar
    public static AliceMemoryData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            AliceMemoryData::load, AliceMemoryData::new, "alice_memory"
        );
    }
}
```
- **Persistencia:** Salva automaticamente com o mundo
- **Escopo:** Per-world (cada mundo tem seus dados)
- **Para dados globais:** Anexar ao Overworld via `MinecraftServer.overworld()`
- **Uso no projeto:** Salvar memoria da Alice (aventuras, containers mapeados, tarefas, combinacoes)

### 7.2 Entity NBT (dados na propria entidade)

- **Como:** Override de `addAdditionalSaveData()` e `readAdditionalSaveData()`
- **Uso:** Dados que pertencem a Alice como entidade (inventario, posicao de spawn, stats)
- **Limitacao:** Tamanho de NBT pode crescer se nao controlado

---

## 8. ENTIDADE E FAKE PLAYER

### 8.1 Forge FakePlayer (API nativa)

- **Classe:** `net.minecraftforge.common.util.FakePlayer`
- **Estende:** `ServerPlayer`
- **Limitacao:** Minimalista, projetado para atribuicao de acoes (quem quebrou bloco), nao para simulacao completa

### 8.2 Abordagem recomendada: Custom Mob Entity com visual de player

- **Estender:** `Mob` ou `PathfinderMob`
- **Visual:** Usar player model + skin customizada
- **Vantagens:**
  - Sistema de AI Goals nativo do Minecraft
  - EntityType registrado corretamente
  - Renderer customizavel
  - Compativel com sistema de pathfinding vanilla
- **Desvantagens:**
  - Nao aparece como "jogador" na TAB list
  - Nao e tratada como ServerPlayer por outros mods
- **Registro:**
```java
public static final DeferredRegister<EntityType<?>> ENTITIES = 
    DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "alice");

public static final RegistryObject<EntityType<AliceEntity>> ALICE = 
    ENTITIES.register("alice", () -> EntityType.Builder
        .of(AliceEntity::new, MobCategory.CREATURE)
        .sized(0.6F, 1.8F) // tamanho de player
        .build("alice"));
```

### 8.3 Abordagem alternativa: Estender ServerPlayer

- **Como Carpet faz:** Cria `EntityPlayerMPFake extends ServerPlayer`
- **Requer:** Fake GameProfile + Fake NetworkManager + Fake Connection
- **Vantagens:**
  - Aparece como jogador real na TAB
  - Compativel com mods que checam ServerPlayer
  - Pode usar Baritone diretamente
- **Desvantagens:**
  - Complexo: precisa simular conexao de rede
  - Mais fragil (pode quebrar com atualizacoes)
  - Conflitos potenciais com mods de autenticacao

### 8.4 DECISAO NECESSARIA

Qual abordagem usar? A recomendacao com base na pesquisa:
- **MVP:** Custom Mob Entity (mais simples, mais estavel)
- **Futuro:** Migrar para ServerPlayer estendido se necessario para compatibilidade total

---

## 9. SCHEMATICS E CONSTRUCAO

### 9.1 Create Mod - Sistema de Schematics

- **Repositorio:** https://github.com/Creators-of-Create/Create
- **Classes relevantes:**
  - `com.simibubi.create.content.schematics.SchematicWorld` - mundo virtual para schematic
  - `com.simibubi.create.content.schematics.SchematicPrinter` - colocacao bloco a bloco
- **Formato:** `.nbt` (NBT structure)
- **Como funciona o Schematicannon:**
  1. Carrega schematic .nbt
  2. Cria SchematicWorld virtual
  3. Itera blocos do schematic
  4. Coloca cada bloco no mundo real consumindo recursos
- **Uso no projeto:** Referencia para como Alice constroi de schematics

### 9.2 Forgematica

- **Modrinth:** https://modrinth.com/mod/forgematica
- **Formatos suportados:** .nbt, .litematic
- **Funcionalidades:** Carregar, salvar, holograma de preview
- **Uso no projeto:** Possivel dependencia para carregar/parsear schematics

### 9.3 Algoritmo de construcao bloco-a-bloco

1. Carregar schematic (parsear NBT)
2. Determinar ordem de colocacao:
   - Bottom-up (de baixo pra cima)
   - Blocos solidos primeiro
   - Blocos que precisam de suporte por ultimo (tochas, portas, etc.)
   - Blocos afetados por gravidade com cuidado (areia, cascalho)
3. Para cada bloco:
   - Navegar ate posicao adjacente (pathfinding)
   - Verificar se tem o material no inventario
   - Colocar bloco
4. Se faltar material: pausar e informar jogador

---

## 10. EVENTOS E PERCEPCAO DO MUNDO (Forge)

### 10.1 Eventos uteis para Alice

```java
// Detectar dano a entidades proximas
@SubscribeEvent
public void onLivingHurt(LivingHurtEvent event) { }

// Entidade entra no mundo
@SubscribeEvent
public void onEntityJoinLevel(EntityJoinLevelEvent event) { }

// Bloco quebrado
@SubscribeEvent
public void onBlockBreak(BlockEvent.BreakEvent event) { }

// Chat do jogador
@SubscribeEvent
public void onServerChat(ServerChatEvent event) { }

// Tick do servidor (para logica periodica)
@SubscribeEvent
public void onServerTick(TickEvent.ServerTickEvent event) { }
```

### 10.2 Scanning de entidades proximas

```java
// Buscar mobs hostis num raio de 32 blocos
AABB area = new AABB(alice.blockPosition()).inflate(32);
List<Monster> mobs = level.getEntitiesOfClass(Monster.class, area);

// Buscar jogadores proximos
List<Player> jogadores = level.getEntitiesOfClass(Player.class, area);

// Buscar itens no chao
List<ItemEntity> itens = level.getEntitiesOfClass(ItemEntity.class, area);
```

### 10.3 Scanning de blocos

```java
// Buscar blocos especificos num raio
BlockPos.betweenClosed(pos.offset(-r,-r,-r), pos.offset(r,r,r))
    .filter(bp -> level.getBlockState(bp).is(Blocks.CHEST))
    .forEach(bp -> /* registrar container */);
```
