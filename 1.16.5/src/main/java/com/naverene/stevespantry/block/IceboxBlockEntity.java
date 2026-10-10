package com.naverene.stevespantry.block;

import com.google.common.collect.ImmutableMap;
import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.component.Freshness;
import com.naverene.stevespantry.menu.IceboxMenu;
import com.naverene.stevespantry.reference.Reference;
import java.util.Map;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

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
public class IceboxBlockEntity extends LockableTileEntity implements ISidedInventory, ITickableTileEntity {
    /** Food slots. */
    public static final int SIZE = 27;
    public static final int ICE_SLOT = SIZE;
    /** Dishes inside spoil this many times slower. */
    public static final int SLOWDOWN = 3;
    private static final int CHILL_INTERVAL = 20;
    private static final int[] FOOD_SLOTS = IntStream.range(0, SIZE).toArray();
    private static final int[] ICE_SLOTS = {ICE_SLOT};

    /** How many ticks of cold each item gives. Snow and ice go nine to the next tier, like their recipes. */
    private static final Map<Item, Integer> COOLANTS = ImmutableMap.of(
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
     * Forge hands hoppers and pipes an item handler instead of the vanilla inventory, so give it
     * the sided view: one wrapper per face, following {@link #getSlotsForFace}.
     */
    private final LazyOptional<IItemHandlerModifiable>[] sidedHandlers =
            SidedInvWrapper.create(this, Direction.values());

    /** Synced to the menu in hundredths of a day, since container data travels as shorts. */
    private final IIntArray data = new IIntArray() {
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

    public IceboxBlockEntity() {
        super(ModRegistries.ICEBOX_BLOCK_ENTITY.get());
    }

    public static int coolantTicks(ItemStack stack) {
        Integer ticks = COOLANTS.get(stack.getItem());
        return ticks == null ? 0 : ticks;
    }

    @Override
    public void tick() {
        World level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        long now = level.getGameTime();
        if (lastChilled < 0 || lastChilled > now) {
            lastChilled = now;
            setChanged();
            return;
        }
        long elapsed = now - lastChilled;
        if (elapsed < CHILL_INTERVAL) {
            return;
        }
        lastChilled = now;
        long chilled = hasDish() ? melt(elapsed) : 0L;
        long credit = chilled * (SLOWDOWN - 1) + carry;
        long refund = credit / SLOWDOWN;
        carry = credit % SLOWDOWN;
        if (refund > 0) {
            for (int i = 0; i < SIZE; i++) {
                ItemStack stack = items.get(i);
                Freshness freshness = Freshness.get(stack);
                if (freshness == null) {
                    continue;
                }
                long madeAt = freshness.stamped() ? Math.min(now, freshness.madeAt() + refund) : now;
                new Freshness(madeAt, freshness.shelfLife()).set(stack);
            }
        }
        setChanged();
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
    protected ITextComponent getDefaultName() {
        return new TranslationTextComponent("container." + Reference.MODID + ".icebox");
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
        ItemStack removed = ItemStackHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStackHelper.takeItem(items, slot);
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
    public boolean stillValid(PlayerEntity player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
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
    protected Container createMenu(int containerId, PlayerInventory inventory) {
        return new IceboxMenu(containerId, inventory, this, data);
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (side != null && cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && !remove) {
            return sidedHandlers[side.ordinal()].cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void invalidateCaps() {
        super.invalidateCaps();
        for (LazyOptional<?> handler : sidedHandlers) {
            handler.invalidate();
        }
    }

    @Override
    public void load(BlockState state, CompoundNBT tag) {
        super.load(state, tag);
        items = NonNullList.withSize(SIZE + 1, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(tag, items);
        lastChilled = tag.contains("last_chilled") ? tag.getLong("last_chilled") : -1L;
        carry = tag.getLong("chill_carry");
        coldLeft = tag.getLong("cold_left");
        coldMax = tag.getLong("cold_max");
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        super.save(tag);
        ItemStackHelper.saveAllItems(tag, items);
        tag.putLong("last_chilled", lastChilled);
        tag.putLong("chill_carry", carry);
        tag.putLong("cold_left", coldLeft);
        tag.putLong("cold_max", coldMax);
        return tag;
    }
}
