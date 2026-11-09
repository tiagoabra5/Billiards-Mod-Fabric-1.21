package com.tiago.billiardsmod.billiards;

import java.util.List;

public class BilliardsPhysics {

    // Minecraft ticks at 20 Hz; three small steps restore the original 60 Hz pace
    // without increasing the distance traveled in an individual collision step.
    private static final int STEPS_PER_TICK = 3;
    private static final float FRICTION = 0.985f;
    private static final float MIN_SPEED = 0.05f;

    /** Advances a server tick and reports whether the cue ball hit an object ball. */
    public static boolean update(List<Ball> balls, TableBounds bounds, List<float[]> pockets, float captureRadius) {
        boolean cueBallContact = false;
        for (int step = 0; step < STEPS_PER_TICK; step++) {
            moveBalls(balls);
            cueBallContact |= resolveBallCollisions(balls);
            checkPockets(balls, pockets, captureRadius);
            resolveWallCollisions(balls, bounds);
        }
        return cueBallContact;
    }

    private static void moveBalls(List<Ball> balls) {
        for (Ball ball : balls) {
            if (ball.pocketed) continue;

            ball.x += ball.vx;
            ball.y += ball.vy;

            ball.vx *= FRICTION;
            ball.vy *= FRICTION;

            if (Math.abs(ball.vx) < MIN_SPEED) ball.vx = 0f;
            if (Math.abs(ball.vy) < MIN_SPEED) ball.vy = 0f;
        }
    }

    private static boolean resolveBallCollisions(List<Ball> balls) {
        boolean cueBallContact = false;
        for (int i = 0; i < balls.size(); i++) {
            Ball a = balls.get(i);
            if (a.pocketed) continue;

            for (int j = i + 1; j < balls.size(); j++) {
                Ball b = balls.get(j);
                if (b.pocketed) continue;

                float dx = b.x - a.x;
                float dy = b.y - a.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                float minDistance = a.radius + b.radius;

                if (distance < minDistance && distance > 0f) {
                    float nx = dx / distance;
                    float ny = dy / distance;

                    float overlap = minDistance - distance;
                    float pushX = nx * (overlap / 2f);
                    float pushY = ny * (overlap / 2f);

                    a.x -= pushX;
                    a.y -= pushY;
                    b.x += pushX;
                    b.y += pushY;

                    float relativeVx = b.vx - a.vx;
                    float relativeVy = b.vy - a.vy;

                    float speedAlongNormal = relativeVx * nx + relativeVy * ny;

                    if (speedAlongNormal > 0f) continue;

                    if (a.isCueBall() != b.isCueBall()) cueBallContact = true;

                    a.vx += speedAlongNormal * nx;
                    a.vy += speedAlongNormal * ny;
                    b.vx -= speedAlongNormal * nx;
                    b.vy -= speedAlongNormal * ny;
                }
            }
        }
        return cueBallContact;
    }

    private static void resolveWallCollisions(List<Ball> balls, TableBounds bounds) {
        for (Ball ball : balls) {
            if (ball.pocketed) continue;

            if (ball.x - ball.radius < bounds.left) {
                ball.x = bounds.left + ball.radius;
                ball.vx = -ball.vx;
            } else if (ball.x + ball.radius > bounds.right) {
                ball.x = bounds.right - ball.radius;
                ball.vx = -ball.vx;
            }

            if (ball.y - ball.radius < bounds.top) {
                ball.y = bounds.top + ball.radius;
                ball.vy = -ball.vy;
            } else if (ball.y + ball.radius > bounds.bottom) {
                ball.y = bounds.bottom - ball.radius;
                ball.vy = -ball.vy;
            }
        }
    }

    private static void checkPockets(List<Ball> balls, List<float[]> pockets, float captureRadius) {
        for (Ball ball : balls) {
            if (ball.pocketed) continue;

            for (float[] pocket : pockets) {
                float dx = pocket[0] - ball.x;
                float dy = pocket[1] - ball.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance < captureRadius) {
                    ball.pocketed = true;
                    ball.vx = 0f;
                    ball.vy = 0f;
                    break;
                }
            }
        }
    }

    public static boolean areBallsMoving(List<Ball> balls) {
        for (Ball ball : balls) {
            if (ball.pocketed) continue;
            if (ball.vx != 0f || ball.vy != 0f) return true;
        }
        return false;
    }
}
