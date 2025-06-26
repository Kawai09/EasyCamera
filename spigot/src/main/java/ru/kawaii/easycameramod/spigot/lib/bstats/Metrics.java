package ru.kawaii.easycameramod.spigot.lib.bstats;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

/**
 * bStats collects some data for plugin authors.
 *
 * <p>To get started, feel free to copy this class into your plugin.
 *
 * <p><b>Authors:</b> bStats, ...
 *
 * <p><b>Slightly modified by me</b>
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Metrics {

    static {
        // You can use the property to disable the check in your test environment
        if (System.getProperty("bstats.relocatecheck") == null || !System.getProperty("bstats.relocatecheck").equals("false")) {
            // Maven's Relocate is clever and changes strings, too. So we have to use this little "trick" ... :D
            final String defaultPackage = new String(
                    new byte[]{'o', 'r', 'g', '.', 'b', 's', 't', 'a', 't', 's', '.', 'b', 'u', 'k', 'k', 'i', 't'});
            final String examplePackage = new String(new byte[]{'y', 'o', 'u', 'r', '.', 'p', 'a', 'c', 'k', 'a', 'g', 'e'});
            // We disable relocation checks if you use the default package
            if (Metrics.class.getPackage().getName().equals(defaultPackage) || Metrics.class.getPackage().getName().equals(examplePackage)) {
                throw new IllegalStateException("bStats Metrics class has not been relocated correctly!");
            }
        }
    }

    // The version of this bStats class
    public static final int B_STATS_VERSION = 2;

    // The url to which the data is sent
    private static final String URL = "https://bStats.org/submitData/bukkit";

    // A list with all custom charts
    private final List<CustomChart> charts = new java.util.ArrayList<>();

    // The plugin
    private final Plugin plugin;

    /**
     * Class constructor.
     *
     * @param plugin The plugin which stats should be measured.
     * @param serviceId The id of the plugin.
     *                  It can be found at <a href="https://bstats.org/what-is-my-plugin-id">What is my plugin id?</a>
     */
    public Metrics(JavaPlugin plugin, int serviceId) {
        this.plugin = plugin;

        // Get the config file
        File bStatsFolder = new File(plugin.getDataFolder().getParentFile(), "bStats");
        File configFile = new File(bStatsFolder, "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Check if the config file exists
        if (!config.isSet("serverUuid")) {
            config.addDefault("enabled", true);
            config.addDefault("serverUuid", UUID.randomUUID().toString());
            config.addDefault("logFailedRequests", false);
            config.addDefault("logSentData", false);
            config.addDefault("logResponseStatusText", false);

            // Inform the server owners about bStats
            config.options().setHeader(
                    List.of(
                            "bStats (https://bStats.org) collects some basic information for plugin authors, like how",
                            "many people use their plugin and their server versions. It's recommended to keep bStats",
                            "enabled, but if you're not comfortable with this, you can turn this setting off. There",
                            "is no performance penalty associated with having metrics enabled, and data sent to bStats",
                            "is anonymous."
                    )
            ).copyDefaults(true);
            try {
                config.save(configFile);
            } catch (IOException ignored) {
            }
        }

        // We are not allowed to send data about this server :(
        if (!config.getBoolean("enabled", true)) {
            return;
        }

        // Get the server-id
        String serverUUID = config.getString("serverUuid");
        boolean logErrors = config.getBoolean("logFailedRequests", false);
        boolean logSentData = config.getBoolean("logSentData", false);
        boolean logResponseStatusText = config.getBoolean("logResponseStatusText", false);


        // And we need a different name...
        // We will use the plugin name as fallback if the name is not defined in the plugin.yml
        String pluginName = plugin.getDescription().getName();
        // The plugin version as fallback if the version is not defined in the plugin.yml
        String pluginVersion = plugin.getDescription().getVersion();


        new Timer(true).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // The data collection is async, as well as sending the data
                // This prevents issues with the server thread
                submitData(serviceId, serverUUID, logErrors, logSentData, logResponseStatusText, pluginName, pluginVersion);
            }
        }, 1000 * 60 * 5, 1000 * 60 * 30);
    }

    /**
     * Adds a custom chart.
     *
     * @param chart The chart to add.
     */
    public void addCustomChart(CustomChart chart) {
        this.charts.add(chart);
    }

    private void submitData(int serviceId, String serverUUID, boolean logErrors, boolean logSentData, boolean logResponseStatusText, String pluginName, String pluginVersion) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // Should be executed async
            try {
                // WARNING: Removing the exception handling may break your plugin!
                // SpigotScheduler and the BukkitScheduler will catch the exception and log it automatically.
                // We create a new connection
                HttpsURLConnection connection = (HttpsURLConnection) new URL(URL).openConnection();
                byte[] compressedData = compress(getPluginData(pluginName, pluginVersion).toString());

                // Add headers
                connection.setRequestMethod("POST");
                connection.addRequestProperty("Accept", "application/json");
                connection.addRequestProperty("Connection", "close");
                connection.addRequestProperty("Content-Encoding", "gzip");
                connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "bStats-Metrics/" + B_STATS_VERSION);
                connection.addRequestProperty("X-Service-Id", String.valueOf(serviceId));
                connection.addRequestProperty("X-Server-Id", serverUUID);

                connection.setDoOutput(true);
                try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                    outputStream.write(compressedData);
                }

                StringBuilder builder = new StringBuilder();
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        builder.append(line);
                    }
                }

                if (logSentData) {
                    plugin.getLogger().info("Sent data to bStats: " + builder);
                }
                if (logResponseStatusText) {
                    plugin.getLogger().info("Sent data to bStats and received response: " + builder);
                }

            } catch (Exception e) {
                if (logErrors) {
                    plugin.getLogger().log(Level.WARNING, "Could not submit stats of " + plugin.getName(), e);
                }
            }
        });
    }


    /**
     * Gets the plugin data of the plugin.
     *
     * @return The plugin data.
     */
    private JsonObject getPluginData(String pluginName, String pluginVersion) {
        JsonObject data = new JsonObject();

        data.addProperty("pluginName", pluginName);
        data.addProperty("pluginVersion", pluginVersion);
        data.add("customCharts", getCustomChartsData());

        return data;
    }

    /**
     * Gets the server data.
     *
     * @return The server data.
     */
    private JsonObject getCustomChartsData() {
        JsonObject data = new JsonObject();
        for (CustomChart customChart : charts) {
            // We skip custom charts with null values
            if (customChart.getRequestJsonObject(plugin.getLogger()) == null) {
                continue;
            }
            data.add(customChart.getChartId(), customChart.getRequestJsonObject(plugin.getLogger()));
        }
        return data;
    }


    /**
     * Gzips the given string.
     *
     * @param str The string to gzip.
     * @return The gzipped string.
     */
    private static byte[] compress(final String str) throws IOException {
        if (str == null) {
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(outputStream)) {
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
        }
        return outputStream.toByteArray();
    }


    /**
     * Represents a custom chart.
     */
    public static abstract class CustomChart {

        // The id of the chart
        private final String chartId;

        /**
         * Class constructor.
         *
         * @param chartId The id of the chart.
         */
        protected CustomChart(String chartId) {
            // Chart ids should be lowercase
            if (chartId == null) {
                throw new IllegalArgumentException("chartId must not be null");
            }
            this.chartId = chartId;
        }

        /**
         * Gets the chart id.
         *
         * @return The chart id.
         */
        public String getChartId() {
            return chartId;
        }

        protected abstract JsonObject getRequestJsonObject(java.util.logging.Logger logger);
    }

    /**
     * Represents a simple pie chart.
     */
    public static class SimplePie extends CustomChart {

        private final Callable<String> callable;

        /**
         * Class constructor.
         *
         * @param chartId The id of the chart.
         * @param callable The callable which is used to request the value for this chart.
         *                 This callable is called once every 30 minutes.
         */
        public SimplePie(String chartId, Callable<String> callable) {
            super(chartId);
            this.callable = callable;
        }

        @Override
        protected JsonObject getRequestJsonObject(java.util.logging.Logger logger) {
            JsonObject chart = new JsonObject();
            try {
                String value = callable.call();
                if (value == null || value.isEmpty()) {
                    // We don't want to send null values
                    return null;
                }
                chart.addProperty("value", value);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to get value for custom chart with id " + getChartId(), e);
                return null;
            }
            return chart;
        }
    }


} 