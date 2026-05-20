package repository;

import db.DatabaseManager;
import dto.chart.CategoryOptionDto;
import dto.journal.JournalArticleDto;
import dto.journal.JournalProfileDto;
import dto.journal.JournalRankingDto;
import dto.journal.JournalSearchResultDto;
import dto.journal.JournalYearlyStatsDto;
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

class JournalRepositoryTest {

    private JournalRepository repository;

    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() {
        repository = new JournalRepository();

        connection = Mockito.mock(Connection.class);
        statement = Mockito.mock(PreparedStatement.class);
        resultSet = Mockito.mock(ResultSet.class);
    }

    @Test
    void searchJournals_shouldReturnMatchingJournals() throws Exception {
        String sql = "search journals sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/journal/search_journals.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            Mockito.when(resultSet.next()).thenReturn(true, true, false);
            Mockito.when(resultSet.wasNull()).thenReturn(false);

            Mockito.when(resultSet.getInt("journal_id")).thenReturn(1, 2);
            Mockito.when(resultSet.getString("journal_name")).thenReturn("Journal One", "Journal Two");
            Mockito.when(resultSet.getInt("publisher_id")).thenReturn(10, 20);
            Mockito.when(resultSet.getString("publisher_name")).thenReturn("Publisher A", "Publisher B");

            List<JournalSearchResultDto> results = repository.searchJournals("data", 10);

            Assertions.assertEquals(2, results.size());

            Assertions.assertEquals(1, results.get(0).journalId());
            Assertions.assertEquals("Journal One", results.get(0).journalName());
            Assertions.assertEquals(10, results.get(0).publisherId());
            Assertions.assertEquals("Publisher A", results.get(0).publisherName());

            Assertions.assertEquals(2, results.get(1).journalId());
            Assertions.assertEquals("Journal Two", results.get(1).journalName());
            Assertions.assertEquals(20, results.get(1).publisherId());
            Assertions.assertEquals("Publisher B", results.get(1).publisherName());

            Mockito.verify(statement).setString(1, "data");
            Mockito.verify(statement).setInt(2, 10);
            Mockito.verify(statement).executeQuery();
        }
    }

    @Test
    void findJournalProfile_whenJournalExists_shouldReturnProfile() throws Exception {
        String sql = "journal profile sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/journal/journal_profile.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            Mockito.when(resultSet.next()).thenReturn(true);
            Mockito.when(resultSet.wasNull()).thenReturn(false);

            Mockito.when(resultSet.getInt("journal_id")).thenReturn(5);
            Mockito.when(resultSet.getString("journal_name")).thenReturn("Information Systems Journal");
            Mockito.when(resultSet.getInt("publisher_id")).thenReturn(100);
            Mockito.when(resultSet.getString("publisher_name")).thenReturn("Test Publisher");

            Mockito.when(resultSet.getInt("first_year")).thenReturn(2010);
            Mockito.when(resultSet.getInt("last_year")).thenReturn(2020);
            Mockito.when(resultSet.getLong("active_years")).thenReturn(11L);

            Mockito.when(resultSet.getLong("total_articles")).thenReturn(150L);
            Mockito.when(resultSet.getLong("total_author_occurrences")).thenReturn(450L);
            Mockito.when(resultSet.getLong("distinct_authors_all_time")).thenReturn(300L);

            Mockito.when(resultSet.getDouble("avg_articles_per_year")).thenReturn(13.64);
            Mockito.when(resultSet.getDouble("avg_author_occurrences_per_year")).thenReturn(40.91);
            Mockito.when(resultSet.getDouble("avg_authors_per_article")).thenReturn(3.0);

            Optional<JournalProfileDto> result =
                    repository.findJournalProfile(5, 2000, 2025);

            Assertions.assertTrue(result.isPresent());

            JournalProfileDto profile = result.get();

            Assertions.assertEquals(5, profile.journalId());
            Assertions.assertEquals("Information Systems Journal", profile.journalName());
            Assertions.assertEquals(100, profile.publisherId());
            Assertions.assertEquals("Test Publisher", profile.publisherName());

            Assertions.assertEquals(2010, profile.firstYear());
            Assertions.assertEquals(2020, profile.lastYear());
            Assertions.assertEquals(11L, profile.activeYears());

            Assertions.assertEquals(150L, profile.totalArticles());
            Assertions.assertEquals(450L, profile.totalAuthorOccurrences());
            Assertions.assertEquals(300L, profile.distinctAuthorsAllTime());

            Assertions.assertEquals(13.64, profile.avgArticlesPerYear());
            Assertions.assertEquals(40.91, profile.avgAuthorOccurrencesPerYear());
            Assertions.assertEquals(3.0, profile.avgAuthorsPerArticle());

            Mockito.verify(statement).setInt(1, 5);
            Mockito.verify(statement).setInt(2, 2000);
            Mockito.verify(statement).setInt(3, 2025);
        }
    }

    @Test
    void findJournalProfile_whenJournalDoesNotExist_shouldReturnEmptyOptional() throws Exception {
        String sql = "journal profile sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/journal/journal_profile.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);
            Mockito.when(resultSet.next()).thenReturn(false);

            Optional<JournalProfileDto> result =
                    repository.findJournalProfile(99, 2000, 2025);

            Assertions.assertTrue(result.isEmpty());

            Mockito.verify(statement).setInt(1, 99);
            Mockito.verify(statement).setInt(2, 2000);
            Mockito.verify(statement).setInt(3, 2025);
        }
    }

    @Test
    void findJournalYearlyStats_shouldReturnYearlyStats() throws Exception {
        String sql = "journal yearly stats sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/journal/journal_yearly_linechart.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            Mockito.when(resultSet.next()).thenReturn(true, true, false);
            Mockito.when(resultSet.wasNull()).thenReturn(false);

            Mockito.when(resultSet.getInt("journal_id")).thenReturn(5, 5);
            Mockito.when(resultSet.getString("journal_name")).thenReturn(
                    "Information Systems Journal",
                    "Information Systems Journal"
            );
            Mockito.when(resultSet.getInt("year")).thenReturn(2020, 2021);

            Mockito.when(resultSet.getLong("total_articles")).thenReturn(40L, 45L);
            Mockito.when(resultSet.getLong("total_author_occurrences")).thenReturn(120L, 135L);
            Mockito.when(resultSet.getLong("distinct_authors")).thenReturn(100L, 110L);
            Mockito.when(resultSet.getDouble("avg_authors_per_article")).thenReturn(3.0, 3.1);

            List<JournalYearlyStatsDto> results =
                    repository.findJournalYearlyStats(5, 2020, 2021);

            Assertions.assertEquals(2, results.size());

            Assertions.assertEquals(5, results.get(0).journalId());
            Assertions.assertEquals("Information Systems Journal", results.get(0).journalName());
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

            Mockito.verify(statement).setInt(1, 5);
            Mockito.verify(statement).setInt(2, 2020);
            Mockito.verify(statement).setInt(3, 2021);
        }
    }

    @Test
    void findJournalRanking_whenRankingExists_shouldReturnRanking() throws Exception {
        String sql = "journal ranking sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/journal/journal_ranking.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            Mockito.when(resultSet.next()).thenReturn(true);
            Mockito.when(resultSet.wasNull()).thenReturn(false);

            Mockito.when(resultSet.getInt("journal_id")).thenReturn(5);
            Mockito.when(resultSet.getString("journal_name")).thenReturn("Information Systems Journal");
            Mockito.when(resultSet.getInt("publisher_id")).thenReturn(100);
            Mockito.when(resultSet.getString("publisher_name")).thenReturn("Test Publisher");

            Mockito.when(resultSet.getInt("ranking_position")).thenReturn(12);
            Mockito.when(resultSet.getString("best_quartile")).thenReturn("Q1");
            Mockito.when(resultSet.getDouble("sjr_index")).thenReturn(2.5);
            Mockito.when(resultSet.getDouble("cite_score")).thenReturn(8.4);
            Mockito.when(resultSet.getInt("h_index")).thenReturn(90);

            Mockito.when(resultSet.getInt("total_docs")).thenReturn(1000);
            Mockito.when(resultSet.getInt("total_docs_3y")).thenReturn(300);
            Mockito.when(resultSet.getInt("total_refs")).thenReturn(5000);
            Mockito.when(resultSet.getInt("total_cites_3y")).thenReturn(2500);
            Mockito.when(resultSet.getInt("citable_docs_3y")).thenReturn(280);
            Mockito.when(resultSet.getDouble("cites_per_doc_2y")).thenReturn(4.2);
            Mockito.when(resultSet.getDouble("refs_per_doc")).thenReturn(35.5);

            Mockito.when(resultSet.getInt("best_area_id")).thenReturn(4602);
            Mockito.when(resultSet.getString("best_subject_area")).thenReturn("Artificial Intelligence");

            Optional<JournalRankingDto> result =
                    repository.findJournalRanking(5);

            Assertions.assertTrue(result.isPresent());

            JournalRankingDto ranking = result.get();

            Assertions.assertEquals(5, ranking.journalId());
            Assertions.assertEquals("Information Systems Journal", ranking.journalName());
            Assertions.assertEquals(100, ranking.publisherId());
            Assertions.assertEquals("Test Publisher", ranking.publisherName());

            Assertions.assertEquals(12, ranking.rankingPosition());
            Assertions.assertEquals("Q1", ranking.bestQuartile());
            Assertions.assertEquals(2.5, ranking.sjrIndex());
            Assertions.assertEquals(8.4, ranking.citeScore());
            Assertions.assertEquals(90, ranking.hIndex());

            Assertions.assertEquals(1000, ranking.totalDocs());
            Assertions.assertEquals(300, ranking.totalDocs3y());
            Assertions.assertEquals(5000, ranking.totalRefs());
            Assertions.assertEquals(2500, ranking.totalCites3y());
            Assertions.assertEquals(280, ranking.citableDocs3y());
            Assertions.assertEquals(4.2, ranking.citesPerDoc2y());
            Assertions.assertEquals(35.5, ranking.refsPerDoc());

            Assertions.assertEquals(4602, ranking.bestAreaId());
            Assertions.assertEquals("Artificial Intelligence", ranking.bestSubjectArea());

            Mockito.verify(statement).setInt(1, 5);
        }
    }

    @Test
    void findJournalRanking_whenRankingDoesNotExist_shouldReturnEmptyOptional() throws Exception {
        String sql = "journal ranking sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/journal/journal_ranking.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);
            Mockito.when(resultSet.next()).thenReturn(false);

            Optional<JournalRankingDto> result =
                    repository.findJournalRanking(99);

            Assertions.assertTrue(result.isEmpty());

            Mockito.verify(statement).setInt(1, 99);
        }
    }

    @Test
    void findJournalArticles_shouldReturnArticles() throws Exception {
        String sql = "journal articles sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/journal/journal_articles_report.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            mockJournalArticleResultSet();

            List<JournalArticleDto> results =
                    repository.findJournalArticles(5, 2000, 2025);

            Assertions.assertEquals(1, results.size());

            JournalArticleDto article = results.get(0);

            assertJournalArticle(article);

            Mockito.verify(statement).setInt(1, 5);
            Mockito.verify(statement).setInt(2, 2000);
            Mockito.verify(statement).setInt(3, 2025);
        }
    }

    @Test
    void findJournalArticlesBatch_shouldReturnArticles() throws Exception {
        String sql = "journal articles batch sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/journal/journal_articles_report_batch.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql)).thenReturn(statement);
            Mockito.when(statement.executeQuery()).thenReturn(resultSet);

            mockJournalArticleResultSet();

            List<JournalArticleDto> results =
                    repository.findJournalArticlesBatch(5, 2000, 2025, 0, 100);

            Assertions.assertEquals(1, results.size());

            JournalArticleDto article = results.get(0);

            assertJournalArticle(article);

            Mockito.verify(statement).setInt(1, 5);
            Mockito.verify(statement).setInt(2, 2000);
            Mockito.verify(statement).setInt(3, 2025);
            Mockito.verify(statement).setInt(4, 0);
            Mockito.verify(statement).setInt(5, 100);
        }
    }

    @Test
    void findBestSubjectAreas_shouldReturnCategories() throws Exception {
        String sql = "best subject areas sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/journal/journal_best_subject_areas.sql"))
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

            List<CategoryOptionDto> results = repository.findBestSubjectAreas();

            Assertions.assertEquals(2, results.size());

            Mockito.verify(statement).executeQuery();
        }
    }

    @Test
    void findJournalArticlesBatch_whenSqlExceptionOccurs_shouldThrowRuntimeException() throws Exception {
        String sql = "journal articles batch sql";

        try (
                MockedStatic<SqlFileLoader> sqlLoaderMock = Mockito.mockStatic(SqlFileLoader.class);
                MockedStatic<DatabaseManager> databaseManagerMock = Mockito.mockStatic(DatabaseManager.class)
        ) {
            sqlLoaderMock
                    .when(() -> SqlFileLoader.load("sql/journal/journal_articles_report_batch.sql"))
                    .thenReturn(sql);

            databaseManagerMock
                    .when(DatabaseManager::getConnection)
                    .thenReturn(connection);

            Mockito.when(connection.prepareStatement(sql))
                    .thenThrow(new SQLException("Database error"));

            RuntimeException exception = Assertions.assertThrows(
                    RuntimeException.class,
                    () -> repository.findJournalArticlesBatch(5, 2000, 2025, 0, 100)
            );

            Assertions.assertEquals("Αποτυχία φόρτωσης batch άρθρων περιοδικού.", exception.getMessage());
            Assertions.assertTrue(exception.getCause() instanceof SQLException);
        }
    }

    private void mockJournalArticleResultSet() throws Exception {
        Mockito.when(resultSet.next()).thenReturn(true, false);
        Mockito.when(resultSet.wasNull()).thenReturn(false);

        Mockito.when(resultSet.getInt("article_id")).thenReturn(100);
        Mockito.when(resultSet.getString("articlekey")).thenReturn("journals/test/Journal2020");
        Mockito.when(resultSet.getString("title")).thenReturn("A Journal Test Publication");
        Mockito.when(resultSet.getInt("year")).thenReturn(2020);
        Mockito.when(resultSet.getString("article_type")).thenReturn("journal");

        Mockito.when(resultSet.getInt("journal_id")).thenReturn(5);
        Mockito.when(resultSet.getString("journal_name")).thenReturn("Information Systems Journal");
        Mockito.when(resultSet.getInt("source_id")).thenReturn(1234);
        Mockito.when(resultSet.getString("volume")).thenReturn("10");
        Mockito.when(resultSet.getString("number")).thenReturn("2");

        Mockito.when(resultSet.getString("pages")).thenReturn("1-12");
        Mockito.when(resultSet.getString("ee")).thenReturn("https://example.com/paper");
        Mockito.when(resultSet.getString("url")).thenReturn("https://example.com");

        Mockito.when(resultSet.getDate("mdate")).thenReturn(Date.valueOf("2020-05-20"));

        Mockito.when(resultSet.getLong("author_count")).thenReturn(3L);
        Mockito.when(resultSet.getString("authors")).thenReturn("John Smith, Maria Papadopoulou, Nick Brown");
    }

    private void assertJournalArticle(JournalArticleDto article) {
        Assertions.assertEquals(100, article.articleId());
        Assertions.assertEquals("journals/test/Journal2020", article.articleKey());
        Assertions.assertEquals("A Journal Test Publication", article.title());
        Assertions.assertEquals(2020, article.year());
        Assertions.assertEquals("journal", article.articleType());

        Assertions.assertEquals(5, article.journalId());
        Assertions.assertEquals("Information Systems Journal", article.journalName());
        Assertions.assertEquals(1234, article.sourceId());
        Assertions.assertEquals("10", article.volume());
        Assertions.assertEquals("2", article.number());

        Assertions.assertEquals("1-12", article.pages());
        Assertions.assertEquals("https://example.com/paper", article.ee());
        Assertions.assertEquals("https://example.com", article.url());
        Assertions.assertEquals(LocalDate.of(2020, 5, 20), article.mdate());

        Assertions.assertEquals(3L, article.authorCount());
        Assertions.assertEquals("John Smith, Maria Papadopoulou, Nick Brown", article.authors());
    }
}