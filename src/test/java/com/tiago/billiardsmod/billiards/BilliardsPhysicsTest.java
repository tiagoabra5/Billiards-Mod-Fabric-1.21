package com.tiago.billiardsmod.billiards;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BilliardsPhysicsTest {
    private static final TableBounds BOUNDS = new TableBounds(0, 0, 130, 160);

    @Test
    void aFullPowerBallCoversTheExpectedDistanceInOneServerTick() {
        Ball ball = new Ball(0, 65f, 100f, 4f);
        ball.vx = 6f;
        BilliardsPhysics.update(List.of(ball), BOUNDS, List.of(), 9f);
        assertTrue(ball.x >= 81f && ball.x <= 83f, "A full shot should move about 18 pixels in 50 ms");
    }

    @Test
    void aFullPowerBallStopsWithinSixSecondsInsteadOfDraggingForTwenty() {
        Ball ball = new Ball(0, 65f, 100f, 4f);
        ball.vx = 6f;
        List<Ball> balls = List.of(ball);
        int ticks = 0;
        while (BilliardsPhysics.areBallsMoving(balls) && ticks < 120) {
            BilliardsPhysics.update(balls, BOUNDS, List.of(), 9f);
            ticks++;
        }
        assertFalse(BilliardsPhysics.areBallsMoving(balls));
        assertTrue(ticks >= 20, "The ball should still roll naturally for more than a second");
    }

    @Test
    void fasterSimulationStillTransfersAHeadOnCollision() {
        Ball cue = new Ball(0, 40f, 60f, 4f);
        Ball target = new Ball(1, 56f, 60f, 4f);
        cue.vx = 6f;
        assertTrue(BilliardsPhysics.update(List.of(cue, target), BOUNDS, List.of(), 9f));
        assertTrue(target.vx > 0f);
        assertEquals(0f, cue.vx, 0.001f);
        assertTrue(target.x > cue.x + target.radius + cue.radius);
    }

    @Test
    void aFastBallIsCapturedByThePocketBeforeReboundingOffTheWall() {
        Ball ball = new Ball(1, 15f, 80f, 4f);
        ball.vx = -6f;
        BilliardsPhysics.update(List.of(ball), BOUNDS, List.of(new float[]{0f, 80f}), 9f);
        assertTrue(ball.pocketed);
        assertEquals(0f, ball.vx);
    }

    @Test
    void collidingObjectBallsDoNotCountAsContactByTheCueBall() {
        Ball cue = new Ball(0, 65f, 140f, 4f);
        Ball first = new Ball(1, 40f, 60f, 4f);
        Ball second = new Ball(2, 56f, 60f, 4f);
        first.vx = 6f;
        assertFalse(BilliardsPhysics.update(List.of(cue, first, second), BOUNDS, List.of(), 9f));
        assertTrue(second.vx > 0f);
    }

    @Test
    void bouncingOffACushionDoesNotCountAsHittingABall() {
        Ball cue = new Ball(0, 124f, 120f, 4f);
        cue.vx = 6f;
        assertFalse(BilliardsPhysics.update(List.of(cue), BOUNDS, List.of(), 9f));
        assertTrue(cue.vx < 0f);
    }
}
