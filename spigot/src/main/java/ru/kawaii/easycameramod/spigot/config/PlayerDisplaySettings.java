package ru.kawaii.easycameramod.spigot.config;

public record PlayerDisplaySettings(
    DisplayMode displayMode,
    float cropBoxX,
    float cropBoxY,
    float cropBoxSize
) {
    public static PlayerDisplaySettings createDefault() {
        return new PlayerDisplaySettings(DisplayMode.STRETCH_FILL, 0.5f, 0.5f, 1.0f);
    }
} 