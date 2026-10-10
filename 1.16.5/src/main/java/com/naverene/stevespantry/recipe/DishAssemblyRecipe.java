package com.naverene.stevespantry.recipe;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.ModTags;
import com.naverene.stevespantry.PantryApiImpl;
import com.naverene.stevespantry.Spice;
import com.naverene.stevespantry.component.DishContents;
import com.naverene.stevespantry.component.Freshness;
import com.naverene.stevespantry.item.DishItem;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Food;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * Bowl + 1 to 4 foods + up to 3 different spices, anywhere in the crafting grid, makes a dish.
 * Items other mods registered as spices through the API count as those spices.
 * Any edible item counts as a food, so every Pam's HarvestCraft crop and meal works without
 * either mod knowing about the other.
 */
public class DishAssemblyRecipe extends SpecialRecipe {
    public static final int MAX_INGREDIENTS = 4;
    public static final int MAX_SPICES = 3;
    private static final int MAX_NUTRITION = 20;

    public DishAssemblyRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingInventory input, World level) {
        return parse(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInventory input) {
        Parsed parsed = parse(input);
        return parsed == null ? ItemStack.EMPTY : build(parsed.foods, parsed.spices);
    }

    /** Whether these foods and spices make a valid dish (the bowl aside). */
    public static boolean accepts(List<ItemStack> foods, List<Spice> spices) {
        return !foods.isEmpty() && foods.size() <= MAX_INGREDIENTS
                && spices.size() <= MAX_SPICES && spices.stream().distinct().count() == spices.size()
                && foods.stream().allMatch(DishAssemblyRecipe::isIngredient);
    }

    /** Makes the dish. Callers check {@link #accepts} first. */
    public static ItemStack build(List<ItemStack> foods, List<Spice> spices) {
        // Tinkers-style: the result's stats come from its parts.
        int nutrition = 0;
        float saturation = 0F;
        for (ItemStack food : foods) {
            Food props = food.getItem().getFoodProperties();
            nutrition += props.getNutrition();
            // The absolute saturation a food restores, as main's food component stores it.
            saturation += props.getNutrition() * props.getSaturationModifier() * 2F;
        }
        long distinct = foods.stream().map(ItemStack::getItem).distinct().count();
        nutrition += (int) (distinct - 1); // variety bonus: +1 per different ingredient beyond the first
        float shelfLife = DishItem.BASE_SHELF_LIFE;
        if (foods.stream().anyMatch(food -> ModTags.PERISHABLE.contains(food.getItem()))) {
            shelfLife *= DishItem.PERISHABLE_MULTIPLIER;
        }
        for (Spice spice : spices) {
            saturation += spice.bonusSaturation();
            shelfLife *= spice.shelfLifeMultiplier();
        }
        nutrition = Math.min(nutrition, MAX_NUTRITION);
        saturation = Math.min(saturation, nutrition);
        float eatSeconds = 1.2F + 0.4F * foods.size();

        ItemStack dish = new ItemStack(ModRegistries.DISH.get());
        new DishContents(foods.stream().map(ItemStack::getItem).collect(Collectors.toList()), spices)
                .set(dish);
        new Freshness(Freshness.UNSTAMPED,
                PantryApiImpl.INSTANCE.modifyShelfLife(foods, spices, (long) shelfLife)).set(dish);
        DishItem.setFood(dish, nutrition, saturation, Math.round(eatSeconds * 20F));
        return dish;
    }

    @Nullable
    private static Parsed parse(CraftingInventory input) {
        int bowls = 0;
        List<ItemStack> foods = new ArrayList<>();
        List<Spice> spices = new ArrayList<>();
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack stack = input.getItem(i);
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
        return stack.isEdible()
                && stack.getItem().getFoodProperties() != null
                && !(stack.getItem() instanceof DishItem)
                && stack.getItem() != ModRegistries.SPOILED_LEFTOVERS.get();
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
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return ModRegistries.DISH_ASSEMBLY.get();
    }
}
