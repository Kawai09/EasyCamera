package ru.kawaii.easycameramod.client.renderer;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;
import ru.kawaii.easycameramod.client.PlayerDisplaySettingsManager;
import ru.kawaii.easycameramod.config.Config;
import net.minecraft.client.render.OverlayTexture;

import java.util.UUID;

public class WebcamRenderer {

    private static final int CIRCLE_SEGMENTS = 64;

    public static void render(VertexConsumer buffer, Matrix4f positionMatrix, UUID playerUuid, float width, float height) {
        // This is the HUD renderer. It was reported as being rotated 180 degrees,
        // which means both U and V axes were flipped from the desired state.
        // The "correct" mapping for it seems to be (+, +).
        renderCircle(buffer, positionMatrix, playerUuid, width, height, (u, v, x, y) -> {
            buffer.vertex(positionMatrix, x, y, 0).texture(u, v);
        }, false); // isWorld = false
    }

    public static void renderInWorld(VertexConsumer buffer, Matrix4f positionMatrix, UUID playerUuid, float width, float height, int light) {
        // This is the in-world renderer. It was reported as being mirrored,
        // which means one axis was flipped. We determined the correct mapping for it
        // is (+, -) to account for the world's coordinate system.
        renderCircle(buffer, positionMatrix, playerUuid, width, height, (u, v, x, y) -> {
            buffer.vertex(positionMatrix, x, y, 0).color(255, 255, 255, 255).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0.0f, 0.0f, 1.0f);
        }, true); // isWorld = true
    }

    @FunctionalInterface
    private interface VertexDrawer {
        void draw(float u, float v, float x, float y);
    }

    private static void renderCircle(VertexConsumer buffer, Matrix4f positionMatrix, UUID playerUuid, float width, float height, VertexDrawer drawer, boolean isWorld) {
        var playerSettings = PlayerDisplaySettingsManager.getInstance().getSettings(playerUuid);
        float radius = width / 2.0f;

        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            float angle1 = (float)i / CIRCLE_SEGMENTS * 2.0f * (float)Math.PI;
            float angle2 = (float)(i + 1) / CIRCLE_SEGMENTS * 2.0f * (float)Math.PI;

            float cos1 = (float)Math.cos(angle1);
            float sin1 = (float)Math.sin(angle1);
            float cos2 = (float)Math.cos(angle2);
            float sin2 = (float)Math.sin(angle2);

            float x1 = radius * cos1;
            float y1 = radius * sin1;
            float x2 = radius * cos2;
            float y2 = radius * sin2;

            float u_cos1, v_sin1, u_cos2, v_sin2;

            if (isWorld) {
                // In-world needs (+, -)
                u_cos1 = 0.5f + 0.5f * cos1;
                v_sin1 = 0.5f - 0.5f * sin1;
                u_cos2 = 0.5f + 0.5f * cos2;
                v_sin2 = 0.5f - 0.5f * sin2;
            } else {
                // HUD needs (+, +) to be correct
                u_cos1 = 0.5f + 0.5f * cos1;
                v_sin1 = 0.5f + 0.5f * sin1;
                u_cos2 = 0.5f + 0.5f * cos2;
                v_sin2 = 0.5f + 0.5f * sin2;
            }

            float base_u0 = 0.5f, base_v0 = 0.5f;
            float base_u1 = u_cos1, base_v1 = v_sin1;
            float base_u2 = u_cos2, base_v2 = v_sin2;

            float u0, v0, u1, v1, u2, v2;

            if (playerSettings.displayMode() == Config.DisplayMode.CROP_BOX) {
                float cropX = playerSettings.cropBoxX();
                float cropY = playerSettings.cropBoxY();
                float cropSize = playerSettings.cropBoxSize();
                if (cropSize == 0) cropSize = 1.0f;

                u0 = (base_u0 - 0.5f) / cropSize + cropX;
                v0 = (base_v0 - 0.5f) / cropSize + cropY;
                u1 = (base_u1 - 0.5f) / cropSize + cropX;
                v1 = (base_v1 - 0.5f) / cropSize + cropY;
                u2 = (base_u2 - 0.5f) / cropSize + cropX;
                v2 = (base_v2 - 0.5f) / cropSize + cropY;
            } else { // STRETCH_FILL
                u0 = base_u0; v0 = base_v0;
                u1 = base_u1; v1 = base_v1;
                u2 = base_u2; v2 = base_v2;
            }

            drawer.draw(u0, v0, 0, 0);
            drawer.draw(u1, v1, x1, y1);
            drawer.draw(u2, v2, x2, y2);
            drawer.draw(u2, v2, x2, y2);
        }
    }
} 