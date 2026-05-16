package service;

import dto.author.AuthorProfileDto;
import dto.author.AuthorPublicationDto;
import dto.author.AuthorSearchResultDto;
import dto.author.AuthorYearlyStatsByTypeDto;
import dto.author.AuthorYearlyStatsDto;
import repository.AuthorRepository;

import java.util.List;
import java.util.Optional;

public class AuthorService {

    private static final int DEFAULT_START_YEAR = 0;
    private static final int DEFAULT_END_YEAR = 9999;

    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_LIMIT = 100;

    private final AuthorRepository authorRepository;

    public AuthorService() {
        this.authorRepository = new AuthorRepository();
    }

    public List<AuthorSearchResultDto> searchAuthors(String searchText, Integer limit) {
        String normalizedSearchText = normalizeSearchText(searchText);

        if (normalizedSearchText.isEmpty()) {
            return List.of();
        }

        int safeLimit = normalizeLimit(limit);

        return authorRepository.searchAuthors(normalizedSearchText, safeLimit);
    }

    public Optional<AuthorProfileDto> getAuthorProfile(
            int authorId,
            Integer startYear,
            Integer endYear
    ) {
        validateId(authorId, "authorId");

        YearRange yearRange = normalizeYearRange(startYear, endYear);

        return authorRepository.findAuthorProfile(
                authorId,
                yearRange.startYear(),
                yearRange.endYear()
        );
    }

    public List<AuthorYearlyStatsDto> getAuthorYearlyStats(
            int authorId,
            Integer startYear,
            Integer endYear
    ) {
        validateId(authorId, "authorId");

        YearRange yearRange = normalizeYearRange(startYear, endYear);

        return authorRepository.findAuthorYearlyStats(
                authorId,
                yearRange.startYear(),
                yearRange.endYear()
        );
    }

    public List<AuthorYearlyStatsByTypeDto> getAuthorYearlyStatsByType(
            int authorId,
            Integer startYear,
            Integer endYear
    ) {
        validateId(authorId, "authorId");

        YearRange yearRange = normalizeYearRange(startYear, endYear);

        return authorRepository.findAuthorYearlyStatsByType(
                authorId,
                yearRange.startYear(),
                yearRange.endYear()
        );
    }

    public List<AuthorPublicationDto> getAuthorPublications(
            int authorId,
            Integer startYear,
            Integer endYear
    ) {
        validateId(authorId, "authorId");

        YearRange yearRange = normalizeYearRange(startYear, endYear);

        return authorRepository.findAuthorPublications(
                authorId,
                yearRange.startYear(),
                yearRange.endYear()
        );
    }

    private String normalizeSearchText(String searchText) {
        if (searchText == null) {
            return "";
        }

        return searchText.trim();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_SEARCH_LIMIT;
        }

        return Math.min(limit, MAX_SEARCH_LIMIT);
    }

    private YearRange normalizeYearRange(Integer startYear, Integer endYear) {
        int safeStartYear = startYear != null ? startYear : DEFAULT_START_YEAR;
        int safeEndYear = endYear != null ? endYear : DEFAULT_END_YEAR;

        if (safeStartYear > safeEndYear) {
            throw new IllegalArgumentException("Το startYear δεν μπορεί να είναι μεγαλύτερο από το endYear.");
        }

        return new YearRange(safeStartYear, safeEndYear);
    }

    private void validateId(int id, String fieldName) {
        if (id <= 0) {
            throw new IllegalArgumentException("Το " + fieldName + " πρέπει να είναι θετικός αριθμός.");
        }
    }

    private record YearRange(
            int startYear,
            int endYear
    ) {
    }
}