package com.naverene.stevespantry;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * 1.7.10 has no item tags, so the lists main keeps as tags live here as Ore Dictionary names.
 * The names are the ones Pam's HarvestCraft 1.7.10 registers, plus the usual alternatives other
 * mods use, so any mod's ginger in {@code cropGinger} grinds into Ground Ginger.
 */
public final class ModTags {
    /** Foods that go off quickly unless kept cold: meat, fish, milk and eggs. */
    public static final List<String> PERISHABLE = Arrays.asList(
            "listAllmeatraw", "listAllmeatcooked", "listAllfishraw", "listAllfishcooked",
            "listAllmilk", "listAllegg", "foodMeatraw", "foodFishraw", "egg", "bucketMilk");

    /** Vanilla perishables, since the ore names above only exist when HarvestCraft is installed. */
    private static final List<Item> PERISHABLE_ITEMS = Arrays.asList(
            Items.beef, Items.cooked_beef, Items.porkchop, Items.cooked_porkchop,
            Items.chicken, Items.cooked_chicken, Items.fish, Items.cooked_fished,
            Items.rotten_flesh, Items.milk_bucket, Items.egg);

    /** What each spice is ground from, as ore names. A grind recipe is only added for names something registers. */
    public static final Map<Spice, List<String>> SPICE_SOURCES = new EnumMap<>(Spice.class);

    static {
        sources(Spice.BLACK_PEPPER, "cropPeppercorn", "cropBlackpepper", "spiceBlackpepper");
        sources(Spice.CINNAMON, "cropCinnamon", "spiceCinnamon");
        sources(Spice.NUTMEG, "cropNutmeg", "spiceNutmeg");
        sources(Spice.VANILLA, "cropVanillabean", "cropVanilla", "spiceVanilla");
        sources(Spice.GINGER, "cropGinger", "spiceGinger");
        sources(Spice.MUSTARD, "cropMustard", "seedMustard", "cropMustardseeds", "cropMustardseed", "spiceMustard");
        sources(Spice.SESAME, "cropSesame", "seedSesameseed", "cropSesameseeds", "seedSesame", "spiceSesame");
        sources(Spice.PAPRIKA, "cropBellpepper", "spicePaprika");
        sources(Spice.CHILI, "cropChilipepper", "cropChili", "spiceChili");
        sources(Spice.GARLIC, "cropGarlic", "spiceGarlic");
        sources(Spice.SAFFRON, "cropSaffron", "spiceSaffron");
        sources(Spice.CUMIN, "cropCumin", "seedCumin", "spiceCumin");
        sources(Spice.TURMERIC, "cropTurmeric", "spiceTurmeric");
        sources(Spice.CARDAMOM, "cropCardamom", "seedCardamom", "spiceCardamom");
        sources(Spice.CLOVE, "cropClove", "spiceClove");
        sources(Spice.STAR_ANISE, "cropStaranise", "cropStarAnise", "spiceStaranise");
    }

    private ModTags() {}

    private static void sources(Spice spice, String... names) {
        SPICE_SOURCES.put(spice, Collections.unmodifiableList(Arrays.asList(names)));
    }

    public static boolean isPerishable(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        if (PERISHABLE_ITEMS.contains(stack.getItem())) {
            return true;
        }
        for (int id : OreDictionary.getOreIDs(stack)) {
            if (PERISHABLE.contains(OreDictionary.getOreName(id))) {
                return true;
            }
        }
        return false;
    }
}
