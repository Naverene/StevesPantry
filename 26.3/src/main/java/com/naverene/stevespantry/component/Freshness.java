package com.naverene.stevespantry.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * When a dish was made (in world game time) and how many ticks it keeps. Game time only advances
 * while the world is running, so food doesn't rot while the server is off.
 */
public record Freshness(long madeAt, long shelfLife) {
    public static final Codec<Freshness> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("made_at").forGetter(Freshness::madeAt),
            Codec.LONG.fieldOf("shelf_life").forGetter(Freshness::shelfLife)
    ).apply(i, Freshness::new));

    public static final StreamCodec<ByteBuf, Freshness> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, Freshness::madeAt,
            ByteBufCodecs.VAR_LONG, Freshness::shelfLife,
            Freshness::new);

    /** Not stamped yet: the dish was just assembled and hasn't reached a world with a clock. */
    public static final long UNSTAMPED = -1L;

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
