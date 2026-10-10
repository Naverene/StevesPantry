package com.naverene.stevespantry;

import com.naverene.stevespantry.block.IceboxBlock;
import com.naverene.stevespantry.block.IceboxBlockEntity;
import com.naverene.stevespantry.item.DishItem;
import com.naverene.stevespantry.item.MortarAndPestleItem;
import com.naverene.stevespantry.item.SpiceItem;
import com.naverene.stevespantry.menu.IceboxMenu;
import com.naverene.stevespantry.recipe.DishAssemblyRecipe;
import java.util.EnumMap;
import java.util.Map;

import com.naverene.stevespantry.reference.Reference;
import net.minecraft.core.NonNullList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Everything the mod registers. There are no data component types on 1.19.2; dish data lives in
 * item NBT (see {@code component}).
 */
public final class ModRegistries {
    private static final String MODID = Reference.MODID;

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    /** 1.19.2 creative tabs aren't registered: subclassing {@link CreativeModeTab} adds one. */
    public static final CreativeModeTab TAB = new CreativeModeTab(MODID) {
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

    public static final Map<Spice, RegistryObject<SpiceItem>> SPICES = new EnumMap<>(Spice.class);

    static {
        for (Spice spice : Spice.values()) {
            SPICES.put(spice, ITEMS.register(spice.id(), () -> new SpiceItem(spice, new Item.Properties().tab(TAB))));
        }
    }

    public static final RegistryObject<MortarAndPestleItem> MORTAR_AND_PESTLE = ITEMS.register("mortar_and_pestle",
            () -> new MortarAndPestleItem(new Item.Properties().durability(128).tab(TAB)));

    public static final RegistryObject<DishItem> DISH = ITEMS.register("dish",
            () -> new DishItem(new Item.Properties().stacksTo(1).food(DishItem.PLACEHOLDER_FOOD)));

    public static final RegistryObject<Item> SPOILED_LEFTOVERS = ITEMS.register("spoiled_leftovers",
            () -> new Item(new Item.Properties().tab(TAB).food(new FoodProperties.Builder()
                    .nutrition(1)
                    .saturationMod(0.1F)
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8F)
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 200, 0), 0.3F)
                    .build())));

    public static final RegistryObject<IceboxBlock> ICEBOX = BLOCKS.register("icebox",
            () -> new IceboxBlock(BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)));
    public static final RegistryObject<BlockItem> ICEBOX_ITEM = ITEMS.register("icebox",
            () -> new BlockItem(ICEBOX.get(), new Item.Properties().tab(TAB)));
    public static final RegistryObject<BlockEntityType<IceboxBlockEntity>> ICEBOX_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("icebox",
                    () -> BlockEntityType.Builder.of(IceboxBlockEntity::new, ICEBOX.get()).build(null));
    public static final RegistryObject<MenuType<IceboxMenu>> ICEBOX_MENU =
            MENUS.register("icebox", () -> new MenuType<>(IceboxMenu::new));

    public static final RegistryObject<SimpleRecipeSerializer<DishAssemblyRecipe>> DISH_ASSEMBLY =
            RECIPE_SERIALIZERS.register("dish_assembly", () -> new SimpleRecipeSerializer<>(DishAssemblyRecipe::new));

    private ModRegistries() {}

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
    }
}
