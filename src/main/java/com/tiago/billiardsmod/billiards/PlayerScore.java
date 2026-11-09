package com.tiago.billiardsmod.billiards;

import java.util.UUID;

public record PlayerScore(UUID playerId, String playerName, int points) {
}
