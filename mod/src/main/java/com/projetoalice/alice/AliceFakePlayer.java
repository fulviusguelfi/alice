package com.projetoalice.alice;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.util.FakePlayer;

/**
 * Custom FakePlayer that restores tick() behavior.
 * Forge's FakePlayer overrides tick() to be a NO-OP, which prevents entity
 * physics (movement, gravity, collision) from being processed. This subclass
 * re-enables the essential parts of the tick chain that Baritone needs.
 */
public class AliceFakePlayer extends FakePlayer {

    public AliceFakePlayer(ServerLevel level, GameProfile profile) {
        super(level, profile);
    }

    /**
     * Re-enable ticking by calling the real ServerPlayer tick chain.
     * - ServerPlayer.tick(): gameMode, containers, camera, advancements
     * - doTick(): calls Player.tick() -> LivingEntity.tick() -> Entity.tick() (physics/movement)
     * FakePlayer.tick() was overridden to {} by Forge, so we call doTick() which invokes super.tick()
     * (Player.tick) directly.
     */
    @Override
    public void tick() {
        // gameMode.tick() is needed for block breaking
        this.gameMode.tick();

        // doTick() calls super.tick() = Player.tick() -> LivingEntity.tick() -> Entity.tick()
        // This processes movement (xxa/zza), gravity, collision detection, etc.
        this.doTick();
    }
}
