package com.tiago.billiardsmod.billiards;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BilliardsGameTest {
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    private BilliardsGame twoPlayerGame() {
        BilliardsGame game = new BilliardsGame();
        game.joinPlayer(first, "PlayerOne");
        game.joinPlayer(second, "PlayerTwo");
        return game;
    }

    private static Ball cueBall(BilliardsGame game) {
        return game.getBalls().stream().filter(Ball::isCueBall).findFirst().orElseThrow();
    }

    private static void settle(BilliardsGame game) {
        for (int tick = 0; tick < 2000 && game.isBusy(); tick++) game.tick();
        assertFalse(game.isBusy(), "A shot must eventually finish");
    }

    private BilliardsGame pocketingGame() {
        BilliardsGame game = twoPlayerGame();
        game.loadBalls(List.of(new Ball(1, 12f, 80f, 4f), new Ball(0, 40f, 80f, 4f)));
        return game;
    }

    private static void assertFreshGame(BilliardsGame game) {
        assertEquals(0, game.getPlayerCount());
        assertNull(game.getCurrentPlayer());
        assertTrue(game.getScores().isEmpty());
        assertNull(game.getLastFoulPlayer());
        assertEquals(FoulReason.NONE, game.getLastFoulReason());
        assertNull(game.saveProgress().shotPlayer());
        assertFalse(game.isBusy());
        List<Ball> freshRack = new BilliardsGame().getBalls();
        assertEquals(freshRack.size(), game.getBalls().size());
        for (int i = 0; i < freshRack.size(); i++) {
            Ball expected = freshRack.get(i);
            Ball actual = game.getBalls().get(i);
            assertEquals(expected.number, actual.number);
            assertEquals(expected.x, actual.x);
            assertEquals(expected.y, actual.y);
            assertEquals(0f, actual.vx);
            assertEquals(0f, actual.vy);
            assertFalse(actual.pocketed);
        }
    }

    @Test
    void playersAlternateOnlyAfterTheSharedBallsStop() {
        BilliardsGame game = twoPlayerGame();
        long firstTurn = game.getTurnId();
        float initialX = cueBall(game).x;

        assertTrue(game.shoot(first, firstTurn, 0f, 0.2f));
        game.tick();
        assertNotEquals(initialX, cueBall(game).x);
        assertEquals(first, game.getCurrentPlayer());
        assertFalse(game.shoot(second, firstTurn, 1f, 1f));
        settle(game);

        assertEquals(second, game.getCurrentPlayer());
        assertNotEquals(firstTurn, game.getTurnId());
        assertFalse(game.shoot(first, game.getTurnId(), 0f, 0.2f));
        assertTrue(game.shoot(second, game.getTurnId(), 1f, 0.2f));
        settle(game);
        assertEquals(first, game.getCurrentPlayer());
    }

    @Test
    void concurrentAndRepeatedShotsCannotChangeTheAcceptedShot() {
        BilliardsGame game = twoPlayerGame();
        long turn = game.getTurnId();
        assertTrue(game.shoot(first, turn, 0f, 0.5f));
        float vx = cueBall(game).vx;
        assertFalse(game.shoot(second, turn, 2f, 1f));
        assertFalse(game.shoot(first, turn, 2f, 1f));
        assertEquals(vx, cueBall(game).vx);
        assertEquals(0f, cueBall(game).vy);
    }

    @Test
    void joiningDuringAShotKeepsTheGameAndReceivesTheNextTurn() {
        BilliardsGame game = new BilliardsGame();
        game.joinPlayer(first, "PlayerOne");
        assertTrue(game.shoot(first, game.getTurnId(), 0f, 0.4f));
        game.tick();
        float sharedX = cueBall(game).x;
        game.joinPlayer(second, "PlayerTwo");
        game.joinPlayer(second, "PlayerTwo");

        assertEquals(sharedX, cueBall(game).x);
        assertEquals(2, game.getPlayerCount());
        assertEquals(first, game.getCurrentPlayer());
        settle(game);
        assertEquals(second, game.getCurrentPlayer());
    }

    @Test
    void moreThanTwoPlayersPlayInOpeningOrder() {
        BilliardsGame game = twoPlayerGame();
        UUID third = UUID.randomUUID();
        game.joinPlayer(third, "PlayerThree");
        for (UUID player : new UUID[]{first, second, third, first}) {
            assertEquals(player, game.getCurrentPlayer());
            assertTrue(game.shoot(player, game.getTurnId(), 0f, 0.01f));
            settle(game);
        }
    }

    @Test
    void closingTheTableDuringAShotDoesNotSkipTheNextPlayer() {
        BilliardsGame game = twoPlayerGame();
        UUID third = UUID.randomUUID();
        game.joinPlayer(third, "PlayerThree");
        assertTrue(game.shoot(first, game.getTurnId(), 0f, 0.2f));
        game.leavePlayer(first);

        assertEquals(second, game.getCurrentPlayer());
        assertFalse(game.shoot(second, game.getTurnId(), 0f, 0.2f));
        settle(game);
        assertEquals(second, game.getCurrentPlayer());
        assertTrue(game.shoot(second, game.getTurnId(), 0f, 0.01f));
        settle(game);
        assertEquals(third, game.getCurrentPlayer());
    }

    @Test
    void aWaitingPlayerLeavingDoesNotChangeTheCurrentTurn() {
        BilliardsGame game = twoPlayerGame();
        long turn = game.getTurnId();
        assertTrue(game.shoot(first, turn, 0f, 0.2f));
        game.leavePlayer(second);
        assertEquals(turn, game.getTurnId());
        settle(game);
        assertEquals(first, game.getCurrentPlayer());
        assertEquals(1, game.getPlayerCount());
    }

    @Test
    void theRackResetsWhenBothPlayersCloseTheTable() {
        BilliardsGame game = twoPlayerGame();
        game.shoot(first, game.getTurnId(), 0f, 0.2f);
        settle(game);
        float sharedX = cueBall(game).x;
        game.leavePlayer(first);
        assertEquals(sharedX, cueBall(game).x);
        game.leavePlayer(second);
        assertFreshGame(game);
        assertFalse(game.shoot(first, game.getTurnId(), 0f, 1f));
        game.joinPlayer(second, "PlayerTwo");
        assertNotEquals(sharedX, cueBall(game).x);
        assertEquals(second, game.getCurrentPlayer());
    }

    @Test
    void returningAfterTheShooterLeavesDoesNotStealTheSuccessorsTurn() {
        BilliardsGame game = twoPlayerGame();
        game.shoot(first, game.getTurnId(), 0f, 0.2f);
        game.leavePlayer(first);
        game.joinPlayer(first, "PlayerOne");
        settle(game);
        assertEquals(second, game.getCurrentPlayer());
    }

    @Test
    void singlePlayerCanPracticeButAnOldTurnCannotBeReplayed() {
        BilliardsGame game = new BilliardsGame();
        game.joinPlayer(first, "PlayerOne");
        long previousTurn = game.getTurnId();
        assertTrue(game.shoot(first, previousTurn, 0f, 0.01f));
        settle(game);
        assertEquals(first, game.getCurrentPlayer());
        assertFalse(game.shoot(first, previousTurn, 0f, 0.1f));
        assertTrue(game.shoot(first, game.getTurnId(), 0f, 0.1f));
    }

    @Test
    void nonParticipantsAndInvalidShotValuesAreRejected() {
        BilliardsGame game = twoPlayerGame();
        long turn = game.getTurnId();
        assertFalse(game.shoot(UUID.randomUUID(), turn, 0f, 1f));
        for (float power : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1f, 0f, 1.01f}) {
            assertFalse(game.shoot(first, turn, 0f, power));
        }
        for (float angle : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertFalse(game.shoot(first, turn, angle, 1f));
        }
        assertFalse(game.isBusy());
        assertEquals(0f, cueBall(game).vx);
        assertEquals(first, game.getCurrentPlayer());
    }

    @Test
    void pocketingTheCueBallRestoresItAndPassesTheTurn() {
        BilliardsGame game = twoPlayerGame();
        Ball cue = cueBall(game);
        cue.x = 10f;
        cue.y = BilliardsGame.TABLE_HEIGHT / 2f;
        assertTrue(game.shoot(first, game.getTurnId(), (float) Math.PI, 1f));
        settle(game);

        assertFalse(cue.pocketed);
        assertEquals(BilliardsGame.TABLE_WIDTH / 2f, cue.x);
        assertEquals(BilliardsGame.TABLE_HEIGHT - 20f, cue.y);
        assertEquals(second, game.getCurrentPlayer());
        assertTrue(game.shoot(second, game.getTurnId(), 0f, 0.1f));
    }

    @Test
    void cueBallRespawnsAwayFromAnOccupiedHeadSpot() {
        BilliardsGame game = twoPlayerGame();
        Ball cue = cueBall(game);
        cue.pocketed = true;
        Ball blocker = game.getBalls().getFirst();
        blocker.x = BilliardsGame.TABLE_WIDTH / 2f;
        blocker.y = BilliardsGame.TABLE_HEIGHT - 20f;
        assertTrue(game.tick());
        assertFalse(cue.pocketed);
        float dx = blocker.x - cue.x;
        float dy = blocker.y - cue.y;
        assertTrue(dx * dx + dy * dy >= Math.pow(blocker.radius + cue.radius, 2));
        assertEquals(first, game.getCurrentPlayer());
    }

    @Test
    void independentTablesDoNotSharePlayersOrBalls() {
        BilliardsGame firstTable = twoPlayerGame();
        BilliardsGame secondTable = new BilliardsGame();
        secondTable.joinPlayer(second, "PlayerTwo");
        firstTable.shoot(first, firstTable.getTurnId(), 0f, 0.5f);
        firstTable.tick();
        assertFalse(secondTable.isBusy());
        assertNotEquals(cueBall(firstTable).x, cueBall(secondTable).x);
        assertEquals(second, secondTable.getCurrentPlayer());
        assertEquals(1, secondTable.getPlayerCount());
    }

    @Test
    void anIdleTableDoesNotProduceContinuousUpdates() {
        BilliardsGame game = twoPlayerGame();
        long revision = game.getRevision();
        assertFalse(game.tick());
        assertEquals(revision, game.getRevision());
    }

    @Test
    void pocketingAnObjectBallScoresOnePointAndKeepsTheTurn() {
        BilliardsGame game = pocketingGame();
        assertTrue(game.shoot(first, game.getTurnId(), (float) Math.PI, 1f));
        settle(game);
        assertTrue(game.getBalls().getFirst().pocketed);
        assertEquals(1, game.getScore(first));
        assertEquals(0, game.getScore(second));
        assertEquals(first, game.getCurrentPlayer());
        assertNull(game.getLastFoulPlayer());
    }

    @Test
    void eachObjectBallIsWorthOnePointRegardlessOfItsNumber() {
        BilliardsGame game = twoPlayerGame();
        game.loadBalls(List.of(new Ball(1, 12f, 80f, 4f), new Ball(15, 124f, 80f, 4f),
                new Ball(0, 40f, 80f, 4f)));
        assertTrue(game.shoot(first, game.getTurnId(), (float) Math.PI, 1f));
        settle(game);
        assertEquals(2, game.getScore(first));
        assertEquals(first, game.getCurrentPlayer());
    }

    @Test
    void anAlreadyPocketedBallCannotScoreAgain() {
        BilliardsGame game = pocketingGame();
        game.shoot(first, game.getTurnId(), (float) Math.PI, 1f);
        settle(game);
        assertEquals(1, game.getScore(first));
        assertTrue(game.shoot(first, game.getTurnId(), 0f, 0.01f));
        settle(game);
        assertEquals(1, game.getScore(first));
        assertEquals(second, game.getCurrentPlayer());
    }

    @Test
    void cueBallScoresNothingAndMarksTheFoul() {
        BilliardsGame game = twoPlayerGame();
        Ball cue = cueBall(game);
        cue.x = 10f;
        cue.y = 80f;
        game.shoot(first, game.getTurnId(), (float) Math.PI, 1f);
        settle(game);
        assertEquals(0, game.getScore(first));
        assertEquals(first, game.getLastFoulPlayer());
        assertEquals(second, game.getCurrentPlayer());
        assertFalse(cue.pocketed);
    }

    @Test
    void cueBallFoulOverridesKeepingTheTurnEvenIfAnObjectBallAlsoDrops() {
        BilliardsGame game = twoPlayerGame();
        game.loadBalls(List.of(new Ball(1, 124f, 80f, 4f), new Ball(0, 6f, 80f, 4f)));
        assertTrue(game.shoot(first, game.getTurnId(), 0f, 0.01f));
        settle(game);
        assertEquals(1, game.getScore(first));
        assertEquals(first, game.getLastFoulPlayer());
        assertEquals(second, game.getCurrentPlayer());
    }

    @Test
    void theNextPlayerWaitsForTheOtherBallsToStopAfterACueBallFoul() {
        BilliardsGame game = twoPlayerGame();
        Ball rolling = new Ball(2, 65f, 40f, 4f);
        game.loadBalls(List.of(new Ball(1, 124f, 80f, 4f), rolling, new Ball(0, 6f, 80f, 4f)));
        assertTrue(game.shoot(first, game.getTurnId(), 0f, 0.01f));
        rolling.vx = 1f;
        game.tick();
        assertTrue(game.isBusy());
        assertEquals(1, game.getScore(first));
        assertEquals(first, game.getLastFoulPlayer());
        assertFalse(game.shoot(second, game.getTurnId(), 0f, 0.1f));
        settle(game);
        assertEquals(second, game.getCurrentPlayer());
        assertTrue(game.shoot(second, game.getTurnId(), 0f, 0.1f));
        assertNull(game.getLastFoulPlayer());
    }

    @Test
    void pointsBelongToTheShooterWhoLeftRatherThanTheNextPlayer() {
        BilliardsGame game = pocketingGame();
        game.shoot(first, game.getTurnId(), (float) Math.PI, 1f);
        game.leavePlayer(first);
        settle(game);
        assertEquals(1, game.getScore(first));
        assertEquals(0, game.getScore(second));
        assertEquals(second, game.getCurrentPlayer());
    }

    @Test
    void closingAndReopeningKeepsTheScoreAndUpdatesTheName() {
        BilliardsGame game = pocketingGame();
        game.shoot(first, game.getTurnId(), (float) Math.PI, 1f);
        settle(game);
        game.leavePlayer(first);
        game.joinPlayer(first, "NewName");
        assertEquals(1, game.getScore(first));
        assertEquals(2, game.getScores().size());
        assertEquals("NewName", game.getScores().getFirst().playerName());
    }

    @Test
    void savingAndLoadingKeepsTheScoreWithoutRestoringAbsentParticipants() {
        BilliardsGame game = pocketingGame();
        game.shoot(first, game.getTurnId(), (float) Math.PI, 1f);
        settle(game);
        BilliardsGame restored = new BilliardsGame();
        restored.loadBalls(game.getBalls().stream().map(Ball::copy).toList());
        restored.loadProgress(game.saveProgress());
        assertEquals(game.getScores(), restored.getScores());
        assertEquals(0, restored.getPlayerCount());
        assertNull(restored.getCurrentPlayer());
        restored.joinPlayer(first, "PlayerOne");
        assertEquals(1, restored.getScore(first));
    }

    @Test
    void loadingAnUnfinishedShotStillAwardsItsShooterWithoutStealingTheNewTurn() {
        BilliardsGame game = pocketingGame();
        game.shoot(first, game.getTurnId(), (float) Math.PI, 1f);
        game.tick();
        assertTrue(game.isBusy());
        assertEquals(0, game.getScore(first));
        BilliardsGame restored = new BilliardsGame();
        restored.loadBalls(game.getBalls().stream().map(Ball::copy).toList());
        restored.loadProgress(game.saveProgress());
        restored.joinPlayer(second, "PlayerTwo");
        settle(restored);
        assertEquals(1, restored.getScore(first));
        assertEquals(0, restored.getScore(second));
        assertEquals(second, restored.getCurrentPlayer());
    }

    @Test
    void continuingAfterScoringRequiresANewTurnIdentity() {
        BilliardsGame game = pocketingGame();
        long oldTurn = game.getTurnId();
        game.shoot(first, oldTurn, (float) Math.PI, 1f);
        settle(game);
        assertEquals(first, game.getCurrentPlayer());
        assertFalse(game.shoot(first, oldTurn, 0f, 0.1f));
        assertTrue(game.shoot(first, game.getTurnId(), 0f, 0.01f));
        settle(game);
        assertEquals(second, game.getCurrentPlayer());
    }

    @Test
    void reloadingAPocketedBallDoesNotDuplicateThePoint() {
        BilliardsGame game = twoPlayerGame();
        game.loadBalls(List.of(new Ball(1, 6f, 80f, 4f), new Ball(0, 65f, 140f, 4f)));
        game.shoot(first, game.getTurnId(), 0f, 0.3f);
        game.tick();
        assertEquals(1, game.getScore(first));
        assertTrue(game.isBusy());
        BilliardsGame restored = new BilliardsGame();
        restored.loadBalls(game.getBalls().stream().map(Ball::copy).toList());
        restored.loadProgress(game.saveProgress());
        settle(restored);
        assertEquals(1, restored.getScore(first));
    }

    @Test
    void savingDuringACueBallFoulStillPassesTheReturningShootersTurn() {
        BilliardsGame game = twoPlayerGame();
        Ball rolling = new Ball(2, 65f, 40f, 4f);
        game.loadBalls(List.of(rolling, new Ball(0, 6f, 80f, 4f)));
        game.shoot(first, game.getTurnId(), 0f, 0.01f);
        rolling.vx = 1f;
        game.tick();
        assertTrue(game.isBusy());
        assertEquals(first, game.getLastFoulPlayer());
        BilliardsGame restored = new BilliardsGame();
        restored.loadBalls(game.getBalls().stream().map(Ball::copy).toList());
        restored.loadProgress(game.saveProgress());
        restored.joinPlayer(first, "PlayerOne");
        restored.joinPlayer(second, "PlayerTwo");
        settle(restored);
        assertEquals(first, restored.getLastFoulPlayer());
        assertEquals(second, restored.getCurrentPlayer());
    }

    @Test
    void missingEveryObjectBallPassesTheTurnAndIdentifiesTheFoul() {
        BilliardsGame game = twoPlayerGame();
        game.loadBalls(List.of(new Ball(1, 65f, 40f, 4f), new Ball(0, 65f, 140f, 4f)));
        assertTrue(game.shoot(first, game.getTurnId(), 0f, 0.3f));
        game.tick();
        assertTrue(game.isBusy());
        assertEquals(first, game.getCurrentPlayer());
        assertNull(game.getLastFoulPlayer());
        assertFalse(game.shoot(second, game.getTurnId(), 0f, 0.1f));
        settle(game);
        assertEquals(second, game.getCurrentPlayer());
        assertEquals(first, game.getLastFoulPlayer());
        assertEquals(FoulReason.NO_BALL_HIT, game.getLastFoulReason());
        assertEquals(0, game.getScore(first));
    }

    @Test
    void hittingABallWithoutPocketingPassesTheTurnWithoutAMissedBallFoul() {
        BilliardsGame game = twoPlayerGame();
        game.loadBalls(List.of(new Ball(1, 60f, 100f, 4f), new Ball(0, 40f, 100f, 4f)));
        assertTrue(game.shoot(first, game.getTurnId(), 0f, 0.15f));
        settle(game);
        assertEquals(second, game.getCurrentPlayer());
        assertEquals(0, game.getScore(first));
        assertNull(game.getLastFoulPlayer());
        assertEquals(FoulReason.NONE, game.getLastFoulReason());
    }

    @Test
    void aPocketedBallCannotKeepTheTurnIfTheCueBallNeverHitAnything() {
        BilliardsGame game = twoPlayerGame();
        game.loadBalls(List.of(new Ball(1, 6f, 80f, 4f), new Ball(0, 65f, 140f, 4f)));
        assertTrue(game.shoot(first, game.getTurnId(), 0f, 0.01f));
        settle(game);
        assertEquals(1, game.getScore(first));
        assertEquals(second, game.getCurrentPlayer());
        assertEquals(FoulReason.NO_BALL_HIT, game.getLastFoulReason());
    }

    @Test
    void bankingOffTheCushionBeforeHittingABallIsNotAMissedBallFoul() {
        BilliardsGame game = twoPlayerGame();
        game.loadBalls(List.of(new Ball(1, 110f, 100f, 4f), new Ball(0, 120f, 100f, 4f)));
        assertTrue(game.shoot(first, game.getTurnId(), 0f, 1f));
        settle(game);
        assertEquals(0, game.getScore(first));
        assertEquals(second, game.getCurrentPlayer());
        assertEquals(FoulReason.NONE, game.getLastFoulReason());
    }

    @Test
    void cueBallFoulTakesPriorityOverNotHittingAnyObjectBall() {
        BilliardsGame game = twoPlayerGame();
        cueBall(game).x = 10f;
        cueBall(game).y = 80f;
        game.shoot(first, game.getTurnId(), (float) Math.PI, 1f);
        settle(game);
        assertEquals(second, game.getCurrentPlayer());
        assertEquals(FoulReason.CUE_BALL, game.getLastFoulReason());
        game.shoot(second, game.getTurnId(), 0f, 0.01f);
        assertNull(game.getLastFoulPlayer());
        assertEquals(FoulReason.NONE, game.getLastFoulReason());
    }

    @Test
    void savingAfterAHitPreservesItForTheRemainderOfTheShot() {
        BilliardsGame game = twoPlayerGame();
        Ball rolling = new Ball(2, 65f, 40f, 4f);
        game.loadBalls(List.of(new Ball(1, 12f, 80f, 4f), new Ball(0, 40f, 80f, 4f), rolling));
        game.shoot(first, game.getTurnId(), (float) Math.PI, 1f);
        rolling.vx = 0.6f;
        game.tick();
        game.tick();
        assertTrue(game.isBusy());
        assertTrue(game.saveProgress().hitObjectBall());
        BilliardsGame restored = new BilliardsGame();
        restored.loadBalls(game.getBalls().stream().map(Ball::copy).toList());
        restored.loadProgress(game.saveProgress());
        restored.joinPlayer(first, "PlayerOne");
        restored.joinPlayer(second, "PlayerTwo");
        settle(restored);
        assertEquals(1, restored.getScore(first));
        assertEquals(first, restored.getCurrentPlayer());
        assertEquals(FoulReason.NONE, restored.getLastFoulReason());
    }

    @Test
    void leavingAndReturningPreservesBallsAndScoresWhileSomeoneElseStays() {
        BilliardsGame game = pocketingGame();
        game.shoot(first, game.getTurnId(), (float) Math.PI, 1f);
        settle(game);
        float whiteX = cueBall(game).x;
        game.leavePlayer(first);
        assertEquals(second, game.getCurrentPlayer());
        assertEquals(1, game.getPlayerCount());
        assertTrue(game.getBalls().getFirst().pocketed);
        game.joinPlayer(first, "PlayerOne");
        assertEquals(List.of(second, first), game.getPlayers());
        assertEquals(second, game.getCurrentPlayer());
        assertEquals(1, game.getScore(first));
        assertEquals(whiteX, cueBall(game).x);
        assertTrue(game.getBalls().getFirst().pocketed);
    }

    @Test
    void theWaitingPlayerCanLeaveAndReturnWithoutResettingTheGame() {
        BilliardsGame game = pocketingGame();
        game.shoot(first, game.getTurnId(), (float) Math.PI, 1f);
        settle(game);
        long turn = game.getTurnId();
        game.leavePlayer(second);
        game.joinPlayer(second, "PlayerTwo");
        assertEquals(first, game.getCurrentPlayer());
        assertEquals(turn, game.getTurnId());
        assertEquals(1, game.getScore(first));
        assertTrue(game.getBalls().getFirst().pocketed);
    }

    @Test
    void theFinalDepartureClearsTheRackScoresAndFouls() {
        BilliardsGame game = twoPlayerGame();
        game.loadBalls(List.of(new Ball(1, 124f, 80f, 4f), new Ball(0, 6f, 80f, 4f)));
        game.shoot(first, game.getTurnId(), 0f, 0.01f);
        settle(game);
        assertEquals(1, game.getScore(first));
        assertEquals(FoulReason.CUE_BALL, game.getLastFoulReason());
        game.leavePlayer(first);
        assertEquals(1, game.getScore(first));
        assertEquals(1, game.getPlayerCount());
        game.leavePlayer(second);
        assertFreshGame(game);
        BilliardsGame restored = new BilliardsGame();
        restored.loadBalls(game.getBalls().stream().map(Ball::copy).toList());
        restored.loadProgress(game.saveProgress());
        assertFreshGame(restored);
    }

    @Test
    void closingASoloGameDuringAShotCancelsItAndStartsFreshOnReopening() {
        BilliardsGame game = new BilliardsGame();
        game.joinPlayer(first, "PlayerOne");
        long oldTurn = game.getTurnId();
        game.shoot(first, oldTurn, 0f, 1f);
        game.tick();
        assertTrue(game.isBusy());
        game.leavePlayer(first);
        assertFreshGame(game);
        assertFalse(game.tick());
        game.joinPlayer(first, "PlayerOne");
        assertEquals(0, game.getScore(first));
        assertFalse(game.shoot(first, oldTurn, 0f, 1f));
        assertTrue(game.shoot(first, game.getTurnId(), 0f, 0.1f));
    }

    @Test
    void bothPlayersLeavingDuringAShotCancelsItOnlyAfterTheSecondDeparture() {
        BilliardsGame game = twoPlayerGame();
        game.shoot(first, game.getTurnId(), 0f, 1f);
        game.tick();
        game.leavePlayer(first);
        assertTrue(game.isBusy());
        assertEquals(second, game.getCurrentPlayer());
        game.leavePlayer(second);
        assertFreshGame(game);
        assertFalse(game.tick());
        game.joinPlayer(first, "PlayerOne");
        game.joinPlayer(second, "PlayerTwo");
        assertEquals(first, game.getCurrentPlayer());
        assertEquals(0, game.getScore(first));
        assertEquals(0, game.getScore(second));
        assertEquals(16, game.getBalls().size());
    }

    @Test
    void threePlayersMustAllLeaveBeforeTheGameResets() {
        BilliardsGame game = pocketingGame();
        UUID third = UUID.randomUUID();
        game.joinPlayer(third, "PlayerThree");
        game.shoot(first, game.getTurnId(), (float) Math.PI, 1f);
        settle(game);
        game.leavePlayer(first);
        game.leavePlayer(second);
        assertEquals(third, game.getCurrentPlayer());
        assertEquals(1, game.getScore(first));
        assertTrue(game.getBalls().getFirst().pocketed);
        game.leavePlayer(third);
        assertFreshGame(game);
    }
}
