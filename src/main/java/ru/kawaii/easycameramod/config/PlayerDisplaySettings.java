package ru.kawaii.easycameramod.config;

public record PlayerDisplaySettings(
    Config.DisplayMode displayMode,
    float cropBoxX,
    float cropBoxY,
    float cropBoxSize
) {
    /**
     * Provides a default set of settings for players whose settings haven't been received yet.
     */
    public static PlayerDisplaySettings createDefault() {
        return new PlayerDisplaySettings(Config.DisplayMode.STRETCH_FILL, 0.5f, 0.5f, 1.0f);
    }
} 