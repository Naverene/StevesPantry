package com.naverene.stevespantry.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.naverene.stevespantry.Spice;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

/** What a dish was made from: its ingredients in order, and the spices that season it. */
public record DishContents(List<Item> ingredients, List<Spice> spices) {
    public static final Codec<DishContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().listOf().fieldOf("ingredients").forGetter(DishContents::ingredients),
            Spice.CODEC.listOf().optionalFieldOf("spices", List.of()).forGetter(DishContents::spices)
    ).apply(i, DishContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DishContents> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list()), DishContents::ingredients,
            Spice.STREAM_CODEC.apply(ByteBufCodecs.list()), DishContents::spices,
            DishContents::new);

    public DishContents {
        ingredients = List.copyOf(ingredients);
        spices = List.copyOf(spices);
    }
}
