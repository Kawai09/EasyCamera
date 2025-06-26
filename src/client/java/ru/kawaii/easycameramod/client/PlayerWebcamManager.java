package ru.kawaii.easycameramod.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class PlayerWebcamManager {
   private static final PlayerWebcamManager INSTANCE = new PlayerWebcamManager();
   private final Map<UUID, byte[]> playerWebcams = new ConcurrentHashMap<>();
   private final WebcamTextureManager textureManager = new WebcamTextureManager();

   private PlayerWebcamManager() {
   }

   public static PlayerWebcamManager getInstance() {
      return INSTANCE;
   }

   public void initialize() {
      this.textureManager.clearAll();
   }

   public static void onWebcamDataReceived(UUID playerUuid, byte[] frameData) {
      getInstance().updatePlayerWebcam(playerUuid, frameData);
   }

   public static void onPlayerDisconnect(UUID playerUuid) {
      getInstance().removePlayerWebcam(playerUuid);
   }

   public void updatePlayerWebcam(UUID playerUuid, byte[] frameData) {
      this.playerWebcams.put(playerUuid, frameData);
      this.textureManager.updateTexture(playerUuid, frameData);
   }

   public byte[] getPlayerWebcam(UUID playerUuid) {
      return this.playerWebcams.get(playerUuid);
   }

   public WebcamTextureManager getTextureManager() {
      return this.textureManager;
   }

   public void removePlayerWebcam(UUID playerUuid) {
      this.playerWebcams.remove(playerUuid);
      this.textureManager.removePlayer(playerUuid);
   }

   public void clearAll() {
      this.playerWebcams.clear();
      this.textureManager.clearAll();
   }

   public Map<UUID, byte[]> getAllPlayerWebcams() {
      return this.playerWebcams;
   }

   public boolean hasWebcam(UUID playerUuid) {
      return this.playerWebcams.containsKey(playerUuid);
   }
}
