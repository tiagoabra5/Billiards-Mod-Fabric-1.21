package com.tiago.billiardsmod.inventory;

import com.tiago.billiardsmod.billiards.BilliardsGame;
import com.tiago.billiardsmod.item.ModItems;
import com.tiago.billiardsmod.item.PoolCueItem;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerCueInventoryTest {
    private static Item secondCueType;
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    @BeforeAll
    static void initializeMinecraft() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        secondCueType = Registry.register(Registries.ITEM, Identifier.of("billiardsmod_test", "player_two_cue"),
                new PoolCueItem(new Item.Settings().maxDamage(60)));
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

    private static boolean shoot(PlayerCueInventory inventory, BilliardsGame game, UUID player) {
        return inventory.tryShoot(game, player, game.getTurnId(), 0f, 0.01f);
    }

    @Test
    void bothPlayersHaveOneIndependentSlotWithTheirOwnDurability() {
        PlayerCueInventory firstInventory = new PlayerCueInventory(first);
        PlayerCueInventory secondInventory = new PlayerCueInventory(second);
        firstInventory.setStack(0, new ItemStack(ModItems.POOL_CUE));
        assertTrue(secondInventory.getStack(0).isEmpty());
        secondInventory.setStack(0, new ItemStack(secondCueType));
        assertEquals(1, firstInventory.size());
        assertEquals(1, secondInventory.size());
        assertEquals(25, PoolCueItem.getMaxUses(firstInventory.getStack(0)));
        assertEquals(60, PoolCueItem.getMaxUses(secondInventory.getStack(0)));
        assertNotSame(firstInventory.getStack(0), secondInventory.getStack(0));
    }

    @Test
    void eachTurnOnlyWearsOutTheShootersOwnCueInTheSharedGame() {
        BilliardsGame game = game();
        PlayerCueInventory firstInventory = new PlayerCueInventory(first);
        PlayerCueInventory secondInventory = new PlayerCueInventory(second);
        firstInventory.setStack(0, new ItemStack(ModItems.POOL_CUE));
        secondInventory.setStack(0, new ItemStack(secondCueType));
        assertTrue(shoot(firstInventory, game, first));
        assertEquals(24, PoolCueItem.getRemainingUses(firstInventory.getStack(0)));
        assertEquals(60, PoolCueItem.getRemainingUses(secondInventory.getStack(0)));
        settle(game);
        assertEquals(second, game.getCurrentPlayer());
        assertTrue(shoot(secondInventory, game, second));
        assertEquals(24, PoolCueItem.getRemainingUses(firstInventory.getStack(0)));
        assertEquals(59, PoolCueItem.getRemainingUses(secondInventory.getStack(0)));
    }

    @Test
    void theCurrentPlayerCannotUseTheOtherPlayersCueInventory() {
        BilliardsGame game = game();
        PlayerCueInventory otherInventory = new PlayerCueInventory(second);
        otherInventory.setStack(0, new ItemStack(secondCueType));
        assertEquals(first, game.getCurrentPlayer());
        assertFalse(shoot(otherInventory, game, first));
        assertFalse(game.isBusy());
        assertEquals(60, PoolCueItem.getRemainingUses(otherInventory.getStack(0)));
    }

    @Test
    void havingACueInTheOtherSlotDoesNotLetAnEmptySlotShoot() {
        BilliardsGame game = game();
        PlayerCueInventory firstInventory = new PlayerCueInventory(first);
        PlayerCueInventory secondInventory = new PlayerCueInventory(second);
        secondInventory.setStack(0, new ItemStack(secondCueType));
        assertFalse(shoot(firstInventory, game, first));
        assertFalse(game.isBusy());
        assertEquals(first, game.getCurrentPlayer());
        assertEquals(60, PoolCueItem.getRemainingUses(secondInventory.getStack(0)));
    }

    @Test
    void breakingAndReplacingOnePlayersCuePreservesTheOthersCueAndTheGame() {
        BilliardsGame game = game();
        PlayerCueInventory firstInventory = new PlayerCueInventory(first);
        PlayerCueInventory secondInventory = new PlayerCueInventory(second);
        ItemStack worn = new ItemStack(ModItems.POOL_CUE);
        worn.setDamage(24);
        firstInventory.setStack(0, worn);
        secondInventory.setStack(0, new ItemStack(secondCueType));
        assertTrue(shoot(firstInventory, game, first));
        assertTrue(firstInventory.getStack(0).isEmpty());
        assertEquals(60, PoolCueItem.getRemainingUses(secondInventory.getStack(0)));
        settle(game);
        assertTrue(shoot(secondInventory, game, second));
        settle(game);
        assertEquals(first, game.getCurrentPlayer());
        long turn = game.getTurnId();
        assertFalse(shoot(firstInventory, game, first));
        firstInventory.setStack(0, new ItemStack(ModItems.POOL_CUE));
        assertEquals(turn, game.getTurnId());
        assertEquals(2, game.getPlayerCount());
        assertTrue(shoot(firstInventory, game, first));
        assertEquals(24, PoolCueItem.getRemainingUses(firstInventory.getStack(0)));
        assertEquals(59, PoolCueItem.getRemainingUses(secondInventory.getStack(0)));
    }
}
