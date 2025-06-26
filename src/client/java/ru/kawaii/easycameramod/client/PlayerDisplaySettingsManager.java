package ru.kawaii.easycameramod.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.kawaii.easycameramod.config.PlayerDisplaySettings;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class PlayerDisplaySettingsManager {
    private static final PlayerDisplaySettingsManager INSTANCE = new PlayerDisplaySettingsManager();
    private final Map<UUID, PlayerDisplaySettings> settingsMap = new ConcurrentHashMap<>();
    private static final PlayerDisplaySettings DEFAULT_SETTINGS = PlayerDisplaySettings.createDefault();

    private PlayerDisplaySettingsManager() {}

    public static PlayerDisplaySettingsManager getInstance() {
        return INSTANCE;
    }

    public void updateSettings(UUID playerUuid, PlayerDisplaySettings settings) {
        settingsMap.put(playerUuid, settings);
    }

    public PlayerDisplaySettings getSettings(UUID playerUuid) {
        return settingsMap.getOrDefault(playerUuid, DEFAULT_SETTINGS);
    }

    public void removeSettings(UUID playerUuid) {
        settingsMap.remove(playerUuid);
    }

    public void clearAll() {
        settingsMap.clear();
    }
} 