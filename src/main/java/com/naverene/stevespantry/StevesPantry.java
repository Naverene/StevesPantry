package com.naverene.stevespantry;

import com.naverene.stevespantry.event.SpiceTrades;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(StevesPantry.MODID)
public class StevesPantry {
    public static final String MODID = "stevespantry";

    public StevesPantry(IEventBus modBus) {
        ModRegistries.register(modBus);
        NeoForge.EVENT_BUS.addListener(SpiceTrades::onWandererTrades);
    }
}
