package service;

import dto.author.AuthorProfileDto;
import dto.author.AuthorPublicationDto;
import dto.author.AuthorSearchResultDto;
import dto.author.AuthorYearlyStatsByTypeDto;
import dto.author.AuthorYearlyStatsDto;
import repository.AuthorRepository;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

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

    // Creates author service
    public AuthorService() {
        this.authorRepository = new AuthorRepository();
    }

    // Creates author service
    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    // Searches author names
    public List<AuthorSearchResultDto> searchAuthors(String searchText, Integer limit) {
        String normalizedSearchText = normalizeSearchText(searchText);

        if (normalizedSearchText.length() < MIN_SEARCH_LENGTH) {
            return List.of();
        }

        int safeLimit = normalizeSearchLimit(limit);

        List<AuthorSearchResultDto> results =
                authorRepository.searchAuthors(normalizedSearchText, safeLimit);

        return sortAuthorsByRelevance(results, normalizedSearchText);
    }

    // Loads author page
    public AuthorPageData loadAuthorPageData(
            int authorId,
            Integer startYear,
            Integer endYear
    ) {
        validateId(authorId, "authorId");

        YearRange yearRange = normalizeYearRange(startYear, endYear);

        Optional<AuthorProfileDto> profile =
                authorRepository.findAuthorProfile(
                        authorId,
                        yearRange.startYear(),
                        yearRange.endYear()
                );

        List<AuthorYearlyStatsDto> yearlyStats =
                authorRepository.findAuthorYearlyStats(
                        authorId,
                        yearRange.startYear(),
                        yearRange.endYear()
                );

        List<AuthorYearlyStatsByTypeDto> yearlyStatsByType =
                authorRepository.findAuthorYearlyStatsByType(
                        authorId,
                        yearRange.startYear(),
                        yearRange.endYear()
                );

        return new AuthorPageData(
                profile.orElse(null),
                yearlyStats,
                yearlyStatsByType
        );
    }

    // Loads publication batches
    public void loadAuthorPublicationsInBatches(
            int authorId,
            Integer startYear,
            Integer endYear,
            Integer lastArticleId,
            Integer batchSize,
            Consumer<List<AuthorPublicationDto>> onBatchLoaded,
            BooleanSupplier shouldContinue
    ) {
        validateId(authorId, "authorId");

        if (onBatchLoaded == null) {
            throw new IllegalArgumentException("onBatchLoaded cannot be null.");
        }

        if (shouldContinue == null) {
            throw new IllegalArgumentException("shouldContinue cannot be null.");
        }

        YearRange yearRange = normalizeYearRange(startYear, endYear);

        int safeLastArticleId = normalizeLastArticleId(lastArticleId);
        int safeBatchSize = normalizeBatchSize(batchSize);

        while (shouldContinue.getAsBoolean()) {
            List<AuthorPublicationDto> batch =
                    authorRepository.findAuthorPublicationsBatch(
                            authorId,
                            yearRange.startYear(),
                            yearRange.endYear(),
                            safeLastArticleId,
                            safeBatchSize
                    );

            if (batch.isEmpty()) {
                break;
            }

            onBatchLoaded.accept(batch);

            AuthorPublicationDto lastPublication = batch.get(batch.size() - 1);

            if (lastPublication.articleId() == null) {
                break;
            }

            safeLastArticleId = lastPublication.articleId();
        }
    }

    // Gets author profile
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

    // Gets yearly stats
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

    // Gets typed stats
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

    // Gets publications batch
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

    // Sorts author results
    private List<AuthorSearchResultDto> sortAuthorsByRelevance(
            List<AuthorSearchResultDto> results,
            String normalizedQuery
    ) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        List<AuthorSearchResultDto> sortedResults = new ArrayList<>(results);

        sortedResults.sort((first, second) -> {
            int firstScore = authorRelevanceScore(first, normalizedQuery);
            int secondScore = authorRelevanceScore(second, normalizedQuery);

            if (firstScore != secondScore) {
                return Integer.compare(firstScore, secondScore);
            }

            String firstName = normalizeSearchText(first.authorName());
            String secondName = normalizeSearchText(second.authorName());

            if (firstName.length() != secondName.length()) {
                return Integer.compare(firstName.length(), secondName.length());
            }

            return firstName.compareTo(secondName);
        });

        return sortedResults;
    }

    // Scores author relevance
    private int authorRelevanceScore(AuthorSearchResultDto author, String query) {
        String name = normalizeSearchText(author == null ? null : author.authorName());

        if (name.equals(query)) {
            return 0;
        }

        if (name.startsWith(query)) {
            return 1;
        }

        if (name.contains(" " + query)) {
            return 2;
        }

        if (name.contains(query)) {
            return 3;
        }

        if (containsAllQueryWords(name, query)) {
            return 4;
        }

        return 5;
    }

    // Checks query words
    private boolean containsAllQueryWords(String text, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        for (String word : query.split(" ")) {
            if (!word.isBlank() && !text.contains(word)) {
                return false;
            }
        }

        return true;
    }

    // Normalizes search text
    private String normalizeSearchText(String searchText) {
        if (searchText == null) {
            return "";
        }

        String normalized = Normalizer.normalize(searchText, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized.toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // Normalizes search limit
    private int normalizeSearchLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_SEARCH_LIMIT;
        }

        return Math.min(limit, MAX_SEARCH_LIMIT);
    }

    // Normalizes year range
    private YearRange normalizeYearRange(Integer startYear, Integer endYear) {
        int safeStartYear = startYear != null ? startYear : DEFAULT_START_YEAR;
        int safeEndYear = endYear != null ? endYear : DEFAULT_END_YEAR;

        if (safeStartYear > safeEndYear) {
            throw new IllegalArgumentException("startYear cannot be greater than endYear.");
        }

        return new YearRange(safeStartYear, safeEndYear);
    }

    // Normalizes article id
    private int normalizeLastArticleId(Integer lastArticleId) {
        if (lastArticleId == null) {
            return DEFAULT_LAST_ARTICLE_ID;
        }

        if (lastArticleId < 0) {
            throw new IllegalArgumentException("lastArticleId cannot be negative.");
        }

        return lastArticleId;
    }

    // Normalizes batch size
    private int normalizeBatchSize(Integer batchSize) {
        if (batchSize == null || batchSize <= 0) {
            return DEFAULT_BATCH_SIZE;
        }

        return Math.min(batchSize, MAX_BATCH_SIZE);
    }

    // Validates positive id
    private void validateId(int id, String fieldName) {
        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive.");
        }
    }

    private record YearRange(
            int startYear,
            int endYear
    ) {
    }

    public record AuthorPageData(
            AuthorProfileDto profile,
            List<AuthorYearlyStatsDto> yearlyStats,
            List<AuthorYearlyStatsByTypeDto> yearlyStatsByType
    ) {
        // Checks profile exists
        public boolean hasProfile() {
            return profile != null;
        }

        // Gets expected publications
        public long expectedPublicationCount() {
            if (profile == null || profile.totalArticles() == null) {
                return 0L;
            }

            return profile.totalArticles();
        }
    }
}