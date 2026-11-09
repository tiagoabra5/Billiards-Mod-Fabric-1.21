package com.tiago.billiardsmod.screen.custom;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tiago.billiardsmod.BilliardsMod;
import com.tiago.billiardsmod.billiards.Ball;
import com.tiago.billiardsmod.billiards.BilliardsGame;
import com.tiago.billiardsmod.billiards.PlayerScore;
import com.tiago.billiardsmod.billiards.FoulReason;
import com.tiago.billiardsmod.network.ShootPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
    private long aimingTurnId;
    private int scoreScroll;

    private float aimLength = 40f;
    private static final float MIN_AIM_LENGTH = 10f;
    private static final float MAX_AIM_LENGTH = 100f;

    private static final float BALL_CLICK_TOLERANCE = 2f;

    private static final int TABLE_LEFT_MARGIN = 12;
    private static final int TABLE_TOP_MARGIN = 12;
    private static final int TABLE_WIDTH = BilliardsGame.TABLE_WIDTH;
    private static final int TABLE_HEIGHT = BilliardsGame.TABLE_HEIGHT;
    private static final int TABLE_BORDER = 7;
    private static final int POCKET_SIZE = 6;

    private static final int FORCE_LINE_HEIGHT = 150;
    private static final int FORCE_LINE_GAP = 6;
    private static final int SLOT_SIZE = 18;

    private static final int COLOR_WOOD = 0xFF4E2E1A;
    private static final int COLOR_FELT = 0xFF1B6B3A;
    private static final int COLOR_POCKET = 0xFF000000;
    private static final int COLOR_FORCE_LINE = 0xFFAAAAAA;
    private static final int COLOR_FORCE_FILL = 0xFFE0A030;
    private static final int COLOR_AIM_LINE = 0xFFFFFFFF;
    private static final int TABLE_PANEL_WIDTH = 178;
    private static final int SCORE_PANEL_WIDTH = 132;
    private static final int SCORE_PANEL_GAP = 8;
    private static final int SCORE_ROW_HEIGHT = 18;
    private static final int VISIBLE_SCORE_ROWS = 7;

    public BilliardsScreen(BilliardsScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = TABLE_PANEL_WIDTH + SCORE_PANEL_GAP + SCORE_PANEL_WIDTH;
        this.backgroundHeight = 211;
    }

    private List<Ball> getBalls() {
        return handler.getState().balls();
    }

    private boolean canShoot() {
        return client != null && client.player != null && handler.canShoot(client.player);
    }

    private void validateInteraction() {
        if (!canShoot() || (state != InteractionState.IDLE && aimingTurnId != handler.getState().turnId())) {
            state = InteractionState.IDLE;
            power = 0f;
        }
    }

    private float toScreenX(float logicalX) {
        return getTableLeft() + logicalX;
    }

    private float toScreenY(float logicalY) {
        return getTableTop() + logicalY;
    }

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

    private List<float[]> getPocketScreenPositions() {
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
        for (float[] pos : getPocketScreenPositions()) {
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

        drawStackedText(context, Text.translatable("screen.billiardsmod.power").getString(), 0xFFFFFFFF);
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
        for (Ball ball : getBalls()) {
            if (ball.isCueBall() && !ball.pocketed) return ball;
        }
        return null;
    }

    private boolean isMouseOverBall(double mouseX, double mouseY, Ball ball) {
        double dx = mouseX - toScreenX(ball.x);
        double dy = mouseY - toScreenY(ball.y);
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance <= ball.radius + BALL_CLICK_TOLERANCE;
    }

    private void drawAimLine(DrawContext context) {
        if (state != InteractionState.AIMING && state != InteractionState.CHARGING) return;

        Ball cueBall = getCueBall();
        if (cueBall == null) return;

        float startX = toScreenX(cueBall.x);
        float startY = toScreenY(cueBall.y);
        float endX = startX + (float) Math.cos(aimAngle) * aimLength;
        float endY = startY + (float) Math.sin(aimAngle) * aimLength;

        int steps = 15;
        for (int i = 0; i < steps; i++) {
            float t = i / (float) steps;
            int x = Math.round(startX + (endX - startX) * t);
            int y = Math.round(startY + (endY - startY) * t);
            context.fill(x - 1, y - 1, x + 1, y + 1, COLOR_AIM_LINE);
        }
    }

    private void shootCueBall() {
        if (!canShoot() || power <= 0f) return;

        ClientPlayNetworking.send(new ShootPacket(handler.syncId, handler.getTablePos(),
                handler.getState().turnId(), aimAngle, power));
        handler.markShotPending();
        power = 0f;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        validateInteraction();
        if (button == 0 && canShoot()) {
            if (state == InteractionState.IDLE) {
                Ball cueBall = getCueBall();
                if (cueBall != null && isMouseOverBall(mouseX, mouseY, cueBall)) {
                    state = InteractionState.AIMING;
                    aimingTurnId = handler.getState().turnId();
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
        validateInteraction();
        if (button == 0 && state == InteractionState.AIMING) {
            Ball cueBall = getCueBall();
            if (cueBall != null) {
                double screenCueX = toScreenX(cueBall.x);
                double screenCueY = toScreenY(cueBall.y);
                double dx = mouseX - screenCueX;
                double dy = mouseY - screenCueY;
                aimAngle = (float) Math.atan2(dy, dx);

                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                aimLength = Math.max(MIN_AIM_LENGTH, Math.min(MAX_AIM_LENGTH, distance));
            }
            return true;
        } else if (button == 0 && state == InteractionState.CHARGING && isMouseOverForceBar(mouseX, mouseY)) {
            power = computePowerFromMouseY(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        validateInteraction();
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
        validateInteraction();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        context.drawTexture(GUI_TEXTURE, x, y, 0, 0, TABLE_PANEL_WIDTH, backgroundHeight,
                TABLE_PANEL_WIDTH, backgroundHeight);

        drawTable(context);
        drawForceBar(context);
        BallRenderer.drawBalls(context, getBalls(), handler.getPreviousState().balls(),
                handler.getRenderProgress(), this::toScreenX, this::toScreenY);
        drawAimLine(context);
        drawTurnStatus(context);
        drawScoreboard(context);
    }

    private void drawTurnStatus(DrawContext context) {
        Text status;
        if (handler.getState().moving()) {
            status = Text.translatable("screen.billiardsmod.balls_moving");
        } else if (!handler.hasUsableCue()) {
            status = Text.translatable("screen.billiardsmod.insert_cue");
        } else if (canShoot()) {
            status = Text.translatable("screen.billiardsmod.your_turn");
        } else if (handler.getState().currentPlayer() != null) {
            status = Text.translatable("screen.billiardsmod.player_turn", handler.getState().currentPlayerName());
        } else {
            status = Text.translatable("screen.billiardsmod.no_players");
        }
        int panelTop = (height - backgroundHeight) / 2;
        int tableCenter = (width - backgroundWidth) / 2 + TABLE_PANEL_WIDTH / 2;
        context.drawCenteredTextWithShadow(textRenderer, status, tableCenter, panelTop - 11,
                canShoot() ? 0xFF80FF80 : 0xFFFFFFFF);
        int maxUses = handler.getMaxCueUses();
        String durability = maxUses > 0 ? handler.getRemainingCueUses() + "/" + maxUses : "--";
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.billiardsmod.players_and_cue", handler.getState().playerCount(),
                        durability),
                tableCenter, panelTop + backgroundHeight + 3, 0xFFFFFFFF);
    }

    private int getScorePanelLeft() {
        return (width - backgroundWidth) / 2 + TABLE_PANEL_WIDTH + SCORE_PANEL_GAP;
    }

    private void drawScoreboard(DrawContext context) {
        int left = getScorePanelLeft();
        int top = (height - backgroundHeight) / 2;
        int right = left + SCORE_PANEL_WIDTH;
        context.fill(left, top, right, top + backgroundHeight, COLOR_WOOD);
        context.fill(left + 2, top + 2, right - 2, top + backgroundHeight - 2, 0xF0212924);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.billiardsmod.scoreboard"),
                left + SCORE_PANEL_WIDTH / 2, top + 9, 0xFFFFD36A);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.billiardsmod.point_rule"),
                left + SCORE_PANEL_WIDTH / 2, top + 22, 0xFFB5BDB8);

        List<PlayerScore> scores = handler.getState().scores();
        scoreScroll = Math.max(0, Math.min(scoreScroll, scores.size() - VISIBLE_SCORE_ROWS));
        int end = Math.min(scores.size(), scoreScroll + VISIBLE_SCORE_ROWS);
        for (int i = scoreScroll; i < end; i++) {
            PlayerScore score = scores.get(i);
            int rowTop = top + 39 + (i - scoreScroll) * SCORE_ROW_HEIGHT;
            boolean active = score.playerId().equals(handler.getState().currentPlayer());
            if (active) context.fill(left + 5, rowTop - 3, right - 5, rowTop + 12, 0xFF315C3F);
            String points = Integer.toString(score.points());
            int pointsWidth = textRenderer.getWidth(points);
            String name = textRenderer.trimToWidth(score.playerName(), SCORE_PANEL_WIDTH - pointsWidth - 29);
            context.drawText(textRenderer, name, left + 9, rowTop, active ? 0xFFAAFFAA : 0xFFFFFFFF, true);
            context.drawText(textRenderer, points, right - 9 - pointsWidth, rowTop, 0xFFFFD36A, true);
        }
        if (scores.size() > VISIBLE_SCORE_ROWS) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.billiardsmod.scroll_scores"),
                    left + SCORE_PANEL_WIDTH / 2, top + 166, 0xFFB5BDB8);
        }
        if (handler.getState().foulPlayer() != null) {
            String name = scores.stream().filter(score -> score.playerId().equals(handler.getState().foulPlayer()))
                    .map(PlayerScore::playerName).findFirst().orElse("");
            String foulKey = handler.getState().foulReason() == FoulReason.NO_BALL_HIT
                    ? "screen.billiardsmod.no_ball_hit_foul" : "screen.billiardsmod.cue_ball_foul";
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable(foulKey),
                    left + SCORE_PANEL_WIDTH / 2, top + 181, 0xFFFF9977);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(textRenderer.trimToWidth(name, SCORE_PANEL_WIDTH - 16)),
                    left + SCORE_PANEL_WIDTH / 2, top + 194, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int left = getScorePanelLeft();
        int top = (height - backgroundHeight) / 2;
        if (mouseX >= left && mouseX < left + SCORE_PANEL_WIDTH
                && mouseY >= top && mouseY < top + backgroundHeight) {
            int direction = verticalAmount > 0 ? -1 : verticalAmount < 0 ? 1 : 0;
            scoreScroll = Math.max(0, Math.min(scoreScroll + direction,
                    handler.getState().scores().size() - VISIBLE_SCORE_ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
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
