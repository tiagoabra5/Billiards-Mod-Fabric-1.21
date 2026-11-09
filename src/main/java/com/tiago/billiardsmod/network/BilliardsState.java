package com.tiago.billiardsmod.network;

import com.tiago.billiardsmod.billiards.Ball;
import com.tiago.billiardsmod.billiards.PlayerScore;
import com.tiago.billiardsmod.billiards.FoulReason;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A detached snapshot: later server ticks cannot mutate a packet being sent. */
public record BilliardsState(List<Ball> balls, @Nullable UUID currentPlayer, String currentPlayerName,
                            int playerCount, boolean moving, long turnId, List<PlayerScore> scores,
                            @Nullable UUID foulPlayer, FoulReason foulReason) {
    public static final PacketCodec<PacketByteBuf, BilliardsState> CODEC =
            PacketCodec.of(BilliardsState::write, BilliardsState::read);

    public BilliardsState {
        balls = balls.stream().map(Ball::copy).toList();
        scores = List.copyOf(scores);
    }

    private void write(PacketByteBuf buf) {
        buf.writeVarInt(balls.size());
        for (Ball ball : balls) {
            buf.writeVarInt(ball.number);
            buf.writeFloat(ball.x);
            buf.writeFloat(ball.y);
            buf.writeFloat(ball.vx);
            buf.writeFloat(ball.vy);
            buf.writeFloat(ball.radius);
            buf.writeBoolean(ball.pocketed);
        }
        buf.writeBoolean(currentPlayer != null);
        if (currentPlayer != null) buf.writeUuid(currentPlayer);
        buf.writeString(currentPlayerName, 16);
        buf.writeVarInt(playerCount);
        buf.writeBoolean(moving);
        buf.writeVarLong(turnId);
        buf.writeVarInt(scores.size());
        for (PlayerScore score : scores) {
            buf.writeUuid(score.playerId());
            buf.writeString(score.playerName(), 16);
            buf.writeVarInt(score.points());
        }
        buf.writeBoolean(foulPlayer != null);
        if (foulPlayer != null) buf.writeUuid(foulPlayer);
        buf.writeEnumConstant(foulReason);
    }

    private static BilliardsState read(PacketByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 16) throw new IllegalArgumentException("Invalid billiards ball count: " + count);
        List<Ball> balls = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int number = buf.readVarInt();
            if (number < 0 || number > 15) throw new IllegalArgumentException("Invalid ball number: " + number);
            float x = buf.readFloat();
            float y = buf.readFloat();
            float vx = buf.readFloat();
            float vy = buf.readFloat();
            Ball decoded = new Ball(number, x, y, buf.readFloat());
            decoded.vx = vx;
            decoded.vy = vy;
            decoded.pocketed = buf.readBoolean();
            balls.add(decoded);
        }
        UUID currentPlayer = buf.readBoolean() ? buf.readUuid() : null;
        String name = buf.readString(16);
        int playerCount = buf.readVarInt();
        boolean moving = buf.readBoolean();
        long turnId = buf.readVarLong();
        int scoreCount = buf.readVarInt();
        // Even an empty name needs 16 UUID bytes, one length byte and one points byte.
        if (scoreCount < 0 || scoreCount > buf.readableBytes() / 18) {
            throw new IllegalArgumentException("Invalid billiards score count: " + scoreCount);
        }
        List<PlayerScore> scores = new ArrayList<>(scoreCount);
        for (int i = 0; i < scoreCount; i++) {
            scores.add(new PlayerScore(buf.readUuid(), buf.readString(16), buf.readVarInt()));
        }
        UUID foulPlayer = buf.readBoolean() ? buf.readUuid() : null;
        return new BilliardsState(balls, currentPlayer, name, playerCount, moving, turnId, scores, foulPlayer,
                buf.readEnumConstant(FoulReason.class));
    }
}
