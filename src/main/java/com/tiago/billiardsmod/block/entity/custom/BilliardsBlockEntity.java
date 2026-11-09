package com.tiago.billiardsmod.block.entity.custom;

import com.tiago.billiardsmod.block.entity.ImplementedInventory;
import com.tiago.billiardsmod.block.entity.ModBlockEntities;
import com.tiago.billiardsmod.billiards.Ball;
import com.tiago.billiardsmod.billiards.BilliardsGame;
import com.tiago.billiardsmod.billiards.PlayerScore;
import com.tiago.billiardsmod.billiards.FoulReason;
import com.tiago.billiardsmod.item.PoolCueItem;
import com.tiago.billiardsmod.network.BilliardsState;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BilliardsBlockEntity extends BlockEntity implements ImplementedInventory, ExtendedScreenHandlerFactory<BilliardsScreenHandler.OpeningData> {

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);

    private final BilliardsGame game = new BilliardsGame();

    public BilliardsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BILLIARDS_BE, pos, state);
    }

    public List<Ball> getBalls() {
        return game.getBalls();
    }

    public long getGameRevision() {
        return game.getRevision();
    }

    public ItemStack takeLegacyCue() {
        ItemStack cue = removeStack(0);
        if (!cue.isEmpty()) markDirty();
        return cue;
    }

    public void joinPlayer(PlayerEntity player) {
        if (!player.isSpectator()) {
            game.joinPlayer(player.getUuid(), player.getGameProfile().getName());
            markDirty();
        }
    }

    public void leavePlayer(PlayerEntity player) {
        game.leavePlayer(player.getUuid());
        // The final departure resets the rack even if the table was already stationary.
        markDirty();
    }

    public BilliardsState createState() {
        UUID currentPlayer = game.getCurrentPlayer();
        ServerPlayerEntity active = world instanceof ServerWorld serverWorld && currentPlayer != null
                ? serverWorld.getServer().getPlayerManager().getPlayer(currentPlayer) : null;
        String name = active == null ? "" : active.getGameProfile().getName();
        return new BilliardsState(game.getBalls(), currentPlayer, name, game.getPlayerCount(),
                game.isBusy(), game.getTurnId(), game.getScores(), game.getLastFoulPlayer(), game.getLastFoulReason());
    }

    public boolean handleShoot(ServerPlayerEntity player, long turnId, float angle, float power) {
        if (world == null || world.isClient || player.isSpectator() || !canPlayerUse(player)
                || !(player.currentScreenHandler instanceof BilliardsScreenHandler handler)
                || !handler.isTable(this) || !handler.canUse(player)) {
            return false;
        }
        int usesBefore = handler.getRemainingCueUses();
        if (!handler.tryShoot(game, player, turnId, angle, power)) return false;
        if (usesBefore == 1) {
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 1f, 1f);
        }
        markDirty();
        return true;
    }

    public static void tick(World world, BlockPos pos, BlockState state, BilliardsBlockEntity blockEntity) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        for (UUID playerId : blockEntity.game.getPlayers()) {
            ServerPlayerEntity player = serverWorld.getServer().getPlayerManager().getPlayer(playerId);
            if (player == null || !player.isAlive() || player.isSpectator()
                    || !(player.currentScreenHandler instanceof BilliardsScreenHandler handler)
                    || !handler.isTable(blockEntity) || !blockEntity.canPlayerUse(player)) {
                blockEntity.game.leavePlayer(playerId);
                blockEntity.markDirty();
            }
        }
        if (blockEntity.game.tick()) blockEntity.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return world != null && !isRemoved() && player.getWorld() == world
                && world.getBlockEntity(pos) == this
                && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return slot == 0 && PoolCueItem.isUsable(stack);
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        // Insertion is manual through the slot in the opened screen.
        return false;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);
        nbt.put("Balls", writeBallsNbt());
        BilliardsGame.Progress progress = game.saveProgress();
        NbtList scoreList = new NbtList();
        for (PlayerScore score : progress.scores()) {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("Player", score.playerId());
            entry.putString("Name", score.playerName());
            entry.putInt("Points", score.points());
            scoreList.add(entry);
        }
        nbt.put("Scores", scoreList);
        if (progress.shotPlayer() != null) nbt.putUuid("ShotPlayer", progress.shotPlayer());
        nbt.putInt("ShotPoints", progress.shotPoints());
        nbt.putBoolean("CueBallFoul", progress.cueBallFoul());
        nbt.putBoolean("HitObjectBall", progress.hitObjectBall());
        if (progress.lastFoulPlayer() != null) nbt.putUuid("LastFoulPlayer", progress.lastFoulPlayer());
        nbt.putString("LastFoulReason", progress.lastFoulReason().name());
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
        if (nbt.contains("Balls")) {
            readBallsNbt(nbt.getList("Balls", 10)); // 10 = NbtCompound type
        }
        List<PlayerScore> scores = new ArrayList<>();
        NbtList scoreList = nbt.getList("Scores", 10);
        for (int i = 0; i < scoreList.size(); i++) {
            NbtCompound entry = scoreList.getCompound(i);
            if (entry.containsUuid("Player")) {
                scores.add(new PlayerScore(entry.getUuid("Player"), entry.getString("Name"), entry.getInt("Points")));
            }
        }
        UUID lastFoulPlayer = nbt.containsUuid("LastFoulPlayer") ? nbt.getUuid("LastFoulPlayer") : null;
        FoulReason reason = nbt.contains("LastFoulReason")
                ? FoulReason.fromName(nbt.getString("LastFoulReason"))
                : lastFoulPlayer == null ? FoulReason.NONE : FoulReason.CUE_BALL;
        game.loadProgress(new BilliardsGame.Progress(scores,
                nbt.containsUuid("ShotPlayer") ? nbt.getUuid("ShotPlayer") : null,
                nbt.getInt("ShotPoints"), nbt.getBoolean("CueBallFoul"), nbt.getBoolean("HitObjectBall"),
                lastFoulPlayer, reason));
    }

    private NbtList writeBallsNbt() {
        NbtList list = new NbtList();
        for (Ball ball : game.getBalls()) {
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
            game.loadBalls(loaded);
        }
    }

    @Override
    public BilliardsScreenHandler.OpeningData getScreenOpeningData(ServerPlayerEntity serverPlayerEntity) {
        return new BilliardsScreenHandler.OpeningData(pos, createState());
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.billiardsmod.billiards_block");
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new BilliardsScreenHandler(syncId, playerInventory, this);
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
