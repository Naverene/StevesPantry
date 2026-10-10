package com.naverene.stevespantry.item;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.ModTags;
import com.naverene.stevespantry.PantryClock;
import com.naverene.stevespantry.Spice;
import com.naverene.stevespantry.component.DishContents;
import com.naverene.stevespantry.component.Freshness;
import com.naverene.stevespantry.reference.Reference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A meal the player assembles, the way Tinkers' Construct assembles tools from parts: the stack
 * carries its own ingredients, spices and nutrition (see {@code DishAssemblyRecipe}) instead of being
 * one fixed item. It keeps for a while, goes stale, then spoils into leftovers.
 *
 * <p>1.12.2 food values are per item, so the dish keeps its computed {@code nutrition},
 * {@code saturation} (saturation points) and {@code eat_ticks} in NBT and hands them to
 * {@link ItemFood}'s stack-aware getters, which is what {@code FoodStats} reads when eating.
 */
public class DishItem extends ItemFood {
    /** Three in-game days. Spices like clove and garlic stretch this. */
    public static final long BASE_SHELF_LIFE = 3 * 24000L;
    /** Dishes with meat, fish, milk or eggs in them keep half as long. An icebox makes up for it. */
    public static final float PERISHABLE_MULTIPLIER = 0.5F;
    /** How long a fresh dish's spice effects last. */
    private static final int SPICE_EFFECT_TICKS = 20 * 60;

    public static final String NUTRITION = "nutrition";
    public static final String SATURATION = "saturation";
    public static final String EAT_TICKS = "eat_ticks";

    public DishItem() {
        super(0, 0F, false);
        setMaxStackSize(1);
    }

    // ---- freshness -------------------------------------------------------------------------

    @Override
    public void onCreated(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            stamp(stack, world);
        }
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isRemote) {
            return;
        }
        stamp(stack, world);
        Freshness freshness = Freshness.get(stack);
        if (freshness != null && freshness.stage(world.getTotalWorldTime()) == Freshness.Stage.SPOILED
                && entity instanceof EntityPlayer) {
            spoil(stack, (EntityPlayer) entity, slot);
        }
    }

    /** Starts the clock the first time the dish is in a world (crafting has no access to one). */
    private static void stamp(ItemStack stack, World world) {
        Freshness freshness = Freshness.get(stack);
        if (freshness != null && !freshness.stamped()) {
            new Freshness(world.getTotalWorldTime(), freshness.shelfLife()).set(stack);
        }
    }

    private static void spoil(ItemStack stack, EntityPlayer player, int slot) {
        ItemStack leftovers = new ItemStack(ModRegistries.SPOILED_LEFTOVERS, stack.getCount());
        // onUpdate's slot index is relative to whichever compartment the stack is in, so find it by identity.
        if (player.inventory.getStackInSlot(slot) == stack) {
            player.inventory.setInventorySlotContents(slot, leftovers);
        } else if (player.getHeldItemOffhand() == stack) {
            player.setHeldItem(EnumHand.OFF_HAND, leftovers);
        }
    }

    public static Freshness.Stage stage(ItemStack stack, long now) {
        Freshness freshness = Freshness.get(stack);
        return freshness == null ? Freshness.Stage.FRESH : freshness.stage(now);
    }

    // ---- eating ----------------------------------------------------------------------------

    @Override
    public int getHealAmount(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0 : tag.getInteger(NUTRITION);
    }

    /** FoodStats adds nutrition x modifier x 2 saturation, so turn the stored points back into a modifier. */
    @Override
    public float getSaturationModifier(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        int nutrition = getHealAmount(stack);
        return tag == null || nutrition <= 0 ? 0F : tag.getFloat(SATURATION) / (2F * nutrition);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey(EAT_TICKS) ? tag.getInteger(EAT_TICKS) : 32;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        // An empty dish (from /give or the creative menu) has nothing to eat.
        if (DishContents.get(player.getHeldItem(hand)) == null) {
            return new ActionResult<>(EnumActionResult.FAIL, player.getHeldItem(hand));
        }
        return super.onItemRightClick(world, player, hand);
    }

    @Override
    protected void onFoodEaten(ItemStack stack, World world, EntityPlayer eater) {
        DishContents contents = DishContents.get(stack);
        if (world.isRemote || contents == null) {
            return;
        }
        switch (stage(stack, world.getTotalWorldTime())) {
            case FRESH:
                for (Spice spice : contents.spices()) {
                    Potion effect = spice.effect();
                    if (effect != null) {
                        eater.addPotionEffect(new PotionEffect(effect, SPICE_EFFECT_TICKS, 0));
                    }
                }
                break;
            case STALE:
                break;
            case SPOILED:
                eater.addPotionEffect(new PotionEffect(MobEffects.HUNGER, 600, 0));
                eater.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 200, 0));
                break;
        }
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase eater) {
        // ItemFood feeds the player from the stack-aware getters above; then hand back the bowl, like a stew.
        super.onItemUseFinish(stack, world, eater);
        return new ItemStack(Items.BOWL);
    }

    // ---- display ---------------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation") // the server-safe translator: dish names are built on both sides
    public String getItemStackDisplayName(ItemStack stack) {
        DishContents contents = DishContents.get(stack);
        if (contents == null || contents.ingredients().isEmpty()) {
            return super.getItemStackDisplayName(stack);
        }
        List<ItemStack> parts = new ArrayList<>();
        for (ItemStack ingredient : contents.ingredients()) {
            boolean seen = false;
            for (ItemStack part : parts) {
                seen |= ItemStack.areItemsEqual(part, ingredient);
            }
            if (!seen) {
                parts.add(ingredient);
            }
        }
        String base = "item." + Reference.MODID + ".dish.";
        String name;
        switch (parts.size()) {
            case 1:
                name = net.minecraft.util.text.translation.I18n.translateToLocalFormatted(base + "one",
                        parts.get(0).getDisplayName());
                break;
            case 2:
                name = net.minecraft.util.text.translation.I18n.translateToLocalFormatted(base + "two",
                        parts.get(0).getDisplayName(), parts.get(1).getDisplayName());
                break;
            default:
                name = net.minecraft.util.text.translation.I18n.translateToLocalFormatted(base + "medley",
                        parts.get(0).getDisplayName());
                break;
        }
        return contents.spices().isEmpty() ? name
                : net.minecraft.util.text.translation.I18n.translateToLocalFormatted(base + "spiced", name);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        DishContents contents = DishContents.get(stack);
        if (contents == null) {
            return;
        }
        String key = "tooltip." + Reference.MODID + ".";
        Freshness freshness = Freshness.get(stack);
        if (freshness != null && world != null) {
            long now = world.getTotalWorldTime();
            Freshness.Stage stage = freshness.stage(now);
            TextFormatting color = stage == Freshness.Stage.FRESH ? TextFormatting.GREEN
                    : stage == Freshness.Stage.STALE ? TextFormatting.GOLD : TextFormatting.DARK_RED;
            long ticksLeft = freshness.stamped() ? Math.max(0, freshness.madeAt() + freshness.shelfLife() - now)
                    : freshness.shelfLife();
            tooltip.add(color + I18n.format(key + "stage." + stage.name().toLowerCase(Locale.ROOT),
                    formatDays(ticksLeft)));
        }
        if (isPerishable(contents)) {
            tooltip.add(TextFormatting.AQUA + I18n.format(key + "perishable"));
        }

        tooltip.add(TextFormatting.GRAY + I18n.format(key + "ingredients"));
        for (ItemStack ingredient : contents.ingredients()) {
            tooltip.add(TextFormatting.DARK_GRAY + "  " + ingredient.getDisplayName());
        }
        if (!contents.spices().isEmpty()) {
            tooltip.add(TextFormatting.GRAY + I18n.format(key + "spices"));
            for (Spice spice : contents.spices()) {
                tooltip.add(TextFormatting.DARK_GRAY + "  " + I18n.format(spice.translationKey() + ".name")
                        + TextFormatting.ITALIC + " (" + spice.latinName() + ")");
                tooltip.add(TextFormatting.BLUE + "    " + describeSpiceBonus(spice));
            }
        }
    }

    public static boolean isPerishable(DishContents contents) {
        for (ItemStack ingredient : contents.ingredients()) {
            if (ModTags.isPerishable(ingredient)) {
                return true;
            }
        }
        return false;
    }

    /** One line saying what a spice adds to a dish. */
    @SideOnly(Side.CLIENT)
    public static String describeSpiceBonus(Spice spice) {
        String key = "tooltip." + Reference.MODID + ".bonus.";
        Potion effect = spice.effect();
        if (effect != null) {
            return I18n.format(key + "effect", I18n.format(effect.getName()));
        }
        if (spice.shelfLifeMultiplier() > 1F) {
            return I18n.format(key + "preserves", Math.round((spice.shelfLifeMultiplier() - 1F) * 100F));
        }
        return I18n.format(key + "saturation", spice.bonusSaturation());
    }

    private static String formatDays(long ticks) {
        float days = ticks / 24000F;
        return days >= 1F ? String.format("%.1f", days) : String.format("%.2f", days);
    }

    // Freshness bar where a tool's durability bar would be.

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return Freshness.has(stack);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1D - remaining(stack);
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return MathHelper.hsvToRGB(remaining(stack) / 3F, 1F, 1F);
    }

    private static float remaining(ItemStack stack) {
        @Nullable Freshness freshness = Freshness.get(stack);
        return freshness == null ? 1F : freshness.remaining(PantryClock.client.getAsLong());
    }
}
