package com.naverene.stevespantry;

import com.naverene.stevespantry.event.SpiceTrades;
import com.naverene.stevespantry.reference.Reference;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod(modid = Reference.MODID, name = Reference.NAME, version = Reference.VERSION,
        acceptedMinecraftVersions = "[1.12.2]", dependencies = "required-after:forge@[14.23.5.2847,)")
public class StevesPantry {
    public static final int GUI_ICEBOX = 0;

    @Mod.Instance(Reference.MODID)
    public static StevesPantry instance;

    @SidedProxy(clientSide = "com.naverene.stevespantry.StevesPantryClient",
            serverSide = "com.naverene.stevespantry.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, proxy);
        SpiceTrades.registerVillagerTrades();
    }
}
