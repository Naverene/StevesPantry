package com.naverene.stevespantry.api;

/** How far along a dish is. */
public enum FreshnessStage {
    /** More than half its shelf life left: full nutrition and every spice effect. */
    FRESH,
    /** Still fills you up, but the spices have gone flat: no effects. */
    STALE,
    /** Out of shelf life. Turns into Spoiled Leftovers in a player's inventory. */
    SPOILED
}
