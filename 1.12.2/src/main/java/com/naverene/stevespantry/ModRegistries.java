package com.naverene.stevespantry;

import com.naverene.stevespantry.block.IceboxBlock;
import com.naverene.stevespantry.block.IceboxBlockEntity;
import com.naverene.stevespantry.item.DishItem;
import com.naverene.stevespantry.item.MortarAndPestleItem;
import com.naverene.stevespantry.item.SpiceItem;
import com.naverene.stevespantry.recipe.DishAssemblyRecipe;
import com.naverene.stevespantry.reference.Reference;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CompoundIngredient;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.OreIngredient;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 * Everything the mod registers. 1.12.2 has no deferred registers, so the objects are built here and
 * handed to Forge from its registry events.
 */
@Mod.EventBusSubscriber(modid = Reference.MODID)
public final class ModRegistries {
    private static final String MODID = Reference.MODID;

    public static final Map<Spice, SpiceItem> SPICES = new EnumMap<>(Spice.class);

    static {
        for (Spice spice : Spice.values()) {
            SPICES.put(spice, item(new SpiceItem(spice), spice.id()));
        }
    }

    public static final MortarAndPestleItem MORTAR_AND_PESTLE = item(new MortarAndPestleItem(128), "mortar_and_pestle");

    public static final DishItem DISH = item(new DishItem(), "dish");

    public static final ItemFood SPOILED_LEFTOVERS = item(new ItemFood(1, 0.1F, false) {
        @Override
        protected void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
            if (!world.isRemote) {
                if (world.rand.nextFloat() < 0.8F) {
                    player.addPotionEffect(new PotionEffect(MobEffects.HUNGER, 600, 0));
                }
                if (world.rand.nextFloat() < 0.3F) {
                    player.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 200, 0));
                }
            }
        }
    }, "spoiled_leftovers");

    public static final IceboxBlock ICEBOX = block(new IceboxBlock(), "icebox");
    public static final ItemBlock ICEBOX_ITEM = item(new ItemBlock(ICEBOX), "icebox");

    public static final CreativeTabs TAB = new CreativeTabs(MODID) {
        @Override
        @SideOnly(Side.CLIENT)
        public ItemStack createIcon() {
            return new ItemStack(SPICES.get(Spice.CINNAMON));
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void displayAllRelevantItems(NonNullList<ItemStack> output) {
            output.add(new ItemStack(MORTAR_AND_PESTLE));
            output.add(new ItemStack(ICEBOX_ITEM));
            for (SpiceItem spice : SPICES.values()) {
                output.add(new ItemStack(spice));
            }
            output.add(new ItemStack(SPOILED_LEFTOVERS));
        }
    };

    static {
        MORTAR_AND_PESTLE.setCreativeTab(TAB);
        ICEBOX.setCreativeTab(TAB);
        SPICES.values().forEach(spice -> spice.setCreativeTab(TAB));
        SPOILED_LEFTOVERS.setCreativeTab(TAB);
    }

    private ModRegistries() {}

    private static <T extends Item> T item(T item, String id) {
        item.setRegistryName(MODID, id);
        item.setTranslationKey(MODID + "." + id);
        return item;
    }

    private static <T extends Block> T block(T block, String id) {
        block.setRegistryName(MODID, id);
        block.setTranslationKey(MODID + "." + id);
        return block;
    }

    /** Every item, in creative tab order, for model registration. */
    public static List<Item> items() {
        List<Item> items = new ArrayList<>();
        items.add(MORTAR_AND_PESTLE);
        items.add(ICEBOX_ITEM);
        items.addAll(SPICES.values());
        items.add(DISH);
        items.add(SPOILED_LEFTOVERS);
        return items;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(ICEBOX);
        GameRegistry.registerTileEntity(IceboxBlockEntity.class, new ResourceLocation(MODID, "icebox"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (Item item : items()) {
            event.getRegistry().register(item);
        }
    }

    /**
     * The dish recipe, plus a grind recipe for each spice whose source ore names have something in
     * them (the 1.12.2 stand-in for main's "tag isn't empty" condition). Runs last so every mod's
     * crops are in the Ore Dictionary by then; Pam's HarvestCraft fills it while registering items.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new DishAssemblyRecipe().setRegistryName(MODID, "dish_assembly"));

        Ingredient mortar = Ingredient.fromStacks(new ItemStack(MORTAR_AND_PESTLE, 1, OreDictionary.WILDCARD_VALUE));
        for (Map.Entry<Spice, SpiceItem> entry : SPICES.entrySet()) {
            List<Ingredient> sources = new ArrayList<>();
            for (String ore : ModTags.SPICE_SOURCES.get(entry.getKey())) {
                if (!OreDictionary.getOres(ore, false).isEmpty()) {
                    sources.add(new OreIngredient(ore));
                }
            }
            if (sources.isEmpty()) {
                continue;
            }
            Ingredient source = sources.size() == 1 ? sources.get(0) : new CompoundIngredient(sources) {};
            IRecipe grind = new ShapelessOreRecipe(new ResourceLocation(MODID, "grind"),
                    new ItemStack(entry.getValue(), 2), mortar, source);
            event.getRegistry().register(grind.setRegistryName(new ResourceLocation(MODID, "grind_" + entry.getKey().id())));
        }
    }
}
