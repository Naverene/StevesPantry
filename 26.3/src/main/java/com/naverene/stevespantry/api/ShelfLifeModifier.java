package com.naverene.stevespantry.api;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * Adjusts a dish's shelf life when it's assembled, after Steve's Pantry's own rules (base life,
 * perishable ingredients, preserving spices) have run. Modifiers run in registration order, each
 * one getting the previous one's result.
 */
@FunctionalInterface
public interface ShelfLifeModifier {
    /**
     * @param foods the food stacks going into the dish (don't modify them)
     * @param spices the spices going into the dish
     * @param shelfLife the shelf life so far, in ticks
     * @return the new shelf life in ticks; return {@code shelfLife} to leave it alone
     */
    long modify(List<ItemStack> foods, List<ISpice> spices, long shelfLife);
}
