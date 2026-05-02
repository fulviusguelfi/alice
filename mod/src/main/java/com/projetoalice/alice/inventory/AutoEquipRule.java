package com.projetoalice.alice.inventory;

import com.mojang.logging.LogUtils;
import com.projetoalice.alice.AliceEntity;
import com.projetoalice.alice.AliceMod;
import com.projetoalice.alice.BehaviorJournal;
import com.projetoalice.alice.Config;
import com.projetoalice.alice.IAliceRule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import org.slf4j.Logger;

/**
 * P32/P33 — Auto-equip rule: every 20 ticks compare armor defense and weapon damage,
 * swapping in better items from Alice's main inventory.
 * Priority 32 (lower = higher priority, after combat rules).
 */
public class AutoEquipRule implements IAliceRule {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CHECK_INTERVAL_TICKS = 20;

    private int tickCounter = 0;

    @Override
    public String name() {
        return "AutoEquip";
    }

    @Override
    public int priority() {
        return 32;
    }

    @Override
    public boolean isToggleable() {
        return true;
    }

    @Override
    public boolean shouldApply(AliceEntity alice, ServerLevel level) {
        if (!Config.autoEquip) return false;
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL_TICKS) return false;
        tickCounter = 0;
        // Only apply if there's something to improve
        return hasImprovement(alice);
    }

    @Override
    public void execute(AliceEntity alice, ServerLevel level) {
        var fp = alice.getFakePlayer();
        var inv = fp.getInventory();

        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) continue;

            EquipmentSlot equipSlot = getEquipmentSlot(stack);
            if (equipSlot == null) continue;

            ItemStack current = fp.getItemBySlot(equipSlot);
            if (isBetter(stack, current, equipSlot)) {
                // Swap: move current back to inventory, equip new one
                inv.setItem(slot, current.isEmpty() ? ItemStack.EMPTY : current.copy());
                fp.setItemSlot(equipSlot, stack.copy());
                LOGGER.info("[Alice][AutoEquip] Equipped {} in slot {}",
                        stack.getItem().getDescriptionId(), equipSlot.getName());
                AliceMod.JOURNAL.record(BehaviorJournal.Type.ACTION,
                        "equipou " + stack.getItem().getDescriptionId(),
                        "slot " + equipSlot.getName());
            }
        }
    }

    private boolean hasImprovement(AliceEntity alice) {
        var fp = alice.getFakePlayer();
        var inv = fp.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) continue;
            EquipmentSlot equipSlot = getEquipmentSlot(stack);
            if (equipSlot == null) continue;
            ItemStack current = fp.getItemBySlot(equipSlot);
            if (isBetter(stack, current, equipSlot)) return true;
        }
        return false;
    }

    private EquipmentSlot getEquipmentSlot(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ArmorItem armorItem) {
            return armorItem.getEquipmentSlot();
        }
        if (item instanceof SwordItem || item instanceof AxeItem) {
            return EquipmentSlot.MAINHAND;
        }
        return null;
    }

    /**
     * Returns true if candidate is strictly better than current.
     */
    private boolean isBetter(ItemStack candidate, ItemStack current, EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            double candidateDmg = getAttackDamage(candidate);
            double currentDmg = current.isEmpty() ? 0.0 : getAttackDamage(current);
            return candidateDmg > currentDmg;
        }
        // Armor slots: compare defense value
        int candidateDef = getArmorDefense(candidate);
        int currentDef = current.isEmpty() ? 0 : getArmorDefense(current);
        return candidateDef > currentDef;
    }

    private double getAttackDamage(ItemStack stack) {
        var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        var list = modifiers.get(Attributes.ATTACK_DAMAGE);
        double bonus = 0;
        for (var m : list) bonus += m.getAmount();
        return bonus;
    }

    private int getArmorDefense(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem armorItem) {
            return armorItem.getDefense();
        }
        return 0;
    }
}
