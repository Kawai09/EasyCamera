package ru.kawaii.easycameramod;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import ru.kawaii.easycameramod.client.PlayerWebcamManager;
import ru.kawaii.easycameramod.config.ConfigManager;
import ru.kawaii.easycameramod.networking.payload.C2SWebcamDataPayload;

@Environment(EnvType.CLIENT)
public class WebcamManager {
   private static final WebcamManager INSTANCE = new WebcamManager();
   private Webcam currentWebcam = null;
   private volatile boolean isSending = false;
   private Thread sendingThread;
   public static final int TARGET_FPS = 15;
   public static final Dimension RESOLUTION;

   private WebcamManager() {
   }

   public static WebcamManager getInstance() {
      return INSTANCE;
   }

   public void initialize() {
      String selectedWebcamName = ConfigManager.getConfig().selectedWebcam;
      if (selectedWebcamName != null && !selectedWebcamName.isEmpty()) {
         for (Webcam webcam : getWebcams()) {
            if (webcam.getName().equals(selectedWebcamName)) {
               selectWebcam(webcam);
               break;
            }
         }
      }

      if (ConfigManager.getConfig().webcamEnabled) {
         start();
      }
   }

   public List<Webcam> getWebcams() {
      return Webcam.getWebcams();
   }

   public void selectWebcam(Webcam webcam) {
      if (this.currentWebcam != null && this.currentWebcam.isOpen()) {
         this.stop();
      }

      this.currentWebcam = webcam;
      if (this.currentWebcam != null) {
         this.currentWebcam.setViewSize(RESOLUTION);
         ConfigManager.getConfig().selectedWebcam = this.currentWebcam.getName();
      } else {
         ConfigManager.getConfig().selectedWebcam = "";
      }
      ConfigManager.saveConfig();
   }

   public void start() {
      if (this.currentWebcam != null && !this.currentWebcam.isOpen()) {
         try {
            this.currentWebcam.open();
            this.isSending = true;
            this.sendingThread = new Thread(this::sendingLoop, "Webcam-Sending-Thread");
            this.sendingThread.setDaemon(true);
            this.sendingThread.start();
         } catch (Exception e) {
            EasyCameraMod.LOGGER.error("Failed to start webcam, it might be already in use: " + e.getMessage());
         }
      }
   }

   public void stop() {
      this.isSending = false;

      try {
         if (this.sendingThread != null) {
            this.sendingThread.join();
            this.sendingThread = null;
         }
      } catch (InterruptedException var2) {
         EasyCameraMod.LOGGER.error("Failed to stop sending thread", var2);
      }

      if (this.currentWebcam != null && this.currentWebcam.isOpen()) {
         this.currentWebcam.close();
      }

      MinecraftClient client = MinecraftClient.getInstance();
      if(client.player != null) {
          PlayerWebcamManager.getInstance().removePlayerWebcam(client.player.getUuid());
          if (ClientPlayNetworking.canSend(C2SWebcamDataPayload.ID)) {
              ClientPlayNetworking.send(new C2SWebcamDataPayload(new byte[0]));
          }
      }
   }

   private void sendingLoop() {
      while(this.isSending) {
         try {
            long startTime = System.currentTimeMillis();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.world != null && client.player != null) {
               byte[] frameData = this.getFrameAsJpeg();
               if (frameData != null && frameData.length > 0) {
                  PlayerWebcamManager.getInstance().updatePlayerWebcam(client.player.getUuid(), frameData);
                  if (ClientPlayNetworking.canSend(C2SWebcamDataPayload.ID)) {
                     ClientPlayNetworking.send(new C2SWebcamDataPayload(frameData));
                  }
               }
            }

            long processingTime = System.currentTimeMillis() - startTime;
            long sleepTime = (1000 / TARGET_FPS) - processingTime;
            if (sleepTime > 0L) {
               Thread.sleep(sleepTime);
            }
         } catch (Exception var8) {
            EasyCameraMod.LOGGER.error("Error in webcam sending loop", var8);
            this.isSending = false;
         }
      }
   }

   public BufferedImage getFrame() {
      return this.currentWebcam != null && this.currentWebcam.isOpen() ? this.currentWebcam.getImage() : null;
   }

   public byte[] getFrameAsJpeg() {
      BufferedImage frame = this.getFrame();
      if (frame != null) {
         try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(frame, "jpeg", baos);
            return baos.toByteArray();
         } catch (IOException var3) {
            EasyCameraMod.LOGGER.error("Failed to compress frame to JPEG", var3);
         }
      }

      return null;
   }

   public Webcam getCurrentWebcam() {
      return this.currentWebcam;
   }

   static {
      RESOLUTION = WebcamResolution.QQVGA.getSize();
   }
}
