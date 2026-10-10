package com.naverene.stevespantry.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Grinds HarvestCraft crops into spices. Stays in the crafting grid and wears down with each use. */
public class MortarAndPestleItem extends Item {
    public MortarAndPestleItem(Properties properties) {
        super(properties);
    }

    // Forge 1.18.2 still calls the crafting remainder the "container item".
    @Override
    public boolean hasContainerItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        ItemStack worn = stack.copy();
        worn.setDamageValue(worn.getDamageValue() + 1);
        return worn.getDamageValue() >= worn.getMaxDamage() ? ItemStack.EMPTY : worn;
    }
}
