package ru.kawaii.easycameramod.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.util.Identifier;
import ru.kawaii.easycameramod.EasyCameraMod;
import ru.kawaii.easycameramod.networking.payload.*;

public class NetworkConstants {
   public static final Identifier C2S_WEBCAM_DATA_PACKET_ID = Identifier.of(EasyCameraMod.MOD_ID, "c2s_webcam_data");
   public static final Identifier S2C_WEBCAM_DATA_PACKET_ID = Identifier.of(EasyCameraMod.MOD_ID, "s2c_webcam_data");
   public static final Identifier S2C_REMOVE_WEBCAM_PACKET_ID = Identifier.of(EasyCameraMod.MOD_ID, "s2c_remove_webcam");
   public static final Identifier C2S_UPDATE_DISPLAY_SETTINGS_PACKET_ID = Identifier.of(EasyCameraMod.MOD_ID, "c2s_update_display_settings");
   public static final Identifier S2C_DISPLAY_SETTINGS_PACKET_ID = Identifier.of(EasyCameraMod.MOD_ID, "s2c_display_settings");

   public static void register() {
      // Client to Server
      PayloadTypeRegistry.playC2S().register(C2SWebcamDataPayload.ID, C2SWebcamDataPayload.CODEC);
      PayloadTypeRegistry.playC2S().register(C2SUpdateDisplaySettingsPayload.ID, C2SUpdateDisplaySettingsPayload.CODEC);

      // Server to Client
      PayloadTypeRegistry.playS2C().register(S2CWebcamDataPayload.ID, S2CWebcamDataPayload.CODEC);
      PayloadTypeRegistry.playS2C().register(S2CRemoveWebcamPayload.ID, S2CRemoveWebcamPayload.CODEC);
      PayloadTypeRegistry.playS2C().register(S2CDisplaySettingsPayload.ID, S2CDisplaySettingsPayload.CODEC);
   }
} 