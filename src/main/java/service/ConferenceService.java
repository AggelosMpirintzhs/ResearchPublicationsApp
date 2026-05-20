package service;

import dto.conference.ConferenceArticleDto;
import dto.conference.ConferenceProfileDto;
import dto.conference.ConferenceRankingDto;
import dto.conference.ConferenceSearchResultDto;
import dto.conference.ConferenceYearlyStatsDto;
import repository.ConferenceRepository;
import dto.chart.CategoryOptionDto;
import java.util.List;
import java.util.Optional;

public class ConferenceService {

    private static final int DEFAULT_START_YEAR = 0;
    private static final int DEFAULT_END_YEAR = 9999;

    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_LIMIT = 100;

    private static final int DEFAULT_LAST_ARTICLE_ID = 0;
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int MAX_BATCH_SIZE = 5000;

    private final ConferenceRepository conferenceRepository;

    public ConferenceService() {
        this.conferenceRepository = new ConferenceRepository();
    }

    public ConferenceService(ConferenceRepository conferenceRepository) {
        this.conferenceRepository = conferenceRepository;
    }
    public List<CategoryOptionDto> getPrimaryFoRCategories() {
        return conferenceRepository.findPrimaryFoRCategories();
    }

    public List<ConferenceSearchResultDto> searchConferences(String searchText, Integer limit) {
        String normalizedSearchText = normalizeSearchText(searchText);

        if (normalizedSearchText.isEmpty()) {
            return List.of();
        }

        int safeLimit = normalizeLimit(limit);

        return conferenceRepository.searchConferences(normalizedSearchText, safeLimit);
    }

    public Optional<ConferenceProfileDto> getConferenceProfile(
            int conferenceId,
            Integer startYear,
            Integer endYear
    ) {
        validateId(conferenceId, "conferenceId");

        YearRange yearRange = normalizeYearRange(startYear, endYear);

        return conferenceRepository.findConferenceProfile(
                conferenceId,
                yearRange.startYear(),
                yearRange.endYear()
        );
    }

    public List<ConferenceYearlyStatsDto> getConferenceYearlyStats(
            int conferenceId,
            Integer startYear,
            Integer endYear
    ) {
        validateId(conferenceId, "conferenceId");

        YearRange yearRange = normalizeYearRange(startYear, endYear);

        return conferenceRepository.findConferenceYearlyStats(
                conferenceId,
                yearRange.startYear(),
                yearRange.endYear()
        );
    }

    public Optional<ConferenceRankingDto> getConferenceRanking(int conferenceId) {
        validateId(conferenceId, "conferenceId");

        return conferenceRepository.findConferenceRanking(conferenceId);
    }

    public List<ConferenceArticleDto> getConferenceArticles(
            int conferenceId,
            Integer startYear,
            Integer endYear
    ) {
        validateId(conferenceId, "conferenceId");

        YearRange yearRange = normalizeYearRange(startYear, endYear);

        return conferenceRepository.findConferenceArticles(
                conferenceId,
                yearRange.startYear(),
                yearRange.endYear()
        );
    }

    public List<ConferenceArticleDto> getConferenceArticlesBatch(
            int conferenceId,
            Integer startYear,
            Integer endYear,
            Integer lastArticleId,
            Integer batchSize
    ) {
        validateId(conferenceId, "conferenceId");

        YearRange yearRange = normalizeYearRange(startYear, endYear);

        int safeLastArticleId = normalizeLastArticleId(lastArticleId);
        int safeBatchSize = normalizeBatchSize(batchSize);

        return conferenceRepository.findConferenceArticlesBatch(
                conferenceId,
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