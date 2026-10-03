package com.naverene.stevespantry.recipe;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.Spice;
import com.naverene.stevespantry.component.DishContents;
import com.naverene.stevespantry.component.Freshness;
import com.naverene.stevespantry.item.DishItem;
import com.naverene.stevespantry.item.SpiceItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Bowl + 1 to 4 foods + up to 3 different spices, anywhere in the crafting grid, makes a dish.
 * Any edible item counts as a food, so every Pam's HarvestCraft crop and meal works without
 * either mod knowing about the other.
 */
public class DishAssemblyRecipe extends CustomRecipe {
    public static final int MAX_INGREDIENTS = 4;
    public static final int MAX_SPICES = 3;
    private static final int MAX_NUTRITION = 20;

    public DishAssemblyRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return parse(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Parsed parsed = parse(input);
        if (parsed == null) {
            return ItemStack.EMPTY;
        }

        // Tinkers-style: the result's stats come from its parts.
        int nutrition = 0;
        float saturation = 0F;
        for (ItemStack food : parsed.foods) {
            FoodProperties props = food.get(DataComponents.FOOD);
            nutrition += props.nutrition();
            saturation += props.saturation();
        }
        long distinct = parsed.foods.stream().map(ItemStack::getItem).distinct().count();
        nutrition += (int) (distinct - 1); // variety bonus: +1 per different ingredient beyond the first
        float shelfLife = DishItem.BASE_SHELF_LIFE;
        for (Spice spice : parsed.spices) {
            saturation += spice.bonusSaturation();
            shelfLife *= spice.shelfLifeMultiplier();
        }
        nutrition = Math.min(nutrition, MAX_NUTRITION);
        saturation = Math.min(saturation, nutrition);
        float eatSeconds = 1.2F + 0.4F * parsed.foods.size();

        ItemStack dish = new ItemStack(ModRegistries.DISH.get());
        dish.set(ModRegistries.DISH_CONTENTS, new DishContents(
                parsed.foods.stream().map(ItemStack::getItem).toList(), parsed.spices));
        dish.set(ModRegistries.FRESHNESS, new Freshness(Freshness.UNSTAMPED, (long) shelfLife));
        dish.set(DataComponents.FOOD, new FoodProperties(nutrition, saturation, false, eatSeconds,
                Optional.of(new ItemStack(Items.BOWL)), List.of()));
        return dish;
    }

    @Nullable
    private static Parsed parse(CraftingInput input) {
        int bowls = 0;
        List<ItemStack> foods = new ArrayList<>();
        List<Spice> spices = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            Item item = stack.getItem();
            if (item == Items.BOWL) {
                bowls++;
            } else if (item instanceof SpiceItem spiceItem) {
                if (spices.contains(spiceItem.spice())) {
                    return null;
                }
                spices.add(spiceItem.spice());
            } else if (isIngredient(stack)) {
                foods.add(stack);
            } else {
                return null;
            }
        }
        if (bowls != 1 || foods.isEmpty() || foods.size() > MAX_INGREDIENTS || spices.size() > MAX_SPICES) {
            return null;
        }
        return new Parsed(foods, spices);
    }

    private static boolean isIngredient(ItemStack stack) {
        return stack.has(DataComponents.FOOD)
                && !(stack.getItem() instanceof DishItem)
                && !stack.is(ModRegistries.SPOILED_LEFTOVERS.get());
    }

    private record Parsed(List<ItemStack> foods, List<Spice> spices) {}

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistries.DISH_ASSEMBLY.get();
    }
}
