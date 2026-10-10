package com.naverene.stevespantry.item;

import com.naverene.stevespantry.reference.Reference;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

/**
 * What a dish turns into once it spoils. 1.7.10's {@link ItemFood} only takes one chance effect,
 * so the two (likely Hunger, maybe Nausea) are rolled here.
 */
public class SpoiledLeftoversItem extends ItemFood {
    public SpoiledLeftoversItem() {
        super(1, 0.1F, false);
        setUnlocalizedName(Reference.MODID + ".spoiled_leftovers");
        setTextureName(Reference.MODID + ":spoiled_leftovers");
    }

    @Override
    protected void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return;
        }
        if (world.rand.nextFloat() < 0.8F) {
            player.addPotionEffect(new PotionEffect(Potion.hunger.id, 600, 0));
        }
        if (world.rand.nextFloat() < 0.3F) {
            player.addPotionEffect(new PotionEffect(Potion.confusion.id, 200, 0));
        }
    }
}
