package com.naverene.stevespantry;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * 1.12.2 has no item tags, so the mod's item groups are Ore Dictionary names instead. Pam's
 * HarvestCraft 1.12.2 registers its crops as {@code cropGinger}, {@code cropPeppercorn} and so on,
 * and its meat, fish, milk and eggs under {@code listAll...} names; other mods use the same names.
 */
public final class ModTags {
    /** Foods that go off quickly unless kept cold: meat, fish, milk and eggs. */
    public static final List<String> PERISHABLE = Collections.unmodifiableList(Arrays.asList(
            "listAllmeatraw", "listAllmeatcooked",
            "listAllfishraw", "listAllfishcooked", "listAllfishfresh",
            "listAllmilk", "listAllheavycream", "listAllegg",
            "foodMilk", "egg"));

    /** Vanilla perishables, for when no mod fills the ore names above. */
    private static final Set<Item> PERISHABLE_ITEMS = new HashSet<>(Arrays.asList(
            Items.BEEF, Items.COOKED_BEEF, Items.PORKCHOP, Items.COOKED_PORKCHOP,
            Items.CHICKEN, Items.COOKED_CHICKEN, Items.MUTTON, Items.COOKED_MUTTON,
            Items.RABBIT, Items.COOKED_RABBIT, Items.RABBIT_STEW,
            Items.FISH, Items.COOKED_FISH, Items.MILK_BUCKET, Items.EGG));

    /**
     * What each spice is ground from: the HarvestCraft 1.12.2 ore name first (where there is one),
     * then the names other mods use. A grind recipe is only added for a spice when one of these has items.
     */
    public static final Map<Spice, List<String>> SPICE_SOURCES = new EnumMap<>(Spice.class);

    static {
        sources(Spice.BLACK_PEPPER, "cropPeppercorn", "cropBlackpepper", "spiceBlackpepper");
        sources(Spice.CINNAMON, "cropCinnamon", "spiceCinnamon");
        sources(Spice.NUTMEG, "cropNutmeg", "spiceNutmeg");
        sources(Spice.VANILLA, "cropVanillabean", "cropVanilla", "spiceVanilla");
        sources(Spice.GINGER, "cropGinger", "spiceGinger");
        sources(Spice.MUSTARD, "cropMustard", "seedMustard", "cropMustardseeds", "spiceMustard");
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

    private static void sources(Spice spice, String... oreNames) {
        SPICE_SOURCES.put(spice, Collections.unmodifiableList(Arrays.asList(oreNames)));
    }

    public static boolean isPerishable(ItemStack stack) {
        if (stack.isEmpty()) {
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
