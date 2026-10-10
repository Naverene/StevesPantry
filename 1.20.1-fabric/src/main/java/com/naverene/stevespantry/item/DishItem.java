package com.naverene.stevespantry.item;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.ModTags;
import com.naverene.stevespantry.PantryClock;
import com.naverene.stevespantry.Spice;
import com.naverene.stevespantry.reference.Reference;
import com.naverene.stevespantry.component.DishContents;
import com.naverene.stevespantry.component.Freshness;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A meal the player assembles, the way Tinkers' Construct assembles tools from parts: the stack
 * carries its own ingredients, spices and nutrition (see {@code DishAssemblyRecipe}) instead of being
 * one fixed item. It keeps for a while, goes stale, then spoils into leftovers.
 *
 * <p>1.20.1 food values belong to the item, not the stack, so the dish registers an empty
 * {@link #PLACEHOLDER_FOOD} (which makes it edible at all) and keeps its real numbers in NBT:
 * {@code nutrition}, {@code saturation} and {@code eat_ticks}. {@code FoodDataMixin} feeds the
 * player from those, and {@link #getUseDuration} reads the eat time.
 */
public class DishItem extends Item {
    /** Three in-game days. Spices like clove and garlic stretch this. */
    public static final long BASE_SHELF_LIFE = 3 * 24000L;
    /** Dishes with meat, fish, milk or eggs in them keep half as long. An icebox makes up for it. */
    public static final float PERISHABLE_MULTIPLIER = 0.5F;
    /** How long a fresh dish's spice effects last. */
    private static final int SPICE_EFFECT_TICKS = 20 * 60;

    /** Item-level food value: none. The stack's NBT holds what this particular dish is worth. */
    public static final FoodProperties PLACEHOLDER_FOOD = new FoodProperties.Builder().nutrition(0).saturationMod(0F).build();
    public static final String NUTRITION = "nutrition";
    public static final String SATURATION = "saturation";
    public static final String EAT_TICKS = "eat_ticks";

    public DishItem(Properties properties) {
        super(properties);
    }

    // ---- food values (NBT) -----------------------------------------------------------------

    public static void setFood(ItemStack stack, int nutrition, float saturation, int eatTicks) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(NUTRITION, nutrition);
        tag.putFloat(SATURATION, saturation);
        tag.putInt(EAT_TICKS, eatTicks);
    }

    public static int nutrition(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(NUTRITION);
    }

    /** Saturation points this dish restores (not the vanilla per-nutrition modifier). */
    public static float saturation(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0F : tag.getFloat(SATURATION);
    }

    /** The vanilla saturation modifier that gives {@link #saturation} points: FoodData adds nutrition * modifier * 2. */
    public static float saturationModifier(ItemStack stack) {
        int nutrition = nutrition(stack);
        return nutrition <= 0 ? 0F : saturation(stack) / (nutrition * 2F);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(EAT_TICKS) ? tag.getInt(EAT_TICKS) : super.getUseDuration(stack);
    }

    /** A dish with nothing in it (say, from /give) isn't food. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (DishContents.get(stack) == null) {
            return InteractionResultHolder.pass(stack);
        }
        return super.use(level, player, hand);
    }

    // ---- freshness -------------------------------------------------------------------------

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        stamp(stack, level);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) {
            return;
        }
        stamp(stack, level);
        Freshness freshness = Freshness.get(stack);
        if (freshness != null && freshness.stage(level.getGameTime()) == Freshness.Stage.SPOILED
                && entity instanceof Player player) {
            spoil(stack, player, slot);
        }
    }

    /** Starts the clock the first time the dish is in a world (crafting has no access to one). */
    private static void stamp(ItemStack stack, Level level) {
        Freshness freshness = Freshness.get(stack);
        if (freshness != null && !freshness.stamped()) {
            new Freshness(level.getGameTime(), freshness.shelfLife()).set(stack);
        }
    }

    private static void spoil(ItemStack stack, Player player, int slot) {
        ItemStack leftovers = new ItemStack(ModRegistries.SPOILED_LEFTOVERS, stack.getCount());
        // inventoryTick's slot index is relative to whichever compartment the stack is in, so find it by identity.
        if (player.getInventory().getItem(slot) == stack) {
            player.getInventory().setItem(slot, leftovers);
        } else if (player.getOffhandItem() == stack) {
            player.setItemInHand(InteractionHand.OFF_HAND, leftovers);
        }
    }

    public static Freshness.Stage stage(ItemStack stack, long now) {
        Freshness freshness = Freshness.get(stack);
        return freshness == null ? Freshness.Stage.FRESH : freshness.stage(now);
    }

    // ---- eating ----------------------------------------------------------------------------

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        DishContents contents = DishContents.get(stack);
        Freshness.Stage stage = stage(stack, level.getGameTime());
        if (!level.isClientSide && contents != null) {
            switch (stage) {
                case FRESH -> {
                    for (Spice spice : contents.spices()) {
                        MobEffect effect = spice.effect();
                        if (effect != null) {
                            eater.addEffect(new MobEffectInstance(effect, SPICE_EFFECT_TICKS, 0));
                        }
                    }
                }
                case STALE -> { }
                case SPOILED -> {
                    eater.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 0));
                    eater.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
                }
            }
        }
        // Vanilla eats it (FoodDataMixin supplies this stack's nutrition); then hand back the bowl, like stews.
        ItemStack result = super.finishUsingItem(stack, level, eater);
        return eater instanceof Player player && player.getAbilities().instabuild ? result : new ItemStack(Items.BOWL);
    }

    // ---- display ---------------------------------------------------------------------------

    @Override
    public Component getName(ItemStack stack) {
        DishContents contents = DishContents.get(stack);
        if (contents == null || contents.ingredients().isEmpty()) {
            return super.getName(stack);
        }
        List<Item> parts = contents.ingredients().stream().distinct().toList();
        String base = "item." + Reference.MODID + ".dish.";
        MutableComponent name = switch (parts.size()) {
            case 1 -> Component.translatable(base + "one", parts.get(0).getDescription());
            case 2 -> Component.translatable(base + "two", parts.get(0).getDescription(), parts.get(1).getDescription());
            default -> Component.translatable(base + "medley", parts.get(0).getDescription());
        };
        return contents.spices().isEmpty() ? name : Component.translatable(base + "spiced", name);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        DishContents contents = DishContents.get(stack);
        if (contents == null) {
            return;
        }
        String key = "tooltip." + Reference.MODID + ".";
        Freshness freshness = Freshness.get(stack);
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
            tooltip.add(Component.translatable(key + "stage." + stage.name().toLowerCase(),
                    formatDays(ticksLeft)).withStyle(color));
        }
        if (isPerishable(contents)) {
            tooltip.add(Component.translatable(key + "perishable").withStyle(ChatFormatting.AQUA));
        }

        tooltip.add(Component.translatable(key + "ingredients").withStyle(ChatFormatting.GRAY));
        for (Item ingredient : contents.ingredients()) {
            tooltip.add(Component.literal("  ").append(ingredient.getDescription()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!contents.spices().isEmpty()) {
            tooltip.add(Component.translatable(key + "spices").withStyle(ChatFormatting.GRAY));
            for (Spice spice : contents.spices()) {
                tooltip.add(Component.literal("  ")
                        .append(Component.translatable(spice.translationKey()))
                        .append(Component.literal(" (" + spice.latinName() + ")").withStyle(ChatFormatting.ITALIC))
                        .withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal("    ").append(describeSpiceBonus(spice)).withStyle(ChatFormatting.BLUE));
            }
        }
    }

    public static boolean isPerishable(DishContents contents) {
        return contents.ingredients().stream().anyMatch(item -> item.getDefaultInstance().is(ModTags.PERISHABLE));
    }

    /** One line saying what a spice adds to a dish. */
    public static MutableComponent describeSpiceBonus(Spice spice) {
        String key = "tooltip." + Reference.MODID + ".bonus.";
        MobEffect effect = spice.effect();
        if (effect != null) {
            return Component.translatable(key + "effect", effect.getDisplayName());
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
        return Freshness.has(stack);
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
        @Nullable Freshness freshness = Freshness.get(stack);
        return freshness == null ? 1F : freshness.remaining(PantryClock.client.getAsLong());
    }
}
