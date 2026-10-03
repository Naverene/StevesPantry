package com.naverene.stevespantry;

import com.naverene.stevespantry.reference.Reference;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = Reference.MODID, dist = Dist.CLIENT)
public class StevesPantryClient {
    public StevesPantryClient(IEventBus modBus) {
        PantryClock.client = () -> {
            var level = Minecraft.getInstance().level;
            return level == null ? -1L : level.getGameTime();
        };
    }
}
