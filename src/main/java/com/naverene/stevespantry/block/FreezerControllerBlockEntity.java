package com.naverene.stevespantry.block;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.reference.Reference;
import java.util.ArrayList;
import java.util.List;
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

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final Chiller chiller = new Chiller(SLOWDOWN);

    public FreezerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.FREEZER_CONTROLLER_BLOCK_ENTITY.get(), pos, state);
    }

    /** The shell's working parts, or why there's no working shell. {@code unloaded} means part of it is in an unloaded chunk. */
    public record Scan(@Nullable Parts parts, Component problem, boolean unloaded) {}

    public record Parts(List<FreezerBusBlockEntity> inputs, List<FreezerBusBlockEntity> outputs,
                        List<FreezerEnergyHatchBlockEntity> hatches) {}

    public BlockPos center() {
        return worldPosition.relative(getBlockState().getValue(FreezerControllerBlock.FACING).getOpposite());
    }

    public Scan scan() {
        Level level = this.level;
        BlockPos center = center();
        Parts parts = new Parts(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (pos.equals(worldPosition)) {
                continue;
            }
            if (!level.isLoaded(pos)) {
                return new Scan(null, message("unloaded"), true);
            }
            BlockState state = level.getBlockState(pos);
            if (pos.equals(center)) {
                if (!state.isAir()) {
                    return new Scan(null, message("not_hollow", state.getBlock().getName()), false);
                }
                continue;
            }
            if (state.is(ModRegistries.FREEZER_CASING.get())) {
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
