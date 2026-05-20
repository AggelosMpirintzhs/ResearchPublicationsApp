package db;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

import static org.mockito.Mockito.*;

class DatabaseManagerTest {

    private DataSource dataSource;
    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);

        setDataSource(null);
    }

    @AfterEach
    void tearDown() throws Exception {
        setDataSource(null);
    }

    @Test
    void getDataSource_whenDataSourceIsNotInitialized_shouldThrowDatabaseConnectionException() {
        DatabaseConnectionException exception = Assertions.assertThrows(
                DatabaseConnectionException.class,
                DatabaseManager::getDataSource
        );

        Assertions.assertEquals(
                "Το DataSource δεν έχει αρχικοποιηθεί. Κάλεσε πρώτα DatabaseManager.initialize().",
                exception.getMessage()
        );
    }

    @Test
    void getDataSource_whenDataSourceIsInitialized_shouldReturnDataSource() throws Exception {
        setDataSource(dataSource);

        DataSource result = DatabaseManager.getDataSource();

        Assertions.assertSame(dataSource, result);
    }

    @Test
    void getConnection_whenDataSourceReturnsConnection_shouldReturnConnection() throws Exception {
        setDataSource(dataSource);

        when(dataSource.getConnection()).thenReturn(connection);

        Connection result = DatabaseManager.getConnection();

        Assertions.assertSame(connection, result);
        verify(dataSource).getConnection();
    }

    @Test
    void getConnection_whenDataSourceThrowsSQLException_shouldThrowDatabaseConnectionException() throws Exception {
        setDataSource(dataSource);

        SQLException sqlException = new SQLException("Connection failed");

        when(dataSource.getConnection()).thenThrow(sqlException);

        DatabaseConnectionException exception = Assertions.assertThrows(
                DatabaseConnectionException.class,
                DatabaseManager::getConnection
        );

        Assertions.assertEquals(
                "Αποτυχία λήψης σύνδεσης από τη βάση.",
                exception.getMessage()
        );

        Assertions.assertSame(sqlException, exception.getCause());
    }

    @Test
    void testConnection_whenConnectionIsValid_shouldNotThrowException() throws Exception {
        setDataSource(dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);

        Assertions.assertDoesNotThrow(DatabaseManager::testConnection);

        verify(dataSource).getConnection();
        verify(connection).isValid(5);
        verify(connection).close();
    }

    @Test
    void testConnection_whenConnectionIsNotValid_shouldThrowDatabaseConnectionException() throws Exception {
        setDataSource(dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(false);

        DatabaseConnectionException exception = Assertions.assertThrows(
                DatabaseConnectionException.class,
                DatabaseManager::testConnection
        );

        Assertions.assertEquals(
                "Η σύνδεση με τη βάση δεν είναι έγκυρη.",
                exception.getMessage()
        );

        verify(connection).isValid(5);
        verify(connection).close();
    }

    @Test
    void testConnection_whenIsValidThrowsSQLException_shouldThrowDatabaseConnectionException() throws Exception {
        setDataSource(dataSource);

        SQLException sqlException = new SQLException("Validation failed");

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenThrow(sqlException);

        DatabaseConnectionException exception = Assertions.assertThrows(
                DatabaseConnectionException.class,
                DatabaseManager::testConnection
        );

        Assertions.assertEquals(
                "Αποτυχία ελέγχου σύνδεσης με τη βάση.",
                exception.getMessage()
        );

        Assertions.assertSame(sqlException, exception.getCause());

        verify(connection).isValid(5);
        verify(connection).close();
    }

    @Test
    void shutdown_whenDataSourceIsHikariDataSource_shouldCloseConnectionPool() throws Exception {
        HikariDataSource hikariDataSource = mock(HikariDataSource.class);

        setDataSource(hikariDataSource);

        DatabaseManager.shutdown();

        verify(hikariDataSource).close();
    }

    @Test
    void shutdown_whenDataSourceIsNotHikariDataSource_shouldNotThrowException() throws Exception {
        setDataSource(dataSource);

        Assertions.assertDoesNotThrow(DatabaseManager::shutdown);
    }

    @Test
    void shutdown_whenDataSourceIsNull_shouldNotThrowException() throws Exception {
        setDataSource(null);

        Assertions.assertDoesNotThrow(DatabaseManager::shutdown);
    }

    private static void setDataSource(DataSource dataSource) throws Exception {
        Field field = DatabaseManager.class.getDeclaredField("dataSource");
        field.setAccessible(true);
        field.set(null, dataSource);
    }
}