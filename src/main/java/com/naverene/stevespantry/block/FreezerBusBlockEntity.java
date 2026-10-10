package com.naverene.stevespantry.block;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.reference.Reference;
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
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Nine slots. Automation can only insert into an input bus and only extract from an output bus. */
public class FreezerBusBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SIZE = 9;
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public FreezerBusBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.FREEZER_BUS_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isOutput() {
        return getBlockState().getBlock() instanceof FreezerBusBlock bus && bus.isOutput();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + Reference.MODID + (isOutput() ? ".freezer_output_bus" : ".freezer_input_bus"));
    }

    @Override
    public NonNullList<ItemStack> getItems() {
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
        return new DispenserMenu(containerId, inventory, this);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return !isOutput();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isOutput();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }
}
