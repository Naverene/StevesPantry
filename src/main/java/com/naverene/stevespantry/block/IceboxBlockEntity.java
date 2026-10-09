package com.naverene.stevespantry.block;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.component.Freshness;
import com.naverene.stevespantry.reference.Reference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A chest-sized cold box. Dishes age on world game time (see {@link Freshness}), so instead of
 * changing that clock the icebox hands back part of the time that passed: every so often it moves
 * each dish's "made at" forward by two thirds of the elapsed ticks, so dishes inside age at a third
 * of the normal rate. Elapsed time is measured from the last chill, which is saved, so the icebox
 * keeps working across chunk unloads and restarts.
 */
public class IceboxBlockEntity extends BaseContainerBlockEntity {
    public static final int SIZE = 27;
    /** Dishes inside spoil this many times slower. */
    public static final int SLOWDOWN = 3;
    private static final int CHILL_INTERVAL = 20;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    /** Game time of the last chill, or -1 before the first one. */
    private long lastChilled = -1L;
    /** Leftover fraction of a tick from the last refund, in 1/SLOWDOWN units. */
    private long carry;

    public IceboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.ICEBOX_BLOCK_ENTITY.get(), pos, state);
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
        long credit = elapsed * (SLOWDOWN - 1) + icebox.carry;
        long refund = credit / SLOWDOWN;
        icebox.carry = credit % SLOWDOWN;
        icebox.lastChilled = now;
        for (ItemStack stack : icebox.items) {
            Freshness freshness = stack.get(ModRegistries.FRESHNESS);
            if (freshness == null) {
                continue;
            }
            long madeAt = freshness.stamped() ? Math.min(now, freshness.madeAt() + refund) : now;
            stack.set(ModRegistries.FRESHNESS, new Freshness(madeAt, freshness.shelfLife()));
        }
        icebox.setChanged();
    }

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
        return SIZE;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.threeRows(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        lastChilled = tag.contains("last_chilled") ? tag.getLong("last_chilled") : -1L;
        carry = tag.getLong("chill_carry");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putLong("last_chilled", lastChilled);
        tag.putLong("chill_carry", carry);
    }
}
