package ru.kawaii.easycameramod.networking.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import ru.kawaii.easycameramod.networking.NetworkConstants;

import java.util.UUID;

public record S2CRemoveWebcamPayload(UUID playerUuid) implements CustomPayload {
    public static final CustomPayload.Id<S2CRemoveWebcamPayload> ID = new CustomPayload.Id<>(NetworkConstants.S2C_REMOVE_WEBCAM_PACKET_ID);
    public static final PacketCodec<ByteBuf, S2CRemoveWebcamPayload> CODEC = PacketCodec.of(S2CRemoveWebcamPayload::write, S2CRemoveWebcamPayload::new);

    public S2CRemoveWebcamPayload(ByteBuf buf) {
        this(new UUID(buf.readLong(), buf.readLong()));
    }

    private void write(ByteBuf buf) {
        buf.writeLong(playerUuid.getMostSignificantBits());
        buf.writeLong(playerUuid.getLeastSignificantBits());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public UUID playerUuid() {
        return this.playerUuid;
    }
}
