package service.charts;

import dto.conference.ConferenceSearchResultDto;
import dto.conference.ConferenceYearlyStatsDto;
import dto.journal.JournalSearchResultDto;
import dto.journal.JournalYearlyStatsDto;
import service.ConferenceService;
import service.JournalService;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VenueAnalysisService {

    public static final String TYPE_JOURNAL = "Journal";
    public static final String TYPE_CONFERENCE = "Conference";

    public static final String METRIC_ARTICLES = "Published articles";
    public static final String METRIC_AUTHOR_ENTRIES = "Total author entries";
    public static final String METRIC_DISTINCT_AUTHORS = "Distinct authors";

    public static final String BAR_TOTAL_ARTICLES = "Total articles";
    public static final String BAR_AVG_ARTICLES_PER_YEAR = "Avg articles / year";
    public static final String BAR_AVG_AUTHOR_ENTRIES_PER_YEAR = "Avg author entries / year";

    public static final int DEFAULT_MIN_YEAR = 1900;

    private static final String[] VENUE_TYPES = {
            TYPE_JOURNAL,
            TYPE_CONFERENCE
    };

    private static final String[] LINE_METRICS = {
            METRIC_ARTICLES,
            METRIC_AUTHOR_ENTRIES,
            METRIC_DISTINCT_AUTHORS
    };

    private final JournalService journalService = new JournalService();
    private final ConferenceService conferenceService = new ConferenceService();

    public String[] getVenueTypes() {
        return VENUE_TYPES;
    }

    public String[] getLineMetrics() {
        return LINE_METRICS;
    }

    public int getCurrentYear() {
        return LocalDate.now().getYear();
    }

    public List<Integer> buildYearList(Integer minimumYear) {
        int currentYear = getCurrentYear();
        int min = minimumYear == null ? DEFAULT_MIN_YEAR : minimumYear;

        List<Integer> years = new ArrayList<>();

        for (int year = currentYear; year >= min; year--) {
            years.add(year);
        }

        return years;
    }

    public YearRange validateYearRange(Integer startYear, Integer endYear) {
        if (startYear != null && endYear != null && endYear < startYear) {
            throw new IllegalArgumentException("The To year must be greater than or equal to the From year.");
        }

        return new YearRange(startYear, endYear);
    }

    public void validateLineMetric(String metric) {
        if (metric == null || metric.isBlank()) {
            throw new IllegalArgumentException("Select what the line chart should display.");
        }
    }

    public List<Object> searchVenues(String venueType, String searchText, int searchLimit) {
        List<Object> results = new ArrayList<>();

        if (venueType == null || venueType.isBlank()) {
            return results;
        }

        if (searchText == null || searchText.isBlank()) {
            return results;
        }

        if (TYPE_JOURNAL.equals(venueType)) {
            results.addAll(journalService.searchJournals(searchText, searchLimit));
        } else if (TYPE_CONFERENCE.equals(venueType)) {
            results.addAll(conferenceService.searchConferences(searchText, searchLimit));
        }

        return sortVenueResultsByRelevance(results, searchText);
    }

    public SelectedVenue createSelectedVenue(String venueType, Object venue) {
        if (venueType == null || venueType.isBlank()) {
            throw new IllegalArgumentException("You must select Journal or Conference first.");
        }

        if (venue == null) {
            throw new IllegalArgumentException("You must select a venue first.");
        }

        return new SelectedVenue(venueType, venue);
    }

    public boolean alreadySelected(List<SelectedVenue> selectedVenues, SelectedVenue newSelectedVenue) {
        if (selectedVenues == null || selectedVenues.isEmpty() || newSelectedVenue == null) {
            return false;
        }

        for (SelectedVenue existing : selectedVenues) {
            if (existing.type().equals(newSelectedVenue.type())
                    && getVenueId(existing.venue()) == getVenueId(newSelectedVenue.venue())) {
                return true;
            }
        }

        return false;
    }

    public List<VenueChartSeries> loadVenueChartSeries(
            List<SelectedVenue> selectedVenues,
            YearRange yearRange
    ) {
        List<VenueChartSeries> chartSeries = new ArrayList<>();

        if (selectedVenues == null || selectedVenues.isEmpty()) {
            return chartSeries;
        }

        for (SelectedVenue selectedVenue : selectedVenues) {
            if (selectedVenue == null) {
                continue;
            }

            if (TYPE_JOURNAL.equals(selectedVenue.type())) {
                int journalId = getJournalId(selectedVenue.venue());

                List<JournalYearlyStatsDto> stats =
                        journalService.getJournalYearlyStats(
                                journalId,
                                yearRange.startYear(),
                                yearRange.endYear()
                        );

                chartSeries.add(
                        new VenueChartSeries(
                                getVenueDisplayName(selectedVenue),
                                selectedVenue.type(),
                                journalId,
                                buildYearlyStats(stats == null ? new ArrayList<>() : new ArrayList<>(stats))
                        )
                );
            }

            if (TYPE_CONFERENCE.equals(selectedVenue.type())) {
                int conferenceId = getConferenceId(selectedVenue.venue());

                List<ConferenceYearlyStatsDto> stats =
                        conferenceService.getConferenceYearlyStats(
                                conferenceId,
                                yearRange.startYear(),
                                yearRange.endYear()
                        );

                chartSeries.add(
                        new VenueChartSeries(
                                getVenueDisplayName(selectedVenue),
                                selectedVenue.type(),
                                conferenceId,
                                buildYearlyStats(stats == null ? new ArrayList<>() : new ArrayList<>(stats))
                        )
                );
            }
        }

        return chartSeries;
    }

    private List<VenueYearlyStats> buildYearlyStats(List<Object> rawStats) {
        List<VenueYearlyStats> yearlyStats = new ArrayList<>();

        if (rawStats == null || rawStats.isEmpty()) {
            return yearlyStats;
        }

        for (Object stat : rawStats) {
            Integer year = extractYear(stat);

            if (year == null) {
                continue;
            }

            yearlyStats.add(
                    new VenueYearlyStats(
                            year,
                            extractMetricValue(stat, METRIC_ARTICLES),
                            extractMetricValue(stat, METRIC_AUTHOR_ENTRIES),
                            extractMetricValue(stat, METRIC_DISTINCT_AUTHORS)
                    )
            );
        }

        return yearlyStats;
    }

    public Long getLineMetricValue(VenueYearlyStats yearlyStats, String metric) {
        if (yearlyStats == null || metric == null) {
            return null;
        }

        if (METRIC_ARTICLES.equals(metric)) {
            return yearlyStats.totalArticles();
        }

        if (METRIC_AUTHOR_ENTRIES.equals(metric)) {
            return yearlyStats.totalAuthorEntries();
        }

        if (METRIC_DISTINCT_AUTHORS.equals(metric)) {
            return yearlyStats.distinctAuthors();
        }

        return null;
    }

    public VenueAggregate calculateVenueAggregate(VenueChartSeries venueSeries) {
        if (venueSeries == null || venueSeries.yearlyStats() == null || venueSeries.yearlyStats().isEmpty()) {
            return new VenueAggregate(0, 0, 0, 0, 0);
        }

        long totalArticles = 0;
        long totalAuthorEntries = 0;
        int activeYears = 0;

        for (VenueYearlyStats yearlyStats : venueSeries.yearlyStats()) {
            Long articles = yearlyStats.totalArticles();
            Long authorEntries = yearlyStats.totalAuthorEntries();

            if (articles == null && authorEntries == null) {
                continue;
            }

            activeYears++;

            if (articles != null) {
                totalArticles += articles;
            }

            if (authorEntries != null) {
                totalAuthorEntries += authorEntries;
            }
        }

        double avgArticlesPerYear = activeYears == 0 ? 0 : (double) totalArticles / activeYears;
        double avgAuthorEntriesPerYear = activeYears == 0 ? 0 : (double) totalAuthorEntries / activeYears;

        return new VenueAggregate(
                totalArticles,
                totalAuthorEntries,
                activeYears,
                avgArticlesPerYear,
                avgAuthorEntriesPerYear
        );
    }

    public double getBarMetricValue(VenueAggregate aggregate, String metric) {
        if (aggregate == null || metric == null) {
            return 0;
        }

        if (BAR_TOTAL_ARTICLES.equals(metric)) {
            return aggregate.totalArticles();
        }

        if (BAR_AVG_ARTICLES_PER_YEAR.equals(metric)) {
            return aggregate.avgArticlesPerYear();
        }

        if (BAR_AVG_AUTHOR_ENTRIES_PER_YEAR.equals(metric)) {
            return aggregate.avgAuthorEntriesPerYear();
        }

        return 0;
    }

    private List<Object> sortVenueResultsByRelevance(List<Object> results, String query) {
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
        String displayName = normalizeSearchText(getRawVenueDisplayName(venue));

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

    public int getVenueId(Object venue) {
        if (venue instanceof JournalSearchResultDto journal) {
            return journal.journalId() == null ? -1 : journal.journalId();
        }

        if (venue instanceof ConferenceSearchResultDto conference) {
            return conference.conferenceId() == null ? -1 : conference.conferenceId();
        }

        return -1;
    }

    private int getJournalId(Object venue) {
        if (venue instanceof JournalSearchResultDto journal) {
            if (journal.journalId() != null && journal.journalId() > 0) {
                return journal.journalId();
            }
        }

        throw new IllegalArgumentException("Valid journal id was not found.");
    }

    private int getConferenceId(Object venue) {
        if (venue instanceof ConferenceSearchResultDto conference) {
            if (conference.conferenceId() != null && conference.conferenceId() > 0) {
                return conference.conferenceId();
            }
        }

        throw new IllegalArgumentException("Valid conference id was not found.");
    }

    public String getVenueDisplayName(SelectedVenue selectedVenue) {
        return selectedVenue.type() + ": " + getRawVenueDisplayName(selectedVenue.venue());
    }

    public String getRawVenueDisplayName(Object venue) {
        if (venue instanceof JournalSearchResultDto journal) {
            if (journal.journalName() != null && !journal.journalName().isBlank()) {
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

    private Integer extractYear(Object stat) {
        if (stat instanceof JournalYearlyStatsDto journalStat) {
            return journalStat.year();
        }

        if (stat instanceof ConferenceYearlyStatsDto conferenceStat) {
            return conferenceStat.year();
        }

        return null;
    }

    private Long extractMetricValue(Object stat, String metric) {
        if (stat instanceof JournalYearlyStatsDto journalStat) {
            if (METRIC_ARTICLES.equals(metric)) {
                return journalStat.totalArticles();
            }

            if (METRIC_AUTHOR_ENTRIES.equals(metric)) {
                return journalStat.totalAuthorOccurrences();
            }

            if (METRIC_DISTINCT_AUTHORS.equals(metric)) {
                return journalStat.distinctAuthors();
            }
        }

        if (stat instanceof ConferenceYearlyStatsDto conferenceStat) {
            if (METRIC_ARTICLES.equals(metric)) {
                return conferenceStat.totalArticles();
            }

            if (METRIC_AUTHOR_ENTRIES.equals(metric)) {
                return conferenceStat.totalAuthorOccurrences();
            }

            if (METRIC_DISTINCT_AUTHORS.equals(metric)) {
                return conferenceStat.distinctAuthors();
            }
        }

        return null;
    }

    public String getVenueKey(VenueChartSeries venueSeries) {
        if (venueSeries == null) {
            return "";
        }

        return venueSeries.type() + "#" + venueSeries.venueId();
    }

    public record SelectedVenue(
            String type,
            Object venue
    ) {
    }

    public record VenueChartSeries(
            String name,
            String type,
            int venueId,
            List<VenueYearlyStats> yearlyStats
    ) {
    }

    public record VenueYearlyStats(
            Integer year,
            Long totalArticles,
            Long totalAuthorEntries,
            Long distinctAuthors
    ) {
    }

    public record VenueAggregate(
            long totalArticles,
            long totalAuthorEntries,
            int activeYears,
            double avgArticlesPerYear,
            double avgAuthorEntriesPerYear
    ) {
    }

    public record YearRange(
            Integer startYear,
            Integer endYear
    ) {
    }
}