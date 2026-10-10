package com.naverene.stevespantry.event;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.Spice;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

/**
 * 1.12.2 has no wandering trader, so the farmer doubles as a spice merchant instead: once a farmer
 * levels up he sells one common spice, and at the next level one rare one (saffron, cumin, turmeric,
 * cardamom, clove, star anise), so they're obtainable even when no installed mod grows them.
 * Prices match main's wandering trader.
 */
public final class SpiceTrades {
    private SpiceTrades() {}

    public static void registerVillagerTrades() {
        VillagerRegistry.VillagerProfession farmer =
                ForgeRegistries.VILLAGER_PROFESSIONS.getValue(new ResourceLocation("minecraft", "farmer"));
        if (farmer == null) {
            return;
        }
        VillagerRegistry.VillagerCareer career = farmer.getCareer(0);
        career.addTrade(2, new SpiceTrade(false));
        career.addTrade(3, new SpiceTrade(true));
    }

    /** Sells a random spice from the common or the rare pool. */
    private static final class SpiceTrade implements EntityVillager.ITradeList {
        private final boolean rare;

        SpiceTrade(boolean rare) {
            this.rare = rare;
        }

        @Override
        public void addMerchantRecipe(IMerchant merchant, MerchantRecipeList recipes, Random random) {
            List<Spice> pool = new ArrayList<>();
            for (Spice spice : Spice.values()) {
                if (spice.rare() == rare) {
                    pool.add(spice);
                }
            }
            Spice spice = pool.get(random.nextInt(pool.size()));
            int price = spice.rare() ? 3 : 1;
            int count = spice.rare() ? 2 : 4;
            recipes.add(new MerchantRecipe(new ItemStack(Items.EMERALD, price), ItemStack.EMPTY,
                    new ItemStack(ModRegistries.SPICES.get(spice), count), 0, 8));
        }
    }
}
