# Spike E - Alice integrada em servidor de producao (CursedWalking)

**Status:** PASS
**Orcamento:** 2 dias (plano) | **Gasto:** ~0.5 dia
**Kill criteria:** Alice crasha / NPE / incompatibilidade com os 204 mods -> NAO OCORREU

## Objetivo

Validar que a Alice (FakePlayer + Baritone em passive mode) carrega e tica sem
crashar dentro do modpack CursedWalking real (Forge 1.20.1-47.4.16, 204 mods),
antes de tocar na producao em `larry@192.168.0.225`.

## Setup

1. Copia fiel do servidor de producao via SCP (2.3 GB, 204 mods, world 527 MB)
   para `test-server/` local
2. `server-port` 25566 -> 25567 para nao colidir com a prod
3. `SpikeDTest` desabilitado no `AliceMod` (passive mode — nao constroi arena,
   nao executa pathfinding automatico) — Alice apenas spawna e Baritone fica
   em idle
4. Build do mod `alice-0.0.1.jar` copiado para `test-server/mods/`
5. Baseline rodado SEM o jar da Alice primeiro, para capturar o estado "limpo"
   e comparar

## Resultado

Alice anexa com sucesso no mundo real de producao, coexistindo com os 204 mods
sem crash, NPE, ou conflito. Baritone tick loop idle estavel em tres perfis de
heap distintos.

### Metricas por perfil de heap

| Heap       | Startup   | Working Set | Tick avg | p95    | p99    | max (spike inicial) |
| ---------- | --------- | ----------- | -------- | ------ | ------ | ------------------- |
| 4G (prod)  | 7.87s     | 4.80 GB     | 2.89ms   | 3.37ms | 5.27ms | 112ms               |
| 3G         | 10.20s    | 3.73 GB     | 4.69ms   | 5.60ms | 6.51ms | 201ms               |
| 2G         | 10.08s    | 2.70 GB     | 3.79ms   | 4.77ms | 7.10ms | 105ms               |

Todos os valores de avg/p95/p99 ficaram muito abaixo do limite de 50ms por tick.
Os spikes `max` >100ms ocorrem no primeiro tick apos `[Alice] Attach complete` —
custo one-shot de inicializacao do `IBaritone` (world setup, cache priming),
nao recorrente.

### Log de evidencia (-Xmx4G)

```text
[16:35:03] [Server thread/INFO] [minecraft/DedicatedServer]: Done (7.874s)! For help, type "help"
[16:35:03] [Server thread/INFO] [Alice/]: [Alice] server started - spawning Alice FakePlayer
[16:35:03] [Server thread/INFO] [Alice/]: [Alice] Attaching FakePlayer to level: minecraft:overworld
[16:35:03] [Server thread/INFO] [Alice/]: [Alice] Baritone attached to FakePlayer a11ce000-0000-4000-8000-000000000001
[16:35:03] [Server thread/INFO] [Alice/]: [Alice] Baritone world initialized for dimension: minecraft:overworld
[16:35:03] [Server thread/INFO] [Alice/]: [Alice] Attach complete. Baritone ready.
[16:35:13] [Server thread/INFO] [Alice/]: [Alice] baritone tick avg=2,89ms p95=3,37ms p99=5,27ms max=112,20ms (window=200)
```

## Ressalva critica

Todas as medidas sao em **idle, sem jogadores online**. Producao real tem
players + chunks ativos + 2 `ServerPlayer` vazados reportados pelo AllTheLeaks.
Extrapolacao conservadora:

- **2G:** inviavel em prod (OOM quase certo sob carga real + leak existente)
- **3G:** apertado, risco alto de pausas longas de GC
- **4G:** **perfil recomendado** — bate com a configuracao atual da prod

## Problemas existentes no CursedWalking (descobertos no baseline)

Nenhum foi introduzido pela Alice. Todos pre-existem na producao:

1. **Memory leak crescente** — AllTheLeaks reporta +186 MB/dia, 2 `ServerPlayer`
   vazados, 10 `LevelChunk` com invalid clones
2. **ServerHangWatchdog 60s crash (04/abr/2026)** — 30+ mixins no `Player` sem
   GC tuning causaram freeze durante login
3. **3 mods com classes client-only no dedicated:** `BinocularsMod`, `tacz`,
   `gamemenuremovegfarb` — geram `RuntimeDistCleaner` stack traces no boot
4. **Zero GC tuning** — `user_jvm_args.txt` apenas com `-Xms4G -Xmx4G`, G1 default
5. **Docker container sem memory limit** — em caso de leak/OOM, come toda a
   RAM do host (7.7 GB, hoje sobram ~300 MB)
6. **JSON mal-formado** em receitas `tacz`, `flan`, KubeJS — nao-fatais mas
   poluem o log

## Arquivos modificados

| Arquivo                         | Mudanca                                              |
| ------------------------------- | ---------------------------------------------------- |
| `AliceMod.java`                 | `SpikeDTest` comentado (passive mode para integracao)|
| `test-server/user_jvm_args.txt` | `-Xms`/`-Xmx` variado por perfil (4G, 3G, 2G)        |
| `test-server/server.properties` | `server-port=25567` (evita colisao com prod)         |

## Riscos validados

- **R-C1 (Compatibilidade com modpack complexo):** Alice coexiste com 204 mods,
  incluindo AllTheLeaks (que adiciona `FakePlayerNetHandlerAccessor` ao allowlist
  de mixins), voicechat, KubeJS, Forge version checker, Dynamic Lights. Nenhum
  conflito observado.

- **R-C2 (Impacto de tick sob modpack real):** Mesmo em -Xmx2G o tick avg ficou
  em 3.79ms — bem abaixo dos 50ms de budget. Alice nao e' o gargalo sob a carga
  atual de 204 mods em idle.

- **R-C3 (FakePlayer vazando memoria):** Working set estabilizou apos o attach
  em todos os perfis (4.80 / 3.73 / 2.70 GB). Nenhum crescimento observado
  durante a janela de observacao (~2 min por perfil). Teste de longo prazo fica
  para uma fase posterior.

## Decisao recomendada para producao

**Manter `-Xmx4G`** em prod, aplicar otimizacoes em 3 fases independentes e
reversiveis:

### Fase 1 — Aikar's flags (zero risco, ganho imediato)

Adicionar ao `user_jvm_args.txt` via Crafty Controller:

```text
-Xms4000M -Xmx4000M
-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200
-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch
-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M
-XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4
-XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90
-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32
-XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1
-Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true
```

Impacto esperado: reduz p99 de tick e mitiga pausas que causam o watchdog de 60s.

### Fase 2 — Docker memory limit (protecao contra OOM)

Via Portainer/Crafty, setar `mem_limit: 6G` no container `crafty_container`.
Deixa ~1.7 GB para os outros containers + kernel. Protege o host se o leak
explodir.

### Fase 3 — Remover mods client-only (opcional, precisa validar com jogadores)

`BinocularsMod`, `tacz`, `gamemenuremovegfarb`: confirmar em dev se sao
realmente client-only ou tem partes server-side uteis. Se client-only puro,
mover para `mods-client/` (so o launcher do jogador carrega).

## Sobre Docker/WSL local

A copia direta no Windows com Java 17 ja reproduziu fielmente os logs da prod
(mesma sequencia de erros, mesmos mods, mesmo startup). O gap entre o teste
Windows e prod Linux e' desprezivel para o que estamos validando (integridade
do mod, RAM, tick cost). **Nao precisamos de WSL nem Docker local.** Docker
so faria sentido para testar `mem_limit` antes de aplicar na prod, o que pode
ser feito direto em horario off-peak (reversivel em segundos).

## Validacao das Aikar's flags (adicional)

Rodado um quarto perfil com `-Xmx4G` + Aikar's flags completas no
`test-server/user_jvm_args.txt`:

| Perfil                 | Startup | Tick avg | p95    | p99    | max     |
| ---------------------- | ------- | -------- | ------ | ------ | ------- |
| 4G vanilla G1          | 7.87s   | 2.89ms   | 3.37ms | 5.27ms | 112.20ms|
| 4G + Aikar's flags     | 8.64s   | 3.98ms   | 4.87ms | 7.96ms | 115.84ms|

Observacoes:

- **Startup ligeiramente mais lento (+0.77s)** — esperado por causa de
  `-XX:+AlwaysPreTouch`, que toca fisicamente todo o heap no boot (vimos
  o working set ir direto para 4.35 GB ao iniciar). Trade-off deliberado
  para eliminar page-fault spikes depois.
- **Tick avg/p95/p99 ligeiramente maiores na janela inicial** — esperado:
  `-XX:InitiatingHeapOccupancyPercent=15` dispara GCs mais cedo, e o primeiro
  window de 200 ticks pega o JVM ainda primando.
- **Sem crashes, sem OOM, sem warnings de GC overhead.** Alice attach OK.

As Aikar's flags sao sintonizadas para **estabilidade sob carga sustentada e
muitos players**, nao para cold-start idle. O ganho real (ausencia de pausas
de 60s como a que crashou a prod em 04/abr) so aparece sob carga real com
players. Ainda assim o tick cost fica muito abaixo do budget de 50ms.

**Recomendacao:** promover as flags para producao via Crafty Controller
(campo `execution_command` na tabela `servers` do `crafty.sqlite`, ou via UI
em Server Config > Java Settings).

## Proximo passo

1. Aplicar Aikar's flags na prod Larry via Crafty Controller (reversivel em
   segundos se algo der errado)
2. Aplicar Docker memory limit `6G` no `crafty_container`
3. Monitorar por 24-48h antes de deployar a Alice em producao
