package util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqlFileLoaderTest {

    @BeforeEach
    void setUp() {
        SqlFileLoader.clearCache();
    }

    @Test
    void load_whenSqlFileExists_shouldReturnFileContent() {
        String sql = SqlFileLoader.load("sql/test/sql_file_loader_test.sql");

        Assertions.assertNotNull(sql);
        Assertions.assertTrue(sql.contains("SELECT *"));
        Assertions.assertTrue(sql.contains("FROM test_table"));
        Assertions.assertTrue(sql.contains("WHERE id = ?;"));
    }

    @Test
    void load_whenPathStartsWithSlash_shouldNormalizePathAndReturnFileContent() {
        String sqlWithoutSlash = SqlFileLoader.load("sql/test/sql_file_loader_test.sql");
        String sqlWithSlash = SqlFileLoader.load("/sql/test/sql_file_loader_test.sql");

        Assertions.assertEquals(sqlWithoutSlash, sqlWithSlash);
    }

    @Test
    void load_whenPathIsNull_shouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SqlFileLoader.load(null)
        );

        Assertions.assertEquals(
                "SQL file path cannot be empty.",
                exception.getMessage()
        );
    }

    @Test
    void load_whenPathIsBlank_shouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SqlFileLoader.load("   ")
        );

        Assertions.assertEquals(
                "SQL file path cannot be empty.",
                exception.getMessage()
        );
    }

    @Test
    void load_whenSqlFileDoesNotExist_shouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SqlFileLoader.load("sql/test/not_existing_file.sql")
        );

        Assertions.assertTrue(
                exception.getMessage().contains("SQL file not found in resources:")
        );
    }

    @Test
    void clearCache_shouldAllowLoadingAgainAfterCacheClear() {
        String firstLoad = SqlFileLoader.load("sql/test/sql_file_loader_test.sql");

        SqlFileLoader.clearCache();

        String secondLoad = SqlFileLoader.load("sql/test/sql_file_loader_test.sql");

        Assertions.assertEquals(firstLoad, secondLoad);
    }
}