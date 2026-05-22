package service;

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
import repository.JournalRepository;
import dto.chart.CategoryOptionDto;
import dto.chart.CategoryTrendDto;
import dto.chart.PublisherOptionDto;
import dto.chart.PublisherQuartileStatsDto;
import dto.chart.ScatterPlotPointDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    @Mock
    private JournalRepository journalRepository;

    private JournalService journalService;

    @BeforeEach
    void setUp() {
        journalService = new JournalService(journalRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "     "
    })
    void searchJournals_returnsEmptyListAndDoesNotCallRepository_whenSearchTextIsBlank(String searchText) {
        List<JournalSearchResultDto> result =
                journalService.searchJournals(searchText, integer(10));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(journalRepository);
    }

    @Test
    void searchJournals_returnsEmptyListAndDoesNotCallRepository_whenSearchTextIsNull() {
        List<JournalSearchResultDto> result =
                journalService.searchJournals(null, integer(10));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(journalRepository);
    }

    @Test
    void searchJournals_trimsSearchTextAndReturnsExpectedResults() {
        JournalSearchResultDto journal1 = mock(JournalSearchResultDto.class);
        JournalSearchResultDto journal2 = mock(JournalSearchResultDto.class);

        List<JournalSearchResultDto> expectedResults =
                List.of(journal1, journal2);

        when(journalRepository.searchJournals("VLDB Journal", 10))
                .thenReturn(expectedResults);

        List<JournalSearchResultDto> result =
                journalService.searchJournals("   VLDB Journal   ", integer(10));

        assertEquals(expectedResults, result);
        assertSame(journal1, result.get(0));
        assertSame(journal2, result.get(1));

        verify(journalRepository).searchJournals("VLDB Journal", 10);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void searchJournals_usesDefaultLimit_whenLimitIsNullOrInvalid() {
        when(journalRepository.searchJournals("Nature", 20))
                .thenReturn(List.of());

        journalService.searchJournals("Nature", null);
        verify(journalRepository).searchJournals("Nature", 20);

        clearInvocations(journalRepository);

        journalService.searchJournals("Nature", integer(0));
        verify(journalRepository).searchJournals("Nature", 20);

        clearInvocations(journalRepository);

        journalService.searchJournals("Nature", integer(-5));
        verify(journalRepository).searchJournals("Nature", 20);

        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void searchJournals_capsLimitToMaxLimit() {
        when(journalRepository.searchJournals("Science", 100))
                .thenReturn(List.of());

        List<JournalSearchResultDto> result =
                journalService.searchJournals("Science", integer(500));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(journalRepository).searchJournals("Science", 100);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalProfile_returnsExpectedProfile() {
        int journalId = 5;
        Integer startYear = integer(2010);
        Integer endYear = integer(2020);

        JournalProfileDto profile = mock(JournalProfileDto.class);

        when(journalRepository.findJournalProfile(journalId, startYear, endYear))
                .thenReturn(Optional.of(profile));

        Optional<JournalProfileDto> result =
                journalService.getJournalProfile(journalId, startYear, endYear);

        assertTrue(result.isPresent());
        assertSame(profile, result.orElseThrow());

        verify(journalRepository).findJournalProfile(journalId, startYear, endYear);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalProfile_returnsEmptyOptional_whenRepositoryDoesNotFindProfile() {
        int journalId = 5;
        Integer startYear = integer(2010);
        Integer endYear = integer(2020);

        when(journalRepository.findJournalProfile(journalId, startYear, endYear))
                .thenReturn(Optional.empty());

        Optional<JournalProfileDto> result =
                journalService.getJournalProfile(journalId, startYear, endYear);

        assertTrue(result.isEmpty());

        verify(journalRepository).findJournalProfile(journalId, startYear, endYear);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalProfile_usesDefaultYearRange_whenYearsAreNull() {
        int journalId = 5;

        JournalProfileDto profile = mock(JournalProfileDto.class);

        when(journalRepository.findJournalProfile(journalId, 0, 9999))
                .thenReturn(Optional.of(profile));

        Optional<JournalProfileDto> result =
                journalService.getJournalProfile(journalId, null, null);

        assertTrue(result.isPresent());
        assertSame(profile, result.orElseThrow());

        verify(journalRepository).findJournalProfile(journalId, 0, 9999);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalYearlyStats_returnsExpectedStats() {
        int journalId = 8;
        Integer startYear = integer(2015);
        Integer endYear = integer(2022);

        JournalYearlyStatsDto stats1 = mock(JournalYearlyStatsDto.class);
        JournalYearlyStatsDto stats2 = mock(JournalYearlyStatsDto.class);

        List<JournalYearlyStatsDto> expectedStats =
                List.of(stats1, stats2);

        when(journalRepository.findJournalYearlyStats(journalId, startYear, endYear))
                .thenReturn(expectedStats);

        List<JournalYearlyStatsDto> result =
                journalService.getJournalYearlyStats(journalId, startYear, endYear);

        assertEquals(expectedStats, result);
        assertSame(stats1, result.get(0));
        assertSame(stats2, result.get(1));

        verify(journalRepository).findJournalYearlyStats(journalId, startYear, endYear);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalRanking_returnsExpectedRanking() {
        int journalId = 11;

        JournalRankingDto ranking = mock(JournalRankingDto.class);

        when(journalRepository.findJournalRanking(journalId))
                .thenReturn(Optional.of(ranking));

        Optional<JournalRankingDto> result =
                journalService.getJournalRanking(journalId);

        assertTrue(result.isPresent());
        assertSame(ranking, result.orElseThrow());

        verify(journalRepository).findJournalRanking(journalId);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalRanking_returnsEmptyOptional_whenRepositoryDoesNotFindRanking() {
        int journalId = 11;

        when(journalRepository.findJournalRanking(journalId))
                .thenReturn(Optional.empty());

        Optional<JournalRankingDto> result =
                journalService.getJournalRanking(journalId);

        assertTrue(result.isEmpty());

        verify(journalRepository).findJournalRanking(journalId);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalArticles_returnsExpectedArticles() {
        int journalId = 9;
        Integer startYear = integer(2018);
        Integer endYear = integer(2023);

        JournalArticleDto article1 = mock(JournalArticleDto.class);
        JournalArticleDto article2 = mock(JournalArticleDto.class);

        List<JournalArticleDto> expectedArticles =
                List.of(article1, article2);

        when(journalRepository.findJournalArticles(journalId, startYear, endYear))
                .thenReturn(expectedArticles);

        List<JournalArticleDto> result =
                journalService.getJournalArticles(journalId, startYear, endYear);

        assertEquals(expectedArticles, result);
        assertSame(article1, result.get(0));
        assertSame(article2, result.get(1));

        verify(journalRepository).findJournalArticles(journalId, startYear, endYear);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalArticles_usesDefaultYearRange_whenYearsAreNull() {
        int journalId = 9;

        JournalArticleDto article = mock(JournalArticleDto.class);

        when(journalRepository.findJournalArticles(journalId, 0, 9999))
                .thenReturn(List.of(article));

        List<JournalArticleDto> result =
                journalService.getJournalArticles(journalId, null, null);

        assertEquals(1, result.size());
        assertSame(article, result.get(0));

        verify(journalRepository).findJournalArticles(journalId, 0, 9999);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalArticlesBatch_usesDefaultLastArticleIdAndDefaultBatchSize() {
        int journalId = 12;

        JournalArticleDto article1 = journalArticle(integer(101));
        JournalArticleDto article2 = journalArticle(integer(102));

        List<JournalArticleDto> expectedBatch =
                List.of(article1, article2);

        when(journalRepository.findJournalArticlesBatch(journalId, 0, 9999, 0, 1000))
                .thenReturn(expectedBatch);

        List<JournalArticleDto> result =
                journalService.getJournalArticlesBatch(
                        journalId,
                        null,
                        null,
                        null,
                        null
                );

        assertEquals(expectedBatch, result);
        assertEquals(ids(101, 102), articleIds(result));

        verify(journalRepository).findJournalArticlesBatch(journalId, 0, 9999, 0, 1000);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalArticlesBatch_usesGivenLastArticleIdAndBatchSize() {
        int journalId = 12;
        Integer startYear = integer(2012);
        Integer endYear = integer(2020);
        Integer lastArticleId = integer(55);
        Integer batchSize = integer(250);

        JournalArticleDto article = journalArticle(integer(70));

        when(journalRepository.findJournalArticlesBatch(journalId, startYear, endYear, lastArticleId, batchSize))
                .thenReturn(List.of(article));

        List<JournalArticleDto> result =
                journalService.getJournalArticlesBatch(
                        journalId,
                        startYear,
                        endYear,
                        lastArticleId,
                        batchSize
                );

        assertEquals(1, result.size());
        assertEquals(ids(70), articleIds(result));

        verify(journalRepository).findJournalArticlesBatch(journalId, startYear, endYear, lastArticleId, batchSize);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalArticlesBatch_capsBatchSizeToMaxBatchSize() {
        int journalId = 12;
        Integer startYear = integer(2012);
        Integer endYear = integer(2020);
        Integer lastArticleId = integer(55);
        Integer veryLargeBatchSize = integer(100_000);

        when(journalRepository.findJournalArticlesBatch(journalId, startYear, endYear, lastArticleId, 5000))
                .thenReturn(List.of());

        List<JournalArticleDto> result =
                journalService.getJournalArticlesBatch(
                        journalId,
                        startYear,
                        endYear,
                        lastArticleId,
                        veryLargeBatchSize
                );

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(journalRepository).findJournalArticlesBatch(journalId, startYear, endYear, lastArticleId, 5000);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalArticlesBatch_usesDefaultBatchSize_whenBatchSizeIsNullOrInvalid() {
        int journalId = 12;
        Integer startYear = integer(2012);
        Integer endYear = integer(2020);
        Integer lastArticleId = integer(55);

        when(journalRepository.findJournalArticlesBatch(journalId, startYear, endYear, lastArticleId, 1000))
                .thenReturn(List.of());

        journalService.getJournalArticlesBatch(
                journalId,
                startYear,
                endYear,
                lastArticleId,
                null
        );

        verify(journalRepository).findJournalArticlesBatch(journalId, startYear, endYear, lastArticleId, 1000);

        clearInvocations(journalRepository);

        journalService.getJournalArticlesBatch(
                journalId,
                startYear,
                endYear,
                lastArticleId,
                integer(0)
        );

        verify(journalRepository).findJournalArticlesBatch(journalId, startYear, endYear, lastArticleId, 1000);

        clearInvocations(journalRepository);

        journalService.getJournalArticlesBatch(
                journalId,
                startYear,
                endYear,
                lastArticleId,
                integer(-10)
        );

        verify(journalRepository).findJournalArticlesBatch(journalId, startYear, endYear, lastArticleId, 1000);

        verifyNoMoreInteractions(journalRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -50})
    void serviceMethodsThrowException_whenJournalIdIsNotPositive(int invalidJournalId) {
        assertThrows(
                IllegalArgumentException.class,
                () -> journalService.getJournalProfile(
                        invalidJournalId,
                        integer(2010),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> journalService.getJournalYearlyStats(
                        invalidJournalId,
                        integer(2010),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> journalService.getJournalRanking(invalidJournalId)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> journalService.getJournalArticles(
                        invalidJournalId,
                        integer(2010),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> journalService.getJournalArticlesBatch(
                        invalidJournalId,
                        integer(2010),
                        integer(2020),
                        integer(0),
                        integer(100)
                )
        );

        verifyNoInteractions(journalRepository);
    }

    @Test
    void serviceMethodsThrowException_whenStartYearIsGreaterThanEndYear() {
        assertThrows(
                IllegalArgumentException.class,
                () -> journalService.getJournalProfile(
                        1,
                        integer(2025),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> journalService.getJournalYearlyStats(
                        1,
                        integer(2025),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> journalService.getJournalArticles(
                        1,
                        integer(2025),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> journalService.getJournalArticlesBatch(
                        1,
                        integer(2025),
                        integer(2020),
                        integer(0),
                        integer(100)
                )
        );

        verifyNoInteractions(journalRepository);
    }

    @Test
    void getJournalArticlesBatchThrowsException_whenLastArticleIdIsNegative() {
        assertThrows(
                IllegalArgumentException.class,
                () -> journalService.getJournalArticlesBatch(
                        1,
                        integer(2010),
                        integer(2020),
                        integer(-1),
                        integer(100)
                )
        );

        verifyNoInteractions(journalRepository);
    }

    private JournalArticleDto journalArticle(Integer articleId) {
        JournalArticleDto dto = mock(JournalArticleDto.class);
        when(dto.articleId()).thenReturn(articleId);
        return dto;
    }

    private List<Integer> articleIds(List<JournalArticleDto> articles) {
        return articles.stream()
                .map(JournalArticleDto::articleId)
                .toList();
    }

    private Integer integer(int value) {
        return Integer.valueOf(value);
    }

    private List<Integer> ids(int... values) {
        List<Integer> result = new ArrayList<>();

        for (int value : values) {
            result.add(Integer.valueOf(value));
        }

        return result;
    }

    @Test
    void getBestSubjectAreas_returnsExpectedCategories() {
        CategoryOptionDto category1 = mock(CategoryOptionDto.class);
        CategoryOptionDto category2 = mock(CategoryOptionDto.class);

        List<CategoryOptionDto> expectedCategories = List.of(category1, category2);

        when(journalRepository.findBestSubjectAreas())
                .thenReturn(expectedCategories);

        List<CategoryOptionDto> result =
                journalService.getBestSubjectAreas();

        assertEquals(expectedCategories, result);
        assertSame(category1, result.get(0));
        assertSame(category2, result.get(1));

        verify(journalRepository).findBestSubjectAreas();
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalBestSubjectAreaYearlyTrends_trimsCategoryFilterAndReturnsExpectedTrends() {
        CategoryTrendDto trend1 = mock(CategoryTrendDto.class);
        CategoryTrendDto trend2 = mock(CategoryTrendDto.class);

        List<CategoryTrendDto> expectedTrends = List.of(trend1, trend2);

        when(journalRepository.findJournalBestSubjectAreaYearlyTrends("Computer Science", 2010, 2020))
                .thenReturn(expectedTrends);

        List<CategoryTrendDto> result =
                journalService.getJournalBestSubjectAreaYearlyTrends(
                        "   Computer Science   ",
                        integer(2010),
                        integer(2020)
                );

        assertEquals(expectedTrends, result);
        assertSame(trend1, result.get(0));
        assertSame(trend2, result.get(1));

        verify(journalRepository).findJournalBestSubjectAreaYearlyTrends("Computer Science", 2010, 2020);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalBestSubjectAreaYearlyTrends_usesEmptyCategoryAndDefaultYears_whenValuesAreNull() {
        CategoryTrendDto trend = mock(CategoryTrendDto.class);

        when(journalRepository.findJournalBestSubjectAreaYearlyTrends("", 0, 9999))
                .thenReturn(List.of(trend));

        List<CategoryTrendDto> result =
                journalService.getJournalBestSubjectAreaYearlyTrends(null, null, null);

        assertEquals(1, result.size());
        assertSame(trend, result.get(0));

        verify(journalRepository).findJournalBestSubjectAreaYearlyTrends("", 0, 9999);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getPublisherOptions_returnsEmptyListAndDoesNotCallRepository_whenPublisherFilterIsBlank() {
        List<PublisherOptionDto> result =
                journalService.getPublisherOptions("   ", integer(10));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(journalRepository);
    }

    @Test
    void getPublisherOptions_returnsEmptyListAndDoesNotCallRepository_whenPublisherFilterIsNull() {
        List<PublisherOptionDto> result =
                journalService.getPublisherOptions(null, integer(10));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(journalRepository);
    }

    @Test
    void getPublisherOptions_trimsPublisherFilterAndReturnsExpectedOptions() {
        PublisherOptionDto publisher1 = mock(PublisherOptionDto.class);
        PublisherOptionDto publisher2 = mock(PublisherOptionDto.class);

        List<PublisherOptionDto> expectedPublishers = List.of(publisher1, publisher2);

        when(journalRepository.getPublisherOptions("Elsevier", 10))
                .thenReturn(expectedPublishers);

        List<PublisherOptionDto> result =
                journalService.getPublisherOptions("   Elsevier   ", integer(10));

        assertEquals(expectedPublishers, result);
        assertSame(publisher1, result.get(0));
        assertSame(publisher2, result.get(1));

        verify(journalRepository).getPublisherOptions("Elsevier", 10);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getPublisherOptions_usesDefaultLimit_whenLimitIsNullOrInvalid() {
        when(journalRepository.getPublisherOptions("Springer", 20))
                .thenReturn(List.of());

        journalService.getPublisherOptions("Springer", null);
        verify(journalRepository).getPublisherOptions("Springer", 20);

        clearInvocations(journalRepository);

        journalService.getPublisherOptions("Springer", integer(0));
        verify(journalRepository).getPublisherOptions("Springer", 20);

        clearInvocations(journalRepository);

        journalService.getPublisherOptions("Springer", integer(-5));
        verify(journalRepository).getPublisherOptions("Springer", 20);

        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getPublisherOptions_capsLimitToMaxLimit() {
        when(journalRepository.getPublisherOptions("IEEE", 50))
                .thenReturn(List.of());

        List<PublisherOptionDto> result =
                journalService.getPublisherOptions("IEEE", integer(500));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(journalRepository).getPublisherOptions("IEEE", 50);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getPublisherQuartilePublicationStats_returnsExpectedStats() {
        int publisherId = 3;

        PublisherQuartileStatsDto stats1 = mock(PublisherQuartileStatsDto.class);
        PublisherQuartileStatsDto stats2 = mock(PublisherQuartileStatsDto.class);

        List<PublisherQuartileStatsDto> expectedStats = List.of(stats1, stats2);

        when(journalRepository.getPublisherQuartilePublicationStatsById(publisherId))
                .thenReturn(expectedStats);

        List<PublisherQuartileStatsDto> result =
                journalService.getPublisherQuartilePublicationStats(publisherId);

        assertEquals(expectedStats, result);
        assertSame(stats1, result.get(0));
        assertSame(stats2, result.get(1));

        verify(journalRepository).getPublisherQuartilePublicationStatsById(publisherId);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getPublisherQuartilePublicationStats_throwsException_whenPublisherIdIsNotPositive() {
        assertThrows(
                IllegalArgumentException.class,
                () -> journalService.getPublisherQuartilePublicationStats(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> journalService.getPublisherQuartilePublicationStats(-1)
        );

        verifyNoInteractions(journalRepository);
    }

    @Test
    void getPublisherQuartilePublicationStatsForPublishers_returnsEmptyList_whenPublisherIdsAreNullOrEmpty() {
        List<PublisherQuartileStatsDto> nullResult =
                journalService.getPublisherQuartilePublicationStatsForPublishers(null);

        assertNotNull(nullResult);
        assertTrue(nullResult.isEmpty());

        List<PublisherQuartileStatsDto> emptyResult =
                journalService.getPublisherQuartilePublicationStatsForPublishers(List.of());

        assertNotNull(emptyResult);
        assertTrue(emptyResult.isEmpty());

        verifyNoInteractions(journalRepository);
    }

    @Test
    void getPublisherQuartilePublicationStatsForPublishers_ignoresNullIdsAndRemovesDuplicates() {
        PublisherQuartileStatsDto stats1 = mock(PublisherQuartileStatsDto.class);
        PublisherQuartileStatsDto stats2 = mock(PublisherQuartileStatsDto.class);

        when(journalRepository.getPublisherQuartilePublicationStatsById(1))
                .thenReturn(List.of(stats1));

        when(journalRepository.getPublisherQuartilePublicationStatsById(2))
                .thenReturn(List.of(stats2));

        List<Integer> publisherIds = new ArrayList<>();
        publisherIds.add(1);
        publisherIds.add(null);
        publisherIds.add(2);
        publisherIds.add(1);

        List<PublisherQuartileStatsDto> result =
                journalService.getPublisherQuartilePublicationStatsForPublishers(publisherIds);

        assertEquals(2, result.size());
        assertSame(stats1, result.get(0));
        assertSame(stats2, result.get(1));

        verify(journalRepository).getPublisherQuartilePublicationStatsById(1);
        verify(journalRepository).getPublisherQuartilePublicationStatsById(2);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getPublisherQuartilePublicationStatsForPublishers_throwsException_whenPublisherIdIsNotPositive() {
        assertThrows(
                IllegalArgumentException.class,
                () -> journalService.getPublisherQuartilePublicationStatsForPublishers(
                        List.of(1, 0, 2)
                )
        );

        verifyNoInteractions(journalRepository);
    }

    @Test
    void getPublisherQuartilePublicationStatsForPublishers_throwsException_whenMoreThanMaxPublishersAreSelected() {
        List<Integer> publisherIds =
                List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);

        assertThrows(
                IllegalArgumentException.class,
                () -> journalService.getPublisherQuartilePublicationStatsForPublishers(publisherIds)
        );

        verifyNoInteractions(journalRepository);
    }

    @Test
    void getJournalRankingScatterData_passesMetricsAndLimitToRepository() {
        ScatterPlotPointDto point1 = mock(ScatterPlotPointDto.class);
        ScatterPlotPointDto point2 = mock(ScatterPlotPointDto.class);

        List<ScatterPlotPointDto> expectedPoints = List.of(point1, point2);

        when(journalRepository.findJournalRankingScatterData("sjr", "hIndex", 100))
                .thenReturn(expectedPoints);

        List<ScatterPlotPointDto> result =
                journalService.getJournalRankingScatterData("sjr", "hIndex", integer(100));

        assertEquals(expectedPoints, result);
        assertSame(point1, result.get(0));
        assertSame(point2, result.get(1));

        verify(journalRepository).findJournalRankingScatterData("sjr", "hIndex", 100);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalRankingScatterData_usesNullLimit_whenLimitIsNullOrInvalid() {
        when(journalRepository.findJournalRankingScatterData("sjr", "hIndex", null))
                .thenReturn(List.of());

        journalService.getJournalRankingScatterData("sjr", "hIndex", null);
        verify(journalRepository).findJournalRankingScatterData("sjr", "hIndex", null);

        clearInvocations(journalRepository);

        journalService.getJournalRankingScatterData("sjr", "hIndex", integer(0));
        verify(journalRepository).findJournalRankingScatterData("sjr", "hIndex", null);

        clearInvocations(journalRepository);

        journalService.getJournalRankingScatterData("sjr", "hIndex", integer(-10));
        verify(journalRepository).findJournalRankingScatterData("sjr", "hIndex", null);

        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalRankingScatterData_capsLimitToMaxLimit() {
        when(journalRepository.findJournalRankingScatterData("sjr", "hIndex", 1000))
                .thenReturn(List.of());

        List<ScatterPlotPointDto> result =
                journalService.getJournalRankingScatterData("sjr", "hIndex", integer(5000));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(journalRepository).findJournalRankingScatterData("sjr", "hIndex", 1000);
        verifyNoMoreInteractions(journalRepository);
    }

    @Test
    void getJournalYearlyStats_usesDefaultYearRange_whenYearsAreNull() {
        int journalId = 8;

        JournalYearlyStatsDto stats = mock(JournalYearlyStatsDto.class);

        when(journalRepository.findJournalYearlyStats(journalId, 0, 9999))
                .thenReturn(List.of(stats));

        List<JournalYearlyStatsDto> result =
                journalService.getJournalYearlyStats(journalId, null, null);

        assertEquals(1, result.size());
        assertSame(stats, result.get(0));

        verify(journalRepository).findJournalYearlyStats(journalId, 0, 9999);
        verifyNoMoreInteractions(journalRepository);
    }
}