package repository;

import db.DatabaseManager;
import dto.year.AvailableYearDto;
import dto.year.YearProfileDto;
import dto.year.YearPublicationDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import util.SqlFileLoader;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

class YearRepositoryTest {

    private YearRepository repository;

    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() {
        repository = new YearRepository();

        connection = Mockito.mock(Connection.class);
        statement = Mockito.mock(PreparedStatement.class);
        resultSet = Mockito.mock(ResultSet.class);
    }

    @Test
    void findAvailableYears_shouldReturnAvailableYears() throws Exception {
        String sql = "available years sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/year/available_years.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            Mockito.when(resultSet.next()).thenReturn(true, true, false);
            Mockito.when(resultSet.wasNull()).thenReturn(false);

            Mockito.when(resultSet.getInt("year")).thenReturn(2020, 2021);
            Mockito.when(resultSet.getLong("total_articles")).thenReturn(100L, 120L);
            Mockito.when(resultSet.getLong("total_journal_articles")).thenReturn(60L, 70L);
            Mockito.when(resultSet.getLong("total_conference_articles")).thenReturn(40L, 50L);

            List<AvailableYearDto> results = repository.findAvailableYears();

            Assertions.assertEquals(2, results.size());

            Assertions.assertEquals(2020, results.get(0).year());
            Assertions.assertEquals(100L, results.get(0).totalArticles());
            Assertions.assertEquals(60L, results.get(0).totalJournalArticles());
            Assertions.assertEquals(40L, results.get(0).totalConferenceArticles());

            Assertions.assertEquals(2021, results.get(1).year());
            Assertions.assertEquals(120L, results.get(1).totalArticles());
            Assertions.assertEquals(70L, results.get(1).totalJournalArticles());
            Assertions.assertEquals(50L, results.get(1).totalConferenceArticles());

            Mockito.verify(statement).executeQuery();
        }
    }

    @Test
    void findYearProfile_whenYearExists_shouldReturnProfile() throws Exception {
        String fullSql = "SELECT * FROM year_profile WHERE year = ?\n-- B2\nSELECT ignored";
        String expectedSql = "SELECT * FROM year_profile WHERE year = ?";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/year/year_profile.sql"))
                    .thenReturn(fullSql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(expectedSql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            Mockito.when(resultSet.next()).thenReturn(true);
            Mockito.when(resultSet.wasNull()).thenReturn(false);

            Mockito.when(resultSet.getInt("year")).thenReturn(2020);

            Mockito.when(resultSet.getLong("total_articles")).thenReturn(150L);
            Mockito.when(resultSet.getLong("total_journal_articles")).thenReturn(90L);
            Mockito.when(resultSet.getLong("total_conference_articles")).thenReturn(60L);

            Mockito.when(resultSet.getLong("distinct_journals")).thenReturn(20L);
            Mockito.when(resultSet.getLong("distinct_conferences")).thenReturn(15L);

            Mockito.when(resultSet.getLong("total_author_occurrences")).thenReturn(450L);
            Mockito.when(resultSet.getLong("distinct_authors")).thenReturn(300L);
            Mockito.when(resultSet.getDouble("avg_authors_per_article")).thenReturn(3.0);

            Optional<YearProfileDto> result = repository.findYearProfile(2020);

            Assertions.assertTrue(result.isPresent());

            YearProfileDto profile = result.get();

            Assertions.assertEquals(2020, profile.year());

            Assertions.assertEquals(150L, profile.totalArticles());
            Assertions.assertEquals(90L, profile.totalJournalArticles());
            Assertions.assertEquals(60L, profile.totalConferenceArticles());

            Assertions.assertEquals(20L, profile.distinctJournals());
            Assertions.assertEquals(15L, profile.distinctConferences());

            Assertions.assertEquals(450L, profile.totalAuthorOccurrences());
            Assertions.assertEquals(300L, profile.distinctAuthors());
            Assertions.assertEquals(3.0, profile.avgAuthorsPerArticle());

            Mockito.verify(connection).prepareStatement(expectedSql);
            Mockito.verify(statement).setInt(1, 2020);
        }
    }

    @Test
    void findYearProfile_whenYearDoesNotExist_shouldReturnEmptyOptional() throws Exception {
        String fullSql = "SELECT * FROM year_profile WHERE year = ?\n-- B2\nSELECT ignored";
        String expectedSql = "SELECT * FROM year_profile WHERE year = ?";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/year/year_profile.sql"))
                    .thenReturn(fullSql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(expectedSql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);
            Mockito.when(resultSet.next()).thenReturn(false);

            Optional<YearProfileDto> result = repository.findYearProfile(1999);

            Assertions.assertTrue(result.isEmpty());

            Mockito.verify(connection).prepareStatement(expectedSql);
            Mockito.verify(statement).setInt(1, 1999);
        }
    }

    @Test
    void findYearPublications_shouldReturnPublications() throws Exception {
        String sql = "year publications sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/year/year_publications_report.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            mockYearPublicationResultSet();

            List<YearPublicationDto> results =
                    repository.findYearPublications(2020, "journal", 5, 0, 7);

            Assertions.assertEquals(1, results.size());

            YearPublicationDto publication = results.get(0);

            assertYearPublication(publication);

            Mockito.verify(statement).setInt(1, 2020);
            Mockito.verify(statement).setString(2, "journal");
            Mockito.verify(statement).setInt(3, 5);
            Mockito.verify(statement).setInt(4, 0);
            Mockito.verify(statement).setInt(5, 7);
        }
    }

    @Test
    void findYearPublicationsBatch_shouldReturnPublications() throws Exception {
        String sql = "year publications batch sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/year/year_publications_report_batch.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            mockYearPublicationResultSet();

            List<YearPublicationDto> results =
                    repository.findYearPublicationsBatch(2020, "conference", 0, 3, 7, 100, 50);

            Assertions.assertEquals(1, results.size());

            YearPublicationDto publication = results.get(0);

            assertYearPublication(publication);

            Mockito.verify(statement).setInt(1, 2020);
            Mockito.verify(statement).setString(2, "conference");
            Mockito.verify(statement).setInt(3, 0);
            Mockito.verify(statement).setInt(4, 3);
            Mockito.verify(statement).setInt(5, 7);
            Mockito.verify(statement).setInt(6, 100);
            Mockito.verify(statement).setInt(7, 50);
        }
    }

    @Test
    void findYearPublicationsBatch_whenSqlExceptionOccurs_shouldThrowRuntimeException() throws Exception {
        String sql = "year publications batch sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/year/year_publications_report_batch.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql))
                    .thenThrow(new SQLException("Database error"));

            RuntimeException exception = Assertions.assertThrows(
                    RuntimeException.class,
                    () -> repository.findYearPublicationsBatch(2020, "journal", 5, 0, 7, 100, 50)
            );

            Assertions.assertEquals("Αποτυχία φόρτωσης batch δημοσιεύσεων χρονιάς.", exception.getMessage());
            Assertions.assertTrue(exception.getCause() instanceof SQLException);
        }
    }

    @Test
    void findAvailableYears_whenSqlExceptionOccurs_shouldThrowRuntimeException() throws Exception {
        String sql = "available years sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/year/available_years.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql))
                    .thenThrow(new SQLException("Database error"));

            RuntimeException exception = Assertions.assertThrows(
                    RuntimeException.class,
                    () -> repository.findAvailableYears()
            );

            Assertions.assertEquals("Αποτυχία φόρτωσης διαθέσιμων χρονιών.", exception.getMessage());
            Assertions.assertTrue(exception.getCause() instanceof SQLException);
        }
    }

    private void mockYearPublicationResultSet() throws Exception {
        Mockito.when(resultSet.next()).thenReturn(true, false);
        Mockito.when(resultSet.wasNull()).thenReturn(false);

        Mockito.when(resultSet.getInt("article_id")).thenReturn(100);
        Mockito.when(resultSet.getString("articlekey")).thenReturn("journals/test/Year2020");
        Mockito.when(resultSet.getString("title")).thenReturn("A Year Test Publication");
        Mockito.when(resultSet.getInt("year")).thenReturn(2020);
        Mockito.when(resultSet.getString("article_type")).thenReturn("journal");

        Mockito.when(resultSet.getInt("journal_id")).thenReturn(5);
        Mockito.when(resultSet.getString("journal_name")).thenReturn("Information Systems Journal");
        Mockito.when(resultSet.getInt("source_id")).thenReturn(1234);
        Mockito.when(resultSet.getString("volume")).thenReturn("10");
        Mockito.when(resultSet.getString("number")).thenReturn("2");

        Mockito.when(resultSet.getInt("conference_id")).thenReturn(3);
        Mockito.when(resultSet.getString("conference_acronym")).thenReturn("SIGMOD");
        Mockito.when(resultSet.getString("conference_title")).thenReturn("ACM SIGMOD Conference");
        Mockito.when(resultSet.getInt("icore_id")).thenReturn(50);

        Mockito.when(resultSet.getString("pages")).thenReturn("1-12");
        Mockito.when(resultSet.getString("ee")).thenReturn("https://example.com/paper");
        Mockito.when(resultSet.getString("url")).thenReturn("https://example.com");

        Mockito.when(resultSet.getDate("mdate")).thenReturn(Date.valueOf("2020-05-20"));

        Mockito.when(resultSet.getLong("author_count")).thenReturn(3L);
        Mockito.when(resultSet.getString("authors")).thenReturn("John Smith, Maria Papadopoulou, Nick Brown");
    }

    private void assertYearPublication(YearPublicationDto publication) {
        Assertions.assertEquals(100, publication.articleId());
        Assertions.assertEquals("journals/test/Year2020", publication.articleKey());
        Assertions.assertEquals("A Year Test Publication", publication.title());
        Assertions.assertEquals(2020, publication.year());
        Assertions.assertEquals("journal", publication.articleType());

        Assertions.assertEquals(5, publication.journalId());
        Assertions.assertEquals("Information Systems Journal", publication.journalName());
        Assertions.assertEquals(1234, publication.sourceId());
        Assertions.assertEquals("10", publication.volume());
        Assertions.assertEquals("2", publication.number());

        Assertions.assertEquals(3, publication.conferenceId());
        Assertions.assertEquals("SIGMOD", publication.conferenceAcronym());
        Assertions.assertEquals("ACM SIGMOD Conference", publication.conferenceTitle());
        Assertions.assertEquals(50, publication.icoreId());

        Assertions.assertEquals("1-12", publication.pages());
        Assertions.assertEquals("https://example.com/paper", publication.ee());
        Assertions.assertEquals("https://example.com", publication.url());
        Assertions.assertEquals(LocalDate.of(2020, 5, 20), publication.mdate());

        Assertions.assertEquals(3L, publication.authorCount());
        Assertions.assertEquals("John Smith, Maria Papadopoulou, Nick Brown", publication.authors());
    }
}