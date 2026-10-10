package com.naverene.stevespantry.component;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * When a dish was made (in world game time) and how many ticks it keeps. Game time only advances
 * while the world is running, so food doesn't rot while the server is off.
 *
 * <p>1.7.10 has no data components, so this is read from and written to the stack's NBT
 * ({@code made_at}, {@code shelf_life}).
 */
public final class Freshness {
    /** Not stamped yet: the dish was just assembled and hasn't reached a world with a clock. */
    public static final long UNSTAMPED = -1L;

    private static final String MADE_AT = "made_at";
    private static final String SHELF_LIFE = "shelf_life";

    private final long madeAt;
    private final long shelfLife;

    public Freshness(long madeAt, long shelfLife) {
        this.madeAt = madeAt;
        this.shelfLife = shelfLife;
    }

    public long madeAt() {
        return madeAt;
    }

    public long shelfLife() {
        return shelfLife;
    }

    /** The stack's freshness, or null if it doesn't have one. */
    public static Freshness get(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null || !tag.hasKey(SHELF_LIFE)) {
            return null;
        }
        return new Freshness(tag.getLong(MADE_AT), tag.getLong(SHELF_LIFE));
    }

    public static boolean has(ItemStack stack) {
        return get(stack) != null;
    }

    public void set(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setLong(MADE_AT, madeAt);
        stack.getTagCompound().setLong(SHELF_LIFE, shelfLife);
    }

    public boolean stamped() {
        return madeAt != UNSTAMPED;
    }

    /** Fraction of shelf life left, from 1 (just made) down to 0 (spoiled). */
    public float remaining(long now) {
        if (!stamped() || now < 0) {
            return 1F;
        }
        float left = 1F - (float) (now - madeAt) / (float) shelfLife;
        return Math.max(0F, Math.min(1F, left));
    }

    public Stage stage(long now) {
        float left = remaining(now);
        if (left <= 0F) {
            return Stage.SPOILED;
        }
        return left > 0.5F ? Stage.FRESH : Stage.STALE;
    }

    public enum Stage {
        /** Full nutrition and every spice effect. */
        FRESH,
        /** Still fills you up, but the spices have gone flat: no effects. */
        STALE,
        /** Turns into Spoiled Leftovers. */
        SPOILED
    }
}
