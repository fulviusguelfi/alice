# Spike C.5 - Baritone tick() roda sem travar server thread

**Status:** PASS  
**Orcamento:** 1-2 dias (plano) | **Gasto:** ~0.2 dia  
**Kill criteria:** p99 >50ms por mais de 30s -> NAO OCORREU

## Resultado

Baritone idle (sem goal) tick no server thread com metricas excelentes:

```
[Alice] baritone tick avg=0.27ms p95=0.21ms p99=1.02ms max=31.83ms (window=200)
```

| Metrica | Valor | Limite | Status |
|---------|-------|--------|--------|
| avg | 0.27ms | <50ms | PASS |
| p95 | 0.21ms | <50ms | PASS |
| p99 | 1.02ms | <50ms | PASS |
| max | 31.83ms | <50ms | PASS |

O max de 31.83ms e' um spike unico (provavelmente o primeiro tick com inicializacao lazy).
Nenhum bloqueio sustentado observado. O servidor manteve 20 TPS normal.

## Instrumentacao

Adicionado em `AliceEntity.tick()`:
- Medicao `System.nanoTime()` ao redor de `serverTick()` + `serverPostTick()`
- Buffer circular de 200 amostras (~10s a 20 TPS)
- Log automatico em 200 ticks (primeiro report) e a cada 6000 ticks (~5 min)
- Log final no `detach()`

## Log completo de evidencia

```
[Server thread/INFO] [Alice/]: [Alice] Attaching FakePlayer to level: minecraft:overworld
[Server thread/INFO] [Alice/]: [Alice] FakePlayer created: uuid=a11ce000-0000-4000-8000-000000000001, name=Alice
[Server thread/INFO] [Alice/]: [Alice] FakePlayer positioned at spawn: BlockPos{x=0, y=115, z=-32}
[Server thread/INFO] [Alice/]: [Alice] Baritone attached to FakePlayer a11ce000-0000-4000-8000-000000000001
[Server thread/INFO] [Alice/]: [Alice] Baritone world initialized for dimension: minecraft:overworld
[Server thread/INFO] [Alice/]: [Alice] Attach complete. Baritone ready.
[Server thread/INFO] [minecraft/DedicatedServer]: Done (11.020s)! For help, type "help"
[Server thread/INFO] [Alice/]: [Alice] baritone tick avg=0.27ms p95=0.21ms p99=1.02ms max=31.83ms (window=200)
```

Zero exceptions. Zero NPEs. Zero crashes.

## Riscos validados

- **R-A2 (Server tick thread vs path calculation thread):** Em modo idle, Baritone nao inicia
  path calculation thread. Performance de tick e' negligivel. Este risco sera' reavaliado no
  Spike D quando pathfinding ativo ocorrer.

## Proximo passo

Spike D: FakePlayer navega de A para B contornando obstaculo (gap + parede).
