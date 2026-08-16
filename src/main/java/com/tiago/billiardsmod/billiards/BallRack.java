package com.tiago.billiardsmod.billiards;

import java.util.ArrayList;
import java.util.List;

public class BallRack {

    public static List<Ball> createStandardRack(float footSpotX, float footSpotY,
                                                float headSpotY, float ballRadius) {
        List<Ball> balls = new ArrayList<>();

        int[] rackOrder = {1, 9, 2, 10, 8, 3, 11, 4, 12, 5, 13, 6, 14, 7, 15};

        float spacing = ballRadius * 2f + 0.5f;
        int index = 0;

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col <= row; col++) {
                int number = rackOrder[index++];
                float x = footSpotX + (col - row / 2f) * spacing;
                float y = footSpotY - row * spacing * 0.87f; // 0.87 ≈ sqrt(3)/2, compacta o triângulo
                balls.add(new Ball(number, x, y, ballRadius));
            }
        }

        balls.add(new Ball(0, footSpotX, headSpotY, ballRadius));

        return balls;
    }
}