package com.projetoalice.alice.rules;

import com.projetoalice.alice.AliceEntity;
import com.projetoalice.alice.Config;
import com.projetoalice.alice.IAliceRule;
import baritone.api.pathing.goals.GoalRunAway;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Priority 2 — Retreat when HP is low (but not critical).
 * Non-toggleable safety rule.
 */
public class FleeOnLowHealthRule implements IAliceRule {

    private static final int COOLDOWN_TICKS = 60;
    private int cooldown;

    @Override public int priority() { return 2; }
    @Override public String name() { return "FleeOnLowHealth"; }
    @Override public boolean isToggleable() { return false; }

    @Override
    public boolean shouldApply(AliceEntity alice, ServerLevel level) {
        if (cooldown > 0) { cooldown--; return false; }
        var fp = alice.getFakePlayer();
        float ratio = fp.getHealth() / fp.getMaxHealth();
        // Only trigger between critical and flee thresholds
        return ratio >= Config.criticalHealthThreshold && ratio < Config.fleeHealthThreshold;
    }

    @Override
    public void execute(AliceEntity alice, ServerLevel level) {
        cooldown = COOLDOWN_TICKS;
        var fp = alice.getFakePlayer();
        var baritone = alice.getBaritone();

        List<Monster> hostiles = level.getEntitiesOfClass(Monster.class,
                new AABB(fp.blockPosition()).inflate(16));

        if (!hostiles.isEmpty()) {
            BlockPos[] threats = hostiles.stream()
                    .map(LivingEntity::blockPosition)
                    .toArray(BlockPos[]::new);
            baritone.getCustomGoalProcess().setGoalAndPath(new GoalRunAway(12, threats));
        }
    }
}
