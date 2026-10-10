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
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Everything the mod registers. Fabric has no deferred registers: entries go straight into the
 * vanilla registries when this class loads, which {@link StevesPantry#onInitialize} triggers.
 * There are no data component types on 1.20.1; dish data lives in item NBT (see {@code component}).
 */
public final class ModRegistries {
    private static final String MODID = Reference.MODID;

    public static final Map<Spice, SpiceItem> SPICES = new EnumMap<>(Spice.class);

    static {
        for (Spice spice : Spice.values()) {
            SPICES.put(spice, item(spice.id(), new SpiceItem(spice, new Item.Properties())));
        }
    }

    public static final MortarAndPestleItem MORTAR_AND_PESTLE = item("mortar_and_pestle",
            new MortarAndPestleItem(new Item.Properties().durability(128)));

    public static final DishItem DISH = item("dish",
            new DishItem(new Item.Properties().stacksTo(1).food(DishItem.PLACEHOLDER_FOOD)));

    public static final Item SPOILED_LEFTOVERS = item("spoiled_leftovers",
            new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(1)
                    .saturationMod(0.1F)
                    .effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8F)
                    .effect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0), 0.3F)
                    .build())));

    public static final IceboxBlock ICEBOX = Registry.register(BuiltInRegistries.BLOCK, id("icebox"),
            new IceboxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)));
    public static final BlockItem ICEBOX_ITEM = item("icebox", new BlockItem(ICEBOX, new Item.Properties()));
    public static final BlockEntityType<IceboxBlockEntity> ICEBOX_BLOCK_ENTITY =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("icebox"),
                    BlockEntityType.Builder.of(IceboxBlockEntity::new, ICEBOX).build(null));
    public static final MenuType<IceboxMenu> ICEBOX_MENU = Registry.register(BuiltInRegistries.MENU, id("icebox"),
            new MenuType<>(IceboxMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final SimpleCraftingRecipeSerializer<DishAssemblyRecipe> DISH_ASSEMBLY =
            Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("dish_assembly"),
                    new SimpleCraftingRecipeSerializer<>(DishAssemblyRecipe::new));

    public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("pantry"),
            FabricItemGroup.builder()
                    .title(Component.translatable("itemGroup." + MODID))
                    .icon(() -> new ItemStack(SPICES.get(Spice.CINNAMON)))
                    .displayItems((params, output) -> {
                        output.accept(MORTAR_AND_PESTLE);
                        output.accept(ICEBOX_ITEM);
                        SPICES.values().forEach(output::accept);
                        output.accept(SPOILED_LEFTOVERS);
                    })
                    .build());

    private ModRegistries() {}

    private static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    private static <T extends Item> T item(String path, T item) {
        return Registry.register(BuiltInRegistries.ITEM, id(path), item);
    }

    /** Loading this class registers everything above; called once from the main entrypoint. */
    static void register() {}
}
