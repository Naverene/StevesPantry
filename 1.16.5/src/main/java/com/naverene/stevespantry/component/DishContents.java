package com.naverene.stevespantry.component;

import com.naverene.stevespantry.Spice;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.common.util.Constants;

/**
 * What a dish was made from: its ingredients in order, and the spices that season it. 1.16.5 has no
 * data components, so this lives in the stack's NBT as {@code ingredients} (item ids) and
 * {@code spices} (spice ids), the same field names main's component uses.
 */
public final class DishContents {
    public static final String INGREDIENTS = "ingredients";
    public static final String SPICES = "spices";

    private final List<Item> ingredients;
    private final List<Spice> spices;

    public DishContents(List<Item> ingredients, List<Spice> spices) {
        this.ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients));
        this.spices = Collections.unmodifiableList(new ArrayList<>(spices));
    }

    public List<Item> ingredients() {
        return ingredients;
    }

    public List<Spice> spices() {
        return spices;
    }

    /** Reads a dish's contents, or null if the stack carries none. Ids that no longer exist are skipped. */
    @Nullable
    public static DishContents get(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        if (tag == null || !tag.contains(INGREDIENTS, Constants.NBT.TAG_LIST)) {
            return null;
        }
        List<Item> ingredients = new ArrayList<>();
        for (INBT entry : tag.getList(INGREDIENTS, Constants.NBT.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
            if (id != null) {
                Registry.ITEM.getOptional(id).ifPresent(ingredients::add);
            }
        }
        List<Spice> spices = new ArrayList<>();
        for (INBT entry : tag.getList(SPICES, Constants.NBT.TAG_STRING)) {
            Spice spice = Spice.byId(entry.getAsString());
            if (spice != null) {
                spices.add(spice);
            }
        }
        return new DishContents(ingredients, spices);
    }

    public void set(ItemStack stack) {
        CompoundNBT tag = stack.getOrCreateTag();
        ListNBT ingredientList = new ListNBT();
        for (Item item : ingredients) {
            ingredientList.add(StringNBT.valueOf(Registry.ITEM.getKey(item).toString()));
        }
        tag.put(INGREDIENTS, ingredientList);
        ListNBT spiceList = new ListNBT();
        for (Spice spice : spices) {
            spiceList.add(StringNBT.valueOf(spice.id()));
        }
        tag.put(SPICES, spiceList);
    }
}
