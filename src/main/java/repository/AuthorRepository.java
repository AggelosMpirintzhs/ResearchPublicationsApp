package repository;

import db.DatabaseManager;
import dto.author.AuthorProfileDto;
import dto.author.AuthorPublicationDto;
import dto.author.AuthorSearchResultDto;
import dto.author.AuthorYearlyStatsByTypeDto;
import dto.author.AuthorYearlyStatsDto;
import util.SqlFileLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuthorRepository {

    // Searches author records
    public List<AuthorSearchResultDto> searchAuthors(String searchText, int limit) {
        String sql = SqlFileLoader.load("sql/author/search_authors.sql");

        String prefixPattern = buildPrefixPattern(searchText);
        String wordStartPattern = buildWordStartPattern(searchText);
        String containsPattern = buildContainsPattern(searchText);

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, prefixPattern);
            statement.setString(2, wordStartPattern);
            statement.setString(3, containsPattern);

            statement.setString(4, prefixPattern);
            statement.setString(5, wordStartPattern);
            statement.setString(6, containsPattern);

            statement.setInt(7, limit);

            try (ResultSet rs = statement.executeQuery()) {
                List<AuthorSearchResultDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapAuthorSearchResult(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to search authors.", exception);
        }
    }

    // Builds prefix pattern
    private String buildPrefixPattern(String searchText) {
        return buildTokenPattern(searchText) + "%";
    }

    // Builds word pattern
    private String buildWordStartPattern(String searchText) {
        return "% " + buildTokenPattern(searchText) + "%";
    }

    // Builds contains pattern
    private String buildContainsPattern(String searchText) {
        return "%" + buildTokenPattern(searchText) + "%";
    }

    // Builds token pattern
    private String buildTokenPattern(String searchText) {
        if (searchText == null || searchText.isBlank()) {
            return "";
        }

        return searchText
                .trim()
                .replaceAll("\\s+", "%");
    }

    // Finds author profile
    public Optional<AuthorProfileDto> findAuthorProfile(
            int authorId,
            int startYear,
            int endYear
    ) {
        String sql = SqlFileLoader.load("sql/author/author_profile.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, authorId);
            statement.setInt(2, startYear);
            statement.setInt(3, endYear);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapAuthorProfile(rs));
                }

                return Optional.empty();
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load author profile.", exception);
        }
    }

    // Finds yearly stats
    public List<AuthorYearlyStatsDto> findAuthorYearlyStats(
            int authorId,
            int startYear,
            int endYear
    ) {
        String sql = SqlFileLoader.load("sql/author/author_yearly_stats.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, authorId);
            statement.setInt(2, startYear);
            statement.setInt(3, endYear);

            try (ResultSet rs = statement.executeQuery()) {
                List<AuthorYearlyStatsDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapAuthorYearlyStats(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load author yearly stats.", exception);
        }
    }

    // Finds typed stats
    public List<AuthorYearlyStatsByTypeDto> findAuthorYearlyStatsByType(
            int authorId,
            int startYear,
            int endYear
    ) {
        String sql = SqlFileLoader.load("sql/author/author_yearly_stats_by_type.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, authorId);
            statement.setInt(2, startYear);
            statement.setInt(3, endYear);

            try (ResultSet rs = statement.executeQuery()) {
                List<AuthorYearlyStatsByTypeDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapAuthorYearlyStatsByType(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load author typed stats.", exception);
        }
    }

    // Finds publications batch
    public List<AuthorPublicationDto> findAuthorPublicationsBatch(
            int authorId,
            int startYear,
            int endYear,
            int lastArticleId,
            int batchSize
    ) {
        String sql = SqlFileLoader.load("sql/author/author_publications_report_batch.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, authorId);
            statement.setInt(2, startYear);
            statement.setInt(3, endYear);
            statement.setInt(4, lastArticleId);
            statement.setInt(5, batchSize);

            try (ResultSet rs = statement.executeQuery()) {
                List<AuthorPublicationDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapAuthorPublication(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load author publications batch.", exception);
        }
    }

    // Maps search result
    private AuthorSearchResultDto mapAuthorSearchResult(ResultSet rs) throws SQLException {
        return new AuthorSearchResultDto(
                getInteger(rs, "author_id"),
                rs.getString("author_name")
        );
    }

    // Maps author profile
    private AuthorProfileDto mapAuthorProfile(ResultSet rs) throws SQLException {
        return new AuthorProfileDto(
                getInteger(rs, "author_id"),
                rs.getString("author_name"),

                getInteger(rs, "first_year"),
                getInteger(rs, "last_year"),
                getLong(rs, "active_years"),

                getLong(rs, "total_articles"),
                getLong(rs, "total_journal_articles"),
                getLong(rs, "total_conference_articles"),

                getLong(rs, "distinct_journals"),
                getLong(rs, "distinct_conferences"),

                getDouble(rs, "avg_articles_per_year")
        );
    }

    // Maps yearly stats
    private AuthorYearlyStatsDto mapAuthorYearlyStats(ResultSet rs) throws SQLException {
        return new AuthorYearlyStatsDto(
                getInteger(rs, "year"),
                getLong(rs, "total_articles")
        );
    }

    // Maps typed stats
    private AuthorYearlyStatsByTypeDto mapAuthorYearlyStatsByType(ResultSet rs) throws SQLException {
        return new AuthorYearlyStatsByTypeDto(
                getInteger(rs, "year"),
                getLong(rs, "total_articles"),
                getLong(rs, "total_journal_articles"),
                getLong(rs, "total_conference_articles")
        );
    }

    // Maps author publication
    private AuthorPublicationDto mapAuthorPublication(ResultSet rs) throws SQLException {
        return new AuthorPublicationDto(
                getInteger(rs, "article_id"),
                rs.getString("articlekey"),
                rs.getString("title"),
                getInteger(rs, "year"),
                rs.getString("article_type"),

                getInteger(rs, "journal_id"),
                rs.getString("journal_name"),
                rs.getString("volume"),
                rs.getString("number"),

                getInteger(rs, "conference_id"),
                rs.getString("conference_acronym"),
                rs.getString("conference_title"),

                rs.getString("pages"),
                rs.getString("ee"),
                rs.getString("url"),

                getLong(rs, "author_count"),
                rs.getString("authors")
        );
    }

    // Gets integer value
    private Integer getInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    // Gets long value
    private Long getLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    // Gets double value
    private Double getDouble(ResultSet rs, String columnName) throws SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? null : value;
    }
}