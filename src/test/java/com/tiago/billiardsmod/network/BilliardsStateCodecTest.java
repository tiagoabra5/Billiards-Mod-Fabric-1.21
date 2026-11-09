package com.tiago.billiardsmod.network;

import com.tiago.billiardsmod.billiards.Ball;
import com.tiago.billiardsmod.billiards.BilliardsGame;
import com.tiago.billiardsmod.billiards.PlayerScore;
import com.tiago.billiardsmod.billiards.FoulReason;
import com.tiago.billiardsmod.screen.custom.BilliardsScreenHandler.OpeningData;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BilliardsStateCodecTest {
    private static BilliardsState snapshot(BilliardsGame game) {
        return new BilliardsState(game.getBalls(), game.getCurrentPlayer(), "PlayerOne",
                game.getPlayerCount(), game.isBusy(), game.getTurnId(), game.getScores(), game.getLastFoulPlayer(),
                game.getLastFoulReason());
    }

    private static void assertSameState(BilliardsState expected, BilliardsState actual) {
        assertEquals(expected.currentPlayer(), actual.currentPlayer());
        assertEquals(expected.currentPlayerName(), actual.currentPlayerName());
        assertEquals(expected.playerCount(), actual.playerCount());
        assertEquals(expected.moving(), actual.moving());
        assertEquals(expected.turnId(), actual.turnId());
        assertEquals(expected.scores(), actual.scores());
        assertEquals(expected.foulPlayer(), actual.foulPlayer());
        assertEquals(expected.foulReason(), actual.foulReason());
        assertEquals(expected.balls().size(), actual.balls().size());
        for (int i = 0; i < expected.balls().size(); i++) {
            Ball a = expected.balls().get(i);
            Ball b = actual.balls().get(i);
            assertEquals(a.number, b.number);
            assertEquals(a.x, b.x);
            assertEquals(a.y, b.y);
            assertEquals(a.vx, b.vx);
            assertEquals(a.vy, b.vy);
            assertEquals(a.radius, b.radius);
            assertEquals(a.pocketed, b.pocketed);
        }
    }

    @Test
    void twoClientsReceiveTheSameMovingGameAndCurrentTurn() {
        BilliardsGame game = new BilliardsGame();
        UUID first = UUID.randomUUID();
        game.joinPlayer(first, "PlayerOne");
        game.joinPlayer(UUID.randomUUID(), "PlayerTwo");
        game.shoot(first, game.getTurnId(), -1f, 0.5f);
        game.tick();
        game.getBalls().getFirst().pocketed = true;
        BilliardsState state = snapshot(game);

        for (int client = 0; client < 2; client++) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            try {
                BilliardsState.CODEC.encode(buf, state);
                assertSameState(state, BilliardsState.CODEC.decode(buf));
                assertFalse(buf.isReadable());
            } finally {
                buf.release();
            }
        }
    }

    @Test
    void packetsKeepTheirPositionsEvenWhenTheNextServerTickRuns() {
        BilliardsGame game = new BilliardsGame();
        UUID player = UUID.randomUUID();
        game.joinPlayer(player, "PlayerOne");
        game.shoot(player, game.getTurnId(), 0f, 0.5f);
        BilliardsState queuedPacket = snapshot(game);
        float snapshotX = queuedPacket.balls().getLast().x;
        game.tick();
        assertEquals(snapshotX, queuedPacket.balls().getLast().x);
        assertNotEquals(snapshotX, game.getBalls().getLast().x);
    }

    @Test
    void lateJoinOpeningAndLiveUpdatesContainTheSameCompleteGame() {
        BilliardsGame game = new BilliardsGame();
        UUID first = UUID.randomUUID();
        game.joinPlayer(first, "PlayerOne");
        game.shoot(first, game.getTurnId(), 0f, 0.4f);
        game.tick();
        game.joinPlayer(UUID.randomUUID(), "PlayerTwo");
        OpeningData opening = new OpeningData(new BlockPos(10, 64, -20), snapshot(game));
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        try {
            OpeningData.CODEC.encode(buf, opening);
            OpeningData decoded = OpeningData.CODEC.decode(buf);
            assertEquals(opening.tablePos(), decoded.tablePos());
            assertSameState(opening.state(), decoded.state());

            buf.clear();
            TableStatePacket.CODEC.encode(buf, new TableStatePacket(7, opening));
            TableStatePacket packet = TableStatePacket.CODEC.decode(buf);
            assertEquals(7, packet.syncId());
            assertEquals(opening.tablePos(), packet.table().tablePos());
            assertSameState(opening.state(), packet.table().state());
            assertFalse(buf.isReadable());
        } finally {
            buf.release();
        }
    }

    @Test
    void anEmptyTableCanBeSynchronized() {
        BilliardsGame game = new BilliardsGame();
        BilliardsState state = new BilliardsState(game.getBalls(), null, "", 0, false, 0, List.of(), null, FoulReason.NONE);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        try {
            BilliardsState.CODEC.encode(buf, state);
            assertSameState(state, BilliardsState.CODEC.decode(buf));
        } finally {
            buf.release();
        }
    }

    @Test
    void aShotKeepsItsScreenTableAndTurnIdentity() {
        ShootPacket shot = new ShootPacket(12, new BlockPos(-5, 80, 9), 123L, -1.2f, 0.75f);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        try {
            ShootPacket.CODEC.encode(buf, shot);
            assertEquals(shot, ShootPacket.CODEC.decode(buf));
            assertFalse(buf.isReadable());
        } finally {
            buf.release();
        }
    }

    @Test
    void bothClientsReceiveTheScoreboardAndCueBallFoul() {
        BilliardsGame game = new BilliardsGame();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        List<PlayerScore> scores = List.of(new PlayerScore(first, "PlayerOne", 3),
                new PlayerScore(second, "PlayerTwo", 2));
        BilliardsState state = new BilliardsState(game.getBalls(), second, "PlayerTwo", 2,
                false, 10, scores, first, FoulReason.CUE_BALL);
        for (int client = 0; client < 2; client++) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            try {
                BilliardsState.CODEC.encode(buf, state);
                assertSameState(state, BilliardsState.CODEC.decode(buf));
                assertFalse(buf.isReadable());
            } finally {
                buf.release();
            }
        }
    }

    @Test
    void bothClientsReceiveTheMissedBallFoulAndTheNextPlayer() {
        BilliardsGame game = new BilliardsGame();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        game.joinPlayer(first, "PlayerOne");
        game.joinPlayer(second, "PlayerTwo");
        game.shoot(first, game.getTurnId(), 0f, 0.01f);
        while (game.isBusy()) game.tick();
        assertEquals(second, game.getCurrentPlayer());
        assertEquals(FoulReason.NO_BALL_HIT, game.getLastFoulReason());
        BilliardsState state = snapshot(game);
        for (int client = 0; client < 2; client++) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            try {
                BilliardsState.CODEC.encode(buf, state);
                assertSameState(state, BilliardsState.CODEC.decode(buf));
                assertFalse(buf.isReadable());
            } finally {
                buf.release();
            }
        }
    }
}
