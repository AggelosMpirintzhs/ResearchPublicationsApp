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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock
    private JournalService journalService;

    @Mock
    private ConferenceService conferenceService;

    private VenueService venueService;

    @BeforeEach
    void setUp() {
        venueService = new VenueService(journalService, conferenceService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "ab",
            "a!"
    })
    void searchVenues_returnsEmptyListAndDoesNotCallAnyService_whenSearchTextIsTooShort(String searchText) {
        List<Object> result =
                venueService.searchVenues(VenueService.TYPE_JOURNAL, searchText, integer(10));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void searchVenues_returnsEmptyListAndDoesNotCallAnyService_whenSearchTextIsNull() {
        List<Object> result =
                venueService.searchVenues(VenueService.TYPE_JOURNAL, null, integer(10));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void searchVenues_callsJournalServiceAndSortsJournalResultsByRelevance() {
        JournalSearchResultDto exactMatch =
                journalSearchResult(integer(1), "Data", "Publisher A");

        JournalSearchResultDto startsWithMatch =
                journalSearchResult(integer(2), "Data Mining Journal", "Publisher B");

        JournalSearchResultDto containsMatch =
                journalSearchResult(integer(3), "Big Data Journal", "Publisher C");

        JournalSearchResultDto noMatch =
                journalSearchResult(integer(4), "Algorithms", "Publisher D");

        when(journalService.searchJournals("data", integer(10)))
                .thenReturn(List.of(
                        noMatch,
                        containsMatch,
                        startsWithMatch,
                        exactMatch
                ));

        List<Object> result =
                venueService.searchVenues(VenueService.TYPE_JOURNAL, "data", integer(10));

        assertEquals(4, result.size());
        assertSame(exactMatch, result.get(0));
        assertSame(startsWithMatch, result.get(1));
        assertSame(containsMatch, result.get(2));
        assertSame(noMatch, result.get(3));

        assertEquals(
                List.of("Data", "Data Mining Journal", "Big Data Journal", "Algorithms"),
                result.stream()
                        .map(item -> ((JournalSearchResultDto) item).journalName())
                        .toList()
        );

        verify(journalService).searchJournals("data", integer(10));
        verifyNoInteractions(conferenceService);
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void searchVenues_callsConferenceServiceAndSortsConferenceResultsByRelevance() {
        ConferenceSearchResultDto exactAcronymMatch =
                conferenceSearchResult(integer(1), "SIGMOD", "International Conference on Management of Data");

        ConferenceSearchResultDto titleStartsWithMatch =
                conferenceSearchResult(integer(2), "OTHER", "SIGMOD Workshop");

        ConferenceSearchResultDto titleContainsMatch =
                conferenceSearchResult(integer(3), "DBCONF", "Modern SIGMOD Research");

        ConferenceSearchResultDto noMatch =
                conferenceSearchResult(integer(4), "VLDB", "Very Large Data Bases");

        when(conferenceService.searchConferences("SIGMOD", integer(10)))
                .thenReturn(List.of(
                        noMatch,
                        titleContainsMatch,
                        exactAcronymMatch,
                        titleStartsWithMatch
                ));

        List<Object> result =
                venueService.searchVenues(VenueService.TYPE_CONFERENCE, "SIGMOD", integer(10));

        assertEquals(4, result.size());
        assertSame(exactAcronymMatch, result.get(0));
        assertSame(titleStartsWithMatch, result.get(1));
        assertSame(titleContainsMatch, result.get(2));
        assertSame(noMatch, result.get(3));

        assertEquals(
                List.of("SIGMOD", "OTHER", "DBCONF", "VLDB"),
                result.stream()
                        .map(item -> ((ConferenceSearchResultDto) item).acronym())
                        .toList()
        );

        verify(conferenceService).searchConferences("SIGMOD", integer(10));
        verifyNoInteractions(journalService);
        verifyNoMoreInteractions(conferenceService);
    }

    @Test
    void searchVenues_throwsException_whenVenueTypeIsInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> venueService.searchVenues("InvalidType", "data", integer(10))
        );

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void loadVenuePageData_returnsExpectedJournalPageData() {
        Integer journalId = integer(5);
        Integer startYear = integer(2010);
        Integer endYear = integer(2020);

        JournalSearchResultDto selectedJournal =
                journalSearchResult(journalId, "Data Journal", "Publisher A");

        JournalProfileDto profile = journalProfile(longValue(150));
        JournalRankingDto ranking = mock(JournalRankingDto.class);
        JournalYearlyStatsDto stats1 = mock(JournalYearlyStatsDto.class);
        JournalYearlyStatsDto stats2 = mock(JournalYearlyStatsDto.class);

        List<JournalYearlyStatsDto> yearlyStats =
                List.of(stats1, stats2);

        when(journalService.getJournalProfile(journalId, startYear, endYear))
                .thenReturn(Optional.of(profile));

        when(journalService.getJournalRanking(journalId))
                .thenReturn(Optional.of(ranking));

        when(journalService.getJournalYearlyStats(journalId, startYear, endYear))
                .thenReturn(yearlyStats);

        VenueService.VenuePageData result =
                venueService.loadVenuePageData(
                        VenueService.TYPE_JOURNAL,
                        selectedJournal,
                        startYear,
                        endYear
                );

        assertNotNull(result);
        assertEquals(VenueService.TYPE_JOURNAL, result.venueType());
        assertEquals(journalId.intValue(), result.venueId());
        assertSame(profile, result.profile());
        assertSame(ranking, result.ranking());
        assertEquals(2, result.yearlyStats().size());
        assertSame(stats1, result.yearlyStats().get(0));
        assertSame(stats2, result.yearlyStats().get(1));
        assertTrue(result.hasAnyData());
        assertTrue(result.hasArticleData());
        assertEquals(150L, result.expectedArticleCount());

        verify(journalService).getJournalProfile(journalId, startYear, endYear);
        verify(journalService).getJournalRanking(journalId);
        verify(journalService).getJournalYearlyStats(journalId, startYear, endYear);
        verifyNoInteractions(conferenceService);
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void loadVenuePageData_returnsExpectedConferencePageData() {
        Integer conferenceId = integer(8);
        Integer startYear = integer(2015);
        Integer endYear = integer(2022);

        ConferenceSearchResultDto selectedConference =
                conferenceSearchResult(conferenceId, "VLDB", "Very Large Data Bases");

        ConferenceProfileDto profile = conferenceProfile(longValue(80));
        ConferenceRankingDto ranking = mock(ConferenceRankingDto.class);
        ConferenceYearlyStatsDto stats = mock(ConferenceYearlyStatsDto.class);

        when(conferenceService.getConferenceProfile(conferenceId, startYear, endYear))
                .thenReturn(Optional.of(profile));

        when(conferenceService.getConferenceRanking(conferenceId))
                .thenReturn(Optional.of(ranking));

        when(conferenceService.getConferenceYearlyStats(conferenceId, startYear, endYear))
                .thenReturn(List.of(stats));

        VenueService.VenuePageData result =
                venueService.loadVenuePageData(
                        VenueService.TYPE_CONFERENCE,
                        selectedConference,
                        startYear,
                        endYear
                );

        assertNotNull(result);
        assertEquals(VenueService.TYPE_CONFERENCE, result.venueType());
        assertEquals(conferenceId.intValue(), result.venueId());
        assertSame(profile, result.profile());
        assertSame(ranking, result.ranking());
        assertEquals(1, result.yearlyStats().size());
        assertSame(stats, result.yearlyStats().get(0));
        assertTrue(result.hasAnyData());
        assertTrue(result.hasArticleData());
        assertEquals(80L, result.expectedArticleCount());

        verify(conferenceService).getConferenceProfile(conferenceId, startYear, endYear);
        verify(conferenceService).getConferenceRanking(conferenceId);
        verify(conferenceService).getConferenceYearlyStats(conferenceId, startYear, endYear);
        verifyNoInteractions(journalService);
        verifyNoMoreInteractions(conferenceService);
    }

    @Test
    void loadVenuePageData_returnsPageDataWithoutProfileOrRanking_whenRepositoryDataIsMissing() {
        Integer conferenceId = integer(8);
        Integer startYear = integer(2015);
        Integer endYear = integer(2022);

        ConferenceSearchResultDto selectedConference =
                conferenceSearchResult(conferenceId, "VLDB", "Very Large Data Bases");

        when(conferenceService.getConferenceProfile(conferenceId, startYear, endYear))
                .thenReturn(Optional.empty());

        when(conferenceService.getConferenceRanking(conferenceId))
                .thenReturn(Optional.empty());

        when(conferenceService.getConferenceYearlyStats(conferenceId, startYear, endYear))
                .thenReturn(List.of());

        VenueService.VenuePageData result =
                venueService.loadVenuePageData(
                        VenueService.TYPE_CONFERENCE,
                        selectedConference,
                        startYear,
                        endYear
                );

        assertNotNull(result);
        assertEquals(VenueService.TYPE_CONFERENCE, result.venueType());
        assertEquals(conferenceId.intValue(), result.venueId());
        assertNull(result.profile());
        assertNull(result.ranking());
        assertTrue(result.yearlyStats().isEmpty());
        assertFalse(result.hasAnyData());
        assertFalse(result.hasArticleData());
        assertEquals(0L, result.expectedArticleCount());

        verify(conferenceService).getConferenceProfile(conferenceId, startYear, endYear);
        verify(conferenceService).getConferenceRanking(conferenceId);
        verify(conferenceService).getConferenceYearlyStats(conferenceId, startYear, endYear);
        verifyNoInteractions(journalService);
        verifyNoMoreInteractions(conferenceService);
    }

    @Test
    void loadVenueArticlesInBatches_loadsJournalArticlesAndUpdatesLastArticleId() {
        int venueId = 3;
        Integer startYear = integer(2010);
        Integer endYear = integer(2020);
        Integer batchSize = integer(2);

        JournalArticleDto article101 = journalArticle(integer(101));
        JournalArticleDto article102 = journalArticle(integer(102));
        JournalArticleDto article205 = journalArticle(integer(205));

        List<JournalArticleDto> firstBatch =
                List.of(article101, article102);

        List<JournalArticleDto> secondBatch =
                List.of(article205);

        when(journalService.getJournalArticlesBatch(venueId, startYear, endYear, integer(0), batchSize))
                .thenReturn(firstBatch);

        when(journalService.getJournalArticlesBatch(venueId, startYear, endYear, integer(102), batchSize))
                .thenReturn(secondBatch);

        when(journalService.getJournalArticlesBatch(venueId, startYear, endYear, integer(205), batchSize))
                .thenReturn(List.of());

        List<List<Object>> loadedBatches = new ArrayList<>();

        venueService.loadVenueArticlesInBatches(
                VenueService.TYPE_JOURNAL,
                venueId,
                startYear,
                endYear,
                null,
                batchSize,
                loadedBatches::add,
                () -> true
        );

        assertEquals(2, loadedBatches.size());
        assertEquals(ids(101, 102), articleIdsFromObjects(loadedBatches.get(0)));
        assertEquals(ids(205), articleIdsFromObjects(loadedBatches.get(1)));

        verify(journalService).getJournalArticlesBatch(venueId, startYear, endYear, integer(0), batchSize);
        verify(journalService).getJournalArticlesBatch(venueId, startYear, endYear, integer(102), batchSize);
        verify(journalService).getJournalArticlesBatch(venueId, startYear, endYear, integer(205), batchSize);
        verifyNoInteractions(conferenceService);
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void loadVenueArticlesInBatches_loadsConferenceArticlesAndUpdatesLastArticleId() {
        int venueId = 4;
        Integer startYear = integer(2015);
        Integer endYear = integer(2022);
        Integer batchSize = integer(2);

        ConferenceArticleDto article11 = conferenceArticle(integer(11));
        ConferenceArticleDto article12 = conferenceArticle(integer(12));

        List<ConferenceArticleDto> firstBatch =
                List.of(article11, article12);

        when(conferenceService.getConferenceArticlesBatch(venueId, startYear, endYear, integer(0), batchSize))
                .thenReturn(firstBatch);

        when(conferenceService.getConferenceArticlesBatch(venueId, startYear, endYear, integer(12), batchSize))
                .thenReturn(List.of());

        List<List<Object>> loadedBatches = new ArrayList<>();

        venueService.loadVenueArticlesInBatches(
                VenueService.TYPE_CONFERENCE,
                venueId,
                startYear,
                endYear,
                null,
                batchSize,
                loadedBatches::add,
                () -> true
        );

        assertEquals(1, loadedBatches.size());
        assertEquals(ids(11, 12), articleIdsFromObjects(loadedBatches.get(0)));

        verify(conferenceService).getConferenceArticlesBatch(venueId, startYear, endYear, integer(0), batchSize);
        verify(conferenceService).getConferenceArticlesBatch(venueId, startYear, endYear, integer(12), batchSize);
        verifyNoInteractions(journalService);
        verifyNoMoreInteractions(conferenceService);
    }

    @Test
    void loadVenueArticlesInBatches_stopsWhenShouldContinueReturnsFalse() {
        int venueId = 3;
        Integer startYear = integer(2010);
        Integer endYear = integer(2020);
        Integer batchSize = integer(2);

        List<JournalArticleDto> firstBatch =
                List.of(
                        journalArticle(integer(101)),
                        journalArticle(integer(102))
                );

        when(journalService.getJournalArticlesBatch(venueId, startYear, endYear, integer(0), batchSize))
                .thenReturn(firstBatch);

        AtomicInteger checks = new AtomicInteger(0);
        List<List<Object>> loadedBatches = new ArrayList<>();

        venueService.loadVenueArticlesInBatches(
                VenueService.TYPE_JOURNAL,
                venueId,
                startYear,
                endYear,
                null,
                batchSize,
                loadedBatches::add,
                () -> checks.getAndIncrement() < 1
        );

        assertEquals(1, loadedBatches.size());
        assertEquals(ids(101, 102), articleIdsFromObjects(loadedBatches.get(0)));

        verify(journalService).getJournalArticlesBatch(venueId, startYear, endYear, integer(0), batchSize);
        verifyNoInteractions(conferenceService);
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void loadVenueArticlesInBatches_stopsWhenLastArticleIdIsNull() {
        int venueId = 3;
        Integer startYear = integer(2010);
        Integer endYear = integer(2020);
        Integer batchSize = integer(2);

        List<JournalArticleDto> firstBatch =
                List.of(
                        journalArticle(integer(101)),
                        journalArticle(null)
                );

        when(journalService.getJournalArticlesBatch(venueId, startYear, endYear, integer(0), batchSize))
                .thenReturn(firstBatch);

        List<List<Object>> loadedBatches = new ArrayList<>();

        venueService.loadVenueArticlesInBatches(
                VenueService.TYPE_JOURNAL,
                venueId,
                startYear,
                endYear,
                null,
                batchSize,
                loadedBatches::add,
                () -> true
        );

        assertEquals(1, loadedBatches.size());
        assertEquals(idsWithNull(integer(101), null), articleIdsFromObjects(loadedBatches.get(0)));

        verify(journalService).getJournalArticlesBatch(venueId, startYear, endYear, integer(0), batchSize);
        verifyNoInteractions(conferenceService);
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void getVenueId_returnsJournalId_whenSelectedVenueIsValidJournal() {
        JournalSearchResultDto journal =
                journalSearchResult(integer(15), "Data Journal", "Publisher A");

        int result =
                venueService.getVenueId(VenueService.TYPE_JOURNAL, journal);

        assertEquals(15, result);
    }

    @Test
    void getVenueId_returnsConferenceId_whenSelectedVenueIsValidConference() {
        ConferenceSearchResultDto conference =
                conferenceSearchResult(integer(20), "VLDB", "Very Large Data Bases");

        int result =
                venueService.getVenueId(VenueService.TYPE_CONFERENCE, conference);

        assertEquals(20, result);
    }

    @Test
    void getVenueId_throwsException_whenJournalIdIsMissingOrInvalid() {
        JournalSearchResultDto missingIdJournal =
                journalSearchResult(null, "Data Journal", "Publisher A");

        JournalSearchResultDto zeroIdJournal =
                journalSearchResult(integer(0), "Data Journal", "Publisher A");

        assertThrows(
                IllegalArgumentException.class,
                () -> venueService.getVenueId(VenueService.TYPE_JOURNAL, missingIdJournal)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> venueService.getVenueId(VenueService.TYPE_JOURNAL, zeroIdJournal)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> venueService.getVenueId(VenueService.TYPE_JOURNAL, "not a journal")
        );
    }

    @Test
    void getVenueId_throwsException_whenConferenceIdIsMissingOrInvalid() {
        ConferenceSearchResultDto missingIdConference =
                conferenceSearchResult(null, "VLDB", "Very Large Data Bases");

        ConferenceSearchResultDto zeroIdConference =
                conferenceSearchResult(integer(0), "VLDB", "Very Large Data Bases");

        assertThrows(
                IllegalArgumentException.class,
                () -> venueService.getVenueId(VenueService.TYPE_CONFERENCE, missingIdConference)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> venueService.getVenueId(VenueService.TYPE_CONFERENCE, zeroIdConference)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> venueService.getVenueId(VenueService.TYPE_CONFERENCE, "not a conference")
        );
    }

    @Test
    void getVenueDisplayName_returnsExpectedJournalDisplayNames() {
        JournalSearchResultDto journalWithPublisher =
                journalSearchResult(integer(1), "Data Journal", "Publisher A");

        JournalSearchResultDto journalWithoutPublisher =
                journalSearchResult(integer(2), "Data Journal", "");

        JournalSearchResultDto journalWithoutName =
                journalSearchResult(integer(3), "", "Publisher B");

        assertEquals(
                "Data Journal (Publisher A)",
                venueService.getVenueDisplayName(journalWithPublisher)
        );

        assertEquals(
                "Data Journal",
                venueService.getVenueDisplayName(journalWithoutPublisher)
        );

        assertEquals(
                "Journal #3",
                venueService.getVenueDisplayName(journalWithoutName)
        );
    }

    @Test
    void getVenueDisplayName_returnsExpectedConferenceDisplayNames() {
        ConferenceSearchResultDto conferenceWithAcronymAndTitle =
                conferenceSearchResult(integer(1), "VLDB", "Very Large Data Bases");

        ConferenceSearchResultDto conferenceWithOnlyAcronym =
                conferenceSearchResult(integer(2), "SIGMOD", "");

        ConferenceSearchResultDto conferenceWithOnlyTitle =
                conferenceSearchResult(integer(3), "", "International Database Conference");

        ConferenceSearchResultDto conferenceWithoutName =
                conferenceSearchResult(integer(4), "", "");

        assertEquals(
                "VLDB - Very Large Data Bases",
                venueService.getVenueDisplayName(conferenceWithAcronymAndTitle)
        );

        assertEquals(
                "SIGMOD",
                venueService.getVenueDisplayName(conferenceWithOnlyAcronym)
        );

        assertEquals(
                "International Database Conference",
                venueService.getVenueDisplayName(conferenceWithOnlyTitle)
        );

        assertEquals(
                "Conference #4",
                venueService.getVenueDisplayName(conferenceWithoutName)
        );
    }

    @Test
    void getVenueDisplayName_returnsUnknownVenue_whenObjectTypeIsNotSupported() {
        assertEquals(
                "Unknown venue",
                venueService.getVenueDisplayName("plain string")
        );
    }

    @Test
    void getExpectedArticleCount_returnsExpectedCounts() {
        JournalProfileDto journalProfile = journalProfile(longValue(25));
        ConferenceProfileDto conferenceProfile = conferenceProfile(longValue(40));
        JournalProfileDto journalProfileWithNullArticles = journalProfile(null);

        assertEquals(25L, venueService.getExpectedArticleCount(journalProfile));
        assertEquals(40L, venueService.getExpectedArticleCount(conferenceProfile));
        assertEquals(0L, venueService.getExpectedArticleCount(journalProfileWithNullArticles));
        assertEquals(0L, venueService.getExpectedArticleCount("unknown profile"));
        assertEquals(0L, venueService.getExpectedArticleCount(null));
    }

    @Test
    void venuePageDataHelperMethodsReturnExpectedValues() {
        JournalProfileDto profileWithArticles = journalProfile(longValue(10));
        JournalProfileDto profileWithoutArticles = journalProfile(longValue(0));

        VenueService.VenuePageData withData =
                new VenueService.VenuePageData(
                        VenueService.TYPE_JOURNAL,
                        1,
                        profileWithArticles,
                        null,
                        List.of()
                );

        VenueService.VenuePageData withoutArticleData =
                new VenueService.VenuePageData(
                        VenueService.TYPE_JOURNAL,
                        1,
                        profileWithoutArticles,
                        null,
                        List.of()
                );

        VenueService.VenuePageData emptyData =
                new VenueService.VenuePageData(
                        VenueService.TYPE_JOURNAL,
                        1,
                        null,
                        null,
                        List.of()
                );

        assertTrue(withData.hasAnyData());
        assertTrue(withData.hasArticleData());
        assertEquals(10L, withData.expectedArticleCount());

        assertTrue(withoutArticleData.hasAnyData());
        assertFalse(withoutArticleData.hasArticleData());
        assertEquals(0L, withoutArticleData.expectedArticleCount());

        assertFalse(emptyData.hasAnyData());
        assertFalse(emptyData.hasArticleData());
        assertEquals(0L, emptyData.expectedArticleCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "Invalid",
            "journal",
            "conference"
    })
    void serviceMethodsThrowException_whenVenueTypeIsInvalid(String invalidVenueType) {
        assertThrows(
                IllegalArgumentException.class,
                () -> venueService.loadVenuePageData(
                        invalidVenueType,
                        "venue",
                        integer(2010),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> venueService.loadVenueArticlesInBatches(
                        invalidVenueType,
                        1,
                        integer(2010),
                        integer(2020),
                        integer(0),
                        integer(100),
                        batch -> {
                        },
                        () -> true
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> venueService.getVenueId(invalidVenueType, "venue")
        );

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -50})
    void loadVenueArticlesInBatchesThrowsException_whenVenueIdIsNotPositive(int invalidVenueId) {
        assertThrows(
                IllegalArgumentException.class,
                () -> venueService.loadVenueArticlesInBatches(
                        VenueService.TYPE_JOURNAL,
                        invalidVenueId,
                        integer(2010),
                        integer(2020),
                        integer(0),
                        integer(100),
                        batch -> {
                        },
                        () -> true
                )
        );

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void loadVenueArticlesInBatchesThrowsException_whenCallbackIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> venueService.loadVenueArticlesInBatches(
                        VenueService.TYPE_JOURNAL,
                        1,
                        integer(2010),
                        integer(2020),
                        integer(0),
                        integer(100),
                        null,
                        () -> true
                )
        );

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void loadVenueArticlesInBatchesThrowsException_whenShouldContinueIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> venueService.loadVenueArticlesInBatches(
                        VenueService.TYPE_JOURNAL,
                        1,
                        integer(2010),
                        integer(2020),
                        integer(0),
                        integer(100),
                        batch -> {
                        },
                        null
                )
        );

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void loadVenueArticlesInBatchesThrowsException_whenLastArticleIdIsNegative() {
        assertThrows(
                IllegalArgumentException.class,
                () -> venueService.loadVenueArticlesInBatches(
                        VenueService.TYPE_JOURNAL,
                        1,
                        integer(2010),
                        integer(2020),
                        integer(-1),
                        integer(100),
                        batch -> {
                        },
                        () -> true
                )
        );

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    private JournalSearchResultDto journalSearchResult(
            Integer journalId,
            String journalName,
            String publisherName
    ) {
        JournalSearchResultDto dto = mock(JournalSearchResultDto.class);

        lenient().when(dto.journalId()).thenReturn(journalId);
        lenient().when(dto.journalName()).thenReturn(journalName);
        lenient().when(dto.publisherName()).thenReturn(publisherName);

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

    private JournalProfileDto journalProfile(Long totalArticles) {
        JournalProfileDto dto = mock(JournalProfileDto.class);

        lenient().when(dto.totalArticles()).thenReturn(totalArticles);

        return dto;
    }

    private ConferenceProfileDto conferenceProfile(Long totalArticles) {
        ConferenceProfileDto dto = mock(ConferenceProfileDto.class);

        lenient().when(dto.totalArticles()).thenReturn(totalArticles);

        return dto;
    }

    private JournalArticleDto journalArticle(Integer articleId) {
        JournalArticleDto dto = mock(JournalArticleDto.class);

        lenient().when(dto.articleId()).thenReturn(articleId);

        return dto;
    }

    private ConferenceArticleDto conferenceArticle(Integer articleId) {
        ConferenceArticleDto dto = mock(ConferenceArticleDto.class);

        lenient().when(dto.articleId()).thenReturn(articleId);

        return dto;
    }

    private List<Integer> articleIdsFromObjects(List<Object> articles) {
        List<Integer> result = new ArrayList<>();

        for (Object article : articles) {
            if (article instanceof JournalArticleDto journalArticle) {
                result.add(journalArticle.articleId());
            } else if (article instanceof ConferenceArticleDto conferenceArticle) {
                result.add(conferenceArticle.articleId());
            }
        }

        return result;
    }

    private Integer integer(int value) {
        return Integer.valueOf(value);
    }

    private Long longValue(long value) {
        return Long.valueOf(value);
    }

    private List<Integer> ids(int... values) {
        List<Integer> result = new ArrayList<>();

        for (int value : values) {
            result.add(Integer.valueOf(value));
        }

        return result;
    }

    private List<Integer> idsWithNull(Integer firstValue, Integer secondValue) {
        List<Integer> result = new ArrayList<>();
        result.add(firstValue);
        result.add(secondValue);
        return result;
    }
}