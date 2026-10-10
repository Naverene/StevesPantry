package com.naverene.stevespantry;

import com.naverene.stevespantry.block.IceboxBlock;
import com.naverene.stevespantry.block.IceboxBlockEntity;
import com.naverene.stevespantry.client.IceboxScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Client proxy: the client clock for freshness bars, and the icebox screen. */
public class StevesPantryClient extends CommonProxy {
    @Override
    public void init() {
        PantryClock.client = () -> {
            World level = Minecraft.getMinecraft().theWorld;
            return level == null ? -1L : level.getTotalWorldTime();
        };
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (id == IceboxBlock.GUI_ID && tile instanceof IceboxBlockEntity) {
            return new IceboxScreen(player.inventory, (IceboxBlockEntity) tile);
        }
        return null;
    }
}
