package com.naverene.stevespantry;

import com.naverene.stevespantry.client.IceboxScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only setup. Only ever called on the physical client, so it's safe to touch {@code Minecraft} here. */
public final class StevesPantryClient {
    private StevesPantryClient() {}

    public static void init(IEventBus modBus) {
        PantryClock.client = () -> {
            var level = Minecraft.getInstance().level;
            return level == null ? -1L : level.getGameTime();
        };
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() ->
                MenuScreens.register(ModRegistries.ICEBOX_MENU.get(), IceboxScreen::new)));
    }
}
