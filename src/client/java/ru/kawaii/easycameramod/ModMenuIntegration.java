package ru.kawaii.easycameramod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import ru.kawaii.easycameramod.client.gui.MessageScreen;
import ru.kawaii.easycameramod.client.gui.SettingsScreen;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) {
                return new MessageScreen(
                        parent,
                        Text.translatable("gui.easycameramod.settings.title"),
                        Text.translatable("gui.easycameramod.settings.server_only_error")
                );
            } else {
                return new SettingsScreen(parent);
            }
        };
    }
} 