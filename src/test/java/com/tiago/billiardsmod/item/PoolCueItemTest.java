package com.tiago.billiardsmod.item;

import com.tiago.billiardsmod.billiards.BilliardsGame;
import com.tiago.billiardsmod.screen.custom.PoolCueSlot;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PoolCueItemTest {
    private static Item durableCue;
    private static Item shortCue;
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    @BeforeAll
    static void initializeMinecraft() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        durableCue = Registry.register(Registries.ITEM, Identifier.of("billiardsmod_test", "durable_cue"),
                new PoolCueItem(new Item.Settings().maxDamage(60)));
        shortCue = Registry.register(Registries.ITEM, Identifier.of("billiardsmod_test", "short_cue"),
                new PoolCueItem(new Item.Settings().maxDamage(3)));
    }

    private BilliardsGame game() {
        BilliardsGame game = new BilliardsGame();
        game.joinPlayer(first, "PlayerOne");
        game.joinPlayer(second, "PlayerTwo");
        return game;
    }

    private static void settle(BilliardsGame game) {
        for (int tick = 0; tick < 2000 && game.isBusy(); tick++) game.tick();
        assertFalse(game.isBusy());
    }

    private static boolean shoot(BilliardsGame game, ItemStack cue) {
        return PoolCueItem.tryShoot(game, cue, game.getCurrentPlayer(), game.getTurnId(), 0f, 0.01f);
    }

    @Test
    void theRegisteredCueHasTwentyFiveUsesAndDoesNotStack() {
        ItemStack cue = new ItemStack(ModItems.POOL_CUE);
        assertTrue(cue.isDamageable());
        assertEquals(25, cue.getMaxDamage());
        assertEquals(25, PoolCueItem.getRemainingUses(cue));
        assertEquals(1, cue.getMaxCount());
    }

    @Test
    void anEmptyOrWrongSlotCannotStartAShot() {
        BilliardsGame game = game();
        long turn = game.getTurnId();
        for (ItemStack wrong : new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.STICK),
                new ItemStack(Items.WOODEN_SWORD), new ItemStack(ModItems.EIGHT_BALL)}) {
            assertFalse(shoot(game, wrong));
            assertFalse(game.isBusy());
            assertEquals(turn, game.getTurnId());
        }
    }

    @Test
    void eachAcceptedShotUsesExactlyOneDurability() {
        BilliardsGame game = game();
        ItemStack cue = new ItemStack(ModItems.POOL_CUE);
        assertTrue(shoot(game, cue));
        assertEquals(24, PoolCueItem.getRemainingUses(cue));
        settle(game);
        assertEquals(24, PoolCueItem.getRemainingUses(cue));
        assertTrue(shoot(game, cue));
        assertEquals(23, PoolCueItem.getRemainingUses(cue));
    }

    @Test
    void rejectedOrDuplicateShotsDoNotConsumeDurability() {
        BilliardsGame game = game();
        ItemStack cue = new ItemStack(ModItems.POOL_CUE);
        long turn = game.getTurnId();
        assertFalse(PoolCueItem.tryShoot(game, cue, second, turn, 0f, 0.1f));
        assertFalse(PoolCueItem.tryShoot(game, cue, first, turn - 1, 0f, 0.1f));
        assertFalse(PoolCueItem.tryShoot(game, cue, first, turn, Float.NaN, 0.1f));
        assertFalse(PoolCueItem.tryShoot(game, cue, first, turn, 0f, 0f));
        assertEquals(25, PoolCueItem.getRemainingUses(cue));
        assertTrue(shoot(game, cue));
        assertFalse(PoolCueItem.tryShoot(game, cue, first, turn, 0f, 0.1f));
        assertFalse(PoolCueItem.tryShoot(game, cue, second, turn, 0f, 0.1f));
        assertEquals(24, PoolCueItem.getRemainingUses(cue));
    }

    @Test
    void theTwentyFifthShotWorksAndBreaksTheCue() {
        BilliardsGame game = game();
        ItemStack cue = new ItemStack(ModItems.POOL_CUE);
        for (int shot = 1; shot <= 25; shot++) {
            assertTrue(shoot(game, cue), "Shot " + shot + " should be accepted");
            if (shot < 25) {
                assertFalse(cue.isEmpty());
                assertEquals(25 - shot, PoolCueItem.getRemainingUses(cue));
            }
            settle(game);
        }
        assertTrue(cue.isEmpty());
        assertFalse(PoolCueItem.isUsable(cue));
        long turn = game.getTurnId();
        assertFalse(shoot(game, cue));
        assertEquals(turn, game.getTurnId());
        assertFalse(game.isBusy());
    }

    @Test
    void replacingABrokenCueContinuesTheSameGameAndTurn() {
        BilliardsGame game = game();
        ItemStack oldCue = new ItemStack(ModItems.POOL_CUE);
        oldCue.setDamage(24);
        assertTrue(shoot(game, oldCue));
        assertTrue(oldCue.isEmpty());
        settle(game);
        long turn = game.getTurnId();
        UUID nextPlayer = game.getCurrentPlayer();
        float whiteX = game.getBalls().getLast().x;
        assertFalse(shoot(game, oldCue));
        ItemStack replacement = new ItemStack(ModItems.POOL_CUE);
        assertEquals(turn, game.getTurnId());
        assertEquals(whiteX, game.getBalls().getLast().x);
        assertEquals(nextPlayer, game.getCurrentPlayer());
        assertTrue(shoot(game, replacement));
        assertEquals(24, PoolCueItem.getRemainingUses(replacement));
    }

    @Test
    void insertingRemovingAndRestartingTheGameDoesNotRepairTheCue() {
        BilliardsGame game = game();
        ItemStack cue = new ItemStack(ModItems.POOL_CUE);
        SimpleInventory inventory = new SimpleInventory(1);
        inventory.setStack(0, cue);
        assertEquals(25, PoolCueItem.getRemainingUses(inventory.getStack(0)));
        assertTrue(shoot(game, inventory.getStack(0)));
        settle(game);
        ItemStack removed = inventory.removeStack(0);
        assertEquals(24, PoolCueItem.getRemainingUses(removed));
        inventory.setStack(0, removed);
        game.leavePlayer(first);
        game.leavePlayer(second);
        assertEquals(24, PoolCueItem.getRemainingUses(inventory.getStack(0)));
        assertTrue(game.getScores().isEmpty());
        game.joinPlayer(first, "PlayerOne");
        assertTrue(shoot(game, inventory.getStack(0)));
        assertEquals(23, PoolCueItem.getRemainingUses(inventory.getStack(0)));
    }

    @Test
    void theCueSlotOnlyAcceptsOneUsablePoolCue() {
        PoolCueSlot slot = new PoolCueSlot(new SimpleInventory(1), 0, 0, 0);
        assertTrue(slot.canInsert(new ItemStack(ModItems.POOL_CUE)));
        assertFalse(slot.canInsert(new ItemStack(Items.STICK)));
        assertFalse(slot.canInsert(new ItemStack(Items.WOODEN_SWORD)));
        assertFalse(slot.canInsert(new ItemStack(ModItems.EIGHT_BALL)));
        assertEquals(1, slot.getMaxItemCount());
        ItemStack exhausted = new ItemStack(ModItems.POOL_CUE);
        exhausted.setDamage(25);
        assertFalse(slot.canInsert(exhausted));
        assertFalse(shoot(game(), exhausted));
    }

    @Test
    void eachCueTypeUsesItsOwnConfiguredMaximum() {
        PoolCueSlot slot = new PoolCueSlot(new SimpleInventory(1), 0, 0, 0);
        ItemStack standard = new ItemStack(ModItems.POOL_CUE);
        ItemStack durable = new ItemStack(durableCue);
        ItemStack shortLived = new ItemStack(shortCue);
        assertEquals(25, PoolCueItem.getMaxUses(standard));
        assertEquals(60, PoolCueItem.getMaxUses(durable));
        assertEquals(3, PoolCueItem.getMaxUses(shortLived));
        assertTrue(slot.canInsert(durable));
        assertTrue(slot.canInsert(shortLived));
        durable.setDamage(7);
        assertEquals(53, PoolCueItem.getRemainingUses(durable));
        assertEquals(60, PoolCueItem.getMaxUses(durable));
    }

    @Test
    void aLongerLastingCueWorksPastTwentyFiveShotsAndBreaksAtItsOwnLimit() {
        BilliardsGame game = game();
        ItemStack cue = new ItemStack(durableCue);
        for (int shot = 1; shot <= 60; shot++) {
            assertTrue(shoot(game, cue), "Shot " + shot + " should use this cue's own durability");
            if (shot < 60) {
                assertFalse(cue.isEmpty());
                assertEquals(60 - shot, PoolCueItem.getRemainingUses(cue));
                assertEquals(60, PoolCueItem.getMaxUses(cue));
            }
            settle(game);
        }
        assertTrue(cue.isEmpty());
        assertFalse(shoot(game, cue));
    }

    @Test
    void aShorterLastingCueBreaksAtItsOwnLimit() {
        BilliardsGame game = game();
        ItemStack cue = new ItemStack(shortCue);
        for (int shot = 1; shot <= 3; shot++) {
            assertTrue(shoot(game, cue));
            if (shot < 3) assertEquals(3 - shot, PoolCueItem.getRemainingUses(cue));
            settle(game);
        }
        assertTrue(cue.isEmpty());
        assertFalse(shoot(game, cue));
    }

    @Test
    void anEmptyOrWrongSlotHasNoMaximumCueDurability() {
        for (ItemStack stack : new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.STICK),
                new ItemStack(Items.WOODEN_SWORD)}) {
            assertEquals(0, PoolCueItem.getMaxUses(stack));
            assertEquals(0, PoolCueItem.getRemainingUses(stack));
        }
    }
}
