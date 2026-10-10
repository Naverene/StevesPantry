package com.naverene.stevespantry;

import com.naverene.stevespantry.reference.Reference;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    /** Foods that go off quickly unless kept cold: meat, fish, milk and eggs. */
    public static final TagKey<Item> PERISHABLE =
            ItemTags.create(Identifier.fromNamespaceAndPath(Reference.MODID, "perishable"));

    private ModTags() {}
}
