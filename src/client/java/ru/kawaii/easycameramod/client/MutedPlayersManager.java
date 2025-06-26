package ru.kawaii.easycameramod.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MutedPlayersManager {
    private static final Path MUTED_PLAYERS_PATH = FabricLoader.getInstance().getConfigDir().resolve("easycameramod_muted.txt");
    private static Set<UUID> mutedPlayers = new HashSet<>();

    public static void loadMutedPlayers() {
        Set<UUID> set = new HashSet<>();
        if (Files.exists(MUTED_PLAYERS_PATH)) {
            try {
                for (String line : Files.readAllLines(MUTED_PLAYERS_PATH)) {
                    try {
                        set.add(UUID.fromString(line.trim()));
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (IOException ignored) {}
        }
        mutedPlayers = set;
    }

    public static void saveMutedPlayers() {
        try {
            Files.createDirectories(MUTED_PLAYERS_PATH.getParent());
            Files.write(MUTED_PLAYERS_PATH, mutedPlayers.stream().map(UUID::toString).collect(Collectors.toList()));
        } catch (IOException ignored) {}
    }

    public static boolean isMuted(UUID uuid) {
        return mutedPlayers.contains(uuid);
    }

    public static void mute(UUID uuid) {
        mutedPlayers.add(uuid);
        saveMutedPlayers();
    }

    public static void unmute(UUID uuid) {
        mutedPlayers.remove(uuid);
        saveMutedPlayers();
    }
    
    public static Set<UUID> getMutedPlayers() {
        return mutedPlayers;
    }
} 