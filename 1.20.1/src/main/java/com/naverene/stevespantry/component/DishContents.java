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
import org.jetbrains.annotations.Nullable;

/**
 * What a dish was made from: its ingredients in order, and the spices that season it. 1.20.1 has no
 * data components, so this lives in the stack's NBT as {@code ingredients} (item ids) and
 * {@code spices} (spice ids), the same field names main's component uses.
 */
public record DishContents(List<Item> ingredients, List<Spice> spices) {
    public static final String INGREDIENTS = "ingredients";
    public static final String SPICES = "spices";

    public DishContents {
        ingredients = List.copyOf(ingredients);
        spices = List.copyOf(spices);
    }

    /** Reads a dish's contents, or null if the stack carries none. Ids that no longer exist are skipped. */
    @Nullable
    public static DishContents get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(INGREDIENTS, Tag.TAG_LIST)) {
            return null;
        }
        List<Item> ingredients = new ArrayList<>();
        for (Tag entry : tag.getList(INGREDIENTS, Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
            if (id != null) {
                BuiltInRegistries.ITEM.getOptional(id).ifPresent(ingredients::add);
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
        CompoundTag tag = stack.getOrCreateTag();
        ListTag ingredientList = new ListTag();
        for (Item item : ingredients) {
            ingredientList.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
        }
        tag.put(INGREDIENTS, ingredientList);
        ListTag spiceList = new ListTag();
        for (Spice spice : spices) {
            spiceList.add(StringTag.valueOf(spice.id()));
        }
        tag.put(SPICES, spiceList);
    }
}
