package com.naverene.stevespantry.event;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.Spice;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * The wandering trader doubles as a spice merchant, so the rare spices (saffron, cumin, turmeric,
 * cardamom, clove, star anise) are obtainable even when no installed mod grows them.
 */
public final class SpiceTrades {
    /** Fabric's wandering trader pools: 1 is the generic list, 2 the rare one. */
    private static final int GENERIC = 1;
    private static final int RARE = 2;

    private SpiceTrades() {}

    public static void register() {
        for (Spice spice : Spice.values()) {
            int price = spice.rare() ? 3 : 1;
            int count = spice.rare() ? 2 : 4;
            VillagerTrades.ItemListing listing = (trader, random) ->
                    new MerchantOffer(new ItemStack(Items.EMERALD, price),
                            new ItemStack(ModRegistries.SPICES.get(spice), count), 8, 1, 0.05F);
            TradeOfferHelper.registerWanderingTraderOffers(spice.rare() ? RARE : GENERIC, trades -> trades.add(listing));
        }
    }
}
