package repository;

import db.DatabaseManager;
import dto.conference.ConferenceArticleDto;
import dto.conference.ConferenceProfileDto;
import dto.conference.ConferenceRankingDto;
import dto.conference.ConferenceSearchResultDto;
import dto.conference.ConferenceYearlyStatsDto;
import util.SqlFileLoader;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConferenceRepository {

    public List<ConferenceSearchResultDto> searchConferences(String searchText, int limit) {
        String sql = SqlFileLoader.load("sql/conference/search_conferences.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, searchText);
            statement.setString(2, searchText);
            statement.setInt(3, limit);

            try (ResultSet rs = statement.executeQuery()) {
                List<ConferenceSearchResultDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapConferenceSearchResult(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Αποτυχία αναζήτησης συνεδρίων.", exception);
        }
    }

    public Optional<ConferenceProfileDto> findConferenceProfile(
            int conferenceId,
            int startYear,
            int endYear
    ) {
        String sql = SqlFileLoader.load("sql/conference/conference_profile.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, conferenceId);
            statement.setInt(2, startYear);
            statement.setInt(3, endYear);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapConferenceProfile(rs));
                }

                return Optional.empty();
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Αποτυχία φόρτωσης προφίλ συνεδρίου.", exception);
        }
    }

    public List<ConferenceArticleDto> findConferenceArticlesBatch(
            int conferenceId,
            int startYear,
            int endYear,
            int lastArticleId,
            int batchSize
    ) {
        String sql = SqlFileLoader.load("sql/conference/conference_articles_report_batch.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, conferenceId);
            statement.setInt(2, startYear);
            statement.setInt(3, endYear);
            statement.setInt(4, lastArticleId);
            statement.setInt(5, batchSize);

            try (ResultSet rs = statement.executeQuery()) {
                List<ConferenceArticleDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapConferenceArticle(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Αποτυχία φόρτωσης batch άρθρων συνεδρίου.", exception);
        }
    }

    public List<ConferenceYearlyStatsDto> findConferenceYearlyStats(
            int conferenceId,
            int startYear,
            int endYear
    ) {
        String sql = SqlFileLoader.load("sql/conference/conference_yearly_linechart.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, conferenceId);
            statement.setInt(2, startYear);
            statement.setInt(3, endYear);

            try (ResultSet rs = statement.executeQuery()) {
                List<ConferenceYearlyStatsDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapConferenceYearlyStats(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Αποτυχία φόρτωσης yearly stats συνεδρίου.", exception);
        }
    }

    public Optional<ConferenceRankingDto> findConferenceRanking(int conferenceId) {
        String sql = SqlFileLoader.load("sql/conference/conference_ranking.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, conferenceId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapConferenceRanking(rs));
                }

                return Optional.empty();
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Αποτυχία φόρτωσης ranking συνεδρίου.", exception);
        }
    }

    public List<ConferenceArticleDto> findConferenceArticles(
            int conferenceId,
            int startYear,
            int endYear
    ) {
        String sql = SqlFileLoader.load("sql/conference/conference_articles_report.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, conferenceId);
            statement.setInt(2, startYear);
            statement.setInt(3, endYear);

            try (ResultSet rs = statement.executeQuery()) {
                List<ConferenceArticleDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapConferenceArticle(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Αποτυχία φόρτωσης άρθρων συνεδρίου.", exception);
        }
    }

    private ConferenceSearchResultDto mapConferenceSearchResult(ResultSet rs) throws SQLException {
        return new ConferenceSearchResultDto(
                getInteger(rs, "conference_id"),
                rs.getString("acronym"),
                rs.getString("conference_title"),
                getInteger(rs, "icore_id")
        );
    }

    private ConferenceProfileDto mapConferenceProfile(ResultSet rs) throws SQLException {
        return new ConferenceProfileDto(
                getInteger(rs, "conference_id"),
                rs.getString("acronym"),
                rs.getString("conference_title"),
                getInteger(rs, "icore_id"),

                getInteger(rs, "first_year"),
                getInteger(rs, "last_year"),
                getLong(rs, "active_years"),

                getLong(rs, "total_articles"),
                getLong(rs, "total_author_occurrences"),
                getLong(rs, "distinct_authors"),

                getDouble(rs, "avg_articles_per_year"),
                getDouble(rs, "avg_author_occurrences_per_year"),
                getDouble(rs, "avg_authors_per_article")
        );
    }

    private ConferenceYearlyStatsDto mapConferenceYearlyStats(ResultSet rs) throws SQLException {
        return new ConferenceYearlyStatsDto(
                getInteger(rs, "conference_id"),
                rs.getString("acronym"),
                rs.getString("conference_title"),
                getInteger(rs, "year"),

                getLong(rs, "total_articles"),
                getLong(rs, "total_author_occurrences"),
                getLong(rs, "distinct_authors"),
                getDouble(rs, "avg_authors_per_article")
        );
    }

    private ConferenceRankingDto mapConferenceRanking(ResultSet rs) throws SQLException {
        return new ConferenceRankingDto(
                getInteger(rs, "conference_id"),
                rs.getString("acronym"),
                rs.getString("conference_title"),
                getInteger(rs, "icore_id"),

                rs.getString("rank_label"),
                getInteger(rs, "primaryFoR_id"),
                rs.getString("primaryFoR_name")
        );
    }

    private ConferenceArticleDto mapConferenceArticle(ResultSet rs) throws SQLException {
        return new ConferenceArticleDto(
                getInteger(rs, "article_id"),
                rs.getString("articlekey"),
                rs.getString("title"),
                getInteger(rs, "year"),
                rs.getString("article_type"),

                getInteger(rs, "conference_id"),
                rs.getString("conference_acronym"),
                rs.getString("conference_title"),
                getInteger(rs, "icore_id"),

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