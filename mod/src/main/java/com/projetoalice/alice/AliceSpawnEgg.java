package com.projetoalice.alice;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * AliceSpawnEgg — Item that spawns (or repositions) Alice at the clicked location.
 *
 * If Alice is not yet attached, this performs a full attach at the clicked position.
 * If Alice is already attached, teleports her to the clicked position.
 *
 * Obtain via: /give @p alice:alice_spawn_egg
 * DOD: D03
 */
public class AliceSpawnEgg extends Item {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static AliceEntity boundEntity;

    public AliceSpawnEgg(Properties properties) {
        super(properties);
    }

    /**
     * Bind the mod-level AliceEntity singleton so the item can drive it.
     * Called once from AliceMod constructor, same pattern as AliceCommands.
     */
    public static void bindEntity(AliceEntity entity) {
        boundEntity = entity;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) {
            // Item is server-side only; ignore client-side invocation
            return InteractionResult.PASS;
        }

        if (boundEntity == null) {
            LOGGER.error("[Alice] AliceSpawnEgg: boundEntity is null — AliceMod not initialised?");
            return InteractionResult.FAIL;
        }

        // Spawn position: top face of the clicked block
        var blockPos = ctx.getClickedPos().relative(ctx.getClickedFace());
        Vec3 spawnPos = new Vec3(
                blockPos.getX() + 0.5,
                blockPos.getY(),
                blockPos.getZ() + 0.5
        );

        if (!boundEntity.isAttached()) {
            LOGGER.info("[Alice] AliceSpawnEgg: attaching Alice at ({}, {}, {})",
                    (int) spawnPos.x, (int) spawnPos.y, (int) spawnPos.z);
            boundEntity.attach(level);
            boundEntity.getFakePlayer().moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0, 0);
            boundEntity.broadcastSpawn(level.getServer());
        } else {
            LOGGER.info("[Alice] AliceSpawnEgg: teleporting Alice to ({}, {}, {})",
                    (int) spawnPos.x, (int) spawnPos.y, (int) spawnPos.z);
            boundEntity.getFakePlayer().moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0, 0);
            boundEntity.broadcastSpawn(level.getServer());
        }

        return InteractionResult.SUCCESS;
    }
}
