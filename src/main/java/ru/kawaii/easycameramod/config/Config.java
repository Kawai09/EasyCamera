package ru.kawaii.easycameramod.config;

import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

@Environment(EnvType.CLIENT)
public class Config {
    public boolean webcamEnabled = true;
    public String selectedWebcam = "";

    public DisplayMode displayMode = DisplayMode.CROP_BOX;
    public CameraIndicator cameraIndicator = CameraIndicator.BOTTOM_RIGHT;
    public float cameraIndicatorScale = 2.0f;

    public float cropBoxX = 0.5f;
    public float cropBoxY = 0.5f;
    public float cropBoxSize = 2.5f;

    public boolean renderInWorld = true;
    public float worldScale = 1.0f;
    public float offsetX = 0.0f;
    public float offsetY = 100.0f;
    public float offsetZ = 0.0f;
    public boolean cameraPositionFaced = true;

    public boolean renderOnScreen = true;

    public enum DisplayMode {
        STRETCH_FILL,
        CROP_BOX
    }

    public enum CameraIndicator {
        NONE,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
} 