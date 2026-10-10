package com.naverene.stevespantry.component;

import com.naverene.stevespantry.Spice;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * What a dish was made from: its ingredients in order, and the spices that season it.
 * 1.20.1 has no data components, so this is read from and written to the stack's NBT
 * ({@code ingredients} and {@code spices}, both lists of ids).
 */
public record DishContents(List<Item> ingredients, List<Spice> spices) {
    public static final String INGREDIENTS = "ingredients";
    public static final String SPICES = "spices";

    public DishContents {
        ingredients = List.copyOf(ingredients);
        spices = List.copyOf(spices);
    }

    /** The contents saved on a dish, or null if the stack has none. */
    @Nullable
    public static DishContents get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(INGREDIENTS, Tag.TAG_LIST)) {
            return null;
        }
        List<Item> ingredients = new ArrayList<>();
        for (Tag entry : tag.getList(INGREDIENTS, Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
            Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
            // An ingredient from a mod that's since been removed just drops out of the list.
            if (item != Items.AIR) {
                ingredients.add(item);
            }
        }
        List<Spice> spices = new ArrayList<>();
        for (Tag entry : tag.getList(SPICES, Tag.TAG_STRING)) {
            Spice spice = Spice.byId(entry.getAsString());
            if (spice != null) {
                spices.add(spice);
            }
        }
        return new DishContents(ingredients, spices);
    }

    public void set(ItemStack stack) {
        ListTag ingredientTags = new ListTag();
        for (Item item : ingredients) {
            ingredientTags.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
        }
        ListTag spiceTags = new ListTag();
        for (Spice spice : spices) {
            spiceTags.add(StringTag.valueOf(spice.id()));
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(INGREDIENTS, ingredientTags);
        tag.put(SPICES, spiceTags);
    }
}
