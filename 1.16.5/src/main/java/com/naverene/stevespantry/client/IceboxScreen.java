package com.naverene.stevespantry.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.naverene.stevespantry.menu.IceboxMenu;
import com.naverene.stevespantry.reference.Reference;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

/** The vanilla three-row chest screen, with a side panel holding the ice slot and a cold gauge. */
public class IceboxScreen extends ContainerScreen<IceboxMenu> {
    private static final ResourceLocation CHEST = new ResourceLocation("textures/gui/container/generic_54.png");
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

    public IceboxScreen(IceboxMenu menu, PlayerInventory inventory, ITextComponent title) {
        super(menu, inventory, title);
        imageHeight = 114 + ROWS * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(MatrixStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        renderTooltip(poseStack, mouseX, mouseY);
        if (isHovering(GAUGE_X, GAUGE_Y, GAUGE_W, GAUGE_H, mouseX, mouseY)) {
            String key = "gui." + Reference.MODID + ".icebox.";
            ITextComponent text = menu.coldLeft() > 0
                    ? new TranslationTextComponent(key + "cold", String.format("%.2f", menu.coldLeft() / 100F))
                    : new TranslationTextComponent(key + "no_ice");
            renderTooltip(poseStack, text, mouseX, mouseY);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void renderBg(MatrixStack poseStack, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        RenderSystem.color4f(1F, 1F, 1F, 1F);
        minecraft.getTextureManager().bind(CHEST);
        blit(poseStack, x, y, 0, 0, imageWidth, ROWS * 18 + 17);
        blit(poseStack, x, y + ROWS * 18 + 17, 0, 126, imageWidth, 96);

        // Side panel in the vanilla bevelled grey.
        int px = x + PANEL_X;
        int py = y + PANEL_Y;
        fill(poseStack, px, py, px + PANEL_W, py + PANEL_H, 0xFF000000);
        fill(poseStack, px + 1, py + 1, px + PANEL_W - 1, py + PANEL_H - 1, 0xFFFFFFFF);
        fill(poseStack, px + 2, py + 2, px + PANEL_W - 1, py + PANEL_H - 1, 0xFF555555);
        fill(poseStack, px + 2, py + 2, px + PANEL_W - 2, py + PANEL_H - 2, 0xFFC6C6C6);
        // Ice slot, borrowing a slot from the chest texture (fill leaves the colour state alone, but rebind to be safe).
        RenderSystem.color4f(1F, 1F, 1F, 1F);
        minecraft.getTextureManager().bind(CHEST);
        blit(poseStack, x + IceboxMenu.ICE_X - 1, y + IceboxMenu.ICE_Y - 1, 7, 17, 18, 18);

        // Cold gauge, filling from the bottom.
        int gx = x + GAUGE_X;
        int gy = y + GAUGE_Y;
        fill(poseStack, gx, gy, gx + GAUGE_W, gy + GAUGE_H, 0xFF373737);
        fill(poseStack, gx + 1, gy + 1, gx + GAUGE_W - 1, gy + GAUGE_H - 1, 0xFF8B8B8B);
        int max = Math.max(menu.coldMax(), menu.coldLeft());
        if (max > 0 && menu.coldLeft() > 0) {
            int inner = GAUGE_H - 2;
            int filled = Math.max(1, Math.round(inner * (float) menu.coldLeft() / max));
            fill(poseStack, gx + 1, gy + 1 + inner - filled, gx + GAUGE_W - 1, gy + GAUGE_H - 1, 0xFF7FC8FF);
        }
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        boolean inPanel = mouseX >= guiLeft + PANEL_X && mouseX < guiLeft + PANEL_X + PANEL_W
                && mouseY >= guiTop + PANEL_Y && mouseY < guiTop + PANEL_Y + PANEL_H;
        return !inPanel && super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, button);
    }
}
