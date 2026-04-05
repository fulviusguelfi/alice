# Projeto Alice — Plano de Fechamento dos Gaps de Processo

**Versão:** 1.0  
**Data:** 2026-04-04  
**Status:** Pronto para execução  
**Pré-requisito:** Documento `processo-desenvolvimento.md` (gaps G01-G10)  
**Quando executar:** ANTES de iniciar qualquer código da Fase 1

---

## Sumário

1. [Contexto e Timing](#contexto)
2. [Bloco A — Setup Imediato (13 arquivos)](#bloco-a)
3. [Bloco B — Branch Strategy](#bloco-b)
4. [Bloco C — Commit e Push](#bloco-c)
5. [Conteúdo Completo de Cada Arquivo](#conteudo)
6. [Mapeamento Gaps → Ações](#mapeamento)
7. [Verificação Pós-Execução](#verificacao)
8. [O que fica para depois](#depois)

---

## 1. Contexto e Timing {#contexto}

### Estado Atual do Projeto

O Projeto Alice tem documentação de planejamento completa mas **ZERO infraestrutura de projeto**:

```
O que EXISTE:
  ✅ docs/brainstorm/ — 49 decisões arquiteturais
  ✅ docs/planejamento/ — 5 documentos (plano, agentes, regras, skills, processo)
  ✅ .git/ — repositório com 2 commits, remote no GitHub

O que NÃO EXISTE:
  ❌ .gitignore
  ❌ .editorconfig
  ❌ CLAUDE.md
  ❌ CHANGELOG.md
  ❌ .github/workflows/ (CI/CD)
  ❌ .github/ISSUE_TEMPLATE/ (templates de issue)
  ❌ .github/PULL_REQUEST_TEMPLATE.md
  ❌ scripts/ (automação)
  ❌ build.gradle, src/ (Forge project — vem na Fase 1)
```

### Quando Executar Este Plano

```
    ╔═══════════════════════════════════════════╗
    ║         SEQUÊNCIA DE EXECUÇÃO             ║
    ╠═══════════════════════════════════════════╣
    ║                                           ║
    ║  1. ▶ ESTE PLANO (fechar gaps)            ║ ← AGORA
    ║     └─ 13 arquivos + branch               ║
    ║                                           ║
    ║  2. Fase 1 — Fundação                     ║ ← DEPOIS
    ║     └─ build.gradle, src/, testes          ║
    ║                                           ║
    ║  3. Gaps restantes (junto com Fase 1)     ║
    ║     └─ JUnit por regra, performance       ║
    ║     └─ baseline, validar riscos            ║
    ║                                           ║
    ║  4. Conclusão Fase 1                      ║
    ║     └─ GameTest, Wiki, Release v0.1.0     ║
    ║                                           ║
    ╚═══════════════════════════════════════════╝
```

---

## 2. Bloco A — Setup Imediato (13 arquivos) {#bloco-a}

Todos criados em `main`, em um único commit.

| # | Arquivo | Fonte | Gap que fecha |
|---|---------|-------|---------------|
| A1 | `.gitignore` | Novo (Forge/Gradle/IDE) | G04 |
| A2 | `.editorconfig` | §4.5 processo-dev | G04 |
| A3 | `CLAUDE.md` | §9.3 processo-dev | G04, G08, G10 |
| A4 | `CHANGELOG.md` | §7.2 processo-dev | G05 |
| A5 | `.github/workflows/build.yml` | §6.1 processo-dev | G03 |
| A6 | `.github/workflows/gametest.yml` | §6.2 processo-dev | G02, G03 |
| A7 | `.github/workflows/release.yml` | §6.3 processo-dev | G03, G05 |
| A8 | `.github/ISSUE_TEMPLATE/bug_report.md` | §8.3 processo-dev | G06, G07 |
| A9 | `.github/ISSUE_TEMPLATE/feature_request.md` | Novo | G06 |
| A10 | `.github/ISSUE_TEMPLATE/crash_report.md` | §8.3 processo-dev | G06, G07 |
| A11 | `.github/PULL_REQUEST_TEMPLATE.md` | Checklist AD03 | G10 |
| A12 | `scripts/check-infra.sh` | AD07 processo-dev | G09 |
| A13 | `scripts/deploy.sh` | §9.2 processo-dev | G09 |

---

## 3. Bloco B — Branch Strategy {#bloco-b}

Após commit do Bloco A em `main`:

```bash
git checkout -b phase/1-foundation
git push -u origin phase/1-foundation
```

A partir daqui, todo trabalho da Fase 1 acontece em `phase/1-foundation` com feature branches curtas (`feat/fake-player`, `feat/baritone-follow`, etc.).

---

## 4. Bloco C — Commit e Push {#bloco-c}

```bash
# Stage todos os novos arquivos + processo-desenvolvimento.md (untracked)
git add .gitignore .editorconfig CLAUDE.md CHANGELOG.md
git add .github/
git add scripts/
git add docs/planejamento/processo-desenvolvimento.md

# Commit
git commit -m "chore: add project infrastructure (CI/CD, templates, scripts, conventions)"

# Push
git push origin main
```

---

## 5. Conteúdo Completo de Cada Arquivo {#conteudo}

### A1 — `.gitignore`

```gitignore
# Gradle
build/
.gradle/

# IDE — IntelliJ
.idea/
*.iml
*.ipr
*.iws
out/

# IDE — Eclipse
.project
.classpath
.settings/
.factorypath

# IDE — VSCode
.vscode/
*.code-workspace

# Minecraft
run/
logs/
*.log
crash-reports/

# OS
.DS_Store
Thumbs.db
desktop.ini

# Environment
.env
*.env.local

# Compiled
*.class
*.jar
!libs/*.jar

# Misc
*.bak
*.tmp
```

---

### A2 — `.editorconfig`

```ini
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

---

### A3 — `CLAUDE.md`

```markdown
# Projeto Alice — Instruções para Claude Code

## Contexto
Mod Minecraft Forge 1.20.1 — companion IA chamada Alice (ruiva, 20 anos, sobrevivência).
Modpack alvo: Cursed Walking (apocalipse zumbi moderno).
Infraestrutura: 3 máquinas LAN (dev, servidor MC + STT, LLM + TTS).

## Documentos de Referência (ler antes de codar)
- `docs/brainstorm/projeto-alice-brainstorm.md` — 49 decisões arquiteturais
- `docs/planejamento/plano-projeto-alice.md` — plano de 6 fases com DOR/DOD
- `docs/planejamento/processo-desenvolvimento.md` — processo, agentes de dev, testes, CI/CD
- `docs/planejamento/agentes.md` — 7 agentes in-game (Orquestrador, Combate, etc.)
- `docs/planejamento/regras.md` — 15 regras always-on com prioridades
- `docs/planejamento/skills.md` — 20 skills composíveis

## Convenções
- Commits: Conventional Commits — `<type>(<scope>): <description>`
  - Tipos: feat, fix, refactor, test, docs, perf, chore, style
  - Scopes: entity, rules, pathfind, llm, voice, build, memory, config, skill, agent, ci, infra
- Branches: `main` → `phase/N-name` → `feat/description`
- Naming: PascalCase classes, camelCase métodos, UPPER_SNAKE constantes
- Prefixo `Alice` para classes core, `I` para interfaces, Records sem prefixo
- Pacotes: `com.alice.<camada>.<subcamada>`

## Regras Invioláveis
1. NUNCA bloquear a server thread — chamadas LLM sempre async (CompletableFuture)
2. NUNCA alocar objetos no hot path (tick handler) sem necessidade
3. NUNCA usar APIs client-side em código server-side
4. TODA regra (`IAliceRule`) precisa de teste unitário correspondente
5. TODA feature precisa passar no build antes de commit
6. Seguir o escopo da fase atual — não implementar features de fases futuras
7. Nunca force push em main ou phase branches
8. Feature branches vivem no máximo 3 dias
9. NUNCA fazer catch sem LOGGER.error() — todo erro DEVE ser logado
10. TODO código novo DEVE ter logs de debug (eventos, chamadas, processamento, fluxo, timing)
11. Logs de debug DEVEM ser removidos/desativados quando a feature estiver estável
12. Ver `docs/planejamento/processo-desenvolvimento.md` §4.6 para política completa de logging

## Stack
- Java 17, Forge 1.20.1-47.3.0
- ollama4j 1.1.6, Baritone API 1.20.1
- JUnit 5, Mockito, mcjunitlib (testes)
- GameTest Framework (testes in-game)

## Infraestrutura
- Máquina 1 (dev): Windows 11, VSCode, Gradle, Git, TLauncher
- Máquina 2 (servidor): 192.168.0.225 — MC Server, faster-whisper Docker :10300
- Máquina 3 (LLM): 192.168.0.200 — Ollama :11434, CPU only

## Performance Budgets
- RuleEngine total: < 2ms por tick
- Percepção scan: < 1ms por tick
- Baritone pathfinding: < 5ms por tick (gerenciado pelo Baritone)
- Alice overhead total: < 8ms por tick (de 50ms disponíveis)
- LLM response: < 10s p95

## Arquitetura (8 camadas, prioridade top-down)
PERCEPÇÃO → VOZ → REGRAS → ORQUESTRADOR → AGENTES → SKILLS → AÇÃO → MEMÓRIA

Prioridade: Regras de Segurança > Regras de Utilidade > Agentes LLM > Comportamento Padrão
```

---

### A4 — `CHANGELOG.md`

```markdown
# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).
Versionamento segue [Semantic Versioning](https://semver.org/lang/pt-BR/).

## [Unreleased]

### Adicionado
- Infraestrutura de projeto: .gitignore, .editorconfig, CLAUDE.md
- CI/CD: GitHub Actions workflows (build, gametest, release)
- Templates: bug report, feature request, crash report, PR template
- Scripts: check-infra.sh, deploy.sh
- Documentação de planejamento completa (6 fases, agentes, regras, skills, processo)
```

---

### A5 — `.github/workflows/build.yml`

```yaml
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

---

### A6 — `.github/workflows/gametest.yml`

```yaml
name: GameTest

on:
  schedule:
    - cron: '0 6 * * *'
  workflow_dispatch:

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

---

### A7 — `.github/workflows/release.yml`

```yaml
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

### A8 — `.github/ISSUE_TEMPLATE/bug_report.md`

```markdown
---
name: Bug Report
about: Reportar comportamento incorreto da Alice
title: '[BUG] '
labels: bug
assignees: ''
---

## Descrição
[O que aconteceu vs. o que deveria acontecer]

## Passos para Reproduzir
1. ...
2. ...
3. ...

## Ambiente
- Fase do projeto: [1-6]
- Versão do mod: [x.y.z]
- Forge version: [1.20.1-47.3.0]
- Modpack: [Cursed Walking com todos os mods / vanilla + Alice]
- Java version: [17]

## Logs
[Colar trecho relevante do latest.log]

## Impacto
- [ ] Crash do servidor
- [ ] Crash do cliente
- [ ] Comportamento incorreto (não crash)
- [ ] Performance (TPS baixo)

## Screenshots / Vídeo
[Se aplicável]
```

---

### A9 — `.github/ISSUE_TEMPLATE/feature_request.md`

```markdown
---
name: Feature Request
about: Sugerir funcionalidade nova para a Alice
title: '[FEAT] '
labels: enhancement
assignees: ''
---

## Descrição
[O que você gostaria que a Alice fizesse]

## Motivação
[Por que essa funcionalidade é importante]

## Comportamento Esperado
[Como deveria funcionar na prática — cenário de uso]

## Fase do Projeto
- [ ] Fase 1 — Fundação (FakePlayer, Baritone, Chat)
- [ ] Fase 2 — Utilidade (Crafting, Inventário)
- [ ] Fase 3 — Voz (TTS, STT)
- [ ] Fase 4 — Construção (Schematics)
- [ ] Fase 5 — Guia (Conhecimento, RAG, Quests)
- [ ] Fase 6 — Inteligência (Agentes, Skills, Memória Git)
- [ ] Não sei / Nova fase

## Alternativas Consideradas
[Outras formas de resolver o mesmo problema]
```

---

### A10 — `.github/ISSUE_TEMPLATE/crash_report.md`

```markdown
---
name: Crash Report
about: Reportar crash do servidor ou cliente causado pela Alice
title: '[CRASH] '
labels: bug, crash
assignees: ''
---

## Crash Log
[Colar crash-report completo de crash-reports/ ou latest.log]

## Contexto
- O que Alice estava fazendo no momento: [combate/follow/idle/chat/building/spawning]
- Último comando dado a Alice: [...]
- Frequência: [sempre/às vezes/primeira vez]

## Ambiente
- Fase do projeto: [1-6]
- Versão do mod: [x.y.z]
- Forge version: [1.20.1-47.3.0]
- Modpack: [Cursed Walking com todos os mods / vanilla + Alice]
- Java version: [17]
- RAM alocada: [...]

## Passos para Reproduzir (se possível)
1. ...
2. ...
```

---

### A11 — `.github/PULL_REQUEST_TEMPLATE.md`

```markdown
## Descrição
[O que esta PR faz e por quê]

## Tipo de mudança
- [ ] `feat` — feature nova
- [ ] `fix` — bug fix
- [ ] `refactor` — refatoração sem mudança de comportamento
- [ ] `test` — adição/modificação de testes
- [ ] `docs` — documentação
- [ ] `perf` — otimização de performance
- [ ] `chore` — build, CI, dependências

## Fase
Fase: [1-6]

## Checklist de Review (AD03 Reviewer)

### Correção
- [ ] Código faz o que deveria
- [ ] Edge cases cobertos

### Performance
- [ ] Sem alocação no hot path (tick handler, rule engine)
- [ ] Chamadas LLM são async (CompletableFuture)
- [ ] Sem `new` dentro de loop de tick

### Forge Compliance
- [ ] Usa APIs públicas (não internals)
- [ ] Thread-safe para server tick
- [ ] Não bloqueia main thread
- [ ] Sem acesso client-side em código server-side

### Qualidade
- [ ] Compila sem warnings
- [ ] Testes passam (JUnit + GameTest se aplicável)
- [ ] Logging adequado (não excessivo, não ausente)
- [ ] Config externalizável onde faz sentido
- [ ] Javadoc em métodos públicos da API
- [ ] Segue naming conventions do projeto
- [ ] Sem TODO/FIXME sem issue associada

## Testes
- [ ] Testes unitários adicionados/atualizados
- [ ] GameTest adicionado (se interação in-game)
- [ ] Testado manualmente no servidor

## Screenshots / Vídeo (se visual)
[Se aplicável]
```

---

### A12 — `scripts/check-infra.sh`

```bash
#!/bin/bash
# check-infra.sh — Verifica se toda a infraestrutura está acessível
# Rodar antes de cada sessão de desenvolvimento
# Uso: bash scripts/check-infra.sh

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

echo "========================================"
echo " Projeto Alice — Health Check de Infra"
echo "========================================"
echo ""

ERRORS=0

# --- Máquina 2: Servidor Minecraft + STT ---
echo "=== Máquina 2: Servidor (192.168.0.225) ==="

if ping -c 1 -W 2 192.168.0.225 > /dev/null 2>&1; then
    echo -e "  Ping:            ${GREEN}OK${NC}"
else
    echo -e "  Ping:            ${RED}FAIL${NC} — Máquina 2 não responde"
    ERRORS=$((ERRORS + 1))
fi

if curl -s --max-time 5 http://192.168.0.225:10300/health > /dev/null 2>&1; then
    echo -e "  faster-whisper:  ${GREEN}OK${NC} (porta 10300)"
else
    echo -e "  faster-whisper:  ${RED}FAIL${NC} — STT não responde na porta 10300"
    ERRORS=$((ERRORS + 1))
fi

echo ""

# --- Máquina 3: LLM (Ollama) ---
echo "=== Máquina 3: LLM (192.168.0.200) ==="

if ping -c 1 -W 2 192.168.0.200 > /dev/null 2>&1; then
    echo -e "  Ping:            ${GREEN}OK${NC}"
else
    echo -e "  Ping:            ${RED}FAIL${NC} — Máquina 3 não responde"
    ERRORS=$((ERRORS + 1))
fi

if curl -s --max-time 5 http://192.168.0.200:11434/api/tags > /dev/null 2>&1; then
    echo -e "  Ollama:          ${GREEN}OK${NC} (porta 11434)"
    # Listar modelos disponíveis
    MODELS=$(curl -s --max-time 5 http://192.168.0.200:11434/api/tags \
        | python3 -c "
import sys, json
try:
    tags = json.load(sys.stdin)
    for m in tags.get('models', []):
        print(f'    - {m[\"name\"]}')
except:
    print('    (não foi possível listar modelos)')
" 2>/dev/null)
    echo "  Modelos:"
    echo "$MODELS"
else
    echo -e "  Ollama:          ${RED}FAIL${NC} — LLM não responde na porta 11434"
    ERRORS=$((ERRORS + 1))
fi

echo ""
echo "========================================"
if [ $ERRORS -eq 0 ]; then
    echo -e "  Resultado: ${GREEN}TUDO OK${NC} — Pronto para desenvolver"
else
    echo -e "  Resultado: ${RED}$ERRORS FALHA(S)${NC} — Verificar máquinas com problema"
fi
echo "========================================"

exit $ERRORS
```

---

### A13 — `scripts/deploy.sh`

```bash
#!/bin/bash
# deploy.sh — Builda e deploya o .jar da Alice para o servidor Minecraft
# Uso: bash scripts/deploy.sh
set -e

# ============================================================
# CONFIGURAÇÃO — Ajustar para seu ambiente
# ============================================================
SERVER_HOST="192.168.0.225"
SERVER_USER="user"                          # TODO: ajustar username SSH
SERVER_MODS_PATH="/path/to/server/mods"     # TODO: ajustar caminho dos mods
SERVER_RESTART_CMD="cd /path/to/server && ./restart.sh"  # TODO: ajustar restart

# ============================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

echo "========================================"
echo " Projeto Alice — Deploy para Servidor"
echo "========================================"
echo ""

# 1. Build
echo -e "${YELLOW}[1/4]${NC} Building Alice Mod..."
./gradlew build
echo -e "  Build: ${GREEN}OK${NC}"
echo ""

# 2. Encontrar JAR
JAR=$(ls -t build/libs/alice-*.jar 2>/dev/null | grep -v sources | grep -v javadoc | head -1)
if [ -z "$JAR" ]; then
    echo -e "  ${RED}ERRO: Nenhum JAR encontrado em build/libs/${NC}"
    exit 1
fi
echo -e "${YELLOW}[2/4]${NC} JAR encontrado: $JAR"
echo ""

# 3. Copiar para servidor
echo -e "${YELLOW}[3/4]${NC} Copiando para $SERVER_HOST:$SERVER_MODS_PATH ..."
scp "$JAR" "$SERVER_USER@$SERVER_HOST:$SERVER_MODS_PATH/"
echo -e "  Deploy: ${GREEN}OK${NC}"
echo ""

# 4. Restart servidor
echo -e "${YELLOW}[4/4]${NC} Reiniciando servidor..."
ssh "$SERVER_USER@$SERVER_HOST" "$SERVER_RESTART_CMD"
echo -e "  Restart: ${GREEN}OK${NC}"

echo ""
echo "========================================"
echo -e "  ${GREEN}Deploy completo!${NC}"
echo "  JAR: $(basename $JAR)"
echo "  Servidor: $SERVER_HOST"
echo "========================================"
```

---

## 6. Mapeamento Gaps → Ações {#mapeamento}

| Gap | Status Antes | O que este plano fecha | Status Depois |
|-----|-------------|----------------------|---------------|
| G01 Agentes de dev | ✅ Definidos no doc | — (já fechado na sessão anterior) | ✅ Completo |
| G02 Testes | Definido, não implementado | Workflows prontos (A5, A6) | ⚠️ Framework pronto, testes concretos vêm com Fase 1 |
| G03 CI/CD | Zero | build.yml + gametest.yml + release.yml (A5-A7) | ✅ Pipeline pronta (ativa quando build.gradle existir) |
| G04 Convenções | Zero | .editorconfig + CLAUDE.md + PR template (A2, A3, A11) | ✅ Completo |
| G05 Release | Zero | release.yml + CHANGELOG.md (A7, A4) | ✅ Completo |
| G06 Documentação | Zero | PR template + issue templates (A8-A11) | ⚠️ Wiki vem ao final da Fase 1 |
| G07 Riscos | Definido no doc | Issue templates para tracking (A8-A10) | ⚠️ Processo definido, validação dos RSK vem com Fase 1 |
| G08 Performance | Zero | CLAUDE.md com budgets (A3) | ⚠️ Budgets definidos, medição vem com Fase 1 |
| G09 Infra como código | Zero | check-infra.sh + deploy.sh (A12, A13) | ✅ Scripts prontos |
| G10 Code review | Zero | PR template com checklist + CLAUDE.md com regras (A11, A3) | ✅ Completo |

**Resultado: 6 de 10 gaps completamente fechados. 4 parcialmente fechados (dependem de código existir).**

---

## 7. Verificação Pós-Execução {#verificacao}

Após executar este plano, verificar:

```
1. git status              → limpo, nada pendente
2. git log --oneline -3    → commit "chore: add project infrastructure..." visível
3. git branch -a           → phase/1-foundation existe local e remote
4. GitHub > Issues > New   → templates bug_report, feature_request, crash_report aparecem
5. GitHub > PR > New       → PR template com checklist aparece
6. GitHub > Actions        → 3 workflows visíveis (vão falhar por falta de build.gradle — esperado)
7. bash scripts/check-infra.sh → roda sem erro de sintaxe (resultado depende da rede)
```

---

## 8. O que fica para depois (não está neste plano) {#depois}

### Junto com Fase 1 (Bloco C do processo-dev)
- `build.gradle`, `settings.gradle`, `gradle.properties` — setup do projeto Forge
- `src/main/java/com/alice/` — código fonte Java
- `src/test/java/com/alice/` — testes JUnit por regra implementada
- Performance baseline — medir TPS com/sem Alice
- Validação dos riscos RSK01-RSK10

### Ao concluir Fase 1 (Bloco D do processo-dev)
- GameTest Framework — testes in-game automatizados
- Wiki GitHub — Primeiros-Passos.md, Comandos.md
- Release workflow — tag v0.1.0-alpha
- Deploy no servidor de teste

### Fases futuras
- Docker Compose para faster-whisper — Fase 3
- Wiki completa — cresce com cada fase
