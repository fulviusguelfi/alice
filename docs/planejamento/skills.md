# Projeto Alice — Catálogo de Skills

**Versão:** 1.0  
**Data:** 2026-04-03  
**Status:** Documento de referência — extraído do Plano de Projeto v1.0

---

## Catálogo de Skills {#skills}

### Estrutura de um Arquivo SKILL.md

```yaml
---
# METADADOS OBRIGATÓRIOS
name: combat-zombie-horde              # ID único, kebab-case
version: 1.0.0
description: Táticas para combate contra hordas de 5+ zumbis
author: alice-core                     # "alice-core" ou "learned"

# QUANDO USAR
triggers:
  - condition: hostile_count >= 5
  - condition: event == blood_moon
  - keyword: ["horda", "muitos zumbis", "cercada"]

# CONTEXTO
phase: [early, mid, late]              # Fases do jogo onde é aplicável
priority: high                         # low | medium | high | critical
domains: [combat]                      # combat|building|crafting|navigation|survival

# RECURSOS
requires_skills: []                    # Skills que devem estar carregadas junto
incompatible_with: [combat-retreat-protocol]  # Não usar junto com estas
---

# Combate Contra Horda de Zumbis

## Situação
Use esta skill quando há 5 ou mais zumbis hostis no raio de percepção,
ou durante blood moons quando múltiplos zumbis atacam simultaneamente.

## Avaliação Rápida
1. Contar zumbis normais vs especiais (mutantes têm HP 3x maior)
2. Verificar munição disponível (armas de fogo preferidas para hordas)
3. Verificar posição: área aberta (desvantagem) vs corredor/porta (vantagem)

## Táticas por Cenário

### Corredor ou Porta (posição defensiva)
- Alice posiciona na porta, ataca de 1 em 1 (funil)
- Jogador protege flanco
- Usar escudo para bloquear ataques entre golpes

### Área Aberta (sem cobertura)
- PRIORIDADE: encontrar cobertura imediatamente
- Alice usa arma de fogo, manter distância 10-15 blocos
- Jogador como isco, Alice como atiradora de cobertura
- Se sem cobertura a <5 blocos: executar retreat-protocol

### Blood Moon
- NUNCA sair da base
- Defender entrada da base
- Alice em posição elevada (vantagem de altura)
- Conservar munição — usar melee para zumbis normais

## O Que Alice Fala
- Início de horda: "Vejo {count} zumbis se aproximando. {tactic}"
- Durante combate: indicações de ameaça especial somente
- Fim de horda: "Área limpa. {playerName}, tudo bem?"

## Auto-verificação
- Sucesso: nenhum hostil a <20 blocos após execução
- Falha: Alice ou jogador morreu / recuaram
- Se falha: registrar como "retreat-success" se ambos sobreviveram
```

### Catálogo Inicial — 20 Skills Prioritárias

#### Domínio: Combate (5 skills)

| ID | Descrição | Fase | Prioridade |
|----|-----------|------|-----------|
| `combat-zombie-horde` | Táticas contra 5+ zumbis | all | high |
| `combat-ranged-engagement` | Combate com armas de fogo | mid/late | medium |
| `combat-retreat-protocol` | Protocolo de fuga organizada | all | critical |
| `combat-special-zombie` | Zumbis mutantes e bosses | mid/late | high |
| `combat-defend-base` | Defender base de invasão | mid/late | high |

#### Domínio: Construção (4 skills)

| ID | Descrição | Fase | Prioridade |
|----|-----------|------|-----------|
| `build-emergency-shelter` | Abrigo de emergência rápido | early | high |
| `build-defensive-wall` | Muro perimetral de defesa | early/mid | medium |
| `build-watchtower` | Torre de vigia com vantagem | mid | medium |
| `build-create-mill` | Moinho automatizado Create | mid/late | low |

#### Domínio: Crafting (3 skills)

| ID | Descrição | Fase | Prioridade |
|----|-----------|------|-----------|
| `craft-optimal-path` | Ordem ótima de crafting para meta | all | medium |
| `craft-batch-planning` | Planejar múltiplos crafts em sequência | mid | low |
| `craft-emergency-weapons` | Armas de emergência com materiais mínimos | early | high |

#### Domínio: Sobrevivência (5 skills)

| ID | Descrição | Fase | Prioridade |
|----|-----------|------|-----------|
| `survival-infection-protocol` | Combater/prevenir infecção zumbi | all | critical |
| `survival-blood-moon-prep` | Preparação para blood moon | all | critical |
| `survival-food-crisis` | Encontrar comida quando tudo acabou | early/mid | high |
| `survival-night-protocol` | Sobreviver a noite sem base | early | high |
| `survival-resource-gathering` | Rotina eficiente de coleta | all | medium |

#### Domínio: Navegação (3 skills)

| ID | Descrição | Fase | Prioridade |
|----|-----------|------|-----------|
| `navigate-safe-route` | Rota segura de A a B evitando ameaças | all | medium |
| `navigate-city-exploration` | Explorar cidade com segurança | mid/late | medium |
| `navigate-return-home` | Retornar à base quando perdida ou de noite | all | high |

### Indexação de Skills

```java
// SkillMatcher — busca por sobreposição de tokens
public List<Skill> findRelevant(String context, int topK) {
    // 1. Tokenizar contexto
    Set<String> contextTokens = tokenize(context);
    
    // 2. Para cada skill, calcular score
    return allSkills.stream()
        .map(skill -> {
            double score = 0;
            score += overlap(contextTokens, skill.getKeywords()) * 2.0;  // keywords pesam 2x
            score += overlap(contextTokens, skill.getDescription()) * 1.0;
            score += (currentPhase.matches(skill.getPhase())) ? 0.5 : 0;
            return Map.entry(skill, score);
        })
        .sorted(Map.Entry.<Skill, Double>comparingByValue().reversed())
        .limit(topK)
        .map(Map.Entry::getKey)
        .collect(toList());
}
```

### Criação Automática de Skills

Quando Alice resolve problema novo não coberto por skill existente:

```
1. Orquestrador detecta: score de match de todas as skills < 0.2
2. Após resolução: LLM recebe prompt de síntese:
   "Você resolveu: {problema}. Usando: {solução}. Resultado: {resultado}.
    Gere um SKILL.md no formato padrão para esta situação."
3. LLM gera SKILL.md
4. SkillCreator valida formato YAML do frontmatter
5. Salva em runtime skill dir (não classpath)
6. GitMemory.commit("New skill: {skillName}")
7. SkillLibrary.reload()
```

---
