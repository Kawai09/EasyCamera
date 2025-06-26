package ru.kawaii.easycameramod.networking;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import ru.kawaii.easycameramod.client.PlayerDisplaySettingsManager;
import ru.kawaii.easycameramod.client.PlayerWebcamManager;
import ru.kawaii.easycameramod.config.PlayerDisplaySettings;
import ru.kawaii.easycameramod.networking.payload.S2CDisplaySettingsPayload;
import ru.kawaii.easycameramod.networking.payload.S2CRemoveWebcamPayload;
import ru.kawaii.easycameramod.networking.payload.S2CWebcamDataPayload;

@Environment(EnvType.CLIENT)
public class ClientPacketHandler {
   public static void register() {
      ClientPlayNetworking.registerGlobalReceiver(S2CWebcamDataPayload.ID, (payload, context) -> {
         context.client().execute(() -> {
            PlayerWebcamManager.onWebcamDataReceived(payload.playerUuid(), payload.frameData());
         });
      });
      ClientPlayNetworking.registerGlobalReceiver(S2CRemoveWebcamPayload.ID, (payload, context) -> {
         context.client().execute(() -> {
            PlayerWebcamManager.onPlayerDisconnect(payload.playerUuid());
         });
      });
      ClientPlayNetworking.registerGlobalReceiver(S2CDisplaySettingsPayload.ID, (payload, context) -> {
         PlayerDisplaySettings settings = new PlayerDisplaySettings(
                 payload.displayMode(),
                 payload.cropBoxX(),
                 payload.cropBoxY(),
                 payload.cropBoxSize()
         );
         context.client().execute(() -> {
            PlayerDisplaySettingsManager.getInstance().updateSettings(payload.playerUuid(), settings);
         });
      });
   }
}
