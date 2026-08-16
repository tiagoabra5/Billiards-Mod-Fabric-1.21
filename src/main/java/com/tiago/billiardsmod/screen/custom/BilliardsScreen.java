package com.tiago.billiardsmod.screen.custom;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tiago.billiardsmod.BilliardsMod;
import com.tiago.billiardsmod.billiards.Ball;
import com.tiago.billiardsmod.billiards.BallRack;
import com.tiago.billiardsmod.billiards.BilliardsPhysics;
import com.tiago.billiardsmod.billiards.TableBounds;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class BilliardsScreen extends HandledScreen<BilliardsScreenHandler> {

    public static final Identifier GUI_TEXTURE =
            Identifier.of(BilliardsMod.MOD_ID, "textures/gui/billiards/billiards_gui.png");

    private enum InteractionState {
        IDLE, AIMING, CHARGING
    }

    private InteractionState state = InteractionState.IDLE;
    private float aimAngle = 0f;
    private float power = 0f;

    private float aimLength = 40f;
    private static final float MIN_AIM_LENGTH = 10f;
    private static final float MAX_AIM_LENGTH = 100f;

    private static final float MAX_SHOT_SPEED = 6f;
    private static final float BALL_CLICK_TOLERANCE = 2f;

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
    private static final int COLOR_FORCE_FILL = 0xFFE0A030;
    private static final int COLOR_AIM_LINE = 0xFFFFFFFF;

    private static final float BALL_RADIUS = 4f;
    private static final int RACK_ROWS = 5;

    private List<Ball> balls;

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

    private TableBounds getTableBounds() {
        return new TableBounds(getTableLeft(), getTableTop(), getTableRight(), getTableBottom());
    }

    private List<float[]> getPocketPositions() {
        int left = getTableLeft();
        int top = getTableTop();
        int right = getTableRight();
        int bottom = getTableBottom();
        int midY = (top + bottom) / 2;

        return List.of(
                new float[]{left, top},
                new float[]{left, midY},
                new float[]{left, bottom},
                new float[]{right, top},
                new float[]{right, midY},
                new float[]{right, bottom}
        );
    }

    private void drawTable(DrawContext context) {
        int left = getTableLeft();
        int top = getTableTop();
        int right = getTableRight();
        int bottom = getTableBottom();

        context.fill(left - TABLE_BORDER, top - TABLE_BORDER,
                right + TABLE_BORDER, bottom + TABLE_BORDER, COLOR_WOOD);

        context.fill(left, top, right, bottom, COLOR_FELT);

        drawPockets(context);
    }

    private void drawPockets(DrawContext context) {
        int half = POCKET_SIZE / 2;
        for (float[] pos : getPocketPositions()) {
            int x = Math.round(pos[0]);
            int y = Math.round(pos[1]);
            context.fill(x - half, y - half, x + half, y + half, COLOR_POCKET);
        }
    }

    private int getForceBarLeft() {
        int panelLeft = (width - backgroundWidth) / 2;
        return panelLeft + handler.getCueSlotX();
    }

    private int getForceBarTop() {
        int panelTop = (height - backgroundHeight) / 2;
        int slotY = panelTop + handler.getCueSlotY();
        return slotY - FORCE_LINE_GAP - FORCE_LINE_HEIGHT;
    }

    private void drawForceBar(DrawContext context) {
        int lineX = getForceBarLeft();
        int lineY = getForceBarTop();

        context.fill(lineX, lineY, lineX + SLOT_SIZE, lineY + FORCE_LINE_HEIGHT, COLOR_FORCE_LINE);

        int filledHeight = Math.round(power * FORCE_LINE_HEIGHT);
        int filledTop = lineY + FORCE_LINE_HEIGHT - filledHeight;
        context.fill(lineX, filledTop, lineX + SLOT_SIZE, lineY + FORCE_LINE_HEIGHT, COLOR_FORCE_FILL);

        drawStackedText(context, "POWER", 0xFFFFFFFF);
    }

    private boolean isMouseOverForceBar(double mouseX, double mouseY) {
        int lineX = getForceBarLeft();
        int lineY = getForceBarTop();

        return mouseX >= lineX && mouseX <= lineX + SLOT_SIZE
                && mouseY >= lineY && mouseY <= lineY + FORCE_LINE_HEIGHT;
    }

    private float computePowerFromMouseY(double mouseY) {
        int lineY = getForceBarTop();
        float relative = (float) (mouseY - lineY) / FORCE_LINE_HEIGHT;
        return 1f - Math.max(0f, Math.min(1f, relative));
    }

    private Ball getCueBall() {
        for (Ball ball : balls) {
            if (ball.isCueBall() && !ball.pocketed) return ball;
        }
        return null;
    }

    private boolean isMouseOverBall(double mouseX, double mouseY, Ball ball) {
        double dx = mouseX - ball.x;
        double dy = mouseY - ball.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance <= ball.radius + BALL_CLICK_TOLERANCE;
    }

    private void drawAimLine(DrawContext context) {
        if (state != InteractionState.AIMING && state != InteractionState.CHARGING) return;

        Ball cueBall = getCueBall();
        if (cueBall == null) return;

        float endX = cueBall.x + (float) Math.cos(aimAngle) * aimLength;
        float endY = cueBall.y + (float) Math.sin(aimAngle) * aimLength;

        int steps = 15;
        for (int i = 0; i < steps; i++) {
            float t = i / (float) steps;
            int x = Math.round(cueBall.x + (endX - cueBall.x) * t);
            int y = Math.round(cueBall.y + (endY - cueBall.y) * t);
            context.fill(x - 1, y - 1, x + 1, y + 1, COLOR_AIM_LINE);
        }
    }

    private void shootCueBall() {
        Ball cueBall = getCueBall();
        if (cueBall == null) return;

        float speed = power * MAX_SHOT_SPEED;
        cueBall.vx = (float) Math.cos(aimAngle) * speed;
        cueBall.vy = (float) Math.sin(aimAngle) * speed;

        power = 0f;
    }

    public BilliardsScreen(BilliardsScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 178;
        this.backgroundHeight = 211;
    }

    @Override
    protected void init() {
        super.init();
        if (balls == null) {
            float spacing = BALL_RADIUS * 2f + 0.5f;
            float triangleHeight = (RACK_ROWS - 1) * spacing * 0.87f;
            float footSpotY = getTableTop() + BALL_RADIUS + 4 + triangleHeight;
            float headSpotY = getTableBottom() - 20;

            balls = BallRack.createStandardRack(
                    (getTableLeft() + getTableRight()) / 2f,
                    footSpotY,
                    headSpotY,
                    BALL_RADIUS
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (state == InteractionState.IDLE) {
                Ball cueBall = getCueBall();
                if (cueBall != null && isMouseOverBall(mouseX, mouseY, cueBall)) {
                    state = InteractionState.AIMING;
                    return true;
                }
            } else if (state == InteractionState.CHARGING && isMouseOverForceBar(mouseX, mouseY)) {
                power = computePowerFromMouseY(mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (state == InteractionState.AIMING) {
            Ball cueBall = getCueBall();
            if (cueBall != null) {
                double dx = mouseX - cueBall.x;
                double dy = mouseY - cueBall.y;
                aimAngle = (float) Math.atan2(dy, dx);

                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                aimLength = Math.max(MIN_AIM_LENGTH, Math.min(MAX_AIM_LENGTH, distance));
            }
            return true;
        } else if (state == InteractionState.CHARGING && isMouseOverForceBar(mouseX, mouseY)) {
            power = computePowerFromMouseY(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (state == InteractionState.AIMING) {
                state = InteractionState.CHARGING;
                power = 0f;
                return true;
            } else if (state == InteractionState.CHARGING) {
                if (power > 0f) {
                    shootCueBall();
                }
                state = InteractionState.IDLE;
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        BilliardsPhysics.update(balls, getTableBounds(), getPocketPositions(), 9f);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        context.drawTexture(GUI_TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight);

        drawTable(context);
        drawForceBar(context);
        BallRenderer.drawBalls(context, balls);
        drawAimLine(context);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        //removi o titulo por enquanto
        //context.drawText(textRenderer, title, titleX, titleY, 0x404040, false);
    }

    private void drawStackedText(DrawContext context, String text, int color) {
        int barLeft = getForceBarLeft();
        int barTop = getForceBarTop();
        int centerX = barLeft + SLOT_SIZE / 2;

        int lineHeight = textRenderer.fontHeight + 1;
        int totalHeight = text.length() * lineHeight;
        int startY = barTop + (FORCE_LINE_HEIGHT - totalHeight) / 2;

        for (int i = 0; i < text.length(); i++) {
            String letter = String.valueOf(text.charAt(i));
            int letterWidth = textRenderer.getWidth(letter);
            int x = centerX - letterWidth / 2;
            int y = startY + i * lineHeight;

            context.drawText(textRenderer, letter, x, y, color, true);
        }
    }
}