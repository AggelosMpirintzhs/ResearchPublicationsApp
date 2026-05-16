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

    public List<AuthorSearchResultDto> searchAuthors(String searchText, int limit) {
        String sql = SqlFileLoader.load("sql/author/search_authors.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, searchText);
            statement.setInt(2, limit);

            try (ResultSet rs = statement.executeQuery()) {
                List<AuthorSearchResultDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapAuthorSearchResult(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Αποτυχία αναζήτησης συγγραφέων.", exception);
        }
    }

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
            throw new RuntimeException("Αποτυχία φόρτωσης προφίλ συγγραφέα.", exception);
        }
    }

    public List<AuthorYearlyStatsDto> findAuthorYearlyStats(
            int authorId,
            int startYear,
            int endYear
    ) {
        String sql = SqlFileLoader.load("sql/author/author_yearly_linechart.sql");

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
            throw new RuntimeException("Αποτυχία φόρτωσης yearly stats συγγραφέα.", exception);
        }
    }

    public List<AuthorYearlyStatsByTypeDto> findAuthorYearlyStatsByType(
            int authorId,
            int startYear,
            int endYear
    ) {
        String sql = SqlFileLoader.load("sql/author/author_yearly_linechart_by_type.sql");

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
            throw new RuntimeException("Αποτυχία φόρτωσης yearly stats συγγραφέα ανά τύπο.", exception);
        }
    }

    public List<AuthorPublicationDto> findAuthorPublications(
            int authorId,
            int startYear,
            int endYear
    ) {
        String sql = SqlFileLoader.load("sql/author/author_publications_report.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, authorId);
            statement.setInt(2, startYear);
            statement.setInt(3, endYear);

            try (ResultSet rs = statement.executeQuery()) {
                List<AuthorPublicationDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapAuthorPublication(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Αποτυχία φόρτωσης δημοσιεύσεων συγγραφέα.", exception);
        }
    }

    private AuthorSearchResultDto mapAuthorSearchResult(ResultSet rs) throws SQLException {
        return new AuthorSearchResultDto(
                getInteger(rs, "author_id"),
                rs.getString("author_name")
        );
    }

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

    private AuthorYearlyStatsDto mapAuthorYearlyStats(ResultSet rs) throws SQLException {
        return new AuthorYearlyStatsDto(
                getInteger(rs, "year"),
                getLong(rs, "total_articles")
        );
    }

    private AuthorYearlyStatsByTypeDto mapAuthorYearlyStatsByType(ResultSet rs) throws SQLException {
        return new AuthorYearlyStatsByTypeDto(
                getInteger(rs, "year"),
                getLong(rs, "total_articles"),
                getLong(rs, "total_journal_articles"),
                getLong(rs, "total_conference_articles")
        );
    }

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
}