package ru.kawaii.easycameramod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ru.kawaii.easycameramod.client.MutedPlayersManager;
import ru.kawaii.easycameramod.client.PlayerDisplaySettingsManager;
import ru.kawaii.easycameramod.client.PlayerWebcamManager;
import ru.kawaii.easycameramod.client.WebcamTextureManager;
import ru.kawaii.easycameramod.config.Config;
import ru.kawaii.easycameramod.config.ConfigManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public class WebcamFeatureRenderer<T extends LivingEntity, M extends PlayerEntityModel<T>> extends FeatureRenderer<T, M> {

    public WebcamFeatureRenderer(FeatureRendererContext<T, M> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        // Do not render the webcam circle if we are in an inventory screen
        if (MinecraftClient.getInstance().currentScreen instanceof AbstractInventoryScreen) {
            return;
        }

        if (!(entity instanceof PlayerEntity) || !entity.isAlive() || MutedPlayersManager.isMuted(entity.getUuid())) {
            return;
        }

        PlayerWebcamManager webcamManager = PlayerWebcamManager.getInstance();
        if (!webcamManager.hasWebcam(entity.getUuid())) {
            return;
        }

        WebcamTextureManager textureManager = webcamManager.getTextureManager();
        Identifier textureId = textureManager.getTexture(entity.getUuid());

        if (textureId == null) {
            return;
        }

        Config config = ConfigManager.getConfig();
        if (entity.isInvisible() || (entity == MinecraftClient.getInstance().player && !config.renderInWorld)) {
            return;
        }

        byte[] frameData = PlayerWebcamManager.getInstance().getPlayerWebcam(entity.getUuid());
        if (frameData == null) {
            textureManager.removePlayer(entity.getUuid());
            return;
        }
        
        matrices.push();

        // --- Billboard logic DONT TOUCH ---
        Matrix4f initialModelMatrix = matrices.peek().getPositionMatrix();
        Quaternionf modelInverseRotation = new Quaternionf();
        modelInverseRotation.setFromUnnormalized(initialModelMatrix);
        modelInverseRotation.normalize(); // Sanitize to remove scaling
        modelInverseRotation.invert();    // Get the inverse

        // 2. Get the camera's rotation. We use the raw rotation for maximum compatibility.
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Quaternionf cameraRotation = camera.getRotation();

        // 3. Undo the model's rotation completely from the stack.
        matrices.multiply(modelInverseRotation);

        // 4. Now that we are in a clean, world-aligned space, we can translate.
        // We add the user-defined offset from the config to our base offset.
        float xOffset = (config.offsetX / 100.0f); // Base X is 0
        float yOffset = ((config.offsetY - 100) / 100.0f) - 0.8f; // Base Y is -100
        float zOffset = (config.offsetZ / 100.0f); // Z offset
        
        float finalY = entity.getHeight() + 0.3f + yOffset;
        if (entity.isFallFlying() || entity.isSwimming()) {
            finalY += 1.0f;
        }
        
        matrices.translate(xOffset, finalY, zOffset);

        // 5. Apply the camera's rotation to achieve the billboard effect.
        matrices.multiply(cameraRotation);

        // 6. Apply scale.
        float scale = config.worldScale;
        matrices.scale(scale, scale, scale);
        // --- End Billboard Logic ---

        // --- Start Tessellator rendering ---
        matrices.push();
        // The 180-degree rotation is no longer needed with the correct billboard logic.
        // matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));

        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(textureId));

        // Use our new renderer to draw the geometry
        // We pass the maximum light value to make it always appear bright.
        WebcamRenderer.renderInWorld(buffer, positionMatrix, entity.getUuid(), 1.0f, 1.0f, LightmapTextureManager.pack(15, 15));

        matrices.pop();
        // --- End Tessellator rendering ---

        matrices.pop();
    }
}
