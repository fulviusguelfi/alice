# Projeto Alice — Processo de Desenvolvimento

**Versão:** 1.0  
**Data:** 2026-04-04  
**Status:** Documento vivo — referência oficial de processo

---

## Sumário

1. [Análise Crítica — Gap Analysis](#gap-analysis)
2. [Casos de Sucesso — Lições Extraídas](#casos-de-sucesso)
3. [Agentes de Desenvolvimento](#agentes-dev)
4. [Convenções e Regras de Desenvolvimento](#convencoes)
5. [Estratégia de Testes](#testes)
6. [Pipeline CI/CD](#cicd)
7. [Release Management](#release)
8. [Gestão de Riscos e Issues](#riscos)
9. [Infraestrutura de Desenvolvimento](#infra)
10. [Tabela de Ganhos por Investimento](#ganhos)

---

## 1. Análise Crítica — Gap Analysis {#gap-analysis}

### O que já temos (definido na sessão anterior)

| Área | Status | Documento |
|------|--------|-----------|
| Plano de 6 fases com DOR/DOD | ✅ Completo | `plano-projeto-alice.md` |
| 7 agentes in-game (IA da Alice) | ✅ Completo | `agentes.md` |
| 15 regras always-on (comportamento tick) | ✅ Completo | `regras.md` |
| 20 skills composíveis | ✅ Completo | `skills.md` |
| Arquitetura de referência (8 camadas) | ✅ Completo | `plano-projeto-alice.md` |
| 49 decisões arquiteturais | ✅ Completo | brainstorm v0.9 |

### O que falta (gaps identificados)

| # | Gap | Impacto | Prioridade | Referência de Sucesso |
|---|-----|---------|------------|----------------------|
| G01 | **Agentes de desenvolvimento** — nenhum agente Claude Code definido para o processo de dev | Sem automação, tudo manual, risco de inconsistência | CRÍTICO | Baritone: automação de build/test |
| G02 | **Estratégia de testes** — zero testes definidos (unit, integration, gametest) | Regressões silenciosas, bugs acumulam entre fases | CRÍTICO | Mekanism: JUnit + GameTest |
| G03 | **CI/CD pipeline** — sem build automatizado, sem deploy automatizado | Build manual, deploy manual, erros humanos | ALTO | Baritone: GitHub Actions + Gradle |
| G04 | **Convenções de código** — sem padrão de formatação, naming, commits | Código inconsistente, dívida técnica desde dia 1 | ALTO | Create: Checkstyle + EditorConfig |
| G05 | **Release management** — sem versionamento, changelog, publicação | Releases caóticas, sem rastreabilidade | MÉDIO | ModPublisher: CurseForge + Modrinth + GitHub |
| G06 | **Documentação** — sem wiki, sem javadoc, sem guia de uso | Impossível para outros contribuírem, difícil para nós relembrarmos | MÉDIO | Create: wiki comunitária extensa |
| G07 | **Gestão de riscos** — riscos listados no brainstorm mas sem processo formal | Riscos se materializam sem plano de contingência | MÉDIO | Projetos enterprise: risk registry |
| G08 | **Performance baseline** — sem benchmark de TPS, sem budget de tick | Mod laga servidor, removido de modpacks | ALTO | Baritone: YourKit profiling |
| G09 | **Infraestrutura como código** — setup das 3 máquinas é manual | Reinstalar = dias perdidos | BAIXO | Docker Compose para dev env |
| G10 | **Code review process** — sem checklist, sem critérios de aprovação | Bugs passam, padrões degradam | ALTO | Applied Energistics 2: review rigoroso |

### Diagnóstico

**O que temos é bom para PLANEJAR. O que falta é o que faz o desenvolvimento ACONTECER.**

Os 7 agentes in-game definem o que a Alice faz no jogo. Mas não temos nenhum agente que nos ajude a **construir** a Alice. É como ter o blueprint de um prédio sem a construtora.

---

## 2. Casos de Sucesso — Lições Extraídas {#casos-de-sucesso}

### Caso 1: Baritone (8.8k ⭐, 85 releases, 1.8k forks)

**O que é:** Pathfinding bot para Minecraft — a mesma lib que Alice usa.

**O que fazem certo:**
- CI com GitHub Actions (`Java CI with Gradle`) — build automático em todo push/PR
- Codacy para análise estática de código — qualidade medida objetivamente
- Snyk para scanner de vulnerabilidades em dependências
- YourKit Profiler para performance — crucial para mod que roda a cada tick
- 85 releases versionadas — cadência regular, changelogs claros
- Suporte multi-plataforma (Forge, Fabric, NeoForge) desde o início
- API separada da implementação (`baritone-api.jar`) — clean architecture

**Lição para Alice:**
> Baritone prova que um mod server-side com tick-based logic **precisa** de CI + profiling + API separation. Alice tem a mesma natureza — regras a cada tick, pathfinding, combate. Sem profiling, um bug na `RuleEngine` pode derrubar o TPS de 20 para 5.

**Ação concreta:** Configurar GitHub Actions com build + test no dia 1. Adicionar profiling com VisualVM (gratuito) desde a Fase 1.

---

### Caso 2: Create Mod (mod mais popular de tech/automação)

**O que é:** Mod de engenharia mecânica com rotações, esteiras, trens.

**O que fazem certo:**
- Arquitetura modular — addons (Create: Crafts & Additions, Create: Steam 'n Rails) estendem sem modificar core
- Wiki comunitária extensa — a razão pela qual jogadores adotam um mod complexo
- Performance obsessiva — otimizam renderização e tick por componente
- Versioning semântico claro com support matrix por versão de Minecraft

**Lição para Alice:**
> Create prova que **documentação é adoção**. Sem wiki, ninguém entende um mod complexo. Alice tem LLM, agentes, skills, regras — é tão complexa quanto Create. A wiki não é "nice to have", é survival.

**Ação concreta:** Criar estrutura de GitHub Wiki desde a Fase 1, com auto-geração parcial por agente.

---

### Caso 3: Mekanism (mod popular de tech/processamento)

**O que é:** Mod de maquinário, energia, ferramentas avançadas.

**O que fazem certo:**
- JUnit extensivo para recipes, configs, multiblocks — testes onde mais quebra
- GitHub Actions CI com matrix build (múltiplas versões MC)
- Issue templates estruturados (bug report, feature request, crash report)
- Changelog automático baseado em commits

**Lição para Alice:**
> Mekanism prova que **testar config e recipes** é tão importante quanto testar lógica. Alice tem `AliceConfig`, `RuleEngine`, `SkillMatcher` — componentes que são configuráveis. Se um teste não valida que `FleeOnLowHealthRule` dispara em HP < 20%, o jogador morre e culpa o mod.

**Ação concreta:** JUnit 5 para toda regra e skill desde a Fase 1. GameTest Framework para validar FakePlayer + Baritone in-game.

---

### Caso 4: AI-Player / shasankp000 (109 ⭐, projeto similar)

**O que é:** Mod com IA companion usando LLM — o concorrente direto.

**O que fazem certo:**
- Integração com múltiplos LLM providers (OpenAI, Anthropic, Gemini, Ollama)
- RAG com embeddings (`nomic-embed-text`) para reduzir alucinações
- Task decomposition (quebra instruções em sub-tarefas)
- Website dedicado com docs

**O que fazem errado (e que Alice deve evitar):**
- Usa internals do Carpet Mod → frágil, admite reescrever
- Sem testes automatizados visíveis no repo
- Q-Learning com tabela Q simples → não escala
- Sem combate funcional (roadmap, não implementado)
- Fabric only → mercado menor para modpacks de sobrevivência

**Lição para Alice:**
> AI-Player valida a demanda (jogadores querem companion IA). Mas a execução técnica tem gaps sérios. Alice pode ser superior em:
> 1. FakePlayer nativo (vs Carpet internals)
> 2. Regras always-on para combate imediato (vs Q-table reativa)
> 3. Testes automatizados (vs zero testes)
> 4. Forge 1.20.1 (vs Fabric 1.21.1 — mais modpacks de sobrevivência em Forge)

---

### Caso 5: Forge-AI-Player / dongge0210 (port para Forge)

**O que é:** Port do AI-Player para Forge 1.20.1 — exatamente nossa versão.

**Lição para Alice:**
> Confirma que a demanda existe em Forge 1.20.1. Mas como port, herda todos os problemas do original. Alice tem oportunidade de ser o "original feito certo" em Forge.

---

### Síntese dos Casos

| Prática | Baritone | Create | Mekanism | AI-Player | Alice (atual) |
|---------|----------|--------|----------|-----------|---------------|
| CI/CD | ✅ GitHub Actions | ✅ | ✅ GitHub Actions | ❌ | ❌ **GAP** |
| Testes automatizados | ✅ | ✅ | ✅ JUnit + GameTest | ❌ | ❌ **GAP** |
| Análise estática | ✅ Codacy | ✅ Checkstyle | ✅ | ❌ | ❌ **GAP** |
| Performance profiling | ✅ YourKit | ✅ obsessivo | ✅ | ❌ | ❌ **GAP** |
| Wiki/documentação | ⚠️ básica | ✅ extensa | ✅ boa | ⚠️ website | ❌ **GAP** |
| Release automatizado | ✅ 85 releases | ✅ | ✅ | ⚠️ manual | ❌ **GAP** |
| Versionamento semântico | ✅ | ✅ | ✅ | ✅ | ❌ **GAP** |
| API separation | ✅ excelente | ✅ | ✅ | ❌ | ⚠️ planejado |
| Issue templates | ⚠️ básico | ✅ | ✅ estruturado | ❌ | ❌ **GAP** |
| Code of Conduct | ✅ | ✅ | ✅ | ❌ | ❌ **GAP** |

**Alice tem ZERO práticas de processo implementadas.** Todo o trabalho até agora foi planejamento funcional (o que a Alice faz), não processo de engenharia (como construímos a Alice).

---

## 3. Agentes de Desenvolvimento {#agentes-dev}

> **Distinção importante:** Os 7 agentes do `agentes.md` são agentes IN-GAME (rodam dentro do Minecraft via LLM). Os agentes abaixo são agentes de DESENVOLVIMENTO (rodam no Claude Code, nos ajudam a construir o mod).

### AD01 — Agente Arquiteto

**Nome:** `architect`  
**Propósito:** Garantir que toda decisão de código respeita as 49 decisões arquiteturais e a estrutura de 8 camadas  
**Quando usar:** Antes de implementar uma feature nova, ao planejar refactoring, ao revisar PRs  

**Responsabilidades:**
- Validar que nova feature se encaixa na camada correta (Percepção → Voz → Regras → Orquestrador → Agentes → Skills → Ação → Memória)
- Verificar que dependências fluem top-down (camada superior nunca depende de inferior)
- Garantir que decisões do brainstorm são respeitadas (ex: FakePlayer, não Custom Mob)
- Propor estrutura de pacotes para features novas
- Identificar quando uma mudança quebra o contrato entre camadas

**Contexto necessário:**
- `docs/brainstorm/projeto-alice-brainstorm.md` (decisões)
- `docs/planejamento/plano-projeto-alice.md` (arquitetura)
- Estrutura atual de pacotes do projeto

**Caso de uso:**
> "Preciso adicionar o sistema de memória. Onde fica?" → Architect valida que memória é a camada 8, define pacotes `memory/local/` e `memory/remote/`, verifica que `SavedData` é para local e `JGit` para remote conforme decisão #38/#39.

---

### AD02 — Agente Codificador

**Nome:** `coder`  
**Propósito:** Implementar features seguindo as convenções do projeto, gerando código Java para Forge 1.20.1  
**Quando usar:** Para implementar entregáveis de cada fase

**Responsabilidades:**
- Escrever código Java seguindo convenções definidas (seção 4)
- Usar APIs corretas: Forge Events, Baritone API, ollama4j, SavedData
- Implementar interfaces definidas (`IAliceRule`, `AliceLLMProvider`, etc.)
- Gerar código que compila contra Forge 1.20.1-47.3.0
- Não inventar abstrações — seguir o plano

**Contexto necessário:**
- Fase atual e seus entregáveis
- Interfaces e records já definidos
- `build.gradle` com dependências
- Código existente no projeto

**Caso de uso:**
> "Implementar FleeOnLowHealthRule" → Coder lê a interface `IAliceRule`, lê o código de exemplo em `regras.md`, implementa a regra com testes unitários correspondentes.

---

### AD03 — Agente Revisor

**Nome:** `reviewer`  
**Propósito:** Revisar código com checklist específico para qualidade, performance, e aderência ao plano  
**Quando usar:** Após implementação de qualquer feature, antes de merge/commit

**Responsabilidades:**
- **Correção:** Código faz o que deveria? Cobre edge cases?
- **Performance:** Código roda a cada tick? Tem alocação desnecessária no hot path? `new` dentro de loop de tick?
- **Forge compliance:** Usa APIs públicas? Thread-safe para server tick? Não bloqueia main thread?
- **Aderência ao plano:** Implementa o que estava definido, não mais, não menos?
- **Segurança:** Input do jogador é sanitizado? LLM response é tratada?
- **Testes:** Feature tem teste correspondente?

**Checklist de Review:**
```
□ Compila sem warnings
□ Testes passam (JUnit + GameTest se aplicável)
□ Sem alocação no hot path (tick handler, rule engine)
□ Chamadas LLM são async (CompletableFuture)
□ Sem acesso a mundo do lado do cliente em código server-side
□ Logging adequado (não excessivo, não ausente)
□ Config externalizável onde faz sentido
□ Javadoc em métodos públicos da API
□ Segue naming conventions do projeto
□ Sem TODO/FIXME sem issue associada
```

**Caso de uso:**
> Após implementar `AliceBaritoneController`, o Reviewer verifica: "Baritone é chamado na server thread? `getBaritoneForPlayer()` pode retornar null? O follow tem distância máxima configurável?"

---

### AD04 — Agente Testador

**Nome:** `tester`  
**Propósito:** Escrever testes automatizados e validar critérios DOD  
**Quando usar:** Após cada implementação, para validar DOD de fase

**Responsabilidades:**
- Escrever testes JUnit 5 para lógica pura (regras, skills, config, parsing)
- Escrever GameTests para interações in-game (FakePlayer spawn, Baritone movement, combat)
- Criar test fixtures e mocks necessários
- Validar cada item do DOD da fase atual
- Reportar cobertura de testes

**Estrutura de testes:**
```
src/test/java/com/alice/
├── behavior/rules/
│   ├── FleeOnLowHealthRuleTest.java
│   ├── AttackNearestHostileRuleTest.java
│   └── RuleEngineTest.java
├── llm/
│   ├── OllamaProviderTest.java
│   └── ChatHandlerTest.java
├── config/
│   └── AliceConfigTest.java
└── gametest/
    ├── AliceSpawnTest.java        # GameTest: Alice spawna corretamente
    ├── AliceFollowTest.java       # GameTest: Alice segue jogador
    └── AliceCombatTest.java       # GameTest: Alice ataca hostile
```

**Caso de uso:**
> "Fase 1 DOD: Alice ataca mobs hostis num raio de 8 blocos" → Tester cria `AttackNearestHostileRuleTest` com cenário: mob a 5 blocos → regra ativa; mob a 10 blocos → regra inativa; mob a 8 blocos exatos → regra ativa (boundary).

---

### AD05 — Agente de Documentação

**Nome:** `documenter`  
**Propósito:** Manter wiki do GitHub, javadoc, e guias de uso atualizados  
**Quando usar:** Após conclusão de cada fase, quando API pública muda

**Responsabilidades:**
- Manter GitHub Wiki com estrutura definida (seção abaixo)
- Gerar/atualizar javadoc para classes e métodos públicos
- Criar guias de instalação e configuração
- Documentar API pública para potenciais addons
- Manter CHANGELOG.md atualizado

**Estrutura da Wiki:**
```
wiki/
├── Home.md                        # Visão geral, links rápidos
├── Instalação.md                  # Como instalar no servidor/cliente
├── Configuração.md                # Todas as configs com exemplos
├── Guia-do-Jogador/
│   ├── Primeiros-Passos.md        # Spawn, comandos básicos
│   ├── Comandos.md                # Lista completa de comandos
│   ├── Combate.md                 # Como Alice combate
│   ├── Construção.md              # Como Alice constrói
│   └── Personalidade.md          # Como conversar com Alice
├── Guia-do-Modpack-Dev/
│   ├── Integração.md              # Como adicionar Alice ao modpack
│   ├── Compatibilidade.md         # Mods compatíveis/incompatíveis
│   └── Performance.md             # Tuning para servidores
├── Desenvolvimento/
│   ├── Arquitetura.md             # Visão geral técnica
│   ├── API.md                     # API pública para addons
│   ├── Contribuindo.md            # Como contribuir
│   └── Build-From-Source.md       # Como compilar
└── FAQ.md                         # Perguntas frequentes
```

**Caso de uso:**
> Fase 1 concluída → Documenter cria `Primeiros-Passos.md` com: como spawnar Alice, como dar comandos (follow/stay/goto), como configurar URL do Ollama. Gera javadoc para `AliceFakePlayer`, `IAliceRule`, `AliceLLMProvider`.

---

### AD06 — Agente de Release

**Nome:** `release-manager`  
**Propósito:** Gerenciar versionamento, builds, changelog, publicação em plataformas  
**Quando usar:** Ao concluir uma fase (milestone), para hotfixes críticos

**Responsabilidades:**
- Manter versionamento semântico: `MAJOR.MINOR.PATCH` (ex: `0.1.0` = Fase 1 alpha)
- Gerar changelog a partir de commits convencionais
- Criar release no GitHub com assets (`.jar`, notas)
- Publicar em CurseForge e Modrinth (quando pronto para público)
- Criar tags Git para cada release
- Validar que todos os testes passam antes de release

**Esquema de Versões:**
```
Fase 1 → 0.1.x (alpha — fundação)
Fase 2 → 0.2.x (alpha — utilidade)
Fase 3 → 0.3.x (alpha — voz)
Fase 4 → 0.4.x (beta — construção)
Fase 5 → 0.5.x (beta — guia)
Fase 6 → 1.0.x (release — inteligência completa)

Patches: 0.1.1, 0.1.2... para bugfixes dentro da fase
```

**Ferramentas:**
- `mod-publish-plugin` (Gradle) — publica em CurseForge + Modrinth + GitHub simultaneamente
- Commit convencional → changelog automático
- GitHub Release com template

**Caso de uso:**
> Fase 1 DOD completo → Release Manager cria tag `v0.1.0`, gera changelog, builda `.jar` final, cria GitHub Release com notas, marca milestone como fechada.

---

### AD07 — Agente de Infraestrutura

**Nome:** `infra`  
**Propósito:** Gerenciar setup das 3 máquinas, deploy, Docker, monitoramento  
**Quando usar:** Setup inicial, troubleshooting de conectividade, deploy de nova versão

**Responsabilidades:**
- Documentar e automatizar setup das 3 máquinas:
  - Máquina 1 (dev): IDE, Gradle, Git
  - Máquina 2 (servidor): Minecraft server, faster-whisper (Docker), deploy de mods
  - Máquina 3 (LLM): Ollama, modelos, Edge TTS
- Criar scripts de deploy (copiar `.jar` para servidor, restart)
- Monitorar conectividade entre máquinas
- Docker Compose para faster-whisper
- Scripts de health check (Ollama responding? Server up? STT responding?)

**Health Checks:**
```bash
# check-infra.sh — rodar antes de cada sessão de dev
echo "=== Máquina 2: Servidor ==="
ping -c 1 192.168.0.225 && echo "OK" || echo "FAIL"
curl -s http://192.168.0.225:10300/health && echo "STT OK" || echo "STT FAIL"

echo "=== Máquina 3: LLM ==="
ping -c 1 192.168.0.200 && echo "OK" || echo "FAIL"
curl -s http://192.168.0.200:11434/api/tags && echo "Ollama OK" || echo "Ollama FAIL"
```

**Caso de uso:**
> "Ollama não responde" → Infra agent diagnostica: ping OK? Porta 11434 aberta? Serviço rodando? Modelo carregado? Propõe fix step-by-step.

---

### AD08 — Agente Otimizador

**Nome:** `optimizer`  
**Propósito:** Monitorar e melhorar performance do mod — TPS, memória, latência LLM  
**Quando usar:** Após implementação de features tick-based, periodicamente em cada fase

**Responsabilidades:**
- Definir e medir baselines de performance
- Identificar hot paths (código que roda a cada tick)
- Profiling com VisualVM (heap dumps, CPU sampling)
- Monitorar TPS do servidor com Alice ativa vs. sem Alice
- Otimizar alocações: reuso de objetos, pools, lazy init
- Monitorar latência das chamadas LLM (p50, p95, p99)

**Budgets de Performance (por tick = 50ms):**
```
RuleEngine (todas as regras):     < 2ms    (4% do tick)
Percepção (scan AABB):            < 1ms    (2% do tick)
Baritone pathfinding:             < 5ms    (10% do tick, gerenciado pelo Baritone)
SkillMatcher:                     < 0.5ms  (1% do tick, roda sob demanda)
Total Alice overhead por tick:    < 8ms    (16% do tick)
Sobra para Minecraft + outros:   > 42ms   (84% do tick)

LLM call (async, não conta no tick):
  Orquestrador:                   < 3s     (p95)
  Agente especializado:           < 5s     (p95)
  Total turnaround (voz→resposta): < 10s   (p95)
```

**Caso de uso:**
> Fase 1 concluída → Optimizer mede: TPS com Alice = 18.5 (vs 20 sem). Investiga: `AttackNearestHostileRule.shouldApply()` faz `level.getEntities()` a cada tick sem cache. Fix: cache de 10 ticks para scan de entidades.

---

### AD09 — Agente Organizador de Código

**Nome:** `code-organizer`  
**Propósito:** Manter código limpo, refatorar quando necessário, gerenciar dívida técnica  
**Quando usar:** Entre fases (janela de refactoring), quando code smells acumulam

**Responsabilidades:**
- Aplicar Checkstyle/formatação consistente
- Identificar e resolver code smells (classes grandes, métodos longos, duplicação)
- Gerenciar dependências (atualizar libs, remover unused)
- Reorganizar pacotes quando complexidade cresce
- Manter `build.gradle` limpo
- Gerenciar backlog de dívida técnica

**Regras de organização:**
```
- Classe > 300 linhas → candidata a split
- Método > 40 linhas → candidato a extract
- 3+ duplicações → candidato a abstração
- Import não usado → remover
- Dependência não usada → remover do gradle
- TODO sem issue → criar issue ou resolver
```

**Caso de uso:**
> Entre Fase 1 e Fase 2 → Organizer revisa: `AliceFakePlayer.java` cresceu para 450 linhas. Extrai `AliceInventoryManager` (gestão de itens) e `AliceEquipmentManager` (equipamento automático) como classes separadas. Atualiza imports e testes.

---

### AD10 — Agente de Mitigação de Riscos

**Nome:** `risk-mitigator`  
**Propósito:** Monitorar riscos conhecidos, identificar novos, propor e executar mitigações  
**Quando usar:** Início de cada fase, quando um risco se materializa, periodicamente

**Responsabilidades:**
- Manter Risk Registry atualizado (seção 8)
- Monitorar indicadores de risco (build time crescendo, testes falhando, TPS caindo)
- Propor mitigações concretas quando risco sobe de nível
- Criar issues para riscos que precisam de ação
- Post-mortem quando algo dá errado
- Validar que fallbacks funcionam (Piper TTS offline, Vosk STT offline)

**Risk Registry inicial:**

| ID | Risco | Probabilidade | Impacto | Mitigação | Owner |
|----|-------|--------------|---------|-----------|-------|
| RSK01 | Baritone API não funciona com FakePlayer | Alta | Crítico | PoC na primeira semana da Fase 1 — se falhar, usar FakePlayer com movement manual | AD01 Architect |
| RSK02 | Ollama latência > 10s na Machine 3 (CPU only) | Alta | Alto | Testar phi3-mini (menor); se > 10s, avaliar GPU na Machine 2 | AD07 Infra |
| RSK03 | FakePlayer não aparece como jogador real no cliente | Média | Alto | Testar GameProfile com skin injection; fallback: custom entity com renderer | AD04 Tester |
| RSK04 | Forge events não disparam para FakePlayer | Média | Crítico | Verificar LivingHurtEvent, ServerChatEvent com FakePlayer; mock se necessário | AD04 Tester |
| RSK05 | TPS cai abaixo de 15 com Alice ativa | Média | Crítico | Profiling desde Fase 1; budget de tick definido (< 8ms); cache de scans | AD08 Optimizer |
| RSK06 | Simple Voice Chat API muda entre versões | Baixa | Médio | Abstrair atrás de interface; soft dependency no mods.toml | AD01 Architect |
| RSK07 | Edge TTS fica indisponível (serviço cloud) | Média | Médio | Fallback Piper TTS offline (decisão #49) já planejado | AD07 Infra |
| RSK08 | faster-whisper Docker consome muita RAM na Machine 2 | Baixa | Médio | Limitar container a 2GB; fallback Vosk (decisão #49) | AD07 Infra |
| RSK09 | ollama4j não suporta tool calling adequadamente | Média | Alto | Testar tool calling na PoC; fallback: parsing de JSON manual | AD02 Coder |
| RSK10 | Modpack Cursed Walking conflita com Alice | Baixa | Alto | Testar em ambiente com modpack completo desde Fase 1 | AD04 Tester |

**Caso de uso:**
> Semana 1 da Fase 1: RSK01 materializa — `BaritoneAPI.getProvider().getBaritoneForPlayer(fakePlayer)` retorna null. Risk Mitigator ativa: escalar para Architect, investigar source do Baritone, verificar se precisa registrar FakePlayer como `EntityPlayerSP`. Documenta workaround, atualiza risco para "mitigado" ou "bloqueador".

---

## 4. Convenções e Regras de Desenvolvimento {#convencoes}

### 4.1 Naming Conventions

```java
// Pacotes: com.alice.<camada>.<subcamada>
com.alice.entity                  // FakePlayer, Manager
com.alice.behavior.rules          // IAliceRule, implementações
com.alice.pathfinding             // Baritone wrapper
com.alice.llm                     // Providers, handlers
com.alice.voice                   // TTS, STT (Fase 3)
com.alice.memory                  // SavedData, JGit (Fase 5)
com.alice.build                   // Schematics, builder (Fase 4)
com.alice.config                  // ForgeConfigSpec
com.alice.events                  // Forge event handlers
com.alice.skill                   // SkillMatcher, SkillLoader
com.alice.agent                   // AgentOrchestrator, agentes

// Classes: PascalCase, prefixo Alice para classes core
AliceFakePlayer, AliceBaritoneController, AliceLLMProvider

// Interfaces: prefixo I para contratos
IAliceRule, IAliceAgent, IAliceSkill

// Records: sem prefixo, descritivos
AgentContext, GameState, CombatDecision, NavigationPlan

// Constantes: UPPER_SNAKE_CASE
MAX_PERCEPTION_RADIUS, DEFAULT_FLEE_DISTANCE, TICK_CACHE_DURATION

// Configs: snake_case (ForgeConfigSpec convention)
ollama_url, perception_radius, flee_health_threshold
```

### 4.2 Commit Convention (Conventional Commits)

```
<type>(<scope>): <description>

Tipos:
  feat     — feature nova
  fix      — bug fix
  refactor — refatoração sem mudança de comportamento
  test     — adição/modificação de testes
  docs     — documentação
  perf     — otimização de performance
  chore    — build, CI, dependências
  style    — formatação, sem mudança lógica

Scopes:
  entity   — FakePlayer, spawn, death
  rules    — RuleEngine, regras always-on
  pathfind — Baritone integration
  llm      — OllamaProvider, chat handler
  voice    — TTS, STT (Fase 3+)
  build    — Schematics, builder (Fase 4+)
  memory   — SavedData, JGit (Fase 5+)
  config   — ForgeConfigSpec
  skill    — SkillMatcher, skills
  agent    — Agentes in-game
  ci       — GitHub Actions, pipeline
  infra    — Docker, deploy, scripts

Exemplos:
  feat(entity): add AliceFakePlayer with skin injection
  fix(rules): FleeOnLowHealthRule not triggering at exactly 20%
  test(rules): add boundary tests for all combat rules
  perf(rules): cache entity scan for 10 ticks in RuleEngine
  docs(wiki): add first-steps guide for Phase 1
  chore(ci): add GitHub Actions build workflow
```

### 4.3 Branch Strategy

```
main                          — sempre estável, deployável
├── phase/1-foundation        — branch da fase atual
│   ├── feat/fake-player      — feature branches curtas
│   ├── feat/baritone-follow
│   └── fix/spawn-crash
├── phase/2-utility           — próxima fase (criada ao fechar fase 1)
└── hotfix/critical-crash     — hotfix direto para main

Regras:
- Feature branches vivem no máximo 3 dias
- Merge para phase branch via PR com review
- Phase branch merge para main quando DOD completo
- Hotfix branches mergeam direto para main E para phase branch ativa
- Nunca force push em main ou phase branches
```

### 4.4 Estrutura de Arquivos

```
alice/
├── .github/
│   ├── workflows/
│   │   ├── build.yml              # CI: build + test em todo push
│   │   ├── release.yml            # CD: publish em tag
│   │   └── gametest.yml           # Testes in-game periódicos
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.md
│   │   ├── feature_request.md
│   │   └── crash_report.md
│   └── PULL_REQUEST_TEMPLATE.md
├── docs/
│   ├── brainstorm/                # Documentos de brainstorm (existente)
│   └── planejamento/              # Documentos de planejamento (existente)
├── scripts/
│   ├── check-infra.sh             # Health check das 3 máquinas
│   ├── deploy.sh                  # Deploy do .jar para servidor
│   └── profile-tps.sh            # Script de monitoramento de TPS
├── src/
│   ├── main/
│   │   ├── java/com/alice/        # Código fonte do mod
│   │   └── resources/
│   │       ├── META-INF/mods.toml
│   │       ├── assets/alice/       # Texturas, lang files
│   │       └── data/alice/         # Receitas, structures, gametests
│   └── test/
│       ├── java/com/alice/         # Testes JUnit 5
│       └── resources/
│           └── data/alice/structures/  # Estruturas para GameTest
├── CHANGELOG.md
├── CLAUDE.md                      # Instruções para Claude Code
├── build.gradle
├── gradle.properties
└── settings.gradle
```

### 4.5 EditorConfig

```ini
# .editorconfig
root = true

[*]
charset = utf-8
end_of_line = lf
indent_style = space
indent_size = 4
insert_final_newline = true
trim_trailing_whitespace = true

[*.{json,yml,yaml}]
indent_size = 2

[*.md]
trim_trailing_whitespace = false

[*.gradle]
indent_size = 4
```

### 4.6 Política de Logging

A Alice é um sistema complexo com múltiplas camadas assíncronas (percepção, regras, LLM, pathfinding, voz). **Sem logging robusto, debugar problemas em produção depende de testes de usuário** — o que é inaceitável. A política abaixo é **regra obrigatória** do projeto.

#### Princípios

1. **Todo erro DEVE ter log de erro** — `catch` sem `LOGGER.error()` é proibido. Sem exceção.
2. **Logging de debug acompanha o código** — todo método significativo deve ter logs de debug que permitam reconstruir o fluxo de execução sem depender de breakpoints ou reprodução manual.
3. **Logs de debug são efêmeros** — existem para desenvolvimento e diagnóstico, e **devem ser removidos ou desativados** quando a feature estiver estável e testada. Código em release não deve ter logs de debug excessivos.

#### Categorias de Log Obrigatórias (nível DEBUG)

| Categoria | O que logar | Exemplo |
|-----------|-------------|---------|
| **Eventos** | Entrada e saída de eventos Forge relevantes | `DEBUG: [EVENT] ServerTickEvent — Alice tick #4521, rules pending: 3` |
| **Chamadas externas** | Toda chamada a serviços externos (Ollama, faster-whisper, Baritone API) com parâmetros e resultado | `DEBUG: [CALL] OllamaProvider.chat() — model=llama3, tokens_in=142, latency=2340ms, status=OK` |
| **Processamento** | Início e fim de processamentos não-triviais (RuleEngine evaluate, SkillMatcher, AgentOrchestrator) | `DEBUG: [PROC] RuleEngine.evaluate() — 15 rules checked, 3 triggered, elapsed=1.2ms` |
| **Fluxo de informação** | Dados passando entre camadas (percepção → regras → agente → skill → ação) | `DEBUG: [FLOW] Perception → RuleEngine: entities=5, threats=1, nearest_threat=Zombie@14.2m` |
| **Timing** | Tempo de execução de cada fase/operação crítica | `DEBUG: [TIME] tick total=6.8ms (perception=0.9ms, rules=1.1ms, agent=4.2ms, action=0.6ms)` |
| **Estado** | Mudanças de estado significativas (FSM transitions, health changes, target changes) | `DEBUG: [STATE] Alice state: IDLE → COMBAT (trigger: FleeOnCriticalHealth, health=4.0)` |

#### Níveis de Log — Quando usar cada um

```java
// ERROR — Algo falhou e impacta funcionalidade. OBRIGATÓRIO em todo catch.
LOGGER.error("[CALL] OllamaProvider.chat() failed — model={}, error={}", model, e.getMessage(), e);

// WARN — Situação inesperada mas recuperável. Degradação graceful.
LOGGER.warn("[FLOW] Baritone path not found to {} — falling back to wander", target);

// INFO — Eventos de alto nível (lifecycle). Poucos por minuto em operação normal.
LOGGER.info("Alice spawned at {} in dimension {}", pos, dimension);
LOGGER.info("Alice despawned — reason: {}", reason);

// DEBUG — Diagnóstico detalhado. Ativado durante desenvolvimento, desativado em release.
LOGGER.debug("[PROC] RuleEngine.evaluate() — {} rules checked, {} triggered, elapsed={}ms",
    total, triggered, elapsed);

// TRACE — Ultra-detalhado (per-tick per-rule). Apenas para investigação pontual.
LOGGER.trace("[RULE] {} — shouldApply={}, priority={}", rule.getName(), applies, rule.getPriority());
```

#### Regras de Implementação

```java
// ❌ PROIBIDO: catch sem log
try {
    ollamaProvider.chat(prompt);
} catch (Exception e) {
    // silêncio — BUG
}

// ✅ OBRIGATÓRIO: todo catch loga o erro
try {
    ollamaProvider.chat(prompt);
} catch (Exception e) {
    LOGGER.error("[CALL] OllamaProvider.chat() failed — prompt_length={}, error={}",
        prompt.length(), e.getMessage(), e);
    // fallback ou re-throw
}

// ❌ PROIBIDO: log sem contexto
LOGGER.debug("processing");

// ✅ OBRIGATÓRIO: log com categoria, método, dados relevantes
LOGGER.debug("[PROC] RuleEngine.evaluate() — rules={}, tick={}", ruleCount, tickCount);

// ❌ PROIBIDO: concatenação de string em log (custo mesmo quando debug desativado)
LOGGER.debug("Result: " + result.toString());

// ✅ OBRIGATÓRIO: usar placeholders SLF4J
LOGGER.debug("[PROC] Result: {}", result);
```

#### Lifecycle dos Logs de Debug

```
Desenvolvimento → DEBUG ativado para camada em foco
                  Logs acompanham TODA mudança de código
                  
Feature estável → Revisar logs de debug
                  Remover logs redundantes ou excessivos
                  Manter logs que ajudam diagnóstico futuro
                  
Release         → DEBUG desativado por padrão (config)
                  TRACE desativado por padrão
                  INFO + WARN + ERROR sempre ativos
```

#### Padrão de Logger

```java
// Toda classe que faz logging:
private static final Logger LOGGER = LogUtils.getLogger();

// Prefixo de categoria entre colchetes: [EVENT], [CALL], [PROC], [FLOW], [TIME], [STATE], [RULE]
// Seguido de classe.método() e dados key=value
```

---

## 5. Estratégia de Testes {#testes}

### 5.1 Pirâmide de Testes

```
            /  GameTest  \           ← Poucos: interações in-game (spawn, movement, combat)
           /  Integration  \         ← Médios: LLM mock, Baritone mock, config loading
          /    Unit Tests    \       ← Muitos: regras, skills, parsing, utils
```

### 5.2 Testes Unitários (JUnit 5)

**O que testar:**
- Toda `IAliceRule`: `shouldApply()` retorna true/false corretamente para cada condição
- `RuleEngine`: executa regras em ordem de prioridade, para na primeira que aplica
- `SkillMatcher`: retorna skills corretas para keywords dadas
- `AliceConfig`: valores default, parsing, validação de ranges
- Parsing de LLM response: JSON parsing, fallback para texto livre
- `GameState`: serialização correta para texto

**Exemplo:**
```java
@Test
void fleeRule_shouldApply_whenHealthBelow20Percent() {
    AliceFakePlayer alice = mockAlice(health = 3.0f); // 15% of 20
    GameState state = mockState(hostiles = List.of(zombie(5)));
    
    FleeOnLowHealthRule rule = new FleeOnLowHealthRule();
    assertTrue(rule.shouldApply(alice, state));
}

@Test
void fleeRule_shouldNotApply_whenHealthAbove20Percent() {
    AliceFakePlayer alice = mockAlice(health = 5.0f); // 25% of 20
    GameState state = mockState(hostiles = List.of(zombie(5)));
    
    FleeOnLowHealthRule rule = new FleeOnLowHealthRule();
    assertFalse(rule.shouldApply(alice, state));
}

@Test
void fleeRule_boundary_exactly20Percent() {
    AliceFakePlayer alice = mockAlice(health = 4.0f); // exactly 20%
    GameState state = mockState(hostiles = List.of(zombie(5)));
    
    FleeOnLowHealthRule rule = new FleeOnLowHealthRule();
    assertTrue(rule.shouldApply(alice, state)); // 20% IS low health
}
```

**Framework:**
- JUnit 5 (`org.junit.jupiter`)
- Mockito para mocks de Minecraft classes
- `mcjunitlib` (alcatrazEscapee) para class loading compatível com Forge

**Dependências Gradle:**
```gradle
dependencies {
    testImplementation "org.junit.jupiter:junit-jupiter:5.10.2"
    testImplementation "org.mockito:mockito-core:5.11.0"
    testImplementation "org.mockito:mockito-junit-jupiter:5.11.0"
    // Para testes que precisam de class loading do Forge
    testImplementation "com.alcatrazescapee:mcjunitlib:1.20.1-1.1.0"
}
```

### 5.3 Testes de Integração

**O que testar:**
- `OllamaProvider`: mock server HTTP, verifica request/response format
- `AliceChatHandler`: end-to-end do chat event → LLM → resposta
- Config loading: `ForgeConfigSpec` carrega corretamente do arquivo

**Sem mock de LLM real** — usar WireMock ou mock server que retorna respostas predefinidas.

### 5.4 GameTest Framework (Forge)

**O que testar in-game:**
- Alice spawna em posição correta
- Alice segue jogador (verifica posição após N ticks)
- Alice ataca zumbi spawnado no raio
- Alice foge quando HP é setado abaixo de 20%
- Alice não sai da base durante blood moon (se simulável)

**Setup no `build.gradle`:**
```gradle
minecraft {
    runs {
        gameTestServer {
            workingDirectory project.file('run')
            property 'forge.logging.console.level', 'debug'
            property 'forge.enabledGameTestNamespaces', 'alice'
            mods {
                alice {
                    source sourceSets.main
                    source sourceSets.test
                }
            }
        }
    }
}
```

**Exemplo:**
```java
@GameTestHolder("alice")
public class AliceSpawnTest {

    @GameTest(timeoutTicks = 100)
    public static void aliceSpawnsCorrectly(GameTestHelper helper) {
        // Spawnar Alice na posição (1, 1, 1) relativa
        BlockPos spawnPos = new BlockPos(1, 1, 1);
        AliceFakePlayer alice = AliceFakePlayerManager.spawn(
            helper.getLevel(), helper.absolutePos(spawnPos)
        );
        
        helper.succeedWhen(() -> {
            helper.assertEntityPresent(EntityType.PLAYER, spawnPos);
        });
    }
}
```

### 5.5 Testes por Fase

| Fase | Testes Unit | Testes Integration | GameTests | Critério Mínimo |
|------|------------|-------------------|-----------|-----------------|
| 1 | Todas as regras, config, parsing | OllamaProvider mock | Spawn, follow, combat | 100% das regras testadas |
| 2 | Crafting, inventory, skills | RecipeManager | Craft autônomo, loot | 100% dos skills testados |
| 3 | Voice parsing, audio pipeline | STT mock, TTS mock | Voice command → ação | Pipeline end-to-end |
| 4 | Schematic loading, materials | Build pipeline | Construção completa | Build e verify structure |
| 5 | Memory persistence, RAG | SavedData, JGit mock | Recall de memória | Persist + recall ciclo |
| 6 | Agent routing, tool calling | Multi-agent pipeline | Cenário complexo | Routing correto 90%+ |

---

## 6. Pipeline CI/CD {#cicd}

### 6.1 GitHub Actions — Build (todo push/PR)

```yaml
# .github/workflows/build.yml
name: Build & Test

on:
  push:
    branches: [main, 'phase/**']
  pull_request:
    branches: [main, 'phase/**']

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
      
      - name: Build
        run: ./gradlew build
      
      - name: Unit Tests
        run: ./gradlew test
      
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: build/reports/tests/
      
      - name: Upload build artifact
        uses: actions/upload-artifact@v4
        with:
          name: alice-mod-jar
          path: build/libs/*.jar
```

### 6.2 GitHub Actions — GameTest (diário ou manual)

```yaml
# .github/workflows/gametest.yml
name: GameTest

on:
  schedule:
    - cron: '0 6 * * *'  # Diário às 6h UTC
  workflow_dispatch:       # Manual trigger

jobs:
  gametest:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
      
      - name: Install dependencies for headless
        run: sudo apt-get install -y libxtst6 libxi6
      
      - name: Run GameTests
        run: ./gradlew runGameTestServer
        env:
          CI: true
      
      - name: Upload GameTest results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: gametest-results
          path: run/logs/
```

### 6.3 GitHub Actions — Release (em tag)

```yaml
# .github/workflows/release.yml
name: Release

on:
  push:
    tags: ['v*']

jobs:
  release:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build release
        run: ./gradlew build
      
      - name: Run all tests
        run: ./gradlew test
      
      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          files: build/libs/*.jar
          body_path: CHANGELOG.md
          draft: false
          prerelease: ${{ contains(github.ref, 'alpha') || contains(github.ref, 'beta') }}
      
      # Descomentar quando pronto para publicação pública:
      # - name: Publish to CurseForge & Modrinth
      #   run: ./gradlew publishMod
      #   env:
      #     CURSE_TOKEN: ${{ secrets.CURSE_TOKEN }}
      #     MODRINTH_TOKEN: ${{ secrets.MODRINTH_TOKEN }}
```

---

## 7. Release Management {#release}

### 7.1 Processo de Release

```
1. DOD da fase = 100% completo ✅
2. Todos os testes passam (unit + integration + gametest) ✅
3. Performance dentro dos budgets ✅
4. Documentação atualizada (wiki, changelog) ✅
5. Risk Registry revisado ✅
6. Criar tag: git tag -a v0.X.0 -m "Phase X: <nome>"
7. Push tag: git push origin v0.X.0
8. GitHub Actions roda release workflow automaticamente
9. Verificar artifacts no GitHub Release
10. Deploy manual no servidor de teste (Machine 2)
11. Teste de sanidade: 30 min sem crash
12. Marcar milestone como fechada no GitHub
```

### 7.2 Changelog Format

```markdown
# Changelog

## [0.1.0] - 2026-XX-XX — Phase 1: Fundação

### Adicionado
- Alice FakePlayer com skin Scarlet (ruiva)
- Spawn via ovo customizado
- Comandos: follow, stay, goto
- Combate automático contra hostis em raio de 8 blocos
- Fuga automática quando HP < 20%
- Chat via LLM (Ollama)
- Respawn sem perda de itens

### Configuração
- `ollama_url`: URL do servidor Ollama
- `model`: modelo LLM a usar
- `perception_radius`: raio de percepção (default: 20)

### Conhecido
- Latência de resposta do LLM: 3-10s dependendo do modelo
- Pathfinding pode falhar em terrenos muito complexos
```

---

## 8. Gestão de Riscos e Issues {#riscos}

### 8.1 Processo de Gestão de Riscos

```
Identificar → Avaliar → Mitigar → Monitorar → Escalar/Fechar

Frequência:
- Início de cada fase: revisão completa do Risk Registry
- Semanal: check rápido dos riscos em "watch"
- Quando materializa: ativar mitigação, post-mortem após resolução
```

### 8.2 Classificação

```
Probabilidade: Alta (>70%) | Média (30-70%) | Baixa (<30%)
Impacto: Crítico (bloqueia fase) | Alto (atrasa fase) | Médio (contornável) | Baixo (cosmético)

Matriz:
              Baixo    Médio    Alto     Crítico
  Alta      | Watch  | Act    | Act    | BLOCK  |
  Média     | Accept | Watch  | Act    | Act    |
  Baixa     | Accept | Accept | Watch  | Watch  |

BLOCK = Parar tudo, resolver primeiro
Act   = Mitigação ativa, deadline para resolver
Watch = Monitorar, preparar contingência
Accept = Aceitar, não investir tempo
```

### 8.3 Issue Templates

**Bug Report:**
```markdown
## Descrição
[O que aconteceu vs. o que deveria acontecer]

## Passos para Reproduzir
1. ...
2. ...

## Ambiente
- Fase do projeto: [1-6]
- Versão do mod: [x.y.z]
- Forge version: [1.20.1-47.3.0]
- Modpack: [Cursed Walking com todos os mods / vanilla + Alice]

## Logs
[Colar crash log ou relevante do latest.log]

## Impacto
- [ ] Crash do servidor
- [ ] Crash do cliente
- [ ] Comportamento incorreto (não crash)
- [ ] Performance (TPS baixo)
```

**Crash Report:**
```markdown
## Crash Log
[Colar crash-report completo]

## Contexto
- O que Alice estava fazendo no momento: [combate/follow/idle/chat]
- Último comando dado: [...]
- Frequência: [sempre/às vezes/primeira vez]
```

### 8.4 Post-Mortem Template

```markdown
## Incidente: [Título]
**Data:** [YYYY-MM-DD]
**Severidade:** [Crítico/Alto/Médio]
**Duração:** [tempo até resolução]

## O que aconteceu
[Timeline factual do incidente]

## Causa raiz
[Análise técnica — por que aconteceu]

## O que fizemos
[Ações tomadas para resolver]

## O que vamos fazer para não acontecer de novo
- [ ] [Ação preventiva 1]
- [ ] [Ação preventiva 2]

## Lições aprendidas
[O que mudamos no processo/código/testes]
```

---

## 9. Infraestrutura de Desenvolvimento {#infra}

### 9.1 Mapa das 3 Máquinas

```
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│ MÁQUINA 1 (Dev)  │     │ MÁQUINA 2 (Server)│     │ MÁQUINA 3 (LLM) │
│                  │     │ 192.168.0.225     │     │ 192.168.0.200    │
│ • IDE (VSCode)   │────▶│ • MC Server       │     │ • Ollama :11434  │
│ • Gradle build   │ JAR │ • faster-whisper  │     │ • llama3.2/phi3  │
│ • Git repo       │     │   Docker :10300   │     │ • Edge TTS proxy │
│ • Claude Code    │     │ • mods/ folder    │     │   (se aplicável) │
│ • TLauncher      │     │                  │     │                  │
│   (cliente MC)   │     │                  │     │                  │
└──────────────────┘     └──────────────────┘     └──────────────────┘
         │                        │                        │
         └────────────────────────┼────────────────────────┘
                                  │
                          LAN 192.168.0.0/24
```

### 9.2 Scripts de Automação

**Deploy script:**
```bash
#!/bin/bash
# scripts/deploy.sh — Builda e deploya o .jar para o servidor
set -e

echo "=== Building Alice Mod ==="
./gradlew build

JAR=$(ls -t build/libs/alice-*.jar | head -1)
echo "=== Deploying $JAR to Server ==="

# Copiar para servidor (ajustar método de acesso)
scp "$JAR" user@192.168.0.225:/path/to/server/mods/

echo "=== Restarting Server ==="
ssh user@192.168.0.225 "cd /path/to/server && ./restart.sh"

echo "=== Deploy complete ==="
```

### 9.3 CLAUDE.md (Instruções para Claude Code)

```markdown
# Projeto Alice — Instruções para Claude Code

## Contexto
Mod Minecraft Forge 1.20.1 — companion IA chamada Alice (ruiva, 20 anos, sobrevivência).

## Documentos de Referência (ler antes de codar)
- `docs/brainstorm/projeto-alice-brainstorm.md` — 49 decisões arquiteturais
- `docs/planejamento/plano-projeto-alice.md` — plano de 6 fases com DOR/DOD
- `docs/planejamento/processo-desenvolvimento.md` — este documento

## Convenções
- Commits: Conventional Commits (feat/fix/refactor/test/docs/perf/chore)
- Branches: phase/N-name → feat/description
- Naming: PascalCase classes, camelCase métodos, UPPER_SNAKE constantes
- Prefixo Alice para classes core, I para interfaces

## Regras Invioláveis
1. NUNCA bloquear a server thread — chamadas LLM sempre async
2. NUNCA alocar objetos no hot path (tick handler) sem necessidade
3. NUNCA usar APIs client-side em código server-side
4. TODA regra (`IAliceRule`) precisa de teste unitário correspondente
5. TODA feature precisa passar no build antes de commit
6. Seguir o escopo da fase atual — não implementar features de fases futuras
7. NUNCA fazer catch sem LOGGER.error() — todo erro DEVE ser logado
8. TODO código novo DEVE ter logs de debug (eventos, chamadas, processamento, fluxo, timing)
9. Logs de debug DEVEM ser removidos/desativados quando a feature estiver estável
10. Ver seção 4.6 do processo-desenvolvimento.md para política completa de logging

## Stack
- Java 17, Forge 1.20.1-47.3.0
- ollama4j 1.1.6, Baritone API 1.20.1
- JUnit 5, Mockito, mcjunitlib (testes)
- GameTest Framework (testes in-game)

## Performance Budgets
- RuleEngine total: < 2ms por tick
- Percepção scan: < 1ms por tick  
- Alice overhead total: < 8ms por tick (de 50ms disponíveis)
- LLM response: < 10s p95
```

---

## 10. Tabela de Ganhos por Investimento {#ganhos}

### Análise ROI — O que implementar primeiro para máximo retorno

| # | Investimento | Esforço | Ganho | ROI | Caso de Sucesso |
|---|-------------|---------|-------|-----|-----------------|
| 1 | **CLAUDE.md com convenções** | 1h | Toda interação Claude Code segue padrões consistentes | ★★★★★ | — |
| 2 | **GitHub Actions: build.yml** | 2h | Build automático em todo push, catch erros cedo | ★★★★★ | Baritone, Mekanism |
| 3 | **JUnit para regras** | 3h | Regras always-on (segurança da Alice) nunca quebram silenciosamente | ★★★★★ | Mekanism |
| 4 | **EditorConfig + .gitignore** | 30min | Formatação consistente automática | ★★★★☆ | Todos os projetos sérios |
| 5 | **Branch strategy** | 30min | Sem conflitos, fases isoladas | ★★★★☆ | Baritone |
| 6 | **Commit convention** | 0h (só disciplina) | Changelog automático, history legível | ★★★★☆ | Todos |
| 7 | **Issue templates** | 1h | Bugs reportados com contexto suficiente | ★★★☆☆ | Mekanism |
| 8 | **Health check scripts** | 1h | 30 segundos para saber se infra está OK | ★★★☆☆ | — |
| 9 | **Risk Registry ativo** | 2h | Riscos não viram surpresas | ★★★☆☆ | Enterprise |
| 10 | **Performance baseline** | 2h | TPS degradado é detectado imediatamente | ★★★☆☆ | Baritone (YourKit) |
| 11 | **GameTest Framework** | 4h | Testes in-game automatizados (spawn, follow, combat) | ★★★☆☆ | Forge docs |
| 12 | **Wiki estrutura** | 2h | Base para documentação cresce com o projeto | ★★☆☆☆ | Create |
| 13 | **Release workflow** | 2h | Releases automáticas quando tag é criada | ★★☆☆☆ | ModPublisher |
| 14 | **Deploy script** | 1h | Deploy em 1 comando ao invés de copiar manual | ★★☆☆☆ | — |
| 15 | **Docker compose (STT)** | 2h | faster-whisper sempre reprodutível | ★☆☆☆☆ | — (Fase 3) |

### Ordem de Execução Recomendada (antes de começar Fase 1)

```
Bloco A — Setup Imediato (antes de escrever código):
  1. CLAUDE.md com convenções e regras
  2. .editorconfig
  3. .gitignore robusto
  4. Branch strategy: criar phase/1-foundation
  5. Issue templates no GitHub

Bloco B — CI mínimo (primeira semana):
  6. GitHub Actions: build.yml
  7. Estrutura de testes JUnit 5 (framework, não testes)
  8. Health check script para infra

Bloco C — Junto com Fase 1 (paralelo):
  9. Testes JUnit para cada regra implementada
  10. Performance baseline (TPS com/sem Alice)
  11. Risk Registry: validar RSK01-RSK10

Bloco D — Ao concluir Fase 1:
  12. GameTest Framework setup
  13. Wiki: Primeiros-Passos.md
  14. Release workflow
  15. Tag v0.1.0-alpha
```

---

## Apêndice: Referências Utilizadas

- [Baritone GitHub](https://github.com/cabaletta/baritone) — CI/CD, profiling, multi-platform
- [GameTest Framework on Forge](https://gist.github.com/SizableShrimp/60ad4109e3d0a23107a546b3bc0d9752) — guia completo
- [mcjunitlib](https://github.com/alcatrazEscapee/mcjunitlib) — JUnit com Forge class loading
- [MC-Runtime-Test](https://github.com/marketplace/actions/mc-runtime-test) — CI para mods
- [ModPublisher](https://github.com/firstdarkdev/modpublisher) — publicação CurseForge + Modrinth
- [mod-publish-plugin](https://github.com/modmuss50/mod-publish-plugin) — publicação multi-plataforma
- [Forge GameTest docs](https://docs.minecraftforge.net/en/1.18.x/misc/gametest/) — documentação oficial
- [MC-Publish via GitHub Actions](https://wiki.fabricmc.net/tutorial:publishing_mods_using_github_actions) — workflow reference
- [AI-Player (shasankp000)](https://github.com/shasankp000/AI-Player) — reference project
- [Forge-AI-Player (dongge0210)](https://github.com/dongge0210/Forge-AI-Player) — Forge port reference
