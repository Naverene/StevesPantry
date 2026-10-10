package com.naverene.stevespantry;

import com.naverene.stevespantry.event.SpiceTrades;
import net.fabricmc.api.ModInitializer;

public class StevesPantry implements ModInitializer {

    @Override
    public void onInitialize() {
        ModRegistries.register();
        SpiceTrades.register();
    }
}
