package repository;

import db.DatabaseManager;
import dto.journal.JournalArticleDto;
import dto.journal.JournalProfileDto;
import dto.journal.JournalRankingDto;
import dto.journal.JournalSearchResultDto;
import dto.journal.JournalYearlyStatsDto;
import util.SqlFileLoader;
import dto.chart.CategoryOptionDto;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JournalRepository {

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
            throw new RuntimeException("Αποτυχία αναζήτησης περιοδικών.", exception);
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
            throw new RuntimeException("Αποτυχία φόρτωσης προφίλ περιοδικού.", exception);
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
            throw new RuntimeException("Αποτυχία φόρτωσης yearly stats περιοδικού.", exception);
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
            throw new RuntimeException("Αποτυχία φόρτωσης ranking περιοδικού.", exception);
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
            throw new RuntimeException("Αποτυχία φόρτωσης άρθρων περιοδικού.", exception);
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
            throw new RuntimeException("Αποτυχία φόρτωσης batch άρθρων περιοδικού.", exception);
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
            throw new RuntimeException("Αποτυχία φόρτωσης BestSubjectArea κατηγοριών.", exception);
        }
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