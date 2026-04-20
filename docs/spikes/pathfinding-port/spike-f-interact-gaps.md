# Spike F — Gaps do port do Baritone: portas, agua, interact

**Status:** PARCIAL — portas JA portadas (bug de execucao); agua NAO portada (swim/dive faltante)
**Orcamento:** 1 dia | **Gasto:** ~0.4 dia (so pesquisa + automacao; fix em followup)
**Kill criteria:** reescrever Baritone inteiro ou exigir mixin em classes vanilla -> NAO OCORREU

## Objetivo

Responder duas perguntas que o usuario levantou apos observacao em jogo:

1. Alice atravessa agua mas nao consegue mergulhar (afundar em pit com agua 2+ blocos).
2. Alice ora atravessa portas sem abrir, ora fica travada em loop open/close.

Perguntas-guia:
- Onde exatamente o codigo do Baritone ja foi portado vs. onde tem gap?
- Existe codigo de door interact? Se sim, por que o comportamento observado esta errado?
- Da pra testar automaticamente sem precisar de jogador manual?

## O que existe no port (inventario por codigo)

### Pathfinding — canWalkThrough (MovementHelper)

- `MovementHelper.canWalkThroughBlockState` em [MovementHelper.java:155-161](../../../mod/src/main/java/baritone/pathing/movement/MovementHelper.java#L155-L161) JA retorna YES para `DoorBlock` e `FenceGateBlock` salvo IRON_DOOR. Comentario da linha 276: *"not including doors or fence gates (we'd have to right click)"*.
- `isDoorPassable` em [MovementHelper.java:338-349](../../../mod/src/main/java/baritone/pathing/movement/MovementHelper.java#L338-L349): usa `BlockStateInterface.get` + `DoorBlock.OPEN` property. Retorna passavel corretamente quando a porta esta aberta na direcao da travessia.
- `isGatePassable` idem para portoes.

### Execution — MovementTraverse (porta/portao)

- [MovementTraverse.java:226-234](../../../mod/src/main/java/baritone/pathing/movement/movements/MovementTraverse.java#L226-L234): detecta porta em `positionsToBreak`, se nao passavel + nao-ferro, seta rotation pro centro do bloco + `Input.CLICK_RIGHT`.
- Linhas 236-246: mesma logica pra portao.

### Input routing

- `Input.CLICK_RIGHT` em `MovementState.inputStates` -> [Movement.java:140](../../../mod/src/main/java/baritone/pathing/movement/Movement.java#L140) -> `InputOverrideHandler.setInputForceState`.
- [InputOverrideHandler.java:109](../../../mod/src/main/java/baritone/utils/InputOverrideHandler.java#L109): `blockPlaceHelper.tick(isInputForcedDown(CLICK_RIGHT))`.
- [BlockPlaceHelper.java:38-57](../../../mod/src/main/java/baritone/utils/BlockPlaceHelper.java#L38-L57): raytrace via `objectMouseOver` -> `processRightClickBlock` com delay `rightClickSpeed` ticks.
- [ServerPlayerController.java:96-98](../../../mod/src/main/java/baritone/utils/player/ServerPlayerController.java#L96-L98): `processRightClickBlock` -> `player.gameMode.useItemOn(...)` — **caminho vanilla para abrir porta**.

### Rotation

- [LookBehavior.java:67-69](../../../mod/src/main/java/baritone/behavior/LookBehavior.java#L67-L69): `updateTarget(rotation, true)` seta `this.target`.
- [LookBehavior.java:90-101](../../../mod/src/main/java/baritone/behavior/LookBehavior.java#L90-L101): `onPlayerUpdate(PRE)` aplica rotation via `player.setYRot/setXRot`.

### Agua — GAP

- `MovementSwim` **nao existe** no port. Grep `MovementSwim|swim|dive`: so `MovementDescend` checa `isWater` pra colisao, nenhum movement dedicado pra descer verticalmente em fluido.
- Em vanilla Baritone upstream, nadar em agua aberta usa swim jump+descend hibrido; aqui a Alice so trata agua como obstaculo a contornar ou atravessar horizontalmente.

## Bug hipotese: door interact no servidor

O codigo esta todo presente, mas o fluxo de tick aplica rotacao *depois* do input ser processado no mesmo tick:

1. `serverTick()` dispatch `TickEvent(PRE, IN)`:
   1. `PathingBehavior.onTick` -> `Movement.update()` seta `target rotation` + `CLICK_RIGHT`.
   2. `InputOverrideHandler.onTick` -> `BlockPlaceHelper.tick(true)` -> `objectMouseOver()`.
      Raytrace usa `ctx.playerRotations()` — na primeira vez que a porta aparece no path, o player AINDA nao girou pra olhar pro bloco; raytrace erra.
2. `serverPlayerUpdate()` -> `LookBehavior` rotaciona player **depois**.
3. Proximo tick: rotacao ja aplicada (persistida no setYRot), raytrace acerta porta -> `useItemOn` -> abre.

Em ticks seguintes, com a porta ja aberta, `isDoorPassable` ainda pode retornar `false` se a Alice estiver **pe no bloco da porta** (linha 339: `if (playerPos.equals(doorPos)) return false`). Nesse momento o notPassable volta a `true`, CLICK_RIGHT dispara de novo -> **fecha a porta na cara dela** enquanto ela ainda esta atravessando. Isso explica o loop open/close observado.

## Acoes

### Portas — FIX pequeno

Opcao A (minima): no ramo de porta do `MovementTraverse.java:226-234`, checar explicitamente se a porta ja esta OPEN antes de disparar CLICK_RIGHT — nao confiar so em `isDoorPassable` que tem short-circuit pra playerPos==doorPos.

Opcao B (defensiva): no `BlockPlaceHelper`, antes de dar tick em CLICK_RIGHT, verificar se objectMouseOver acertou mesmo o bloco alvo da rotation — se nao, segurar mais um tick.

**Decisao:** Opcao A. Menos invasiva, resolve o loop com 3 linhas.

### Agua — GAP real (futuro)

Portar `MovementSwim` exige:
- Logica de "can swim down" testando se bloco abaixo e water + nao solid.
- JUMP=false, forward=0, deixar gravidade + velocity y negativa atuar (no-clip com fluido e gratis).
- `MovementHelper.canWalkThrough` ja trata agua como YES (fluido nao-solido).

Escopo: ~80 linhas, classe nova + registro em MovementType. **Saindo deste spike pra backlog** porque precisa de testes dedicados + validacao em mundo real (spawn em lago vs pit). Ticket: T-09 no backlog.

### Automacao de teste

`test-server/tests/test_alice_behavior.py` ja tem:
- `test_alice_opens_door` marcado `xfail` (documenta gap).
- `test_alice_dives_into_water` marcado `xfail` (documenta gap).
- `test_stuck_detector_no_false_positive` regressao.
- `test_alice_is_spawned` smoke.

Quando o fix da porta (Opcao A) landar, remover `@pytest.mark.xfail` do teste de porta e rodar:
```
pytest test-server/tests/ -v
```

Requisitos: test-server em pe, RCON habilitado (`enable-rcon=true, rcon.password=alicetest`).

**Nao da pra testar `isDoorPassable` em JUnit puro** — a funcao depende de `BlockStateInterface.get(ctx, pos)` que precisa de um `Level` com chunks carregados. Mock disso demanda framework (Mockito + stubs profundos) cujo custo nao vale vs. teste de integracao via RCON que ja cobre o caminho inteiro.

## Conclusao

- **Portas:** codigo ja portado, bug identificado (playerPos==doorPos disparando re-click). Fix e trivial — 3 linhas no MovementTraverse. 
- **Agua:** gap real, MovementSwim precisa ser portado. Estimativa: 80 linhas + teste. Fora do escopo deste spike — ticket T-09.
- **Testes automaticos:** pytest/RCON cobrem ambos os cenarios hoje como xfail. Fix da porta deve virar PASS no mesmo teste automaticamente.

## Iteracao 2 — trapdoors + diagnostico (mesmo spike, 2a rodada)

Usuario testou o fix da porta em cenario real: Alice ficou oscilando 216↔217 por 25s sem nenhum log `[Alice][Door]`. Via RCON, o layout descoberto foi:

```
             z=162              z=163              z=164
x=216        AIR                                   (Alice oscilava aqui)
x=217       DOOR                                   DOOR
x=218       PRESSURE_PLATE                         PRESSURE_PLATE
x=219       DOOR                TRAPDOOR           DOOR
```

Problemas identificados na 2a rodada:

1. **Trapdoor sempre NO em canWalkThrough.** [MovementHelper.java:143](../../../mod/src/main/java/baritone/pathing/movement/MovementHelper.java#L143) listava `TrapDoorBlock` em um bloco de retorno `NO` indiscriminado — pathfinder nunca tentava atravessar, mesmo aberto. **Fix:** movido pra branch separada que retorna `YES` salvo IRON_TRAPDOOR (linhas 143-150 agora).
2. **Sem interact code pra trapdoor.** Adicionado branch dedicado em [MovementTraverse.java:241-260](../../../mod/src/main/java/baritone/pathing/movement/movements/MovementTraverse.java#L241-L260) que detecta trapdoor fechado e dispara `CLICK_RIGHT` igual porta.
3. **Sem observabilidade no branch de porta/portao/trapdoor.** Adicionado `LOGGER.info("[Alice][Interact] ...")` logado **uma vez por Movement instance** (flag `interactLogged`). Proximo teste vai mostrar no log exatamente:
   - Se o branch disparou (instanceof match?)
   - `notPassable`, `canOpen`, `alreadyOpen` pra porta
   - `trapOpen`, `isIron` pra trapdoor
   - `blocked` pra portao

### Gap redstone — **NAO sera portado neste spike**

Mecanicas que dependem de entender redstone:
- **Pressure plate abrindo porta a 1 bloco de distancia** (padrao do usuario): a porta esta fisicamente fechada quando Alice planeja o path. Plate so ativa se Alice ANDAR por cima. Pathfinder nao sabe que "pisar em X abre Y".
- **Alavanca/botao**: requer interact deliberado num bloco que nao esta no caminho. Pathfinder nao tem conceito de "side quest" pra destravar caminho.
- **Porta de ferro com redstone**: pathfinder marcaria como impassavel (correto), mas perde caminhos legitimos.

Escopo pra portar isso corretamente:
- Grafo de dependencias redstone (block → listeners).
- Pathfinding com sub-goals: "chegar em X via Y onde Y e pre-requisito".
- Custom Movement class: `MovementRedstonePrep`.
- Estimativa: **~3-5 dias de trabalho, alto risco**.

**Decisao:** ficar fora deste spike. Pro usuario: se testar com portas+plate+alavanca, Alice vai desviar ou parar. Usar portas simples pra validacao comportamental. Mark no backlog como **T-10 — redstone-aware pathfinding** (prioridade baixa).

### Arquivos tocados na iteracao 2

- [MovementHelper.java:143-150](../../../mod/src/main/java/baritone/pathing/movement/MovementHelper.java#L143-L150) — trapdoor YES/NO correto.
- [MovementTraverse.java:43,47-48,58-60,227-278](../../../mod/src/main/java/baritone/pathing/movement/movements/MovementTraverse.java) — imports, LOGGER, interactLogged flag, branch novo de trapdoor, logs de diagnostico em door/gate/trapdoor.

## Iteracao 3 — fix CONSUME, comando RCON, testes automatizados

Teste de re-validacao da iteracao 2 (15:10-15:16 UTC) expos um novo bug **muito mais grave** do que o previsto:

**Porta em (230,68,158) sem placa de pressao do outro lado** ficou 42 segundos sem abrir mesmo com Alice batendo o branch correto (`notPassable=true canOpen=true alreadyOpen=false`, CLICK_RIGHT disparado ~200 vezes). Alice so passou por essa porta quando deu a volta e veio pelo lado oposto da outra porta do aposento.

### Root cause: `BlockPlaceHelper` assume resultado client-side

[BlockPlaceHelper.java:49](../../../mod/src/main/java/baritone/utils/BlockPlaceHelper.java#L49) original so tratava `InteractionResult.SUCCESS` como sucesso:

```java
if (processRightClickBlock(...) == InteractionResult.SUCCESS) { swing(); return; }
// else cai pro OFF_HAND, tenta de novo
```

Em Minecraft vanilla **server-side**, `DoorBlock#use` / `TrapDoorBlock#use` / `FenceGateBlock#use` retornam `InteractionResult.CONSUME`, nao `SUCCESS`. SUCCESS eh client-side only (`useOn` confirma o server-roundtrip).

Entao o fluxo real era:
1. MAIN_HAND: useItemOn → **CONSUME** (porta abriu!). Codigo ve != SUCCESS → continua pro OFF_HAND.
2. OFF_HAND: useItemOn no mesmo frame → **CONSUME** (porta fechou de novo, vanilla toggle).
3. Loop termina, porta continua fechada. Proximo tick repete.

Alice literalmente abria-e-fechava a porta **no mesmo tick** por 42 segundos.

### Fix aplicado

`BlockPlaceHelper.tick` refatorado:
- Testa MAIN_HAND primeiro; se `result.consumesAction()` (cobre SUCCESS + CONSUME), retorna imediatamente (sem tentar OFF_HAND).
- So OFF_HAND se MAIN retornou PASS (nada atuou).
- Mesmo tratamento pra `processRightClick` (item use sem bloco).
- Log throttled `[Alice][BlockPlace]` reporta: hit position, face, block, main/off result — serve de diagnostico pra novas regressoes.

### Comando RCON + testes automatizados

[AliceCommands.java](../../../mod/src/main/java/com/projetoalice/alice/AliceCommands.java) novo: registra `/alicecmd {goto,stay,status,pos}` via `RegisterCommandsEvent`. Permite que pytest dirija a Alice via RCON (sem precisar de player real enviando chat).

[test-server/tests/test_door_interact.py](../../../test-server/tests/test_door_interact.py) novo:
- Constroi arena em (x=-5500, z=-5500) longe de estruturas.
- Caso A: porta de carvalho fechada + parede solida; Alice deve cruzar em 45s + porta ficar `open=true` no fim.
- Caso B: fence gate fechado; mesmo criterio.
- Caso C: trapdoor lateral (xfail — bloqueio horizontal de trapdoor e edge case).

Rodar (contra Larry):
```
ALICE_RCON_HOST=192.168.0.225 ALICE_RCON_PASSWORD=alice2026 \
  pytest test-server/tests/test_door_interact.py -v
```

### Bugs separados observados no teste (fora do escopo deste spike)

- **MovementPillar loop** em (196,78,154) — Alice repete `y=78→79 cost=8.51` 8 vezes em 12s. Suspeita: FakePlayer nao aplica velocidade Y com JUMP input, ou sneak+click-place nao efetiva o pilar. **Abrir spike G: FakePlayer physics/jump**.
- **MovementFall loop** em (217,73,153) — mesmo Fall repetido 4x. Suspeita relacionada ao spike G.

### Arquivos tocados na iteracao 3

- [BlockPlaceHelper.java](../../../mod/src/main/java/baritone/utils/BlockPlaceHelper.java) — fix CONSUME + logs `[Alice][BlockPlace]`.
- [AliceCommands.java](../../../mod/src/main/java/com/projetoalice/alice/AliceCommands.java) — novo, comandos `/alicecmd`.
- [AliceMod.java](../../../mod/src/main/java/com/projetoalice/alice/AliceMod.java) — registra `AliceCommands` no EVENT_BUS.
- [test-server/tests/test_door_interact.py](../../../test-server/tests/test_door_interact.py) — pytest automatizado.
- [test-server/tests/conftest.py](../../../test-server/tests/conftest.py) — log_tail fixture skip graceful quando rodando contra remoto.

## Arquivos lidos (referencia)

- [MovementHelper.java](../../../mod/src/main/java/baritone/pathing/movement/MovementHelper.java)
- [MovementTraverse.java](../../../mod/src/main/java/baritone/pathing/movement/movements/MovementTraverse.java)
- [Movement.java](../../../mod/src/main/java/baritone/pathing/movement/Movement.java)
- [InputOverrideHandler.java](../../../mod/src/main/java/baritone/utils/InputOverrideHandler.java)
- [BlockPlaceHelper.java](../../../mod/src/main/java/baritone/utils/BlockPlaceHelper.java)
- [ServerPlayerController.java](../../../mod/src/main/java/baritone/utils/player/ServerPlayerController.java)
- [LookBehavior.java](../../../mod/src/main/java/baritone/behavior/LookBehavior.java)
- [ServerPlayerContext.java](../../../mod/src/main/java/baritone/utils/player/ServerPlayerContext.java)
