package com.naverene.stevespantry;

import com.naverene.stevespantry.reference.Reference;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Wandering trader spice trades are data in 26.1 (villager_trade files tagged into the trader's
 * trade sets), so there's no trade event to listen to here.
 */
@Mod(Reference.MODID)
public class StevesPantry {

    public StevesPantry(IEventBus modBus) {
        ModRegistries.register(modBus);
    }
}
