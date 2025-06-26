package ru.kawaii.easycameramod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import ru.kawaii.easycameramod.client.PlayerDisplaySettingsManager;
import ru.kawaii.easycameramod.client.PlayerWebcamManager;
import ru.kawaii.easycameramod.client.WebcamTextureManager;
import ru.kawaii.easycameramod.config.Config;
import ru.kawaii.easycameramod.config.ConfigManager;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class HudRenderer {
    public static void init() {
        WebcamTextureManager textureManager = PlayerWebcamManager.getInstance().getTextureManager();
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            Config config = ConfigManager.getConfig();
            if (config.webcamEnabled) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player == null) return;
                UUID playerUuid = client.player.getUuid();

                // We check for frameData to see if a webcam is active for the player
                byte[] frameData = PlayerWebcamManager.getInstance().getPlayerWebcam(playerUuid);
                if (frameData != null && config.cameraIndicator != Config.CameraIndicator.NONE) {
                    Identifier textureId = textureManager.getTexture(playerUuid);
                    if (textureId == null) return;

                    var playerSettings = PlayerDisplaySettingsManager.getInstance().getSettings(playerUuid);

                    double guiScale = client.getWindow().getScaleFactor();
                    
                    // Define size and margin in pixels
                    float basePixelSize = 150.0f;
                    float desiredPixelSize = basePixelSize * config.cameraIndicatorScale;
                    float marginInPixels = 10.0f;

                    // Convert pixel values to GUI units for rendering
                    float size = desiredPixelSize / (float)guiScale;
                    float margin = marginInPixels / (float)guiScale;

                    int screenWidth = drawContext.getScaledWindowWidth();
                    int screenHeight = drawContext.getScaledWindowHeight();
                    float x = 0, y = 0;

                    switch (config.cameraIndicator) {
                        case TOP_LEFT:
                            x = margin;
                            y = margin;
                            break;
                        case TOP_RIGHT:
                            x = screenWidth - size - margin;
                            y = margin;
                            break;
                        case BOTTOM_LEFT:
                            x = margin;
                            y = screenHeight - size - margin;
                            break;
                        case BOTTOM_RIGHT:
                            x = screenWidth - size - margin;
                            y = screenHeight - size - margin;
                            break;
                    }

                    RenderSystem.setShader(GameRenderer::getPositionTexProgram);
                    RenderSystem.setShaderTexture(0, textureId);
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.disableCull();

                    Tessellator tessellator = Tessellator.getInstance();
                    BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
                    
                    drawContext.getMatrices().push();
                    drawContext.getMatrices().translate(x + size / 2.0f, y + size / 2.0f, 0);
                    Matrix4f positionMatrix = drawContext.getMatrices().peek().getPositionMatrix();

                    // Use our renderer to draw the geometry directly to the buffer
                    WebcamRenderer.render(buffer, positionMatrix, playerUuid, size, size);

                    // We must draw the buffer here
                    BufferRenderer.drawWithGlobalProgram(buffer.end());

                    drawContext.getMatrices().pop();

                    RenderSystem.enableCull();
                    RenderSystem.disableBlend();
                }
            }
        });
    }
}
