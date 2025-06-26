package ru.kawaii.easycameramod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kawaii.easycameramod.networking.payload.C2SWebcamDataPayload;
import ru.kawaii.easycameramod.networking.payload.S2CRemoveWebcamPayload;
import ru.kawaii.easycameramod.networking.payload.S2CWebcamDataPayload;
import ru.kawaii.easycameramod.config.ConfigManager;

public class EasyCameraMod implements ModInitializer {
   public static final String MOD_ID = "easycameramod";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

   @Override
   public void onInitialize() {
      LOGGER.info("EasyCameraMod has been initialized!");
      ConfigManager.loadConfig();
      // Packet registration and handling will be done in dedicated classes.
   }
}
