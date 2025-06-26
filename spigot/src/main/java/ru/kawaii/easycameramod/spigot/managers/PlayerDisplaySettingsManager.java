package ru.kawaii.easycameramod.spigot.managers;

import ru.kawaii.easycameramod.spigot.config.PlayerDisplaySettings;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDisplaySettingsManager {
    private final Map<UUID, PlayerDisplaySettings> settingsMap = new ConcurrentHashMap<>();

    public void updateSettings(UUID playerUuid, PlayerDisplaySettings settings) {
        settingsMap.put(playerUuid, settings);
    }

    public PlayerDisplaySettings getSettings(UUID playerUuid) {
        return settingsMap.get(playerUuid);
    }

    public Map<UUID, PlayerDisplaySettings> getAllSettings() {
        return settingsMap;
    }

    public void removeSettings(UUID playerUuid) {
        settingsMap.remove(playerUuid);
    }
} 