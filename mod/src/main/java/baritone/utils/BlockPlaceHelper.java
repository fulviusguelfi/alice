/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils;

import baritone.Baritone;
import baritone.api.utils.IPlayerContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockPlaceHelper {
    // base ticks between places caused by tick logic
    private static final int BASE_PLACE_DELAY = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger("Alice");
    // Throttle: log at most once every N ticks to avoid spamming while a right-click spam loop
    // fires 1 event every rightClickSpeed ticks (default 4).
    private static final int DIAG_LOG_INTERVAL_TICKS = 20;

    private final IPlayerContext ctx;
    private int rightClickTimer;
    private int diagLogCooldown;

    BlockPlaceHelper(IPlayerContext playerContext) {
        this.ctx = playerContext;
    }

    public void tick(boolean rightClickRequested) {
        if (diagLogCooldown > 0) diagLogCooldown--;
        if (rightClickTimer > 0) {
            rightClickTimer--;
            return;
        }
        HitResult mouseOver = ctx.objectMouseOver();
        if (!rightClickRequested) {
            return;
        }
        // Diagnostic: why didn't the click land? Throttled.
        if (mouseOver == null || mouseOver.getType() != HitResult.Type.BLOCK) {
            if (diagLogCooldown == 0) {
                diagLogCooldown = DIAG_LOG_INTERVAL_TICKS;
                LOGGER.info("[Alice][BlockPlace] skip: mouseOver={} (need BLOCK) rot=({},{})",
                        mouseOver == null ? "null" : mouseOver.getType(),
                        String.format("%.1f", ctx.playerRotations().getYaw()),
                        String.format("%.1f", ctx.playerRotations().getPitch()));
            }
            return;
        }
        BlockHitResult blockHit = (BlockHitResult) mouseOver;
        rightClickTimer = Baritone.settings().rightClickSpeed.value - BASE_PLACE_DELAY;
        boolean logThis = diagLogCooldown == 0;
        if (logThis) diagLogCooldown = DIAG_LOG_INTERVAL_TICKS;
        // Server-side, DoorBlock#use/TrapDoorBlock#use/FenceGateBlock#use return InteractionResult.CONSUME
        // (not SUCCESS, which is the client-side value). Vanilla Baritone breaks out on SUCCESS only,
        // so when running server-side it would continue to OFF_HAND and re-toggle the door. We consider
        // any "consumed the click" result as done to avoid the oscillation.
        InteractionResult mainResult = ctx.playerController().processRightClickBlock(
                ctx.player(), ctx.world(), InteractionHand.MAIN_HAND, blockHit);
        if (mainResult.consumesAction()) {
            if (mainResult == InteractionResult.SUCCESS) ctx.player().swing(InteractionHand.MAIN_HAND);
            if (logThis) {
                LOGGER.info("[Alice][BlockPlace] click done hand=MAIN hit={} face={} block={} -> {}",
                        blockHit.getBlockPos(), blockHit.getDirection(),
                        ctx.world().getBlockState(blockHit.getBlockPos()).getBlock().getDescriptionId(),
                        mainResult);
            }
            return;
        }
        if (!ctx.player().getItemInHand(InteractionHand.MAIN_HAND).isEmpty()
                && ctx.playerController().processRightClick(ctx.player(), ctx.world(), InteractionHand.MAIN_HAND).consumesAction()) {
            return;
        }
        InteractionResult offResult = ctx.playerController().processRightClickBlock(
                ctx.player(), ctx.world(), InteractionHand.OFF_HAND, blockHit);
        if (offResult.consumesAction()) {
            if (offResult == InteractionResult.SUCCESS) ctx.player().swing(InteractionHand.OFF_HAND);
            if (logThis) {
                LOGGER.info("[Alice][BlockPlace] click done hand=OFF hit={} face={} block={} -> {}",
                        blockHit.getBlockPos(), blockHit.getDirection(),
                        ctx.world().getBlockState(blockHit.getBlockPos()).getBlock().getDescriptionId(),
                        offResult);
            }
            return;
        }
        if (!ctx.player().getItemInHand(InteractionHand.OFF_HAND).isEmpty()) {
            ctx.playerController().processRightClick(ctx.player(), ctx.world(), InteractionHand.OFF_HAND);
        }
        if (logThis) {
            LOGGER.info("[Alice][BlockPlace] no consume hit={} face={} block={} main={} off={}",
                    blockHit.getBlockPos(), blockHit.getDirection(),
                    ctx.world().getBlockState(blockHit.getBlockPos()).getBlock().getDescriptionId(),
                    mainResult, offResult);
        }
    }
}
