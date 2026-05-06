package db;

import com.zaxxer.hikari.HikariDataSource;
import config.AppConfig;
import config.DatabaseConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private static DataSource dataSource;

    private DatabaseManager() {
        // Utility class. Δεν θέλουμε instances.
    }

    public static void initialize() {
        AppConfig appConfig = new AppConfig();
        DatabaseConfig databaseConfig = DatabaseConfig.from(appConfig);

        dataSource = DataSourceFactory.createDataSource(databaseConfig);

        testConnection();
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            throw new DatabaseConnectionException(
                    "Το DataSource δεν έχει αρχικοποιηθεί. Κάλεσε πρώτα DatabaseManager.initialize()."
            );
        }

        return dataSource;
    }

    public static Connection getConnection() {
        try {
            return getDataSource().getConnection();
        } catch (SQLException exception) {
            throw new DatabaseConnectionException(
                    "Αποτυχία λήψης σύνδεσης από τη βάση.",
                    exception
            );
        }
    }

    public static void testConnection() {
        try (Connection connection = getConnection()) {
            if (!connection.isValid(5)) {
                throw new DatabaseConnectionException("Η σύνδεση με τη βάση δεν είναι έγκυρη.");
            }

            System.out.println("Επιτυχής σύνδεση με τη βάση δεδομένων.");

        } catch (SQLException exception) {
            throw new DatabaseConnectionException(
                    "Αποτυχία ελέγχου σύνδεσης με τη βάση.",
                    exception
            );
        }
    }

    public static void shutdown() {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            hikariDataSource.close();
            System.out.println("Το database connection pool έκλεισε.");
        }
    }
}