package com.naverene.stevespantry.block;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.component.Freshness;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * The spoilage slowdown shared by every cold store. Dishes age on world game time (see
 * {@link Freshness}), so instead of changing that clock a cold store hands back part of the time
 * that passed: every so often it moves each dish's "made at" forward by {@code (slowdown - 1) /
 * slowdown} of the elapsed ticks, so dishes inside age at {@code 1 / slowdown} of the normal rate.
 * Elapsed time is measured from the last chill, which is saved, so cold stores keep working
 * across chunk unloads and restarts.
 */
public final class Chiller {
    public static final int CHILL_INTERVAL = 20;

    private int slowdown;
    /** Game time of the last chill, or -1 before the first one. */
    private long lastChilled = -1L;
    /** Leftover fraction of a tick from the last refund, in 1/slowdown units. */
    private long carry;

    public Chiller(int slowdown) {
        this.slowdown = slowdown;
    }

    /** Changes how hard it chills from the next refund on, like the freezer does when its condenser wears out. */
    public void setSlowdown(int slowdown) {
        if (slowdown != this.slowdown) {
            this.slowdown = slowdown;
            carry = 0;
        }
    }

    /**
     * Ticks since the last chill if a chill is due now, otherwise -1. Starting the clock (first
     * call, or game time going backwards) also returns -1. The caller follows a due chill with
     * {@link #refund}.
     */
    public long due(long now) {
        if (lastChilled < 0 || lastChilled > now) {
            lastChilled = now;
            return -1L;
        }
        long elapsed = now - lastChilled;
        return elapsed < CHILL_INTERVAL ? -1L : elapsed;
    }

    /** Marks a chill done at {@code now} and returns the ticks to give back for {@code chilledTicks} of cold. */
    public long refund(long now, long chilledTicks) {
        lastChilled = now;
        long credit = chilledTicks * (slowdown - 1) + carry;
        carry = credit % slowdown;
        return credit / slowdown;
    }

    /** Moves every dish's "made at" forward by {@code refund} ticks, never past {@code now}. */
    public static void chill(Iterable<ItemStack> stacks, long refund, long now) {
        for (ItemStack stack : stacks) {
            Freshness freshness = stack.get(ModRegistries.FRESHNESS);
            if (freshness == null) {
                continue;
            }
            long madeAt = freshness.stamped() ? Math.min(now, freshness.madeAt() + refund) : now;
            stack.set(ModRegistries.FRESHNESS, new Freshness(madeAt, freshness.shelfLife()));
        }
    }

    public void load(CompoundTag tag) {
        lastChilled = tag.contains("last_chilled") ? tag.getLong("last_chilled") : -1L;
        carry = tag.getLong("chill_carry");
    }

    public void save(CompoundTag tag) {
        tag.putLong("last_chilled", lastChilled);
        tag.putLong("chill_carry", carry);
    }
}
