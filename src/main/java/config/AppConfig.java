package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final String CONFIG_FILE = "application.properties";

    private final Properties properties;

    // Creates app config
    public AppConfig() {
        this.properties = loadProperties();
    }

    // Loads config file
    private Properties loadProperties() {
        Properties loadedProperties = new Properties();

        try (InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (inputStream == null) {
                throw new ConfigException("Configuration file not found: " + CONFIG_FILE);
            }

            loadedProperties.load(inputStream);
            return loadedProperties;

        } catch (IOException exception) {
            throw new ConfigException("Error loading configuration.", exception);
        }
    }

    // Gets string value
    public String getString(String key) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new ConfigException("Missing setting: " + key);
        }

        return value.trim();
    }

    // Gets string default
    public String getString(String key, String defaultValue) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }

    // Gets integer value
    public int getInt(String key) {
        String value = getString(key);

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new ConfigException("Setting '" + key + "' must be an integer.", exception);
        }
    }

    // Gets integer default
    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new ConfigException("Setting '" + key + "' must be an integer.", exception);
        }
    }

    // Gets boolean default
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value.trim());
    }

    // Gets app name
    public String getAppName() {
        return getString("app.name", "Thesis Query App");
    }

    // Gets app version
    public String getAppVersion() {
        return getString("app.version", "1.0.0");
    }
}