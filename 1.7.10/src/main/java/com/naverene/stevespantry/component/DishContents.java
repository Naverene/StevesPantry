package com.naverene.stevespantry.component;

import com.naverene.stevespantry.Spice;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

/**
 * What a dish was made from: its ingredients in order, and the spices that season it.
 *
 * <p>Stored in the stack's NBT: {@code ingredients} is a list of {@code {id, meta}} compounds
 * (items still carry metadata on 1.7.10, e.g. raw salmon is fish:1), {@code spices} a list of spice ids.
 */
public final class DishContents {
    private static final String INGREDIENTS = "ingredients";
    private static final String SPICES = "spices";

    private final List<ItemStack> ingredients;
    private final List<Spice> spices;

    public DishContents(List<ItemStack> ingredients, List<Spice> spices) {
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack ingredient : ingredients) {
            copies.add(new ItemStack(ingredient.getItem(), 1, ingredient.getItemDamage()));
        }
        this.ingredients = Collections.unmodifiableList(copies);
        this.spices = Collections.unmodifiableList(new ArrayList<>(spices));
    }

    public List<ItemStack> ingredients() {
        return ingredients;
    }

    public List<Spice> spices() {
        return spices;
    }

    /** The stack's contents, or null if it doesn't have any. Ingredients from removed mods are skipped. */
    public static DishContents get(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null || !tag.hasKey(INGREDIENTS)) {
            return null;
        }
        List<ItemStack> ingredients = new ArrayList<>();
        NBTTagList list = tag.getTagList(INGREDIENTS, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            Item item = (Item) Item.itemRegistry.getObject(entry.getString("id"));
            if (item != null) {
                ingredients.add(new ItemStack(item, 1, entry.getShort("meta")));
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
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagList list = new NBTTagList();
        for (ItemStack ingredient : ingredients) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("id", Item.itemRegistry.getNameForObject(ingredient.getItem()));
            entry.setShort("meta", (short) ingredient.getItemDamage());
            list.appendTag(entry);
        }
        NBTTagList spiceList = new NBTTagList();
        for (Spice spice : spices) {
            spiceList.appendTag(new NBTTagString(spice.id()));
        }
        stack.getTagCompound().setTag(INGREDIENTS, list);
        stack.getTagCompound().setTag(SPICES, spiceList);
    }
}
