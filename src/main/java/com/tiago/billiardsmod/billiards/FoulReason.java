package com.tiago.billiardsmod.billiards;

public enum FoulReason {
    NONE,
    CUE_BALL,
    NO_BALL_HIT;

    public static FoulReason fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException exception) {
            return NONE;
        }
    }
}
