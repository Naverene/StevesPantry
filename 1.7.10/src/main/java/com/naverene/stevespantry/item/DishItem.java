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
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

/**
 * A meal the player assembles, the way Tinkers' Construct assembles tools from parts: the stack
 * carries its own ingredients, spices and nutrition (see {@code DishAssemblyRecipe}) instead of being
 * one fixed item. It keeps for a while, goes stale, then spoils into leftovers.
 *
 * <p>1.7.10 food values are per item, so nutrition, saturation and eat time are stored in the stack's
 * NBT ({@code nutrition}, {@code saturation}, {@code eat_ticks}) and read back by the overrides below.
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
        setUnlocalizedName(Reference.MODID + ".dish");
        setTextureName(Reference.MODID + ":dish");
    }

    // ---- freshness -------------------------------------------------------------------------

    @Override
    public void onCreated(ItemStack stack, World world, EntityPlayer player) {
        stamp(stack, world);
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
        ItemStack leftovers = new ItemStack(ModRegistries.SPOILED_LEFTOVERS, stack.stackSize);
        // 1.7.10 only ticks the main inventory, and onUpdate's slot index is its index there.
        if (slot >= 0 && slot < player.inventory.mainInventory.length && player.inventory.mainInventory[slot] == stack) {
            player.inventory.setInventorySlotContents(slot, leftovers);
        }
    }

    public static Freshness.Stage stage(ItemStack stack, long now) {
        Freshness freshness = Freshness.get(stack);
        return freshness == null ? Freshness.Stage.FRESH : freshness.stage(now);
    }

    // ---- eating ----------------------------------------------------------------------------

    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer eater) {
        DishContents contents = DishContents.get(stack);
        Freshness.Stage stage = stage(stack, world.getTotalWorldTime());
        if (!world.isRemote && contents != null) {
            switch (stage) {
                case FRESH:
                    for (Spice spice : contents.spices()) {
                        Potion effect = spice.effect();
                        if (effect != null) {
                            eater.addPotionEffect(new PotionEffect(effect.id, SPICE_EFFECT_TICKS, 0));
                        }
                    }
                    break;
                case STALE:
                    break;
                case SPOILED:
                    eater.addPotionEffect(new PotionEffect(Potion.hunger.id, 600, 0));
                    eater.addPotionEffect(new PotionEffect(Potion.confusion.id, 200, 0));
                    break;
            }
        }
        // Nutrition comes from the stack's NBT, not the item, so feed the player here, then hand back the bowl.
        --stack.stackSize;
        eater.getFoodStats().addStats(func_150905_g(stack), func_150906_h(stack));
        world.playSoundAtEntity(eater, "random.burp", 0.5F, world.rand.nextFloat() * 0.1F + 0.9F);
        return stack.stackSize <= 0 ? new ItemStack(Items.bowl) : stack;
    }

    /** Hunger restored, from NBT. Mods that read food values through ItemFood see the real number. */
    @Override
    public int func_150905_g(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0 : tag.getInteger(NUTRITION);
    }

    /** Saturation modifier: NBT holds the absolute saturation, and vanilla adds nutrition * modifier * 2. */
    @Override
    public float func_150906_h(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        int nutrition = func_150905_g(stack);
        return tag == null || nutrition <= 0 ? 0F : tag.getFloat(SATURATION) / (2F * nutrition);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null || !tag.hasKey(EAT_TICKS) ? 32 : tag.getInteger(EAT_TICKS);
    }

    // ---- display ---------------------------------------------------------------------------

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        DishContents contents = DishContents.get(stack);
        if (contents == null || contents.ingredients().isEmpty()) {
            return super.getItemStackDisplayName(stack);
        }
        List<ItemStack> parts = distinct(contents.ingredients());
        String base = "item." + Reference.MODID + ".dish.";
        String name;
        switch (parts.size()) {
            case 1:
                name = StatCollector.translateToLocalFormatted(base + "one", parts.get(0).getDisplayName());
                break;
            case 2:
                name = StatCollector.translateToLocalFormatted(base + "two",
                        parts.get(0).getDisplayName(), parts.get(1).getDisplayName());
                break;
            default:
                name = StatCollector.translateToLocalFormatted(base + "medley", parts.get(0).getDisplayName());
        }
        return contents.spices().isEmpty() ? name : StatCollector.translateToLocalFormatted(base + "spiced", name);
    }

    /** Ingredients with repeats removed; on 1.7.10 the same item with different metadata is a different food. */
    public static List<ItemStack> distinct(List<ItemStack> stacks) {
        List<ItemStack> parts = new ArrayList<>();
        for (ItemStack stack : stacks) {
            boolean seen = false;
            for (ItemStack part : parts) {
                if (part.getItem() == stack.getItem() && part.getItemDamage() == stack.getItemDamage()) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                parts.add(stack);
            }
        }
        return parts;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        DishContents contents = DishContents.get(stack);
        if (contents == null) {
            return;
        }
        String key = "tooltip." + Reference.MODID + ".";
        Freshness freshness = Freshness.get(stack);
        if (freshness != null && player != null && player.worldObj != null) {
            long now = player.worldObj.getTotalWorldTime();
            Freshness.Stage stage = freshness.stage(now);
            EnumChatFormatting color;
            switch (stage) {
                case FRESH:
                    color = EnumChatFormatting.GREEN;
                    break;
                case STALE:
                    color = EnumChatFormatting.GOLD;
                    break;
                default:
                    color = EnumChatFormatting.DARK_RED;
            }
            long ticksLeft = freshness.stamped() ? Math.max(0, freshness.madeAt() + freshness.shelfLife() - now)
                    : freshness.shelfLife();
            tooltip.add(color + StatCollector.translateToLocalFormatted(key + "stage." + stage.name().toLowerCase(),
                    formatDays(ticksLeft)));
        }
        if (isPerishable(contents)) {
            tooltip.add(EnumChatFormatting.AQUA + StatCollector.translateToLocal(key + "perishable"));
        }

        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal(key + "ingredients"));
        for (ItemStack ingredient : contents.ingredients()) {
            tooltip.add(EnumChatFormatting.DARK_GRAY + "  " + ingredient.getDisplayName());
        }
        if (!contents.spices().isEmpty()) {
            tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal(key + "spices"));
            for (Spice spice : contents.spices()) {
                tooltip.add(EnumChatFormatting.DARK_GRAY + "  " + StatCollector.translateToLocal(spice.translationKey())
                        + EnumChatFormatting.ITALIC + " (" + spice.latinName() + ")");
                tooltip.add(EnumChatFormatting.BLUE + "    " + describeSpiceBonus(spice));
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
    public static String describeSpiceBonus(Spice spice) {
        String key = "tooltip." + Reference.MODID + ".bonus.";
        Potion effect = spice.effect();
        if (effect != null) {
            return StatCollector.translateToLocalFormatted(key + "effect", StatCollector.translateToLocal(effect.getName()));
        }
        if (spice.shelfLifeMultiplier() > 1F) {
            return StatCollector.translateToLocalFormatted(key + "preserves",
                    Math.round((spice.shelfLifeMultiplier() - 1F) * 100F));
        }
        return StatCollector.translateToLocalFormatted(key + "saturation", spice.bonusSaturation());
    }

    private static String formatDays(long ticks) {
        float days = ticks / 24000F;
        return days >= 1F ? String.format("%.1f", days) : String.format("%.2f", days);
    }

    // Freshness bar where a tool's durability bar would be; Forge colours it green to red.

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return Freshness.has(stack);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1D - remaining(stack);
    }

    private static float remaining(ItemStack stack) {
        Freshness freshness = Freshness.get(stack);
        return freshness == null ? 1F : freshness.remaining(PantryClock.client.getAsLong());
    }
}
