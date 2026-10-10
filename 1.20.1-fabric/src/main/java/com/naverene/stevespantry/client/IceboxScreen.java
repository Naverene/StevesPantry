package com.naverene.stevespantry.client;

import com.naverene.stevespantry.menu.IceboxMenu;
import com.naverene.stevespantry.reference.Reference;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The vanilla three-row chest screen, with a side panel holding the ice slot and a cold gauge. */
public class IceboxScreen extends AbstractContainerScreen<IceboxMenu> {
    private static final ResourceLocation CHEST =
            new ResourceLocation("textures/gui/container/generic_54.png");
    private static final int ROWS = 3;
    // Side panel, relative to the main panel.
    private static final int PANEL_X = 179;
    private static final int PANEL_Y = 4;
    private static final int PANEL_W = 26;
    private static final int PANEL_H = 64;
    private static final int GAUGE_X = IceboxMenu.ICE_X + 3;
    private static final int GAUGE_Y = IceboxMenu.ICE_Y + 22;
    private static final int GAUGE_W = 10;
    private static final int GAUGE_H = 22;

    public IceboxScreen(IceboxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 114 + ROWS * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1.20.1 screens draw their own dimmed background before the container.
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(GAUGE_X, GAUGE_Y, GAUGE_W, GAUGE_H, mouseX, mouseY)) {
            String key = "gui." + Reference.MODID + ".icebox.";
            Component text = menu.coldLeft() > 0
                    ? Component.translatable(key + "cold", String.format("%.2f", menu.coldLeft() / 100F))
                    : Component.translatable(key + "no_ice");
            graphics.renderTooltip(font, text, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.blit(CHEST, x, y, 0, 0, imageWidth, ROWS * 18 + 17);
        graphics.blit(CHEST, x, y + ROWS * 18 + 17, 0, 126, imageWidth, 96);

        // Side panel in the vanilla bevelled grey.
        int px = x + PANEL_X;
        int py = y + PANEL_Y;
        graphics.fill(px, py, px + PANEL_W, py + PANEL_H, 0xFF000000);
        graphics.fill(px + 1, py + 1, px + PANEL_W - 1, py + PANEL_H - 1, 0xFFFFFFFF);
        graphics.fill(px + 2, py + 2, px + PANEL_W - 1, py + PANEL_H - 1, 0xFF555555);
        graphics.fill(px + 2, py + 2, px + PANEL_W - 2, py + PANEL_H - 2, 0xFFC6C6C6);
        // Ice slot, borrowing a slot from the chest texture.
        graphics.blit(CHEST, x + IceboxMenu.ICE_X - 1, y + IceboxMenu.ICE_Y - 1, 7, 17, 18, 18);

        // Cold gauge, filling from the bottom.
        int gx = x + GAUGE_X;
        int gy = y + GAUGE_Y;
        graphics.fill(gx, gy, gx + GAUGE_W, gy + GAUGE_H, 0xFF373737);
        graphics.fill(gx + 1, gy + 1, gx + GAUGE_W - 1, gy + GAUGE_H - 1, 0xFF8B8B8B);
        int max = Math.max(menu.coldMax(), menu.coldLeft());
        if (max > 0 && menu.coldLeft() > 0) {
            int inner = GAUGE_H - 2;
            int filled = Math.max(1, Math.round(inner * (float) menu.coldLeft() / max));
            graphics.fill(gx + 1, gy + 1 + inner - filled, gx + GAUGE_W - 1, gy + GAUGE_H - 1, 0xFF7FC8FF);
        }
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        boolean inPanel = mouseX >= guiLeft + PANEL_X && mouseX < guiLeft + PANEL_X + PANEL_W
                && mouseY >= guiTop + PANEL_Y && mouseY < guiTop + PANEL_Y + PANEL_H;
        return !inPanel && super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, button);
    }
}
