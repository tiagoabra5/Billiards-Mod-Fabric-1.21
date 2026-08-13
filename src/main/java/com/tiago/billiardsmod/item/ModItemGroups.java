package com.tiago.billiardsmod.item;

import com.tiago.billiardsmod.BilliardsMod;
import com.tiago.billiardsmod.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {

    //deixei dentro do mesmo grupo tanto os blocos quanto os itens por enquanto
    public static final ItemGroup BILLIARDS_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(BilliardsMod.MOD_ID, "billiards_items"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.POOL_CUE))
                    .displayName(Text.translatable("itemgroup.billiardsmod.billiards_items"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.POOL_CUE);
                        entries.add(ModBlocks.BILLIARDS_TABLE_BLOCK);
                    }).build());

    public static void registerItemGroups(){
        BilliardsMod.LOGGER.info("Registering Item Groups for " + BilliardsMod.MOD_ID);
    }
}
