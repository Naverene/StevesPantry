package com.naverene.stevespantry.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Grinds HarvestCraft crops into spices. Stays in the crafting grid and wears down with each use. */
public class MortarAndPestleItem extends Item {
    public MortarAndPestleItem(int durability) {
        setMaxDamage(durability);
        setMaxStackSize(1);
    }

    @Override
    public boolean hasContainerItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        ItemStack worn = stack.copy();
        worn.setItemDamage(worn.getItemDamage() + 1);
        return worn.getItemDamage() >= worn.getMaxDamage() ? ItemStack.EMPTY : worn;
    }
}
