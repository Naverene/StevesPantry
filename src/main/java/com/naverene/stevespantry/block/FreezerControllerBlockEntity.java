package com.naverene.stevespantry.block;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.reference.Reference;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * Runs the freezer, GregTech cleanroom style: it has no storage of its own. Once a second it checks
 * the shell, and while it has power it chills every container standing inside the room (chests,
 * barrels, modded storage) with the same {@link Chiller} the Icebox uses, only harder. Buses in the
 * walls pass items through to the container right behind them. Power is only drawn while there is
 * food inside. Without power, or with a broken shell, food inside ages normally.
 */
public class FreezerControllerBlockEntity extends BlockEntity {
    /** Dishes inside spoil this many times slower while it has power. */
    public static final int SLOWDOWN = 10;
    /** Power drawn per tick while chilling food. */
    public static final int FE_PER_TICK = 20;
    public static final int WALK_IN_SIZE = 5;
    public static final int COMPACT_SIZE = 3;

    private final Chiller chiller = new Chiller(SLOWDOWN);

    public FreezerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.FREEZER_CONTROLLER_BLOCK_ENTITY.get(), pos, state);
    }

    /** The shell's working parts, or why there's no working shell. {@code unloaded} means part of it is in an unloaded chunk. */
    public record Scan(@Nullable Parts parts, Component problem, boolean unloaded) {}

    /** A bus in a wall, and which way the room is from it. */
    public record Bus(FreezerBusBlockEntity bus, Direction inward) {}

    public record Parts(int size, List<Bus> buses, List<FreezerEnergyHatchBlockEntity> hatches, List<BlockPos> inside) {}

    /**
     * Checks the shell. The freezer is either the walk-in size (5x5x5, a 3x3x3 room inside) or the
     * compact size (3x3x3, one block inside). Which one is meant is read from the block two behind
     * the controller: that's the far wall of a compact freezer but inside the room of a walk-in one.
     */
    public Scan scan() {
        Direction back = getBlockState().getValue(FreezerControllerBlock.FACING).getOpposite();
        BlockPos probe = worldPosition.relative(back, 2);
        if (!level.isLoaded(probe)) {
            return new Scan(null, message("unloaded"), true);
        }
        return scan(isShell(probe) ? COMPACT_SIZE : WALK_IN_SIZE);
    }

    private boolean isShell(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(ModRegistries.FREEZER_CASING.get())
                || level.getBlockEntity(pos) instanceof FreezerBusBlockEntity
                || level.getBlockEntity(pos) instanceof FreezerEnergyHatchBlockEntity;
    }

    private Scan scan(int size) {
        int half = size / 2;
        Direction back = getBlockState().getValue(FreezerControllerBlock.FACING).getOpposite();
        BlockPos center = worldPosition.relative(back, half);
        Parts parts = new Parts(size, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-half, -half, -half), center.offset(half, half, half))) {
            if (pos.equals(worldPosition)) {
                continue;
            }
            if (!level.isLoaded(pos)) {
                return new Scan(null, message("unloaded"), true);
            }
            BlockState state = level.getBlockState(pos);
            int dx = pos.getX() - center.getX();
            int dy = pos.getY() - center.getY();
            int dz = pos.getZ() - center.getZ();
            int faces = (Math.abs(dx) == half ? 1 : 0) + (Math.abs(dy) == half ? 1 : 0) + (Math.abs(dz) == half ? 1 : 0);
            if (faces == 0) {
                // The room: anything goes. Containers in here get chilled.
                if (level.getBlockEntity(pos) != null) {
                    parts.inside().add(pos.immutable());
                }
                continue;
            }
            if (state.is(ModRegistries.FREEZER_CASING.get())) {
                continue;
            }
            // A walk-in freezer needs a way in: doors may go anywhere in a side wall except its edges.
            if (size == WALK_IN_SIZE && faces == 1 && Math.abs(dy) < half && state.is(BlockTags.DOORS)) {
                continue;
            }
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof FreezerBusBlockEntity bus) {
                if (faces != 1) {
                    return new Scan(null, message("bus_on_edge", pos.getX(), pos.getY(), pos.getZ()), false);
                }
                Direction inward = Math.abs(dx) == half ? (dx > 0 ? Direction.WEST : Direction.EAST)
                        : Math.abs(dy) == half ? (dy > 0 ? Direction.DOWN : Direction.UP)
                        : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
                parts.buses().add(new Bus(bus, inward));
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

    /** What right-clicking the controller says. */
    public Component status() {
        Scan scan = scan();
        if (scan.parts() == null) {
            return scan.problem();
        }
        Parts parts = scan.parts();
        long stored = parts.hatches().stream().mapToLong(h -> h.energy().getEnergyStored()).sum();
        return message(stored >= FE_PER_TICK ? "running" : "no_power",
                parts.size(), parts.size(), parts.size(), countChilled(parts), stored);
    }

    private long countChilled(Parts parts) {
        return parts.inside().stream().filter(pos -> handlerAt(pos, null) != null).count();
    }

    static Component message(String key, Object... args) {
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
        freezer.passThrough(parts);

        Contents contents = freezer.contents(parts);
        long powered = 0;
        if (contents.hasFood()) {
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
        long refund = freezer.chiller.refund(now, powered);
        if (refund > 0) {
            contents.chill(refund, now);
        }
        freezer.setChanged();
    }

    /** Input buses push into the container behind them; output buses pull from it. */
    private void passThrough(Parts parts) {
        for (Bus entry : parts.buses()) {
            FreezerBusBlockEntity bus = entry.bus();
            IItemHandler inside = handlerAt(bus.getBlockPos().relative(entry.inward()), entry.inward().getOpposite());
            if (inside == null) {
                continue;
            }
            IItemHandler busItems = new InvWrapper(bus);
            if (bus.isOutput()) {
                move(inside, busItems);
            } else {
                move(busItems, inside);
            }
        }
    }

    private static void move(IItemHandler from, IItemHandler to) {
        for (int slot = 0; slot < from.getSlots(); slot++) {
            ItemStack available = from.extractItem(slot, Integer.MAX_VALUE, true);
            if (available.isEmpty()) {
                continue;
            }
            int fits = available.getCount() - ItemHandlerHelper.insertItemStacked(to, available, true).getCount();
            if (fits > 0) {
                ItemHandlerHelper.insertItemStacked(to, from.extractItem(slot, fits, false), false);
            }
        }
    }

    @Nullable
    private IItemHandler handlerAt(BlockPos pos, @Nullable Direction side) {
        // An Icebox inside already chills itself; chilling it twice would freeze time entirely.
        if (level.getBlockEntity(pos) instanceof IceboxBlockEntity) {
            return null;
        }
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
    }

    /** Everything the freezer chills: what's in its buses and in the containers inside the room. */
    private Contents contents(Parts parts) {
        Contents contents = new Contents();
        for (Bus bus : parts.buses()) {
            contents.add(bus.bus());
        }
        for (BlockPos pos : parts.inside()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof IceboxBlockEntity) {
                continue;
            }
            if (entity instanceof Container container) {
                contents.add(container);
            } else if (handlerAt(pos, null) instanceof IItemHandlerModifiable handler) {
                contents.handlers.add(handler);
            }
        }
        return contents;
    }

    private static final class Contents {
        final List<Container> containers = new ArrayList<>();
        final List<IItemHandlerModifiable> handlers = new ArrayList<>();

        void add(Container container) {
            containers.add(container);
        }

        boolean hasFood() {
            for (Container container : containers) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    if (container.getItem(i).has(ModRegistries.FRESHNESS)) {
                        return true;
                    }
                }
            }
            for (IItemHandlerModifiable handler : handlers) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    if (handler.getStackInSlot(i).has(ModRegistries.FRESHNESS)) {
                        return true;
                    }
                }
            }
            return false;
        }

        void chill(long refund, long now) {
            for (Container container : containers) {
                List<ItemStack> stacks = new ArrayList<>();
                for (int i = 0; i < container.getContainerSize(); i++) {
                    stacks.add(container.getItem(i));
                }
                Chiller.chill(stacks, refund, now);
                container.setChanged();
            }
            // Modded storage that isn't a plain Container may hand out copies, so write changes back.
            for (IItemHandlerModifiable handler : handlers) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (stack.has(ModRegistries.FRESHNESS)) {
                        ItemStack chilled = stack.copy();
                        Chiller.chill(List.of(chilled), refund, now);
                        handler.setStackInSlot(i, chilled);
                    }
                }
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        chiller.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        chiller.save(tag);
    }
}
