package com.naverene.stevespantry.block;

import com.naverene.stevespantry.component.Freshness;
import com.naverene.stevespantry.reference.Reference;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;

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
public class IceboxBlockEntity extends TileEntity implements ISidedInventory {
    /** Food slots. */
    public static final int SIZE = 27;
    public static final int ICE_SLOT = SIZE;
    /** Dishes inside spoil this many times slower. */
    public static final int SLOWDOWN = 3;
    private static final int CHILL_INTERVAL = 20;
    private static final int[] FOOD_SLOTS = new int[SIZE];
    private static final int[] ICE_SLOTS = {ICE_SLOT};

    static {
        for (int i = 0; i < SIZE; i++) {
            FOOD_SLOTS[i] = i;
        }
    }

    /** How many ticks of cold each item gives. Snow and ice go nine to the next tier, like their recipes. */
    private static final Map<Item, Integer> COOLANTS = new HashMap<>();

    static {
        COOLANTS.put(Items.snowball, 6000);
        COOLANTS.put(Item.getItemFromBlock(Blocks.snow), 24000);
        COOLANTS.put(Item.getItemFromBlock(Blocks.ice), 24000);
        COOLANTS.put(Item.getItemFromBlock(Blocks.packed_ice), 9 * 24000);
        // 1.7.10 has no blue ice.
    }

    private ItemStack[] items = new ItemStack[SIZE + 1];
    /** Game time of the last chill, or -1 before the first one. */
    private long lastChilled = -1L;
    /** Leftover fraction of a tick from the last refund, in 1/SLOWDOWN units. */
    private long carry;
    /** Ticks of cold left from ice already melted into the box. */
    private long coldLeft;
    /** What the last piece of ice was worth, for the gauge. */
    private long coldMax;

    public static int coolantTicks(ItemStack stack) {
        if (stack == null) {
            return 0;
        }
        Integer ticks = COOLANTS.get(stack.getItem());
        return ticks == null ? 0 : ticks;
    }

    /** Synced to the menu in hundredths of a day, since container data travels as shorts. */
    public int coldData(int index) {
        long ticks = index == 0 ? coldLeft : coldMax;
        return (int) Math.min(Short.MAX_VALUE, ticks / 240L);
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        long now = worldObj.getTotalWorldTime();
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
                ItemStack stack = items[i];
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
            if (Freshness.has(items[i])) {
                return true;
            }
        }
        return false;
    }

    /** Melts as much ice as {@code ticks} of cold needs and returns how many ticks were covered. */
    private long melt(long ticks) {
        while (coldLeft < ticks && items[ICE_SLOT] != null && coolantTicks(items[ICE_SLOT]) > 0) {
            coldMax = coolantTicks(items[ICE_SLOT]);
            coldLeft += coldMax;
            if (--items[ICE_SLOT].stackSize <= 0) {
                items[ICE_SLOT] = null;
            }
        }
        long covered = Math.min(ticks, coldLeft);
        coldLeft -= covered;
        return covered;
    }

    // ---- container ---------------------------------------------------------------------------

    @Override
    public int getSizeInventory() {
        return SIZE + 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return items[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        if (items[slot] == null) {
            return null;
        }
        ItemStack taken;
        if (items[slot].stackSize <= count) {
            taken = items[slot];
            items[slot] = null;
        } else {
            taken = items[slot].splitStack(count);
        }
        markDirty();
        return taken;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack stack = items[slot];
        items[slot] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        items[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return "container." + Reference.MODID + ".icebox";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
                && player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64D;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot != ICE_SLOT || coolantTicks(stack) > 0;
    }

    // Hoppers above or below reach the food; hoppers on the sides feed the ice slot.

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return side == 0 || side == 1 ? FOOD_SLOTS : ICE_SLOTS;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return slot != ICE_SLOT;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        items = new ItemStack[SIZE + 1];
        NBTTagList list = tag.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            int slot = entry.getByte("Slot") & 255;
            if (slot < items.length) {
                items[slot] = ItemStack.loadItemStackFromNBT(entry);
            }
        }
        lastChilled = tag.hasKey("last_chilled") ? tag.getLong("last_chilled") : -1L;
        carry = tag.getLong("chill_carry");
        coldLeft = tag.getLong("cold_left");
        coldMax = tag.getLong("cold_max");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setByte("Slot", (byte) i);
                items[i].writeToNBT(entry);
                list.appendTag(entry);
            }
        }
        tag.setTag("Items", list);
        tag.setLong("last_chilled", lastChilled);
        tag.setLong("chill_carry", carry);
        tag.setLong("cold_left", coldLeft);
        tag.setLong("cold_max", coldMax);
    }
}
