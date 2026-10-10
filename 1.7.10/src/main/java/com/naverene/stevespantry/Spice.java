package com.naverene.stevespantry;

import com.naverene.stevespantry.reference.Reference;
import net.minecraft.potion.Potion;

/**
 * Every spice the mod adds. Each one carries its botanical (Latin) name for the tooltip and one
 * thing it does to a dish: a status effect when eaten fresh, extra saturation, or a longer shelf life.
 *
 * <p>1.7.10 has no Luck effect (and its Saturation effect does nothing over time), so saffron gives Invisibility.
 */
public enum Spice {
    BLACK_PEPPER("black_pepper", "Piper nigrum", 0x3B3330, Potion.digSpeed, 0F, 1F, false),
    CINNAMON("cinnamon", "Cinnamomum verum", 0x9C5A2E, Potion.regeneration, 0F, 1F, false),
    NUTMEG("nutmeg", "Myristica fragrans", 0x8A5F3C, Potion.nightVision, 0F, 1F, false),
    VANILLA("vanilla", "Vanilla planifolia", 0x2E211A, Potion.field_76444_x, 0F, 1F, false),
    GINGER("ginger", "Zingiber officinale", 0xD9B26A, Potion.resistance, 0F, 1F, false),
    MUSTARD("mustard", "Sinapis alba", 0xE0B32C, Potion.damageBoost, 0F, 1F, false),
    SESAME("sesame", "Sesamum indicum", 0xE8D9B0, null, 1.5F, 1F, false),
    PAPRIKA("paprika", "Capsicum annuum", 0xC0321E, Potion.fireResistance, 0F, 1F, false),
    CHILI("chili", "Capsicum frutescens", 0xE0401A, Potion.moveSpeed, 0F, 1F, false),
    GARLIC("garlic", "Allium sativum", 0xEDE6D3, null, 0.5F, 1.5F, false),
    SAFFRON("saffron", "Crocus sativus", 0xD8461B, Potion.invisibility, 0F, 1F, true),
    CUMIN("cumin", "Cuminum cyminum", 0x8C6A3F, null, 1.5F, 1F, true),
    TURMERIC("turmeric", "Curcuma longa", 0xE3A21A, Potion.field_76434_w, 0F, 1F, true),
    CARDAMOM("cardamom", "Elettaria cardamomum", 0x7E9A4E, Potion.jump, 0F, 1F, true),
    CLOVE("clove", "Syzygium aromaticum", 0x5A3221, null, 0F, 2F, true),
    STAR_ANISE("star_anise", "Illicium verum", 0x6B3A22, Potion.waterBreathing, 0F, 1F, true);

    private final String id;
    private final String latinName;
    private final int color;
    private final Potion effect;
    private final float bonusSaturation;
    private final float shelfLifeMultiplier;
    private final boolean rare;

    Spice(String id, String latinName, int color, Potion effect,
            float bonusSaturation, float shelfLifeMultiplier, boolean rare) {
        this.id = id;
        this.latinName = latinName;
        this.color = color;
        this.effect = effect;
        this.bonusSaturation = bonusSaturation;
        this.shelfLifeMultiplier = shelfLifeMultiplier;
        this.rare = rare;
    }

    public String id() {
        return id;
    }

    public String latinName() {
        return latinName;
    }

    public int color() {
        return color;
    }

    /** The effect a fresh dish gives, or null for spices that add saturation or shelf life instead. */
    public Potion effect() {
        return effect;
    }

    public float bonusSaturation() {
        return bonusSaturation;
    }

    public float shelfLifeMultiplier() {
        return shelfLifeMultiplier;
    }

    /** Rare spices have no HarvestCraft source; they come from other mods' ore names or a farmer villager. */
    public boolean rare() {
        return rare;
    }

    /** Unlocalized name without the {@code item.} prefix, as passed to {@code setUnlocalizedName}. */
    public String unlocalizedName() {
        return Reference.MODID + "." + id;
    }

    public String translationKey() {
        return "item." + unlocalizedName() + ".name";
    }

    public static Spice byId(String id) {
        for (Spice spice : values()) {
            if (spice.id.equals(id)) {
                return spice;
            }
        }
        return null;
    }
}
