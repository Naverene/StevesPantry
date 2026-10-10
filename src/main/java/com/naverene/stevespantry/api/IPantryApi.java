package com.naverene.stevespantry.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Everything other mods can ask Steve's Pantry or tell it. Get it from {@link StevesPantryApi#get()}.
 *
 * <p>Time values are in ticks of world game time; a Minecraft day is 24000 ticks.
 */
public interface IPantryApi {

    // ---- spices --------------------------------------------------------------------------------

    /** Every spice, in a stable order. */
    Collection<ISpice> spices();

    /** The spice with this id, like {@code "saffron"}. */
    Optional<ISpice> spice(String id);

    /** The spice this stack seasons a dish as: one of the mod's spice items, or an item registered with {@link #registerSpiceItem}. */
    Optional<ISpice> spiceOf(ItemStack stack);

    /** A stack of the mod's own item for this spice. */
    ItemStack spiceStack(ISpice spice, int count);

    /**
     * Lets another mod's item season a dish as one of the spices, as if it were the mod's own spice
     * item. For example, register your mod's ground pepper as {@code black_pepper}.
     */
    void registerSpiceItem(Item item, ISpice spice);

    // ---- dishes --------------------------------------------------------------------------------

    /** Whether the stack is an assembled dish. */
    boolean isDish(ItemStack stack);

    /** What a dish was made from, or empty if the stack isn't a dish. */
    Optional<DishView> dish(ItemStack stack);

    /**
     * Builds a dish the same way the crafting grid does, for machines that cook without a player.
     * Returns {@link ItemStack#EMPTY} when the recipe rules aren't met (1 to 4 edible foods, up to
     * 3 different spices). The bowl isn't consumed here; that's up to the caller. The dish's clock
     * starts the first time it ticks in a world, or call {@link #stamp} to start it now.
     */
    ItemStack makeDish(List<ItemStack> foods, List<ISpice> spices);

    // ---- freshness -----------------------------------------------------------------------------

    /** Whether the stack carries a freshness clock (dishes do; most items don't). */
    boolean ages(ItemStack stack);

    /** Starts the stack's clock at the level's current time if it hasn't started yet. */
    void stamp(ItemStack stack, Level level);

    /** The stack's stage. Items that don't age are always {@link FreshnessStage#FRESH}. */
    FreshnessStage stage(ItemStack stack, Level level);

    /** Fraction of shelf life left, from 1 (just made) to 0 (spoiled). Items that don't age read 1. */
    float remaining(ItemStack stack, Level level);

    /** Ticks until the stack spoils, 0 if it already has, or -1 if it doesn't age. */
    long ticksLeft(ItemStack stack, Level level);

    /**
     * Gives back {@code ticks} of age to a stack, never making it fresher than just made. This is
     * how cold storage works: to make food age at 1/n speed, call this every so often with
     * {@code (n - 1) / n} of the ticks that passed. Does nothing to stacks that don't age.
     */
    void chill(ItemStack stack, long ticks, Level level);

    // ---- shelf life ----------------------------------------------------------------------------

    /** Foods that halve a dish's shelf life: {@code stevespantry:perishable}. Add to it with a datapack or tag provider. */
    TagKey<Item> perishableTag();

    /** Whether the food is in {@link #perishableTag()}, or the dish contains one that is. */
    boolean isPerishable(ItemStack stack);

    /** Adds a rule that adjusts every newly assembled dish's shelf life. */
    void registerShelfLifeModifier(ShelfLifeModifier modifier);

    // ---- cold storage --------------------------------------------------------------------------

    /**
     * Makes an item usable as ice in the Icebox, worth {@code ticks} of cold (an ice block is 24000).
     * Overrides the built-in value for vanilla snow and ice if you register one of those.
     */
    void registerCoolant(Item item, int ticks);

    /** Ticks of cold one of this item gives in the Icebox, or 0 if it isn't a coolant. */
    int coolantTicks(ItemStack stack);
}
