package com.naverene.stevespantry.block;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.component.Freshness;
import com.naverene.stevespantry.menu.IceboxMenu;
import com.naverene.stevespantry.reference.Reference;
import java.util.Map;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.NotNull;
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

    /**
     * Forge hands hoppers and pipes an item handler instead of the vanilla container, so give it
     * the sided view: one wrapper per face, following {@link #getSlotsForFace}.
     */
    private LazyOptional<IItemHandlerModifiable>[] sidedHandlers =
            SidedInvWrapper.create(this, Direction.values());

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
        Integer registered = com.naverene.stevespantry.PantryApiImpl.INSTANCE.registeredCoolant(stack.getItem());
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
                Freshness freshness = Freshness.get(stack);
                if (freshness == null) {
                    continue;
                }
                long madeAt = freshness.stamped() ? Math.min(now, freshness.madeAt() + refund) : now;
                new Freshness(madeAt, freshness.shelfLife()).set(stack);
            }
        }
        icebox.setChanged();
    }

    private boolean hasDish() {
        for (int i = 0; i < SIZE; i++) {
            if (Freshness.has(items.get(i))) {
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
    public int getContainerSize() {
        return SIZE + 1;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
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
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (side != null && cap == ForgeCapabilities.ITEM_HANDLER && !remove) {
            return sidedHandlers[side.ordinal()].cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (LazyOptional<?> handler : sidedHandlers) {
            handler.invalidate();
        }
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        sidedHandlers = SidedInvWrapper.create(this, Direction.values());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(SIZE + 1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        lastChilled = tag.contains("last_chilled") ? tag.getLong("last_chilled") : -1L;
        carry = tag.getLong("chill_carry");
        coldLeft = tag.getLong("cold_left");
        coldMax = tag.getLong("cold_max");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putLong("last_chilled", lastChilled);
        tag.putLong("chill_carry", carry);
        tag.putLong("cold_left", coldLeft);
        tag.putLong("cold_max", coldMax);
    }
}
