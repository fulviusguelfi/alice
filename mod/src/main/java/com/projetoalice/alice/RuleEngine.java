package com.projetoalice.alice;

import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Evaluates Alice's rules every tick in priority order.
 * First matching rule wins (no further rules evaluated that tick).
 */
public class RuleEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger("Alice");
    private final List<IAliceRule> rules = new ArrayList<>();
    private final Set<String> disabledRules = new HashSet<>();
    private String lastFired;

    public void addRule(IAliceRule rule) {
        rules.add(rule);
        rules.sort(Comparator.comparingInt(IAliceRule::priority));
    }

    public void toggleRule(String name, boolean enabled) {
        if (enabled) {
            disabledRules.remove(name);
        } else {
            disabledRules.add(name);
        }
    }

    /**
     * Evaluate all rules in priority order. Returns the name of the rule that fired, or null.
     */
    public String tick(AliceEntity alice, ServerLevel level) {
        String fired = null;
        for (IAliceRule rule : rules) {
            if (rule.isToggleable() && disabledRules.contains(rule.name())) continue;
            if (rule.shouldApply(alice, level)) {
                rule.execute(alice, level);
                if (Config.logRules) {
                    LOGGER.info("[Alice][Rule] {} fired", rule.name());
                }
                fired = rule.name();
                break;
            }
        }
        if (!java.util.Objects.equals(fired, lastFired)) {
            LOGGER.info("[Alice][Rule] switch {} -> {}",
                    lastFired == null ? "<none>" : lastFired,
                    fired == null ? "<none>" : fired);
            AliceMod.JOURNAL.recordRuleSwitch(fired,
                    "transicao de " + (lastFired == null ? "idle" : lastFired));
            lastFired = fired;
        }
        return fired;
    }
}
