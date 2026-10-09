package com.naverene.stevespantry.block;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.reference.Reference;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the freezer's cold storage (a double chest's worth) and runs the multiblock. Once a second
 * it checks the shell, pulls items out of input buses, tops up output buses, and, while it has
 * power, chills everything in storage and in its buses with the same {@link Chiller} the Icebox
 * uses, only harder. Power is only drawn while there is food inside. Without power, or with a
 * broken shell, it is just a big box and dishes age normally.
 */
public class FreezerControllerBlockEntity extends BaseContainerBlockEntity {
    public static final int SIZE = 54;
    /** Dishes inside spoil this many times slower while it has power. */
    public static final int SLOWDOWN = 10;
    /** Power drawn per tick while chilling food. */
    public static final int FE_PER_TICK = 20;
    public static final int WALK_IN_SIZE = 5;
    public static final int COMPACT_SIZE = 3;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final Chiller chiller = new Chiller(SLOWDOWN);

    public FreezerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.FREEZER_CONTROLLER_BLOCK_ENTITY.get(), pos, state);
    }

    /** The shell's working parts, or why there's no working shell. {@code unloaded} means part of it is in an unloaded chunk. */
    public record Scan(@Nullable Parts parts, Component problem, boolean unloaded) {}

    public record Parts(List<FreezerBusBlockEntity> inputs, List<FreezerBusBlockEntity> outputs,
                        List<FreezerEnergyHatchBlockEntity> hatches) {}

    /**
     * Checks the shell. The freezer is either the walk-in size (5x5x5, a 3x3x3 room inside) or the
     * compact size (3x3x3, one empty block inside). Which one is meant is read from the block two
     * behind the controller: that's the far wall of a compact freezer but open floor space in a
     * walk-in one.
     */
    public Scan scan() {
        Direction back = getBlockState().getValue(FreezerControllerBlock.FACING).getOpposite();
        BlockPos probe = worldPosition.relative(back, 2);
        if (!level.isLoaded(probe)) {
            return new Scan(null, message("unloaded"), true);
        }
        boolean walkIn = isOpen(probe);
        return scan(walkIn ? WALK_IN_SIZE : COMPACT_SIZE);
    }

    private Scan scan(int size) {
        int half = size / 2;
        Direction back = getBlockState().getValue(FreezerControllerBlock.FACING).getOpposite();
        BlockPos center = worldPosition.relative(back, half);
        Parts parts = new Parts(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-half, -half, -half), center.offset(half, half, half))) {
            if (pos.equals(worldPosition)) {
                continue;
            }
            if (!level.isLoaded(pos)) {
                return new Scan(null, message("unloaded"), true);
            }
            BlockState state = level.getBlockState(pos);
            int dx = Math.abs(pos.getX() - center.getX());
            int dy = Math.abs(pos.getY() - center.getY());
            int dz = Math.abs(pos.getZ() - center.getZ());
            int faces = (dx == half ? 1 : 0) + (dy == half ? 1 : 0) + (dz == half ? 1 : 0);
            if (faces == 0) {
                if (!isOpen(pos)) {
                    return new Scan(null, message("not_hollow", state.getBlock().getName(),
                            pos.getX(), pos.getY(), pos.getZ()), false);
                }
                continue;
            }
            if (state.is(ModRegistries.FREEZER_CASING.get())) {
                continue;
            }
            // A walk-in freezer needs a way in: doors may go anywhere in a side wall except its edges.
            if (size == WALK_IN_SIZE && faces == 1 && dy < half && state.is(BlockTags.DOORS)) {
                continue;
            }
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof FreezerBusBlockEntity bus) {
                (bus.isOutput() ? parts.outputs() : parts.inputs()).add(bus);
            } else if (entity instanceof FreezerEnergyHatchBlockEntity hatch) {
                parts.hatches().add(hatch);
            } else {
                return new Scan(null, message("wrong_block", state.getBlock().getName(),
                        pos.getX(), pos.getY(), pos.getZ()), false);
            }
        }
        if (parts.hatches().isEmpty()) {
            return new Scan(null, message("no_energy"), false);
        }
        return new Scan(parts, Component.empty(), false);
    }

    /** Inside space must be walkable: air, or things without collision like torches and signs. */
    private boolean isOpen(BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private static Component message(String key, Object... args) {
        return Component.translatable("message." + Reference.MODID + ".freezer." + key, args);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FreezerControllerBlockEntity freezer) {
        long now = level.getGameTime();
        long elapsed = freezer.chiller.due(now);
        if (elapsed < 0) {
            return;
        }
        Scan scan = freezer.scan();
        if (scan.unloaded()) {
            // Try again later; the time adds up and is charged for once the whole shell is back.
            return;
        }
        Parts parts = scan.parts();
        if (parts == null) {
            freezer.chiller.refund(now, 0);
            freezer.setChanged();
            return;
        }
        freezer.moveItems(parts);

        List<ItemStack> contents = new ArrayList<>(freezer.items);
        parts.inputs().forEach(bus -> contents.addAll(bus.getItems()));
        parts.outputs().forEach(bus -> contents.addAll(bus.getItems()));
        long powered = 0;
        if (contents.stream().anyMatch(stack -> stack.has(ModRegistries.FRESHNESS))) {
            long stored = 0;
            for (FreezerEnergyHatchBlockEntity hatch : parts.hatches()) {
                stored += hatch.energy().getEnergyStored();
            }
            powered = Math.min(elapsed, stored / FE_PER_TICK);
            long owed = powered * FE_PER_TICK;
            for (FreezerEnergyHatchBlockEntity hatch : parts.hatches()) {
                owed -= hatch.energy().drain((int) Math.min(Integer.MAX_VALUE, owed));
            }
        }
        Chiller.chill(contents, freezer.chiller.refund(now, powered), now);
        freezer.setChanged();
        parts.inputs().forEach(BlockEntity::setChanged);
        parts.outputs().forEach(BlockEntity::setChanged);
    }

    /** Empties input buses into storage, then tops up output buses from storage. */
    private void moveItems(Parts parts) {
        IItemHandler storage = new InvWrapper(this);
        for (FreezerBusBlockEntity bus : parts.inputs()) {
            for (int slot = 0; slot < bus.getContainerSize(); slot++) {
                ItemStack stack = bus.getItem(slot);
                if (!stack.isEmpty()) {
                    bus.setItem(slot, ItemHandlerHelper.insertItemStacked(storage, stack, false));
                }
            }
        }
        for (FreezerBusBlockEntity bus : parts.outputs()) {
            IItemHandler out = new InvWrapper(bus);
            for (int slot = 0; slot < SIZE; slot++) {
                ItemStack stack = items.get(slot);
                if (!stack.isEmpty()) {
                    setItem(slot, ItemHandlerHelper.insertItemStacked(out, stack, false));
                }
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + Reference.MODID + ".freezer");
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
        return ChestMenu.sixRows(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        chiller.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        chiller.save(tag);
    }
}
