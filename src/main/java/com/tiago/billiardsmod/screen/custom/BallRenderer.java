package com.tiago.billiardsmod.screen.custom;

import com.tiago.billiardsmod.billiards.Ball;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public class BallRenderer {

    private static final int COLOR_STRIPE = 0xFFFFFFFF;

    public static void drawBalls(DrawContext context, List<Ball> balls) {
        for (Ball ball : balls) {
            if (ball.pocketed) continue;
            drawBall(context, ball);
        }
    }

    private static void drawBall(DrawContext context, Ball ball) {
        int left = Math.round(ball.x - ball.radius);
        int top = Math.round(ball.y - ball.radius);
        int right = Math.round(ball.x + ball.radius);
        int bottom = Math.round(ball.y + ball.radius);

        // Corpo do quadrado, cor sólida da bola
        context.fill(left, top, right, bottom, ball.getColor());

        // Bolas listradas (9-15) ganham uma faixa branca horizontal no meio
        if (ball.isStripe()) {
            int stripeHeight = Math.max(1, Math.round(ball.radius * 0.8f));
            int stripeTop = Math.round(ball.y - stripeHeight / 2f);
            int stripeBottom = stripeTop + stripeHeight;
            context.fill(left, stripeTop, right, stripeBottom, COLOR_STRIPE);
        }
    }
}