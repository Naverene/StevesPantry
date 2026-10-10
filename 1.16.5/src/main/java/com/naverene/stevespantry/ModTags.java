package com.naverene.stevespantry;

import com.naverene.stevespantry.reference.Reference;
import net.minecraft.item.Item;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;

public final class ModTags {
    /** Foods that go off quickly unless kept cold: meat, fish, milk and eggs. */
    public static final ITag.INamedTag<Item> PERISHABLE =
            ItemTags.createOptional(new ResourceLocation(Reference.MODID, "perishable"));

    private ModTags() {}
}
