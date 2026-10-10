package com.naverene.stevespantry;

import com.naverene.stevespantry.event.SpiceTrades;
import com.naverene.stevespantry.reference.Reference;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Reference.MODID, name = Reference.NAME, version = Reference.VERSION,
        acceptedMinecraftVersions = "[1.7.10]", dependencies = "after:harvestcraft")
public class StevesPantry {
    public static final Logger LOGGER = LogManager.getLogger(Reference.MODID);

    @Mod.Instance(Reference.MODID)
    public static StevesPantry instance;

    @SidedProxy(clientSide = "com.naverene.stevespantry.StevesPantryClient",
            serverSide = "com.naverene.stevespantry.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModRegistries.register();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
        ModRegistries.registerRecipes();
        SpiceTrades.register();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // Other mods register their ore names by now, so grind recipes only go in for sources that exist.
        LOGGER.info("Added {} spice grinding recipes from the Ore Dictionary", ModRegistries.registerGrindRecipes());
    }
}
