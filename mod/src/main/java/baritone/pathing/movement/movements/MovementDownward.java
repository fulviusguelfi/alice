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

package baritone.pathing.movement.movements;

import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.CalculationContext;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class MovementDownward extends Movement {

    private int numTicks = 0;

    public MovementDownward(IBaritone baritone, BetterBlockPos start, BetterBlockPos end) {
        super(baritone, start, end, new BetterBlockPos[]{end});
    }

    @Override
    public void reset() {
        super.reset();
        numTicks = 0;
    }

    @Override
    public double calculateCost(CalculationContext context) {
        return cost(context, src.x, src.y, src.z);
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        return ImmutableSet.of(src, dest);
    }

    public static double cost(CalculationContext context, int x, int y, int z) {
        if (!context.allowDownward) {
            return COST_INF;
        }
        if (!MovementHelper.canWalkOn(context, x, y - 2, z)
                && !MovementHelper.isClimbable(context.get(x, y - 2, z))) {
            return COST_INF;
        }
        BlockState down = context.get(x, y - 1, z);
        if (MovementHelper.isClimbable(down)) {
            return LADDER_DOWN_ONE_COST;
        } else {
            // we're standing on it, while it might be block falling, it'll be air by the time we get here in the movement
            return FALL_N_BLOCKS_COST[1] + MovementHelper.getMiningDurationTicks(context, x, y - 1, z, down, false);
        }
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        if (ctx.playerFeet().equals(dest)) {
            return state.setStatus(MovementStatus.SUCCESS);
        } else if (!playerInValidPosition()) {
            return state.setStatus(MovementStatus.UNREACHABLE);
        }
        double diffX = ctx.player().position().x - (dest.getX() + 0.5);
        double diffZ = ctx.player().position().z - (dest.getZ() + 0.5);
        double ab = Math.sqrt(diffX * diffX + diffZ * diffZ);

        if (numTicks++ < 10 && ab < 0.2) {
            return state;
        }
        MovementHelper.moveTowards(ctx, state, positionsToBreak[0]);
        // Scaffolding: SNEAK always, both to sink through the solid top face AND to step off the
        // edge when entering from above.
        // Other free-standing climbables (rope, twisting_vines): SNEAK only when stepping OFF (feet
        // not yet climbable) — once inside, SNEAK halts descent (ladder-like "hold position").
        // Vanilla ladder/vine: never SNEAK (MOVE_FORWARD against wall handles descent).
        BlockState feet = BlockStateInterface.get(ctx, ctx.playerFeet());
        BlockState destState = BlockStateInterface.get(ctx, positionsToBreak[0]);
        if (MovementHelper.isClimbable(destState)
                && destState.getBlock() != Blocks.LADDER
                && destState.getBlock() != Blocks.VINE) {
            boolean isScaffolding = destState.getBlock() == Blocks.SCAFFOLDING;
            if (isScaffolding || !MovementHelper.isClimbable(feet)) {
                state.setInput(Input.SNEAK, true);
            }
        }
        return state;
    }
}
