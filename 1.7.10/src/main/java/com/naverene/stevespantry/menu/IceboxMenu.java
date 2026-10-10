package com.naverene.stevespantry.menu;

import com.naverene.stevespantry.block.IceboxBlockEntity;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Three rows of food like a chest, plus the ice slot in a side panel. */
public class IceboxMenu extends Container {
    private static final int ROWS = 3;
    /** Where the ice slot sits, relative to the main panel (it's in a panel off the right edge). */
    public static final int ICE_X = 184;
    public static final int ICE_Y = 18;

    private final IceboxBlockEntity container;
    /** Server: last values sent. Client: last values received. Hundredths of a day. */
    private final int[] data = {-1, -1};

    public IceboxMenu(InventoryPlayer inventory, IceboxBlockEntity container) {
        this.container = container;
        container.openInventory();

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
        return Math.max(0, data[0]);
    }

    /** Hundredths of a day the last piece of ice was worth. */
    public int coldMax() {
        return Math.max(0, data[1]);
    }

    @Override
    public void addCraftingToCrafters(ICrafting crafter) {
        super.addCraftingToCrafters(crafter);
        for (int i = 0; i < data.length; i++) {
            crafter.sendProgressBarUpdate(this, i, container.coldData(i));
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (int i = 0; i < data.length; i++) {
            int value = container.coldData(i);
            if (value != data[i]) {
                for (Object crafter : crafters) {
                    ((ICrafting) crafter).sendProgressBarUpdate(this, i, value);
                }
                data[i] = value;
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int index, int value) {
        if (index >= 0 && index < data.length) {
            data[index] = value;
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return null;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        int boxEnd = IceboxBlockEntity.SIZE + 1;
        if (index < boxEnd) {
            if (!mergeItemStack(stack, boxEnd, inventorySlots.size(), true)) {
                return null;
            }
        } else if (IceboxBlockEntity.coolantTicks(stack) > 0
                && mergeItemStack(stack, IceboxBlockEntity.ICE_SLOT, boxEnd, false)) {
            // Ice goes to the ice slot first; whatever doesn't fit falls through below.
        } else if (!mergeItemStack(stack, 0, IceboxBlockEntity.SIZE, false)) {
            return null;
        }
        if (stack.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }
        return original;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return container.isUseableByPlayer(player);
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        container.closeInventory();
    }
}
