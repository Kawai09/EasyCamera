package ru.kawaii.easycameramod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.kawaii.easycameramod.client.gui.MessageScreen;
import ru.kawaii.easycameramod.client.gui.SettingsScreen;
import ru.kawaii.easycameramod.client.gui.WebcamMuteScreen;

public class KeyBinds {

    private static KeyBinding settingsKey;
    private static KeyBinding muteKey;

    public static void register() {
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.easycameramod.settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_PERIOD,
                "category.easycameramod.main"
        ));

        muteKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.easycameramod.mute_screen",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_COMMA,
                "category.easycameramod.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (settingsKey.wasPressed()) {
                if (client.world == null) {
                    client.setScreen(new MessageScreen(
                            client.currentScreen,
                            Text.translatable("gui.easycameramod.settings.title"),
                            Text.translatable("gui.easycameramod.settings.server_only_error")
                    ));
                } else {
                    client.setScreen(new SettingsScreen(client.currentScreen));
                }
            }

            while (muteKey.wasPressed()) {
                if (client.world != null) {
                    client.setScreen(new WebcamMuteScreen(client.currentScreen));
                }
            }
        });
    }
} 