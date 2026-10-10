package com.naverene.stevespantry.item;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.ModTags;
import com.naverene.stevespantry.PantryClock;
import com.naverene.stevespantry.Spice;
import com.naverene.stevespantry.reference.Reference;
import com.naverene.stevespantry.component.DishContents;
import com.naverene.stevespantry.component.Freshness;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A meal the player assembles, the way Tinkers' Construct assembles tools from parts: the stack
 * carries its own ingredients, spices and nutrition (see {@code DishAssemblyRecipe}) instead of being
 * one fixed item. It keeps for a while, goes stale, then spoils into leftovers.
 */
public class DishItem extends Item {
    /** Three in-game days. Spices like clove and garlic stretch this. */
    public static final long BASE_SHELF_LIFE = 3 * 24000L;
    /** Dishes with meat, fish, milk or eggs in them keep half as long. An icebox makes up for it. */
    public static final float PERISHABLE_MULTIPLIER = 0.5F;
    /** How long a fresh dish's spice effects last. */
    private static final int SPICE_EFFECT_TICKS = 20 * 60;

    public DishItem(Properties properties) {
        super(properties);
    }

    // ---- freshness -------------------------------------------------------------------------

    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        stamp(stack, player.level());
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        stamp(stack, level);
        Freshness freshness = stack.get(ModRegistries.FRESHNESS);
        if (freshness != null && freshness.stage(level.getGameTime()) == Freshness.Stage.SPOILED
                && entity instanceof Player player) {
            spoil(stack, player);
        }
    }

    /** Starts the clock the first time the dish is in a world (crafting has no access to one). */
    private static void stamp(ItemStack stack, Level level) {
        Freshness freshness = stack.get(ModRegistries.FRESHNESS);
        if (freshness != null && !freshness.stamped()) {
            stack.set(ModRegistries.FRESHNESS, new Freshness(level.getGameTime(), freshness.shelfLife()));
        }
    }

    private static void spoil(ItemStack stack, Player player) {
        ItemStack leftovers = new ItemStack(ModRegistries.SPOILED_LEFTOVERS.get(), stack.getCount());
        // inventoryTick only says which equipment slot (if any) the stack is in, so find it by identity.
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i) == stack) {
                inventory.setItem(i, leftovers);
                return;
            }
        }
    }

    public static Freshness.Stage stage(ItemStack stack, long now) {
        Freshness freshness = stack.get(ModRegistries.FRESHNESS);
        return freshness == null ? Freshness.Stage.FRESH : freshness.stage(now);
    }

    // ---- eating ----------------------------------------------------------------------------

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        DishContents contents = stack.get(ModRegistries.DISH_CONTENTS);
        Freshness.Stage stage = stage(stack, level.getGameTime());
        if (!level.isClientSide() && contents != null) {
            switch (stage) {
                case FRESH -> {
                    for (Spice spice : contents.spices()) {
                        Holder<MobEffect> effect = spice.effect();
                        if (effect != null) {
                            eater.addEffect(new MobEffectInstance(effect, SPICE_EFFECT_TICKS, 0));
                        }
                    }
                }
                case STALE -> { }
                case SPOILED -> {
                    eater.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 0));
                    eater.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
                }
            }
        }
        // Vanilla handles nutrition and hands back the bowl, from the stack's food, consumable and use remainder components.
        return super.finishUsingItem(stack, level, eater);
    }

    // ---- display ---------------------------------------------------------------------------

    @Override
    public Component getName(ItemStack stack) {
        DishContents contents = stack.get(ModRegistries.DISH_CONTENTS);
        if (contents == null || contents.ingredients().isEmpty()) {
            return super.getName(stack);
        }
        List<Item> parts = contents.ingredients().stream().distinct().toList();
        String base = "item." + Reference.MODID + ".dish.";
        MutableComponent name = switch (parts.size()) {
            case 1 -> Component.translatable(base + "one", nameOf(parts.get(0)));
            case 2 -> Component.translatable(base + "two", nameOf(parts.get(0)), nameOf(parts.get(1)));
            default -> Component.translatable(base + "medley", nameOf(parts.get(0)));
        };
        return contents.spices().isEmpty() ? name : Component.translatable(base + "spiced", name);
    }

    /** An item's plain name, as its default stack would show it. */
    public static Component nameOf(Item item) {
        return item.getDefaultInstance().getItemName();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        DishContents contents = stack.get(ModRegistries.DISH_CONTENTS);
        if (contents == null) {
            return;
        }
        String key = "tooltip." + Reference.MODID + ".";
        Level level = context.level();
        Freshness freshness = stack.get(ModRegistries.FRESHNESS);
        if (freshness != null && level != null) {
            long now = level.getGameTime();
            Freshness.Stage stage = freshness.stage(now);
            ChatFormatting color = switch (stage) {
                case FRESH -> ChatFormatting.GREEN;
                case STALE -> ChatFormatting.GOLD;
                case SPOILED -> ChatFormatting.DARK_RED;
            };
            long ticksLeft = freshness.stamped() ? Math.max(0, freshness.madeAt() + freshness.shelfLife() - now)
                    : freshness.shelfLife();
            tooltip.accept(Component.translatable(key + "stage." + stage.name().toLowerCase(),
                    formatDays(ticksLeft)).withStyle(color));
        }
        if (isPerishable(contents)) {
            tooltip.accept(Component.translatable(key + "perishable").withStyle(ChatFormatting.AQUA));
        }

        tooltip.accept(Component.translatable(key + "ingredients").withStyle(ChatFormatting.GRAY));
        for (Item ingredient : contents.ingredients()) {
            tooltip.accept(Component.literal("  ").append(nameOf(ingredient)).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!contents.spices().isEmpty()) {
            tooltip.accept(Component.translatable(key + "spices").withStyle(ChatFormatting.GRAY));
            for (Spice spice : contents.spices()) {
                tooltip.accept(Component.literal("  ")
                        .append(Component.translatable(spice.translationKey()))
                        .append(Component.literal(" (" + spice.latinName() + ")").withStyle(ChatFormatting.ITALIC))
                        .withStyle(ChatFormatting.DARK_GRAY));
                tooltip.accept(Component.literal("    ").append(describeSpiceBonus(spice)).withStyle(ChatFormatting.BLUE));
            }
        }
    }

    public static boolean isPerishable(DishContents contents) {
        return contents.ingredients().stream().anyMatch(item -> item.getDefaultInstance().is(ModTags.PERISHABLE));
    }

    /** One line saying what a spice adds to a dish. */
    public static MutableComponent describeSpiceBonus(Spice spice) {
        String key = "tooltip." + Reference.MODID + ".bonus.";
        Holder<MobEffect> effect = spice.effect();
        if (effect != null) {
            return Component.translatable(key + "effect", effect.value().getDisplayName());
        }
        if (spice.shelfLifeMultiplier() > 1F) {
            return Component.translatable(key + "preserves", Math.round((spice.shelfLifeMultiplier() - 1F) * 100F));
        }
        return Component.translatable(key + "saturation", spice.bonusSaturation());
    }

    private static Component formatDays(long ticks) {
        float days = ticks / 24000F;
        return Component.literal(days >= 1F ? String.format("%.1f", days) : String.format("%.2f", days));
    }

    // Freshness bar where a tool's durability bar would be.

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(ModRegistries.FRESHNESS);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * remaining(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(remaining(stack) / 3F, 1F, 1F);
    }

    private static float remaining(ItemStack stack) {
        @Nullable Freshness freshness = stack.get(ModRegistries.FRESHNESS);
        return freshness == null ? 1F : freshness.remaining(PantryClock.client.getAsLong());
    }
}
