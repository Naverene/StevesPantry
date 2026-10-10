package com.naverene.stevespantry;

import com.naverene.stevespantry.event.SpiceTrades;
import com.naverene.stevespantry.reference.Reference;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Reference.MODID)
public class StevesPantry {

    public StevesPantry(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModRegistries.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(SpiceTrades::onWandererTrades);
        // Forge 1.20.1 has no client-only @Mod entrypoint, so the client half is wired up from here.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            StevesPantryClient.init(modBus);
        }
    }
}
