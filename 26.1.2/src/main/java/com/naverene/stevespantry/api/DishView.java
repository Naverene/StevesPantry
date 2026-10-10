package com.naverene.stevespantry.api;

import java.util.List;
import net.minecraft.world.item.Item;

/**
 * Read-only look at what a dish was made from.
 *
 * @param ingredients the foods, in the order they were put in
 * @param spices the spices seasoning it
 * @param perishable whether any ingredient is in the {@code stevespantry:perishable} tag
 */
public record DishView(List<Item> ingredients, List<ISpice> spices, boolean perishable) {
    public DishView {
        ingredients = List.copyOf(ingredients);
        spices = List.copyOf(spices);
    }
}
