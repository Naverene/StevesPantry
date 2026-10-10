package com.naverene.stevespantry;

import com.naverene.stevespantry.block.IceboxBlock;
import com.naverene.stevespantry.block.IceboxBlockEntity;
import com.naverene.stevespantry.item.DishItem;
import com.naverene.stevespantry.item.MortarAndPestleItem;
import com.naverene.stevespantry.item.SpiceItem;
import com.naverene.stevespantry.menu.IceboxMenu;
import com.naverene.stevespantry.recipe.DishAssemblyRecipe;
import com.naverene.stevespantry.reference.Reference;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModRegistries {
    private static final String MODID = Reference.MODID;

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<TileEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, MODID);
    public static final DeferredRegister<IRecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);
    public static final DeferredRegister<ContainerType<?>> MENUS = DeferredRegister.create(ForgeRegistries.CONTAINERS, MODID);

    public static final Map<Spice, RegistryObject<SpiceItem>> SPICES = new EnumMap<>(Spice.class);

    /**
     * 1.16.5 has no creative tab registry: a tab is an {@link ItemGroup} subclass, and items join it
     * through {@link Item.Properties#tab}. {@link ItemGroup#fillItemList} is overridden to keep main's order.
     */
    public static final ItemGroup TAB = new ItemGroup(MODID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(SPICES.get(Spice.CINNAMON).get());
        }

        @Override
        public void fillItemList(NonNullList<ItemStack> items) {
            items.add(new ItemStack(MORTAR_AND_PESTLE.get()));
            items.add(new ItemStack(ICEBOX_ITEM.get()));
            SPICES.values().forEach(spice -> items.add(new ItemStack(spice.get())));
            items.add(new ItemStack(SPOILED_LEFTOVERS.get()));
        }
    };

    static {
        for (Spice spice : Spice.values()) {
            SPICES.put(spice, ITEMS.register(spice.id(), () -> new SpiceItem(spice, new Item.Properties().tab(TAB))));
        }
    }

    public static final RegistryObject<MortarAndPestleItem> MORTAR_AND_PESTLE = ITEMS.register("mortar_and_pestle",
            () -> new MortarAndPestleItem(new Item.Properties().durability(128).tab(TAB)));

    // The base food only makes the dish edible and restores nothing; the real numbers come from each
    // stack's NBT (see DishItem).
    public static final RegistryObject<DishItem> DISH = ITEMS.register("dish",
            () -> new DishItem(new Item.Properties().stacksTo(1)
                    .food(new Food.Builder().nutrition(0).saturationMod(0F).build())));

    public static final RegistryObject<Item> SPOILED_LEFTOVERS = ITEMS.register("spoiled_leftovers",
            () -> new Item(new Item.Properties().tab(TAB).food(new Food.Builder()
                    .nutrition(1)
                    .saturationMod(0.1F)
                    .effect(() -> new EffectInstance(Effects.HUNGER, 600, 0), 0.8F)
                    .effect(() -> new EffectInstance(Effects.CONFUSION, 200, 0), 0.3F)
                    .build())));

    public static final RegistryObject<IceboxBlock> ICEBOX = BLOCKS.register("icebox",
            () -> new IceboxBlock(AbstractBlock.Properties.of(Material.WOOD, MaterialColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
                    // No mineable/axe tag before 1.17; Forge's harvest tool does that job.
                    .harvestTool(ToolType.AXE)));
    public static final RegistryObject<BlockItem> ICEBOX_ITEM = ITEMS.register("icebox",
            () -> new BlockItem(ICEBOX.get(), new Item.Properties().tab(TAB)));
    public static final RegistryObject<TileEntityType<IceboxBlockEntity>> ICEBOX_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("icebox",
                    () -> TileEntityType.Builder.of(IceboxBlockEntity::new, ICEBOX.get()).build(null));
    public static final RegistryObject<ContainerType<IceboxMenu>> ICEBOX_MENU =
            MENUS.register("icebox", () -> new ContainerType<>(IceboxMenu::new));

    public static final RegistryObject<SpecialRecipeSerializer<DishAssemblyRecipe>>
            DISH_ASSEMBLY = RECIPE_SERIALIZERS.register("dish_assembly",
                    () -> new SpecialRecipeSerializer<>(DishAssemblyRecipe::new));

    private ModRegistries() {}

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
    }
}
