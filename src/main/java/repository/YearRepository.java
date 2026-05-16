package repository;

import db.DatabaseManager;
import dto.year.AvailableYearDto;
import dto.year.YearProfileDto;
import dto.year.YearPublicationDto;
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

public class YearRepository {

    public List<AvailableYearDto> findAvailableYears() {
        String sql = SqlFileLoader.load("sql/year/available_years.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()
        ) {
            List<AvailableYearDto> results = new ArrayList<>();

            while (rs.next()) {
                results.add(mapAvailableYear(rs));
            }

            return results;

        } catch (SQLException exception) {
            throw new RuntimeException("Αποτυχία φόρτωσης διαθέσιμων χρονιών.", exception);
        }
    }

    public Optional<YearProfileDto> findYearProfile(int year) {
        String sql = SqlFileLoader
                .load("sql/year/year_profile.sql")
                .split("-- B2")[0]
                .trim();

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, year);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapYearProfile(rs));
                }

                return Optional.empty();
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Αποτυχία φόρτωσης προφίλ χρονιάς.", exception);
        }
    }

    public List<YearPublicationDto> findYearPublications(
            int year,
            String publicationType,
            int journalId,
            int conferenceId,
            int authorId
    ) {
        String sql = SqlFileLoader.load("sql/year/year_publications_report.sql");

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, year);
            statement.setString(2, publicationType);
            statement.setInt(3, journalId);
            statement.setInt(4, conferenceId);
            statement.setInt(5, authorId);

            try (ResultSet rs = statement.executeQuery()) {
                List<YearPublicationDto> results = new ArrayList<>();

                while (rs.next()) {
                    results.add(mapYearPublication(rs));
                }

                return results;
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Αποτυχία φόρτωσης δημοσιεύσεων χρονιάς.", exception);
        }
    }

    private AvailableYearDto mapAvailableYear(ResultSet rs) throws SQLException {
        return new AvailableYearDto(
                getInteger(rs, "year"),
                getLong(rs, "total_articles"),
                getLong(rs, "total_journal_articles"),
                getLong(rs, "total_conference_articles")
        );
    }

    private YearProfileDto mapYearProfile(ResultSet rs) throws SQLException {
        return new YearProfileDto(
                getInteger(rs, "year"),

                getLong(rs, "total_articles"),
                getLong(rs, "total_journal_articles"),
                getLong(rs, "total_conference_articles"),

                getLong(rs, "distinct_journals"),
                getLong(rs, "distinct_conferences"),

                getLong(rs, "total_author_occurrences"),
                getLong(rs, "distinct_authors"),
                getDouble(rs, "avg_authors_per_article")
        );
    }

    private YearPublicationDto mapYearPublication(ResultSet rs) throws SQLException {
        return new YearPublicationDto(
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