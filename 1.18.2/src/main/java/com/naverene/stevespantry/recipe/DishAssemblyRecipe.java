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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Bowl + 1 to 4 foods + up to 3 different spices, anywhere in the crafting grid, makes a dish.
 * Items other mods registered as spices through the API count as those spices.
 * Any edible item counts as a food, so every Pam's HarvestCraft crop and meal works without
 * either mod knowing about the other.
 */
public class DishAssemblyRecipe extends CustomRecipe {
    public static final int MAX_INGREDIENTS = 4;
    public static final int MAX_SPICES = 3;
    private static final int MAX_NUTRITION = 20;

    public DishAssemblyRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        return parse(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer input) {
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
            FoodProperties props = food.getItem().getFoodProperties();
            nutrition += props.getNutrition();
            // The absolute saturation a food restores, as main's food component stores it.
            saturation += props.getNutrition() * props.getSaturationModifier() * 2F;
        }
        long distinct = foods.stream().map(ItemStack::getItem).distinct().count();
        nutrition += (int) (distinct - 1); // variety bonus: +1 per different ingredient beyond the first
        float shelfLife = DishItem.BASE_SHELF_LIFE;
        if (foods.stream().anyMatch(food -> food.is(ModTags.PERISHABLE))) {
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
        new DishContents(foods.stream().map(ItemStack::getItem).toList(), spices).set(dish);
        new Freshness(Freshness.UNSTAMPED,
                PantryApiImpl.INSTANCE.modifyShelfLife(foods, spices, (long) shelfLife)).set(dish);
        DishItem.setFood(dish, nutrition, saturation, Math.round(eatSeconds * 20F));
        return dish;
    }

    @Nullable
    private static Parsed parse(CraftingContainer input) {
        int bowls = 0;
        List<ItemStack> foods = new ArrayList<>();
        List<Spice> spices = new ArrayList<>();
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            Spice spice = PantryApiImpl.INSTANCE.spiceOf(stack).map(Spice.class::cast).orElse(null);
            if (stack.is(Items.BOWL)) {
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
