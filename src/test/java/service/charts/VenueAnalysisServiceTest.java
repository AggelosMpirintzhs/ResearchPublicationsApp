package service.charts;

import dto.conference.ConferenceSearchResultDto;
import dto.conference.ConferenceYearlyStatsDto;
import dto.journal.JournalSearchResultDto;
import dto.journal.JournalYearlyStatsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.ConferenceService;
import service.JournalService;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VenueAnalysisServiceTest {

    @Mock
    private JournalService journalService;

    @Mock
    private ConferenceService conferenceService;

    private VenueAnalysisService venueAnalysisService;

    @BeforeEach
    void setUp() throws Exception {
        venueAnalysisService = new VenueAnalysisService();
        injectServiceMocks();
    }

    @Test
    void getVenueTypes_returnsExpectedVenueTypes() {
        String[] result = venueAnalysisService.getVenueTypes();

        assertArrayEquals(
                new String[]{"Journal", "Conference"},
                result
        );
    }

    @Test
    void getLineMetrics_returnsExpectedLineMetrics() {
        String[] result = venueAnalysisService.getLineMetrics();

        assertArrayEquals(
                new String[]{
                        "Published articles",
                        "Total author entries",
                        "Distinct authors"
                },
                result
        );
    }

    @Test
    void getCurrentYear_returnsSystemCurrentYear() {
        int result = venueAnalysisService.getCurrentYear();

        assertEquals(LocalDate.now().getYear(), result);
    }

    @Test
    void buildYearList_usesDefaultMinimumYear_whenMinimumYearIsNull() {
        int currentYear = LocalDate.now().getYear();

        List<Integer> result = venueAnalysisService.buildYearList(null);

        assertFalse(result.isEmpty());
        assertEquals(currentYear, result.get(0));
        assertEquals(VenueAnalysisService.DEFAULT_MIN_YEAR, result.get(result.size() - 1));
    }

    @Test
    void buildYearList_usesGivenMinimumYear() {
        int currentYear = LocalDate.now().getYear();
        int minimumYear = currentYear - 3;

        List<Integer> result = venueAnalysisService.buildYearList(minimumYear);

        assertEquals(4, result.size());
        assertEquals(currentYear, result.get(0));
        assertEquals(currentYear - 1, result.get(1));
        assertEquals(currentYear - 2, result.get(2));
        assertEquals(minimumYear, result.get(3));
    }

    @Test
    void validateYearRange_returnsExpectedRange_whenRangeIsValid() {
        VenueAnalysisService.YearRange result =
                venueAnalysisService.validateYearRange(2010, 2020);

        assertEquals(2010, result.startYear());
        assertEquals(2020, result.endYear());
    }

    @Test
    void validateYearRange_allowsNullYears() {
        VenueAnalysisService.YearRange result =
                venueAnalysisService.validateYearRange(null, null);

        assertNull(result.startYear());
        assertNull(result.endYear());
    }

    @Test
    void validateYearRange_throwsException_whenEndYearIsBeforeStartYear() {
        assertThrows(
                IllegalArgumentException.class,
                () -> venueAnalysisService.validateYearRange(2025, 2020)
        );
    }

    @Test
    void validateLineMetric_doesNotThrow_whenMetricIsValidText() {
        assertDoesNotThrow(
                () -> venueAnalysisService.validateLineMetric(VenueAnalysisService.METRIC_ARTICLES)
        );
    }

    @Test
    void validateLineMetric_throwsException_whenMetricIsNullOrBlank() {
        assertThrows(
                IllegalArgumentException.class,
                () -> venueAnalysisService.validateLineMetric(null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> venueAnalysisService.validateLineMetric("   ")
        );
    }

    @Test
    void searchVenues_returnsEmptyListAndDoesNotCallServices_whenVenueTypeIsNullOrBlank() {
        assertTrue(venueAnalysisService.searchVenues(null, "data", 10).isEmpty());
        assertTrue(venueAnalysisService.searchVenues("   ", "data", 10).isEmpty());

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void searchVenues_returnsEmptyListAndDoesNotCallServices_whenSearchTextIsNullOrBlank() {
        assertTrue(venueAnalysisService.searchVenues(VenueAnalysisService.TYPE_JOURNAL, null, 10).isEmpty());
        assertTrue(venueAnalysisService.searchVenues(VenueAnalysisService.TYPE_JOURNAL, "   ", 10).isEmpty());

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void searchVenues_callsJournalServiceAndSortsResultsByRelevance() {
        JournalSearchResultDto exactMatch =
                journalSearchResult(1, "Data");

        JournalSearchResultDto startsWithMatch =
                journalSearchResult(2, "Data Mining Journal");

        JournalSearchResultDto containsMatch =
                journalSearchResult(3, "Big Data Journal");

        JournalSearchResultDto noMatch =
                journalSearchResult(4, "Algorithms");

        when(journalService.searchJournals("data", 10))
                .thenReturn(List.of(
                        noMatch,
                        containsMatch,
                        startsWithMatch,
                        exactMatch
                ));

        List<Object> result =
                venueAnalysisService.searchVenues(
                        VenueAnalysisService.TYPE_JOURNAL,
                        "data",
                        10
                );

        assertEquals(4, result.size());
        assertSame(exactMatch, result.get(0));
        assertSame(startsWithMatch, result.get(1));
        assertSame(containsMatch, result.get(2));
        assertSame(noMatch, result.get(3));

        verify(journalService).searchJournals("data", 10);
        verifyNoInteractions(conferenceService);
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void searchVenues_callsConferenceServiceAndSortsResultsByRelevance() {
        ConferenceSearchResultDto exactAcronymMatch =
                conferenceSearchResult(1, "SIGMOD", "International Conference on Management of Data");

        ConferenceSearchResultDto titleStartsWithMatch =
                conferenceSearchResult(2, "OTHER", "SIGMOD Workshop");

        ConferenceSearchResultDto titleContainsMatch =
                conferenceSearchResult(3, "DBCONF", "Modern SIGMOD Research");

        ConferenceSearchResultDto noMatch =
                conferenceSearchResult(4, "VLDB", "Very Large Data Bases");

        when(conferenceService.searchConferences("SIGMOD", 10))
                .thenReturn(List.of(
                        noMatch,
                        titleContainsMatch,
                        exactAcronymMatch,
                        titleStartsWithMatch
                ));

        List<Object> result =
                venueAnalysisService.searchVenues(
                        VenueAnalysisService.TYPE_CONFERENCE,
                        "SIGMOD",
                        10
                );

        assertEquals(4, result.size());
        assertSame(exactAcronymMatch, result.get(0));
        assertSame(titleStartsWithMatch, result.get(1));
        assertSame(titleContainsMatch, result.get(2));
        assertSame(noMatch, result.get(3));

        verify(conferenceService).searchConferences("SIGMOD", 10);
        verifyNoInteractions(journalService);
        verifyNoMoreInteractions(conferenceService);
    }

    @Test
    void searchVenues_returnsEmptyList_whenVenueTypeIsUnknown() {
        List<Object> result =
                venueAnalysisService.searchVenues("Unknown", "data", 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void createSelectedVenue_returnsSelectedVenue_whenInputIsValid() {
        JournalSearchResultDto journal =
                journalSearchResult(1, "Data Journal");

        VenueAnalysisService.SelectedVenue result =
                venueAnalysisService.createSelectedVenue(
                        VenueAnalysisService.TYPE_JOURNAL,
                        journal
                );

        assertEquals(VenueAnalysisService.TYPE_JOURNAL, result.type());
        assertSame(journal, result.venue());
    }

    @Test
    void createSelectedVenue_throwsException_whenVenueTypeIsNullOrBlank() {
        JournalSearchResultDto journal =
                journalSearchResult(1, "Data Journal");

        assertThrows(
                IllegalArgumentException.class,
                () -> venueAnalysisService.createSelectedVenue(null, journal)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> venueAnalysisService.createSelectedVenue("   ", journal)
        );
    }

    @Test
    void createSelectedVenue_throwsException_whenVenueIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> venueAnalysisService.createSelectedVenue(
                        VenueAnalysisService.TYPE_JOURNAL,
                        null
                )
        );
    }

    @Test
    void alreadySelected_returnsFalse_whenListIsNullEmptyOrNewVenueIsNull() {
        JournalSearchResultDto journal =
                journalSearchResult(1, "Data Journal");

        VenueAnalysisService.SelectedVenue selectedVenue =
                new VenueAnalysisService.SelectedVenue(
                        VenueAnalysisService.TYPE_JOURNAL,
                        journal
                );

        assertFalse(venueAnalysisService.alreadySelected(null, selectedVenue));
        assertFalse(venueAnalysisService.alreadySelected(List.of(), selectedVenue));
        assertFalse(venueAnalysisService.alreadySelected(List.of(selectedVenue), null));
    }

    @Test
    void alreadySelected_returnsTrue_whenSameTypeAndSameVenueIdExist() {
        JournalSearchResultDto journal =
                journalSearchResult(1, "Data Journal");

        JournalSearchResultDto sameJournal =
                journalSearchResult(1, "Data Journal Updated");

        VenueAnalysisService.SelectedVenue existing =
                new VenueAnalysisService.SelectedVenue(
                        VenueAnalysisService.TYPE_JOURNAL,
                        journal
                );

        VenueAnalysisService.SelectedVenue newSelected =
                new VenueAnalysisService.SelectedVenue(
                        VenueAnalysisService.TYPE_JOURNAL,
                        sameJournal
                );

        assertTrue(venueAnalysisService.alreadySelected(List.of(existing), newSelected));
    }

    @Test
    void alreadySelected_returnsFalse_whenTypeIsDifferentEvenIfIdIsSame() {
        JournalSearchResultDto journal =
                journalSearchResult(1, "Data Journal");

        ConferenceSearchResultDto conference =
                conferenceSearchResult(1, "DATA", "Data Conference");

        VenueAnalysisService.SelectedVenue existing =
                new VenueAnalysisService.SelectedVenue(
                        VenueAnalysisService.TYPE_JOURNAL,
                        journal
                );

        VenueAnalysisService.SelectedVenue newSelected =
                new VenueAnalysisService.SelectedVenue(
                        VenueAnalysisService.TYPE_CONFERENCE,
                        conference
                );

        assertFalse(venueAnalysisService.alreadySelected(List.of(existing), newSelected));
    }

    @Test
    void loadVenueChartSeries_returnsEmptyList_whenSelectedVenuesIsNullOrEmpty() {
        VenueAnalysisService.YearRange yearRange =
                new VenueAnalysisService.YearRange(2010, 2020);

        assertTrue(venueAnalysisService.loadVenueChartSeries(null, yearRange).isEmpty());
        assertTrue(venueAnalysisService.loadVenueChartSeries(List.of(), yearRange).isEmpty());

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void loadVenueChartSeries_loadsJournalStatsAndBuildsYearlyStats() {
        JournalSearchResultDto journal =
                journalSearchResult(5, "Data Journal");

        VenueAnalysisService.SelectedVenue selectedVenue =
                new VenueAnalysisService.SelectedVenue(
                        VenueAnalysisService.TYPE_JOURNAL,
                        journal
                );

        VenueAnalysisService.YearRange yearRange =
                new VenueAnalysisService.YearRange(2010, 2020);

        JournalYearlyStatsDto stat2020 =
                journalYearlyStat(2020, 10L, 30L, 8L);

        JournalYearlyStatsDto statWithoutYear =
                journalYearlyStat(null, 20L, 60L, 15L);

        when(journalService.getJournalYearlyStats(5, 2010, 2020))
                .thenReturn(List.of(stat2020, statWithoutYear));

        List<VenueAnalysisService.VenueChartSeries> result =
                venueAnalysisService.loadVenueChartSeries(
                        List.of(selectedVenue),
                        yearRange
                );

        assertEquals(1, result.size());

        VenueAnalysisService.VenueChartSeries series = result.get(0);

        assertEquals("Journal: Data Journal", series.name());
        assertEquals(VenueAnalysisService.TYPE_JOURNAL, series.type());
        assertEquals(5, series.venueId());
        assertEquals(1, series.yearlyStats().size());

        VenueAnalysisService.VenueYearlyStats yearlyStats =
                series.yearlyStats().get(0);

        assertEquals(2020, yearlyStats.year());
        assertEquals(10L, yearlyStats.totalArticles());
        assertEquals(30L, yearlyStats.totalAuthorEntries());
        assertEquals(8L, yearlyStats.distinctAuthors());

        verify(journalService).getJournalYearlyStats(5, 2010, 2020);
        verifyNoInteractions(conferenceService);
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void loadVenueChartSeries_loadsConferenceStatsAndBuildsYearlyStats() {
        ConferenceSearchResultDto conference =
                conferenceSearchResult(7, "VLDB", "Very Large Data Bases");

        VenueAnalysisService.SelectedVenue selectedVenue =
                new VenueAnalysisService.SelectedVenue(
                        VenueAnalysisService.TYPE_CONFERENCE,
                        conference
                );

        VenueAnalysisService.YearRange yearRange =
                new VenueAnalysisService.YearRange(2015, 2022);

        ConferenceYearlyStatsDto stat2022 =
                conferenceYearlyStat(2022, 20L, 100L, 40L);

        when(conferenceService.getConferenceYearlyStats(7, 2015, 2022))
                .thenReturn(List.of(stat2022));

        List<VenueAnalysisService.VenueChartSeries> result =
                venueAnalysisService.loadVenueChartSeries(
                        List.of(selectedVenue),
                        yearRange
                );

        assertEquals(1, result.size());

        VenueAnalysisService.VenueChartSeries series = result.get(0);

        assertEquals("Conference: VLDB - Very Large Data Bases", series.name());
        assertEquals(VenueAnalysisService.TYPE_CONFERENCE, series.type());
        assertEquals(7, series.venueId());
        assertEquals(1, series.yearlyStats().size());

        VenueAnalysisService.VenueYearlyStats yearlyStats =
                series.yearlyStats().get(0);

        assertEquals(2022, yearlyStats.year());
        assertEquals(20L, yearlyStats.totalArticles());
        assertEquals(100L, yearlyStats.totalAuthorEntries());
        assertEquals(40L, yearlyStats.distinctAuthors());

        verify(conferenceService).getConferenceYearlyStats(7, 2015, 2022);
        verifyNoInteractions(journalService);
        verifyNoMoreInteractions(conferenceService);
    }

    @Test
    void loadVenueChartSeries_skipsNullSelectedVenue() {
        VenueAnalysisService.YearRange yearRange =
                new VenueAnalysisService.YearRange(2010, 2020);

        List<VenueAnalysisService.SelectedVenue> selectedVenues = new ArrayList<>();
        selectedVenues.add(null);

        List<VenueAnalysisService.VenueChartSeries> result =
                venueAnalysisService.loadVenueChartSeries(selectedVenues, yearRange);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void loadVenueChartSeries_throwsException_whenJournalIdIsInvalid() {
        JournalSearchResultDto journal =
                journalSearchResult(null, "Data Journal");

        VenueAnalysisService.SelectedVenue selectedVenue =
                new VenueAnalysisService.SelectedVenue(
                        VenueAnalysisService.TYPE_JOURNAL,
                        journal
                );

        VenueAnalysisService.YearRange yearRange =
                new VenueAnalysisService.YearRange(2010, 2020);

        assertThrows(
                IllegalArgumentException.class,
                () -> venueAnalysisService.loadVenueChartSeries(
                        List.of(selectedVenue),
                        yearRange
                )
        );

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void loadVenueChartSeries_throwsException_whenConferenceIdIsInvalid() {
        ConferenceSearchResultDto conference =
                conferenceSearchResult(0, "VLDB", "Very Large Data Bases");

        VenueAnalysisService.SelectedVenue selectedVenue =
                new VenueAnalysisService.SelectedVenue(
                        VenueAnalysisService.TYPE_CONFERENCE,
                        conference
                );

        VenueAnalysisService.YearRange yearRange =
                new VenueAnalysisService.YearRange(2010, 2020);

        assertThrows(
                IllegalArgumentException.class,
                () -> venueAnalysisService.loadVenueChartSeries(
                        List.of(selectedVenue),
                        yearRange
                )
        );

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void getLineMetricValue_returnsExpectedMetricValues() {
        VenueAnalysisService.VenueYearlyStats yearlyStats =
                new VenueAnalysisService.VenueYearlyStats(
                        2020,
                        10L,
                        30L,
                        8L
                );

        assertEquals(
                10L,
                venueAnalysisService.getLineMetricValue(
                        yearlyStats,
                        VenueAnalysisService.METRIC_ARTICLES
                )
        );

        assertEquals(
                30L,
                venueAnalysisService.getLineMetricValue(
                        yearlyStats,
                        VenueAnalysisService.METRIC_AUTHOR_ENTRIES
                )
        );

        assertEquals(
                8L,
                venueAnalysisService.getLineMetricValue(
                        yearlyStats,
                        VenueAnalysisService.METRIC_DISTINCT_AUTHORS
                )
        );
    }

    @Test
    void getLineMetricValue_returnsNull_whenInputIsNullOrMetricIsUnknown() {
        VenueAnalysisService.VenueYearlyStats yearlyStats =
                new VenueAnalysisService.VenueYearlyStats(
                        2020,
                        10L,
                        30L,
                        8L
                );

        assertNull(venueAnalysisService.getLineMetricValue(null, VenueAnalysisService.METRIC_ARTICLES));
        assertNull(venueAnalysisService.getLineMetricValue(yearlyStats, null));
        assertNull(venueAnalysisService.getLineMetricValue(yearlyStats, "Unknown metric"));
    }

    @Test
    void calculateVenueAggregate_returnsZeroAggregate_whenSeriesIsNullOrEmpty() {
        VenueAnalysisService.VenueAggregate nullAggregate =
                venueAnalysisService.calculateVenueAggregate(null);

        assertEquals(0L, nullAggregate.totalArticles());
        assertEquals(0L, nullAggregate.totalAuthorEntries());
        assertEquals(0, nullAggregate.activeYears());
        assertEquals(0.0, nullAggregate.avgArticlesPerYear());
        assertEquals(0.0, nullAggregate.avgAuthorEntriesPerYear());

        VenueAnalysisService.VenueChartSeries emptySeries =
                new VenueAnalysisService.VenueChartSeries(
                        "Journal: Empty",
                        VenueAnalysisService.TYPE_JOURNAL,
                        1,
                        List.of()
                );

        VenueAnalysisService.VenueAggregate emptyAggregate =
                venueAnalysisService.calculateVenueAggregate(emptySeries);

        assertEquals(0L, emptyAggregate.totalArticles());
        assertEquals(0L, emptyAggregate.totalAuthorEntries());
        assertEquals(0, emptyAggregate.activeYears());
        assertEquals(0.0, emptyAggregate.avgArticlesPerYear());
        assertEquals(0.0, emptyAggregate.avgAuthorEntriesPerYear());
    }

    @Test
    void calculateVenueAggregate_sumsValuesAndCalculatesAverages() {
        VenueAnalysisService.VenueYearlyStats stats2020 =
                new VenueAnalysisService.VenueYearlyStats(
                        2020,
                        10L,
                        30L,
                        8L
                );

        VenueAnalysisService.VenueYearlyStats stats2021 =
                new VenueAnalysisService.VenueYearlyStats(
                        2021,
                        20L,
                        50L,
                        15L
                );

        VenueAnalysisService.VenueChartSeries series =
                new VenueAnalysisService.VenueChartSeries(
                        "Journal: Data Journal",
                        VenueAnalysisService.TYPE_JOURNAL,
                        1,
                        List.of(stats2020, stats2021)
                );

        VenueAnalysisService.VenueAggregate result =
                venueAnalysisService.calculateVenueAggregate(series);

        assertEquals(30L, result.totalArticles());
        assertEquals(80L, result.totalAuthorEntries());
        assertEquals(2, result.activeYears());
        assertEquals(15.0, result.avgArticlesPerYear());
        assertEquals(40.0, result.avgAuthorEntriesPerYear());
    }

    @Test
    void calculateVenueAggregate_skipsYearWhenArticlesAndAuthorEntriesAreBothNull() {
        VenueAnalysisService.VenueYearlyStats skippedStats =
                new VenueAnalysisService.VenueYearlyStats(
                        2020,
                        null,
                        null,
                        5L
                );

        VenueAnalysisService.VenueYearlyStats countedStats =
                new VenueAnalysisService.VenueYearlyStats(
                        2021,
                        10L,
                        null,
                        8L
                );

        VenueAnalysisService.VenueChartSeries series =
                new VenueAnalysisService.VenueChartSeries(
                        "Journal: Data Journal",
                        VenueAnalysisService.TYPE_JOURNAL,
                        1,
                        List.of(skippedStats, countedStats)
                );

        VenueAnalysisService.VenueAggregate result =
                venueAnalysisService.calculateVenueAggregate(series);

        assertEquals(10L, result.totalArticles());
        assertEquals(0L, result.totalAuthorEntries());
        assertEquals(1, result.activeYears());
        assertEquals(10.0, result.avgArticlesPerYear());
        assertEquals(0.0, result.avgAuthorEntriesPerYear());
    }

    @Test
    void getBarMetricValue_returnsExpectedValues() {
        VenueAnalysisService.VenueAggregate aggregate =
                new VenueAnalysisService.VenueAggregate(
                        100L,
                        300L,
                        5,
                        20.0,
                        60.0
                );

        assertEquals(
                100.0,
                venueAnalysisService.getBarMetricValue(
                        aggregate,
                        VenueAnalysisService.BAR_TOTAL_ARTICLES
                )
        );

        assertEquals(
                20.0,
                venueAnalysisService.getBarMetricValue(
                        aggregate,
                        VenueAnalysisService.BAR_AVG_ARTICLES_PER_YEAR
                )
        );

        assertEquals(
                60.0,
                venueAnalysisService.getBarMetricValue(
                        aggregate,
                        VenueAnalysisService.BAR_AVG_AUTHOR_ENTRIES_PER_YEAR
                )
        );
    }

    @Test
    void getBarMetricValue_returnsZero_whenInputIsNullOrMetricIsUnknown() {
        VenueAnalysisService.VenueAggregate aggregate =
                new VenueAnalysisService.VenueAggregate(
                        100L,
                        300L,
                        5,
                        20.0,
                        60.0
                );

        assertEquals(0.0, venueAnalysisService.getBarMetricValue(null, VenueAnalysisService.BAR_TOTAL_ARTICLES));
        assertEquals(0.0, venueAnalysisService.getBarMetricValue(aggregate, null));
        assertEquals(0.0, venueAnalysisService.getBarMetricValue(aggregate, "Unknown metric"));
    }

    @Test
    void getVenueId_returnsExpectedIds() {
        assertEquals(1, venueAnalysisService.getVenueId(journalSearchResult(1, "Data Journal")));
        assertEquals(2, venueAnalysisService.getVenueId(conferenceSearchResult(2, "VLDB", "Very Large Data Bases")));
        assertEquals(-1, venueAnalysisService.getVenueId(journalSearchResult(null, "Data Journal")));
        assertEquals(-1, venueAnalysisService.getVenueId("unknown"));
    }

    @Test
    void getRawVenueDisplayName_returnsExpectedJournalNames() {
        assertEquals(
                "Data Journal",
                venueAnalysisService.getRawVenueDisplayName(journalSearchResult(1, "Data Journal"))
        );

        assertEquals(
                "Journal #2",
                venueAnalysisService.getRawVenueDisplayName(journalSearchResult(2, ""))
        );
    }

    @Test
    void getRawVenueDisplayName_returnsExpectedConferenceNames() {
        assertEquals(
                "VLDB - Very Large Data Bases",
                venueAnalysisService.getRawVenueDisplayName(
                        conferenceSearchResult(1, "VLDB", "Very Large Data Bases")
                )
        );

        assertEquals(
                "SIGMOD",
                venueAnalysisService.getRawVenueDisplayName(
                        conferenceSearchResult(2, "SIGMOD", "")
                )
        );

        assertEquals(
                "International Database Conference",
                venueAnalysisService.getRawVenueDisplayName(
                        conferenceSearchResult(3, "", "International Database Conference")
                )
        );

        assertEquals(
                "Conference #4",
                venueAnalysisService.getRawVenueDisplayName(
                        conferenceSearchResult(4, "", "")
                )
        );
    }

    @Test
    void getRawVenueDisplayName_returnsUnknownVenue_whenTypeIsUnsupported() {
        assertEquals(
                "Unknown venue",
                venueAnalysisService.getRawVenueDisplayName("plain string")
        );
    }

    @Test
    void getVenueDisplayName_returnsTypeAndRawName() {
        JournalSearchResultDto journal =
                journalSearchResult(1, "Data Journal");

        VenueAnalysisService.SelectedVenue selectedVenue =
                new VenueAnalysisService.SelectedVenue(
                        VenueAnalysisService.TYPE_JOURNAL,
                        journal
                );

        assertEquals(
                "Journal: Data Journal",
                venueAnalysisService.getVenueDisplayName(selectedVenue)
        );
    }

    @Test
    void getVenueKey_returnsEmptyString_whenSeriesIsNull() {
        assertEquals("", venueAnalysisService.getVenueKey(null));
    }

    @Test
    void getVenueKey_returnsExpectedKey() {
        VenueAnalysisService.VenueChartSeries series =
                new VenueAnalysisService.VenueChartSeries(
                        "Journal: Data Journal",
                        VenueAnalysisService.TYPE_JOURNAL,
                        5,
                        List.of()
                );

        assertEquals(
                "Journal#5",
                venueAnalysisService.getVenueKey(series)
        );
    }

    private void injectServiceMocks() throws Exception {
        Field journalField = VenueAnalysisService.class.getDeclaredField("journalService");
        journalField.setAccessible(true);
        journalField.set(venueAnalysisService, journalService);

        Field conferenceField = VenueAnalysisService.class.getDeclaredField("conferenceService");
        conferenceField.setAccessible(true);
        conferenceField.set(venueAnalysisService, conferenceService);
    }

    private JournalSearchResultDto journalSearchResult(
            Integer journalId,
            String journalName
    ) {
        JournalSearchResultDto dto = mock(JournalSearchResultDto.class);

        lenient().when(dto.journalId()).thenReturn(journalId);
        lenient().when(dto.journalName()).thenReturn(journalName);

        return dto;
    }

    private ConferenceSearchResultDto conferenceSearchResult(
            Integer conferenceId,
            String acronym,
            String conferenceTitle
    ) {
        ConferenceSearchResultDto dto = mock(ConferenceSearchResultDto.class);

        lenient().when(dto.conferenceId()).thenReturn(conferenceId);
        lenient().when(dto.acronym()).thenReturn(acronym);
        lenient().when(dto.conferenceTitle()).thenReturn(conferenceTitle);

        return dto;
    }

    private JournalYearlyStatsDto journalYearlyStat(
            Integer year,
            Long totalArticles,
            Long totalAuthorOccurrences,
            Long distinctAuthors
    ) {
        JournalYearlyStatsDto dto = mock(JournalYearlyStatsDto.class);

        lenient().when(dto.year()).thenReturn(year);
        lenient().when(dto.totalArticles()).thenReturn(totalArticles);
        lenient().when(dto.totalAuthorOccurrences()).thenReturn(totalAuthorOccurrences);
        lenient().when(dto.distinctAuthors()).thenReturn(distinctAuthors);

        return dto;
    }

    private ConferenceYearlyStatsDto conferenceYearlyStat(
            Integer year,
            Long totalArticles,
            Long totalAuthorOccurrences,
            Long distinctAuthors
    ) {
        ConferenceYearlyStatsDto dto = mock(ConferenceYearlyStatsDto.class);

        lenient().when(dto.year()).thenReturn(year);
        lenient().when(dto.totalArticles()).thenReturn(totalArticles);
        lenient().when(dto.totalAuthorOccurrences()).thenReturn(totalAuthorOccurrences);
        lenient().when(dto.distinctAuthors()).thenReturn(distinctAuthors);

        return dto;
    }
}