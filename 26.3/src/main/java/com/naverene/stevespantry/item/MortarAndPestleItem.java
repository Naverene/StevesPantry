package com.naverene.stevespantry.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jetbrains.annotations.Nullable;

/** Grinds HarvestCraft crops into spices. Stays in the crafting grid and wears down with each use. */
public class MortarAndPestleItem extends Item {
    public MortarAndPestleItem(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
        // Recipes hand us the stack in the grid; anything else (a recipe display) gets a fresh mortar back.
        ItemStack worn = instance instanceof ItemStack stack ? stack.copyWithCount(1) : new ItemStack(this);
        worn.setDamageValue(worn.getDamageValue() + 1);
        return worn.getDamageValue() >= worn.getMaxDamage() ? null : ItemStackTemplate.fromNonEmptyStack(worn);
    }
}
