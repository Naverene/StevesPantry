package com.naverene.stevespantry.block;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.PantryApiImpl;
import com.naverene.stevespantry.component.Freshness;
import com.naverene.stevespantry.menu.IceboxMenu;
import com.naverene.stevespantry.reference.Reference;
import java.util.Map;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A chest-sized cold box with a separate ice slot. Dishes age on world game time (see
 * {@link Freshness}), so instead of changing that clock the icebox hands back part of the time that
 * passed: every so often it moves each dish's "made at" forward by two thirds of the chilled ticks,
 * so dishes inside age at a third of the normal rate. Elapsed time is measured from the last chill,
 * which is saved, so the icebox keeps working across chunk unloads and restarts.
 *
 * <p>Chilling melts ice, like a furnace burns fuel, but only while there's a dish inside. With no
 * ice left, dishes spoil at the normal rate.
 */
public class IceboxBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    /** Food slots. */
    public static final int SIZE = 27;
    public static final int ICE_SLOT = SIZE;
    /** Dishes inside spoil this many times slower. */
    public static final int SLOWDOWN = 3;
    private static final int CHILL_INTERVAL = 20;
    private static final int[] FOOD_SLOTS = IntStream.range(0, SIZE).toArray();
    private static final int[] ICE_SLOTS = {ICE_SLOT};

    /** How many ticks of cold each item gives. Snow and ice go nine to the next tier, like their recipes. */
    private static final Map<Item, Integer> COOLANTS = Map.of(
            Items.SNOWBALL, 6000,
            Items.SNOW_BLOCK, 24000,
            Items.ICE, 24000,
            Items.PACKED_ICE, 9 * 24000,
            Items.BLUE_ICE, 81 * 24000);

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE + 1, ItemStack.EMPTY);
    /** Game time of the last chill, or -1 before the first one. */
    private long lastChilled = -1L;
    /** Leftover fraction of a tick from the last refund, in 1/SLOWDOWN units. */
    private long carry;
    /** Ticks of cold left from ice already melted into the box. */
    private long coldLeft;
    /** What the last piece of ice was worth, for the gauge. */
    private long coldMax;

    /** Synced to the menu in hundredths of a day, since container data travels as shorts. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            long ticks = index == 0 ? coldLeft : coldMax;
            return (int) Math.min(Short.MAX_VALUE, ticks / 240L);
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 2;
        }
    };

    public IceboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.ICEBOX_BLOCK_ENTITY.get(), pos, state);
    }

    public static int coolantTicks(ItemStack stack) {
        Integer registered = PantryApiImpl.INSTANCE.registeredCoolant(stack.getItem());
        return registered != null ? registered : COOLANTS.getOrDefault(stack.getItem(), 0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, IceboxBlockEntity icebox) {
        long now = level.getGameTime();
        if (icebox.lastChilled < 0 || icebox.lastChilled > now) {
            icebox.lastChilled = now;
            icebox.setChanged();
            return;
        }
        long elapsed = now - icebox.lastChilled;
        if (elapsed < CHILL_INTERVAL) {
            return;
        }
        icebox.lastChilled = now;
        long chilled = icebox.hasDish() ? icebox.melt(elapsed) : 0L;
        long credit = chilled * (SLOWDOWN - 1) + icebox.carry;
        long refund = credit / SLOWDOWN;
        icebox.carry = credit % SLOWDOWN;
        if (refund > 0) {
            for (int i = 0; i < SIZE; i++) {
                ItemStack stack = icebox.items.get(i);
                Freshness freshness = stack.get(ModRegistries.FRESHNESS);
                if (freshness == null) {
                    continue;
                }
                long madeAt = freshness.stamped() ? Math.min(now, freshness.madeAt() + refund) : now;
                stack.set(ModRegistries.FRESHNESS, new Freshness(madeAt, freshness.shelfLife()));
            }
        }
        icebox.setChanged();
    }

    private boolean hasDish() {
        for (int i = 0; i < SIZE; i++) {
            if (items.get(i).has(ModRegistries.FRESHNESS)) {
                return true;
            }
        }
        return false;
    }

    /** Melts as much ice as {@code ticks} of cold needs and returns how many ticks were covered. */
    private long melt(long ticks) {
        ItemStack ice = items.get(ICE_SLOT);
        while (coldLeft < ticks && !ice.isEmpty() && coolantTicks(ice) > 0) {
            coldMax = coolantTicks(ice);
            coldLeft += coldMax;
            ice.shrink(1);
        }
        long covered = Math.min(ticks, coldLeft);
        coldLeft -= covered;
        return covered;
    }

    // ---- container ---------------------------------------------------------------------------

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + Reference.MODID + ".icebox");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return SIZE + 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot != ICE_SLOT || coolantTicks(stack) > 0;
    }

    // Hoppers above or below reach the food; hoppers on the sides feed the ice slot.

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side.getAxis() == Direction.Axis.Y ? FOOD_SLOTS : ICE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot != ICE_SLOT;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new IceboxMenu(containerId, inventory, this, data);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE + 1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        lastChilled = tag.contains("last_chilled") ? tag.getLong("last_chilled") : -1L;
        carry = tag.getLong("chill_carry");
        coldLeft = tag.getLong("cold_left");
        coldMax = tag.getLong("cold_max");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putLong("last_chilled", lastChilled);
        tag.putLong("chill_carry", carry);
        tag.putLong("cold_left", coldLeft);
        tag.putLong("cold_max", coldMax);
    }
}
