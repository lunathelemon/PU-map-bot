package io.github.profjb58.pumapbot.config;

import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigFile {

    private final File file;
    private final Properties defaults;
    private Properties properties;
    private final String headerComments;

    ConfigFile(String title, Properties defaults, String headerComments, String directory) {
        this.defaults = defaults;
        this.headerComments = headerComments;

        this.file = new File(directory + "/" + title + ".properties");

        try {
            var configPath = Path.of(directory);
            if(Files.notExists(configPath)) { // Create the main config directory if it doesn't exist
                Files.createDirectory(configPath);
            }

            if(file.createNewFile()) { // Create file & set to defaults if it doesn't exist
                setToDefaults();
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        load();
    }

    private void save() {
        try {
            OutputStream output = new FileOutputStream(file);
            properties.store(output, headerComments);
            output.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void load() {
        try {
            if(file.createNewFile()) { // If no file currently exists
                setToDefaults();
            } else {
                InputStream input = new FileInputStream(file);
                properties = new Properties();
                properties.load(input);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public void setLongProperty(String key, long value) {
        properties.setProperty(key, String.valueOf(value));
    }

    public void removeProperty(String key) {
        properties.remove(key);
    }

    @Nullable
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public long getLongProperty(String key, boolean unsigned) {
        String property = properties.getProperty(key);
        if(property != null) {
            try {
                long value = Long.parseLong(property);
                if(!unsigned || value > 0) return value;
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    @Nullable
    public <T extends Enum<T>> T getEnumProperty(String key, Class<T> enumType) {
        String property = getProperty(key);
        if(property != null)
            for (T enumValue : enumType.getEnumConstants())
                if(property.equals(enumValue.name().toLowerCase()))
                    return enumValue;
        return null;
    }

    public boolean getBooleanProperty(String key) {
        String property = getProperty(key);
        if(property != null)
            return Boolean.parseBoolean(property);
        return false;
    }

    public synchronized void update() {
        save();
        load();
    }

    public void setToDefaults() {
        properties = defaults;
    }
}
