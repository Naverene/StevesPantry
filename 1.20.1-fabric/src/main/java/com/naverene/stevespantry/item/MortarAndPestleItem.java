package com.naverene.stevespantry.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Grinds HarvestCraft crops into spices. Stays in the crafting grid and wears down with each use. */
public class MortarAndPestleItem extends Item {
    public MortarAndPestleItem(Properties properties) {
        super(properties);
    }

    /** Fabric's stack-aware crafting remainder ({@code FabricItem}): the same mortar, one use more worn. */
    @Override
    public ItemStack getRecipeRemainder(ItemStack stack) {
        ItemStack worn = stack.copy();
        worn.setDamageValue(worn.getDamageValue() + 1);
        return worn.getDamageValue() >= worn.getMaxDamage() ? ItemStack.EMPTY : worn;
    }
}
