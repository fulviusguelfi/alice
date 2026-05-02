# Projeto Alice — Instruções para Claude Code

## Contexto
Mod Minecraft Forge 1.20.1 — companion IA chamada Alice (ruiva, 20 anos, sobrevivência).
Modpack alvo: Cursed Walking (apocalipse zumbi moderno) rodando no servidor Larry.
Desenvolvimento solo: dev machine local (Windows 11) + test-server local na pasta `test-server/`.

## Documentos de Referência (ler antes de codar)
- `docs/brainstorm/projeto-alice-brainstorm.md` — 49 decisões arquiteturais
- `docs/planejamento/plano-projeto-alice.md` — plano de 6 fases com DOR/DOD
- `docs/planejamento/processo-desenvolvimento.md` — convenções, agentes de dev, processo
- `docs/planejamento/agentes.md` — 7 agentes in-game
- `docs/planejamento/regras.md` — regras always-on com prioridades
- `docs/planejamento/skills.md` — skills composíveis

## Estado Atual — Fase 1 (Fundação)
### Implementado
- `AliceMod.java` — entry point, ciclo de vida do servidor
- `AliceFakePlayer.java` — FakePlayer com tick() reativado para física do Baritone
- `AliceEntity.java` — lifecycle Baritone completo, broadcast de posição, métricas de perf
- `AliceCommands.java` — `/alicecmd goto|stay|status|pos|tp|chat|sethealth|perfstats`
- `AliceChatHandler.java` — parsing PT-BR/EN, comandos via chat, fallback LLM
- `HttpLLMProvider.java` + `AliceLLMProvider.java` — HTTP para Groq cloud
- `RuleEngine.java` + 3 regras: FleeOnCritical, FleeOnLow, AttackNearestHostile
- `BehaviorJournal.java` — logging estruturado
- `Config.java` — ForgeConfigSpec
- `SpikeDTest.java` — arena de obstáculos para validação do Baritone
- Baritone portado server-side completo (`mod/src/main/java/baritone/`)
- Suite pytest cobrindo D01–D12 via RCON

### Pendente (DOD incompleto)
- **D03** — `AliceSpawnEgg` — Item customizado de spawn (não existe ainda)
- **D10** — death/respawn handler — `LivingDeathEvent` + `PlayerRespawnEvent` para Alice FakePlayer

## Arquitetura (8 camadas, prioridade top-down)
```
PERCEPÇÃO → VOZ → REGRAS → ORQUESTRADOR → AGENTES → SKILLS → AÇÃO → MEMÓRIA
```
Prioridade de execução: Regras de Segurança > Regras de Utilidade > Agentes LLM > Padrão

## Pacotes Java
```
com.projetoalice.alice/
├── AliceMod.java           — entry point, eventos Forge
├── AliceFakePlayer.java    — FakePlayer com tick reativado
├── AliceEntity.java        — lifecycle, Baritone, broadcasts
├── AliceCommands.java      — /alicecmd via RCON/chat
├── AliceChatHandler.java   — chat PT-BR/EN → comandos → LLM
├── AliceLLMProvider.java   — interface LLM
├── HttpLLMProvider.java    — implementação HTTP (Groq)
├── IAliceRule.java         — interface de regras
├── RuleEngine.java         — engine prioridade < 2ms/tick
├── BehaviorJournal.java    — logging estruturado
├── Config.java             — ForgeConfigSpec
├── SpikeDTest.java         — teste Baritone in-game
└── rules/
    ├── AttackNearestHostileRule.java
    ├── FleeOnCriticalHealthRule.java
    └── FleeOnLowHealthRule.java
```

## Convenções
- Commits: Conventional Commits — `<type>(<scope>): <description>`
  - Tipos: feat, fix, refactor, test, docs, perf, chore, style
  - Scopes: entity, rules, pathfind, llm, voice, build, memory, config, skill, agent
- Naming: PascalCase classes, camelCase métodos, UPPER_SNAKE constantes
- Prefixo `Alice` para classes core, `I` para interfaces

## Regras Invioláveis
1. NUNCA bloquear a server thread — chamadas LLM sempre async (`CompletableFuture`)
2. NUNCA alocar objetos no hot path (tick handler, rule engine)
3. NUNCA usar APIs client-side em código server-side
4. TODA regra (`IAliceRule`) precisa de teste correspondente
5. TODA feature precisa passar no build antes de commit
6. Seguir o escopo da fase atual — não antecipar features de fases futuras
7. NUNCA fazer catch sem `LOGGER.error()` — todo erro deve ser logado
8. Logs de diagnóstico devem usar prefix `[Alice]` para filtrar facilmente

## Build e Deploy
```powershell
# Compilar
cd mod
./gradlew build

# Deploy no test-server local
./scripts/deploy.ps1

# Verificar pré-requisitos antes de testar
./scripts/check-infra.ps1
```

## Rodar Testes
```powershell
# 1. Garantir que test-server está rodando (iniciar manualmente)
# 2. RCON deve estar habilitado em test-server/server.properties (enable-rcon=true)
# 3. Rodar:
cd test-server
pytest tests/ -m "not slow" -v

# Testes slow (D11 — 30 min estabilidade) rodam manualmente:
pytest tests/ -m slow -v
```

## Infraestrutura
- LLM: Groq cloud (`api.groq.com`) — API key em `test-server/config/alice-common.toml`
- STT: faster-whisper em Docker no servidor Larry (192.168.0.225:10300) — Fase 3
- test-server: pasta local `test-server/` — Forge + Cursed Walking modpack
- Servidor produção: Larry (192.168.0.225) via crafty-control Docker — acesso manual apenas

## Performance Budgets
- RuleEngine total: < 2ms por tick
- Baritone tick: < 5ms (gerenciado internamente)
- Alice overhead total: < 8ms (de 50ms disponíveis a 20 TPS)
- LLM response: < 10s p95
