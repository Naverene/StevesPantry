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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistries {
    private static final String MODID = Reference.MODID;

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final Map<Spice, RegistryObject<SpiceItem>> SPICES = new EnumMap<>(Spice.class);

    static {
        for (Spice spice : Spice.values()) {
            SPICES.put(spice, ITEMS.register(spice.id(), () -> new SpiceItem(spice, new Item.Properties())));
        }
    }

    public static final RegistryObject<MortarAndPestleItem> MORTAR_AND_PESTLE = ITEMS.register("mortar_and_pestle",
            () -> new MortarAndPestleItem(new Item.Properties().durability(128)));

    // The base food only makes the dish edible; the real numbers come from each stack's NBT (see DishItem).
    public static final RegistryObject<DishItem> DISH = ITEMS.register("dish",
            () -> new DishItem(new Item.Properties().stacksTo(1)
                    .food(new FoodProperties.Builder().nutrition(1).saturationMod(0.1F).build())));

    public static final RegistryObject<Item> SPOILED_LEFTOVERS = ITEMS.register("spoiled_leftovers",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(1)
                    .saturationMod(0.1F)
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8F)
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 200, 0), 0.3F)
                    .build())));

    public static final RegistryObject<IceboxBlock> ICEBOX = BLOCKS.register("icebox",
            () -> new IceboxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)));
    public static final RegistryObject<BlockItem> ICEBOX_ITEM = ITEMS.register("icebox",
            () -> new BlockItem(ICEBOX.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<IceboxBlockEntity>> ICEBOX_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("icebox",
                    () -> BlockEntityType.Builder.of(IceboxBlockEntity::new, ICEBOX.get()).build(null));
    public static final RegistryObject<MenuType<IceboxMenu>> ICEBOX_MENU =
            MENUS.register("icebox", () -> new MenuType<>(IceboxMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<SimpleCraftingRecipeSerializer<DishAssemblyRecipe>>
            DISH_ASSEMBLY = RECIPE_SERIALIZERS.register("dish_assembly",
                    () -> new SimpleCraftingRecipeSerializer<>(DishAssemblyRecipe::new));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("pantry",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID))
                    .icon(() -> new ItemStack(SPICES.get(Spice.CINNAMON).get()))
                    .displayItems((params, output) -> {
                        output.accept(MORTAR_AND_PESTLE.get());
                        output.accept(ICEBOX_ITEM.get());
                        SPICES.values().forEach(spice -> output.accept(spice.get()));
                        output.accept(SPOILED_LEFTOVERS.get());
                    })
                    .build());

    private ModRegistries() {}

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        TABS.register(modBus);
    }
}
