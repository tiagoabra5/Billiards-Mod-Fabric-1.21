package com.tiago.billiardsmod.network;

import com.tiago.billiardsmod.BilliardsMod;
import com.tiago.billiardsmod.screen.custom.BilliardsScreenHandler.OpeningData;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record TableStatePacket(int syncId, OpeningData table) implements CustomPayload {
    public static final Id<TableStatePacket> ID = new Id<>(BilliardsMod.id("table_state"));
    public static final PacketCodec<PacketByteBuf, TableStatePacket> CODEC = PacketCodec.of(
            (packet, buf) -> {
                buf.writeVarInt(packet.syncId);
                OpeningData.CODEC.encode(buf, packet.table);
            },
            buf -> new TableStatePacket(buf.readVarInt(), OpeningData.CODEC.decode(buf))
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
