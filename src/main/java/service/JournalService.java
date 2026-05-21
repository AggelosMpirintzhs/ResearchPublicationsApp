package service;

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
import repository.JournalRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class JournalService {

    private static final int DEFAULT_START_YEAR = 0;
    private static final int DEFAULT_END_YEAR = 9999;

    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_LIMIT = 100;

    private static final int DEFAULT_PUBLISHER_SEARCH_LIMIT = 20;
    private static final int MAX_PUBLISHER_SEARCH_LIMIT = 50;
    private static final int MAX_SELECTED_PUBLISHERS = 10;

    private static final int DEFAULT_LAST_ARTICLE_ID = 0;
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int MAX_BATCH_SIZE = 5000;

    private static final int MAX_RANKING_SCATTER_LIMIT = 1000;

    private final JournalRepository journalRepository;

    public JournalService() {
        this.journalRepository = new JournalRepository();
    }

    public JournalService(JournalRepository journalRepository) {
        this.journalRepository = journalRepository;
    }

    public List<CategoryOptionDto> getBestSubjectAreas() {
        return journalRepository.findBestSubjectAreas();
    }

    public List<CategoryTrendDto> getJournalBestSubjectAreaYearlyTrends(
            String categoryFilter,
            Integer startYear,
            Integer endYear
    ) {
        YearRange yearRange = normalizeYearRange(startYear, endYear);
        String normalizedCategoryFilter = normalizeCategoryFilter(categoryFilter);

        return journalRepository.findJournalBestSubjectAreaYearlyTrends(
                normalizedCategoryFilter,
                yearRange.startYear(),
                yearRange.endYear()
        );
    }

    public List<PublisherOptionDto> getPublisherOptions(
            String publisherFilter,
            Integer limit
    ) {
        String normalizedPublisherFilter = normalizePublisherFilter(publisherFilter);
        int safeLimit = normalizePublisherSearchLimit(limit);

        if (normalizedPublisherFilter.isEmpty()) {
            return List.of();
        }

        return journalRepository.getPublisherOptions(
                normalizedPublisherFilter,
                safeLimit
        );
    }

    public List<PublisherQuartileStatsDto> getPublisherQuartilePublicationStats(
            int publisherId
    ) {
        validateId(publisherId, "publisherId");

        return journalRepository.getPublisherQuartilePublicationStatsById(publisherId);
    }

    public List<PublisherQuartileStatsDto> getPublisherQuartilePublicationStatsForPublishers(
            List<Integer> publisherIds
    ) {
        List<Integer> safePublisherIds = normalizeSelectedPublisherIds(publisherIds);

        if (safePublisherIds.isEmpty()) {
            return List.of();
        }

        List<PublisherQuartileStatsDto> results = new ArrayList<>();

        for (Integer publisherId : safePublisherIds) {
            results.addAll(
                    journalRepository.getPublisherQuartilePublicationStatsById(publisherId)
            );
        }

        return results;
    }

    public List<JournalSearchResultDto> searchJournals(String searchText, Integer limit) {
        String normalizedSearchText = normalizeSearchText(searchText);

        if (normalizedSearchText.isEmpty()) {
            return List.of();
        }

        int safeLimit = normalizeLimit(limit);

        return journalRepository.searchJournals(normalizedSearchText, safeLimit);
    }

    public Optional<JournalProfileDto> getJournalProfile(
            int journalId,
            Integer startYear,
            Integer endYear
    ) {
        validateId(journalId, "journalId");

        YearRange yearRange = normalizeYearRange(startYear, endYear);

        return journalRepository.findJournalProfile(
                journalId,
                yearRange.startYear(),
                yearRange.endYear()
        );
    }

    public List<JournalYearlyStatsDto> getJournalYearlyStats(
            int journalId,
            Integer startYear,
            Integer endYear
    ) {
        validateId(journalId, "journalId");

        YearRange yearRange = normalizeYearRange(startYear, endYear);

        return journalRepository.findJournalYearlyStats(
                journalId,
                yearRange.startYear(),
                yearRange.endYear()
        );
    }

    public Optional<JournalRankingDto> getJournalRanking(int journalId) {
        validateId(journalId, "journalId");

        return journalRepository.findJournalRanking(journalId);
    }

    public List<JournalArticleDto> getJournalArticles(
            int journalId,
            Integer startYear,
            Integer endYear
    ) {
        validateId(journalId, "journalId");

        YearRange yearRange = normalizeYearRange(startYear, endYear);

        return journalRepository.findJournalArticles(
                journalId,
                yearRange.startYear(),
                yearRange.endYear()
        );
    }

    public List<JournalArticleDto> getJournalArticlesBatch(
            int journalId,
            Integer startYear,
            Integer endYear,
            Integer lastArticleId,
            Integer batchSize
    ) {
        validateId(journalId, "journalId");

        YearRange yearRange = normalizeYearRange(startYear, endYear);

        int safeLastArticleId = normalizeLastArticleId(lastArticleId);
        int safeBatchSize = normalizeBatchSize(batchSize);

        return journalRepository.findJournalArticlesBatch(
                journalId,
                yearRange.startYear(),
                yearRange.endYear(),
                safeLastArticleId,
                safeBatchSize
        );
    }

    public List<ScatterPlotPointDto> getJournalRankingScatterData(
            String xMetric,
            String yMetric,
            Integer limit
    ) {
        Integer safeLimit = normalizeRankingScatterLimit(limit);

        return journalRepository.findJournalRankingScatterData(
                xMetric,
                yMetric,
                safeLimit
        );
    }

    private String normalizeSearchText(String searchText) {
        if (searchText == null) {
            return "";
        }

        return searchText.trim();
    }

    private String normalizeCategoryFilter(String categoryFilter) {
        if (categoryFilter == null) {
            return "";
        }

        return categoryFilter.trim();
    }

    private String normalizePublisherFilter(String publisherFilter) {
        if (publisherFilter == null) {
            return "";
        }

        return publisherFilter.trim();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_SEARCH_LIMIT;
        }

        return Math.min(limit, MAX_SEARCH_LIMIT);
    }

    private int normalizePublisherSearchLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_PUBLISHER_SEARCH_LIMIT;
        }

        return Math.min(limit, MAX_PUBLISHER_SEARCH_LIMIT);
    }

    private Integer normalizeRankingScatterLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return null;
        }

        return Math.min(limit, MAX_RANKING_SCATTER_LIMIT);
    }

    private List<Integer> normalizeSelectedPublisherIds(List<Integer> publisherIds) {
        if (publisherIds == null || publisherIds.isEmpty()) {
            return List.of();
        }

        Set<Integer> uniquePublisherIds = new LinkedHashSet<>();

        for (Integer publisherId : publisherIds) {
            if (publisherId == null) {
                continue;
            }

            validateId(publisherId, "publisherId");
            uniquePublisherIds.add(publisherId);
        }

        if (uniquePublisherIds.size() > MAX_SELECTED_PUBLISHERS) {
            throw new IllegalArgumentException(
                    "You can compare up to " + MAX_SELECTED_PUBLISHERS + " publishers."
            );
        }

        return new ArrayList<>(uniquePublisherIds);
    }

    private YearRange normalizeYearRange(Integer startYear, Integer endYear) {
        int safeStartYear = startYear != null ? startYear : DEFAULT_START_YEAR;
        int safeEndYear = endYear != null ? endYear : DEFAULT_END_YEAR;

        if (safeStartYear > safeEndYear) {
            throw new IllegalArgumentException("startYear cannot be greater than endYear.");
        }

        return new YearRange(safeStartYear, safeEndYear);
    }

    private int normalizeLastArticleId(Integer lastArticleId) {
        if (lastArticleId == null) {
            return DEFAULT_LAST_ARTICLE_ID;
        }

        if (lastArticleId < 0) {
            throw new IllegalArgumentException("lastArticleId cannot be negative.");
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
            throw new IllegalArgumentException(fieldName + " must be a positive number.");
        }
    }

    private record YearRange(
            int startYear,
            int endYear
    ) {
    }
}