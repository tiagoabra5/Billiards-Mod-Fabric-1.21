package com.tiago.billiardsmod.item;

import com.tiago.billiardsmod.BilliardsMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item POOL_CUE = registerItem("pool_cue", new Item(new Item.Settings()));

    private static Item registerItem(String name, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(BilliardsMod.MOD_ID, name), item);
    }

    public static void registerModItems(){
        BilliardsMod.LOGGER.info("Registering Mod Items for " + BilliardsMod.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(fabricItemGroupEntries -> {
            fabricItemGroupEntries.add(POOL_CUE);
        });
    }
}
