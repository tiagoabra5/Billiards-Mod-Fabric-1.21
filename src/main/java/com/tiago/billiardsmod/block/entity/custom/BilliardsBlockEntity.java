package com.tiago.billiardsmod.block.entity.custom;

import com.tiago.billiardsmod.block.entity.ImplementedInventory;
import com.tiago.billiardsmod.block.entity.ModBlockEntities;
import com.tiago.billiardsmod.billiards.Ball;
import com.tiago.billiardsmod.billiards.BallRack;
import com.tiago.billiardsmod.billiards.BilliardsPhysics;
import com.tiago.billiardsmod.billiards.TableBounds;
import com.tiago.billiardsmod.screen.custom.BilliardsScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BilliardsBlockEntity extends BlockEntity implements ImplementedInventory, ExtendedScreenHandlerFactory<BlockPos> {

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);

    public static final float TABLE_WIDTH = 130f;
    public static final float TABLE_HEIGHT = 160f;
    public static final float BALL_RADIUS = 4f;
    public static final float POCKET_CAPTURE_RADIUS = 9f;
    private static final int RACK_ROWS = 5;

    private List<Ball> balls;

    public BilliardsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BILLIARDS_BE, pos, state);
        this.balls = createInitialRack();
    }

    private List<Ball> createInitialRack() {
        float spacing = BALL_RADIUS * 2f + 0.5f;
        float triangleHeight = (RACK_ROWS - 1) * spacing * 0.87f;
        float footSpotY = BALL_RADIUS + 4 + triangleHeight;
        float headSpotY = TABLE_HEIGHT - 20;

        return BallRack.createStandardRack(
                TABLE_WIDTH / 2f,
                footSpotY,
                headSpotY,
                BALL_RADIUS
        );
    }

    private TableBounds getLogicalBounds() {
        return new TableBounds(0f, 0f, TABLE_WIDTH, TABLE_HEIGHT);
    }

    private List<float[]> getLogicalPocketPositions() {
        float midY = TABLE_HEIGHT / 2f;
        return List.of(
                new float[]{0f, 0f},
                new float[]{0f, midY},
                new float[]{0f, TABLE_HEIGHT},
                new float[]{TABLE_WIDTH, 0f},
                new float[]{TABLE_WIDTH, midY},
                new float[]{TABLE_WIDTH, TABLE_HEIGHT}
        );
    }

    public List<Ball> getBalls() {
        return balls;
    }

    public void tick(World world, BlockPos pos, BlockState state) {
        if (world.isClient) return;

        boolean wasMoving = BilliardsPhysics.areBallsMoving(balls);

        BilliardsPhysics.update(balls, getLogicalBounds(), getLogicalPocketPositions(), POCKET_CAPTURE_RADIUS);

        boolean isMoving = BilliardsPhysics.areBallsMoving(balls);

        if (wasMoving || isMoving) {
            markDirty();
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                serverWorld.getChunkManager().markForUpdate(pos);
            }
        }
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);
        nbt.put("Balls", writeBallsNbt());
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
        if (nbt.contains("Balls")) {
            readBallsNbt(nbt.getList("Balls", 10)); // 10 = NbtCompound type
        }
    }

    private NbtList writeBallsNbt() {
        NbtList list = new NbtList();
        for (Ball ball : balls) {
            NbtCompound ballNbt = new NbtCompound();
            ballNbt.putInt("Number", ball.number);
            ballNbt.putFloat("X", ball.x);
            ballNbt.putFloat("Y", ball.y);
            ballNbt.putFloat("Vx", ball.vx);
            ballNbt.putFloat("Vy", ball.vy);
            ballNbt.putFloat("Radius", ball.radius);
            ballNbt.putBoolean("Pocketed", ball.pocketed);
            list.add(ballNbt);
        }
        return list;
    }

    private void readBallsNbt(NbtList list) {
        List<Ball> loaded = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            NbtCompound ballNbt = list.getCompound(i);
            Ball ball = new Ball(
                    ballNbt.getInt("Number"),
                    ballNbt.getFloat("X"),
                    ballNbt.getFloat("Y"),
                    ballNbt.getFloat("Radius")
            );
            ball.vx = ballNbt.getFloat("Vx");
            ball.vy = ballNbt.getFloat("Vy");
            ball.pocketed = ballNbt.getBoolean("Pocketed");
            loaded.add(ball);
        }
        if (!loaded.isEmpty()) {
            this.balls = loaded;
        }
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity serverPlayerEntity) {
        return this.pos;
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Billiards");
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new BilliardsScreenHandler(syncId, playerInventory, this.pos);
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }
}