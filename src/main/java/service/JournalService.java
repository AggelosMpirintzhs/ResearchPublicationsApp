package service;

import dto.journal.JournalArticleDto;
import dto.journal.JournalProfileDto;
import dto.journal.JournalRankingDto;
import dto.journal.JournalSearchResultDto;
import dto.journal.JournalYearlyStatsDto;
import repository.JournalRepository;

import java.util.List;
import java.util.Optional;

public class JournalService {

    private static final int DEFAULT_START_YEAR = 0;
    private static final int DEFAULT_END_YEAR = 9999;

    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_LIMIT = 100;

    private final JournalRepository journalRepository;

    public JournalService() {
        this.journalRepository = new JournalRepository();
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