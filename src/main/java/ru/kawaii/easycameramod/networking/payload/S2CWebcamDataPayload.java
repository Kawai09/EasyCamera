package ru.kawaii.easycameramod.networking.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import ru.kawaii.easycameramod.networking.NetworkConstants;

import java.util.UUID;

public record S2CWebcamDataPayload(UUID playerUuid, byte[] frameData) implements CustomPayload {
    public static final CustomPayload.Id<S2CWebcamDataPayload> ID = new CustomPayload.Id<>(NetworkConstants.S2C_WEBCAM_DATA_PACKET_ID);
    public static final PacketCodec<ByteBuf, S2CWebcamDataPayload> CODEC = PacketCodec.of(S2CWebcamDataPayload::write, S2CWebcamDataPayload::new);

    public S2CWebcamDataPayload(ByteBuf buf) {
        this(new UUID(buf.readLong(), buf.readLong()), PacketCodecs.BYTE_ARRAY.decode(buf));
    }

    private void write(ByteBuf buf) {
        buf.writeLong(playerUuid.getMostSignificantBits());
        buf.writeLong(playerUuid.getLeastSignificantBits());
        PacketCodecs.BYTE_ARRAY.encode(buf, frameData);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public UUID playerUuid() {
        return this.playerUuid;
    }

    public byte[] frameData() {
        return this.frameData;
    }
}
