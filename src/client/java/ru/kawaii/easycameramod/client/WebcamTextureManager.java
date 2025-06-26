package ru.kawaii.easycameramod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import ru.kawaii.easycameramod.EasyCameraMod;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebcamTextureManager {

    private final Map<UUID, NativeImageBackedTexture> textures = new ConcurrentHashMap<>();
    private final Map<UUID, Identifier> identifiers = new ConcurrentHashMap<>();
    private final ExecutorService decodingExecutor = Executors.newCachedThreadPool();
    private final MinecraftClient client = MinecraftClient.getInstance();

    public void updateTexture(UUID playerUuid, byte[] frameData) {
        if (frameData == null) {
            removePlayer(playerUuid);
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage bImage = ImageIO.read(new ByteArrayInputStream(frameData));
                if (bImage == null) {
                    throw new IOException("Failed to decode image.");
                }

                NativeImage image = new NativeImage(bImage.getWidth(), bImage.getHeight(), false);
                for (int y = 0; y < bImage.getHeight(); ++y) {
                    for (int x = 0; x < bImage.getWidth(); ++x) {
                        int rgb = bImage.getRGB(x, y);
                        image.setColor(x, y, rgb & 0xFF000000 | (rgb & 0x00FF0000) >> 16 | (rgb & 0x0000FF00) | (rgb & 0x000000FF) << 16);
                    }
                }
                return image;
            } catch (IOException e) {
                EasyCameraMod.LOGGER.error("Failed to read webcam frame for player {}", playerUuid, e);
                return null;
            }
        }, decodingExecutor).thenAcceptAsync(image -> {
            if (image == null) {
                removePlayer(playerUuid);
                return;
            }

            NativeImageBackedTexture texture = textures.get(playerUuid);
            if (texture != null && texture.getImage() != null && texture.getImage().getWidth() == image.getWidth() && texture.getImage().getHeight() == image.getHeight()) {
                texture.getImage().copyFrom(image);
                image.close();
                texture.upload();
            } else {
                if (texture != null) {
                    client.getTextureManager().destroyTexture(identifiers.get(playerUuid));
                }
                texture = new NativeImageBackedTexture(image);
                Identifier textureId = client.getTextureManager().registerDynamicTexture("easycameramod_webcam/" + playerUuid, texture);
                textures.put(playerUuid, texture);
                identifiers.put(playerUuid, textureId);
            }
        }, this.client);
    }

    public Identifier getTexture(UUID playerUuid) {
        return identifiers.get(playerUuid);
    }

    public void removePlayer(UUID playerUuid) {
        Identifier textureId = identifiers.remove(playerUuid);
        if (textureId != null) {
            client.execute(() -> client.getTextureManager().destroyTexture(textureId));
        }
        textures.remove(playerUuid);
    }

    public void clearAll() {
        identifiers.keySet().forEach(this::removePlayer);
    }
}
