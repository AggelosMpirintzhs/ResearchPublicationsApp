package db;

import com.zaxxer.hikari.HikariDataSource;
import config.AppConfig;
import config.DatabaseConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private static DataSource dataSource;

    // Prevents object creation
    private DatabaseManager() {
    }

    // Initializes database source
    public static void initialize() {
        AppConfig appConfig = new AppConfig();
        DatabaseConfig databaseConfig = DatabaseConfig.from(appConfig);

        dataSource = DataSourceFactory.createDataSource(databaseConfig);

        testConnection();
    }

    // Gets data source
    public static DataSource getDataSource() {
        if (dataSource == null) {
            throw new DatabaseConnectionException(
                    "DataSource has not been initialized. Call DatabaseManager.initialize() first."
            );
        }

        return dataSource;
    }

    // Gets database connection
    public static Connection getConnection() {
        try {
            return getDataSource().getConnection();
        } catch (SQLException exception) {
            throw new DatabaseConnectionException(
                    "Failed to get database connection.",
                    exception
            );
        }
    }

    // Tests database connection
    public static void testConnection() {
        try (Connection connection = getConnection()) {
            if (!connection.isValid(5)) {
                throw new DatabaseConnectionException("Database connection is not valid.");
            }

            System.out.println("Database connection successful.");

        } catch (SQLException exception) {
            throw new DatabaseConnectionException(
                    "Failed to test database connection.",
                    exception
            );
        }
    }

    // Closes database pool
    public static void shutdown() {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            hikariDataSource.close();
            System.out.println("Database connection pool closed.");
        }
    }
}