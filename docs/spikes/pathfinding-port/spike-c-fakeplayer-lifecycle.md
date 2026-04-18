# Spike C - FakePlayer instancia Baritone sem NPE

**Status:** PASS  
**Orcamento:** 2 dias (plano) | **Gasto:** ~0.3 dia  
**Kill criteria:** NPE irredutivel em area client-side nao prevista no Spike A -> NAO OCORREU  

## Resultado

O servidor dedicado Forge 1.20.1 inicia, spawna um FakePlayer "Alice" no overworld,
instancia Baritone para esse FakePlayer, inicializa o world provider, e roda ticks sem
nenhuma excecao.

### Log de evidencia

```
[Server thread/INFO] [co.pr.al.AliceMod/]: [Alice] server started - spawning Alice FakePlayer
[Server thread/INFO] [Alice/]: [Alice] Attaching FakePlayer to level: minecraft:overworld
[Server thread/INFO] [Alice/]: [Alice] FakePlayer created: uuid=a11ce000-0000-4000-8000-000000000001, name=Alice
[Server thread/INFO] [Alice/]: [Alice] FakePlayer positioned at spawn: BlockPos{x=0, y=115, z=-32}
[Server thread/INFO] [Baritone/]: [Baritone] Baritone settings file not found, resetting.
[Server thread/INFO] [Alice/]: [Alice] Baritone attached to FakePlayer a11ce000-0000-4000-8000-000000000001
Baritone world data dir: .\world\.\baritone\minecraft\overworld_384
[Server thread/INFO] [Alice/]: [Alice] Baritone world initialized for dimension: minecraft:overworld
[Server thread/INFO] [Alice/]: [Alice] Attach complete. Baritone ready.
[Server thread/INFO] [minecraft/DedicatedServer]: Done (25.432s)! For help, type "help"
```

Zero NPE. Zero exceptions. Servidor rodou ticks normalmente apos attach.

## Arquivos criados/modificados

### Criados
| Arquivo | Descricao |
|---------|-----------|
| `AliceEntity.java` | Gerencia lifecycle do FakePlayer + Baritone (attach/tick/detach) |

### Modificados
| Arquivo | Mudanca |
|---------|---------|
| `AliceMod.java` | Hooks: `ServerStartedEvent` -> attach, `ServerTickEvent` -> tick, `ServerStoppingEvent` -> detach |

## Decisoes tecnicas

1. **Forge FakePlayer usado diretamente** - `FakePlayerFactory.get(level, profile)` funciona sem
   modificacao. Nao foi necessario criar FakePlayer customizado (diferente do Automatone/Fabric).
   
2. **GameProfile fixo** - UUID `a11ce000-0000-4000-8000-000000000001`, nome "Alice".

3. **Attach no `ServerStartedEvent`** (nao `ServerStartingEvent`) - o mundo precisa estar pronto
   com chunks carregados para o FakePlayer ser posicionado.

4. **Tick no `ServerTickEvent.Phase.END`** - Baritone processa apos o tick vanilla para ter
   estado atualizado do mundo.

5. **WorldProvider.initWorld()** chamado explicitamente - necessario para Baritone criar
   o cache de mundo e diretorio de dados.

## Riscos validados

- **R-A1 (FakePlayer do Forge suficiente?)** -> SIM. FakePlayer do Forge ja tem:
  - `ServerPlayerGameMode` funcional
  - `FakePlayerNetHandler` que NO-OPs todos os packets
  - Inventario funcional
  - Posicionamento no mundo
  
- **Baritone constructor sem NPE** -> PASS. Todos os behaviors e processes inicializam
  sem tocar em estado client-side.

## Proximo passo

**Go/no-go PO obrigatorio** antes de Spike C.5 (Baritone tick() sem travar server thread).
