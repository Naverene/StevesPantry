package com.naverene.stevespantry.item;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.ModTags;
import com.naverene.stevespantry.PantryClock;
import com.naverene.stevespantry.Spice;
import com.naverene.stevespantry.component.DishContents;
import com.naverene.stevespantry.component.Freshness;
import com.naverene.stevespantry.reference.Reference;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;

/**
 * A meal the player assembles, the way Tinkers' Construct assembles tools from parts: the stack
 * carries its own ingredients, spices and nutrition (see {@code DishAssemblyRecipe}) instead of being
 * one fixed item. It keeps for a while, goes stale, then spoils into leftovers.
 *
 * <p>1.16.5 has no per-stack food component, and Forge 36 has no stack-aware food hook either, so the
 * assembled nutrition, saturation and eat time are saved in the stack's NBT. The registered item's
 * food restores nothing; {@link #finishUsingItem} feeds the player the stack's own numbers, and
 * {@link #getUseDuration(ItemStack)} reads its eat time.
 */
public class DishItem extends Item {
    /** Three in-game days. Spices like clove and garlic stretch this. */
    public static final long BASE_SHELF_LIFE = 3 * 24000L;
    /** Dishes with meat, fish, milk or eggs in them keep half as long. An icebox makes up for it. */
    public static final float PERISHABLE_MULTIPLIER = 0.5F;
    /** How long a fresh dish's spice effects last. */
    private static final int SPICE_EFFECT_TICKS = 20 * 60;

    /** NBT keys for the assembled food values. Saturation is the absolute amount restored, as on main. */
    public static final String NUTRITION = "nutrition";
    public static final String SATURATION = "saturation";
    public static final String EAT_TICKS = "eat_ticks";

    public DishItem(Properties properties) {
        super(properties);
    }

    /** Writes the assembled food values onto a dish stack. */
    public static void setFood(ItemStack stack, int nutrition, float saturation, int eatTicks) {
        CompoundNBT tag = stack.getOrCreateTag();
        tag.putInt(NUTRITION, nutrition);
        tag.putFloat(SATURATION, saturation);
        tag.putInt(EAT_TICKS, eatTicks);
    }

    // ---- freshness -------------------------------------------------------------------------

    @Override
    public void onCraftedBy(ItemStack stack, World level, PlayerEntity player) {
        stamp(stack, level);
    }

    @Override
    public void inventoryTick(ItemStack stack, World level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) {
            return;
        }
        stamp(stack, level);
        Freshness freshness = Freshness.get(stack);
        if (freshness != null && freshness.stage(level.getGameTime()) == Freshness.Stage.SPOILED
                && entity instanceof PlayerEntity) {
            spoil(stack, (PlayerEntity) entity, slot);
        }
    }

    /** Starts the clock the first time the dish is in a world (crafting has no access to one). */
    private static void stamp(ItemStack stack, World level) {
        Freshness freshness = Freshness.get(stack);
        if (freshness != null && !freshness.stamped()) {
            new Freshness(level.getGameTime(), freshness.shelfLife()).set(stack);
        }
    }

    private static void spoil(ItemStack stack, PlayerEntity player, int slot) {
        ItemStack leftovers = new ItemStack(ModRegistries.SPOILED_LEFTOVERS.get(), stack.getCount());
        // inventoryTick's slot index is relative to whichever compartment the stack is in, so find it by identity.
        if (player.inventory.getItem(slot) == stack) {
            player.inventory.setItem(slot, leftovers);
        } else if (player.getOffhandItem() == stack) {
            player.setItemInHand(Hand.OFF_HAND, leftovers);
        }
    }

    public static Freshness.Stage stage(ItemStack stack, long now) {
        Freshness freshness = Freshness.get(stack);
        return freshness == null ? Freshness.Stage.FRESH : freshness.stage(now);
    }

    // ---- eating ----------------------------------------------------------------------------

    @Override
    public int getUseDuration(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        return tag != null && tag.contains(EAT_TICKS, Constants.NBT.TAG_ANY_NUMERIC) ? tag.getInt(EAT_TICKS)
                : super.getUseDuration(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World level, LivingEntity eater) {
        DishContents contents = DishContents.get(stack);
        Freshness.Stage stage = stage(stack, level.getGameTime());
        CompoundNBT tag = stack.getTag();
        if (!level.isClientSide && contents != null) {
            switch (stage) {
                case FRESH:
                    for (Spice spice : contents.spices()) {
                        Effect effect = spice.effect();
                        if (effect != null) {
                            eater.addEffect(new EffectInstance(effect, SPICE_EFFECT_TICKS, 0));
                        }
                    }
                    break;
                case STALE:
                    break;
                case SPOILED:
                    eater.addEffect(new EffectInstance(Effects.HUNGER, 600, 0));
                    eater.addEffect(new EffectInstance(Effects.CONFUSION, 200, 0));
                    break;
            }
        }
        // The stack's own nutrition; read before vanilla shrinks the stack. Vanilla restores
        // nutrition * modifier * 2 saturation, so turn the stored amount back into a modifier.
        if (eater instanceof PlayerEntity && tag != null && tag.contains(NUTRITION, Constants.NBT.TAG_ANY_NUMERIC)) {
            int nutrition = tag.getInt(NUTRITION);
            float modifier = nutrition > 0 ? tag.getFloat(SATURATION) / (nutrition * 2F) : 0F;
            ((PlayerEntity) eater).getFoodData().eat(nutrition, modifier);
        }
        // Vanilla plays the sounds and counts the stat (its own food value is zero); the bowl comes back like a stew's.
        ItemStack result = super.finishUsingItem(stack, level, eater);
        return eater instanceof PlayerEntity && ((PlayerEntity) eater).abilities.instabuild ? result : new ItemStack(Items.BOWL);
    }

    // ---- display ---------------------------------------------------------------------------

    @Override
    public ITextComponent getName(ItemStack stack) {
        DishContents contents = DishContents.get(stack);
        if (contents == null || contents.ingredients().isEmpty()) {
            return super.getName(stack);
        }
        List<Item> parts = contents.ingredients().stream().distinct().collect(Collectors.toList());
        String base = "item." + Reference.MODID + ".dish.";
        IFormattableTextComponent name;
        switch (parts.size()) {
            case 1:
                name = new TranslationTextComponent(base + "one", parts.get(0).getDescription());
                break;
            case 2:
                name = new TranslationTextComponent(base + "two", parts.get(0).getDescription(),
                        parts.get(1).getDescription());
                break;
            default:
                name = new TranslationTextComponent(base + "medley", parts.get(0).getDescription());
                break;
        }
        return contents.spices().isEmpty() ? name : new TranslationTextComponent(base + "spiced", name);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> tooltip, ITooltipFlag flag) {
        DishContents contents = DishContents.get(stack);
        if (contents == null) {
            return;
        }
        String key = "tooltip." + Reference.MODID + ".";
        Freshness freshness = Freshness.get(stack);
        if (freshness != null && level != null) {
            long now = level.getGameTime();
            Freshness.Stage stage = freshness.stage(now);
            TextFormatting color = stage == Freshness.Stage.FRESH ? TextFormatting.GREEN
                    : stage == Freshness.Stage.STALE ? TextFormatting.GOLD : TextFormatting.DARK_RED;
            long ticksLeft = freshness.stamped() ? Math.max(0, freshness.madeAt() + freshness.shelfLife() - now)
                    : freshness.shelfLife();
            tooltip.add(new TranslationTextComponent(key + "stage." + stage.name().toLowerCase(Locale.ROOT),
                    formatDays(ticksLeft)).withStyle(color));
        }
        if (isPerishable(contents)) {
            tooltip.add(new TranslationTextComponent(key + "perishable").withStyle(TextFormatting.AQUA));
        }

        tooltip.add(new TranslationTextComponent(key + "ingredients").withStyle(TextFormatting.GRAY));
        for (Item ingredient : contents.ingredients()) {
            tooltip.add(new StringTextComponent("  ").append(ingredient.getDescription()).withStyle(TextFormatting.DARK_GRAY));
        }
        if (!contents.spices().isEmpty()) {
            tooltip.add(new TranslationTextComponent(key + "spices").withStyle(TextFormatting.GRAY));
            for (Spice spice : contents.spices()) {
                tooltip.add(new StringTextComponent("  ")
                        .append(new TranslationTextComponent(spice.translationKey()))
                        .append(new StringTextComponent(" (" + spice.latinName() + ")").withStyle(TextFormatting.ITALIC))
                        .withStyle(TextFormatting.DARK_GRAY));
                tooltip.add(new StringTextComponent("    ").append(describeSpiceBonus(spice)).withStyle(TextFormatting.BLUE));
            }
        }
    }

    public static boolean isPerishable(DishContents contents) {
        return contents.ingredients().stream().anyMatch(ModTags.PERISHABLE::contains);
    }

    /** One line saying what a spice adds to a dish. */
    public static IFormattableTextComponent describeSpiceBonus(Spice spice) {
        String key = "tooltip." + Reference.MODID + ".bonus.";
        Effect effect = spice.effect();
        if (effect != null) {
            return new TranslationTextComponent(key + "effect", effect.getDisplayName());
        }
        if (spice.shelfLifeMultiplier() > 1F) {
            return new TranslationTextComponent(key + "preserves", Math.round((spice.shelfLifeMultiplier() - 1F) * 100F));
        }
        return new TranslationTextComponent(key + "saturation", spice.bonusSaturation());
    }

    private static ITextComponent formatDays(long ticks) {
        float days = ticks / 24000F;
        return new StringTextComponent(days >= 1F ? String.format("%.1f", days) : String.format("%.2f", days));
    }

    // Freshness bar where a tool's durability bar would be (Forge 36's durability-bar hooks).

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return Freshness.has(stack);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        // Forge counts this the way durability is counted: 0 is a full bar, 1 an empty one.
        return 1D - remaining(stack);
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return MathHelper.hsvToRgb(remaining(stack) / 3F, 1F, 1F);
    }

    private static float remaining(ItemStack stack) {
        @Nullable Freshness freshness = Freshness.get(stack);
        return freshness == null ? 1F : freshness.remaining(PantryClock.client.getAsLong());
    }
}
