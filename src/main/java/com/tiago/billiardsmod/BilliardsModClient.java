package com.tiago.billiardsmod;

import com.tiago.billiardsmod.screen.ModScreenHandlers;
import com.tiago.billiardsmod.screen.custom.BilliardsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class BilliardsModClient implements ClientModInitializer{

    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.BILLIARDS_SCREEN_HANDLER, BilliardsScreen::new);
    }
}
