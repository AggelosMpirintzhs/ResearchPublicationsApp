package repository;

import db.DatabaseManager;
import dto.chart.CategoryOptionDto;
import dto.conference.ConferenceArticleDto;
import dto.conference.ConferenceProfileDto;
import dto.conference.ConferenceRankingDto;
import dto.conference.ConferenceSearchResultDto;
import dto.conference.ConferenceYearlyStatsDto;
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

class ConferenceRepositoryTest {

    private ConferenceRepository repository;

    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() {
        repository = new ConferenceRepository();

        connection = Mockito.mock(Connection.class);
        statement = Mockito.mock(PreparedStatement.class);
        resultSet = Mockito.mock(ResultSet.class);
    }

    @Test
    void searchConferences_shouldReturnMatchingConferences() throws Exception {
        String sql = "search conferences sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/conference/search_conferences.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            Mockito.when(resultSet.next()).thenReturn(true, true, false);
            Mockito.when(resultSet.wasNull()).thenReturn(false);

            Mockito.when(resultSet.getInt("conference_id")).thenReturn(1, 2);
            Mockito.when(resultSet.getString("acronym")).thenReturn("SIGMOD", "VLDB");
            Mockito.when(resultSet.getString("conference_title")).thenReturn(
                    "ACM SIGMOD Conference",
                    "Very Large Data Bases"
            );
            Mockito.when(resultSet.getInt("icore_id")).thenReturn(10, 20);

            List<ConferenceSearchResultDto> results =
                    repository.searchConferences("data", 10);

            Assertions.assertEquals(2, results.size());

            Assertions.assertEquals(1, results.get(0).conferenceId());
            Assertions.assertEquals("SIGMOD", results.get(0).acronym());
            Assertions.assertEquals("ACM SIGMOD Conference", results.get(0).conferenceTitle());
            Assertions.assertEquals(10, results.get(0).icoreId());

            Assertions.assertEquals(2, results.get(1).conferenceId());
            Assertions.assertEquals("VLDB", results.get(1).acronym());
            Assertions.assertEquals("Very Large Data Bases", results.get(1).conferenceTitle());
            Assertions.assertEquals(20, results.get(1).icoreId());

            Mockito.verify(statement).setString(1, "data");
            Mockito.verify(statement).setString(2, "data");
            Mockito.verify(statement).setInt(3, 10);
            Mockito.verify(statement).executeQuery();
        }
    }

    @Test
    void findConferenceProfile_whenConferenceExists_shouldReturnProfile() throws Exception {
        String sql = "conference profile sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/conference/conference_profile.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            Mockito.when(resultSet.next()).thenReturn(true);
            Mockito.when(resultSet.wasNull()).thenReturn(false);

            Mockito.when(resultSet.getInt("conference_id")).thenReturn(5);
            Mockito.when(resultSet.getString("acronym")).thenReturn("ICDE");
            Mockito.when(resultSet.getString("conference_title")).thenReturn("International Conference on Data Engineering");
            Mockito.when(resultSet.getInt("icore_id")).thenReturn(100);

            Mockito.when(resultSet.getInt("first_year")).thenReturn(2010);
            Mockito.when(resultSet.getInt("last_year")).thenReturn(2020);
            Mockito.when(resultSet.getLong("active_years")).thenReturn(11L);

            Mockito.when(resultSet.getLong("total_articles")).thenReturn(150L);
            Mockito.when(resultSet.getLong("total_author_occurrences")).thenReturn(450L);
            Mockito.when(resultSet.getLong("distinct_authors")).thenReturn(300L);

            Mockito.when(resultSet.getDouble("avg_articles_per_year")).thenReturn(13.64);
            Mockito.when(resultSet.getDouble("avg_author_occurrences_per_year")).thenReturn(40.91);
            Mockito.when(resultSet.getDouble("avg_authors_per_article")).thenReturn(3.0);

            Optional<ConferenceProfileDto> result =
                    repository.findConferenceProfile(5, 2000, 2025);

            Assertions.assertTrue(result.isPresent());

            ConferenceProfileDto profile = result.get();

            Assertions.assertEquals(5, profile.conferenceId());
            Assertions.assertEquals("ICDE", profile.acronym());
            Assertions.assertEquals("International Conference on Data Engineering", profile.conferenceTitle());
            Assertions.assertEquals(100, profile.icoreId());

            Assertions.assertEquals(2010, profile.firstYear());
            Assertions.assertEquals(2020, profile.lastYear());
            Assertions.assertEquals(11L, profile.activeYears());

            Assertions.assertEquals(150L, profile.totalArticles());
            Assertions.assertEquals(450L, profile.totalAuthorOccurrences());
            Assertions.assertEquals(300L, profile.distinctAuthors());

            Assertions.assertEquals(13.64, profile.avgArticlesPerYear());
            Assertions.assertEquals(40.91, profile.avgAuthorOccurrencesPerYear());
            Assertions.assertEquals(3.0, profile.avgAuthorsPerArticle());

            Mockito.verify(statement).setInt(1, 5);
            Mockito.verify(statement).setInt(2, 2000);
            Mockito.verify(statement).setInt(3, 2025);
        }
    }

    @Test
    void findConferenceProfile_whenConferenceDoesNotExist_shouldReturnEmptyOptional() throws Exception {
        String sql = "conference profile sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/conference/conference_profile.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);
            Mockito.when(resultSet.next()).thenReturn(false);

            Optional<ConferenceProfileDto> result =
                    repository.findConferenceProfile(99, 2000, 2025);

            Assertions.assertTrue(result.isEmpty());

            Mockito.verify(statement).setInt(1, 99);
            Mockito.verify(statement).setInt(2, 2000);
            Mockito.verify(statement).setInt(3, 2025);
        }
    }

    @Test
    void findConferenceArticlesBatch_shouldReturnArticles() throws Exception {
        String sql = "conference articles batch sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/conference/conference_articles_report_batch.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            mockConferenceArticleResultSet();

            List<ConferenceArticleDto> results =
                    repository.findConferenceArticlesBatch(3, 2000, 2025, 0, 100);

            Assertions.assertEquals(1, results.size());

            ConferenceArticleDto article = results.get(0);

            assertConferenceArticle(article);

            Mockito.verify(statement).setInt(1, 3);
            Mockito.verify(statement).setInt(2, 2000);
            Mockito.verify(statement).setInt(3, 2025);
            Mockito.verify(statement).setInt(4, 0);
            Mockito.verify(statement).setInt(5, 100);
        }
    }

    @Test
    void findConferenceYearlyStats_shouldReturnYearlyStats() throws Exception {
        String sql = "conference yearly stats sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/conference/conference_yearly_linechart.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            Mockito.when(resultSet.next()).thenReturn(true, true, false);
            Mockito.when(resultSet.wasNull()).thenReturn(false);

            Mockito.when(resultSet.getInt("conference_id")).thenReturn(3, 3);
            Mockito.when(resultSet.getString("acronym")).thenReturn("SIGMOD", "SIGMOD");
            Mockito.when(resultSet.getString("conference_title")).thenReturn(
                    "ACM SIGMOD Conference",
                    "ACM SIGMOD Conference"
            );
            Mockito.when(resultSet.getInt("year")).thenReturn(2020, 2021);

            Mockito.when(resultSet.getLong("total_articles")).thenReturn(40L, 45L);
            Mockito.when(resultSet.getLong("total_author_occurrences")).thenReturn(120L, 135L);
            Mockito.when(resultSet.getLong("distinct_authors")).thenReturn(100L, 110L);
            Mockito.when(resultSet.getDouble("avg_authors_per_article")).thenReturn(3.0, 3.1);

            List<ConferenceYearlyStatsDto> results =
                    repository.findConferenceYearlyStats(3, 2020, 2021);

            Assertions.assertEquals(2, results.size());

            Assertions.assertEquals(3, results.get(0).conferenceId());
            Assertions.assertEquals("SIGMOD", results.get(0).acronym());
            Assertions.assertEquals("ACM SIGMOD Conference", results.get(0).conferenceTitle());
            Assertions.assertEquals(2020, results.get(0).year());
            Assertions.assertEquals(40L, results.get(0).totalArticles());
            Assertions.assertEquals(120L, results.get(0).totalAuthorOccurrences());
            Assertions.assertEquals(100L, results.get(0).distinctAuthors());
            Assertions.assertEquals(3.0, results.get(0).avgAuthorsPerArticle());

            Assertions.assertEquals(2021, results.get(1).year());
            Assertions.assertEquals(45L, results.get(1).totalArticles());
            Assertions.assertEquals(135L, results.get(1).totalAuthorOccurrences());
            Assertions.assertEquals(110L, results.get(1).distinctAuthors());
            Assertions.assertEquals(3.1, results.get(1).avgAuthorsPerArticle());

            Mockito.verify(statement).setInt(1, 3);
            Mockito.verify(statement).setInt(2, 2020);
            Mockito.verify(statement).setInt(3, 2021);
        }
    }

    @Test
    void findConferenceRanking_whenRankingExists_shouldReturnRanking() throws Exception {
        String sql = "conference ranking sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/conference/conference_ranking.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            Mockito.when(resultSet.next()).thenReturn(true);
            Mockito.when(resultSet.wasNull()).thenReturn(false);

            Mockito.when(resultSet.getInt("conference_id")).thenReturn(3);
            Mockito.when(resultSet.getString("acronym")).thenReturn("SIGMOD");
            Mockito.when(resultSet.getString("conference_title")).thenReturn("ACM SIGMOD Conference");
            Mockito.when(resultSet.getInt("icore_id")).thenReturn(50);

            Mockito.when(resultSet.getString("rank_label")).thenReturn("A*");
            Mockito.when(resultSet.getInt("primaryFoR_id")).thenReturn(4602);
            Mockito.when(resultSet.getString("primaryFoR_name")).thenReturn("Artificial Intelligence");

            Optional<ConferenceRankingDto> result =
                    repository.findConferenceRanking(3);

            Assertions.assertTrue(result.isPresent());

            ConferenceRankingDto ranking = result.get();

            Assertions.assertEquals(3, ranking.conferenceId());
            Assertions.assertEquals("SIGMOD", ranking.acronym());
            Assertions.assertEquals("ACM SIGMOD Conference", ranking.conferenceTitle());
            Assertions.assertEquals(50, ranking.icoreId());

            Assertions.assertEquals("A*", ranking.rankLabel());
            Assertions.assertEquals(4602, ranking.primaryFoRId());
            Assertions.assertEquals("Artificial Intelligence", ranking.primaryFoRName());

            Mockito.verify(statement).setInt(1, 3);
        }
    }

    @Test
    void findConferenceRanking_whenRankingDoesNotExist_shouldReturnEmptyOptional() throws Exception {
        String sql = "conference ranking sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/conference/conference_ranking.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);
            Mockito.when(resultSet.next()).thenReturn(false);

            Optional<ConferenceRankingDto> result =
                    repository.findConferenceRanking(99);

            Assertions.assertTrue(result.isEmpty());

            Mockito.verify(statement).setInt(1, 99);
        }
    }

    @Test
    void findConferenceArticles_shouldReturnArticles() throws Exception {
        String sql = "conference articles sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/conference/conference_articles_report.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            mockConferenceArticleResultSet();

            List<ConferenceArticleDto> results =
                    repository.findConferenceArticles(3, 2000, 2025);

            Assertions.assertEquals(1, results.size());

            ConferenceArticleDto article = results.get(0);

            assertConferenceArticle(article);

            Mockito.verify(statement).setInt(1, 3);
            Mockito.verify(statement).setInt(2, 2000);
            Mockito.verify(statement).setInt(3, 2025);
        }
    }

    @Test
    void findPrimaryFoRCategories_shouldReturnCategories() throws Exception {
        String sql = "primary for categories sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/conference/conference_primary_for_categories.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            Mockito.when(resultSet.next()).thenReturn(true, true, false);
            Mockito.when(resultSet.getInt("category_id")).thenReturn(4602, 4611);
            Mockito.when(resultSet.getString("category_name")).thenReturn(
                    "Artificial Intelligence",
                    "Machine Learning"
            );

            List<CategoryOptionDto> results = repository.findPrimaryFoRCategories();

            Assertions.assertEquals(2, results.size());

            Mockito.verify(statement).executeQuery();
        }
    }

    @Test
    void findConferenceArticlesBatch_whenSqlExceptionOccurs_shouldThrowRuntimeException() throws Exception {
        String sql = "conference articles batch sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/conference/conference_articles_report_batch.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql))
                    .thenThrow(new SQLException("Database error"));

            RuntimeException exception = Assertions.assertThrows(
                    RuntimeException.class,
                    () -> repository.findConferenceArticlesBatch(3, 2000, 2025, 0, 100)
            );

            Assertions.assertEquals("Αποτυχία φόρτωσης batch άρθρων συνεδρίου.", exception.getMessage());
            Assertions.assertTrue(exception.getCause() instanceof SQLException);
        }
    }

    private void mockConferenceArticleResultSet() throws Exception {
        Mockito.when(resultSet.next()).thenReturn(true, false);
        Mockito.when(resultSet.wasNull()).thenReturn(false);

        Mockito.when(resultSet.getInt("article_id")).thenReturn(100);
        Mockito.when(resultSet.getString("articlekey")).thenReturn("conf/test/SIGMOD2020");
        Mockito.when(resultSet.getString("title")).thenReturn("A Conference Test Publication");
        Mockito.when(resultSet.getInt("year")).thenReturn(2020);
        Mockito.when(resultSet.getString("article_type")).thenReturn("conference");

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

    private void assertConferenceArticle(ConferenceArticleDto article) {
        Assertions.assertEquals(100, article.articleId());
        Assertions.assertEquals("conf/test/SIGMOD2020", article.articleKey());
        Assertions.assertEquals("A Conference Test Publication", article.title());
        Assertions.assertEquals(2020, article.year());
        Assertions.assertEquals("conference", article.articleType());

        Assertions.assertEquals(3, article.conferenceId());
        Assertions.assertEquals("SIGMOD", article.conferenceAcronym());
        Assertions.assertEquals("ACM SIGMOD Conference", article.conferenceTitle());
        Assertions.assertEquals(50, article.icoreId());

        Assertions.assertEquals("1-12", article.pages());
        Assertions.assertEquals("https://example.com/paper", article.ee());
        Assertions.assertEquals("https://example.com", article.url());
        Assertions.assertEquals(LocalDate.of(2020, 5, 20), article.mdate());

        Assertions.assertEquals(3L, article.authorCount());
        Assertions.assertEquals("John Smith, Maria Papadopoulou, Nick Brown", article.authors());
    }
}