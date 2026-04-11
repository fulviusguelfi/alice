# Spikes — Port Manual do Baritone para Forge Server-Side

**Projeto:** Alice (mod Minecraft Forge 1.20.1)
**Decisão relacionada:** #50b no brainstorm — rota principal = port manual Baritone; Plan B = Alice-Service (ver [plano-b-alice-service.md](../../planejamento/plano-b-alice-service.md))
**Status:** aguardando go/no-go formal do PO antes de iniciar Spike A
**Hard cap agregado:** 2 semanas (10 dias úteis)
**Criado em:** 2026-04-11

---

## Contexto

O Baritone é um bot de pathfinding robusto para Minecraft, porém acoplado ao cliente (classes `Minecraft`, `ClientPlayerEntity`, renderer, mouse input). Para Alice, precisamos rodar Baritone server-side dentro de um FakePlayer em Forge 1.20.1. Não existe fork Forge server-side mantido (PlayerEngine é Fabric-only, Automatone é Fabric-only). A rota escolhida é porte manual, usando Automatone como tutorial de desacoplamento.

Riscos conhecidos:
1. **Imprevisibilidade de cronograma** — pode levar semanas ou meses até "olá mundo pathfinding".
2. **Acoplamento intransponível** — pode-se descobrir no meio do port que alguma dependência client-side é irredutível e perder trabalho.

Este documento define a estratégia de spikes timeboxed com kill criteria que mitigam esses riscos, e estabelece o ponto exato em que o projeto pivota para o Plan B (Alice-Service).

---

## Princípios

- **Walking skeleton:** cada spike produz artefato verificável (JAR, log, teste automatizado). Spike que não produz artefato não passa.
- **Logs-first:** todos os spikes seguem a política de logs do projeto (prefixo `[Alice]`, TRACE/DEBUG/INFO/WARN/ERROR, zero `System.out`, zero catch silencioso).
- **Vendor/pin:** Baritone entra como fork local no monorepo, versão congelada, sem pull upstream.
- **Kill individual + kill agregado:** cada spike tem critério próprio de aborto; adicionalmente, o conjunto tem kill agregado para evitar "tecnicamente dentro do plano" acumulando 4 semanas.
- **Honestidade do artefato:** se um spike compila mas não funciona, não passa. Se funciona mas trava o server, não passa.

---

## Spikes

### Spike A — Mapa preliminar e qualitativo de acoplamentos

**Objetivo:** ler o código do Baritone e do Automatone em paralelo; produzir um documento qualitativo mapeando cada ponto de acoplamento client-side do Baritone e classificando como **trivial**, **médio** ou **bloqueio**.

**Budget:** 3 dias úteis

**Tarefas:**
- Ler `baritone/src/main/java/baritone/` com foco em referências a `Minecraft.getInstance()`, `ClientPlayerEntity`, `Mouse`, `KeyBinding`, renderer e event bus client.
- Ler o source do Automatone (fork Fabric server-side) observando como cada acoplamento foi resolvido.
- Para cada ponto de acoplamento, escrever 1 parágrafo: o que é, como o Baritone usa, como o Automatone resolveu, se a mesma solução é aplicável em Forge.

**Artefato esperado:** `docs/spikes/pathfinding-port/spike-a-acoplamento-map.md` com tabela qualitativa (não quantitativa) dos acoplamentos.

**Kill individual:** mais de 3 acoplamentos classificados como **bloqueio** sem solução aparente no Automatone.

**Go/no-go PO obrigatório** antes de iniciar Spike B.

---

### Spike B — Baritone "gutted" compila em Forge MDK

**Objetivo:** vendorizar um fork do Baritone no monorepo e fazer compilar contra Forge 1.20.1 + Java 17, removendo (stub) todas as dependências client-side identificadas no Spike A.

**Budget:** 2-3 dias úteis

**Tarefas:**
- Importar fork do Baritone como subprojeto Gradle do `mod/`.
- Stubar classes client-side conforme mapa do Spike A.
- Resolver erros de compilação um a um, documentando cada stub.

**Artefato esperado:** `mod/baritone-port/build/libs/baritone-alice-*.jar` gerado por `gradlew build`.

**Kill individual:** não compila após budget, ou número de stubs ultrapassa o que foi estimado no Spike A por fator >2x.

---

### Spike C — FakePlayer instancia Baritone sem NPE

**Objetivo:** criar um FakePlayer de teste em Forge, injetar uma instância do Baritone portado, chamar o ciclo de vida básico (init, attach, detach) sem NullPointerException.

**Budget:** 2 dias úteis

**Tarefas:**
- Criar teste de integração que sobe um servidor dedicado Forge, spawna FakePlayer, instancia Baritone.
- Tratar cada NPE com logs explícitos e correção dirigida.
- Não tentar pathfinding ainda — apenas ciclo de vida.

**Artefato esperado:** log de teste mostrando `[Alice] Baritone attached to FakePlayer <uuid>` sem exceções.

**Kill individual:** NPE irredutível em área client-side não prevista no Spike A.

**Go/no-go PO obrigatório** antes de iniciar Spike C.5.

---

### Spike C.5 — Baritone tick() roda sem travar server thread

**Objetivo:** validar que o loop de tick do Baritone, quando executado no thread do server, não bloqueia o tick principal do Minecraft por mais de 50ms (meio tick de 20 TPS).

**Budget:** 1-2 dias úteis

**Tarefas:**
- Executar Baritone em ciclo vazio (sem goal) no FakePlayer durante 5 minutos de gameplay real.
- Instrumentar logs de duração de tick no server principal.
- Verificar que `mspt` (ms per tick) não ultrapassa 50ms sustentadamente por causa do Baritone.

**Artefato esperado:** log `[Alice] baritone tick avg=<x>ms p95=<y>ms p99=<z>ms` com todos os valores <50ms.

**Kill individual:** bloqueio de tick sustentado inevitável (p99 >50ms por mais de 30s).

**Por que esse spike existe:** um port que compila e instancia mas trava o server tick é pior que não ter port. Detectar isso antes do Spike D evita estourar budget em D por sintoma causado aqui.

---

### Spike D — Pathfind com obstáculo obrigatório

**Objetivo:** FakePlayer navega de ponto A para ponto B em mundo de teste **contornando obrigatoriamente um obstáculo não-trivial**: gap de 2 blocos (exige pular) **e** parede de 1 bloco (exige contornar ou quebrar).

**Budget:** 3-5 dias úteis

**Tarefas:**
- Criar mundo de teste determinístico com o obstáculo descrito.
- Definir goal via API do Baritone portado.
- Observar execução, corrigir falhas de pathfinding.

**Artefato esperado:** vídeo ou sequência de logs mostrando FakePlayer completando o trajeto em <60s, sem intervenção manual.

**Kill individual:** path não resolve o obstáculo após budget, mesmo com correções dirigidas.

**Go/no-go PO obrigatório** antes de iniciar Spike E.

---

### Spike E — Integração real em servidor dedicado

**Objetivo:** FakePlayer navega X→Y com 1 obstáculo não-trivial em **servidor dedicado real** (não ambiente de teste unit), executando por 5 minutos sem crash, sem lag spike e sem memory leak aparente.

**Budget:** budget restante do agregado (tipicamente 2-3 dias)

**Tarefas:**
- Deploy do mod no server de teste da rede (topologia deploy cliente+servidor).
- Executar cenário de integração por 5 minutos.
- Coletar logs de ambos os lados (cliente observador + server).

**Artefato esperado:** log completo de 5 minutos + relatório de mspt/memória/erros.

**Kill individual:** crash do server, lag spike sustentado, memory leak, ou qualquer regressão que torne o servidor inutilizável para outros jogadores.

**Go/no-go PO final** — se Spike E passa, encerra a fase de spikes e a Fase 1 do projeto principal avança para Fase 2.

---

## Kill agregado (aplicado durante todo o período de spikes)

Mesmo que os spikes estejam tecnicamente dentro do kill individual, **o Plan B dispara automaticamente** se:

- **Orçamento acumulado A-E ultrapassar 3 semanas** (50% acima do hard cap de 2 semanas), **ou**
- **Dois ou mais spikes estourarem seu budget individual em mais de 50%**, **ou**
- **Spike E falha** (mesmo com A-D passando).

Esse critério existe para evitar a armadilha clássica: "spike 1 estourou um pouco, spike 2 também, mas cada um individualmente passou, então seguimos… e 4 semanas depois ainda estamos nos spikes".

Quando o kill agregado dispara, o PO é notificado e a transição para Plan B é formalmente iniciada conforme [plano-b-alice-service.md](../../planejamento/plano-b-alice-service.md).

---

## Pontos de go/no-go do PO

Go/no-go formais obrigatórios em 4 pontos:

1. **Antes de iniciar Spike A** — assinar este README + o 1-pager do Plan B.
2. **Após Spike A** — decidir seguir para Spike B ou pivotar.
3. **Após Spike C** — decidir seguir para Spike C.5/D ou pivotar.
4. **Após Spike D** — decidir seguir para Spike E ou pivotar.
5. **Após Spike E** — encerrar fase de spikes e avançar Fase 1 → Fase 2.

Entre esses pontos, a equipe tem autonomia para trabalhar. Fora desses pontos, o PO só é acionado se o kill agregado disparar.

---

## Integração ao cronograma de 6 fases

- Os spikes vivem **dentro da Fase 1** (Arquitetura/Bootstrap).
- Fase 1 só avança para Fase 2 (Core Skeleton) após Spike E passar com go do PO.
- **Cenário otimista:** 2 semanas de spikes → Fase 1 conclui → Fase 2 inicia normalmente.
- **Cenário pivot:** kill agregado dispara → Fase 1 **suspende** → abre-se Fase 1b (Alice-Service, 6 semanas cap) → Fase 2 retoma após MVP do Alice-Service → atraso total ~6-8 semanas no projeto principal.

---

## Referências

- Baritone: https://github.com/cabaletta/baritone
- Automatone (fork Fabric server-side): https://github.com/Ladysnake/Automatone
- Política de logs do projeto: ver memória `feedback_logs_first.md`
- Regra No-Fallback: ver memória `feedback_no_fallback.md`
- Decisão #50b: `docs/planejamento/brainstorm.md`
