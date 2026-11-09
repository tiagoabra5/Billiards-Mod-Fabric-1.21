package com.tiago.billiardsmod;

import com.tiago.billiardsmod.block.ModBlocks;
import com.tiago.billiardsmod.block.entity.ModBlockEntities;
import com.tiago.billiardsmod.item.ModItemGroups;
import com.tiago.billiardsmod.item.ModItems;
import com.tiago.billiardsmod.screen.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import com.tiago.billiardsmod.network.ShootPacket;
import com.tiago.billiardsmod.network.TableStatePacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.tiago.billiardsmod.screen.custom.BilliardsScreenHandler;

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

        PayloadTypeRegistry.playC2S().register(ShootPacket.ID, ShootPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(TableStatePacket.ID, TableStatePacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ShootPacket.ID, (payload, context) -> {
            var player = context.player();
            if (player.currentScreenHandler instanceof BilliardsScreenHandler handler
                    && handler.syncId == payload.syncId() && handler.getTablePos().equals(payload.tablePos())
                    && handler.canUse(player)) {
                handler.shoot(player, payload.turnId(), payload.angle(), payload.power());
            }
        });
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
