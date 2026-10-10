package com.naverene.stevespantry.event;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.Spice;
import net.minecraft.entity.merchant.villager.VillagerTrades;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MerchantOffer;
import net.minecraftforge.event.village.WandererTradesEvent;

/**
 * The wandering trader doubles as a spice merchant, so the rare spices (saffron, cumin, turmeric,
 * cardamom, clove, star anise) are obtainable even when no installed mod grows them.
 */
public final class SpiceTrades {
    private SpiceTrades() {}

    public static void onWandererTrades(WandererTradesEvent event) {
        for (Spice spice : Spice.values()) {
            int price = spice.rare() ? 3 : 1;
            int count = spice.rare() ? 2 : 4;
            VillagerTrades.ITrade listing = (trader, random) ->
                    new MerchantOffer(new ItemStack(Items.EMERALD, price),
                            new ItemStack(ModRegistries.SPICES.get(spice).get(), count), 8, 1, 0.05F);
            (spice.rare() ? event.getRareTrades() : event.getGenericTrades()).add(listing);
        }
    }
}
