package com.naverene.stevespantry.menu;

import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.block.IceboxBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IntArray;

/** Three rows of food like a chest, plus the ice slot in a side panel. */
public class IceboxMenu extends Container {
    private static final int ROWS = 3;
    /** Where the ice slot sits, relative to the main panel (it's in a panel off the right edge). */
    public static final int ICE_X = 184;
    public static final int ICE_Y = 18;

    private final IInventory container;
    private final IIntArray data;

    public IceboxMenu(int containerId, PlayerInventory inventory) {
        this(containerId, inventory, new Inventory(IceboxBlockEntity.SIZE + 1), new IntArray(2));
    }

    public IceboxMenu(int containerId, PlayerInventory inventory, IInventory container, IIntArray data) {
        super(ModRegistries.ICEBOX_MENU.get(), containerId);
        checkContainerSize(container, IceboxBlockEntity.SIZE + 1);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.data = data;
        container.startOpen(inventory.player);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        addSlot(new Slot(container, IceboxBlockEntity.ICE_SLOT, ICE_X, ICE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return IceboxBlockEntity.coolantTicks(stack) > 0;
            }
        });

        int playerTop = 18 + ROWS * 18 + 13;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, playerTop + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, playerTop + 58));
        }
        addDataSlots(data);
    }

    /** Hundredths of a day of cold left from melted ice. */
    public int coldLeft() {
        return data.get(0);
    }

    /** Hundredths of a day the last piece of ice was worth. */
    public int coldMax() {
        return data.get(1);
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int boxEnd = IceboxBlockEntity.SIZE + 1;
        if (index < boxEnd) {
            if (!moveItemStackTo(stack, boxEnd, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (IceboxBlockEntity.coolantTicks(stack) > 0
                && moveItemStackTo(stack, IceboxBlockEntity.ICE_SLOT, boxEnd, false)) {
            // Ice goes to the ice slot first; whatever doesn't fit falls through below.
        } else if (!moveItemStackTo(stack, 0, IceboxBlockEntity.SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(PlayerEntity player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
