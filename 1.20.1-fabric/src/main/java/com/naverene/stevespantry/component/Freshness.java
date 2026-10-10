package com.naverene.stevespantry.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * When a dish was made (in world game time) and how many ticks it keeps. Game time only advances
 * while the world is running, so food doesn't rot while the server is off. Saved in the stack's
 * NBT as {@code made_at} and {@code shelf_life}, since 1.20.1 has no data components.
 */
public record Freshness(long madeAt, long shelfLife) {
    public static final String MADE_AT = "made_at";
    public static final String SHELF_LIFE = "shelf_life";

    /** Not stamped yet: the dish was just assembled and hasn't reached a world with a clock. */
    public static final long UNSTAMPED = -1L;

    /** The freshness saved on a stack, or null if it has none (it isn't a dish). */
    @Nullable
    public static Freshness get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(SHELF_LIFE, Tag.TAG_ANY_NUMERIC)) {
            return null;
        }
        long madeAt = tag.contains(MADE_AT, Tag.TAG_ANY_NUMERIC) ? tag.getLong(MADE_AT) : UNSTAMPED;
        return new Freshness(madeAt, tag.getLong(SHELF_LIFE));
    }

    public static boolean has(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(SHELF_LIFE, Tag.TAG_ANY_NUMERIC);
    }

    public void set(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
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
