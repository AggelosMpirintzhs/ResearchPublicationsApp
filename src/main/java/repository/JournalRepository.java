package repository;

import db.DatabaseManager;
import dto.chart.CategoryOptionDto;
import dto.chart.CategoryTrendDto;
import dto.chart.PublisherOptionDto;
import dto.chart.PublisherQuartileStatsDto;
import dto.chart.ScatterPlotPointDto;
import dto.journal.JournalArticleDto;
import dto.journal.JournalProfileDto;
import dto.journal.JournalRankingDto;
import dto.journal.JournalSearchResultDto;
import dto.journal.JournalYearlyStatsDto;
import util.SqlFileLoader;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JournalRepository {

    private static final int MAX_RANKING_SCATTER_LIMIT = 1000;

    private static final Map<String, String> RANKING_METRIC_COLUMNS = Map.ofEntries(
            Map.entry("Total Docs", "total_docs"),
            Map.entry("Total Docs 3y", "total_docs_3y"),
            Map.entry("Total Refs", "total_refs"),
            Map.entry("Total Cites 3y", "total_cites_3y"),
            Map.entry("Citable Docs 3y", "citable_docs_3y"),
            Map.entry("Cites / Doc 2y", "cites_per_doc_2y"),
            Map.entry("Refs / Doc", "refs_per_doc"),
            Map.entry("SJR", "sjr_index"),
            Map.entry("Cite Score", "cite_score"),
            Map.entry("H index", "h_index")
    );

    public List<ScatterPlotPointDto> findJournalRankingScatterData(
            String xMetric,
            String yMetric,
            Integer limit
    ) {
        String xColumn = RANKING_METRIC_COLUMNS.get(xMetric);
        String yColumn = RANKING_METRIC_COLUMNS.get(yMetric);

        if (xColumn == null || yColumn == null) {
            throw new IllegalArgumentException("Unknown ranking metric.");
        }

        boolean useLimit = limit != null && limit > 0;
        int safeLimit = useLimit ? normalizeRankingScatterLimit(limit) : 0;
        String limitClause = useLimit ? "LIMIT ?" : "";

        String sqlTemplate = SqlFileLoader.load("sql/journal/journal_ranking_scatter.sql");

        String sql = sqlTemplate.formatted(
                xColumn,
                yColumn,
                xColumn,
                yColumn,
                xColumn,
                yColumn,
                limitClause
        );

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            if (useLimit) {
                statement.setInt(1, safeLimit);
            }

            try (ResultSet rs = statement.executeQuery()) {
                List<ScatterPlotPointDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(new ScatterPlotPointDto(
                            getInteger(rs, "id"),
                            rs.getString("label"),
                            getDouble(rs, "x_value"),
                            getDouble(rs, "y_value")
                    ));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load journal ranking scatter data.", exception);
        }
    }

    public List<JournalSearchResultDto> searchJournals(String searchText, int limit) {
        String sql = SqlFileLoader.load("sql/journal/search_journals.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, searchText);
            statement.setInt(2, limit);

            try (ResultSet rs = statement.executeQuery()) {
                List<JournalSearchResultDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapJournalSearchResult(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to search journals.", exception);
        }
    }

    public Optional<JournalProfileDto> findJournalProfile(
            int journalId,
            int startYear,
            int endYear
    ) {
        String sql = SqlFileLoader.load("sql/journal/journal_profile.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, journalId);
            statement.setInt(2, startYear);
            statement.setInt(3, endYear);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapJournalProfile(rs));
                }

                return Optional.empty();
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load journal profile.", exception);
        }
    }

    public List<JournalYearlyStatsDto> findJournalYearlyStats(
            int journalId,
            int startYear,
            int endYear
    ) {
        String sql = SqlFileLoader.load("sql/journal/journal_yearly_linechart.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, journalId);
            statement.setInt(2, startYear);
            statement.setInt(3, endYear);

            try (ResultSet rs = statement.executeQuery()) {
                List<JournalYearlyStatsDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapJournalYearlyStats(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load journal yearly stats.", exception);
        }
    }

    public Optional<JournalRankingDto> findJournalRanking(int journalId) {
        String sql = SqlFileLoader.load("sql/journal/journal_ranking.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, journalId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapJournalRanking(rs));
                }

                return Optional.empty();
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load journal ranking.", exception);
        }
    }

    public List<JournalArticleDto> findJournalArticles(
            int journalId,
            int startYear,
            int endYear
    ) {
        String sql = SqlFileLoader.load("sql/journal/journal_articles_report.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, journalId);
            statement.setInt(2, startYear);
            statement.setInt(3, endYear);

            try (ResultSet rs = statement.executeQuery()) {
                List<JournalArticleDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapJournalArticle(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load journal articles.", exception);
        }
    }

    public List<JournalArticleDto> findJournalArticlesBatch(
            int journalId,
            int startYear,
            int endYear,
            int lastArticleId,
            int batchSize
    ) {
        String sql = SqlFileLoader.load("sql/journal/journal_articles_report_batch.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, journalId);
            statement.setInt(2, startYear);
            statement.setInt(3, endYear);
            statement.setInt(4, lastArticleId);
            statement.setInt(5, batchSize);

            try (ResultSet rs = statement.executeQuery()) {
                List<JournalArticleDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapJournalArticle(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load journal article batch.", exception);
        }
    }

    public List<CategoryOptionDto> findBestSubjectAreas() {
        String sql = SqlFileLoader.load("sql/journal/journal_best_subject_areas.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()
        ) {
            List<CategoryOptionDto> results = new ArrayList<>();

            while (rs.next()) {
                results.add(
                        new CategoryOptionDto(
                                String.valueOf(rs.getInt("category_id")),
                                rs.getString("category_name")
                        )
                );
            }

            return results;

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load BestSubjectArea categories.", exception);
        }
    }

    public List<CategoryTrendDto> findJournalBestSubjectAreaYearlyTrends(
            String categoryFilter,
            int startYear,
            int endYear
    ) {
        String sql = SqlFileLoader.load("sql/journal/journal_best_subject_area_yearly_trends.sql");
        String safeCategoryFilter = categoryFilter == null ? "" : categoryFilter.trim();

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, safeCategoryFilter);
            statement.setString(2, safeCategoryFilter);
            statement.setInt(3, startYear);
            statement.setInt(4, endYear);

            try (ResultSet rs = statement.executeQuery()) {
                List<CategoryTrendDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapCategoryTrend(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load BestSubjectArea yearly trends.", exception);
        }
    }

    public List<PublisherOptionDto> getPublisherOptions(
            String publisherFilter,
            int limit
    ) {
        String sql = SqlFileLoader.load("sql/journal/publisher_options.sql");

        String safePublisherFilter = publisherFilter == null ? "" : publisherFilter.trim();
        int safeLimit = limit <= 0 ? 20 : limit;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, safePublisherFilter);
            statement.setString(2, safePublisherFilter);
            statement.setString(3, safePublisherFilter);
            statement.setInt(4, safeLimit);

            try (ResultSet rs = statement.executeQuery()) {
                List<PublisherOptionDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapPublisherOption(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load publisher options.", exception);
        }
    }

    public List<PublisherQuartileStatsDto> getPublisherQuartilePublicationStatsById(int publisherId) {
        String sql = SqlFileLoader.load("sql/journal/publisher_quartile_publication_stats_by_id.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, publisherId);

            try (ResultSet rs = statement.executeQuery()) {
                List<PublisherQuartileStatsDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapPublisherQuartileStats(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load publisher quartile publication stats.", exception);
        }
    }

    public List<PublisherQuartileStatsDto> getPublisherQuartileStats(
            String publisherFilter,
            int limit
    ) {
        List<PublisherOptionDto> publisherOptions = getPublisherOptions(publisherFilter, limit);
        List<PublisherQuartileStatsDto> results = new ArrayList<>();

        for (PublisherOptionDto publisherOption : publisherOptions) {
            results.addAll(getPublisherQuartilePublicationStatsById(publisherOption.publisherId()));
        }

        return results;
    }

    private int normalizeRankingScatterLimit(Integer limit) {
        return Math.min(limit, MAX_RANKING_SCATTER_LIMIT);
    }

    private JournalSearchResultDto mapJournalSearchResult(ResultSet rs) throws SQLException {
        return new JournalSearchResultDto(
                getInteger(rs, "journal_id"),
                rs.getString("journal_name"),
                getInteger(rs, "publisher_id"),
                rs.getString("publisher_name")
        );
    }

    private JournalProfileDto mapJournalProfile(ResultSet rs) throws SQLException {
        return new JournalProfileDto(
                getInteger(rs, "journal_id"),
                rs.getString("journal_name"),
                getInteger(rs, "publisher_id"),
                rs.getString("publisher_name"),

                getInteger(rs, "first_year"),
                getInteger(rs, "last_year"),
                getLong(rs, "active_years"),

                getLong(rs, "total_articles"),
                getLong(rs, "total_author_occurrences"),
                getLong(rs, "distinct_authors_all_time"),

                getDouble(rs, "avg_articles_per_year"),
                getDouble(rs, "avg_author_occurrences_per_year"),
                getDouble(rs, "avg_authors_per_article")
        );
    }

    private JournalYearlyStatsDto mapJournalYearlyStats(ResultSet rs) throws SQLException {
        return new JournalYearlyStatsDto(
                getInteger(rs, "journal_id"),
                rs.getString("journal_name"),
                getInteger(rs, "year"),

                getLong(rs, "total_articles"),
                getLong(rs, "total_author_occurrences"),
                getLong(rs, "distinct_authors"),
                getDouble(rs, "avg_authors_per_article")
        );
    }

    private JournalRankingDto mapJournalRanking(ResultSet rs) throws SQLException {
        return new JournalRankingDto(
                getInteger(rs, "journal_id"),
                rs.getString("journal_name"),
                getInteger(rs, "publisher_id"),
                rs.getString("publisher_name"),

                getInteger(rs, "ranking_position"),
                rs.getString("best_quartile"),
                getDouble(rs, "sjr_index"),
                getDouble(rs, "cite_score"),
                getInteger(rs, "h_index"),

                getInteger(rs, "total_docs"),
                getInteger(rs, "total_docs_3y"),
                getInteger(rs, "total_refs"),
                getInteger(rs, "total_cites_3y"),
                getInteger(rs, "citable_docs_3y"),
                getDouble(rs, "cites_per_doc_2y"),
                getDouble(rs, "refs_per_doc"),

                getInteger(rs, "best_area_id"),
                rs.getString("best_subject_area")
        );
    }

    private JournalArticleDto mapJournalArticle(ResultSet rs) throws SQLException {
        return new JournalArticleDto(
                getInteger(rs, "article_id"),
                rs.getString("articlekey"),
                rs.getString("title"),
                getInteger(rs, "year"),
                rs.getString("article_type"),

                getInteger(rs, "journal_id"),
                rs.getString("journal_name"),
                getInteger(rs, "source_id"),
                rs.getString("volume"),
                rs.getString("number"),

                rs.getString("pages"),
                rs.getString("ee"),
                rs.getString("url"),
                getLocalDate(rs, "mdate"),

                getLong(rs, "author_count"),
                rs.getString("authors")
        );
    }

    private CategoryTrendDto mapCategoryTrend(ResultSet rs) throws SQLException {
        return new CategoryTrendDto(
                rs.getString("category"),
                rs.getInt("year"),
                rs.getLong("count")
        );
    }

    private PublisherOptionDto mapPublisherOption(ResultSet rs) throws SQLException {
        return new PublisherOptionDto(
                rs.getInt("publisher_id"),
                rs.getString("publisher_name"),
                rs.getLong("total_publications")
        );
    }

    private PublisherQuartileStatsDto mapPublisherQuartileStats(ResultSet rs) throws SQLException {
        return new PublisherQuartileStatsDto(
                rs.getInt("publisher_id"),
                rs.getString("publisher_name"),
                rs.getString("quartile"),
                rs.getLong("publication_count"),
                rs.getLong("total_publications")
        );
    }

    private Integer getInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private Long getLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private Double getDouble(ResultSet rs, String columnName) throws SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? null : value;
    }

    private LocalDate getLocalDate(ResultSet rs, String columnName) throws SQLException {
        Date date = rs.getDate(columnName);
        return date == null ? null : date.toLocalDate();
    }
}