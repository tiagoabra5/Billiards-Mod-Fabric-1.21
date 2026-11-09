package com.tiago.billiardsmod.network;

import com.tiago.billiardsmod.BilliardsMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record ShootPacket(int syncId, BlockPos tablePos, long turnId, float angle, float power) implements CustomPayload {

    public static final CustomPayload.Id<ShootPacket> ID =
            new CustomPayload.Id<>(BilliardsMod.id("shoot"));

    public static final PacketCodec<PacketByteBuf, ShootPacket> CODEC = PacketCodec.of(
            (packet, buf) -> {
                buf.writeVarInt(packet.syncId);
                buf.writeBlockPos(packet.tablePos);
                buf.writeVarLong(packet.turnId);
                buf.writeFloat(packet.angle);
                buf.writeFloat(packet.power);
            },
            buf -> new ShootPacket(buf.readVarInt(), buf.readBlockPos(), buf.readVarLong(),
                    buf.readFloat(), buf.readFloat())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
