package com.naverene.stevespantry.mixin;

import com.naverene.stevespantry.item.DishItem;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * On 1.20.1 vanilla feeds the player from {@code Item#getFoodProperties()}, which can't see the
 * stack. A dish's nutrition and saturation are per stack (in its NBT), so this swaps them in at
 * the one place eating restores hunger.
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @Inject(method = "eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true)
    private void stevespantry$eatDish(Item item, ItemStack stack, CallbackInfo ci) {
        if (item instanceof DishItem) {
            ((FoodData) (Object) this).eat(DishItem.nutrition(stack), DishItem.saturationModifier(stack));
            ci.cancel();
        }
    }
}
