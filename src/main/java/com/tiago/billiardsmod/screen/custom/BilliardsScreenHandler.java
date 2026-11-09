package com.tiago.billiardsmod.screen.custom;

import com.tiago.billiardsmod.screen.ModScreenHandlers;
import com.tiago.billiardsmod.block.entity.custom.BilliardsBlockEntity;
import com.tiago.billiardsmod.network.BilliardsState;
import com.tiago.billiardsmod.network.TableStatePacket;
import com.tiago.billiardsmod.item.PoolCueItem;
import com.tiago.billiardsmod.inventory.PlayerCueInventory;
import com.tiago.billiardsmod.billiards.BilliardsGame;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class BilliardsScreenHandler extends ScreenHandler {

    private static final int CUE_SLOT_X = 154;
    private static final int CUE_SLOT_Y = 163;
    private static final int HOTBAR_Y = 185;

    private final PlayerCueInventory inventory;
    private final BlockPos tablePos;
    @Nullable
    private final BilliardsBlockEntity table;
    @Nullable
    private final ServerPlayerEntity viewer;
    private BilliardsState state;
    private BilliardsState previousState;
    private long stateReceivedAtNanos = System.nanoTime();
    private long lastRevision = -1;
    private boolean awaitingShot;

    public record OpeningData(BlockPos tablePos, BilliardsState state) {
        public static final PacketCodec<PacketByteBuf, OpeningData> CODEC = PacketCodec.of(
                (data, buf) -> {
                    buf.writeBlockPos(data.tablePos);
                    BilliardsState.CODEC.encode(buf, data.state);
                },
                buf -> new OpeningData(buf.readBlockPos(), BilliardsState.CODEC.decode(buf))
        );
    }

    // Client: opening data contains the complete game, including a join in the middle of a shot.
    public BilliardsScreenHandler(int syncId, PlayerInventory playerInventory, OpeningData data) {
        this(syncId, playerInventory, new PlayerCueInventory(playerInventory.player.getUuid()),
                data.tablePos(), data.state(), null);
    }

    // Server: the game is shared, but each viewer has their own cue in the same UI slot.
    public BilliardsScreenHandler(int syncId, PlayerInventory playerInventory, BilliardsBlockEntity table) {
        this(syncId, playerInventory, new PlayerCueInventory(playerInventory.player.getUuid()),
                table.getPos(), table.createState(), table);
        table.joinPlayer(playerInventory.player);
        if (!playerInventory.player.isSpectator()) {
            // Preserve items left in the old shared slot when upgrading the mod.
            ItemStack legacyCue = table.takeLegacyCue();
            if (!legacyCue.isEmpty()) playerInventory.offerOrDrop(legacyCue);
        }
        state = table.createState();
        previousState = state;
    }

    private BilliardsScreenHandler(int syncId, PlayerInventory playerInventory, PlayerCueInventory inventory,
                                  BlockPos pos, BilliardsState state, @Nullable BilliardsBlockEntity table) {
        super(ModScreenHandlers.BILLIARDS_SCREEN_HANDLER, syncId);
        this.inventory = inventory;
        this.tablePos = pos.toImmutable();
        this.state = state;
        this.previousState = state;
        this.table = table;
        this.viewer = playerInventory.player instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;

        inventory.onOpen(playerInventory.player);

        this.addSlot(new PoolCueSlot(inventory, 0, CUE_SLOT_X, CUE_SLOT_Y));

        addPlayerHotbar(playerInventory);
    }

    public BlockPos getTablePos() {
        return tablePos;
    }

    public boolean isTable(BilliardsBlockEntity table) {
        return this.table == table;
    }

    public BilliardsState getState() {
        return state;
    }

    public void applyState(BilliardsState state) {
        previousState = this.state;
        this.state = state;
        stateReceivedAtNanos = System.nanoTime();
        awaitingShot = false;
    }

    public BilliardsState getPreviousState() {
        return previousState;
    }

    public float getRenderProgress() {
        return Math.min(1f, (System.nanoTime() - stateReceivedAtNanos) / 50_000_000f);
    }

    public boolean canShoot(PlayerEntity player) {
        return inventory.isOwner(player.getUuid()) && hasUsableCue()
                && !awaitingShot && !state.moving() && !player.isSpectator()
                && player.getUuid().equals(state.currentPlayer());
    }

    public boolean hasUsableCue() {
        return PoolCueItem.isUsable(inventory.getStack(0));
    }

    public int getRemainingCueUses() {
        return PoolCueItem.getRemainingUses(inventory.getStack(0));
    }

    public int getMaxCueUses() {
        return PoolCueItem.getMaxUses(inventory.getStack(0));
    }

    public boolean tryShoot(BilliardsGame game, ServerPlayerEntity player, long turnId, float angle, float power) {
        return player == viewer && inventory.tryShoot(game, player.getUuid(), turnId, angle, power);
    }

    public void markShotPending() {
        awaitingShot = true;
    }

    public void shoot(ServerPlayerEntity player, long turnId, float angle, float power) {
        if (table != null && player == viewer) {
            table.handleShoot(player, turnId, angle, power);
            // Also acknowledge a rejected request, so the client can leave its pending state.
            lastRevision = -1;
            sendContentUpdates();
        }
    }

    @Override
    public void sendContentUpdates() {
        super.sendContentUpdates();
        if (table != null && viewer != null && viewer.currentScreenHandler == this
                && lastRevision != table.getGameRevision()) {
            state = table.createState();
            ServerPlayNetworking.send(viewer, new TableStatePacket(syncId, new OpeningData(tablePos, state)));
            lastRevision = table.getGameRevision();
        }
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
        if (table != null && player == viewer) {
            dropInventory(player, inventory);
            table.leavePlayer(player);
        }
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
        if (!inventory.isOwner(player.getUuid()) || invSlot < 0 || invSlot >= slots.size()) return ItemStack.EMPTY;
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
        return inventory.isOwner(player.getUuid()) && (table == null || table.canPlayerUse(player));
    }

    private void addPlayerHotbar(PlayerInventory playerInventory){
        for (int i=0; i<9; ++i){
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, HOTBAR_Y));
        }
    }
}
