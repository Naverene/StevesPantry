package com.naverene.stevespantry.client;

import com.naverene.stevespantry.block.IceboxBlockEntity;
import com.naverene.stevespantry.menu.IceboxMenu;
import com.naverene.stevespantry.reference.Reference;
import java.util.Collections;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

/** The vanilla three-row chest screen, with a side panel holding the ice slot and a cold gauge. */
public class IceboxScreen extends GuiContainer {
    private static final ResourceLocation CHEST = new ResourceLocation("textures/gui/container/generic_54.png");
    private static final int ROWS = 3;
    private static final int CHEST_WIDTH = 176;
    // Side panel, relative to the main panel.
    private static final int PANEL_X = 179;
    private static final int PANEL_Y = 4;
    private static final int PANEL_W = 26;
    private static final int PANEL_H = 64;
    private static final int GAUGE_X = IceboxMenu.ICE_X + 3;
    private static final int GAUGE_Y = IceboxMenu.ICE_Y + 22;
    private static final int GAUGE_W = 10;
    private static final int GAUGE_H = 22;

    private final IceboxMenu menu;
    private final InventoryPlayer inventory;

    public IceboxScreen(InventoryPlayer inventory, IceboxBlockEntity icebox) {
        this(new IceboxMenu(inventory, icebox), inventory);
    }

    private IceboxScreen(IceboxMenu menu, InventoryPlayer inventory) {
        super(menu);
        this.menu = menu;
        this.inventory = inventory;
        // 1.7.10 treats clicks outside xSize as "outside the screen" and drops the held stack,
        // so the screen is widened to cover the side panel.
        xSize = PANEL_X + PANEL_W;
        ySize = 114 + ROWS * 18;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        super.drawScreen(mouseX, mouseY, partialTick);
        if (func_146978_c(GAUGE_X, GAUGE_Y, GAUGE_W, GAUGE_H, mouseX, mouseY)) {
            String key = "gui." + Reference.MODID + ".icebox.";
            String text = menu.coldLeft() > 0
                    ? StatCollector.translateToLocalFormatted(key + "cold", String.format("%.2f", menu.coldLeft() / 100F))
                    : StatCollector.translateToLocal(key + "no_ice");
            func_146283_a(Collections.singletonList(text), mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(StatCollector.translateToLocal("container." + Reference.MODID + ".icebox"),
                8, 6, 0x404040);
        fontRendererObj.drawString(StatCollector.translateToLocal(inventory.getInventoryName()),
                8, ySize - 96 + 2, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY) {
        int x = guiLeft;
        int y = guiTop;
        GL11.glColor4f(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(CHEST);
        drawTexturedModalRect(x, y, 0, 0, CHEST_WIDTH, ROWS * 18 + 17);
        drawTexturedModalRect(x, y + ROWS * 18 + 17, 0, 126, CHEST_WIDTH, 96);

        // Side panel in the vanilla bevelled grey.
        int px = x + PANEL_X;
        int py = y + PANEL_Y;
        drawRect(px, py, px + PANEL_W, py + PANEL_H, 0xFF000000);
        drawRect(px + 1, py + 1, px + PANEL_W - 1, py + PANEL_H - 1, 0xFFFFFFFF);
        drawRect(px + 2, py + 2, px + PANEL_W - 1, py + PANEL_H - 1, 0xFF555555);
        drawRect(px + 2, py + 2, px + PANEL_W - 2, py + PANEL_H - 2, 0xFFC6C6C6);
        // Ice slot, borrowing a slot from the chest texture.
        GL11.glColor4f(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(CHEST);
        drawTexturedModalRect(x + IceboxMenu.ICE_X - 1, y + IceboxMenu.ICE_Y - 1, 7, 17, 18, 18);

        // Cold gauge, filling from the bottom.
        int gx = x + GAUGE_X;
        int gy = y + GAUGE_Y;
        drawRect(gx, gy, gx + GAUGE_W, gy + GAUGE_H, 0xFF373737);
        drawRect(gx + 1, gy + 1, gx + GAUGE_W - 1, gy + GAUGE_H - 1, 0xFF8B8B8B);
        int max = Math.max(menu.coldMax(), menu.coldLeft());
        if (max > 0 && menu.coldLeft() > 0) {
            int inner = GAUGE_H - 2;
            int filled = Math.max(1, Math.round(inner * (float) menu.coldLeft() / max));
            drawRect(gx + 1, gy + 1 + inner - filled, gx + GAUGE_W - 1, gy + GAUGE_H - 1, 0xFF7FC8FF);
        }
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }
}
