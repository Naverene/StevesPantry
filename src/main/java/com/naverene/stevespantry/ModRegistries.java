package com.naverene.stevespantry;

import com.naverene.stevespantry.block.FreezerBusBlock;
import com.naverene.stevespantry.block.FreezerBusBlockEntity;
import com.naverene.stevespantry.block.FreezerControllerBlock;
import com.naverene.stevespantry.block.FreezerControllerBlockEntity;
import com.naverene.stevespantry.block.FreezerEnergyHatchBlock;
import com.naverene.stevespantry.block.FreezerEnergyHatchBlockEntity;
import com.naverene.stevespantry.block.IceboxBlock;
import com.naverene.stevespantry.block.IceboxBlockEntity;
import com.naverene.stevespantry.component.DishContents;
import com.naverene.stevespantry.component.Freshness;
import com.naverene.stevespantry.item.DishItem;
import com.naverene.stevespantry.item.MortarAndPestleItem;
import com.naverene.stevespantry.item.SpiceItem;
import com.naverene.stevespantry.recipe.DishAssemblyRecipe;
import java.util.EnumMap;
import java.util.Map;

import com.naverene.stevespantry.reference.Reference;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistries {
    private static final String MODID = Reference.MODID;

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DishContents>> DISH_CONTENTS =
            COMPONENTS.register("dish_contents", () -> DataComponentType.<DishContents>builder()
                    .persistent(DishContents.CODEC)
                    .networkSynchronized(DishContents.STREAM_CODEC)
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Freshness>> FRESHNESS =
            COMPONENTS.register("freshness", () -> DataComponentType.<Freshness>builder()
                    .persistent(Freshness.CODEC)
                    .networkSynchronized(Freshness.STREAM_CODEC)
                    .build());

    public static final Map<Spice, DeferredItem<SpiceItem>> SPICES = new EnumMap<>(Spice.class);

    static {
        for (Spice spice : Spice.values()) {
            SPICES.put(spice, ITEMS.register(spice.id(), () -> new SpiceItem(spice, new Item.Properties())));
        }
    }

    public static final DeferredItem<MortarAndPestleItem> MORTAR_AND_PESTLE = ITEMS.register("mortar_and_pestle",
            () -> new MortarAndPestleItem(new Item.Properties().durability(128)));

    public static final DeferredItem<DishItem> DISH = ITEMS.register("dish",
            () -> new DishItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> SPOILED_LEFTOVERS = ITEMS.registerSimpleItem("spoiled_leftovers",
            new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(1)
                    .saturationModifier(0.1F)
                    .effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8F)
                    .effect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0), 0.3F)
                    .build()));

    public static final DeferredBlock<IceboxBlock> ICEBOX = BLOCKS.register("icebox",
            () -> new IceboxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> ICEBOX_ITEM = ITEMS.registerSimpleBlockItem(ICEBOX);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IceboxBlockEntity>> ICEBOX_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("icebox",
                    () -> BlockEntityType.Builder.of(IceboxBlockEntity::new, ICEBOX.get()).build(null));

    // Walk-in freezer multiblock. Casings are plain blocks; only the controller and the hatches
    // carry block entities, because they hold items or energy.
    public static final DeferredBlock<Block> FREEZER_CASING = BLOCKS.registerSimpleBlock("freezer_casing", freezerMetal());
    public static final DeferredItem<BlockItem> FREEZER_CASING_ITEM = ITEMS.registerSimpleBlockItem(FREEZER_CASING);
    public static final DeferredBlock<FreezerControllerBlock> FREEZER_CONTROLLER = BLOCKS.register("freezer_controller",
            () -> new FreezerControllerBlock(freezerMetal()));
    public static final DeferredItem<BlockItem> FREEZER_CONTROLLER_ITEM = ITEMS.registerSimpleBlockItem(FREEZER_CONTROLLER);
    public static final DeferredBlock<FreezerBusBlock> FREEZER_INPUT_BUS = BLOCKS.register("freezer_input_bus",
            () -> new FreezerBusBlock(freezerMetal(), false));
    public static final DeferredItem<BlockItem> FREEZER_INPUT_BUS_ITEM = ITEMS.registerSimpleBlockItem(FREEZER_INPUT_BUS);
    public static final DeferredBlock<FreezerBusBlock> FREEZER_OUTPUT_BUS = BLOCKS.register("freezer_output_bus",
            () -> new FreezerBusBlock(freezerMetal(), true));
    public static final DeferredItem<BlockItem> FREEZER_OUTPUT_BUS_ITEM = ITEMS.registerSimpleBlockItem(FREEZER_OUTPUT_BUS);
    public static final DeferredBlock<FreezerEnergyHatchBlock> FREEZER_ENERGY_HATCH = BLOCKS.register("freezer_energy_hatch",
            () -> new FreezerEnergyHatchBlock(freezerMetal()));
    public static final DeferredItem<BlockItem> FREEZER_ENERGY_HATCH_ITEM = ITEMS.registerSimpleBlockItem(FREEZER_ENERGY_HATCH);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FreezerControllerBlockEntity>>
            FREEZER_CONTROLLER_BLOCK_ENTITY = BLOCK_ENTITIES.register("freezer_controller",
                    () -> BlockEntityType.Builder.of(FreezerControllerBlockEntity::new, FREEZER_CONTROLLER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FreezerBusBlockEntity>>
            FREEZER_BUS_BLOCK_ENTITY = BLOCK_ENTITIES.register("freezer_bus",
                    () -> BlockEntityType.Builder.of(FreezerBusBlockEntity::new,
                            FREEZER_INPUT_BUS.get(), FREEZER_OUTPUT_BUS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FreezerEnergyHatchBlockEntity>>
            FREEZER_ENERGY_HATCH_BLOCK_ENTITY = BLOCK_ENTITIES.register("freezer_energy_hatch",
                    () -> BlockEntityType.Builder.of(FreezerEnergyHatchBlockEntity::new, FREEZER_ENERGY_HATCH.get()).build(null));

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<DishAssemblyRecipe>>
            DISH_ASSEMBLY = RECIPE_SERIALIZERS.register("dish_assembly",
                    () -> new SimpleCraftingRecipeSerializer<>(DishAssemblyRecipe::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("pantry",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID))
                    .icon(() -> new ItemStack(SPICES.get(Spice.CINNAMON).get()))
                    .displayItems((params, output) -> {
                        output.accept(MORTAR_AND_PESTLE.get());
                        output.accept(ICEBOX_ITEM.get());
                        output.accept(FREEZER_CONTROLLER_ITEM.get());
                        output.accept(FREEZER_CASING_ITEM.get());
                        output.accept(FREEZER_INPUT_BUS_ITEM.get());
                        output.accept(FREEZER_OUTPUT_BUS_ITEM.get());
                        output.accept(FREEZER_ENERGY_HATCH_ITEM.get());
                        SPICES.values().forEach(spice -> output.accept(spice.get()));
                        output.accept(SPOILED_LEFTOVERS.get());
                    })
                    .build());

    private ModRegistries() {}

    private static BlockBehaviour.Properties freezerMetal() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F, 6.0F)
                .sound(SoundType.METAL);
    }

    /** Lets hoppers and pipes reach the freezer's buses and cables reach its energy hatch. */
    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FREEZER_BUS_BLOCK_ENTITY.get(),
                (bus, side) -> side == null ? new InvWrapper(bus) : new SidedInvWrapper(bus, side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FREEZER_ENERGY_HATCH_BLOCK_ENTITY.get(),
                (hatch, side) -> hatch.energy());
    }

    static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(ModRegistries::registerCapabilities);
    }
}
