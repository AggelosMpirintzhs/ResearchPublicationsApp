package config;

public record DatabaseConfig(
        String url,
        String username,
        String password,
        String driver,
        int poolSize
) {

    public static DatabaseConfig from(AppConfig appConfig) {
        return new DatabaseConfig(
                appConfig.getString("db.url"),
                appConfig.getString("db.username", ""),
                appConfig.getString("db.password", ""),
                appConfig.getString("db.driver"),
                appConfig.getInt("db.pool.size", 10)
        );
    }

    public boolean isSQLite() {
        return url != null && url.startsWith("jdbc:sqlite:");
    }

    public boolean isPostgreSQL() {
        return url != null && url.startsWith("jdbc:postgresql:");
    }

    public boolean isMySQL() {
        return url != null && url.startsWith("jdbc:mysql:");
    }
}