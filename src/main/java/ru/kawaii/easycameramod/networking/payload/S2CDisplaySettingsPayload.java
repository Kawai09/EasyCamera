package ru.kawaii.easycameramod.networking.payload;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.PacketByteBuf;
import ru.kawaii.easycameramod.config.Config;
import ru.kawaii.easycameramod.networking.NetworkConstants;

import java.util.UUID;

public record S2CDisplaySettingsPayload(
        UUID playerUuid,
        Config.DisplayMode displayMode,
        float cropBoxX,
        float cropBoxY,
        float cropBoxSize
) implements CustomPayload {

    public static final CustomPayload.Id<S2CDisplaySettingsPayload> ID = new CustomPayload.Id<>(NetworkConstants.S2C_DISPLAY_SETTINGS_PACKET_ID);
    public static final PacketCodec<PacketByteBuf, S2CDisplaySettingsPayload> CODEC = PacketCodec.of(
            S2CDisplaySettingsPayload::write,
            S2CDisplaySettingsPayload::new
    );

    private S2CDisplaySettingsPayload(PacketByteBuf buf) {
        this(
                buf.readUuid(),
                buf.readEnumConstant(Config.DisplayMode.class),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    private void write(PacketByteBuf buf) {
        buf.writeUuid(playerUuid);
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