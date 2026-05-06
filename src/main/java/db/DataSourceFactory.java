package db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import config.DatabaseConfig;

import javax.sql.DataSource;

public class DataSourceFactory {

    private DataSourceFactory() {
        // Utility class. Δεν θέλουμε να δημιουργείται αντικείμενο από αυτή την κλάση.
    }

    public static DataSource createDataSource(DatabaseConfig databaseConfig) {
        try {
            Class.forName(databaseConfig.driver());

            HikariConfig hikariConfig = new HikariConfig();

            hikariConfig.setJdbcUrl(databaseConfig.url());
            hikariConfig.setUsername(databaseConfig.username());
            hikariConfig.setPassword(databaseConfig.password());

            hikariConfig.setMaximumPoolSize(databaseConfig.poolSize());
            hikariConfig.setMinimumIdle(1);

            hikariConfig.setPoolName("ThesisQueryAppPool");

            hikariConfig.setConnectionTimeout(10_000);
            hikariConfig.setIdleTimeout(60_000);
            hikariConfig.setMaxLifetime(1_800_000);

            return new HikariDataSource(hikariConfig);

        } catch (ClassNotFoundException exception) {
            throw new DatabaseConnectionException(
                    "Δεν βρέθηκε ο JDBC driver: " + databaseConfig.driver(),
                    exception
            );
        } catch (Exception exception) {
            throw new DatabaseConnectionException(
                    "Αποτυχία δημιουργίας DataSource.",
                    exception
            );
        }
    }
}