package ru.kawaii.easycameramod.spigot;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import ru.kawaii.easycameramod.spigot.config.DisplayMode;
import ru.kawaii.easycameramod.spigot.config.PlayerDisplaySettings;
import ru.kawaii.easycameramod.spigot.lib.bstats.Metrics;
import ru.kawaii.easycameramod.spigot.managers.PlayerDisplaySettingsManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EasyCameraModSpigot extends JavaPlugin implements PluginMessageListener, Listener {
    private static final String C2S_WEBCAM_CHANNEL = "easycameramod:c2s_webcam_data";
    private static final String S2C_WEBCAM_CHANNEL = "easycameramod:s2c_webcam_data";
    private static final String S2C_REMOVE_CHANNEL = "easycameramod:s2c_remove_webcam";
    private static final String C2S_SETTINGS_CHANNEL = "easycameramod:c2s_update_display_settings";
    private static final String S2C_SETTINGS_CHANNEL = "easycameramod:s2c_display_settings";
    private static final long FRAME_INTERVAL_MS = 1000 / 30;

    private final PlayerDisplaySettingsManager settingsManager = new PlayerDisplaySettingsManager();
    private final Map<UUID, Long> lastPacketTimestamps = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        getServer().getMessenger().registerIncomingPluginChannel(this, C2S_WEBCAM_CHANNEL, this);
        getServer().getMessenger().registerIncomingPluginChannel(this, C2S_SETTINGS_CHANNEL, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, S2C_WEBCAM_CHANNEL);
        getServer().getMessenger().registerOutgoingPluginChannel(this, S2C_REMOVE_CHANNEL);
        getServer().getMessenger().registerOutgoingPluginChannel(this, S2C_SETTINGS_CHANNEL);
        getServer().getPluginManager().registerEvents(this, this);

        // Initialize bStats
        int pluginId = 26285;
        new Metrics(this, pluginId);

        getLogger().info("EasyCameraMod-Spigot has been enabled!");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getLogger().info("EasyCameraMod-Spigot has been disabled!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player newPlayer = event.getPlayer();
        UUID newPlayerUuid = newPlayer.getUniqueId();

        // Send all existing players' settings to the new player
        for (Map.Entry<UUID, PlayerDisplaySettings> entry : settingsManager.getAllSettings().entrySet()) {
            sendSettingsToPlayer(newPlayer, entry.getKey(), entry.getValue());
        }
        
        // Add default settings for the new player and broadcast them
        PlayerDisplaySettings defaultSettings = PlayerDisplaySettings.createDefault();
        settingsManager.updateSettings(newPlayerUuid, defaultSettings);
        broadcastSettings(newPlayerUuid, defaultSettings, null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player quittingPlayer = event.getPlayer();
        settingsManager.removeSettings(quittingPlayer.getUniqueId());
        lastPacketTimestamps.remove(quittingPlayer.getUniqueId());
        
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeLong(quittingPlayer.getUniqueId().getMostSignificantBits());
        out.writeLong(quittingPlayer.getUniqueId().getLeastSignificantBits());
        byte[] message = out.toByteArray();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendPluginMessage(this, S2C_REMOVE_CHANNEL, message);
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player sender, byte[] message) {
        if (C2S_WEBCAM_CHANNEL.equals(channel)) {
            handleWebcamData(sender, message);
        } else if (C2S_SETTINGS_CHANNEL.equals(channel)) {
            handleSettingsUpdate(sender, message);
        }
    }

    private void handleWebcamData(Player sender, byte[] message) {
        long currentTime = System.currentTimeMillis();
        long lastTime = this.lastPacketTimestamps.getOrDefault(sender.getUniqueId(), 0L);
        if (currentTime - lastTime < FRAME_INTERVAL_MS) {
            return; // Drop packet
        }
        this.lastPacketTimestamps.put(sender.getUniqueId(), currentTime);
        
        if (message.length == 0) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeLong(sender.getUniqueId().getMostSignificantBits());
            out.writeLong(sender.getUniqueId().getLeastSignificantBits());
            byte[] removeMessage = out.toByteArray();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.getUniqueId().equals(sender.getUniqueId())) {
                    onlinePlayer.sendPluginMessage(this, S2C_REMOVE_CHANNEL, removeMessage);
                }
            }
            return;
        }
        
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeLong(sender.getUniqueId().getMostSignificantBits());
        out.writeLong(sender.getUniqueId().getLeastSignificantBits());
        writeVarInt(out, message.length);
        out.write(message);
        byte[] s2cMessage = out.toByteArray();

        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (!recipient.getUniqueId().equals(sender.getUniqueId())) {
                recipient.sendPluginMessage(this, S2C_WEBCAM_CHANNEL, s2cMessage);
            }
        }
    }
    
    private void handleSettingsUpdate(Player sender, byte[] rawMessage) {
        ByteArrayDataInput in = ByteStreams.newDataInput(rawMessage);
        try {
            DisplayMode displayMode = DisplayMode.values()[readVarInt(in)];
            float cropBoxX = in.readFloat();
            float cropBoxY = in.readFloat();
            float cropBoxSize = in.readFloat();

            PlayerDisplaySettings newSettings = new PlayerDisplaySettings(displayMode, cropBoxX, cropBoxY, cropBoxSize);
            settingsManager.updateSettings(sender.getUniqueId(), newSettings);
            
            // Broadcast the updated settings to all other players
            broadcastSettings(sender.getUniqueId(), newSettings, sender);
        } catch (Exception e) {
            getLogger().warning("Failed to parse display settings packet from " + sender.getName() + ": " + e.getMessage());
        }
    }

    private void broadcastSettings(UUID fromPlayer, PlayerDisplaySettings settings, Player excludePlayer) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (excludePlayer != null && onlinePlayer.getUniqueId().equals(excludePlayer.getUniqueId())) {
                continue; // Don't send settings back to the sender
            }
            sendSettingsToPlayer(onlinePlayer, fromPlayer, settings);
        }
    }
    
    private void sendSettingsToPlayer(Player recipient, UUID subject, PlayerDisplaySettings settings) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeLong(subject.getMostSignificantBits());
        out.writeLong(subject.getLeastSignificantBits());
        writeVarInt(out, settings.displayMode().ordinal());
        out.writeFloat(settings.cropBoxX());
        out.writeFloat(settings.cropBoxY());
        out.writeFloat(settings.cropBoxSize());
        
        recipient.sendPluginMessage(this, S2C_SETTINGS_CHANNEL, out.toByteArray());
    }

    private int readVarInt(ByteArrayDataInput in) {
        int i = 0;
        int j = 0;
        while (true) {
            byte b0 = in.readByte();
            i |= (b0 & 0x7F) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt is too big");
            }
            if ((b0 & 0x80) != 128) {
                break;
            }
        }
        return i;
    }

    private void writeVarInt(ByteArrayDataOutput out, int value) {
        int localValue = value;
        while ((localValue & ~0x7F) != 0) {
            out.writeByte((localValue & 0x7F) | 0x80);
            localValue >>>= 7;
        }
        out.writeByte(localValue);
    }
} 