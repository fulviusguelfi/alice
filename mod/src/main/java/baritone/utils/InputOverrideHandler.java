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
import baritone.api.event.events.TickEvent;
import baritone.api.utils.IInputOverrideHandler;
import baritone.api.utils.input.Input;
import baritone.behavior.Behavior;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Server-side input override handler. Instead of swapping the player's Input object
 * (which is client-only), this directly sets movement fields on the LivingEntity.
 *
 * @author Brady (original), Alice Project (server-side port)
 * @since 7/31/2018
 */
public final class InputOverrideHandler extends Behavior implements IInputOverrideHandler {

    private final Set<Input> inputForceStateMap = EnumSet.noneOf(Input.class);

    private final BlockBreakHelper blockBreakHelper;
    private final BlockPlaceHelper blockPlaceHelper;
    private boolean needsUpdate;

    public InputOverrideHandler(Baritone baritone) {
        super(baritone);
        this.blockBreakHelper = new BlockBreakHelper(baritone.getPlayerContext());
        this.blockPlaceHelper = new BlockPlaceHelper(baritone.getPlayerContext());
    }

    @Override
    public final synchronized boolean isInputForcedDown(Input input) {
        return input != null && this.inputForceStateMap.contains(input);
    }

    @Override
    public final synchronized void setInputForceState(Input input, boolean forced) {
        if (forced) {
            this.inputForceStateMap.add(input);
        } else {
            this.inputForceStateMap.remove(input);
        }
        this.needsUpdate = true;
    }

    @Override
    public final synchronized void clearAllKeys() {
        this.inputForceStateMap.clear();
        this.needsUpdate = true;
    }

    @Override
    public final void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            return;
        }
        if (!this.needsUpdate) return;

        if (isInputForcedDown(Input.CLICK_LEFT)) {
            setInputForceState(Input.CLICK_RIGHT, false);
        }

        LivingEntity entity = (LivingEntity) ctx.player();
        entity.xxa = 0.0F;  // sidewaysSpeed
        entity.zza = 0.0F;  // forwardSpeed
        entity.setShiftKeyDown(false);

        entity.setJumping(isInputForcedDown(Input.JUMP));

        if (isInputForcedDown(Input.MOVE_FORWARD)) {
            entity.zza++;
        }
        if (isInputForcedDown(Input.MOVE_BACK)) {
            entity.zza--;
        }
        if (isInputForcedDown(Input.MOVE_LEFT)) {
            entity.xxa++;
        }
        if (isInputForcedDown(Input.MOVE_RIGHT)) {
            entity.xxa--;
        }
        if (isInputForcedDown(Input.SNEAK)) {
            entity.setShiftKeyDown(true);
            entity.xxa *= 0.3F;
            entity.zza *= 0.3F;
        }

        blockBreakHelper.tick(isInputForcedDown(Input.CLICK_LEFT));
        blockPlaceHelper.tick(isInputForcedDown(Input.CLICK_RIGHT));

        this.needsUpdate = false;
    }

    public BlockBreakHelper getBlockBreakHelper() {
        return blockBreakHelper;
    }
}
