package ru.kawaii.easycameramod.networking.payload;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.PacketByteBuf;
import ru.kawaii.easycameramod.config.Config;
import ru.kawaii.easycameramod.networking.NetworkConstants;

public record C2SUpdateDisplaySettingsPayload(
        Config.DisplayMode displayMode,
        float cropBoxX,
        float cropBoxY,
        float cropBoxSize
) implements CustomPayload {

    public static final CustomPayload.Id<C2SUpdateDisplaySettingsPayload> ID = new CustomPayload.Id<>(NetworkConstants.C2S_UPDATE_DISPLAY_SETTINGS_PACKET_ID);
    public static final PacketCodec<PacketByteBuf, C2SUpdateDisplaySettingsPayload> CODEC = PacketCodec.of(
            C2SUpdateDisplaySettingsPayload::write,
            C2SUpdateDisplaySettingsPayload::new
    );

    private C2SUpdateDisplaySettingsPayload(PacketByteBuf buf) {
        this(
                buf.readEnumConstant(Config.DisplayMode.class),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    private void write(PacketByteBuf buf) {
        buf.writeEnumConstant(displayMode);
        buf.writeFloat(cropBoxX);
        buf.writeFloat(cropBoxY);
        buf.writeFloat(cropBoxSize);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
} 