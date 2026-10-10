package com.naverene.stevespantry;

import com.naverene.stevespantry.client.IceboxScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class StevesPantryClient {
    private StevesPantryClient() {}

    static void init(IEventBus modBus) {
        PantryClock.client = () -> {
            var level = Minecraft.getInstance().level;
            return level == null ? -1L : level.getGameTime();
        };
        // MenuScreens isn't thread-safe, so register on the main thread.
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() ->
                MenuScreens.register(ModRegistries.ICEBOX_MENU.get(), IceboxScreen::new)));
    }
}
