package config;

public record DatabaseConfig(
        String url,
        String username,
        String password,
        String driver,
        int poolSize
) {

    // Creates database config
    public static DatabaseConfig from(AppConfig appConfig) {
        return new DatabaseConfig(
                appConfig.getString("db.url"),
                appConfig.getString("db.username", ""),
                appConfig.getString("db.password", ""),
                appConfig.getString("db.driver"),
                appConfig.getInt("db.pool.size", 10)
        );
    }

    // Checks SQLite database
    public boolean isSQLite() {
        return url != null && url.startsWith("jdbc:sqlite:");
    }

    // Checks PostgreSQL database
    public boolean isPostgreSQL() {
        return url != null && url.startsWith("jdbc:postgresql:");
    }

    // Checks MySQL database
    public boolean isMySQL() {
        return url != null && url.startsWith("jdbc:mysql:");
    }
}