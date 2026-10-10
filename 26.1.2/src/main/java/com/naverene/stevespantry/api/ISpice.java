package com.naverene.stevespantry.api;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import org.jetbrains.annotations.Nullable;

/** One of the mod's spices and what it does to a dish. */
public interface ISpice {
    /** Short id, like {@code "cinnamon"}. Also the spice item's registry path. */
    String id();

    /** Botanical name, like {@code "Cinnamomum verum"}. */
    String latinName();

    /** RGB colour of the ground spice. */
    int color();

    /** Effect a fresh dish with this spice gives, or null if the spice does something else. */
    @Nullable
    Holder<MobEffect> effect();

    /** Saturation the spice adds to a dish. */
    float bonusSaturation();

    /** What the spice multiplies a dish's shelf life by (1 = no change). */
    float shelfLifeMultiplier();

    /** Rare spices have no HarvestCraft source. */
    boolean rare();

    /** Translation key of the spice's name. */
    String translationKey();
}
