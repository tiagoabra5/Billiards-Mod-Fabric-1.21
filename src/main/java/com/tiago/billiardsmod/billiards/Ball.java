package com.tiago.billiardsmod.billiards;

public class Ball {

    public final int number;

    public float x;
    public float y;

    public float vx;
    public float vy;

    public final float radius;

    public boolean pocketed;

    public Ball(int number, float x, float y, float radius) {
        this.number = number;
        this.x = x;
        this.y = y;
        this.vx = 0f;
        this.vy = 0f;
        this.radius = radius;
        this.pocketed = false;
    }

    public boolean isCueBall() {
        return number == 0;
    }

    public boolean isStripe() {
        return number >= 9 && number <= 15;
    }

    public boolean isEightBall() {
        return number == 8;
    }

    public int getColor() {
        return BALL_COLORS[number];
    }

    private static final int COLOR_WHITE  = 0xFFFFFFFF;
    private static final int COLOR_YELLOW = 0xFFE0C300;
    private static final int COLOR_BLUE   = 0xFF1B4FA0;
    private static final int COLOR_RED    = 0xFFB3221A;
    private static final int COLOR_PURPLE = 0xFF6A2C91;
    private static final int COLOR_ORANGE = 0xFFE07A1A;
    private static final int COLOR_GREEN  = 0xFF2CEC21;
    private static final int COLOR_MAROON = 0xFF6B1A1A;
    private static final int COLOR_BLACK  = 0xFF141414;

    private static final int[] BALL_COLORS = {
            COLOR_WHITE,
            COLOR_YELLOW,
            COLOR_BLUE,
            COLOR_RED,
            COLOR_PURPLE,
            COLOR_ORANGE,
            COLOR_GREEN,
            COLOR_MAROON,
            COLOR_BLACK,
            COLOR_YELLOW,
            COLOR_BLUE,
            COLOR_RED,
            COLOR_PURPLE,
            COLOR_ORANGE,
            COLOR_MAROON,
            COLOR_GREEN,
    };
}