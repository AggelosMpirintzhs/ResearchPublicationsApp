package service.charts;

import dto.chart.PublisherOptionDto;
import dto.chart.PublisherQuartileStatsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.JournalService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublisherAnalysisServiceTest {

    @Mock
    private JournalService journalService;

    private PublisherAnalysisService publisherAnalysisService;

    @BeforeEach
    void setUp() throws Exception {
        publisherAnalysisService = new PublisherAnalysisService();
        injectJournalServiceMock();
    }

    @Test
    void getQuartiles_returnsExpectedQuartiles() {
        List<String> result = publisherAnalysisService.getQuartiles();

        assertEquals(List.of("Q1", "Q2", "Q3", "Q4"), result);
    }

    @Test
    void searchPublishers_returnsEmptyListAndDoesNotCallJournalService_whenSearchTextIsNull() {
        List<PublisherOptionDto> result =
                publisherAnalysisService.searchPublishers(null, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(journalService);
    }

    @Test
    void searchPublishers_returnsEmptyListAndDoesNotCallJournalService_whenSearchTextIsBlank() {
        List<PublisherOptionDto> result =
                publisherAnalysisService.searchPublishers("   ", 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(journalService);
    }

    @Test
    void searchPublishers_callsJournalServiceAndSortsResultsByRelevance() {
        PublisherOptionDto exactMatch =
                publisherOption(1, "Elsevier", 100L);

        PublisherOptionDto startsWithMatch =
                publisherOption(2, "Elsevier Science", 80L);

        PublisherOptionDto containsMatch =
                publisherOption(3, "Big Elsevier Publisher", 60L);

        PublisherOptionDto noMatch =
                publisherOption(4, "Springer", 40L);

        when(journalService.getPublisherOptions("elsevier", 10))
                .thenReturn(List.of(
                        noMatch,
                        containsMatch,
                        startsWithMatch,
                        exactMatch
                ));

        List<PublisherOptionDto> result =
                publisherAnalysisService.searchPublishers("elsevier", 10);

        assertEquals(4, result.size());
        assertSame(exactMatch, result.get(0));
        assertSame(startsWithMatch, result.get(1));
        assertSame(containsMatch, result.get(2));
        assertSame(noMatch, result.get(3));

        verify(journalService).getPublisherOptions("elsevier", 10);
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void searchPublishers_passesOriginalSearchTextAndLimitToJournalService() {
        String searchText = "  Elsevier  ";

        PublisherOptionDto publisher =
                publisherOption(1, "Elsevier", 100L);

        when(journalService.getPublisherOptions(searchText, 5))
                .thenReturn(List.of(publisher));

        List<PublisherOptionDto> result =
                publisherAnalysisService.searchPublishers(searchText, 5);

        assertEquals(1, result.size());
        assertSame(publisher, result.get(0));

        verify(journalService).getPublisherOptions(searchText, 5);
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void searchPublishers_sortsSameRelevanceByNameLengthAndAlphabetically() {
        PublisherOptionDto longerName =
                publisherOption(1, "Data Science Publisher", 100L);

        PublisherOptionDto shorterName =
                publisherOption(2, "Data Publisher", 80L);

        PublisherOptionDto alphabeticallySecond =
                publisherOption(3, "Data Zebra", 60L);

        PublisherOptionDto alphabeticallyFirst =
                publisherOption(4, "Data Alpha", 40L);

        when(journalService.getPublisherOptions("data", 10))
                .thenReturn(List.of(
                        longerName,
                        alphabeticallySecond,
                        shorterName,
                        alphabeticallyFirst
                ));

        List<PublisherOptionDto> result =
                publisherAnalysisService.searchPublishers("data", 10);

        assertEquals(4, result.size());
        assertSame(alphabeticallyFirst, result.get(0));
        assertSame(alphabeticallySecond, result.get(1));
        assertSame(shorterName, result.get(2));
        assertSame(longerName, result.get(3));

        verify(journalService).getPublisherOptions("data", 10);
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void alreadySelected_returnsFalse_whenPublisherIsNull() {
        List<PublisherOptionDto> selectedPublishers =
                List.of(publisherOption(1, "Elsevier", 100L));

        boolean result =
                publisherAnalysisService.alreadySelected(selectedPublishers, null);

        assertFalse(result);
    }

    @Test
    void alreadySelected_returnsFalse_whenSelectedPublishersIsNullOrEmpty() {
        PublisherOptionDto publisher =
                publisherOption(1, "Elsevier", 100L);

        assertFalse(publisherAnalysisService.alreadySelected(null, publisher));
        assertFalse(publisherAnalysisService.alreadySelected(List.of(), publisher));
    }

    @Test
    void alreadySelected_returnsTrue_whenPublisherIdAlreadyExists() {
        PublisherOptionDto publisher1 =
                publisherOption(1, "Elsevier", 100L);

        PublisherOptionDto publisher2 =
                publisherOption(2, "Springer", 80L);

        PublisherOptionDto sameAsPublisher1 =
                publisherOption(1, "Elsevier Updated", 120L);

        boolean result =
                publisherAnalysisService.alreadySelected(
                        List.of(publisher1, publisher2),
                        sameAsPublisher1
                );

        assertTrue(result);
    }

    @Test
    void alreadySelected_returnsFalse_whenPublisherIdDoesNotExist() {
        PublisherOptionDto publisher1 =
                publisherOption(1, "Elsevier", 100L);

        PublisherOptionDto publisher2 =
                publisherOption(2, "Springer", 80L);

        PublisherOptionDto newPublisher =
                publisherOption(3, "IEEE", 60L);

        boolean result =
                publisherAnalysisService.alreadySelected(
                        List.of(publisher1, publisher2),
                        newPublisher
                );

        assertFalse(result);
    }

    @Test
    void loadPublisherSummaries_returnsEmptyListAndDoesNotCallJournalService_whenSelectedPublishersIsNull() {
        List<PublisherAnalysisService.PublisherSummary> result =
                publisherAnalysisService.loadPublisherSummaries(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(journalService);
    }

    @Test
    void loadPublisherSummaries_returnsEmptyListAndDoesNotCallJournalService_whenSelectedPublishersIsEmpty() {
        List<PublisherAnalysisService.PublisherSummary> result =
                publisherAnalysisService.loadPublisherSummaries(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(journalService);
    }

    @Test
    void loadPublisherSummaries_usesSelectedPublisherIdsAndConvertsStatsToSummaries() {
        PublisherOptionDto elsevier =
                publisherOption(1, "Elsevier", 100L);

        PublisherOptionDto springer =
                publisherOption(2, "Springer", 80L);

        PublisherQuartileStatsDto elsevierQ1 =
                publisherStats(1, "Elsevier Ltd", "Q1", 10L, 105L);

        PublisherQuartileStatsDto elsevierQ2 =
                publisherStats(1, "Elsevier Ltd", "q2", 20L, 105L);

        PublisherQuartileStatsDto springerQ3 =
                publisherStats(2, "Springer", "Best Q3", 7L, 80L);

        PublisherQuartileStatsDto springerQ4 =
                publisherStats(2, "Springer", "Q4", 3L, 80L);

        when(journalService.getPublisherQuartilePublicationStatsForPublishers(List.of(1, 2)))
                .thenReturn(List.of(
                        elsevierQ1,
                        elsevierQ2,
                        springerQ3,
                        springerQ4
                ));

        List<PublisherAnalysisService.PublisherSummary> result =
                publisherAnalysisService.loadPublisherSummaries(
                        List.of(elsevier, springer)
                );

        assertEquals(2, result.size());

        PublisherAnalysisService.PublisherSummary elsevierSummary = result.get(0);
        assertEquals(1, elsevierSummary.publisherId());
        assertEquals("Elsevier Ltd", elsevierSummary.publisherName());
        assertEquals(10L, elsevierSummary.q1());
        assertEquals(20L, elsevierSummary.q2());
        assertEquals(0L, elsevierSummary.q3());
        assertEquals(0L, elsevierSummary.q4());
        assertEquals(105L, elsevierSummary.totalPublications());

        PublisherAnalysisService.PublisherSummary springerSummary = result.get(1);
        assertEquals(2, springerSummary.publisherId());
        assertEquals("Springer", springerSummary.publisherName());
        assertEquals(0L, springerSummary.q1());
        assertEquals(0L, springerSummary.q2());
        assertEquals(7L, springerSummary.q3());
        assertEquals(3L, springerSummary.q4());
        assertEquals(80L, springerSummary.totalPublications());

        verify(journalService).getPublisherQuartilePublicationStatsForPublishers(List.of(1, 2));
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void loadPublisherSummaries_ignoresNullSelectedPublishersWhenCollectingIds() {
        PublisherOptionDto elsevier =
                publisherOption(1, "Elsevier", 100L);

        PublisherOptionDto springer =
                publisherOption(2, "Springer", 80L);

        when(journalService.getPublisherQuartilePublicationStatsForPublishers(List.of(1, 2)))
                .thenReturn(List.of());

        List<PublisherOptionDto> selectedPublishers = new ArrayList<>();
        selectedPublishers.add(elsevier);
        selectedPublishers.add(null);
        selectedPublishers.add(springer);

        List<PublisherAnalysisService.PublisherSummary> result =
                publisherAnalysisService.loadPublisherSummaries(selectedPublishers);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).publisherId());
        assertEquals(2, result.get(1).publisherId());

        verify(journalService).getPublisherQuartilePublicationStatsForPublishers(List.of(1, 2));
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void loadPublisherSummaries_returnsSelectedPublishersWithZeroQuartiles_whenStatsAreEmpty() {
        PublisherOptionDto elsevier =
                publisherOption(1, "Elsevier", 100L);

        when(journalService.getPublisherQuartilePublicationStatsForPublishers(List.of(1)))
                .thenReturn(List.of());

        List<PublisherAnalysisService.PublisherSummary> result =
                publisherAnalysisService.loadPublisherSummaries(List.of(elsevier));

        assertEquals(1, result.size());

        PublisherAnalysisService.PublisherSummary summary = result.get(0);

        assertEquals(1, summary.publisherId());
        assertEquals("Elsevier", summary.publisherName());
        assertEquals(0L, summary.q1());
        assertEquals(0L, summary.q2());
        assertEquals(0L, summary.q3());
        assertEquals(0L, summary.q4());
        assertEquals(100L, summary.totalPublications());

        verify(journalService).getPublisherQuartilePublicationStatsForPublishers(List.of(1));
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void loadPublisherSummaries_ignoresNullStatsRows() {
        PublisherOptionDto elsevier =
                publisherOption(1, "Elsevier", 100L);

        PublisherQuartileStatsDto q1 =
                publisherStats(1, "Elsevier", "Q1", 10L, 100L);

        List<PublisherQuartileStatsDto> stats = new ArrayList<>();
        stats.add(q1);
        stats.add(null);

        when(journalService.getPublisherQuartilePublicationStatsForPublishers(List.of(1)))
                .thenReturn(stats);

        List<PublisherAnalysisService.PublisherSummary> result =
                publisherAnalysisService.loadPublisherSummaries(List.of(elsevier));

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).q1());

        verify(journalService).getPublisherQuartilePublicationStatsForPublishers(List.of(1));
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void buildPublisherCategory_returnsDash_whenSummaryIsNull() {
        String result =
                publisherAnalysisService.buildPublisherCategory(null);

        assertEquals("-", result);
    }

    @Test
    void buildPublisherCategory_returnsShortNameWithPublisherId() {
        PublisherAnalysisService.PublisherSummary summary =
                publisherSummary(1, "Elsevier", 10L, 20L, 30L, 40L, 100L);

        String result =
                publisherAnalysisService.buildPublisherCategory(summary);

        assertEquals("Elsevier #1", result);
    }

    @Test
    void buildPublisherCategory_shortensLongPublisherName() {
        PublisherAnalysisService.PublisherSummary summary =
                publisherSummary(
                        5,
                        "Very Long Publisher Name Example",
                        0L,
                        0L,
                        0L,
                        0L,
                        100L
                );

        String result =
                publisherAnalysisService.buildPublisherCategory(summary);

        assertEquals("Very Long Publisher N... #5", result);
    }

    @Test
    void buildTotalPublisherKey_returnsEmptyString_whenSummaryIsNull() {
        String result =
                publisherAnalysisService.buildTotalPublisherKey(null);

        assertEquals("", result);
    }

    @Test
    void buildTotalPublisherKey_returnsExpectedKey() {
        PublisherAnalysisService.PublisherSummary summary =
                publisherSummary(7, "IEEE", 0L, 0L, 0L, 0L, 50L);

        String result =
                publisherAnalysisService.buildTotalPublisherKey(summary);

        assertEquals("publisher#7", result);
    }

    @Test
    void formatPublisherForUi_returnsEmptyString_whenPublisherIsNull() {
        String result =
                publisherAnalysisService.formatPublisherForUi(null);

        assertEquals("", result);
    }

    @Test
    void formatPublisherForUi_returnsExpectedText() {
        PublisherOptionDto publisher =
                publisherOption(1, "Elsevier", 100L);

        String result =
                publisherAnalysisService.formatPublisherForUi(publisher);

        assertEquals("Elsevier (100 publications)", result);
    }

    @Test
    void shortenName_returnsDash_whenNameIsNullOrBlank() {
        assertEquals("-", publisherAnalysisService.shortenName(null, 10));
        assertEquals("-", publisherAnalysisService.shortenName("   ", 10));
    }

    @Test
    void shortenName_returnsOriginalName_whenNameFitsMaxLength() {
        String result =
                publisherAnalysisService.shortenName("Elsevier", 10);

        assertEquals("Elsevier", result);
    }

    @Test
    void shortenName_shortensLongNameWithEllipsis() {
        String result =
                publisherAnalysisService.shortenName("Very Long Publisher", 12);

        assertEquals("Very Long...", result);
    }

    @Test
    void publisherSummaryCountForQuartile_returnsExpectedCounts() {
        PublisherAnalysisService.PublisherSummary summary =
                publisherSummary(1, "Elsevier", 10L, 20L, 30L, 40L, 100L);

        assertEquals(10L, summary.countForQuartile(PublisherAnalysisService.QUARTILE_Q1));
        assertEquals(20L, summary.countForQuartile(PublisherAnalysisService.QUARTILE_Q2));
        assertEquals(30L, summary.countForQuartile(PublisherAnalysisService.QUARTILE_Q3));
        assertEquals(40L, summary.countForQuartile(PublisherAnalysisService.QUARTILE_Q4));
        assertEquals(0L, summary.countForQuartile("Q5"));
        assertEquals(0L, summary.countForQuartile(null));
    }

    private void injectJournalServiceMock() throws Exception {
        Field field = PublisherAnalysisService.class.getDeclaredField("journalService");
        field.setAccessible(true);
        field.set(publisherAnalysisService, journalService);
    }

    private PublisherOptionDto publisherOption(
            int publisherId,
            String publisherName,
            long totalPublications
    ) {
        PublisherOptionDto dto = mock(PublisherOptionDto.class);

        lenient().when(dto.publisherId()).thenReturn(publisherId);
        lenient().when(dto.publisherName()).thenReturn(publisherName);
        lenient().when(dto.totalPublications()).thenReturn(totalPublications);

        return dto;
    }

    private PublisherQuartileStatsDto publisherStats(
            int publisherId,
            String publisherName,
            String quartile,
            long publicationCount,
            long totalPublications
    ) {
        PublisherQuartileStatsDto dto = mock(PublisherQuartileStatsDto.class);

        lenient().when(dto.publisherId()).thenReturn(publisherId);
        lenient().when(dto.publisherName()).thenReturn(publisherName);
        lenient().when(dto.quartile()).thenReturn(quartile);
        lenient().when(dto.publicationCount()).thenReturn(publicationCount);
        lenient().when(dto.totalPublications()).thenReturn(totalPublications);

        return dto;
    }

    private PublisherAnalysisService.PublisherSummary publisherSummary(
            int publisherId,
            String publisherName,
            long q1,
            long q2,
            long q3,
            long q4,
            long totalPublications
    ) {
        return new PublisherAnalysisService.PublisherSummary(
                publisherId,
                publisherName,
                q1,
                q2,
                q3,
                q4,
                totalPublications
        );
    }
}