package com.naverene.stevespantry.component;

import com.naverene.stevespantry.Spice;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

/**
 * What a dish was made from: its ingredients in order, and the spices that season it. 1.12.2 has no
 * data components, so this lives in the dish's NBT: {@code ingredients} is a list of
 * {@code {id, Damage}} compounds (items still carry metadata here, e.g. the four raw fish), and
 * {@code spices} a list of spice ids.
 */
public final class DishContents {
    public static final String INGREDIENTS = "ingredients";
    public static final String SPICES = "spices";

    private final List<ItemStack> ingredients;
    private final List<Spice> spices;

    public DishContents(List<ItemStack> ingredients, List<Spice> spices) {
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack ingredient : ingredients) {
            copies.add(new ItemStack(ingredient.getItem(), 1, ingredient.getMetadata()));
        }
        this.ingredients = Collections.unmodifiableList(copies);
        this.spices = Collections.unmodifiableList(new ArrayList<>(spices));
    }

    /** One of each ingredient, in the order they went in. */
    public List<ItemStack> ingredients() {
        return ingredients;
    }

    public List<Spice> spices() {
        return spices;
    }

    @Nullable
    public static DishContents get(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(INGREDIENTS, Constants.NBT.TAG_LIST)) {
            return null;
        }
        List<ItemStack> ingredients = new ArrayList<>();
        NBTTagList list = tag.getTagList(INGREDIENTS, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            Item item = Item.getByNameOrId(entry.getString("id"));
            if (item != null) {
                ingredients.add(new ItemStack(item, 1, entry.getShort("Damage")));
            }
        }
        List<Spice> spices = new ArrayList<>();
        NBTTagList spiceList = tag.getTagList(SPICES, Constants.NBT.TAG_STRING);
        for (int i = 0; i < spiceList.tagCount(); i++) {
            Spice spice = Spice.byId(spiceList.getStringTagAt(i));
            if (spice != null) {
                spices.add(spice);
            }
        }
        return new DishContents(ingredients, spices);
    }

    public void set(ItemStack stack) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (ItemStack ingredient : ingredients) {
            ResourceLocation id = ingredient.getItem().getRegistryName();
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("id", id == null ? "minecraft:air" : id.toString());
            entry.setShort("Damage", (short) ingredient.getMetadata());
            list.appendTag(entry);
        }
        tag.setTag(INGREDIENTS, list);
        NBTTagList spiceList = new NBTTagList();
        for (Spice spice : spices) {
            spiceList.appendTag(new NBTTagString(spice.id()));
        }
        tag.setTag(SPICES, spiceList);
        stack.setTagCompound(tag);
    }
}
