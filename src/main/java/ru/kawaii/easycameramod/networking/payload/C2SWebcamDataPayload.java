package ru.kawaii.easycameramod.networking.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import ru.kawaii.easycameramod.networking.NetworkConstants;

public record C2SWebcamDataPayload(byte[] frameData) implements CustomPayload {
    public static final CustomPayload.Id<C2SWebcamDataPayload> ID = new CustomPayload.Id<>(NetworkConstants.C2S_WEBCAM_DATA_PACKET_ID);
    public static final PacketCodec<ByteBuf, C2SWebcamDataPayload> CODEC = PacketCodec.of(C2SWebcamDataPayload::write, C2SWebcamDataPayload::new);

    public C2SWebcamDataPayload(ByteBuf buf) {
        this(readByteArray(buf));
    }

    private void write(ByteBuf buf) {
        buf.writeBytes(this.frameData);
    }

    private static byte[] readByteArray(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
