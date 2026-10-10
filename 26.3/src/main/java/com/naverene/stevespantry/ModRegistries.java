package com.naverene.stevespantry;

import com.naverene.stevespantry.block.IceboxBlock;
import com.naverene.stevespantry.block.IceboxBlockEntity;
import com.naverene.stevespantry.block.IceboxBlockItem;
import com.naverene.stevespantry.component.DishContents;
import com.naverene.stevespantry.component.Freshness;
import com.naverene.stevespantry.item.DishItem;
import com.naverene.stevespantry.item.MortarAndPestleItem;
import com.naverene.stevespantry.item.SpiceItem;
import com.naverene.stevespantry.menu.IceboxMenu;
import com.naverene.stevespantry.recipe.DishAssemblyRecipe;
import java.util.EnumMap;
import java.util.Map;

import com.naverene.stevespantry.reference.Reference;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
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
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
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
            SPICES.put(spice, ITEMS.registerItem(spice.id(), props -> new SpiceItem(spice, props)));
        }
    }

    public static final DeferredItem<MortarAndPestleItem> MORTAR_AND_PESTLE = ITEMS.registerItem("mortar_and_pestle",
            MortarAndPestleItem::new, props -> props.durability(128));

    // Eating any dish hands back the bowl; its food and eat time come from the stack (see DishAssemblyRecipe).
    public static final DeferredItem<DishItem> DISH = ITEMS.registerItem("dish",
            DishItem::new, props -> props.stacksTo(1).usingConvertsTo(Items.BOWL));

    public static final DeferredItem<Item> SPOILED_LEFTOVERS = ITEMS.registerSimpleItem("spoiled_leftovers",
            props -> props.food(new FoodProperties.Builder()
                            .nutrition(1)
                            .saturationModifier(0.1F)
                            .build(),
                    Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8F))
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0), 0.3F))
                            .build()));

    public static final DeferredBlock<IceboxBlock> ICEBOX = BLOCKS.registerBlock("icebox",
            IceboxBlock::new, props -> props
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD));
    public static final DeferredItem<BlockItem> ICEBOX_ITEM = ITEMS.registerItem("icebox",
            props -> new IceboxBlockItem(ICEBOX.get(), props), Item.Properties::useBlockDescriptionPrefix);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IceboxBlockEntity>> ICEBOX_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("icebox",
                    () -> new BlockEntityType<>(IceboxBlockEntity::new, ICEBOX.get()));
    public static final DeferredHolder<MenuType<?>, MenuType<IceboxMenu>> ICEBOX_MENU =
            MENUS.register("icebox", () -> new MenuType<>(IceboxMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DishAssemblyRecipe>>
            DISH_ASSEMBLY = RECIPE_SERIALIZERS.register("dish_assembly",
                    () -> new RecipeSerializer<>(DishAssemblyRecipe.MAP_CODEC, DishAssemblyRecipe.STREAM_CODEC));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("pantry",
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
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        TABS.register(modBus);
    }
}
