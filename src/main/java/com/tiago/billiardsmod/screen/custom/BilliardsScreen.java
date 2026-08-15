package com.tiago.billiardsmod.screen.custom;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tiago.billiardsmod.BilliardsMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BilliardsScreen extends HandledScreen<BilliardsScreenHandler> {

    public static final Identifier GUI_TEXTURE =
            Identifier.of(BilliardsMod.MOD_ID, "textures/gui/billiards/billiards_gui.png");

    private static final int TABLE_LEFT_MARGIN = 12;
    private static final int TABLE_TOP_MARGIN = 12;
    private static final int TABLE_WIDTH = 130;
    private static final int TABLE_HEIGHT = 160;
    private static final int TABLE_BORDER = 7;
    private static final int POCKET_SIZE = 6;

    private static final int SIDE_GAP = 10;
    private static final int FORCE_LINE_HEIGHT = 150;
    private static final int FORCE_LINE_GAP = 6;
    private static final int SLOT_SIZE = 18;

    private static final int COLOR_WOOD = 0xFF4E2E1A;
    private static final int COLOR_FELT = 0xFF1B6B3A;
    private static final int COLOR_POCKET = 0xFF000000;
    private static final int COLOR_FORCE_LINE = 0xFFAAAAAA;

    private int getTableLeft() {
        return (width - backgroundWidth) / 2 + TABLE_LEFT_MARGIN;
    }

    private int getTableTop() {
        return (height - backgroundHeight) / 2 + TABLE_TOP_MARGIN;
    }

    private int getTableRight() {
        return getTableLeft() + TABLE_WIDTH;
    }

    private int getTableBottom() {
        return getTableTop() + TABLE_HEIGHT;
    }

    private void drawTable(DrawContext context) {
        int left = getTableLeft();
        int top = getTableTop();
        int right = getTableRight();
        int bottom = getTableBottom();

        context.fill(left - TABLE_BORDER, top - TABLE_BORDER,
                right + TABLE_BORDER, bottom + TABLE_BORDER, COLOR_WOOD);

        context.fill(left, top, right, bottom, COLOR_FELT);

        drawPockets(context, left, top, right, bottom);
    }

    private void drawPockets(DrawContext context, int left, int top, int right, int bottom) {
        int half = POCKET_SIZE / 2;
        int midX = (left + right) / 2;

        int[][] pocketPositions = {
                {left, top},
                {midX, top},
                {right, top},
                {left, bottom},
                {midX, bottom},
                {right, bottom}
        };

        for (int[] pos : pocketPositions) {
            int x = pos[0];
            int y = pos[1];
            context.fill(x - half, y - half, x + half, y + half, COLOR_POCKET);
        }
    }

    private void drawForceLinePlaceholder(DrawContext context) {
        int panelLeft = (width - backgroundWidth) / 2;
        int panelTop = (height - backgroundHeight) / 2;

        int lineX = panelLeft + handler.getCueSlotX();
        int slotY = panelTop + handler.getCueSlotY();
        int lineY = slotY - FORCE_LINE_GAP - FORCE_LINE_HEIGHT;

        context.fill(lineX, lineY, lineX + SLOT_SIZE, lineY + FORCE_LINE_HEIGHT, COLOR_FORCE_LINE);
    }

    public BilliardsScreen(BilliardsScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 178;
        this.backgroundHeight = 211;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        context.drawTexture(GUI_TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight);

        drawTable(context);
        drawForceLinePlaceholder(context);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        //removi o titulo por enquanto
        //context.drawText(textRenderer, title, titleX, titleY, 0x404040, false);
    }
}