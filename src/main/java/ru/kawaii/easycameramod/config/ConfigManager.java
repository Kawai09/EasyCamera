package ru.kawaii.easycameramod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ru.kawaii.easycameramod.EasyCameraMod;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class ConfigManager {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(EasyCameraMod.MOD_ID + ".json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Config config;

    public static void loadConfig() {
        try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
            config = GSON.fromJson(reader, Config.class);
            if (config == null) {
                config = new Config();
            }
        } catch (IOException e) {
            config = new Config();
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            EasyCameraMod.LOGGER.error("Failed to save config", e);
        }
    }

    public static Config getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }
} 