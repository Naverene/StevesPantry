package com.naverene.stevespantry.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.ItemStack;

/**
 * Read-only look at what a dish was made from. (A record of {@code List<Item>} on main; here a small
 * immutable class with the same accessors, holding one-count {@code ItemStack}s because 1.12.2 items
 * still carry metadata, e.g. the four raw fish.)
 */
public final class DishView {
    private final List<ItemStack> ingredients;
    private final List<ISpice> spices;
    private final boolean perishable;

    /**
     * @param ingredients the foods, in the order they were put in
     * @param spices the spices seasoning it
     * @param perishable whether any ingredient is perishable (see {@link IPantryApi#perishableOreNames()})
     */
    public DishView(List<ItemStack> ingredients, List<ISpice> spices, boolean perishable) {
        List<ItemStack> copies = new ArrayList<>(ingredients.size());
        for (ItemStack ingredient : ingredients) {
            copies.add(ingredient.copy());
        }
        this.ingredients = Collections.unmodifiableList(copies);
        this.spices = Collections.unmodifiableList(new ArrayList<>(spices));
        this.perishable = perishable;
    }

    /** The foods, one of each stack, in the order they were put in. Copies: changing them changes nothing. */
    public List<ItemStack> ingredients() {
        return ingredients;
    }

    /** The spices seasoning it. */
    public List<ISpice> spices() {
        return spices;
    }

    /** Whether any ingredient is perishable. */
    public boolean perishable() {
        return perishable;
    }

    @Override
    public String toString() {
        return "DishView[ingredients=" + ingredients + ", spices=" + spices + ", perishable=" + perishable + "]";
    }
}
