package repository;

import db.DatabaseManager;
import dto.author.AuthorProfileDto;
import dto.author.AuthorPublicationDto;
import dto.author.AuthorSearchResultDto;
import dto.author.AuthorYearlyStatsByTypeDto;
import dto.author.AuthorYearlyStatsDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import util.SqlFileLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

class AuthorRepositoryTest {

    private AuthorRepository repository;

    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() {
        repository = new AuthorRepository();

        connection = Mockito.mock(Connection.class);
        statement = Mockito.mock(PreparedStatement.class);
        resultSet = Mockito.mock(ResultSet.class);
    }

    @Test
    void searchAuthors_shouldReturnMatchingAuthors_andSetSearchPatternsCorrectly() throws Exception {
        String sql = "SELECT * FROM authors WHERE author_name LIKE ? LIMIT ?";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/author/search_authors.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            Mockito.when(resultSet.next()).thenReturn(true, true, false);

            Mockito.when(resultSet.getInt("author_id")).thenReturn(1, 2);
            Mockito.when(resultSet.wasNull()).thenReturn(false);
            Mockito.when(resultSet.getString("author_name")).thenReturn("John Smith", "John Doe");

            List<AuthorSearchResultDto> results = repository.searchAuthors("  John   Sm  ", 10);

            Assertions.assertEquals(2, results.size());

            Assertions.assertEquals(1, results.get(0).authorId());
            Assertions.assertEquals("John Smith", results.get(0).authorName());

            Assertions.assertEquals(2, results.get(1).authorId());
            Assertions.assertEquals("John Doe", results.get(1).authorName());

            Mockito.verify(statement).setString(1, "John%Sm%");
            Mockito.verify(statement).setString(2, "% John%Sm%");
            Mockito.verify(statement).setString(3, "%John%Sm%");

            Mockito.verify(statement).setString(4, "John%Sm%");
            Mockito.verify(statement).setString(5, "% John%Sm%");
            Mockito.verify(statement).setString(6, "%John%Sm%");

            Mockito.verify(statement).setInt(7, 10);
            Mockito.verify(statement).executeQuery();
        }
    }

    @Test
    void searchAuthors_withBlankSearchText_shouldUseWildcardOnlyPatterns() throws Exception {
        String sql = "search authors sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/author/search_authors.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);
            Mockito.when(resultSet.next()).thenReturn(false);

            List<AuthorSearchResultDto> results = repository.searchAuthors("   ", 5);

            Assertions.assertTrue(results.isEmpty());

            Mockito.verify(statement).setString(1, "%");
            Mockito.verify(statement).setString(2, "% %");
            Mockito.verify(statement).setString(3, "%%");

            Mockito.verify(statement).setString(4, "%");
            Mockito.verify(statement).setString(5, "% %");
            Mockito.verify(statement).setString(6, "%%");

            Mockito.verify(statement).setInt(7, 5);
        }
    }

    @Test
    void findAuthorProfile_whenAuthorExists_shouldReturnProfile() throws Exception {
        String sql = "author profile sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/author/author_profile.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            Mockito.when(resultSet.next()).thenReturn(true);
            Mockito.when(resultSet.wasNull()).thenReturn(false);

            Mockito.when(resultSet.getInt("author_id")).thenReturn(7);
            Mockito.when(resultSet.getString("author_name")).thenReturn("Maria Papadopoulou");
            Mockito.when(resultSet.getInt("first_year")).thenReturn(2010);
            Mockito.when(resultSet.getInt("last_year")).thenReturn(2020);

            Mockito.when(resultSet.getLong("active_years")).thenReturn(11L);
            Mockito.when(resultSet.getLong("total_articles")).thenReturn(25L);
            Mockito.when(resultSet.getLong("total_journal_articles")).thenReturn(15L);
            Mockito.when(resultSet.getLong("total_conference_articles")).thenReturn(10L);
            Mockito.when(resultSet.getLong("distinct_journals")).thenReturn(5L);
            Mockito.when(resultSet.getLong("distinct_conferences")).thenReturn(4L);

            Mockito.when(resultSet.getDouble("avg_articles_per_year")).thenReturn(2.27);

            Optional<AuthorProfileDto> result = repository.findAuthorProfile(7, 2000, 2025);

            Assertions.assertTrue(result.isPresent());

            AuthorProfileDto profile = result.get();

            Assertions.assertEquals(7, profile.authorId());
            Assertions.assertEquals("Maria Papadopoulou", profile.authorName());
            Assertions.assertEquals(2010, profile.firstYear());
            Assertions.assertEquals(2020, profile.lastYear());
            Assertions.assertEquals(11L, profile.activeYears());
            Assertions.assertEquals(25L, profile.totalArticles());
            Assertions.assertEquals(15L, profile.totalJournalArticles());
            Assertions.assertEquals(10L, profile.totalConferenceArticles());
            Assertions.assertEquals(5L, profile.distinctJournals());
            Assertions.assertEquals(4L, profile.distinctConferences());
            Assertions.assertEquals(2.27, profile.avgArticlesPerYear());

            Mockito.verify(statement).setInt(1, 7);
            Mockito.verify(statement).setInt(2, 2000);
            Mockito.verify(statement).setInt(3, 2025);
        }
    }

    @Test
    void findAuthorProfile_whenAuthorDoesNotExist_shouldReturnEmptyOptional() throws Exception {
        String sql = "author profile sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/author/author_profile.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);
            Mockito.when(resultSet.next()).thenReturn(false);

            Optional<AuthorProfileDto> result = repository.findAuthorProfile(99, 2000, 2025);

            Assertions.assertTrue(result.isEmpty());

            Mockito.verify(statement).setInt(1, 99);
            Mockito.verify(statement).setInt(2, 2000);
            Mockito.verify(statement).setInt(3, 2025);
        }
    }

    @Test
    void findAuthorYearlyStats_shouldReturnYearlyStats() throws Exception {
        String sql = "author yearly stats sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/author/author_yearly_stats.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            Mockito.when(resultSet.next()).thenReturn(true, true, false);
            Mockito.when(resultSet.wasNull()).thenReturn(false);

            Mockito.when(resultSet.getInt("year")).thenReturn(2020, 2021);
            Mockito.when(resultSet.getLong("total_articles")).thenReturn(3L, 5L);

            List<AuthorYearlyStatsDto> results = repository.findAuthorYearlyStats(4, 2020, 2021);

            Assertions.assertEquals(2, results.size());

            Assertions.assertEquals(2020, results.get(0).year());
            Assertions.assertEquals(3L, results.get(0).totalArticles());

            Assertions.assertEquals(2021, results.get(1).year());
            Assertions.assertEquals(5L, results.get(1).totalArticles());

            Mockito.verify(statement).setInt(1, 4);
            Mockito.verify(statement).setInt(2, 2020);
            Mockito.verify(statement).setInt(3, 2021);
        }
    }

    @Test
    void findAuthorYearlyStatsByType_shouldReturnYearlyStatsByType() throws Exception {
        String sql = "author yearly stats by type sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/author/author_yearly_stats_by_type.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            Mockito.when(resultSet.next()).thenReturn(true, false);
            Mockito.when(resultSet.wasNull()).thenReturn(false);

            Mockito.when(resultSet.getInt("year")).thenReturn(2022);
            Mockito.when(resultSet.getLong("total_articles")).thenReturn(8L);
            Mockito.when(resultSet.getLong("total_journal_articles")).thenReturn(5L);
            Mockito.when(resultSet.getLong("total_conference_articles")).thenReturn(3L);

            List<AuthorYearlyStatsByTypeDto> results =
                    repository.findAuthorYearlyStatsByType(4, 2020, 2025);

            Assertions.assertEquals(1, results.size());

            AuthorYearlyStatsByTypeDto stats = results.get(0);

            Assertions.assertEquals(2022, stats.year());
            Assertions.assertEquals(8L, stats.totalArticles());
            Assertions.assertEquals(5L, stats.totalJournalArticles());
            Assertions.assertEquals(3L, stats.totalConferenceArticles());

            Mockito.verify(statement).setInt(1, 4);
            Mockito.verify(statement).setInt(2, 2020);
            Mockito.verify(statement).setInt(3, 2025);
        }
    }

    @Test
    void findAuthorPublicationsBatch_shouldReturnPublications() throws Exception {
        String sql = "author publications batch sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/author/author_publications_report_batch.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            Mockito.when(resultSet.next()).thenReturn(true, false);
            Mockito.when(resultSet.wasNull()).thenReturn(false);

            Mockito.when(resultSet.getInt("article_id")).thenReturn(100);
            Mockito.when(resultSet.getString("articlekey")).thenReturn("journals/test/Smith2020");
            Mockito.when(resultSet.getString("title")).thenReturn("A Test Publication");
            Mockito.when(resultSet.getInt("year")).thenReturn(2020);
            Mockito.when(resultSet.getString("article_type")).thenReturn("journal");

            Mockito.when(resultSet.getInt("journal_id")).thenReturn(12);
            Mockito.when(resultSet.getString("journal_name")).thenReturn("Test Journal");
            Mockito.when(resultSet.getString("volume")).thenReturn("10");
            Mockito.when(resultSet.getString("number")).thenReturn("2");

            Mockito.when(resultSet.getInt("conference_id")).thenReturn(0);
            Mockito.when(resultSet.getString("conference_acronym")).thenReturn(null);
            Mockito.when(resultSet.getString("conference_title")).thenReturn(null);

            Mockito.when(resultSet.getString("pages")).thenReturn("1-10");
            Mockito.when(resultSet.getString("ee")).thenReturn("https://example.com/paper");
            Mockito.when(resultSet.getString("url")).thenReturn("https://example.com");

            Mockito.when(resultSet.getLong("author_count")).thenReturn(2L);
            Mockito.when(resultSet.getString("authors")).thenReturn("John Smith, Maria Papadopoulou");

            List<AuthorPublicationDto> results =
                    repository.findAuthorPublicationsBatch(5, 2000, 2025, 0, 100);

            Assertions.assertEquals(1, results.size());

            AuthorPublicationDto publication = results.get(0);

            Assertions.assertEquals(100, publication.articleId());
            Assertions.assertEquals("journals/test/Smith2020", publication.articleKey());
            Assertions.assertEquals("A Test Publication", publication.title());
            Assertions.assertEquals(2020, publication.year());
            Assertions.assertEquals("journal", publication.articleType());

            Assertions.assertEquals(12, publication.journalId());
            Assertions.assertEquals("Test Journal", publication.journalName());
            Assertions.assertEquals("10", publication.volume());
            Assertions.assertEquals("2", publication.number());

            Assertions.assertEquals(0, publication.conferenceId());
            Assertions.assertNull(publication.conferenceAcronym());
            Assertions.assertNull(publication.conferenceTitle());

            Assertions.assertEquals("1-10", publication.pages());
            Assertions.assertEquals("https://example.com/paper", publication.ee());
            Assertions.assertEquals("https://example.com", publication.url());

            Assertions.assertEquals(2L, publication.authorCount());
            Assertions.assertEquals("John Smith, Maria Papadopoulou", publication.authors());

            Mockito.verify(statement).setInt(1, 5);
            Mockito.verify(statement).setInt(2, 2000);
            Mockito.verify(statement).setInt(3, 2025);
            Mockito.verify(statement).setInt(4, 0);
            Mockito.verify(statement).setInt(5, 100);
        }
    }

    @Test
    void findAuthorPublicationsBatch_whenSqlExceptionOccurs_shouldThrowRuntimeException() throws Exception {
        String sql = "author publications batch sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/author/author_publications_report_batch.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenThrow(new SQLException("Database error"));

            RuntimeException exception = Assertions.assertThrows(
                    RuntimeException.class,
                    () -> repository.findAuthorPublicationsBatch(5, 2000, 2025, 0, 100)
            );

            Assertions.assertEquals("Αποτυχία φόρτωσης batch δημοσιεύσεων συγγραφέα.", exception.getMessage());
            Assertions.assertTrue(exception.getCause() instanceof SQLException);
        }
    }
}