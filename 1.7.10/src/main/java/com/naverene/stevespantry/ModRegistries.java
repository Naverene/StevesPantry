package com.naverene.stevespantry;

import com.naverene.stevespantry.block.IceboxBlock;
import com.naverene.stevespantry.block.IceboxBlockEntity;
import com.naverene.stevespantry.block.IceboxItem;
import com.naverene.stevespantry.item.DishItem;
import com.naverene.stevespantry.item.MortarAndPestleItem;
import com.naverene.stevespantry.item.SpiceItem;
import com.naverene.stevespantry.item.SpoiledLeftoversItem;
import com.naverene.stevespantry.recipe.DishAssemblyRecipe;
import com.naverene.stevespantry.reference.Reference;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.RecipeSorter;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 * Everything the mod registers. 1.7.10 has no deferred registers, data components, menu types or
 * recipe serializers: items and blocks go straight into {@link GameRegistry} in pre-init, the menu
 * is opened through {@link CommonProxy}'s GUI handler, and recipes are added in code.
 */
public final class ModRegistries {
    private static final String MODID = Reference.MODID;

    public static final CreativeTabs TAB = new CreativeTabs(MODID) {
        @Override
        @SideOnly(Side.CLIENT)
        public Item getTabIconItem() {
            return SPICES.get(Spice.CINNAMON);
        }
    };

    public static final Map<Spice, SpiceItem> SPICES = new EnumMap<>(Spice.class);
    public static MortarAndPestleItem MORTAR_AND_PESTLE;
    public static DishItem DISH;
    public static SpoiledLeftoversItem SPOILED_LEFTOVERS;
    public static IceboxBlock ICEBOX;

    private ModRegistries() {}

    /** Items and blocks, in creative tab order: mortar, icebox, spices, leftovers. */
    static void register() {
        MORTAR_AND_PESTLE = new MortarAndPestleItem();
        MORTAR_AND_PESTLE.setCreativeTab(TAB);
        GameRegistry.registerItem(MORTAR_AND_PESTLE, "mortar_and_pestle");

        ICEBOX = new IceboxBlock();
        ICEBOX.setCreativeTab(TAB);
        GameRegistry.registerBlock(ICEBOX, IceboxItem.class, "icebox");
        GameRegistry.registerTileEntity(IceboxBlockEntity.class, MODID + ":icebox");

        for (Spice spice : Spice.values()) {
            SpiceItem item = new SpiceItem(spice);
            item.setCreativeTab(TAB);
            GameRegistry.registerItem(item, spice.id());
            SPICES.put(spice, item);
        }

        SPOILED_LEFTOVERS = new SpoiledLeftoversItem();
        SPOILED_LEFTOVERS.setCreativeTab(TAB);
        GameRegistry.registerItem(SPOILED_LEFTOVERS, "spoiled_leftovers");

        // Dishes only exist with contents, so the bare item stays out of the creative tab.
        DISH = new DishItem();
        GameRegistry.registerItem(DISH, "dish");
    }

    static void registerRecipes() {
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(MORTAR_AND_PESTLE),
                "  S", "B B", " B ", 'S', "stickWood", 'B', Blocks.stone));
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(ICEBOX),
                "PIP", "ICI", "PIP", 'P', "plankWood", 'I', "ingotIron", 'C', Blocks.packed_ice));

        RecipeSorter.register(MODID + ":dish_assembly", DishAssemblyRecipe.class,
                RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless");
        GameRegistry.addRecipe(new DishAssemblyRecipe());
    }

    /**
     * Mortar + source -> 2 spice, one recipe per ore name that something has registered by
     * post-init. Names nobody registers get no recipe, like main's empty-tag condition.
     */
    static int registerGrindRecipes() {
        int added = 0;
        ItemStack mortar = new ItemStack(MORTAR_AND_PESTLE, 1, OreDictionary.WILDCARD_VALUE);
        for (Map.Entry<Spice, List<String>> entry : ModTags.SPICE_SOURCES.entrySet()) {
            for (String oreName : entry.getValue()) {
                if (OreDictionary.doesOreNameExist(oreName) && !OreDictionary.getOres(oreName).isEmpty()) {
                    GameRegistry.addRecipe(new ShapelessOreRecipe(
                            new ItemStack(SPICES.get(entry.getKey()), 2), mortar, oreName));
                    added++;
                }
            }
        }
        return added;
    }
}
