package com.tiago.billiardsmod;

import com.tiago.billiardsmod.screen.ModScreenHandlers;
import com.tiago.billiardsmod.screen.custom.BilliardsScreen;
import com.tiago.billiardsmod.screen.custom.BilliardsScreenHandler;
import com.tiago.billiardsmod.network.TableStatePacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class BilliardsModClient implements ClientModInitializer{

    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.BILLIARDS_SCREEN_HANDLER, BilliardsScreen::new);
        ClientPlayNetworking.registerGlobalReceiver(TableStatePacket.ID, (payload, context) -> {
            if (context.player().currentScreenHandler instanceof BilliardsScreenHandler handler
                    && handler.syncId == payload.syncId()
                    && handler.getTablePos().equals(payload.table().tablePos())) {
                handler.applyState(payload.table().state());
            }
        });
    }
}
