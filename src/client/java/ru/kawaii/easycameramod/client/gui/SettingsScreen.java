package ru.kawaii.easycameramod.client.gui;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.option.Perspective;
import net.minecraft.text.Text;
import ru.kawaii.easycameramod.WebcamManager;
import ru.kawaii.easycameramod.config.Config;
import ru.kawaii.easycameramod.config.ConfigManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import ru.kawaii.easycameramod.networking.payload.C2SUpdateDisplaySettingsPayload;
import ru.kawaii.easycameramod.config.PlayerDisplaySettings;
import ru.kawaii.easycameramod.client.PlayerDisplaySettingsManager;
import ru.kawaii.easycameramod.client.gui.MessageScreen;
import ru.kawaii.easycameramod.client.gui.SettingsScreen;
import ru.kawaii.easycameramod.client.gui.WebcamMuteScreen;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class SettingsScreen extends Screen {
    private final Screen parent;
    private Perspective originalPerspective;
    private float originalYaw, originalPitch;
    private WebcamEntryList webcamEntryList;
    private CyclingButtonWidget<Boolean> visibleToggleButton;
    private CyclingButtonWidget<Config.CameraIndicator> cameraIndicatorButton;
    private SliderWidget cameraIndicatorScaleSlider;
    private CyclingButtonWidget<Config.DisplayMode> displayModeButton;
    private SliderWidget cropBoxXSlider, cropBoxYSlider, cropBoxSizeSlider;
    private SliderWidget cameraPositionXSlider, cameraPositionYSlider, cameraPositionZSlider, cameraScaleSlider;
    private ButtonWidget doneButton;

    public SettingsScreen(Screen parent) {
        super(Text.translatable("gui.easycameramod.settings.title"));
        this.parent = parent;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (this.client != null && this.client.player != null) {
            if (!this.webcamEntryList.canSwitch) {
                context.drawTextWithShadow(this.textRenderer, Text.translatable("gui.easycameramod.settings.opening_webcam"), this.width / 4 * 2, 50, -1);
            }
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("gui.easycameramod.settings.server_only_error"), this.width / 2, this.height / 2, 0xffff6767);
        }
        // Draw header for webcam list
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("gui.easycameramod.settings.select_webcam"), this.width / 8, 20, 0xFFFFFF);
    }

    @Override
    protected void init() {
        if (this.client != null && this.client.player != null) {
            this.originalPerspective = this.client.options.getPerspective();
            this.originalYaw = this.client.player.getYaw();
            this.originalPitch = this.client.player.getPitch();
            this.client.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
            float snappedYaw = Math.round(this.client.player.getYaw() / 90.0f) * 90.0f;
            this.client.player.setYaw(snappedYaw);
            this.client.player.setPitch(0);
            this.client.player.setBodyYaw(snappedYaw);
            this.client.player.setHeadYaw(snappedYaw);

            this.initWebcamList();
            this.initVisibleToggle();
            this.initCameraIndicatorButton();
            
            this.initDisplayModeControls();
            this.updateCropBoxControlsVisibility();
            this.updateIndicatorControlsVisibility();
            this.initCameraPositionControls();
            this.initDoneButton();
        } else {
            this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), (button) -> {
                if(this.client != null) this.client.setScreen(this.parent);
            }).dimensions(this.width / 2 - 100, this.height - 40, 200, 20).build());
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            if (this.client.player != null && this.originalPerspective != null) {
                this.client.options.setPerspective(this.originalPerspective);
                this.client.player.setYaw(this.originalYaw);
                this.client.player.setPitch(this.originalPitch);
                this.client.player.setBodyYaw(this.originalYaw);
            }
            if(ConfigManager.getConfig().webcamEnabled) WebcamManager.getInstance().start();
            else WebcamManager.getInstance().stop();

            ConfigManager.saveConfig();
            this.client.setScreen(parent);
        }
    }

    private void initDoneButton() {
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), (button) -> this.close())
                .dimensions(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    private void initWebcamList() {
        int listWidth = this.width / 4;
        this.webcamEntryList = new WebcamEntryList(this.client, listWidth, this.height - 80, 40, 20);
        this.webcamEntryList.onSelected((webcam) -> {
            try {
                WebcamManager.getInstance().selectWebcam(webcam);
            } catch (WebcamException e) {
                System.out.println(e.getMessage());
            }
            return null;
        });
        this.addDrawableChild(this.webcamEntryList);
    }

    private void initVisibleToggle() {
        int buttonWidth = 180;
        int rightSideX = this.width - buttonWidth - 10;
        int startY = 50;

        this.visibleToggleButton = CyclingButtonWidget.<Boolean>builder((visible) -> Text.translatable(visible ? "gui.on" : "gui.off"))
                .values(true, false)
                .initially(ConfigManager.getConfig().webcamEnabled)
                .build(rightSideX, startY, buttonWidth, 20, Text.translatable("gui.easycameramod.settings.webcam"), (button, value) -> {
                    ConfigManager.getConfig().webcamEnabled = value;
                    if (value) {
                        WebcamManager.getInstance().start();
                    } else {
                        WebcamManager.getInstance().stop();
                    }
                });
        this.addDrawableChild(this.visibleToggleButton);
    }

    private void initCameraIndicatorButton() {
        int buttonWidth = 180;
        int rightSideX = this.width - buttonWidth - 10;
        int y = 74;
        this.cameraIndicatorButton = CyclingButtonWidget.<Config.CameraIndicator>builder((indicator) -> Text.translatable("gui.easycameramod.settings.camera_indicator." + indicator.name().toLowerCase()))
                .values(Config.CameraIndicator.values())
                .initially(ConfigManager.getConfig().cameraIndicator)
                .build(rightSideX, y, buttonWidth, 20, Text.translatable("gui.easycameramod.settings.camera_indicator"), (button, value) -> {
                    ConfigManager.getConfig().cameraIndicator = value;
                    this.updateIndicatorControlsVisibility();
                });
        this.addDrawableChild(this.cameraIndicatorButton);

        this.cameraIndicatorScaleSlider = new ModSliderWidget(
                rightSideX, y + 22, buttonWidth, 20, "gui.easycameramod.settings.indicator_scale",
                ConfigManager.getConfig().cameraIndicatorScale, 1.0, 5.0, 0.1,
                (slider, value) -> {
                    ConfigManager.getConfig().cameraIndicatorScale = value.floatValue();
                    slider.setMessage(Text.translatable("gui.easycameramod.settings.indicator_scale_value", String.format("%.1f", value)));
                },
                null
        );
        this.addDrawableChild(this.cameraIndicatorScaleSlider);
    }

    private void initDisplayModeControls() {
        int buttonWidth = 180;
        int rightSideX = this.width - buttonWidth - 10;
        int startY = 122;

        this.displayModeButton = CyclingButtonWidget.<Config.DisplayMode>builder((mode) -> Text.translatable("gui.easycameramod.settings.display_mode." + mode.name().toLowerCase()))
                .values(Config.DisplayMode.values())
                .initially(ConfigManager.getConfig().displayMode)
                .tooltip((mode) -> Tooltip.of(Text.translatable("gui.easycameramod.settings.display_mode." + mode.name().toLowerCase() + ".tooltip")))
                .build(rightSideX, startY, buttonWidth, 20, Text.translatable("gui.easycameramod.settings.display_mode"), (button, mode) -> {
                    ConfigManager.getConfig().displayMode = mode;
                    this.updateCropBoxControlsVisibility();
                    this.updateLocalPreview();
                    this.sendSettingsToServer();
                });
        this.addDrawableChild(this.displayModeButton);

        int controlSpacing = 22;
        this.cropBoxXSlider = new ModSliderWidget(rightSideX, startY + controlSpacing, buttonWidth, 20, "gui.easycameramod.settings.crop_x", ConfigManager.getConfig().cropBoxX, 0, 1, 0.01,
                (slider, value) -> {
                    Config config = ConfigManager.getConfig();
                    float cropSize = config.cropBoxSize;
                    if (cropSize <= 0) return;

                    float min_offset = 0.5f / cropSize;
                    float max_offset = 1.0f - (0.5f / cropSize);
                    float clampedX = (float)Math.max(min_offset, Math.min(max_offset, value));

                    config.cropBoxX = clampedX;
                    slider.setMessage(Text.translatable("gui.easycameramod.settings.crop_x_value", String.format("%.2f", clampedX)));
                    this.updateLocalPreview();
                },
                this::sendSettingsToServer
        );
        this.cropBoxYSlider = new ModSliderWidget(rightSideX, startY + 2 * controlSpacing, buttonWidth, 20, "gui.easycameramod.settings.crop_y", ConfigManager.getConfig().cropBoxY, 0, 1, 0.01,
                (slider, value) -> {
                    Config config = ConfigManager.getConfig();
                    float cropSize = config.cropBoxSize;
                    if (cropSize <= 0) return;
                    
                    float min_offset = 0.5f / cropSize;
                    float max_offset = 1.0f - (0.5f / cropSize);
                    float clampedY = (float)Math.max(min_offset, Math.min(max_offset, value));

                    config.cropBoxY = clampedY;
                    slider.setMessage(Text.translatable("gui.easycameramod.settings.crop_y_value", String.format("%.2f", clampedY)));
                    this.updateLocalPreview();
                },
                this::sendSettingsToServer
        );

        // --- Crop Box Size Slider ---
        double initialSliderValue = ConfigManager.getConfig().cropBoxSize;
        this.cropBoxSizeSlider = new ModSliderWidget(
                rightSideX, startY + 3 * controlSpacing, buttonWidth, 20, "gui.easycameramod.settings.zoom",
                initialSliderValue, 1, 3.5, 0.05,
                (slider, value) -> {
                    Config config = ConfigManager.getConfig();
                    config.cropBoxSize = value.floatValue();
                    slider.setMessage(Text.translatable("gui.easycameramod.settings.zoom_value", String.format("%.2f", value)));

                    // Re-clamp X and Y offsets in case they are now out of bounds
                    float cropSize = config.cropBoxSize;
                    if (cropSize > 0) {
                        float min_offset = 0.5f / cropSize;
                        float max_offset = 1.0f - (0.5f / cropSize);
                        config.cropBoxX = Math.max(min_offset, Math.min(max_offset, config.cropBoxX));
                        config.cropBoxY = Math.max(min_offset, Math.min(max_offset, config.cropBoxY));
                    }
                    this.updateLocalPreview();
                },
                this::sendSettingsToServer
            );
            
        this.addDrawableChild(this.cropBoxXSlider);
        this.addDrawableChild(this.cropBoxYSlider);
        this.addDrawableChild(this.cropBoxSizeSlider);
    }

    private void initCameraPositionControls() {
        int buttonWidth = 180;
        int rightSideX = this.width - buttonWidth - 10;
        int startY = 220;
        int controlSpacing = 22;

        this.addDrawableChild(new TextWidget(rightSideX + buttonWidth/2 - (textRenderer.getWidth(Text.translatable("gui.easycameramod.settings.in_world_rendering"))/2), startY, textRenderer.getWidth(Text.translatable("gui.easycameramod.settings.in_world_rendering")), 10, Text.translatable("gui.easycameramod.settings.in_world_rendering"), textRenderer));
        startY += 12;

        this.cameraPositionXSlider = new ModSliderWidget(rightSideX, startY, buttonWidth, 20, "gui.easycameramod.settings.offset_x", ConfigManager.getConfig().offsetX, -50, 50, 1,
                (slider, value) -> {
                    ConfigManager.getConfig().offsetX=value.floatValue();
                    slider.setMessage(Text.translatable("gui.easycameramod.settings.offset_x_value", value / 100.0));
                }, null);
        this.cameraPositionYSlider = new ModSliderWidget(rightSideX, startY + controlSpacing, buttonWidth, 20, "gui.easycameramod.settings.offset_y", ConfigManager.getConfig().offsetY, -50, 250, 1.0,
                (slider, value) -> {
                    ConfigManager.getConfig().offsetY=value.floatValue();
                    slider.setMessage(Text.translatable("gui.easycameramod.settings.offset_y_value", (value - 100.0) / 100.0));
                }, null);
        this.cameraPositionZSlider = new ModSliderWidget(rightSideX, startY + 2*controlSpacing, buttonWidth, 20, "gui.easycameramod.settings.offset_z", ConfigManager.getConfig().offsetZ, -50, 50, 1.0,
                (slider, value) -> {
                    ConfigManager.getConfig().offsetZ=value.floatValue();
                    slider.setMessage(Text.translatable("gui.easycameramod.settings.offset_z_value", value / 100.0));
                }, null);
        this.cameraScaleSlider = new ModSliderWidget(rightSideX, startY + 3*controlSpacing, buttonWidth, 20, "gui.easycameramod.settings.scale", ConfigManager.getConfig().worldScale, 0.1, 2.0, 0.01,
                (slider, value) -> {
                    ConfigManager.getConfig().worldScale=value.floatValue();
                    slider.setMessage(Text.translatable("gui.easycameramod.settings.scale_value", value));
                }, null);

        this.addDrawableChild(this.cameraPositionXSlider);
        this.addDrawableChild(this.cameraPositionYSlider);
        this.addDrawableChild(this.cameraPositionZSlider);
        this.addDrawableChild(this.cameraScaleSlider);
    }

    private void updateCropBoxControlsVisibility() {
        boolean isVisible = ConfigManager.getConfig().displayMode == Config.DisplayMode.CROP_BOX;
        this.cropBoxXSlider.visible = isVisible;
        this.cropBoxYSlider.visible = isVisible;
        this.cropBoxSizeSlider.visible = isVisible;
    }

    private void updateIndicatorControlsVisibility() {
        this.cameraIndicatorScaleSlider.visible = ConfigManager.getConfig().cameraIndicator != Config.CameraIndicator.NONE;
    }

    private void sendSettingsToServer() {
        if (this.client != null && this.client.player != null && WebcamManager.getInstance().getCurrentWebcam() != null) {
            Config config = ConfigManager.getConfig();
            C2SUpdateDisplaySettingsPayload payload = new C2SUpdateDisplaySettingsPayload(
                    config.displayMode,
                    config.cropBoxX,
                    config.cropBoxY,
                    config.cropBoxSize
            );
            ClientPlayNetworking.send(payload);
        }
    }

    private void updateLocalPreview() {
        if (this.client != null && this.client.player != null) {
            Config config = ConfigManager.getConfig();
            PlayerDisplaySettings localSettings = new PlayerDisplaySettings(
                    config.displayMode,
                    config.cropBoxX,
                    config.cropBoxY,
                    config.cropBoxSize
            );
            PlayerDisplaySettingsManager.getInstance().updateSettings(this.client.player.getUuid(), localSettings);
        }
    }

    @Environment(EnvType.CLIENT)
    public class WebcamEntryList extends ElementListWidget<WebcamEntryList.WebcamEntry> {
        public boolean canSwitch = true;
        private Function<Webcam, ?> selectedCallback;

        public WebcamEntryList(MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
            this.refreshWebcams();
        }

        public void refreshWebcams() {
            this.clearEntries();
            this.addEntry(new WebcamEntry(null)); // "Not Selected" entry
            List<Webcam> webcams = WebcamManager.getInstance().getWebcams();
            for (Webcam webcam : webcams) {
                this.addEntry(new WebcamEntry(webcam));
            }

            String selectedName = ConfigManager.getConfig().selectedWebcam;
            for(WebcamEntry entry : children()){
                if((entry.webcam != null && entry.webcam.getName().equals(selectedName)) || (entry.webcam == null && selectedName.isEmpty())){
                    this.setSelected(entry);
                    break;
                }
            }
        }

        public void onSelected(Function<Webcam, ?> selectedCallback) {
            this.selectedCallback = selectedCallback;
        }

        @Environment(EnvType.CLIENT)
        public class WebcamEntry extends ElementListWidget.Entry<WebcamEntry> {
            public final Webcam webcam;
            private final Object webcamSwitchLock = new Object();

            public WebcamEntry(Webcam webcam) { this.webcam = webcam; }

            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                boolean isSelected = WebcamEntryList.this.getSelectedOrNull() == this;

                if (isSelected) {
                    // "Concave" effect for the selected item. A dark background gives an inset feel.
                    context.fill(x, y, x + entryWidth, y + entryHeight, 0xFF3A3A3A);
                } else if (hovered) {
                    // A lighter gray for hover effect
                    context.fill(x, y, x + entryWidth, y + entryHeight, 0x40FFFFFF);
                }

                String textToDraw = this.webcam != null ? this.webcam.getName() : Text.translatable("gui.easycameramod.settings.not_selected").getString();
                if (textToDraw.length() > 31) {
                    textToDraw = textToDraw.substring(0, Math.min(textToDraw.length(), 31)) + "...";
                }
                Text text = Text.of(textToDraw);
                
                int color = 0xFFFFFF; // White text is readable on dark/hovered backgrounds
                if (!WebcamEntryList.this.canSwitch || !SettingsScreen.this.visibleToggleButton.getValue()) {
                    color = 0xAAAAAA; // Gray out if disabled
                }

                // Center the text
                context.drawCenteredTextWithShadow(SettingsScreen.this.textRenderer, text, x + entryWidth / 2, y + (entryHeight - 8) / 2, color);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if(button == 0) {
                    synchronized(this.webcamSwitchLock) {
                        if (WebcamEntryList.this.canSwitch && this != WebcamEntryList.this.getSelectedOrNull()) {
                           WebcamEntryList.this.canSwitch = false;
                           WebcamEntryList.this.setSelected(this);
                            (new Thread(() -> {
                                try {
                                    WebcamManager.getInstance().selectWebcam(this.webcam);
                                    if (this.webcam == null) {
                                        WebcamManager.getInstance().stop();
                                    } else if (ConfigManager.getConfig().webcamEnabled) {
                                        WebcamManager.getInstance().start();
                                    }
                                } finally {
                                    WebcamEntryList.this.canSwitch = true;
                                }
                            })).start();
                        }
                    }
                    return true;
                }
                return super.mouseClicked(mouseX,mouseY,button);
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return Collections.emptyList();
            }

            @Override
            public List<? extends Element> children() {
                return Collections.emptyList();
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public static class ModSliderWidget extends SliderWidget {
        private final double min, max, step;
        private final BiConsumer<ModSliderWidget, Double> onUpdate;
        private final Runnable onRelease;

        public ModSliderWidget(int x, int y, int width, int height, String key, double current, double min, double max, double step, BiConsumer<ModSliderWidget, Double> onUpdate, Runnable onRelease) {
            super(x, y, width, height, Text.translatable(key), (current - min) / (max - min));
            this.min = min;
            this.max = max;
            this.step = step;
            this.onUpdate = onUpdate;
            this.onRelease = onRelease;
            this.applyValue(); // Set initial snapped value and text
        }

        @Override
        protected void updateMessage() {
            // This is called before applyValue, so we let applyValue handle the logic
            // to prevent doing it twice per event.
        }

        @Override
        protected void applyValue() {
            double rawActualValue = this.min + (this.max - this.min) * this.value;
            
            // Round to the specified step.
            double steppedValue;
            if (this.step > 0) {
                 steppedValue = Math.round(rawActualValue / this.step) * this.step;
                 // Use BigDecimal to clean up floating point representation issues after stepping
                 java.math.BigDecimal bd = new java.math.BigDecimal(steppedValue);
                 bd = bd.setScale(10, java.math.RoundingMode.HALF_UP);
                 steppedValue = bd.doubleValue();
            } else {
                 steppedValue = rawActualValue;
            }

            // Clamp value to prevent floating point errors from going out of bounds
            steppedValue = Math.max(this.min, Math.min(this.max, steppedValue));

            // Snap the slider handle to the new stepped value
            this.value = (steppedValue - this.min) / (this.max - this.min);

            // Now, call the update logic with the final, stepped value.
            this.onUpdate.accept(this, steppedValue);
        }
        
        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (this.onRelease != null) {
                this.onRelease.run();
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Do nothing to keep the screen transparent
    }
} 