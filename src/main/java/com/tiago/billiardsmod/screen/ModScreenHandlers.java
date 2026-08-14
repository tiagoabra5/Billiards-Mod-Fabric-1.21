package com.tiago.billiardsmod.screen;

import com.tiago.billiardsmod.BilliardsMod;
import com.tiago.billiardsmod.screen.custom.BilliardsScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ModScreenHandlers {

    public static final ScreenHandlerType<BilliardsScreenHandler> BILLIARDS_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(BilliardsMod.MOD_ID, "billiards_screen_handler"),
                    new ExtendedScreenHandlerType<>(BilliardsScreenHandler::new, BlockPos.PACKET_CODEC));

    public static void registerScreenHandlers(){
        BilliardsMod.LOGGER.info("Registering Screen Handlers for " + BilliardsMod.MOD_ID);
    }
}
