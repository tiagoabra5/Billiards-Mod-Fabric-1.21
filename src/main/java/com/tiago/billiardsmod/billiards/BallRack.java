package com.tiago.billiardsmod.billiards;

import java.util.ArrayList;
import java.util.List;

public class BallRack {

    /**
     * Monta as 16 bolas no formato padrão de 8-ball:
     * triângulo de 15 bolas (1 no ápice, encostado no "foot spot"),
     * mais a bola branca posicionada perto do "head spot" (lado oposto).
     *
     * @param footSpotX, footSpotY  ponto do ápice do triângulo (centro da mesa, lado de baixo, por ex.)
     * @param headSpotY             posição Y onde a branca começa (lado de cima da mesa)
     * @param ballRadius            raio de cada bola, em pixels
     */
    public static List<Ball> createStandardRack(float footSpotX, float footSpotY,
                                                float headSpotY, float ballRadius) {
        List<Ball> balls = new ArrayList<>();

        // Ordem clássica de rack: 1 no ápice, 8 no meio da 3ª fileira,
        // uma lisa e uma listrada em cada canto de trás. Simplificado aqui
        // em ordem sequencial só pra já ter algo funcional; pode ajustar
        // a ordem exata das posições depois sem mudar a estrutura.
        int[] rackOrder = {1, 9, 2, 10, 8, 3, 11, 4, 12, 5, 13, 6, 14, 7, 15};

        float spacing = ballRadius * 2f + 0.5f; // pequeno gap pra não ficarem coladas
        int index = 0;

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col <= row; col++) {
                int number = rackOrder[index++];
                float x = footSpotX + (col - row / 2f) * spacing;
                float y = footSpotY - row * spacing * 0.87f; // 0.87 ≈ sqrt(3)/2, compacta o triângulo
                balls.add(new Ball(number, x, y, ballRadius));
            }
        }

        // Bola branca, centralizada no eixo X da mesa, na altura do head spot
        balls.add(new Ball(0, footSpotX, headSpotY, ballRadius));

        return balls;
    }
}