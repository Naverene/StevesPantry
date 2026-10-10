package com.naverene.stevespantry;

import com.naverene.stevespantry.api.DishView;
import com.naverene.stevespantry.api.FreshnessStage;
import com.naverene.stevespantry.api.IPantryApi;
import com.naverene.stevespantry.api.ISpice;
import com.naverene.stevespantry.api.ShelfLifeModifier;
import com.naverene.stevespantry.block.IceboxBlockEntity;
import com.naverene.stevespantry.component.DishContents;
import com.naverene.stevespantry.component.Freshness;
import com.naverene.stevespantry.item.DishItem;
import com.naverene.stevespantry.item.SpiceItem;
import com.naverene.stevespantry.recipe.DishAssemblyRecipe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The mod's side of {@link IPantryApi}. Internal: addons go through {@code StevesPantryApi.get()}. */
public final class PantryApiImpl implements IPantryApi {
    public static final PantryApiImpl INSTANCE = new PantryApiImpl();

    private final Map<Item, Spice> spiceItems = new ConcurrentHashMap<>();
    private final Map<Item, Integer> coolants = new ConcurrentHashMap<>();
    private final List<ShelfLifeModifier> shelfLifeModifiers = new CopyOnWriteArrayList<>();

    private PantryApiImpl() {}

    // ---- spices --------------------------------------------------------------------------------

    @Override
    public Collection<ISpice> spices() {
        return List.of(Spice.values());
    }

    @Override
    public Optional<ISpice> spice(String id) {
        return Arrays.stream(Spice.values()).filter(spice -> spice.id().equals(id)).map(ISpice.class::cast).findFirst();
    }

    @Override
    public Optional<ISpice> spiceOf(ItemStack stack) {
        if (stack.getItem() instanceof SpiceItem spiceItem) {
            return Optional.of(spiceItem.spice());
        }
        return Optional.ofNullable(spiceItems.get(stack.getItem()));
    }

    @Override
    public ItemStack spiceStack(ISpice spice, int count) {
        return new ItemStack(ModRegistries.SPICES.get(own(spice)), count);
    }

    @Override
    public void registerSpiceItem(Item item, ISpice spice) {
        spiceItems.put(item, own(spice));
    }

    private static Spice own(ISpice spice) {
        if (spice instanceof Spice own) {
            return own;
        }
        throw new IllegalArgumentException("Not a Steve's Pantry spice: " + spice.id());
    }

    // ---- dishes --------------------------------------------------------------------------------

    @Override
    public boolean isDish(ItemStack stack) {
        return stack.getItem() instanceof DishItem;
    }

    @Override
    public Optional<DishView> dish(ItemStack stack) {
        DishContents contents = DishContents.get(stack);
        if (contents == null) {
            return Optional.empty();
        }
        return Optional.of(new DishView(contents.ingredients(), List.copyOf(contents.spices()),
                DishItem.isPerishable(contents)));
    }

    @Override
    public ItemStack makeDish(List<ItemStack> foods, List<ISpice> spices) {
        List<Spice> own = new ArrayList<>(spices.size());
        for (ISpice spice : spices) {
            own.add(own(spice));
        }
        return DishAssemblyRecipe.accepts(foods, own) ? DishAssemblyRecipe.build(foods, own) : ItemStack.EMPTY;
    }

    // ---- freshness -----------------------------------------------------------------------------

    @Override
    public boolean ages(ItemStack stack) {
        return Freshness.has(stack);
    }

    @Override
    public void stamp(ItemStack stack, Level level) {
        Freshness freshness = Freshness.get(stack);
        if (freshness != null && !freshness.stamped()) {
            new Freshness(level.getGameTime(), freshness.shelfLife()).set(stack);
        }
    }

    @Override
    public FreshnessStage stage(ItemStack stack, Level level) {
        return switch (DishItem.stage(stack, level.getGameTime())) {
            case FRESH -> FreshnessStage.FRESH;
            case STALE -> FreshnessStage.STALE;
            case SPOILED -> FreshnessStage.SPOILED;
        };
    }

    @Override
    public float remaining(ItemStack stack, Level level) {
        Freshness freshness = Freshness.get(stack);
        return freshness == null ? 1F : freshness.remaining(level.getGameTime());
    }

    @Override
    public long ticksLeft(ItemStack stack, Level level) {
        Freshness freshness = Freshness.get(stack);
        if (freshness == null) {
            return -1L;
        }
        if (!freshness.stamped()) {
            return freshness.shelfLife();
        }
        return Math.max(0L, freshness.madeAt() + freshness.shelfLife() - level.getGameTime());
    }

    @Override
    public void chill(ItemStack stack, long ticks, Level level) {
        Freshness freshness = Freshness.get(stack);
        if (freshness == null || ticks <= 0) {
            return;
        }
        long now = level.getGameTime();
        long madeAt = freshness.stamped() ? Math.min(now, freshness.madeAt() + ticks) : now;
        new Freshness(madeAt, freshness.shelfLife()).set(stack);
    }

    // ---- shelf life ----------------------------------------------------------------------------

    @Override
    public TagKey<Item> perishableTag() {
        return ModTags.PERISHABLE;
    }

    @Override
    public boolean isPerishable(ItemStack stack) {
        DishContents contents = DishContents.get(stack);
        return contents != null ? DishItem.isPerishable(contents) : stack.is(ModTags.PERISHABLE);
    }

    @Override
    public void registerShelfLifeModifier(ShelfLifeModifier modifier) {
        shelfLifeModifiers.add(modifier);
    }

    /** Runs the registered modifiers over a new dish's shelf life. Never goes below one tick. */
    public long modifyShelfLife(List<ItemStack> foods, List<Spice> spices, long shelfLife) {
        if (shelfLifeModifiers.isEmpty()) {
            return shelfLife;
        }
        List<ItemStack> foodView = foods.stream().map(ItemStack::copy).toList();
        List<ISpice> spiceView = List.copyOf(spices);
        for (ShelfLifeModifier modifier : shelfLifeModifiers) {
            shelfLife = modifier.modify(foodView, spiceView, shelfLife);
        }
        return Math.max(1L, shelfLife);
    }

    // ---- cold storage --------------------------------------------------------------------------

    @Override
    public void registerCoolant(Item item, int ticks) {
        if (ticks <= 0) {
            throw new IllegalArgumentException("Coolant ticks must be positive, got " + ticks);
        }
        coolants.put(item, ticks);
    }

    @Override
    public int coolantTicks(ItemStack stack) {
        return IceboxBlockEntity.coolantTicks(stack);
    }

    /** A coolant another mod registered, or null. Overrides the Icebox's built-in values. */
    public Integer registeredCoolant(Item item) {
        return coolants.get(item);
    }
}
