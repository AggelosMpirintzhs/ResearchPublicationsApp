package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final String CONFIG_FILE = "application.properties";

    private final Properties properties;

    public AppConfig() {
        this.properties = loadProperties();
    }

    private Properties loadProperties() {
        Properties loadedProperties = new Properties();

        try (InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (inputStream == null) {
                throw new ConfigException("Δεν βρέθηκε το αρχείο configuration: " + CONFIG_FILE);
            }

            loadedProperties.load(inputStream);
            return loadedProperties;

        } catch (IOException exception) {
            throw new ConfigException("Σφάλμα κατά τη φόρτωση του configuration.", exception);
        }
    }

    public String getString(String key) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new ConfigException("Λείπει η ρύθμιση: " + key);
        }

        return value.trim();
    }

    public String getString(String key, String defaultValue) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }

    public int getInt(String key) {
        String value = getString(key);

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new ConfigException("Η ρύθμιση '" + key + "' πρέπει να είναι ακέραιος αριθμός.", exception);
        }
    }

    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new ConfigException("Η ρύθμιση '" + key + "' πρέπει να είναι ακέραιος αριθμός.", exception);
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value.trim());
    }

    public String getAppName() {
        return getString("app.name", "Thesis Query App");
    }

    public String getAppVersion() {
        return getString("app.version", "1.0.0");
    }
}