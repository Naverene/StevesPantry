package com.naverene.stevespantry.block;

import com.naverene.stevespantry.StevesPantry;
import com.naverene.stevespantry.reference.Reference;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/** Unpowered cold storage: 27 food slots and an ice slot. Dishes inside spoil three times slower while there's ice. */
public class IceboxBlock extends BlockContainer {
    public static final int GUI_ID = 0;

    @SideOnly(Side.CLIENT)
    private IIcon top;
    @SideOnly(Side.CLIENT)
    private IIcon bottom;

    public IceboxBlock() {
        super(Material.wood);
        setHardness(2.5F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
        setBlockName(Reference.MODID + ".icebox");
        setBlockTextureName(Reference.MODID + ":icebox_side");
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new IceboxBlockEntity();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side,
            float hitX, float hitY, float hitZ) {
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof IceboxBlockEntity) {
            player.openGui(StevesPantry.instance, GUI_ID, world, x, y, z);
        }
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof IInventory) {
            IInventory inventory = (IInventory) tile;
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack stack = inventory.getStackInSlotOnClosing(i);
                if (stack != null) {
                    EntityItem drop = new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, stack);
                    drop.motionX = world.rand.nextGaussian() * 0.05D;
                    drop.motionY = world.rand.nextGaussian() * 0.05D + 0.2D;
                    drop.motionZ = world.rand.nextGaussian() * 0.05D;
                    world.spawnEntityInWorld(drop);
                }
            }
            world.func_147453_f(x, y, z, block);
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof IInventory ? Container.calcRedstoneFromInventory((IInventory) tile) : 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(Reference.MODID + ":icebox_side");
        top = register.registerIcon(Reference.MODID + ":icebox_top");
        bottom = register.registerIcon(Reference.MODID + ":icebox_bottom");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return side == 0 ? bottom : side == 1 ? top : blockIcon;
    }
}
