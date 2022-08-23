package io.github.profjb58.pumapbot.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.crypto.Data;
import java.util.EnumMap;
import java.util.Properties;

public class ConfigHandler {

    // Config directory
    private static final String MAIN_DIRECTORY = System.getProperty("user.dir") + "/resources";
    private static ConfigFile settingsConfig;

    public ConfigHandler() {
        Properties defaults = new Properties();
        defaults.setProperty("debug_mode", "true");
        settingsConfig = new ConfigFile("settings", defaults, "Project University (v1) Map Bot Settings", MAIN_DIRECTORY);
    }

    public ConfigFile getSettingsConfig() {
       return settingsConfig;
    }
}
