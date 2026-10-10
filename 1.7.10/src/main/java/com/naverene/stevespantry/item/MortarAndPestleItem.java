package com.naverene.stevespantry.item;

import com.naverene.stevespantry.reference.Reference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Grinds HarvestCraft crops into spices. Stays in the crafting grid and wears down with each use. */
public class MortarAndPestleItem extends Item {
    public MortarAndPestleItem() {
        setMaxStackSize(1);
        setMaxDamage(128);
        setNoRepair();
        setUnlocalizedName(Reference.MODID + ".mortar_and_pestle");
        setTextureName(Reference.MODID + ":mortar_and_pestle");
    }

    @Override
    public boolean hasContainerItem(ItemStack stack) {
        return true;
    }

    @Override
    public boolean doesContainerItemLeaveCraftingGrid(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        ItemStack worn = stack.copy();
        worn.stackSize = 1;
        worn.setItemDamage(worn.getItemDamage() + 1);
        return worn.getItemDamage() >= worn.getMaxDamage() ? null : worn;
    }
}
