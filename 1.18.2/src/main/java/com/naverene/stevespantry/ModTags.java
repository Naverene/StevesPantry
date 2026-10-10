package com.naverene.stevespantry;

import com.naverene.stevespantry.reference.Reference;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    /** Foods that go off quickly unless kept cold: meat, fish, milk and eggs. */
    public static final TagKey<Item> PERISHABLE =
            TagKey.create(Registry.ITEM_REGISTRY, ResourceLocation.fromNamespaceAndPath(Reference.MODID, "perishable"));

    private ModTags() {}
}
