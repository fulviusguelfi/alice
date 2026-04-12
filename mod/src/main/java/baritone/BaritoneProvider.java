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

package baritone;

import baritone.api.IBaritone;
import baritone.api.IBaritoneProvider;
import baritone.api.cache.IWorldScanner;
import baritone.api.command.ICommandSystem;
import baritone.api.schematic.ISchematicSystem;
import baritone.cache.FasterWorldScanner;
import baritone.command.CommandSystem;
import baritone.utils.schematic.SchematicSystem;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-side Baritone provider. Each ServerPlayer (FakePlayer) gets its own Baritone instance,
 * looked up by UUID. There is no "primary" baritone on the server.
 *
 * @author Brady (original), Alice Project (server-side port)
 * @since 9/29/2018
 */
public final class BaritoneProvider implements IBaritoneProvider {

    private final List<IBaritone> all;
    private final List<IBaritone> allView;
    private final Map<UUID, Baritone> byPlayer;

    public BaritoneProvider() {
        this.all = new CopyOnWriteArrayList<>();
        this.allView = Collections.unmodifiableList(this.all);
        this.byPlayer = new ConcurrentHashMap<>();
    }

    @Override
    public IBaritone getPrimaryBaritone() {
        if (this.all.isEmpty()) {
            throw new IllegalStateException("[Alice] No baritone instances registered. Server-side provider has no primary.");
        }
        return this.all.get(0);
    }

    @Override
    public List<IBaritone> getAllBaritones() {
        return this.allView;
    }

    public synchronized Baritone getOrCreateBaritone(ServerPlayer player) {
        return this.byPlayer.computeIfAbsent(player.getUUID(), uuid -> {
            Baritone b = new Baritone(player);
            this.all.add(b);
            return b;
        });
    }

    public synchronized boolean destroyBaritone(ServerPlayer player) {
        Baritone removed = this.byPlayer.remove(player.getUUID());
        if (removed != null) {
            this.all.remove(removed);
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean destroyBaritone(IBaritone baritone) {
        if (this.all.remove(baritone)) {
            if (baritone instanceof Baritone b) {
                this.byPlayer.remove(b.getPlayer().getUUID());
            }
            return true;
        }
        return false;
    }

    @Override
    public IWorldScanner getWorldScanner() {
        return FasterWorldScanner.INSTANCE;
    }

    @Override
    public ICommandSystem getCommandSystem() {
        return CommandSystem.INSTANCE;
    }

    @Override
    public ISchematicSystem getSchematicSystem() {
        return SchematicSystem.INSTANCE;
    }
}
