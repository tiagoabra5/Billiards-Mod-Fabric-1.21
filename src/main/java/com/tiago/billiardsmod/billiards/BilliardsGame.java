package com.tiago.billiardsmod.billiards;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The shared game of one table. Only the server advances this simulation. */
public class BilliardsGame {
    public static final int TABLE_WIDTH = 130;
    public static final int TABLE_HEIGHT = 160;
    public static final float BALL_RADIUS = 4f;
    public static final float POCKET_CAPTURE_RADIUS = 9f;
    private static final float MAX_SHOT_SPEED = 6f;
    private static final float HEAD_SPOT_Y = TABLE_HEIGHT - 20f;
    private static final TableBounds BOUNDS = new TableBounds(0f, 0f, TABLE_WIDTH, TABLE_HEIGHT);
    private static final List<float[]> POCKETS = List.of(
            new float[]{0f, 0f}, new float[]{0f, TABLE_HEIGHT / 2f}, new float[]{0f, TABLE_HEIGHT},
            new float[]{TABLE_WIDTH, 0f}, new float[]{TABLE_WIDTH, TABLE_HEIGHT / 2f},
            new float[]{TABLE_WIDTH, TABLE_HEIGHT}
    );

    private List<Ball> balls = createInitialRack();
    private final List<UUID> players = new ArrayList<>();
    private final Map<UUID, PlayerScore> scores = new LinkedHashMap<>();
    private UUID currentPlayer;
    private UUID shotPlayer;
    private UUID lastFoulPlayer;
    private FoulReason lastFoulReason = FoulReason.NONE;
    private int shotPocketedMask;
    private int shotPoints;
    private boolean cueBallFoul;
    private boolean hitObjectBall;
    private boolean shotInProgress;
    private long turnId;
    private long revision;

    /** Participants are transient; scores and ownership of an unfinished shot survive a reload. */
    public record Progress(List<PlayerScore> scores, UUID shotPlayer, int shotPoints,
                           boolean cueBallFoul, boolean hitObjectBall, UUID lastFoulPlayer,
                           FoulReason lastFoulReason) {
        public Progress {
            scores = List.copyOf(scores);
        }
    }

    private static List<Ball> createInitialRack() {
        float spacing = BALL_RADIUS * 2f + 0.5f;
        float triangleHeight = 4 * spacing * 0.87f;
        return BallRack.createStandardRack(TABLE_WIDTH / 2f, BALL_RADIUS + 4f + triangleHeight,
                HEAD_SPOT_Y, BALL_RADIUS);
    }

    public List<Ball> getBalls() {
        return balls;
    }

    public void loadBalls(List<Ball> balls) {
        this.balls = new ArrayList<>(balls);
        shotInProgress = false;
        shotPlayer = null;
        shotPoints = 0;
        cueBallFoul = false;
        hitObjectBall = false;
        revision++;
    }

    public List<PlayerScore> getScores() {
        return List.copyOf(scores.values());
    }

    public int getScore(UUID player) {
        PlayerScore score = scores.get(player);
        return score == null ? 0 : score.points();
    }

    public UUID getLastFoulPlayer() {
        return lastFoulPlayer;
    }

    public FoulReason getLastFoulReason() {
        return lastFoulReason;
    }

    public Progress saveProgress() {
        return new Progress(getScores(), shotPlayer, shotPoints, cueBallFoul, hitObjectBall,
                lastFoulPlayer, lastFoulReason);
    }

    public void loadProgress(Progress progress) {
        scores.clear();
        for (PlayerScore score : progress.scores()) {
            scores.put(score.playerId(), new PlayerScore(score.playerId(), score.playerName(),
                    Math.max(0, score.points())));
        }
        shotPlayer = progress.shotPlayer();
        shotPoints = progress.shotPoints();
        cueBallFoul = progress.cueBallFoul();
        hitObjectBall = progress.hitObjectBall();
        lastFoulPlayer = progress.lastFoulPlayer();
        lastFoulReason = progress.lastFoulReason();
        shotInProgress = shotPlayer != null;
        shotPocketedMask = pocketedMask();
        revision++;
    }

    public UUID getCurrentPlayer() {
        return currentPlayer;
    }

    public List<UUID> getPlayers() {
        return List.copyOf(players);
    }

    public int getPlayerCount() {
        return players.size();
    }

    public long getTurnId() {
        return turnId;
    }

    public long getRevision() {
        return revision;
    }

    public boolean isBusy() {
        return shotInProgress || BilliardsPhysics.areBallsMoving(balls);
    }

    public void joinPlayer(UUID player, String name) {
        if (players.contains(player)) return;
        players.add(player);
        scores.put(player, new PlayerScore(player, name, getScore(player)));
        if (currentPlayer == null) {
            currentPlayer = player;
            turnId++;
        }
        revision++;
    }

    public void leavePlayer(UUID player) {
        int index = players.indexOf(player);
        if (index < 0) return;
        players.remove(index);
        if (players.isEmpty()) {
            resetGame();
            return;
        }
        if (player.equals(currentPlayer)) {
            currentPlayer = players.get(index % players.size());
            turnId++;
        }
        revision++;
    }

    private void resetGame() {
        balls = createInitialRack();
        scores.clear();
        currentPlayer = null;
        shotPlayer = null;
        lastFoulPlayer = null;
        lastFoulReason = FoulReason.NONE;
        shotPocketedMask = 0;
        shotPoints = 0;
        cueBallFoul = false;
        hitObjectBall = false;
        shotInProgress = false;
        // Keep identities increasing so a request from the previous game stays invalid.
        turnId++;
        revision++;
    }

    public boolean shoot(UUID player, long expectedTurnId, float angle, float power) {
        if (!player.equals(currentPlayer) || expectedTurnId != turnId || isBusy()
                || !Float.isFinite(angle) || !Float.isFinite(power) || power <= 0f || power > 1f) {
            return false;
        }

        Ball cueBall = getCueBall();
        if (cueBall == null || cueBall.pocketed) return false;

        float speed = power * MAX_SHOT_SPEED;
        cueBall.vx = (float) Math.cos(angle) * speed;
        cueBall.vy = (float) Math.sin(angle) * speed;
        shotInProgress = true;
        shotPlayer = player;
        shotPocketedMask = pocketedMask();
        shotPoints = 0;
        cueBallFoul = false;
        hitObjectBall = false;
        lastFoulPlayer = null;
        lastFoulReason = FoulReason.NONE;
        revision++;
        return true;
    }

    /** Returns whether a new state needs to be saved and sent to the viewers. */
    public boolean tick() {
        if (!isBusy()) {
            if (!restoreCueBall()) return false;
            revision++;
            return true;
        }

        hitObjectBall |= BilliardsPhysics.update(balls, BOUNDS, POCKETS, POCKET_CAPTURE_RADIUS);
        recordPocketedBalls();
        if (!BilliardsPhysics.areBallsMoving(balls)) {
            restoreCueBall();
            if (shotInProgress) {
                if (!hitObjectBall && !cueBallFoul) {
                    lastFoulPlayer = shotPlayer;
                    lastFoulReason = FoulReason.NO_BALL_HIT;
                }
                // Leaving/reloading can give the turn to someone else while the shot is moving.
                // Only advance if it still belongs to the shooter who missed or scratched.
                if (shotPlayer != null && shotPlayer.equals(currentPlayer)
                        && (shotPoints == 0 || cueBallFoul || !hitObjectBall) && !players.isEmpty()) {
                    int index = players.indexOf(currentPlayer);
                    currentPlayer = players.get((index + 1) % players.size());
                }
                turnId++;
            }
            shotInProgress = false;
            shotPlayer = null;
            shotPoints = 0;
            cueBallFoul = false;
            hitObjectBall = false;
        }
        revision++;
        return true;
    }

    private int pocketedMask() {
        int mask = 0;
        for (Ball ball : balls) {
            if (ball.pocketed) mask |= 1 << ball.number;
        }
        return mask;
    }

    private void recordPocketedBalls() {
        if (!shotInProgress || shotPlayer == null) return;
        int pocketed = pocketedMask();
        int newPockets = pocketed & ~shotPocketedMask;
        shotPocketedMask = pocketed;
        if ((newPockets & 1) != 0) {
            cueBallFoul = true;
            lastFoulPlayer = shotPlayer;
            lastFoulReason = FoulReason.CUE_BALL;
        }
        int points = Integer.bitCount(newPockets & ~1);
        if (points > 0) {
            PlayerScore score = scores.get(shotPlayer);
            if (score != null) {
                scores.put(shotPlayer, new PlayerScore(shotPlayer, score.playerName(), score.points() + points));
            }
            shotPoints += points;
        }
    }

    private Ball getCueBall() {
        for (Ball ball : balls) {
            if (ball.isCueBall()) return ball;
        }
        return null;
    }

    private boolean restoreCueBall() {
        Ball cueBall = getCueBall();
        if (cueBall == null || !cueBall.pocketed) return false;
        if (tryPlaceCueBall(cueBall, TABLE_WIDTH / 2f, HEAD_SPOT_Y)) return true;

        float spacing = BALL_RADIUS * 2f + 1f;
        for (float y = HEAD_SPOT_Y; y >= POCKET_CAPTURE_RADIUS; y -= spacing) {
            for (float x = POCKET_CAPTURE_RADIUS; x <= TABLE_WIDTH - POCKET_CAPTURE_RADIUS; x += spacing) {
                if (tryPlaceCueBall(cueBall, x, y)) return true;
            }
        }
        return false;
    }

    private boolean tryPlaceCueBall(Ball cueBall, float x, float y) {
        for (Ball other : balls) {
            if (other == cueBall || other.pocketed) continue;
            float dx = other.x - x;
            float dy = other.y - y;
            float distance = cueBall.radius + other.radius + 0.5f;
            if (dx * dx + dy * dy < distance * distance) return false;
        }
        cueBall.x = x;
        cueBall.y = y;
        cueBall.vx = 0f;
        cueBall.vy = 0f;
        cueBall.pocketed = false;
        return true;
    }
}
