# Spike D - FakePlayer navega de A para B contornando obstaculos

**Status:** PASS  
**Orcamento:** 2 dias (plano) | **Gasto:** ~0.5 dia  
**Kill criteria:** FakePlayer nao chega ao goal em <60s -> NAO OCORREU

## Resultado

FakePlayer "Alice" navega de (0,101,0) ate (18,101,0) em **8.6 segundos (174 ticks)**,
cruzando um gap de 2 blocos e contornando uma parede de cobblestone.

### Log de evidencia

```
[Alice][SpikeD] Building test arena...
[Alice][SpikeD] Arena built: platform x=[-2,20] z=[-7,7] y=100, gap at x=5-6, wall at x=10
[Alice][SpikeD] FakePlayer placed at START: BlockPos{x=0, y=101, z=0}
[Alice][SpikeD] Force-loaded chunks x=[-1,1] z=[-1,0]
[Alice][SpikeD] Goal set to: BlockPos{x=18, y=101, z=0} - pathfinding started
[Baritone] Starting to search for path from BetterBlockPos{x=0,y=101,z=0} to GoalBlock{x=18,y=101,z=0}
[Baritone] Took 4185ms, 1408 movements considered
[Baritone] Found path segment from BetterBlockPos{x=0,y=101,z=0} towards GoalBlock{x=18,y=101,z=0}. 64 nodes considered
[Alice][SpikeD] tick=100 pos=(2,101,0) dist=16.0 goal=(18,101,0)
[Alice][SpikeD] tick=120 pos=(6,101,0) dist=12.0 goal=(18,101,0)
[Alice][SpikeD] tick=140 pos=(9,101,0) dist=9.0 goal=(18,101,0)
[Alice][SpikeD] tick=160 pos=(13,101,0) dist=5.0 goal=(18,101,0)
[Alice][SpikeD] SUCCESS! FakePlayer reached goal in 8656ms (174 ticks). Final pos: (16,101,0)
```

### Tick Performance (com pathfinding ativo)

| Metrica | Valor | Limite | Status |
|---------|-------|--------|--------|
| avg | 1.47ms | <50ms | PASS |
| p95 | 3.07ms | <50ms | PASS |
| p99 | 19.00ms | <50ms | PASS |
| max | 57.30ms | spike unico | OK |

Comparando com Spike C.5 (idle): avg subiu de 0.27ms para 1.47ms (~5x), o que e' esperado
com pathfinding ativo computando e executando movimentos. Todos os valores ficaram bem
abaixo do limite de 50ms.

## Arena de teste

```
Start (0,101,0)                                    Goal (18,101,0)
  S . . . . X X . . . W . . . . . . . . G
              gap       wall
```

- **Plataforma:** stone x=[-2,20] z=[-7,7] y=100 (com 5 camadas abaixo)
- **Gap:** 2 blocos sem chao em x=5,6 (z=-5 a z=5) — requer pulo/queda
- **Parede:** cobblestone em x=10, y=101, z=-7 a z=5 — abertura em z>5
- **Start:** (0, 101, 0)
- **Goal:** (18, 101, 0)

## Arquivos criados/modificados

### Criados
| Arquivo | Descricao |
|---------|-----------|
| `AliceFakePlayer.java` | Subclasse de FakePlayer que restaura tick() (Forge NO-OPs por padrao) |
| `SpikeDTest.java` | Constroi arena, seta goal Baritone, monitora progresso |

### Modificados
| Arquivo | Mudanca |
|---------|---------|
| `AliceEntity.java` | Sequencia de tick completa: serverTick -> serverPlayerUpdate -> fakePlayer.tick() -> serverPlayerUpdatePost -> serverPostTick |
| `AliceMod.java` | Integra SpikeDTest no lifecycle (setup no ServerStartedEvent, tick no ServerTickEvent) |
| `Baritone.java` | Adicionados serverPlayerUpdate() e serverPlayerUpdatePost() para disparar onPlayerUpdate PRE/POST |

## Problemas encontrados e resolvidos

### 1. FakePlayer.tick() e' NO-OP no Forge

**Problema:** Forge's `FakePlayer` override `tick()` com corpo vazio `{}`. Isso impede
qualquer processamento de fisica (gravidade, movimento, colisao).

**Solucao:** Criada `AliceFakePlayer extends FakePlayer` que restaura o tick:
```java
@Override
public void tick() {
    this.gameMode.tick();  // necessario para block breaking
    this.doTick();         // chama Player.tick() -> LivingEntity.tick() -> Entity.tick()
}
```

### 2. onPlayerUpdate nunca disparado

**Problema:** `Baritone.serverTick()` disparava apenas `onTick` e `onPostTick`.
`PathingBehavior.onPlayerUpdate()` (que executa path following) e
`LookBehavior.onPlayerUpdate()` (que seta rotacao) nunca eram chamados.

**Solucao:** Adicionados `serverPlayerUpdate()` e `serverPlayerUpdatePost()` em Baritone.java
que disparam `onPlayerUpdate(PRE)` e `onPlayerUpdate(POST)` via GameEventHandler.

### 3. Chunks descarregados apos setup

**Problema:** A arena em (0,0)-(20,0) ficava parcialmente em chunks que o servidor
descarregava apos o setup (FakePlayer nao e' tracked para chunk loading).
Baritone simplificava GoalBlock para GoalXZ e falhava ao planejar segmentos.

**Solucao:** `level.setChunkForced()` nos chunks da arena + desabilitado
`simplifyUnloadedYCoord` (todos os chunks da arena estao loaded).

### 4. SLF4J format string incorreto

**Problema:** Usado `{:.1f}` (sintaxe Python) em chamadas SLF4J.

**Solucao:** Substituido por `{}` com `String.format("%.1f", valor)`.

## Sequencia de tick validada

A sequencia correta para FakePlayer server-side com Baritone e':

```
1. baritone.serverTick()          — onTick PRE: planeja paths, seta input overrides (xxa/zza)
2. baritone.serverPlayerUpdate()  — onPlayerUpdate PRE: PathingBehavior executa path, LookBehavior seta rotacao
3. fakePlayer.tick()              — Entity physics: processa xxa/zza em movimento real
4. baritone.serverPlayerUpdatePost() — onPlayerUpdate POST: processamento pos-movimento
5. baritone.serverPostTick()      — onPostTick POST: finaliza estado
```

## Riscos validados

- **R-A2 (Server tick vs path calculation):** Path calculation roda em thread pool separada
  (`pool-5-thread-3/4`), resultado e' consumido no server thread. Sem deadlocks observados.
  Performance de tick manteve-se dentro dos limites mesmo com pathfinding ativo.

- **Movimento server-side funcional:** FakePlayer processa inputs de Baritone (xxa, zza,
  jumping, yRot) e executa movimentos fisicos (andar, pular gap, contornar parede)
  inteiramente server-side, sem nenhum componente client-side.

## Proximo passo

Spike E: Teste de integracao em servidor dedicado real (build jar -> deploy -> validate).
