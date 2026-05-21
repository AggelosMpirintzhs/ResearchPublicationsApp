package service.charts;

import dto.chart.ScatterPlotPointDto;
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

public class ScatterPlotsService {

    public static final String TYPE_JOURNAL = "Journal";
    public static final String TYPE_CONFERENCE = "Conference";

    public static final String METRIC_ARTICLES = "Published articles";
    public static final String METRIC_AUTHOR_ENTRIES = "Total author entries";

    public static final int DEFAULT_MIN_YEAR = 1900;

    private static final String[] VENUE_TYPES = {
            TYPE_JOURNAL,
            TYPE_CONFERENCE
    };

    private static final String[] RANKING_METRICS = {
            "Total Docs",
            "Total Docs 3y",
            "Total Refs",
            "Total Cites 3y",
            "Citable Docs 3y",
            "Cites / Doc 2y",
            "Refs / Doc",
            "SJR",
            "Cite Score",
            "H index"
    };

    private final JournalService journalService = new JournalService();
    private final ConferenceService conferenceService = new ConferenceService();

    public String[] getVenueTypes() {
        return VENUE_TYPES;
    }

    public String[] getRankingMetrics() {
        return RANKING_METRICS;
    }

    public List<Integer> buildYearList(Integer minimumYear) {
        int currentYear = LocalDate.now().getYear();
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

    public void validateRankingMetrics(String xMetric, String yMetric) {
        if (xMetric == null || xMetric.isBlank()) {
            throw new IllegalArgumentException("You must choose a metric for the X axis.");
        }

        if (yMetric == null || yMetric.isBlank()) {
            throw new IllegalArgumentException("You must choose a metric for the Y axis.");
        }

        if (xMetric.equals(yMetric)) {
            throw new IllegalArgumentException("Choose different X and Y metrics.");
        }
    }

    public Integer resolveRankingLimit(String selectedLimit, int defaultLimit) {
        if (selectedLimit == null || selectedLimit.isBlank()) {
            return defaultLimit;
        }

        if ("All".equalsIgnoreCase(selectedLimit.trim())) {
            return null;
        }

        try {
            int parsedLimit = Integer.parseInt(selectedLimit.trim());

            if (parsedLimit <= 0) {
                return defaultLimit;
            }

            return parsedLimit;
        } catch (NumberFormatException exception) {
            return defaultLimit;
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

    public List<VenueScatterSeries> loadVenueScatterSeries(
            List<SelectedVenue> selectedVenues,
            YearRange yearRange
    ) {
        List<VenueScatterSeries> chartSeries = new ArrayList<>();

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
                        new VenueScatterSeries(
                                getVenueDisplayName(selectedVenue),
                                selectedVenue.type(),
                                journalId,
                                buildVenueScatterPoints(
                                        getVenueDisplayName(selectedVenue),
                                        selectedVenue.type(),
                                        journalId,
                                        stats == null ? new ArrayList<>() : new ArrayList<>(stats)
                                )
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
                        new VenueScatterSeries(
                                getVenueDisplayName(selectedVenue),
                                selectedVenue.type(),
                                conferenceId,
                                buildVenueScatterPoints(
                                        getVenueDisplayName(selectedVenue),
                                        selectedVenue.type(),
                                        conferenceId,
                                        stats == null ? new ArrayList<>() : new ArrayList<>(stats)
                                )
                        )
                );
            }
        }

        return chartSeries;
    }

    public List<RankingScatterPoint> loadJournalRankingScatterData(
            String xMetric,
            String yMetric,
            Integer limit
    ) {
        List<ScatterPlotPointDto> rawRows = journalService.getJournalRankingScatterData(
                xMetric,
                yMetric,
                limit
        );

        List<RankingScatterPoint> points = new ArrayList<>();

        if (rawRows == null || rawRows.isEmpty()) {
            return points;
        }

        for (ScatterPlotPointDto row : rawRows) {
            if (row == null) {
                continue;
            }

            String journalName = row.label();
            Double xValue = row.xValue();
            Double yValue = row.yValue();

            if (journalName == null || journalName.isBlank()) {
                journalName = "Journal #" + row.id();
            }

            if (!isValidNumber(xValue) || !isValidNumber(yValue)) {
                continue;
            }

            points.add(
                    new RankingScatterPoint(
                            journalName,
                            xMetric,
                            yMetric,
                            xValue,
                            yValue
                    )
            );
        }

        return points;
    }

    private List<VenueScatterPoint> buildVenueScatterPoints(
            String venueName,
            String venueType,
            int venueId,
            List<Object> yearlyStats
    ) {
        List<VenueScatterPoint> points = new ArrayList<>();

        if (yearlyStats == null || yearlyStats.isEmpty()) {
            return points;
        }

        for (Object stat : yearlyStats) {
            Integer year = extractYear(stat);
            Long articlesValue = extractMetricValue(stat, METRIC_ARTICLES);
            Long authorEntriesValue = extractMetricValue(stat, METRIC_AUTHOR_ENTRIES);

            if (year == null || articlesValue == null || authorEntriesValue == null) {
                continue;
            }

            if (articlesValue <= 0) {
                continue;
            }

            double articlesPerYear = articlesValue;
            double avgAuthorsPerArticle = (double) authorEntriesValue / articlesValue;

            points.add(
                    new VenueScatterPoint(
                            venueName,
                            venueType,
                            venueId,
                            year,
                            articlesPerYear,
                            avgAuthorsPerArticle
                    )
            );
        }

        return points;
    }

    private boolean isValidNumber(Double value) {
        return value != null
                && !Double.isNaN(value)
                && !Double.isInfinite(value);
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
        }

        if (stat instanceof ConferenceYearlyStatsDto conferenceStat) {
            if (METRIC_ARTICLES.equals(metric)) {
                return conferenceStat.totalArticles();
            }

            if (METRIC_AUTHOR_ENTRIES.equals(metric)) {
                return conferenceStat.totalAuthorOccurrences();
            }
        }

        return null;
    }

    public String getVenueKey(VenueScatterSeries venueSeries) {
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

    public record YearRange(
            Integer startYear,
            Integer endYear
    ) {
    }

    public record VenueScatterSeries(
            String name,
            String type,
            int venueId,
            List<VenueScatterPoint> points
    ) {
    }

    public record VenueScatterPoint(
            String venueName,
            String venueType,
            int venueId,
            int year,
            double articlesPerYear,
            double avgAuthorsPerArticle
    ) {
    }

    public record RankingScatterPoint(
            String journalName,
            String xMetric,
            String yMetric,
            double xValue,
            double yValue
    ) {
    }
}