package com.projetoalice.alice;

import net.minecraft.server.level.ServerLevel;

/**
 * A tick-based rule that Alice evaluates every server tick.
 * Rules have priority (lower = higher priority) and run before any LLM decision.
 */
public interface IAliceRule {

    /** Lower number = higher priority. Safety rules: 1-10, Combat: 11-30, Utility: 31-60. */
    int priority();

    /** Human-readable name for logging. */
    String name();

    /** Whether the rule can be toggled off by the player. */
    boolean isToggleable();

    /** Check if this rule should fire given current state. Called every tick. Must be fast (<1ms). */
    boolean shouldApply(AliceEntity alice, ServerLevel level);

    /** Execute the rule's action. Called only when shouldApply() returns true. */
    void execute(AliceEntity alice, ServerLevel level);
}
