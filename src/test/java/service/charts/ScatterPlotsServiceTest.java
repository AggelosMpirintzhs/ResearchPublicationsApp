package service.charts;

import dto.chart.ScatterPlotPointDto;
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
class ScatterPlotsServiceTest {

    @Mock
    private JournalService journalService;

    @Mock
    private ConferenceService conferenceService;

    private ScatterPlotsService scatterPlotsService;

    @BeforeEach
    void setUp() throws Exception {
        scatterPlotsService = new ScatterPlotsService();
        injectServiceMocks();
    }

    @Test
    void getVenueTypes_returnsExpectedVenueTypes() {
        String[] result = scatterPlotsService.getVenueTypes();

        assertArrayEquals(
                new String[]{"Journal", "Conference"},
                result
        );
    }

    @Test
    void getRankingMetrics_returnsExpectedMetrics() {
        String[] result = scatterPlotsService.getRankingMetrics();

        assertArrayEquals(
                new String[]{
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
                },
                result
        );
    }

    @Test
    void buildYearList_usesDefaultMinimumYear_whenMinimumYearIsNull() {
        int currentYear = LocalDate.now().getYear();

        List<Integer> result = scatterPlotsService.buildYearList(null);

        assertFalse(result.isEmpty());
        assertEquals(currentYear, result.get(0));
        assertEquals(ScatterPlotsService.DEFAULT_MIN_YEAR, result.get(result.size() - 1));
    }

    @Test
    void buildYearList_usesGivenMinimumYear() {
        int currentYear = LocalDate.now().getYear();
        int minimumYear = currentYear - 3;

        List<Integer> result = scatterPlotsService.buildYearList(minimumYear);

        assertEquals(4, result.size());
        assertEquals(currentYear, result.get(0));
        assertEquals(currentYear - 1, result.get(1));
        assertEquals(currentYear - 2, result.get(2));
        assertEquals(minimumYear, result.get(3));
    }

    @Test
    void validateYearRange_returnsExpectedYearRange_whenRangeIsValid() {
        ScatterPlotsService.YearRange result =
                scatterPlotsService.validateYearRange(2010, 2020);

        assertEquals(2010, result.startYear());
        assertEquals(2020, result.endYear());
    }

    @Test
    void validateYearRange_allowsNullYears() {
        ScatterPlotsService.YearRange result =
                scatterPlotsService.validateYearRange(null, null);

        assertNull(result.startYear());
        assertNull(result.endYear());
    }

    @Test
    void validateYearRange_throwsException_whenEndYearIsBeforeStartYear() {
        assertThrows(
                IllegalArgumentException.class,
                () -> scatterPlotsService.validateYearRange(2025, 2020)
        );
    }

    @Test
    void validateRankingMetrics_doesNotThrow_whenMetricsAreValidAndDifferent() {
        assertDoesNotThrow(
                () -> scatterPlotsService.validateRankingMetrics("SJR", "H index")
        );
    }

    @Test
    void validateRankingMetrics_throwsException_whenXMetricIsMissing() {
        assertThrows(
                IllegalArgumentException.class,
                () -> scatterPlotsService.validateRankingMetrics(null, "H index")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> scatterPlotsService.validateRankingMetrics("   ", "H index")
        );
    }

    @Test
    void validateRankingMetrics_throwsException_whenYMetricIsMissing() {
        assertThrows(
                IllegalArgumentException.class,
                () -> scatterPlotsService.validateRankingMetrics("SJR", null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> scatterPlotsService.validateRankingMetrics("SJR", "   ")
        );
    }

    @Test
    void validateRankingMetrics_throwsException_whenMetricsAreSame() {
        assertThrows(
                IllegalArgumentException.class,
                () -> scatterPlotsService.validateRankingMetrics("SJR", "SJR")
        );
    }

    @Test
    void resolveRankingLimit_returnsDefaultLimit_whenSelectedLimitIsNullBlankInvalidOrNonPositive() {
        assertEquals(100, scatterPlotsService.resolveRankingLimit(null, 100));
        assertEquals(100, scatterPlotsService.resolveRankingLimit("   ", 100));
        assertEquals(100, scatterPlotsService.resolveRankingLimit("abc", 100));
        assertEquals(100, scatterPlotsService.resolveRankingLimit("0", 100));
        assertEquals(100, scatterPlotsService.resolveRankingLimit("-5", 100));
    }

    @Test
    void resolveRankingLimit_returnsNull_whenSelectedLimitIsAll() {
        assertNull(scatterPlotsService.resolveRankingLimit("All", 100));
        assertNull(scatterPlotsService.resolveRankingLimit(" all ", 100));
    }

    @Test
    void resolveRankingLimit_returnsParsedLimit_whenSelectedLimitIsPositiveNumber() {
        assertEquals(250, scatterPlotsService.resolveRankingLimit("250", 100));
        assertEquals(50, scatterPlotsService.resolveRankingLimit(" 50 ", 100));
    }

    @Test
    void searchVenues_returnsEmptyListAndDoesNotCallServices_whenVenueTypeIsNullOrBlank() {
        assertTrue(scatterPlotsService.searchVenues(null, "data", 10).isEmpty());
        assertTrue(scatterPlotsService.searchVenues("   ", "data", 10).isEmpty());

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void searchVenues_returnsEmptyListAndDoesNotCallServices_whenSearchTextIsNullOrBlank() {
        assertTrue(scatterPlotsService.searchVenues(ScatterPlotsService.TYPE_JOURNAL, null, 10).isEmpty());
        assertTrue(scatterPlotsService.searchVenues(ScatterPlotsService.TYPE_JOURNAL, "   ", 10).isEmpty());

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
                scatterPlotsService.searchVenues(
                        ScatterPlotsService.TYPE_JOURNAL,
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
                scatterPlotsService.searchVenues(
                        ScatterPlotsService.TYPE_CONFERENCE,
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
                scatterPlotsService.searchVenues("Unknown", "data", 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void createSelectedVenue_returnsSelectedVenue_whenInputIsValid() {
        JournalSearchResultDto journal = journalSearchResult(1, "Data Journal");

        ScatterPlotsService.SelectedVenue result =
                scatterPlotsService.createSelectedVenue(
                        ScatterPlotsService.TYPE_JOURNAL,
                        journal
                );

        assertEquals(ScatterPlotsService.TYPE_JOURNAL, result.type());
        assertSame(journal, result.venue());
    }

    @Test
    void createSelectedVenue_throwsException_whenVenueTypeIsMissing() {
        JournalSearchResultDto journal = journalSearchResult(1, "Data Journal");

        assertThrows(
                IllegalArgumentException.class,
                () -> scatterPlotsService.createSelectedVenue(null, journal)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> scatterPlotsService.createSelectedVenue("   ", journal)
        );
    }

    @Test
    void createSelectedVenue_throwsException_whenVenueIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> scatterPlotsService.createSelectedVenue(
                        ScatterPlotsService.TYPE_JOURNAL,
                        null
                )
        );
    }

    @Test
    void alreadySelected_returnsFalse_whenListIsNullEmptyOrNewVenueIsNull() {
        JournalSearchResultDto journal = journalSearchResult(1, "Data Journal");

        ScatterPlotsService.SelectedVenue selectedVenue =
                new ScatterPlotsService.SelectedVenue(
                        ScatterPlotsService.TYPE_JOURNAL,
                        journal
                );

        assertFalse(scatterPlotsService.alreadySelected(null, selectedVenue));
        assertFalse(scatterPlotsService.alreadySelected(List.of(), selectedVenue));
        assertFalse(scatterPlotsService.alreadySelected(List.of(selectedVenue), null));
    }

    @Test
    void alreadySelected_returnsTrue_whenSameTypeAndSameVenueIdExist() {
        JournalSearchResultDto journal1 = journalSearchResult(1, "Data Journal");
        JournalSearchResultDto sameJournal = journalSearchResult(1, "Data Journal Updated");

        ScatterPlotsService.SelectedVenue existing =
                new ScatterPlotsService.SelectedVenue(
                        ScatterPlotsService.TYPE_JOURNAL,
                        journal1
                );

        ScatterPlotsService.SelectedVenue newSelected =
                new ScatterPlotsService.SelectedVenue(
                        ScatterPlotsService.TYPE_JOURNAL,
                        sameJournal
                );

        assertTrue(scatterPlotsService.alreadySelected(List.of(existing), newSelected));
    }

    @Test
    void alreadySelected_returnsFalse_whenTypeIsDifferentEvenIfIdIsSame() {
        JournalSearchResultDto journal = journalSearchResult(1, "Data Journal");
        ConferenceSearchResultDto conference = conferenceSearchResult(1, "DATA", "Data Conference");

        ScatterPlotsService.SelectedVenue existing =
                new ScatterPlotsService.SelectedVenue(
                        ScatterPlotsService.TYPE_JOURNAL,
                        journal
                );

        ScatterPlotsService.SelectedVenue newSelected =
                new ScatterPlotsService.SelectedVenue(
                        ScatterPlotsService.TYPE_CONFERENCE,
                        conference
                );

        assertFalse(scatterPlotsService.alreadySelected(List.of(existing), newSelected));
    }

    @Test
    void loadVenueScatterSeries_returnsEmptyList_whenSelectedVenuesIsNullOrEmpty() {
        ScatterPlotsService.YearRange yearRange =
                new ScatterPlotsService.YearRange(2010, 2020);

        assertTrue(scatterPlotsService.loadVenueScatterSeries(null, yearRange).isEmpty());
        assertTrue(scatterPlotsService.loadVenueScatterSeries(List.of(), yearRange).isEmpty());

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void loadVenueScatterSeries_loadsJournalStatsAndBuildsScatterPoints() {
        JournalSearchResultDto journal =
                journalSearchResult(5, "Data Journal");

        ScatterPlotsService.SelectedVenue selectedVenue =
                new ScatterPlotsService.SelectedVenue(
                        ScatterPlotsService.TYPE_JOURNAL,
                        journal
                );

        ScatterPlotsService.YearRange yearRange =
                new ScatterPlotsService.YearRange(2010, 2020);

        JournalYearlyStatsDto validStat =
                journalYearlyStat(2020, 10L, 30L);

        JournalYearlyStatsDto zeroArticlesStat =
                journalYearlyStat(2021, 0L, 50L);

        JournalYearlyStatsDto nullMetricStat =
                journalYearlyStat(2022, null, 20L);

        when(journalService.getJournalYearlyStats(5, 2010, 2020))
                .thenReturn(List.of(validStat, zeroArticlesStat, nullMetricStat));

        List<ScatterPlotsService.VenueScatterSeries> result =
                scatterPlotsService.loadVenueScatterSeries(
                        List.of(selectedVenue),
                        yearRange
                );

        assertEquals(1, result.size());

        ScatterPlotsService.VenueScatterSeries series = result.get(0);

        assertEquals("Journal: Data Journal", series.name());
        assertEquals(ScatterPlotsService.TYPE_JOURNAL, series.type());
        assertEquals(5, series.venueId());
        assertEquals(1, series.points().size());

        ScatterPlotsService.VenueScatterPoint point = series.points().get(0);

        assertEquals("Journal: Data Journal", point.venueName());
        assertEquals(ScatterPlotsService.TYPE_JOURNAL, point.venueType());
        assertEquals(5, point.venueId());
        assertEquals(2020, point.year());
        assertEquals(10.0, point.articlesPerYear());
        assertEquals(3.0, point.avgAuthorsPerArticle());

        verify(journalService).getJournalYearlyStats(5, 2010, 2020);
        verifyNoInteractions(conferenceService);
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void loadVenueScatterSeries_loadsConferenceStatsAndBuildsScatterPoints() {
        ConferenceSearchResultDto conference =
                conferenceSearchResult(7, "VLDB", "Very Large Data Bases");

        ScatterPlotsService.SelectedVenue selectedVenue =
                new ScatterPlotsService.SelectedVenue(
                        ScatterPlotsService.TYPE_CONFERENCE,
                        conference
                );

        ScatterPlotsService.YearRange yearRange =
                new ScatterPlotsService.YearRange(2015, 2022);

        ConferenceYearlyStatsDto validStat =
                conferenceYearlyStat(2022, 20L, 100L);

        when(conferenceService.getConferenceYearlyStats(7, 2015, 2022))
                .thenReturn(List.of(validStat));

        List<ScatterPlotsService.VenueScatterSeries> result =
                scatterPlotsService.loadVenueScatterSeries(
                        List.of(selectedVenue),
                        yearRange
                );

        assertEquals(1, result.size());

        ScatterPlotsService.VenueScatterSeries series = result.get(0);

        assertEquals("Conference: VLDB - Very Large Data Bases", series.name());
        assertEquals(ScatterPlotsService.TYPE_CONFERENCE, series.type());
        assertEquals(7, series.venueId());
        assertEquals(1, series.points().size());

        ScatterPlotsService.VenueScatterPoint point = series.points().get(0);

        assertEquals(2022, point.year());
        assertEquals(20.0, point.articlesPerYear());
        assertEquals(5.0, point.avgAuthorsPerArticle());

        verify(conferenceService).getConferenceYearlyStats(7, 2015, 2022);
        verifyNoInteractions(journalService);
        verifyNoMoreInteractions(conferenceService);
    }

    @Test
    void loadVenueScatterSeries_skipsNullSelectedVenue() {
        ScatterPlotsService.YearRange yearRange =
                new ScatterPlotsService.YearRange(2010, 2020);

        List<ScatterPlotsService.SelectedVenue> selectedVenues = new ArrayList<>();
        selectedVenues.add(null);

        List<ScatterPlotsService.VenueScatterSeries> result =
                scatterPlotsService.loadVenueScatterSeries(selectedVenues, yearRange);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void loadVenueScatterSeries_throwsException_whenJournalIdIsInvalid() {
        JournalSearchResultDto journal =
                journalSearchResult(null, "Data Journal");

        ScatterPlotsService.SelectedVenue selectedVenue =
                new ScatterPlotsService.SelectedVenue(
                        ScatterPlotsService.TYPE_JOURNAL,
                        journal
                );

        ScatterPlotsService.YearRange yearRange =
                new ScatterPlotsService.YearRange(2010, 2020);

        assertThrows(
                IllegalArgumentException.class,
                () -> scatterPlotsService.loadVenueScatterSeries(
                        List.of(selectedVenue),
                        yearRange
                )
        );

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void loadVenueScatterSeries_throwsException_whenConferenceIdIsInvalid() {
        ConferenceSearchResultDto conference =
                conferenceSearchResult(0, "VLDB", "Very Large Data Bases");

        ScatterPlotsService.SelectedVenue selectedVenue =
                new ScatterPlotsService.SelectedVenue(
                        ScatterPlotsService.TYPE_CONFERENCE,
                        conference
                );

        ScatterPlotsService.YearRange yearRange =
                new ScatterPlotsService.YearRange(2010, 2020);

        assertThrows(
                IllegalArgumentException.class,
                () -> scatterPlotsService.loadVenueScatterSeries(
                        List.of(selectedVenue),
                        yearRange
                )
        );

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void loadJournalRankingScatterData_convertsRowsAndFiltersInvalidValues() {
        ScatterPlotPointDto validRow =
                scatterPlotPoint(1, "Data Journal", 2.5, 10.0);

        ScatterPlotPointDto blankLabelRow =
                scatterPlotPoint(2, "   ", 3.5, 11.0);

        ScatterPlotPointDto nullXRow =
                scatterPlotPoint(3, "Invalid X", null, 12.0);

        ScatterPlotPointDto nanYRow =
                scatterPlotPoint(4, "Invalid Y", 5.0, Double.NaN);

        ScatterPlotPointDto infinityRow =
                scatterPlotPoint(5, "Infinity", Double.POSITIVE_INFINITY, 4.0);

        List<ScatterPlotPointDto> rawRows = new ArrayList<>();
        rawRows.add(validRow);
        rawRows.add(blankLabelRow);
        rawRows.add(nullXRow);
        rawRows.add(nanYRow);
        rawRows.add(infinityRow);
        rawRows.add(null);

        when(journalService.getJournalRankingScatterData("SJR", "H index", 100))
                .thenReturn(rawRows);

        List<ScatterPlotsService.RankingScatterPoint> result =
                scatterPlotsService.loadJournalRankingScatterData(
                        "SJR",
                        "H index",
                        100
                );

        assertEquals(2, result.size());

        ScatterPlotsService.RankingScatterPoint firstPoint = result.get(0);
        assertEquals("Data Journal", firstPoint.journalName());
        assertEquals("SJR", firstPoint.xMetric());
        assertEquals("H index", firstPoint.yMetric());
        assertEquals(2.5, firstPoint.xValue());
        assertEquals(10.0, firstPoint.yValue());

        ScatterPlotsService.RankingScatterPoint secondPoint = result.get(1);
        assertEquals("Journal #2", secondPoint.journalName());
        assertEquals(3.5, secondPoint.xValue());
        assertEquals(11.0, secondPoint.yValue());

        verify(journalService).getJournalRankingScatterData("SJR", "H index", 100);
        verifyNoMoreInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void loadJournalRankingScatterData_returnsEmptyList_whenRepositoryReturnsNullOrEmpty() {
        when(journalService.getJournalRankingScatterData("SJR", "H index", 100))
                .thenReturn(null);

        List<ScatterPlotsService.RankingScatterPoint> nullResult =
                scatterPlotsService.loadJournalRankingScatterData("SJR", "H index", 100);

        assertNotNull(nullResult);
        assertTrue(nullResult.isEmpty());

        clearInvocations(journalService);

        when(journalService.getJournalRankingScatterData("SJR", "H index", 100))
                .thenReturn(List.of());

        List<ScatterPlotsService.RankingScatterPoint> emptyResult =
                scatterPlotsService.loadJournalRankingScatterData("SJR", "H index", 100);

        assertNotNull(emptyResult);
        assertTrue(emptyResult.isEmpty());

        verify(journalService).getJournalRankingScatterData("SJR", "H index", 100);
        verifyNoMoreInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void getVenueId_returnsExpectedIds() {
        assertEquals(1, scatterPlotsService.getVenueId(journalSearchResult(1, "Data Journal")));
        assertEquals(2, scatterPlotsService.getVenueId(conferenceSearchResult(2, "VLDB", "Very Large Data Bases")));
        assertEquals(-1, scatterPlotsService.getVenueId(journalSearchResult(null, "Data Journal")));
        assertEquals(-1, scatterPlotsService.getVenueId("unknown"));
    }

    @Test
    void getRawVenueDisplayName_returnsExpectedJournalNames() {
        assertEquals(
                "Data Journal",
                scatterPlotsService.getRawVenueDisplayName(journalSearchResult(1, "Data Journal"))
        );

        assertEquals(
                "Journal #2",
                scatterPlotsService.getRawVenueDisplayName(journalSearchResult(2, ""))
        );
    }

    @Test
    void getRawVenueDisplayName_returnsExpectedConferenceNames() {
        assertEquals(
                "VLDB - Very Large Data Bases",
                scatterPlotsService.getRawVenueDisplayName(
                        conferenceSearchResult(1, "VLDB", "Very Large Data Bases")
                )
        );

        assertEquals(
                "SIGMOD",
                scatterPlotsService.getRawVenueDisplayName(
                        conferenceSearchResult(2, "SIGMOD", "")
                )
        );

        assertEquals(
                "International Database Conference",
                scatterPlotsService.getRawVenueDisplayName(
                        conferenceSearchResult(3, "", "International Database Conference")
                )
        );

        assertEquals(
                "Conference #4",
                scatterPlotsService.getRawVenueDisplayName(
                        conferenceSearchResult(4, "", "")
                )
        );
    }

    @Test
    void getRawVenueDisplayName_returnsUnknownVenue_whenTypeIsUnsupported() {
        assertEquals(
                "Unknown venue",
                scatterPlotsService.getRawVenueDisplayName("plain string")
        );
    }

    @Test
    void getVenueDisplayName_returnsTypeAndRawName() {
        JournalSearchResultDto journal =
                journalSearchResult(1, "Data Journal");

        ScatterPlotsService.SelectedVenue selectedVenue =
                new ScatterPlotsService.SelectedVenue(
                        ScatterPlotsService.TYPE_JOURNAL,
                        journal
                );

        assertEquals(
                "Journal: Data Journal",
                scatterPlotsService.getVenueDisplayName(selectedVenue)
        );
    }

    @Test
    void getVenueKey_returnsEmptyString_whenSeriesIsNull() {
        assertEquals("", scatterPlotsService.getVenueKey(null));
    }

    @Test
    void getVenueKey_returnsExpectedKey() {
        ScatterPlotsService.VenueScatterSeries series =
                new ScatterPlotsService.VenueScatterSeries(
                        "Journal: Data Journal",
                        ScatterPlotsService.TYPE_JOURNAL,
                        5,
                        List.of()
                );

        assertEquals(
                "Journal#5",
                scatterPlotsService.getVenueKey(series)
        );
    }

    private void injectServiceMocks() throws Exception {
        Field journalField = ScatterPlotsService.class.getDeclaredField("journalService");
        journalField.setAccessible(true);
        journalField.set(scatterPlotsService, journalService);

        Field conferenceField = ScatterPlotsService.class.getDeclaredField("conferenceService");
        conferenceField.setAccessible(true);
        conferenceField.set(scatterPlotsService, conferenceService);
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
            Long totalAuthorOccurrences
    ) {
        JournalYearlyStatsDto dto = mock(JournalYearlyStatsDto.class);

        lenient().when(dto.year()).thenReturn(year);
        lenient().when(dto.totalArticles()).thenReturn(totalArticles);
        lenient().when(dto.totalAuthorOccurrences()).thenReturn(totalAuthorOccurrences);

        return dto;
    }

    private ConferenceYearlyStatsDto conferenceYearlyStat(
            Integer year,
            Long totalArticles,
            Long totalAuthorOccurrences
    ) {
        ConferenceYearlyStatsDto dto = mock(ConferenceYearlyStatsDto.class);

        lenient().when(dto.year()).thenReturn(year);
        lenient().when(dto.totalArticles()).thenReturn(totalArticles);
        lenient().when(dto.totalAuthorOccurrences()).thenReturn(totalAuthorOccurrences);

        return dto;
    }

    private ScatterPlotPointDto scatterPlotPoint(
            Integer id,
            String label,
            Double xValue,
            Double yValue
    ) {
        ScatterPlotPointDto dto = mock(ScatterPlotPointDto.class);

        lenient().when(dto.id()).thenReturn(id);
        lenient().when(dto.label()).thenReturn(label);
        lenient().when(dto.xValue()).thenReturn(xValue);
        lenient().when(dto.yValue()).thenReturn(yValue);

        return dto;
    }
}