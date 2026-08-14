package com.tiago.billiardsmod;

import com.tiago.billiardsmod.block.ModBlocks;
import com.tiago.billiardsmod.block.entity.ModBlockEntities;
import com.tiago.billiardsmod.item.ModItemGroups;
import com.tiago.billiardsmod.item.ModItems;
import com.tiago.billiardsmod.screen.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BilliardsMod implements ModInitializer {
	public static final String MOD_ID = "billiardsmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
        ModItemGroups.registerItemGroups();

        ModItems.registerModItems();
        ModBlocks.registerBlocks();

        ModBlockEntities.registerBlockEntities();
        ModScreenHandlers.registerScreenHandlers();
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
