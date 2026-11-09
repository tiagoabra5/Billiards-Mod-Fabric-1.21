package com.tiago.billiardsmod.inventory;

import com.tiago.billiardsmod.billiards.BilliardsGame;
import com.tiago.billiardsmod.item.PoolCueItem;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;

import java.util.UUID;

/** One private cue slot for the player using a table screen. */
public class PlayerCueInventory extends SimpleInventory {
    private final UUID owner;

    public PlayerCueInventory(UUID owner) {
        super(1);
        this.owner = owner;
    }

    public boolean isOwner(UUID player) {
        return owner.equals(player);
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return slot == 0 && PoolCueItem.isUsable(stack);
    }

    public boolean tryShoot(BilliardsGame game, UUID player, long turnId, float angle, float power) {
        if (!isOwner(player) || !PoolCueItem.tryShoot(game, getStack(0), player, turnId, angle, power)) {
            return false;
        }
        if (getStack(0).isEmpty()) setStack(0, ItemStack.EMPTY);
        markDirty();
        return true;
    }
}
