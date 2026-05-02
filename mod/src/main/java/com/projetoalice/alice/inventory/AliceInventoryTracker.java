package com.projetoalice.alice.inventory;

import com.projetoalice.alice.AliceEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot of Alice's inventory and an optional tracked player's inventory.
 * Used by MaterialCalculator to determine what is available vs. missing.
 */
public class AliceInventoryTracker {

    private final List<ItemStack> aliceItems;
    private final List<ItemStack> playerItems;

    public AliceInventoryTracker(AliceEntity alice) {
        this.aliceItems = snapshot(alice.getFakePlayer().getInventory().items);
        this.playerItems = List.of();
    }

    public AliceInventoryTracker(AliceEntity alice, ServerPlayer player) {
        this.aliceItems = snapshot(alice.getFakePlayer().getInventory().items);
        this.playerItems = snapshot(player.getInventory().items);
    }

    private List<ItemStack> snapshot(List<ItemStack> source) {
        List<ItemStack> copy = new ArrayList<>(source.size());
        for (ItemStack stack : source) {
            if (!stack.isEmpty()) copy.add(stack.copy());
        }
        return copy;
    }

    /** All non-empty stacks in Alice's inventory. */
    public List<ItemStack> getAliceItems() {
        return aliceItems;
    }

    /** All non-empty stacks in the tracked player's inventory (may be empty list). */
    public List<ItemStack> getPlayerItems() {
        return playerItems;
    }
}
