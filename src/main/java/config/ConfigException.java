package config;

public class ConfigException extends RuntimeException {

    // Creates config error
    public ConfigException(String message) {
        super(message);
    }

    // Creates config error
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}