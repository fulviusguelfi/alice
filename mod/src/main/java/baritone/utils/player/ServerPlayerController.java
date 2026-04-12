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

package baritone.utils.player;

import baritone.api.utils.IPlayerController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Server-side implementation of {@link IPlayerController} that operates directly
 * on ServerPlayerGameMode instead of the client-side MultiPlayerGameMode.
 */
public final class ServerPlayerController implements IPlayerController {

    private final ServerPlayer player;

    public ServerPlayerController(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void syncHeldItem() {
        // NO-OP server-side: held item is already authoritative on the server
    }

    @Override
    public boolean hasBrokenBlock() {
        // Check if the game mode is not currently mining (i.e., previous break completed)
        return !player.gameMode.isDestroyingBlock;
    }

    @Override
    public boolean onPlayerDamageBlock(BlockPos pos, Direction side) {
        if (player.gameMode.isDestroyingBlock) {
            player.gameMode.handleBlockBreakAction(
                    pos,
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    side,
                    player.level().getMaxBuildHeight(),
                    -1
            );
            return true;
        }
        return false;
    }

    @Override
    public void resetBlockRemoving() {
        if (player.gameMode.isDestroyingBlock) {
            player.gameMode.handleBlockBreakAction(
                    player.gameMode.destroyPos,
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                    Direction.UP,
                    player.level().getMaxBuildHeight(),
                    -1
            );
        }
    }

    @Override
    public void windowClick(int windowId, int slotId, int mouseButton, ClickType type, Player p) {
        player.containerMenu.clicked(slotId, mouseButton, type, player);
    }

    @Override
    public GameType getGameType() {
        return player.gameMode.getGameModeForPlayer();
    }

    @Override
    public InteractionResult processRightClickBlock(Player p, Level world, InteractionHand hand, BlockHitResult result) {
        return player.gameMode.useItemOn(player, player.level(), player.getItemInHand(hand), hand, result);
    }

    @Override
    public InteractionResult processRightClick(Player p, Level world, InteractionHand hand) {
        return player.gameMode.useItem(player, player.level(), player.getItemInHand(hand), hand);
    }

    @Override
    public boolean clickBlock(BlockPos loc, Direction face) {
        if (player.level().getBlockState(loc).isAir()) return false;
        player.gameMode.handleBlockBreakAction(
                loc,
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                face,
                player.level().getMaxBuildHeight(),
                -1
        );
        return player.gameMode.isDestroyingBlock || player.level().getBlockState(loc).isAir();
    }

    @Override
    public void setHittingBlock(boolean hittingBlock) {
        // NO-OP server-side
    }
}
