package com.naverene.stevespantry.event;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.Spice;
import cpw.mods.fml.common.registry.VillagerRegistry;
import java.util.Random;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;

/**
 * 1.7.10 has no wandering trader, so the farmer villager doubles as a spice merchant instead, making
 * the rare spices (saffron, cumin, turmeric, cardamom, clove, star anise) obtainable even when no
 * installed mod grows them. Prices match main's wanderer: 1 emerald for 4, or 3 emeralds for 2 rare.
 */
public final class SpiceTrades implements VillagerRegistry.IVillageTradeHandler {
    private static final int FARMER = 0;
    /** Each time a farmer rolls new trades, each spice has this chance of being a candidate, like vanilla's. */
    private static final float GENERIC_CHANCE = 0.25F;
    private static final float RARE_CHANCE = 0.1F;

    private SpiceTrades() {}

    public static void register() {
        VillagerRegistry.instance().registerVillageTradeHandler(FARMER, new SpiceTrades());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void manipulateTradesForVillager(EntityVillager villager, MerchantRecipeList recipes, Random random) {
        for (Spice spice : Spice.values()) {
            if (random.nextFloat() >= (spice.rare() ? RARE_CHANCE : GENERIC_CHANCE)) {
                continue;
            }
            int price = spice.rare() ? 3 : 1;
            int count = spice.rare() ? 2 : 4;
            recipes.add(new MerchantRecipe(new ItemStack(Items.emerald, price),
                    new ItemStack(ModRegistries.SPICES.get(spice), count)));
        }
    }
}
