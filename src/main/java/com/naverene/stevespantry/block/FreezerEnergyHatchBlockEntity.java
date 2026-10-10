package com.naverene.stevespantry.block;

import com.naverene.stevespantry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

/** An FE buffer. Cables can only push energy in; only the controller draws it out. */
public class FreezerEnergyHatchBlockEntity extends BlockEntity {
    public static final int CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 1_000;

    private final HatchEnergy energy = new HatchEnergy();

    public FreezerEnergyHatchBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.FREEZER_ENERGY_HATCH_BLOCK_ENTITY.get(), pos, state);
    }

    public HatchEnergy energy() {
        return energy;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.set(tag.getInt("energy"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy.getEnergyStored());
    }

    public final class HatchEnergy extends EnergyStorage {
        HatchEnergy() {
            super(CAPACITY, MAX_RECEIVE, 0);
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int received = super.receiveEnergy(toReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }

        /** Takes up to {@code amount} FE for the controller, ignoring the no-extract rule cables see. */
        public int drain(int amount) {
            int drained = Math.min(energy, Math.max(0, amount));
            if (drained > 0) {
                energy -= drained;
                setChanged();
            }
            return drained;
        }

        void set(int value) {
            energy = Math.max(0, Math.min(capacity, value));
        }
    }
}
