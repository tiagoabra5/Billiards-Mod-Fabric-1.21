package com.tiago.billiardsmod.item;

import com.tiago.billiardsmod.billiards.BilliardsGame;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.UUID;

public class PoolCueItem extends Item {
    public PoolCueItem(Settings settings) {
        super(settings);
    }

    public static int getMaxUses(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PoolCueItem)) return 0;
        return stack.getMaxDamage();
    }

    public static int getRemainingUses(ItemStack stack) {
        return Math.max(0, getMaxUses(stack) - stack.getDamage());
    }

    public static boolean isUsable(ItemStack stack) {
        return getRemainingUses(stack) > 0;
    }

    /** The table calls this on the server; rejected shots never wear out the cue. */
    public static boolean tryShoot(BilliardsGame game, ItemStack cue, UUID player,
                                   long turnId, float angle, float power) {
        if (!isUsable(cue) || !game.shoot(player, turnId, angle, power)) return false;

        // Consume exactly one use of the shooter's cue per accepted shot,
        // independently of the player's game mode or equipment slots.
        int damage = cue.getDamage() + 1;
        cue.setDamage(damage);
        if (damage >= cue.getMaxDamage()) {
            cue.decrement(1);
            if (!cue.isEmpty()) cue.setDamage(0);
        }
        return true;
    }
}
