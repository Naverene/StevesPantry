package com.naverene.stevespantry.component;

import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.Constants;

/**
 * When a dish was made (in world game time) and how many ticks it keeps. Game time only advances
 * while the world is running, so food doesn't rot while the server is off. Stored in the stack's
 * NBT as {@code made_at} and {@code shelf_life}.
 */
public final class Freshness {
    public static final String MADE_AT = "made_at";
    public static final String SHELF_LIFE = "shelf_life";

    /** Not stamped yet: the dish was just assembled and hasn't reached a world with a clock. */
    public static final long UNSTAMPED = -1L;

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

    /** Reads a stack's freshness, or null if it doesn't spoil. */
    @Nullable
    public static Freshness get(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        if (tag == null || !tag.contains(SHELF_LIFE, Constants.NBT.TAG_ANY_NUMERIC)) {
            return null;
        }
        long madeAt = tag.contains(MADE_AT, Constants.NBT.TAG_ANY_NUMERIC) ? tag.getLong(MADE_AT) : UNSTAMPED;
        return new Freshness(madeAt, tag.getLong(SHELF_LIFE));
    }

    public static boolean has(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        return tag != null && tag.contains(SHELF_LIFE, Constants.NBT.TAG_ANY_NUMERIC);
    }

    public void set(ItemStack stack) {
        CompoundNBT tag = stack.getOrCreateTag();
        tag.putLong(MADE_AT, madeAt);
        tag.putLong(SHELF_LIFE, shelfLife);
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
