package com.naverene.stevespantry;

import com.naverene.stevespantry.client.IceboxScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;

public class StevesPantryClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PantryClock.client = () -> {
            var level = Minecraft.getInstance().level;
            return level == null ? -1L : level.getGameTime();
        };
        // MenuScreens.register is made public by Fabric API's transitive access wideners.
        MenuScreens.register(ModRegistries.ICEBOX_MENU, IceboxScreen::new);
    }
}
