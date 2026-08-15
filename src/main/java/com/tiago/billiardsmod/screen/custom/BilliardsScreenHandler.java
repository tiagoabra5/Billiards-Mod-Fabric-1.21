package com.tiago.billiardsmod.screen.custom;

import com.tiago.billiardsmod.screen.ModScreenHandlers;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

public class BilliardsScreenHandler extends ScreenHandler {

    private static final int CUE_SLOT_X = 154;
    private static final int CUE_SLOT_Y = 163;
    private static final int HOTBAR_Y = 185;

    private final Inventory inventory;

    public BilliardsScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, playerInventory.player.getWorld().getBlockEntity(pos));
    }

    public BilliardsScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity){
        super(ModScreenHandlers.BILLIARDS_SCREEN_HANDLER, syncId);
        this.inventory = ((Inventory) blockEntity);

        this.addSlot(new Slot(inventory, 0, CUE_SLOT_X, CUE_SLOT_Y));

        addPlayerHotbar(playerInventory);
    }

    public int getCueSlotY() {
        return CUE_SLOT_Y;
    }

    public int getCueSlotX(){
        return CUE_SLOT_X;
    }

    //shift + left click - manda o item para o slot da tela
    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.inventory.size(), false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    private void addPlayerHotbar(PlayerInventory playerInventory){
        for (int i=0; i<9; ++i){
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, HOTBAR_Y));
        }
    }
}