package com.naverene.stevespantry;

import com.naverene.stevespantry.block.IceboxBlockEntity;
import com.naverene.stevespantry.client.IceboxScreen;
import com.naverene.stevespantry.reference.Reference;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Client half of the sided proxy: the client clock, item models and the icebox screen. */
@Mod.EventBusSubscriber(modid = Reference.MODID, value = Side.CLIENT)
public class StevesPantryClient extends CommonProxy {
    @Override
    public void preInit() {
        PantryClock.client = () -> {
            World level = Minecraft.getMinecraft().world;
            return level == null ? -1L : level.getTotalWorldTime();
        };
    }

    @Override
    @Nullable
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (id == StevesPantry.GUI_ICEBOX && tile instanceof IceboxBlockEntity) {
            return new IceboxScreen(player.inventory, (IceboxBlockEntity) tile);
        }
        return null;
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (Item item : ModRegistries.items()) {
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }
}
