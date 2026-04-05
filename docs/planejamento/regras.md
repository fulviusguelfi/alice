# Projeto Alice — Regras Always-On

**Versão:** 1.0  
**Data:** 2026-04-03  
**Status:** Documento de referência — extraído do Plano de Projeto v1.0

---

## Regras Always-On {#regras}

As regras são executadas a cada tick do servidor (50ms) e têm **prioridade absoluta** sobre qualquer decisão do LLM. Elas garantem a segurança e sobrevivência da Alice sem depender de latência de IA.

### Estrutura de Implementação

```java
public interface IAliceRule {
    int priority();          // Menor número = maior prioridade
    boolean shouldApply(AliceFakePlayer alice, GameState state);
    void execute(AliceFakePlayer alice);
    String getName();
    boolean isToggleable();  // Algumas regras podem ser desativadas pelo jogador
}
```

### Catálogo Completo de Regras

#### Regras de Segurança (Prioridade 1-10, não toggleáveis)

| # | Nome | Prioridade | Trigger | Ação | Latência |
|---|------|-----------|---------|------|----------|
| R01 | FleeOnCriticalHealth | 1 | HP < 10% | Baritone foge em direção oposta ao mob mais próximo, ativa sprint | <1ms |
| R02 | FleeOnLowHealth | 2 | HP < 20% | Baritone recua para posição defensiva (atrás de obstáculo) | <1ms |
| R03 | PanicOnSurround | 3 | 4+ hostis a <3 blocos em todos os lados | Pular + sprint em diagonal para romper cerco | <1ms |
| R04 | StopBuildingOnAttack | 4 | Em construção AND recebe dano | Cancelar BuilderProcess, entrar em modo combate | <1ms |
| R05 | NeverLeaveBase_BloodMoon | 5 | Blood moon ativo AND fora da base | Baritone retornar à base imediatamente | <1ms |

#### Regras de Combate (Prioridade 11-30, toggleáveis)

| # | Nome | Prioridade | Trigger | Ação | Toggle |
|---|------|-----------|---------|------|--------|
| R11 | AttackNearestHostile | 11 | Mob hostil a <8 blocos E modo follow | Atacar mob com arma equipada | Sim |
| R12 | PrioritizeSpecialZombie | 12 | Zumbi especial (mutante, boss) na área | Mudar target para especial primeiro | Sim |
| R13 | UseRangedOnDistance | 13 | Mob hostil a 8-20 blocos AND arma de fogo disponível | Equipar arma de fogo, atacar | Sim |
| R14 | DefendPlayer | 14 | Jogador está sendo atacado E Alice não está em fuga | Interceptar mob atacando jogador | Sim |

#### Regras de Utilidade (Prioridade 31-60, toggleáveis)

| # | Nome | Prioridade | Trigger | Ação | Toggle |
|---|------|-----------|---------|------|--------|
| R31 | EatWhenHungry | 31 | Fome < 6/20 | Comer comida do inventário (prioridade: carne cozida > pão > qualquer) | Não |
| R32 | AutoEquipBestArmor | 32 | Item de armadura no inventário melhor que atual | Equipar automaticamente | Sim |
| R33 | AutoEquipBestWeapon | 33 | Arma no inventário melhor que atual (por tier) | Equipar automaticamente no hotbar | Sim |
| R34 | PickupNearbyItems | 34 | Item no chão a <4 blocos E modo follow | Navegar até item, coletar | Sim |
| R35 | HealWithFood | 35 | HP < 70% E fome > 15 E fora de combate | Comer comida para regen natural | Não |
| R36 | RechargeShield | 36 | Escudo no inventário E fora de combate | Equipar escudo em offhand entre lutas | Sim |

#### Regras de Estado (Prioridade 61-80, não toggleáveis)

| # | Nome | Prioridade | Trigger | Ação | |
|---|------|-----------|---------|------|--|
| R61 | RespawnAndReunite | 61 | Alice respawnou | Baritone goto base ou jogador (mais próximo) | — |
| R62 | FollowIfPlayerFar | 62 | Modo follow AND distância > 20 blocos do jogador | Retomar follow | — |
| R63 | StayIfPlayerClose | 63 | Modo stay AND distância < 4 blocos (bloqueando) | Mover 2 blocos para o lado | — |

### Implementação Técnica das Regras Críticas

**R01 - FleeOnCriticalHealth:**
```java
public void execute(AliceFakePlayer alice) {
    // Encontrar mob mais próximo
    LivingEntity nearest = findNearestHostile(alice, 20);
    if (nearest == null) return;
    
    // Calcular direção oposta
    Vec3 awayDir = alice.position().subtract(nearest.position()).normalize();
    BlockPos fleeTarget = BlockPos.containing(
        alice.position().add(awayDir.scale(20))
    );
    
    // Baritone foge
    IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(alice);
    baritone.getCommandManager().execute("sprint on");
    baritone.getPathingBehavior().setGoal(new GoalNear(fleeTarget, 2));
    baritone.getPathingBehavior().startPathing();
    
    // Aviso ao jogador
    alice.getServer().getPlayerList().broadcastSystemMessage(
        Component.literal("[Alice] FUGINDO! HP crítico!").withStyle(Style.EMPTY.withColor(0xFF0000)),
        false
    );
}
```

**R33 - AutoEquipBestWeapon:**
```java
public boolean shouldApply(AliceFakePlayer alice, GameState state) {
    ItemStack currentWeapon = alice.getMainHandItem();
    return alice.getInventory().items.stream()
        .filter(item -> item.getItem() instanceof SwordItem || item.getItem() instanceof AxeItem)
        .anyMatch(item -> compareWeaponStrength(item, currentWeapon) > 0);
}

public void execute(AliceFakePlayer alice) {
    alice.getInventory().items.stream()
        .filter(item -> item.getItem() instanceof SwordItem || item.getItem() instanceof AxeItem)
        .max((a, b) -> compareWeaponStrength(a, b))
        .ifPresent(bestWeapon -> {
            int slot = alice.getInventory().findSlotMatchingItem(bestWeapon);
            alice.getInventory().selected = slot % 9; // Hotbar slot
        });
}
```

---

## RuleEngine — Motor de Execução

O `RuleEngine` é um componente singleton que avalia todas as regras registradas a cada `ServerTickEvent` do Forge, respeitando prioridade e budget de tempo.

### Contrato de Execução

```java
public class RuleEngine {
    private final List<IAliceRule> rules = new ArrayList<>();  // Ordenado por prioridade
    private final Map<String, Boolean> toggles = new HashMap<>();  // Config runtime

    public void register(IAliceRule rule) {
        rules.add(rule);
        rules.sort(Comparator.comparingInt(IAliceRule::priority));
    }

    public void evaluate(AliceFakePlayer alice, GameState state) {
        long start = System.nanoTime();
        for (IAliceRule rule : rules) {
            // Toggle de runtime (regras toggleáveis podem estar desativadas)
            if (rule.isToggleable() && !toggles.getOrDefault(rule.getName(), true)) {
                continue;
            }
            if (rule.shouldApply(alice, state)) {
                rule.execute(alice);
                // Regras de segurança (priority 1-10) são EXCLUSIVAS — para após executar
                if (rule.priority() <= 10) break;
            }
        }
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        if (elapsed > 2) {
            LOGGER.warn("[PROC] RuleEngine.evaluate() exceeded budget: {}ms (budget=2ms)", elapsed);
        }
    }
}
```

### Ordem de Avaliação e Exclusividade

| Faixa de Prioridade | Categoria | Comportamento |
|---------------------|-----------|---------------|
| 1-10 | Segurança | **Exclusiva** — primeira regra que aplica termina o tick |
| 11-30 | Combate | Múltiplas podem aplicar em sequência |
| 31-60 | Utilidade | Múltiplas podem aplicar em sequência |
| 61-80 | Estado | Avaliadas sempre por último |

**Razão da exclusividade:** Uma regra de segurança (ex: fugir) não pode coexistir com combate no mesmo tick — a Alice não foge e ataca simultaneamente.

### Performance Budget

- **Budget total da RuleEngine:** < 2ms por tick (de 50ms do server tick)
- **Budget individual por regra:** < 0.1ms em `shouldApply()`
- **Exceder budget:** log `WARN` automático com `[PROC]` + regra lenta identificada

---

## Configuração Runtime (Toggles)

Regras marcadas como `isToggleable() == true` podem ser desativadas pelo jogador via comando de chat ou arquivo de config.

### Comandos de Chat (Fase 1+)

```
/alice rule list                       # Lista todas as regras e estado (on/off)
/alice rule toggle <nome>              # Ativa/desativa uma regra
/alice rule enable <nome>              # Força ativação
/alice rule disable <nome>             # Força desativação
/alice rule reset                      # Restaura padrões
```

### Arquivo de Config (ForgeConfigSpec)

```toml
# config/alice-common.toml
[rules]
  [rules.combat]
    attack_nearest_hostile = true
    prioritize_special_zombie = true
    use_ranged_on_distance = true
    defend_player = true

  [rules.utility]
    auto_equip_best_armor = true
    auto_equip_best_weapon = true
    pickup_nearby_items = true
    recharge_shield = true
```

**Regras NÃO toggleáveis** (segurança + state) não aparecem na config — são sempre ativas.

---

## Requisitos de Teste

Toda `IAliceRule` DEVE ter:

1. **Teste unitário de `shouldApply()`** — cobrir cenário positivo, negativo e boundary (ex: HP exatamente em 10%, 9.9%, 10.1%)
2. **Teste unitário de `execute()`** — validar efeito esperado (mock de Baritone, inventário, etc.)
3. **Teste de prioridade** — garantir que regra respeita ordem em `RuleEngine`
4. **Teste de toggle** — se `isToggleable() == true`, validar que toggle funciona

Exemplo:

```java
@Test
void fleeOnCriticalHealth_triggersAtExactly10Percent() {
    AliceFakePlayer alice = mockAliceWithHealth(2.0f);  // 10% de 20 HP
    GameState state = mockState();
    assertTrue(new FleeOnCriticalHealth().shouldApply(alice, state));
}

@Test
void fleeOnCriticalHealth_doesNotTriggerAt11Percent() {
    AliceFakePlayer alice = mockAliceWithHealth(2.2f);  // 11% de 20 HP
    GameState state = mockState();
    assertFalse(new FleeOnCriticalHealth().shouldApply(alice, state));
}
```

Referência: `processo-desenvolvimento.md` §5.2 (Testes Unitários).

---

## Resumo do Catálogo

| Categoria | Faixa | Quantidade | Toggleáveis | Exclusiva |
|-----------|-------|-----------|-------------|-----------|
| Segurança | 1-10 | 5 (R01-R05) | Não | **Sim** |
| Combate | 11-30 | 4 (R11-R14) | Sim | Não |
| Utilidade | 31-60 | 6 (R31-R36) | Maioria | Não |
| Estado | 61-80 | 3 (R61-R63) | Não | Não |
| **TOTAL** | 1-80 | **18 regras** | 10 | — |

### Rule Registry Inicial — Distribuição por Fase

| Fase | Regras Implementadas | Observação |
|------|---------------------|------------|
| Fase 1 (Fundação) | R01, R02, R11, R31, R32, R33, R61, R62, R63 (9 regras) | MVP das always-on |
| Fase 2 (Utilidade) | R34, R35, R36 (3 regras) | Depende de inventário/containers |
| Fase 3 (Voz) | — | Voz não altera regras |
| Fase 4 (Construção) | R04 (1 regra) | Depende de BuilderProcess |
| Fase 5 (Guia) | R05 (1 regra) | Depende de base conhecida |
| Fase 6 (Inteligência) | R03, R12, R13, R14 (4 regras) | Depende de detecção avançada |

**Nota:** A faixa de prioridade (1-80) tem gaps intencionais (R06-R10, R15-R30, R37-R60, R64-R80) reservados para regras futuras descobertas durante implementação, sem necessidade de renumerar o catálogo.

---

## Documentos Relacionados

- `plano-projeto-alice.md` §Regras Always-On — catálogo original (sincronizado com este documento)
- `processo-desenvolvimento.md` §4.6 — Política de Logging (obrigatória em todas as regras)
- `processo-desenvolvimento.md` §5.2 — Testes Unitários para regras
- `agentes.md` — Agentes LLM (prioridade INFERIOR às regras; só agem quando nenhuma regra aplicou)

---
