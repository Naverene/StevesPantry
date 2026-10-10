package com.naverene.stevespantry.recipe;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.ModTags;
import com.naverene.stevespantry.PantryApiImpl;
import com.naverene.stevespantry.Spice;
import com.naverene.stevespantry.component.DishContents;
import com.naverene.stevespantry.component.Freshness;
import com.naverene.stevespantry.item.DishItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * Bowl + 1 to 4 foods + up to 3 different spices, anywhere in the crafting grid, makes a dish.
 * Items other mods registered as spices through the API count as those spices.
 * Any edible item counts as a food, so every Pam's HarvestCraft crop and meal works without
 * either mod knowing about the other.
 */
public class DishAssemblyRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    public static final int MAX_INGREDIENTS = 4;
    public static final int MAX_SPICES = 3;
    private static final int MAX_NUTRITION = 20;

    @Override
    public boolean matches(InventoryCrafting input, World world) {
        return parse(input) != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting input) {
        Parsed parsed = parse(input);
        return parsed == null ? ItemStack.EMPTY : build(parsed.foods, parsed.spices);
    }

    /** Whether these foods and spices make a valid dish (the bowl aside). */
    public static boolean accepts(List<ItemStack> foods, List<Spice> spices) {
        if (foods.isEmpty() || foods.size() > MAX_INGREDIENTS || spices.size() > MAX_SPICES
                || new HashSet<>(spices).size() != spices.size()) {
            return false;
        }
        for (ItemStack food : foods) {
            if (food == null || !isIngredient(food)) {
                return false;
            }
        }
        return true;
    }

    /** Makes the dish. Callers check {@link #accepts} first. */
    public static ItemStack build(List<ItemStack> foods, List<Spice> spices) {
        // Tinkers-style: the result's stats come from its parts.
        int nutrition = 0;
        float saturation = 0F;
        for (ItemStack food : foods) {
            ItemFood item = (ItemFood) food.getItem();
            int heal = item.getHealAmount(food);
            nutrition += heal;
            saturation += heal * item.getSaturationModifier(food) * 2F;
        }
        List<ItemStack> distinct = new ArrayList<>();
        boolean perishable = false;
        for (ItemStack food : foods) {
            boolean seen = false;
            for (ItemStack other : distinct) {
                seen |= ItemStack.areItemsEqual(other, food);
            }
            if (!seen) {
                distinct.add(food);
            }
            perishable |= ModTags.isPerishable(food);
        }
        nutrition += distinct.size() - 1; // variety bonus: +1 per different ingredient beyond the first
        float shelfLife = DishItem.BASE_SHELF_LIFE;
        if (perishable) {
            shelfLife *= DishItem.PERISHABLE_MULTIPLIER;
        }
        for (Spice spice : spices) {
            saturation += spice.bonusSaturation();
            shelfLife *= spice.shelfLifeMultiplier();
        }
        nutrition = Math.min(nutrition, MAX_NUTRITION);
        saturation = Math.min(saturation, nutrition);
        float eatSeconds = 1.2F + 0.4F * foods.size();

        ItemStack dish = new ItemStack(ModRegistries.DISH);
        new DishContents(foods, spices).set(dish);
        new Freshness(Freshness.UNSTAMPED,
                PantryApiImpl.INSTANCE.modifyShelfLife(foods, spices, (long) shelfLife)).set(dish);
        NBTTagCompound tag = dish.getTagCompound();
        tag.setInteger(DishItem.NUTRITION, nutrition);
        tag.setFloat(DishItem.SATURATION, saturation);
        tag.setInteger(DishItem.EAT_TICKS, Math.round(eatSeconds * 20F));
        return dish;
    }

    @Nullable
    private static Parsed parse(InventoryCrafting input) {
        int bowls = 0;
        List<ItemStack> foods = new ArrayList<>();
        List<Spice> spices = new ArrayList<>();
        for (int i = 0; i < input.getSizeInventory(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            Spice spice = (Spice) PantryApiImpl.INSTANCE.spiceOf(stack).orElse(null);
            if (stack.getItem() == Items.BOWL) {
                bowls++;
            } else if (spice != null) {
                if (spices.contains(spice)) {
                    return null;
                }
                spices.add(spice);
            } else if (isIngredient(stack)) {
                foods.add(stack);
            } else {
                return null;
            }
        }
        if (bowls != 1 || !accepts(foods, spices)) {
            return null;
        }
        return new Parsed(foods, spices);
    }

    private static boolean isIngredient(ItemStack stack) {
        return stack.getItem() instanceof ItemFood
                && !(stack.getItem() instanceof DishItem)
                && stack.getItem() != ModRegistries.SPOILED_LEFTOVERS;
    }

    private static final class Parsed {
        final List<ItemStack> foods;
        final List<Spice> spices;

        Parsed(List<ItemStack> foods, List<Spice> spices) {
            this.foods = foods;
            this.spices = spices;
        }
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    /** The result depends on the grid, so keep it out of the recipe book. */
    @Override
    public boolean isDynamic() {
        return true;
    }
}
