package com.naverene.stevespantry;

import com.naverene.stevespantry.block.IceboxBlock;
import com.naverene.stevespantry.block.IceboxBlockEntity;
import com.naverene.stevespantry.menu.IceboxMenu;
import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Server-side proxy and GUI handler. {@link StevesPantryClient} adds the client half. */
public class CommonProxy implements IGuiHandler {
    public void init() {}

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (id == IceboxBlock.GUI_ID && tile instanceof IceboxBlockEntity) {
            return new IceboxMenu(player.inventory, (IceboxBlockEntity) tile);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }
}
