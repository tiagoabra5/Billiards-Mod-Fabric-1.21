package com.tiago.billiardsmod.screen.custom;

import com.tiago.billiardsmod.item.PoolCueItem;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class PoolCueSlot extends Slot {
    public PoolCueSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return PoolCueItem.isUsable(stack);
    }

    @Override
    public int getMaxItemCount() {
        return 1;
    }
}
