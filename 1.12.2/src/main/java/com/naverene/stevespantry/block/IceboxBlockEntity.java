package com.naverene.stevespantry.block;

import com.naverene.stevespantry.component.Freshness;
import com.naverene.stevespantry.menu.IceboxMenu;
import com.naverene.stevespantry.reference.Reference;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityLockable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
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
public class IceboxBlockEntity extends TileEntityLockable implements ISidedInventory, ITickable {
    /** Food slots. */
    public static final int SIZE = 27;
    public static final int ICE_SLOT = SIZE;
    /** Dishes inside spoil this many times slower. */
    public static final int SLOWDOWN = 3;
    private static final int CHILL_INTERVAL = 20;
    private static final int[] FOOD_SLOTS = IntStream.range(0, SIZE).toArray();
    private static final int[] ICE_SLOTS = {ICE_SLOT};

    /** How many ticks of cold each item gives. Snow and ice go nine to the next tier, like their recipes. 1.12.2 has no blue ice. */
    private static Map<Item, Integer> coolants;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE + 1, ItemStack.EMPTY);
    /** Game time of the last chill, or -1 before the first one. */
    private long lastChilled = -1L;
    /** Leftover fraction of a tick from the last refund, in 1/SLOWDOWN units. */
    private long carry;
    /** Ticks of cold left from ice already melted into the box. */
    private long coldLeft;
    /** What the last piece of ice was worth, for the gauge. */
    private long coldMax;
    @Nullable private String customName;

    // Hoppers above or below reach the food; hoppers on the sides feed the ice slot.
    private final IItemHandler handlerTop = new SidedInvWrapper(this, EnumFacing.UP);
    private final IItemHandler handlerBottom = new SidedInvWrapper(this, EnumFacing.DOWN);
    private final IItemHandler handlerSide = new SidedInvWrapper(this, EnumFacing.NORTH);
    private final IItemHandler handlerAll = new InvWrapper(this);

    public static int coolantTicks(ItemStack stack) {
        if (coolants == null) {
            Map<Item, Integer> map = new HashMap<>();
            map.put(Items.SNOWBALL, 6000);
            map.put(Item.getItemFromBlock(Blocks.SNOW), 24000);
            map.put(Item.getItemFromBlock(Blocks.ICE), 24000);
            map.put(Item.getItemFromBlock(Blocks.PACKED_ICE), 9 * 24000);
            coolants = map;
        }
        Integer registered = com.naverene.stevespantry.PantryApiImpl.INSTANCE.registeredCoolant(stack.getItem());
        if (registered != null) {
            return registered;
        }
        Integer ticks = coolants.get(stack.getItem());
        return ticks == null ? 0 : ticks;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        long now = world.getTotalWorldTime();
        if (lastChilled < 0 || lastChilled > now) {
            lastChilled = now;
            markDirty();
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
        markDirty();
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
    public String getName() {
        return hasCustomName() ? customName : "container." + Reference.MODID + ".icebox";
    }

    @Override
    public boolean hasCustomName() {
        return customName != null && !customName.isEmpty();
    }

    public void setCustomName(String name) {
        customName = name;
    }

    @Override
    public int getSizeInventory() {
        return SIZE + 1;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        ItemStack taken = ItemStackHelper.getAndSplit(items, slot, count);
        if (!taken.isEmpty()) {
            markDirty();
        }
        return taken;
    }

    @Override
    public ItemStack removeStackFromSlot(int slot) {
        return ItemStackHelper.getAndRemove(items, slot);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getInventoryStackLimit()) {
            stack.setCount(getInventoryStackLimit());
        }
        markDirty();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return world.getTileEntity(pos) == this
                && player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory(EntityPlayer player) {}

    @Override
    public void closeInventory(EntityPlayer player) {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot != ICE_SLOT || coolantTicks(stack) > 0;
    }

    /** Synced to the menu in hundredths of a day, since window properties travel as shorts. */
    @Override
    public int getField(int index) {
        long ticks = index == 0 ? coldLeft : coldMax;
        return (int) Math.min(Short.MAX_VALUE, ticks / 240L);
    }

    @Override
    public void setField(int index, int value) {}

    @Override
    public int getFieldCount() {
        return 2;
    }

    @Override
    public void clear() {
        items.clear();
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return side.getAxis() == EnumFacing.Axis.Y ? FOOD_SLOTS : ICE_SLOTS;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, EnumFacing side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, EnumFacing side) {
        return slot != ICE_SLOT;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            IItemHandler handler = facing == null ? handlerAll
                    : facing == EnumFacing.UP ? handlerTop
                    : facing == EnumFacing.DOWN ? handlerBottom : handlerSide;
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(handler);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public String getGuiID() {
        return Reference.MODID + ":icebox";
    }

    @Override
    public Container createContainer(InventoryPlayer inventory, EntityPlayer player) {
        return new IceboxMenu(inventory, this);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        items = NonNullList.withSize(SIZE + 1, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(tag, items);
        lastChilled = tag.hasKey("last_chilled") ? tag.getLong("last_chilled") : -1L;
        carry = tag.getLong("chill_carry");
        coldLeft = tag.getLong("cold_left");
        coldMax = tag.getLong("cold_max");
        customName = tag.hasKey("CustomName", 8) ? tag.getString("CustomName") : null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        ItemStackHelper.saveAllItems(tag, items);
        tag.setLong("last_chilled", lastChilled);
        tag.setLong("chill_carry", carry);
        tag.setLong("cold_left", coldLeft);
        tag.setLong("cold_max", coldMax);
        if (hasCustomName()) {
            tag.setString("CustomName", customName);
        }
        return tag;
    }
}
