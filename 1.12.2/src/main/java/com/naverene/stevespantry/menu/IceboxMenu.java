package com.naverene.stevespantry.menu;

import com.naverene.stevespantry.block.IceboxBlockEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Three rows of food like a chest, plus the ice slot in a side panel. */
public class IceboxMenu extends Container {
    private static final int ROWS = 3;
    /** Where the ice slot sits, relative to the main panel (it's in a panel off the right edge). */
    public static final int ICE_X = 184;
    public static final int ICE_Y = 18;

    private final IInventory container;
    /** The cold gauge: last values sent on the server, received values on the client. */
    private final int[] data = new int[2];

    public IceboxMenu(InventoryPlayer inventory, IInventory container) {
        this.container = container;
        container.openInventory(inventory.player);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        addSlotToContainer(new Slot(container, IceboxBlockEntity.ICE_SLOT, ICE_X, ICE_Y) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return IceboxBlockEntity.coolantTicks(stack) > 0;
            }
        });

        int playerTop = 18 + ROWS * 18 + 13;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, playerTop + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(inventory, col, 8 + col * 18, playerTop + 58));
        }
    }

    /** Hundredths of a day of cold left from melted ice. */
    public int coldLeft() {
        return data[0];
    }

    /** Hundredths of a day the last piece of ice was worth. */
    public int coldMax() {
        return data[1];
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendAllWindowProperties(this, container);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (int i = 0; i < data.length; i++) {
            int value = container.getField(i);
            if (value != data[i]) {
                for (IContainerListener listener : listeners) {
                    listener.sendWindowProperty(this, i, value);
                }
                data[i] = value;
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int value) {
        if (id >= 0 && id < data.length) {
            data[id] = value;
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        int boxEnd = IceboxBlockEntity.SIZE + 1;
        if (index < boxEnd) {
            if (!mergeItemStack(stack, boxEnd, inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (IceboxBlockEntity.coolantTicks(stack) > 0
                && mergeItemStack(stack, IceboxBlockEntity.ICE_SLOT, boxEnd, false)) {
            // Ice goes to the ice slot first; whatever doesn't fit falls through below.
        } else if (!mergeItemStack(stack, 0, IceboxBlockEntity.SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        return original;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return container.isUsableByPlayer(player);
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        container.closeInventory(player);
    }
}
