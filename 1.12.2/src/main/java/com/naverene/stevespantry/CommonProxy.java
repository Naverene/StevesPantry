package com.naverene.stevespantry;

import com.naverene.stevespantry.block.IceboxBlockEntity;
import com.naverene.stevespantry.menu.IceboxMenu;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/** Server-side half of the sided proxy; also opens the icebox menu. The client half is {@link StevesPantryClient}. */
public class CommonProxy implements IGuiHandler {
    public void preInit() {}

    @Override
    @Nullable
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (id == StevesPantry.GUI_ICEBOX && tile instanceof IceboxBlockEntity) {
            return new IceboxMenu(player.inventory, (IceboxBlockEntity) tile);
        }
        return null;
    }

    @Override
    @Nullable
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }
}
