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

    private static final int MIN_SEARCH_LENGTH = 3;
    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_LIMIT = 50;

    private static final int DEFAULT_LAST_ARTICLE_ID = 0;
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int MAX_BATCH_SIZE = 5000;

    private final AuthorRepository authorRepository;

    public AuthorService() {
        this.authorRepository = new AuthorRepository();
    }

    public List<AuthorSearchResultDto> searchAuthors(String searchText, Integer limit) {
        String normalizedSearchText = normalizeSearchText(searchText);

        if (normalizedSearchText.length() < MIN_SEARCH_LENGTH) {
            return List.of();
        }

        int safeLimit = normalizeSearchLimit(limit);

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

    public List<AuthorPublicationDto> getAuthorPublicationsBatch(
            int authorId,
            Integer startYear,
            Integer endYear,
            Integer lastArticleId,
            Integer batchSize
    ) {
        validateId(authorId, "authorId");

        YearRange yearRange = normalizeYearRange(startYear, endYear);

        int safeLastArticleId = normalizeLastArticleId(lastArticleId);
        int safeBatchSize = normalizeBatchSize(batchSize);

        return authorRepository.findAuthorPublicationsBatch(
                authorId,
                yearRange.startYear(),
                yearRange.endYear(),
                safeLastArticleId,
                safeBatchSize
        );
    }

    private String normalizeSearchText(String searchText) {
        if (searchText == null) {
            return "";
        }

        return searchText
                .toLowerCase()
                .trim()
                .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
                .replaceAll("\\s+", " ");
    }

    private int normalizeSearchLimit(Integer limit) {
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

    private int normalizeLastArticleId(Integer lastArticleId) {
        if (lastArticleId == null) {
            return DEFAULT_LAST_ARTICLE_ID;
        }

        if (lastArticleId < 0) {
            throw new IllegalArgumentException("Το lastArticleId δεν μπορεί να είναι αρνητικό.");
        }

        return lastArticleId;
    }

    private int normalizeBatchSize(Integer batchSize) {
        if (batchSize == null || batchSize <= 0) {
            return DEFAULT_BATCH_SIZE;
        }

        return Math.min(batchSize, MAX_BATCH_SIZE);
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