package com.naverene.stevespantry.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.item.Item;

/**
 * Read-only look at what a dish was made from. (A record on main; a small immutable class here,
 * with the same accessor names, since this version compiles as Java 8.)
 */
public final class DishView {
    private final List<Item> ingredients;
    private final List<ISpice> spices;
    private final boolean perishable;

    /**
     * @param ingredients the foods, in the order they were put in
     * @param spices the spices seasoning it
     * @param perishable whether any ingredient is in the {@code stevespantry:perishable} tag
     */
    public DishView(List<Item> ingredients, List<ISpice> spices, boolean perishable) {
        this.ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients));
        this.spices = Collections.unmodifiableList(new ArrayList<>(spices));
        this.perishable = perishable;
    }

    /** The foods, in the order they were put in. */
    public List<Item> ingredients() {
        return ingredients;
    }

    /** The spices seasoning it. */
    public List<ISpice> spices() {
        return spices;
    }

    /** Whether any ingredient is in the {@code stevespantry:perishable} tag. */
    public boolean perishable() {
        return perishable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DishView)) {
            return false;
        }
        DishView other = (DishView) o;
        return perishable == other.perishable && ingredients.equals(other.ingredients) && spices.equals(other.spices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ingredients, spices, perishable);
    }

    @Override
    public String toString() {
        return "DishView[ingredients=" + ingredients + ", spices=" + spices + ", perishable=" + perishable + "]";
    }
}
