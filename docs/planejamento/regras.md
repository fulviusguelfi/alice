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
