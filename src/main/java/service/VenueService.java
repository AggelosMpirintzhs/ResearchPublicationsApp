package service;

import dto.conference.ConferenceArticleDto;
import dto.conference.ConferenceProfileDto;
import dto.conference.ConferenceRankingDto;
import dto.conference.ConferenceSearchResultDto;
import dto.conference.ConferenceYearlyStatsDto;
import dto.journal.JournalArticleDto;
import dto.journal.JournalProfileDto;
import dto.journal.JournalRankingDto;
import dto.journal.JournalSearchResultDto;
import dto.journal.JournalYearlyStatsDto;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class VenueService {

    public static final String TYPE_JOURNAL = "Journal";
    public static final String TYPE_CONFERENCE = "Conference";

    private static final int MIN_SEARCH_LENGTH = 3;

    private final JournalService journalService;
    private final ConferenceService conferenceService;

    public VenueService() {
        this.journalService = new JournalService();
        this.conferenceService = new ConferenceService();
    }

    public VenueService(
            JournalService journalService,
            ConferenceService conferenceService
    ) {
        this.journalService = journalService;
        this.conferenceService = conferenceService;
    }

    public List<Object> searchVenues(
            String venueType,
            String searchText,
            Integer limit
    ) {
        validateVenueType(venueType);

        String normalizedSearchText = normalizeSearchText(searchText);

        if (normalizedSearchText.length() < MIN_SEARCH_LENGTH) {
            return List.of();
        }

        List<Object> results = new ArrayList<>();

        if (TYPE_JOURNAL.equals(venueType)) {
            results.addAll(journalService.searchJournals(searchText, limit));
        } else if (TYPE_CONFERENCE.equals(venueType)) {
            results.addAll(conferenceService.searchConferences(searchText, limit));
        }

        return sortVenueResultsByRelevance(results, searchText);
    }

    public VenuePageData loadVenuePageData(
            String venueType,
            Object selectedVenue,
            Integer startYear,
            Integer endYear
    ) {
        validateVenueType(venueType);

        int venueId = getVenueId(venueType, selectedVenue);

        if (TYPE_JOURNAL.equals(venueType)) {
            Optional<JournalProfileDto> profile =
                    journalService.getJournalProfile(
                            venueId,
                            startYear,
                            endYear
                    );

            Optional<JournalRankingDto> ranking =
                    journalService.getJournalRanking(venueId);

            List<JournalYearlyStatsDto> yearlyStats =
                    journalService.getJournalYearlyStats(
                            venueId,
                            startYear,
                            endYear
                    );

            List<Object> yearlyStatsAsObjects = new ArrayList<>();
            yearlyStatsAsObjects.addAll(yearlyStats);

            return new VenuePageData(
                    TYPE_JOURNAL,
                    venueId,
                    profile.orElse(null),
                    ranking.orElse(null),
                    yearlyStatsAsObjects
            );
        }

        Optional<ConferenceProfileDto> profile =
                conferenceService.getConferenceProfile(
                        venueId,
                        startYear,
                        endYear
                );

        Optional<ConferenceRankingDto> ranking =
                conferenceService.getConferenceRanking(venueId);

        List<ConferenceYearlyStatsDto> yearlyStats =
                conferenceService.getConferenceYearlyStats(
                        venueId,
                        startYear,
                        endYear
                );

        List<Object> yearlyStatsAsObjects = new ArrayList<>();
        yearlyStatsAsObjects.addAll(yearlyStats);

        return new VenuePageData(
                TYPE_CONFERENCE,
                venueId,
                profile.orElse(null),
                ranking.orElse(null),
                yearlyStatsAsObjects
        );
    }

    public void loadVenueArticlesInBatches(
            String venueType,
            int venueId,
            Integer startYear,
            Integer endYear,
            Integer lastArticleId,
            Integer batchSize,
            Consumer<List<Object>> onBatchLoaded,
            BooleanSupplier shouldContinue
    ) {
        validateVenueType(venueType);
        validateId(venueId, "venueId");

        if (onBatchLoaded == null) {
            throw new IllegalArgumentException("Το onBatchLoaded δεν μπορεί να είναι null.");
        }

        if (shouldContinue == null) {
            throw new IllegalArgumentException("Το shouldContinue δεν μπορεί να είναι null.");
        }

        int safeLastArticleId = lastArticleId == null ? 0 : lastArticleId;

        if (safeLastArticleId < 0) {
            throw new IllegalArgumentException("Το lastArticleId δεν μπορεί να είναι αρνητικό.");
        }

        while (shouldContinue.getAsBoolean()) {
            List<Object> batch = new ArrayList<>();

            if (TYPE_JOURNAL.equals(venueType)) {
                batch.addAll(
                        journalService.getJournalArticlesBatch(
                                venueId,
                                startYear,
                                endYear,
                                safeLastArticleId,
                                batchSize
                        )
                );
            } else if (TYPE_CONFERENCE.equals(venueType)) {
                batch.addAll(
                        conferenceService.getConferenceArticlesBatch(
                                venueId,
                                startYear,
                                endYear,
                                safeLastArticleId,
                                batchSize
                        )
                );
            }

            if (batch.isEmpty()) {
                break;
            }

            onBatchLoaded.accept(batch);

            Integer newLastArticleId = extractArticleId(batch.get(batch.size() - 1));

            if (newLastArticleId == null) {
                break;
            }

            safeLastArticleId = newLastArticleId;
        }
    }

    public int getVenueId(String venueType, Object venue) {
        validateVenueType(venueType);

        if (TYPE_JOURNAL.equals(venueType)) {
            if (venue instanceof JournalSearchResultDto journal) {
                if (journal.journalId() != null && journal.journalId() > 0) {
                    return journal.journalId();
                }
            }

            throw new IllegalArgumentException("Δεν βρέθηκε έγκυρο journalId για το επιλεγμένο journal.");
        }

        if (venue instanceof ConferenceSearchResultDto conference) {
            if (conference.conferenceId() != null && conference.conferenceId() > 0) {
                return conference.conferenceId();
            }
        }

        throw new IllegalArgumentException("Δεν βρέθηκε έγκυρο conferenceId για το επιλεγμένο conference.");
    }

    public String getVenueDisplayName(Object venue) {
        if (venue instanceof JournalSearchResultDto journal) {
            if (journal.journalName() != null && !journal.journalName().isBlank()) {
                if (journal.publisherName() != null && !journal.publisherName().isBlank()) {
                    return journal.journalName() + " (" + journal.publisherName() + ")";
                }

                return journal.journalName();
            }

            return "Journal #" + journal.journalId();
        }

        if (venue instanceof ConferenceSearchResultDto conference) {
            if (conference.acronym() != null && !conference.acronym().isBlank()) {
                if (conference.conferenceTitle() != null && !conference.conferenceTitle().isBlank()) {
                    return conference.acronym() + " - " + conference.conferenceTitle();
                }

                return conference.acronym();
            }

            if (conference.conferenceTitle() != null && !conference.conferenceTitle().isBlank()) {
                return conference.conferenceTitle();
            }

            return "Conference #" + conference.conferenceId();
        }

        return "Unknown venue";
    }

    public long getExpectedArticleCount(Object profile) {
        if (profile instanceof JournalProfileDto journalProfile) {
            return defaultLong(journalProfile.totalArticles());
        }

        if (profile instanceof ConferenceProfileDto conferenceProfile) {
            return defaultLong(conferenceProfile.totalArticles());
        }

        return 0L;
    }

    private List<Object> sortVenueResultsByRelevance(
            List<Object> results,
            String query
    ) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        String normalizedQuery = normalizeSearchText(query);
        List<Object> sortedResults = new ArrayList<>(results);

        sortedResults.sort((first, second) -> {
            int firstScore = venueRelevanceScore(first, normalizedQuery);
            int secondScore = venueRelevanceScore(second, normalizedQuery);

            if (firstScore != secondScore) {
                return Integer.compare(firstScore, secondScore);
            }

            String firstTitle = normalizeSearchText(getVenueTitleForSearch(first));
            String secondTitle = normalizeSearchText(getVenueTitleForSearch(second));

            if (firstTitle.length() != secondTitle.length()) {
                return Integer.compare(firstTitle.length(), secondTitle.length());
            }

            return firstTitle.compareTo(secondTitle);
        });

        return sortedResults;
    }

    private int venueRelevanceScore(Object venue, String query) {
        String title = normalizeSearchText(getVenueTitleForSearch(venue));
        String acronym = normalizeSearchText(getVenueAcronymForSearch(venue));
        String displayName = normalizeSearchText(getVenueDisplayName(venue));

        if (title.equals(query) || acronym.equals(query) || displayName.equals(query)) {
            return 0;
        }

        if (title.startsWith(query) || acronym.startsWith(query) || displayName.startsWith(query)) {
            return 1;
        }

        if (title.contains(query) || acronym.contains(query) || displayName.contains(query)) {
            return 2;
        }

        if (containsAllQueryWords(title + " " + acronym + " " + displayName, query)) {
            return 3;
        }

        return 4;
    }

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

    private String getVenueTitleForSearch(Object venue) {
        if (venue instanceof JournalSearchResultDto journal) {
            return journal.journalName();
        }

        if (venue instanceof ConferenceSearchResultDto conference) {
            return conference.conferenceTitle();
        }

        return "";
    }

    private String getVenueAcronymForSearch(Object venue) {
        if (venue instanceof ConferenceSearchResultDto conference) {
            return conference.acronym();
        }

        return "";
    }

    private Integer extractArticleId(Object article) {
        if (article instanceof JournalArticleDto journalArticle) {
            return journalArticle.articleId();
        }

        if (article instanceof ConferenceArticleDto conferenceArticle) {
            return conferenceArticle.articleId();
        }

        return null;
    }

    private String normalizeSearchText(String text) {
        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized.toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
                .replaceAll("\\s+", " ");
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private void validateVenueType(String venueType) {
        if (!TYPE_JOURNAL.equals(venueType) && !TYPE_CONFERENCE.equals(venueType)) {
            throw new IllegalArgumentException("Μη έγκυρος τύπος venue.");
        }
    }

    private void validateId(int id, String fieldName) {
        if (id <= 0) {
            throw new IllegalArgumentException("Το " + fieldName + " πρέπει να είναι θετικός αριθμός.");
        }
    }

    public record VenuePageData(
            String venueType,
            int venueId,
            Object profile,
            Object ranking,
            List<Object> yearlyStats
    ) {
        public boolean hasAnyData() {
            return profile != null
                    || ranking != null
                    || yearlyStats != null && !yearlyStats.isEmpty();
        }

        public boolean hasArticleData() {
            return profile != null && expectedArticleCount() > 0;
        }

        public long expectedArticleCount() {
            if (profile instanceof JournalProfileDto journalProfile) {
                return journalProfile.totalArticles() == null ? 0L : journalProfile.totalArticles();
            }

            if (profile instanceof ConferenceProfileDto conferenceProfile) {
                return conferenceProfile.totalArticles() == null ? 0L : conferenceProfile.totalArticles();
            }

            return 0L;
        }
    }
}