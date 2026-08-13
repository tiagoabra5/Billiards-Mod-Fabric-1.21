package com.tiago.billiardsmod.block.entity;

import com.tiago.billiardsmod.BilliardsMod;
import com.tiago.billiardsmod.block.ModBlocks;
import com.tiago.billiardsmod.block.entity.custom.BilliardsBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static final BlockEntityType<BilliardsBlockEntity> BILLIARDS_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(BilliardsMod.MOD_ID, "billiards_be"),
                    BlockEntityType.Builder.create(BilliardsBlockEntity::new, ModBlocks.BILLIARDS_BLOCK).build(null));

    public static void registerBlockEntities() {
        BilliardsMod.LOGGER.info("Registering Block Entities for " + BilliardsMod.MOD_ID);
    }
}
