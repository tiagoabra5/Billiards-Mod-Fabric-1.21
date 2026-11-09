package com.tiago.billiardsmod.screen.custom;

import com.tiago.billiardsmod.billiards.Ball;
import net.minecraft.client.gui.DrawContext;

import java.util.List;
import java.util.function.Function;

public class BallRenderer {

    private static final int COLOR_STRIPE = 0xFFFFFFFF;
    private static final float MAX_INTERPOLATION_DISTANCE = 32f;

    public static void drawBalls(DrawContext context, List<Ball> balls,
                                 List<Ball> previousBalls, float progress,
                                 Function<Float, Float> toScreenX, Function<Float, Float> toScreenY) {
        for (int i = 0; i < balls.size(); i++) {
            Ball ball = balls.get(i);
            if (ball.pocketed) continue;
            float x = ball.x;
            float y = ball.y;
            if (i < previousBalls.size()) {
                Ball previous = previousBalls.get(i);
                float dx = ball.x - previous.x;
                float dy = ball.y - previous.y;
                // A respawn is a teleport, not a movement across the felt.
                if (previous.number == ball.number && !previous.pocketed
                        && dx * dx + dy * dy <= MAX_INTERPOLATION_DISTANCE * MAX_INTERPOLATION_DISTANCE) {
                    x = previous.x + dx * progress;
                    y = previous.y + dy * progress;
                }
            }
            drawBall(context, ball, toScreenX.apply(x), toScreenY.apply(y));
        }
    }

    private static void drawBall(DrawContext context, Ball ball, float screenX, float screenY) {
        int left = Math.round(screenX - ball.radius);
        int top = Math.round(screenY - ball.radius);
        int right = Math.round(screenX + ball.radius);
        int bottom = Math.round(screenY + ball.radius);

        context.fill(left, top, right, bottom, ball.getColor());

        if (ball.isStripe()) {
            int stripeHeight = Math.max(1, Math.round(ball.radius * 0.8f));
            int stripeTop = Math.round(screenY - stripeHeight / 2f);
            int stripeBottom = stripeTop + stripeHeight;
            context.fill(left, stripeTop, right, stripeBottom, COLOR_STRIPE);
        }
    }
}
