package com.naverene.stevespantry.component;

import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

/**
 * When a dish was made (in world game time) and how many ticks it keeps. Game time only advances
 * while the world is running, so food doesn't rot while the server is off. Stored as the
 * {@code made_at} and {@code shelf_life} longs in the dish's NBT.
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

    @Nullable
    public static Freshness get(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(SHELF_LIFE, Constants.NBT.TAG_LONG)) {
            return null;
        }
        return new Freshness(tag.getLong(MADE_AT), tag.getLong(SHELF_LIFE));
    }

    public static boolean has(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey(SHELF_LIFE, Constants.NBT.TAG_LONG);
    }

    public void set(ItemStack stack) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setLong(MADE_AT, madeAt);
        tag.setLong(SHELF_LIFE, shelfLife);
        stack.setTagCompound(tag);
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
