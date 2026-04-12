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

package baritone.api;

import baritone.api.cache.IWorldScanner;
import baritone.api.command.ICommand;
import baritone.api.command.ICommandSystem;
import baritone.api.schematic.ISchematicSystem;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Objects;

/**
 * Provides the present {@link IBaritone} instances, as well as non-baritone instance related APIs.
 * Server-side port: no Minecraft client references.
 *
 * @author leijurv (original), Alice Project (server-side port)
 */
public interface IBaritoneProvider {

    /**
     * Returns the primary {@link IBaritone} instance.
     * On the server, this is the first registered instance.
     *
     * @return The primary {@link IBaritone} instance.
     */
    IBaritone getPrimaryBaritone();

    /**
     * Returns all of the active {@link IBaritone} instances.
     *
     * @return All active {@link IBaritone} instances.
     */
    List<IBaritone> getAllBaritones();

    /**
     * Provides the {@link IBaritone} instance for a given {@link Player}.
     *
     * @param player The player
     * @return The {@link IBaritone} instance, or null if not found.
     */
    default IBaritone getBaritoneForPlayer(Player player) {
        for (IBaritone baritone : this.getAllBaritones()) {
            if (Objects.equals(player, baritone.getPlayerContext().player())) {
                return baritone;
            }
        }
        return null;
    }

    /**
     * Destroys and removes the specified {@link IBaritone} instance.
     *
     * @param baritone The baritone instance to remove
     * @return Whether the baritone instance was removed
     */
    boolean destroyBaritone(IBaritone baritone);

    /**
     * Returns the {@link IWorldScanner} instance.
     *
     * @return The {@link IWorldScanner} instance.
     */
    IWorldScanner getWorldScanner();

    /**
     * Returns the {@link ICommandSystem} instance.
     *
     * @return The {@link ICommandSystem} instance.
     */
    ICommandSystem getCommandSystem();

    /**
     * @return The {@link ISchematicSystem} instance.
     */
    ISchematicSystem getSchematicSystem();
}
