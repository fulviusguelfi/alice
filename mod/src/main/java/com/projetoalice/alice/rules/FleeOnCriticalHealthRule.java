package com.projetoalice.alice.rules;

import com.projetoalice.alice.AliceEntity;
import com.projetoalice.alice.Config;
import com.projetoalice.alice.IAliceRule;
import baritone.api.pathing.goals.GoalRunAway;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Priority 1 — Sprint-flee when HP is critically low.
 * Non-toggleable safety rule.
 */
public class FleeOnCriticalHealthRule implements IAliceRule {

    private static final int COOLDOWN_TICKS = 40; // Don't re-trigger for 2 seconds
    private int cooldown;

    @Override public int priority() { return 1; }
    @Override public String name() { return "FleeOnCriticalHealth"; }
    @Override public boolean isToggleable() { return false; }

    @Override
    public boolean shouldApply(AliceEntity alice, ServerLevel level) {
        if (cooldown > 0) { cooldown--; return false; }
        var fp = alice.getFakePlayer();
        return fp.getHealth() / fp.getMaxHealth() < Config.criticalHealthThreshold;
    }

    @Override
    public void execute(AliceEntity alice, ServerLevel level) {
        cooldown = COOLDOWN_TICKS;
        var fp = alice.getFakePlayer();
        var baritone = alice.getBaritone();

        // Find nearest hostile to flee FROM
        List<Monster> hostiles = level.getEntitiesOfClass(Monster.class,
                new AABB(fp.blockPosition()).inflate(20));

        if (!hostiles.isEmpty()) {
            BlockPos[] threats = hostiles.stream()
                    .map(LivingEntity::blockPosition)
                    .toArray(BlockPos[]::new);
            baritone.getCustomGoalProcess().setGoalAndPath(new GoalRunAway(20, threats));
        }

        // Broadcast warning
        if (fp.getServer() != null) {
            fp.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("[Alice] FUGINDO! HP critico!"), false);
        }
    }
}
