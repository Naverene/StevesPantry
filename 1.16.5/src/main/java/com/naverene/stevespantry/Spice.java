package com.naverene.stevespantry;

import com.naverene.stevespantry.api.ISpice;
import com.naverene.stevespantry.reference.Reference;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.potion.Effect;
import net.minecraft.potion.Effects;
import net.minecraft.util.IStringSerializable;

/**
 * Every spice the mod adds. Each one carries its botanical (Latin) name for the tooltip and one
 * thing it does to a dish: a status effect when eaten fresh, extra saturation, or a longer shelf life.
 */
public enum Spice implements IStringSerializable, ISpice {
    BLACK_PEPPER("black_pepper", "Piper nigrum", 0x3B3330, () -> Effects.DIG_SPEED, 0F, 1F, false),
    CINNAMON("cinnamon", "Cinnamomum verum", 0x9C5A2E, () -> Effects.REGENERATION, 0F, 1F, false),
    NUTMEG("nutmeg", "Myristica fragrans", 0x8A5F3C, () -> Effects.NIGHT_VISION, 0F, 1F, false),
    VANILLA("vanilla", "Vanilla planifolia", 0x2E211A, () -> Effects.ABSORPTION, 0F, 1F, false),
    GINGER("ginger", "Zingiber officinale", 0xD9B26A, () -> Effects.DAMAGE_RESISTANCE, 0F, 1F, false),
    MUSTARD("mustard", "Sinapis alba", 0xE0B32C, () -> Effects.DAMAGE_BOOST, 0F, 1F, false),
    SESAME("sesame", "Sesamum indicum", 0xE8D9B0, null, 1.5F, 1F, false),
    PAPRIKA("paprika", "Capsicum annuum", 0xC0321E, () -> Effects.FIRE_RESISTANCE, 0F, 1F, false),
    CHILI("chili", "Capsicum frutescens", 0xE0401A, () -> Effects.MOVEMENT_SPEED, 0F, 1F, false),
    GARLIC("garlic", "Allium sativum", 0xEDE6D3, null, 0.5F, 1.5F, false),
    SAFFRON("saffron", "Crocus sativus", 0xD8461B, () -> Effects.LUCK, 0F, 1F, true),
    CUMIN("cumin", "Cuminum cyminum", 0x8C6A3F, null, 1.5F, 1F, true),
    TURMERIC("turmeric", "Curcuma longa", 0xE3A21A, () -> Effects.HEALTH_BOOST, 0F, 1F, true),
    CARDAMOM("cardamom", "Elettaria cardamomum", 0x7E9A4E, () -> Effects.JUMP, 0F, 1F, true),
    CLOVE("clove", "Syzygium aromaticum", 0x5A3221, null, 0F, 2F, true),
    STAR_ANISE("star_anise", "Illicium verum", 0x6B3A22, () -> Effects.WATER_BREATHING, 0F, 1F, true);

    private final String id;
    private final String latinName;
    private final int color;
    @Nullable private final Supplier<Effect> effect;
    private final float bonusSaturation;
    private final float shelfLifeMultiplier;
    private final boolean rare;

    Spice(String id, String latinName, int color, @Nullable Supplier<Effect> effect,
            float bonusSaturation, float shelfLifeMultiplier, boolean rare) {
        this.id = id;
        this.latinName = latinName;
        this.color = color;
        this.effect = effect;
        this.bonusSaturation = bonusSaturation;
        this.shelfLifeMultiplier = shelfLifeMultiplier;
        this.rare = rare;
    }

    /** Looks a spice up by its id, as saved in a dish's NBT. Null for an id this version doesn't know. */
    @Nullable
    public static Spice byId(String id) {
        for (Spice spice : values()) {
            if (spice.id.equals(id)) {
                return spice;
            }
        }
        return null;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String latinName() {
        return latinName;
    }

    @Override
    public int color() {
        return color;
    }

    @Override
    @Nullable
    public Effect effect() {
        return effect == null ? null : effect.get();
    }

    @Override
    public float bonusSaturation() {
        return bonusSaturation;
    }

    @Override
    public float shelfLifeMultiplier() {
        return shelfLifeMultiplier;
    }

    /** Rare spices have no HarvestCraft source; they come from other mods' {@code forge:} tags or the wandering trader. */
    @Override
    public boolean rare() {
        return rare;
    }

    @Override
    public String translationKey() {
        return "item." + Reference.MODID + "." + id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
