package com.projetoalice.alice.rules;

import com.projetoalice.alice.AliceEntity;
import com.projetoalice.alice.Config;
import com.projetoalice.alice.IAliceRule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

/**
 * Priority 11 — Attack the nearest hostile mob within combat radius.
 * Toggleable combat rule.
 *
 * When no hostiles are in range, this rule MUST clear the CustomGoalProcess
 * it previously set, otherwise FollowProcess stays preempted in Baritone's
 * PathingControlManager (ties on DEFAULT_PRIORITY resolve by insertion order).
 */
public class AttackNearestHostileRule implements IAliceRule {

    private static final Logger LOGGER = LoggerFactory.getLogger("Alice");
    private static final int ATTACK_COOLDOWN_TICKS = 10; // 0.5 seconds between swings
    private int cooldown;
    private boolean hadCombatGoal;

    @Override public int priority() { return 11; }
    @Override public String name() { return "AttackNearestHostile"; }
    @Override public boolean isToggleable() { return true; }

    @Override
    public boolean shouldApply(AliceEntity alice, ServerLevel level) {
        if (!Config.autoCombat) {
            clearCombatGoalIfAny(alice);
            return false;
        }
        if (cooldown > 0) { cooldown--; return false; }

        var fp = alice.getFakePlayer();
        if (fp.getHealth() / fp.getMaxHealth() < Config.fleeHealthThreshold) {
            clearCombatGoalIfAny(alice);
            return false;
        }

        List<Monster> hostiles = level.getEntitiesOfClass(Monster.class,
                new AABB(fp.blockPosition()).inflate(Config.combatRadius));
        if (hostiles.isEmpty()) {
            clearCombatGoalIfAny(alice);
            return false;
        }
        return true;
    }

    @Override
    public void execute(AliceEntity alice, ServerLevel level) {
        cooldown = ATTACK_COOLDOWN_TICKS;
        var fp = alice.getFakePlayer();

        List<Monster> hostiles = level.getEntitiesOfClass(Monster.class,
                new AABB(fp.blockPosition()).inflate(Config.combatRadius));

        hostiles.stream()
                .min(Comparator.comparingDouble(m -> m.distanceToSqr(fp)))
                .ifPresent(target -> {
                    double dist = fp.distanceTo(target);
                    var goalPos = target.blockPosition();
                    LOGGER.info("[Alice][Combat] engaging {} at ({},{},{}) dist={}",
                            target.getType().getDescriptionId(),
                            goalPos.getX(), goalPos.getY(), goalPos.getZ(),
                            String.format("%.2f", dist));
                    if (dist <= 3.0) {
                        // Melee: swing and do not touch path — let FollowProcess stay in control.
                        fp.swing(InteractionHand.MAIN_HAND);
                        fp.attack(target);
                    } else {
                        alice.getBaritone().getCustomGoalProcess().setGoalAndPath(
                                new baritone.api.pathing.goals.GoalNear(goalPos, 2));
                        hadCombatGoal = true;
                    }
                });
    }

    private void clearCombatGoalIfAny(AliceEntity alice) {
        if (!hadCombatGoal) return;
        try {
            alice.getBaritone().getCustomGoalProcess().onLostControl();
        } catch (Exception e) {
            LOGGER.warn("[Alice][Combat] error clearing combat goal", e);
        }
        hadCombatGoal = false;
        LOGGER.info("[Alice][Combat] No hostiles — cleared combat goal, follow resumes");
    }
}
