package com.naverene.stevespantry;

import com.naverene.stevespantry.event.SpiceTrades;
import com.naverene.stevespantry.reference.Reference;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Reference.MODID)
public class StevesPantry {

    public StevesPantry(IEventBus modBus) {
        ModRegistries.register(modBus);
        NeoForge.EVENT_BUS.addListener(SpiceTrades::onWandererTrades);
    }
}
