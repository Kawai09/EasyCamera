package ru.kawaii.easycameramod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import ru.kawaii.easycameramod.client.KeyBinds;
import ru.kawaii.easycameramod.client.MutedPlayersManager;
import ru.kawaii.easycameramod.client.PlayerDisplaySettingsManager;
import ru.kawaii.easycameramod.client.PlayerWebcamManager;
import ru.kawaii.easycameramod.client.renderer.HudRenderer;
import ru.kawaii.easycameramod.client.renderer.WebcamFeatureRenderer;
import ru.kawaii.easycameramod.config.Config;
import ru.kawaii.easycameramod.config.ConfigManager;
import ru.kawaii.easycameramod.networking.ClientPacketHandler;
import ru.kawaii.easycameramod.networking.NetworkConstants;
import ru.kawaii.easycameramod.WebcamManager;
import com.github.sarxos.webcam.WebcamException;

@Environment(EnvType.CLIENT)
public class EasyCameraModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        NetworkConstants.register();

        ConfigManager.loadConfig();
        MutedPlayersManager.loadMutedPlayers();
        disableWebcamForSafety();

        KeyBinds.register();
        ClientPacketHandler.register();
        PlayerWebcamManager.getInstance().initialize();
        HudRenderer.init();

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                registrationHelper.register(new WebcamFeatureRenderer<>(playerRenderer));
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PlayerWebcamManager.getInstance().clearAll();
            PlayerDisplaySettingsManager.getInstance().clearAll();
            WebcamManager.getInstance().stop();
            try {
                WebcamManager.getInstance().selectWebcam(null);
            } catch (WebcamException e) {
                EasyCameraMod.LOGGER.error("Failed to deselect webcam on disconnect", e);
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            PlayerWebcamManager.getInstance().clearAll();
            PlayerDisplaySettingsManager.getInstance().clearAll();
            disableWebcamForSafety();
        });
    }

    private void disableWebcamForSafety() {
        Config config = ConfigManager.getConfig();
        config.webcamEnabled = false;
        config.selectedWebcam = "";
        ConfigManager.saveConfig();
        WebcamManager.getInstance().stop();
    }
}
