# Spike A — Mapa de Acoplamento Baritone → Forge Server-Side

**Data:** 2026-04-11
**Autor:** dev-agent + PO
**Status:** completo (segunda passada com leitura real dos arquivos "médio")
**Budget usado:** 0.8 dia / 3 dias
**Fontes:**
- Baritone branch `1.20.1`, commit local `/c/Users/Usuario/Desktop/alice-refs/baritone`
- Automatone `master`, commit local `/c/Users/Usuario/Desktop/alice-refs/Automatone`

---

## 0. TL;DR para o PO

- **Baritone 1.20.1 já tem build Forge nativo** via `unimined`. A premissa "port manual do zero" estava errada. O projeto produz jar Forge real (não fake loader) — o problema é que o jar é **client-only**.
- **Automatone resolveu exatamente o mesmo problema em Fabric** e o padrão é diretamente portável: reescrever a camada `launch/mixins/` para hooks server-side + acoplar `IBaritone` ao FakePlayer via Capability (Forge) em vez de Cardinal Components (Fabric).
- **Nenhum acoplamento classificado como `bloqueio`** até aqui. 6 pontos de atrito, todos em categorias `trivial` ou `médio`.
- **Revisão do cronograma sugerida:** o port é provavelmente menor do que Spike A–E implicavam. Sugiro reduzir o hard cap agregado de 2 semanas → 10 dias úteis e acrescentar um go/no-go extra **após Spike B** (pelo alto retorno de um "compilou" precoce).

---

## 1. Estrutura real do Baritone 1.20.1

```
baritone/
├── src/
│   ├── api/java        (160 arquivos) — interfaces públicas
│   ├── main/java       (153 arquivos) — core pathfinding + behaviors
│   ├── launch/java     (12 mixins)    — hooks em classes client
│   └── schematica_api  — integração Schematica (não usamos)
├── fabric/             — loader Fabric
├── forge/              — loader Forge (stub @Mod, 1 arquivo)
└── tweaker/            — loader legacy
```

`gradle.properties`:
```
minecraft_version=1.20.1
forge_version=47.0.1     # compatível com nosso Forge MDK 47.4.0
fabric_version=0.14.18
java_version=17
available_loaders=fabric,forge,tweaker
```

A root build.gradle compartilha sourceSets (`api`, `main`, `launch`, `schematica_api`) entre os subprojetos `fabric/`, `forge/`, `tweaker/`. Cada loader só contribui um `@Mod` entry point e configuração de build. **A camada de mixins em `src/launch/java/baritone/launch/mixins/` é loader-agnóstica** (usa SpongeMixin padrão) e **é inteiramente client-side** no Baritone upstream.

---

## 2. Mapa de acoplamentos client-side

### 2.1 Camada `launch/mixins/` (12 arquivos, 100% client)

| Mixin | Alvo | Função | Substituição server-side | Categoria |
|---|---|---|---|---|
| `MixinMinecraft` | `Minecraft` (client) | Hook do tick loop principal; dispara `TickEvent` e `PlayerUpdateEvent POST` | Subscrever `ServerTickEvent` no Forge event bus; chamar `baritone.getGameEventHandler().onTick(...)` para cada FakePlayer ativo | **trivial** |
| `MixinClientPlayerEntity` | `LocalPlayer` | Hook `tick`/`aiStep`/`rideTick` para disparar `PlayerUpdateEvent PRE`, controlar sprint key, bloquear elytra durante path | Chamar manualmente de dentro do `tick()` do nosso FakePlayer (que nós controlamos). Sprint/elytra viram flags em vez de redirect de KeyMapping | **trivial** |
| `MixinClientChunkProvider` | `ClientChunkCache` | Acesso a chunks carregados no client | Usar `ServerLevel.getChunkSource()` direto; não precisa de mixin | **trivial** |
| `MixinClientPlayNetHandler` | `ClientPacketListener` | Intercepta pacotes recebidos para rastrear mundo | No server não precisa — temos acesso direto a `ServerLevel` | **trivial** (remover) |
| `MixinScreen` | `Screen` | Suprime pause menu quando pathing | N/A server-side — remover | **trivial** (remover) |
| `MixinWorldRenderer` | `LevelRenderer` | Desenha path no mundo (debug visual) | **Remover** — rendering não vai para o server. Se precisarmos debug visual no cliente, vira feature futura de pacote-para-cliente. | **trivial** (remover, com nota no Plan A.1) |
| `MixinEntityRenderManager` | `EntityRenderDispatcher` | Camera/renderer | Remover | **trivial** (remover) |
| `MixinPlayerController` | `MultiPlayerGameMode` | Intercepta `startDestroyBlock`, `useItem`, etc para path | Server-side, o path age direto via `ServerPlayerGameMode` — precisa equivalente. **Automatone tem `MixinServerPlayerInteractionManager`** exatamente para isso. | **médio** |
| `MixinCommandSuggestionHelper` | client command suggest | Autocomplete de `.goto` etc no chat do client | Remover (comandos virão via packet/chat do FakePlayer owner) | **trivial** (remover) |
| `MixinEntity` / `MixinLivingEntity` | `Entity`/`LivingEntity` | Ajustes finos (sprint state, fall damage, etc) | Mantém — são classes não-client. Automatone usa exatamente os mesmos. | **trivial** |
| `MixinItemStack` | `ItemStack` | Durabilidade/swap logic | Mantém — não é client | **trivial** |
| `MixinFireworkRocketEntity` | `FireworkRocketEntity` | Controle de elytra | Mantém | **trivial** |
| `MixinChunkArray` / `MixinPalettedContainer` / `MixinPalettedContainer$Data` | chunk storage | Perf optimization em leitura de chunks | Mantém, são classes comuns | **trivial** |
| `MixinNetworkManager` | `Connection` | Hook packets | Mantém, classe comum | **trivial** |
| `MixinLootContext` | `LootContext` | Previsão de drops para pathing de mineração | Mantém | **trivial** |

**Conclusão camada launch/:** 7 mixins podem ser **removidos** (renderer/client-only), 2 precisam **reescrita server-side** (`MixinMinecraft` → ServerTickEvent, `MixinClientPlayerEntity` → direct call no FakePlayer tick), 1 precisa de **substituto direto** (`MixinPlayerController` → `MixinServerPlayerGameMode`), e 6 ficam **como estão** (mixins em classes não-client).

### 2.2 `src/main/java` — arquivos com referência a `net.minecraft.client.*`

Total: 20 arquivos com pelo menos uma referência client (fonte: grep). Após leitura real do código Baritone upstream e do equivalente Automatone, segue a análise detalhada por ponto de acoplamento.

#### 2.2.1 `Baritone.java` — construtor e campo `Minecraft mc` — **médio**

**O que o Baritone faz:** O construtor `Baritone(Minecraft mc)` armazena a instância client e a usa para: (a) obter `mc.gameDirectory` para criar `baritone/` folder de cache, (b) passar `mc` para `BaritonePlayerContext`, (c) abrir `GuiClick` via `mc.setScreen()`. Os behaviors e processes recebem `this` (Baritone), não `mc` diretamente.

**Como Automatone resolveu:** Trocou o construtor para `Baritone(LivingEntity player)`. Removeu o campo `Minecraft mc` completamente. O diretório de cache vem de `IWorldProvider.KEY.get(player.world)` (Cardinal Components no World). O `GuiClick`/`openClick()` foi removido. O `playerContext` recebe a entity diretamente via `new EntityContext(player)`.

**Aplicabilidade em Forge:** Direta. Construtor vira `Baritone(ServerPlayer player)` ou `Baritone(LivingEntity entity)`. O diretório de cache pode usar `FMLPaths.GAMEDIR.get().resolve("baritone")`. O `openClick()` é deletado (não existe no server). A cascata de mudança é contida: behaviors recebem `Baritone this`, não `mc`, então não propagam.

#### 2.2.2 `BaritoneProvider.java` — registry por `Minecraft`/`LocalPlayer` — **médio**

**O que o Baritone faz:** `BaritoneProvider()` cria o primary baritone via `createBaritone(Minecraft.getInstance())`. O método `createBaritone(Minecraft)` faz lookup por instância Minecraft (para suportar múltiplos clientes, o que na prática nunca acontece). `getBaritoneForPlayer(LocalPlayer)` itera `all` e compara `ctx.player()`.

**Como Automatone resolveu:** `BaritoneProvider` virou singleton (`INSTANCE`). Não tem mais `createBaritone` — cada IBaritone é um Cardinal Component attached à entity via `IBaritone.KEY.get(entity)`. O provider só faz `getBaritone(LivingEntity)` que delega ao component key. Factory: `componentFactory()` retorna `Baritone::new`.

**Aplicabilidade em Forge:** Em vez de Cardinal Components, usamos Forge Capabilities. `AttachCapabilitiesEvent<Entity>` para associar `IBaritone` ao FakePlayer. `BaritoneProvider.getBaritone(entity)` consulta `entity.getCapability(BARITONE_CAP)`. Não há "primary baritone" no server — cada FakePlayer tem o seu. Padrão bem documentado no ecossistema Forge.

#### 2.2.3 `BaritonePlayerController.java` → `ServerPlayerController` — **médio**

**O que o Baritone faz:** Wrapa `mc.gameMode` (tipo `MultiPlayerGameMode`, que é a classe client que envia pacotes C2S ao servidor). Métodos: `syncHeldItem()`, `hasBrokenBlock()`, `onPlayerDamageBlock()`, `resetBlockRemoving()`, `windowClick()`, `clickBlock()`, `processRightClickBlock/Click()`, `getGameType()`. Todos delegam a `mc.gameMode`.

**Como Automatone resolveu:** Criou `ServerPlayerController` que wrapa `player.interactionManager` (tipo `ServerPlayerInteractionManager`, que é o equivalente server-side do `MultiPlayerGameMode`). Cada método mapeia 1:1. Exemplo: `clickBlock(pos, face)` chama `player.interactionManager.processBlockBreakingAction(pos, START_DESTROY_BLOCK, face, ...)`. Também usa accessor mixin `IServerPlayerInteractionManager` para expor `isMining()`, `getMiningPos()`, `getBlockBreakingProgress()` — campos privados do interaction manager.

**Aplicabilidade em Forge:** Direta. `ServerPlayer.gameMode` é o `ServerPlayerGameMode` do Forge (equivalente Mojmap do `ServerPlayerInteractionManager` de Yarn). Os accessors precisam de mixin ou Access Transformer — AT é mais idiomático em Forge. Cada método do controller tem correspondência 1:1. Sem surpresa.

#### 2.2.4 `BaritonePlayerContext.java` → `EntityContext` — **médio (rebaixar para trivial)**

**O que o Baritone faz:** Getters: `player()` retorna `mc.player` (LocalPlayer), `world()` retorna `mc.level` (ClientLevel), `minecraft()` retorna `mc`, `viewerPos()` usa `mc.getCameraEntity()`, `objectMouseOver()` faz rayTrace. Tudo puxa do singleton Minecraft.

**Como Automatone resolveu:** `EntityContext(LivingEntity entity)` armazena a entity diretamente. `entity()` retorna a entity, `world()` retorna `(ServerWorld) entity.world` com assert `!isClient`. `playerController()` consulta `IPlayerController.KEY.get(entity)`. `objectMouseOver()` faz rayTrace a partir da entity. `inventory()` faz cast para PlayerEntity se aplicável.

**Aplicabilidade em Forge:** Trivial. É uma classe de ~50 linhas que só troca "pegar do Minecraft singleton" para "pegar da entity passada no construtor". Sem complicação de API ou dependência externa.

#### 2.2.5 `InputOverrideHandler.java` + `PlayerMovementInput.java` — **trivial (rebaixado de médio)**

**O que o Baritone faz:** `InputOverrideHandler` mantém mapa `Input→Boolean` de teclas virtuais. No `onTick()`, se Baritone está em controle, troca `player.input` de `KeyboardInput` (input real do teclado) para `PlayerMovementInput` (input virtual que lê o mapa). `PlayerMovementInput` extends `net.minecraft.client.player.Input` e implementa `tick()` setando `forwardImpulse`, `leftImpulse`, `jumping`, `shiftKeyDown` conforme mapa de inputs.

**Como Automatone resolveu:** **Eliminou `PlayerMovementInput` completamente.** No server não existe `player.input` (esse campo é client-only). Em vez disso, `InputOverrideHandler.onTickServer()` seta diretamente os campos do `LivingEntity`: `entity.forwardSpeed`, `entity.sidewaysSpeed`, `entity.setSneaking()`, `entity.setJumping()`. Também adicionou `synchronized` nos métodos de read/write do mapa (thread safety para path calculator).

**Aplicabilidade em Forge:** Muito simples — mais simples que o original. `PlayerMovementInput.java` é deletado. `InputOverrideHandler.onTick()` vira versão server que seta campos diretamente na entity. Os campos `forwardSpeed`, `sidewaysSpeed`, `setSneaking()`, `setJumping()` existem em `LivingEntity` tanto no Forge quanto no Fabric (são campos MCP/Mojmap da superclasse). Zero complicação.

#### 2.2.6 Render stack e demais

| Arquivo | Ação | Categoria |
|---|---|---|
| `GuiClick.java`, `PathRenderer.java`, `IRenderer.java` | **Deletar.** Rendering puro-client. | **trivial** |
| `CachedChunk.java`, `CalculationContext.java`, `BlockStateInterface.java`, `WorldScanner.java`, `WorldProvider.java` | Substituir `mc.level` por `entity.level()` ou `ServerLevel` passado por parâmetro. Automatone fez exatamente isso. | **trivial** |
| `ElytraCommand.java`, `SetCommand.java`, `InventoryBehavior.java`, `ToolSet.java`, `IClientChunkProvider.java`, `MovementDiagonal.java` | Refs pontuais (`mc.options`, `mc.player`). Ajustes localizados de 1-3 linhas cada. | **trivial** |

**Conclusão src/main após segunda passada:** Dos 5 pontos originalmente classificados como "médio", 2 foram **rebaixados para trivial** (`BaritonePlayerContext`, `InputOverrideHandler`/`PlayerMovementInput`). Restam 3 médios reais (`Baritone.java` init, `BaritoneProvider`, `BaritonePlayerController`), todos com equivalente direto no Automatone e sem surpresas na leitura do código. **Nenhum ponto reclassificado para bloqueio.**

### 2.3 `src/api/java` — 5 arquivos com ref client

| Arquivo | Situação | Ação |
|---|---|---|
| `IBaritoneProvider.java` | tem `getBaritoneForPlayer(LocalPlayer)` | sobrecarga com `ServerPlayer`, manter compat |
| `IGameEventListener.java` | eventos usam `ClientLevel` em WorldEvent | generificar para `Level` |
| `Settings.java` | `KeyMapping` em settings de UI | mover para subclasse client ou stub |
| `WorldEvent.java` | envelope `ClientLevel` | usar `Level` |
| `RelativeFile.java` | usa `mc.gameDirectory` | ler via `FMLPaths.GAMEDIR` (Forge) |

**Conclusão src/api:** 100% trivial + médio. Nenhum bloqueio.

---

## 3. Como Automatone resolveu (playbook)

Automatone é fork Fabric server-side do Baritone (mesma versão major — Baritone 1.2.x, MC antigo). A transformação que eles fizeram:

**Removidos inteiramente** (os que no Baritone são client-mixins):
- MixinMinecraft
- MixinClientPlayerEntity
- MixinClientChunkProvider
- MixinClientPlayNetHandler
- MixinScreen
- MixinWorldRenderer
- MixinEntityRenderManager
- MixinCommandSuggestionHelper
- MixinPlayerController

**Adicionados** (server-side):
- `MixinServerWorld` — hook tick do server level
- `MixinServerChunkManager` — chunk access server-side
- `MixinServerCommandSource` — comando via `/` no server
- `MixinServerPlayerInteractionManager` — substitui `MixinPlayerController`
- `MixinMobEntity` — permitir que mobs "inteligentes" também usem Baritone
- `player/ServerPlayerEntityMixin` — cancelar lógica cliente em FakePlayer para evitar deadlocks
- `player/EntityMixin`, `PlayerEntityMixin` — attachments

**Introduziram**:
- `baritone.api.fakeplayer.AutomatoneFakePlayer` — **interface marker** para FakePlayers
- `baritone.api.fakeplayer.FakePlayers` — factory + registro client/server de spawn packets
- `FakeServerPlayerEntity` (server-side) e `FakeClientPlayerEntity` (client-side)
- Custom spawn packet `automatone:fake_player_spawn` para sincronizar server→client
- `IBaritone.KEY.get(entity)` via Cardinal Components — attachment por entidade

Para Forge:
- Interface `AutomatoneFakePlayer` vira interface nossa (trivial)
- `FakeServerPlayerEntity` no Fabric ≈ `FakePlayer` do Forge (já existe em `net.minecraftforge.common.util.FakePlayer`) — precisa confirmar se o FakePlayer do Forge é suficiente ou se precisamos estender
- Cardinal Components (attachment) ≈ Forge Capabilities (`ICapabilityProvider`) — 1:1 conceitual
- Custom spawn packet: Forge tem `SimpleChannel` — equivalente direto

---

## 4. Lista de acoplamentos classificados (pós segunda passada)

| # | Ponto | Categoria | Justificativa | Equivalente Automatone |
|---|---|---|---|---|
| 1 | Tick loop (MixinMinecraft) | **trivial** | ServerTickEvent já existe no Forge, 20 linhas de código | `Baritone.serverTick()` chamado de `MixinServerWorld` |
| 2 | Player tick hooks (MixinClientPlayerEntity) | **trivial** | Chamamos direto do nosso FakePlayer.tick() | Removido; calls diretos |
| 3 | `Baritone.java` init referencia `Minecraft mc` | **médio** | Refatorar construtor para `Baritone(LivingEntity)`. Cascata contida — behaviors recebem `this`, não `mc` | `Baritone(LivingEntity player)` |
| 4 | `BaritonePlayerController` wrapa `mc.gameMode` | **médio** | Reescrever contra `ServerPlayer.gameMode`. Mapping 1:1 dos métodos. Precisa AT ou mixin accessor para campos privados | `ServerPlayerController` com `IServerPlayerInteractionManager` |
| 5 | `InputOverrideHandler`/`PlayerMovementInput` | **trivial** | Deletar `PlayerMovementInput`. Setar `entity.forwardSpeed`/`sidewaysSpeed`/`setJumping()`/`setSneaking()` direto | `InputOverrideHandler.onTickServer()` — exatamente isso |
| 6 | Render stack (`PathRenderer`, `GuiClick`, `IRenderer`) | **trivial** | Deletar. Debug visual futuramente via packet | Removido |
| 7 | `BaritoneProvider` registry por `Minecraft` | **médio** | Trocar para Forge Capability lookup por entity | `IBaritone.KEY.get(entity)` via Cardinal Components |
| 8 | `BaritonePlayerContext` retorna tipos client | **trivial** | Classe de ~50 linhas, só trocar `mc.player`→`entity`, `mc.level`→`entity.level()` | `EntityContext(LivingEntity)` |
| 9 | `RelativeFile` usa `mc.gameDirectory` | **trivial** | `FMLPaths.GAMEDIR.get()` | N/A (removido) |
| 10 | `Settings.KeyMapping` | **trivial** | Mover para subclasse client-only ou stub | Removido |

**Contagem final: 7 triviais, 3 médios, 0 bloqueios.**

Critério de kill individual do Spike A (`>3 bloqueios sem solução`): **não disparou. Spike A passa.**

---

## 5. Riscos remanescentes (não-bloqueantes mas a validar em Spikes B+)

- **R-A1 — FakePlayer do Forge é suficiente?** Forge tem `net.minecraftforge.common.util.FakePlayer`. Automatone no Fabric precisou criar `FakeServerPlayerEntity` próprio porque o FabricAPI não tem um. Se o FakePlayer do Forge já tiver o necessário (inventário, interaction manager funcional, packet de spawn), poupamos trabalho. **Validar no Spike C.**
- **R-A2 — Server tick thread vs. path calculation thread.** Baritone calcula path em thread própria e aplica inputs no tick thread. Para server-side isso precisa ser rigoroso: nenhum acesso a `ServerLevel`/`BlockState` de thread secundária. `CalculationContext` já tem "snapshot" do mundo — a confirmar se o snapshot cobre tudo. **Validar no Spike C.5.**
- **R-A3 — unimined + Forge MDK compatibilidade.** Baritone usa `unimined` (WagYourTail) como plugin Gradle multi-loader. Nosso MDK é Forge puro (gradle `net.minecraftforge.gradle`). Vendorizar Baritone como subprojeto pode exigir reescrever seu build.gradle para ForgeGradle. **Validar no Spike B.**
- **R-A4 — Mixins Forge em runtime.** Forge 47.x suporta Mixins via Mixin Bootstrap, mas menos plug-and-play que Fabric. Alguns mixins do Baritone podem precisar adaptação de sintaxe (targets de obfuscation etc). **Validar no Spike B.**
- **R-A5 — Schematica API.** Baritone tem sourceSet `schematica_api` para builds. Não usamos no Alice (Fase 1 não tem build schematic). **Excluir do vendor.**
- **R-A6 — nether-pathfinder.** Dependência nativa JNI (`dev.babbaj:nether-pathfinder`). Incluir no shadow jar ou excluir se não usamos. **Decidir antes do Spike B.**

---

## 6. Conclusão do Spike A

**Resultado: PASS.** 0 bloqueios identificados, muito abaixo do kill criteria (>3 bloqueios).

O mapa confirma que o port é viável e que Automatone serve como playbook direto para cada ponto de acoplamento. A descoberta de que Baritone 1.20.1 já tem build Forge nativo reduz significativamente o escopo esperado.

**Spike A encerrado com 0.8 dia de budget usado (de 3 alocados).**

Próximo passo conforme plano: go/no-go do PO para iniciar Spike B (Baritone "gutted" compila em Forge MDK).
