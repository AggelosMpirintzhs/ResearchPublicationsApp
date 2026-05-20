package service;

import dto.conference.ConferenceArticleDto;
import dto.conference.ConferenceProfileDto;
import dto.conference.ConferenceRankingDto;
import dto.conference.ConferenceSearchResultDto;
import dto.conference.ConferenceYearlyStatsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.ConferenceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConferenceServiceTest {

    @Mock
    private ConferenceRepository conferenceRepository;

    private ConferenceService conferenceService;

    @BeforeEach
    void setUp() {
        conferenceService = new ConferenceService(conferenceRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "     "
    })
    void searchConferences_returnsEmptyListAndDoesNotCallRepository_whenSearchTextIsBlank(String searchText) {
        List<ConferenceSearchResultDto> result =
                conferenceService.searchConferences(searchText, integer(10));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(conferenceRepository);
    }

    @Test
    void searchConferences_returnsEmptyListAndDoesNotCallRepository_whenSearchTextIsNull() {
        List<ConferenceSearchResultDto> result =
                conferenceService.searchConferences(null, integer(10));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(conferenceRepository);
    }

    @Test
    void searchConferences_trimsSearchTextAndReturnsExpectedResults() {
        ConferenceSearchResultDto conference1 = mock(ConferenceSearchResultDto.class);
        ConferenceSearchResultDto conference2 = mock(ConferenceSearchResultDto.class);

        List<ConferenceSearchResultDto> expectedResults =
                List.of(conference1, conference2);

        when(conferenceRepository.searchConferences("ICDE", 10))
                .thenReturn(expectedResults);

        List<ConferenceSearchResultDto> result =
                conferenceService.searchConferences("   ICDE   ", integer(10));

        assertEquals(expectedResults, result);
        assertSame(conference1, result.get(0));
        assertSame(conference2, result.get(1));

        verify(conferenceRepository).searchConferences("ICDE", 10);
        verifyNoMoreInteractions(conferenceRepository);
    }

    @Test
    void searchConferences_usesDefaultLimit_whenLimitIsNullOrInvalid() {
        when(conferenceRepository.searchConferences("SIGMOD", 20))
                .thenReturn(List.of());

        conferenceService.searchConferences("SIGMOD", null);
        verify(conferenceRepository).searchConferences("SIGMOD", 20);

        clearInvocations(conferenceRepository);

        conferenceService.searchConferences("SIGMOD", integer(0));
        verify(conferenceRepository).searchConferences("SIGMOD", 20);

        clearInvocations(conferenceRepository);

        conferenceService.searchConferences("SIGMOD", integer(-5));
        verify(conferenceRepository).searchConferences("SIGMOD", 20);

        verifyNoMoreInteractions(conferenceRepository);
    }

    @Test
    void searchConferences_capsLimitToMaxLimit() {
        when(conferenceRepository.searchConferences("VLDB", 100))
                .thenReturn(List.of());

        List<ConferenceSearchResultDto> result =
                conferenceService.searchConferences("VLDB", integer(500));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(conferenceRepository).searchConferences("VLDB", 100);
        verifyNoMoreInteractions(conferenceRepository);
    }

    @Test
    void getConferenceProfile_returnsExpectedProfile() {
        int conferenceId = 5;
        Integer startYear = integer(2010);
        Integer endYear = integer(2020);

        ConferenceProfileDto profile = mock(ConferenceProfileDto.class);

        when(conferenceRepository.findConferenceProfile(conferenceId, startYear, endYear))
                .thenReturn(Optional.of(profile));

        Optional<ConferenceProfileDto> result =
                conferenceService.getConferenceProfile(conferenceId, startYear, endYear);

        assertTrue(result.isPresent());
        assertSame(profile, result.orElseThrow());

        verify(conferenceRepository).findConferenceProfile(conferenceId, startYear, endYear);
        verifyNoMoreInteractions(conferenceRepository);
    }

    @Test
    void getConferenceProfile_returnsEmptyOptional_whenRepositoryDoesNotFindProfile() {
        int conferenceId = 5;
        Integer startYear = integer(2010);
        Integer endYear = integer(2020);

        when(conferenceRepository.findConferenceProfile(conferenceId, startYear, endYear))
                .thenReturn(Optional.empty());

        Optional<ConferenceProfileDto> result =
                conferenceService.getConferenceProfile(conferenceId, startYear, endYear);

        assertTrue(result.isEmpty());

        verify(conferenceRepository).findConferenceProfile(conferenceId, startYear, endYear);
        verifyNoMoreInteractions(conferenceRepository);
    }

    @Test
    void getConferenceProfile_usesDefaultYearRange_whenYearsAreNull() {
        int conferenceId = 5;

        ConferenceProfileDto profile = mock(ConferenceProfileDto.class);

        when(conferenceRepository.findConferenceProfile(conferenceId, 0, 9999))
                .thenReturn(Optional.of(profile));

        Optional<ConferenceProfileDto> result =
                conferenceService.getConferenceProfile(conferenceId, null, null);

        assertTrue(result.isPresent());
        assertSame(profile, result.orElseThrow());

        verify(conferenceRepository).findConferenceProfile(conferenceId, 0, 9999);
        verifyNoMoreInteractions(conferenceRepository);
    }

    @Test
    void getConferenceYearlyStats_returnsExpectedStats() {
        int conferenceId = 8;
        Integer startYear = integer(2015);
        Integer endYear = integer(2022);

        ConferenceYearlyStatsDto stats1 = mock(ConferenceYearlyStatsDto.class);
        ConferenceYearlyStatsDto stats2 = mock(ConferenceYearlyStatsDto.class);

        List<ConferenceYearlyStatsDto> expectedStats =
                List.of(stats1, stats2);

        when(conferenceRepository.findConferenceYearlyStats(conferenceId, startYear, endYear))
                .thenReturn(expectedStats);

        List<ConferenceYearlyStatsDto> result =
                conferenceService.getConferenceYearlyStats(conferenceId, startYear, endYear);

        assertEquals(expectedStats, result);
        assertSame(stats1, result.get(0));
        assertSame(stats2, result.get(1));

        verify(conferenceRepository).findConferenceYearlyStats(conferenceId, startYear, endYear);
        verifyNoMoreInteractions(conferenceRepository);
    }

    @Test
    void getConferenceRanking_returnsExpectedRanking() {
        int conferenceId = 11;

        ConferenceRankingDto ranking = mock(ConferenceRankingDto.class);

        when(conferenceRepository.findConferenceRanking(conferenceId))
                .thenReturn(Optional.of(ranking));

        Optional<ConferenceRankingDto> result =
                conferenceService.getConferenceRanking(conferenceId);

        assertTrue(result.isPresent());
        assertSame(ranking, result.orElseThrow());

        verify(conferenceRepository).findConferenceRanking(conferenceId);
        verifyNoMoreInteractions(conferenceRepository);
    }

    @Test
    void getConferenceRanking_returnsEmptyOptional_whenRepositoryDoesNotFindRanking() {
        int conferenceId = 11;

        when(conferenceRepository.findConferenceRanking(conferenceId))
                .thenReturn(Optional.empty());

        Optional<ConferenceRankingDto> result =
                conferenceService.getConferenceRanking(conferenceId);

        assertTrue(result.isEmpty());

        verify(conferenceRepository).findConferenceRanking(conferenceId);
        verifyNoMoreInteractions(conferenceRepository);
    }

    @Test
    void getConferenceArticles_returnsExpectedArticles() {
        int conferenceId = 9;
        Integer startYear = integer(2018);
        Integer endYear = integer(2023);

        ConferenceArticleDto article1 = mock(ConferenceArticleDto.class);
        ConferenceArticleDto article2 = mock(ConferenceArticleDto.class);

        List<ConferenceArticleDto> expectedArticles =
                List.of(article1, article2);

        when(conferenceRepository.findConferenceArticles(conferenceId, startYear, endYear))
                .thenReturn(expectedArticles);

        List<ConferenceArticleDto> result =
                conferenceService.getConferenceArticles(conferenceId, startYear, endYear);

        assertEquals(expectedArticles, result);
        assertSame(article1, result.get(0));
        assertSame(article2, result.get(1));

        verify(conferenceRepository).findConferenceArticles(conferenceId, startYear, endYear);
        verifyNoMoreInteractions(conferenceRepository);
    }

    @Test
    void getConferenceArticles_usesDefaultYearRange_whenYearsAreNull() {
        int conferenceId = 9;

        ConferenceArticleDto article = mock(ConferenceArticleDto.class);

        when(conferenceRepository.findConferenceArticles(conferenceId, 0, 9999))
                .thenReturn(List.of(article));

        List<ConferenceArticleDto> result =
                conferenceService.getConferenceArticles(conferenceId, null, null);

        assertEquals(1, result.size());
        assertSame(article, result.get(0));

        verify(conferenceRepository).findConferenceArticles(conferenceId, 0, 9999);
        verifyNoMoreInteractions(conferenceRepository);
    }

    @Test
    void getConferenceArticlesBatch_usesDefaultLastArticleIdAndDefaultBatchSize() {
        int conferenceId = 12;

        ConferenceArticleDto article1 = conferenceArticle(integer(101));
        ConferenceArticleDto article2 = conferenceArticle(integer(102));

        List<ConferenceArticleDto> expectedBatch =
                List.of(article1, article2);

        when(conferenceRepository.findConferenceArticlesBatch(conferenceId, 0, 9999, 0, 1000))
                .thenReturn(expectedBatch);

        List<ConferenceArticleDto> result =
                conferenceService.getConferenceArticlesBatch(
                        conferenceId,
                        null,
                        null,
                        null,
                        null
                );

        assertEquals(expectedBatch, result);
        assertEquals(ids(101, 102), articleIds(result));

        verify(conferenceRepository).findConferenceArticlesBatch(conferenceId, 0, 9999, 0, 1000);
        verifyNoMoreInteractions(conferenceRepository);
    }

    @Test
    void getConferenceArticlesBatch_usesGivenLastArticleIdAndBatchSize() {
        int conferenceId = 12;
        Integer startYear = integer(2012);
        Integer endYear = integer(2020);
        Integer lastArticleId = integer(55);
        Integer batchSize = integer(250);

        ConferenceArticleDto article = conferenceArticle(integer(70));

        when(conferenceRepository.findConferenceArticlesBatch(conferenceId, startYear, endYear, lastArticleId, batchSize))
                .thenReturn(List.of(article));

        List<ConferenceArticleDto> result =
                conferenceService.getConferenceArticlesBatch(
                        conferenceId,
                        startYear,
                        endYear,
                        lastArticleId,
                        batchSize
                );

        assertEquals(1, result.size());
        assertEquals(ids(70), articleIds(result));

        verify(conferenceRepository).findConferenceArticlesBatch(conferenceId, startYear, endYear, lastArticleId, batchSize);
        verifyNoMoreInteractions(conferenceRepository);
    }

    @Test
    void getConferenceArticlesBatch_capsBatchSizeToMaxBatchSize() {
        int conferenceId = 12;
        Integer startYear = integer(2012);
        Integer endYear = integer(2020);
        Integer lastArticleId = integer(55);
        Integer veryLargeBatchSize = integer(100_000);

        when(conferenceRepository.findConferenceArticlesBatch(conferenceId, startYear, endYear, lastArticleId, 5000))
                .thenReturn(List.of());

        List<ConferenceArticleDto> result =
                conferenceService.getConferenceArticlesBatch(
                        conferenceId,
                        startYear,
                        endYear,
                        lastArticleId,
                        veryLargeBatchSize
                );

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(conferenceRepository).findConferenceArticlesBatch(conferenceId, startYear, endYear, lastArticleId, 5000);
        verifyNoMoreInteractions(conferenceRepository);
    }

    @Test
    void getConferenceArticlesBatch_usesDefaultBatchSize_whenBatchSizeIsNullOrInvalid() {
        int conferenceId = 12;
        Integer startYear = integer(2012);
        Integer endYear = integer(2020);
        Integer lastArticleId = integer(55);

        when(conferenceRepository.findConferenceArticlesBatch(conferenceId, startYear, endYear, lastArticleId, 1000))
                .thenReturn(List.of());

        conferenceService.getConferenceArticlesBatch(
                conferenceId,
                startYear,
                endYear,
                lastArticleId,
                null
        );

        verify(conferenceRepository).findConferenceArticlesBatch(conferenceId, startYear, endYear, lastArticleId, 1000);

        clearInvocations(conferenceRepository);

        conferenceService.getConferenceArticlesBatch(
                conferenceId,
                startYear,
                endYear,
                lastArticleId,
                integer(0)
        );

        verify(conferenceRepository).findConferenceArticlesBatch(conferenceId, startYear, endYear, lastArticleId, 1000);

        clearInvocations(conferenceRepository);

        conferenceService.getConferenceArticlesBatch(
                conferenceId,
                startYear,
                endYear,
                lastArticleId,
                integer(-10)
        );

        verify(conferenceRepository).findConferenceArticlesBatch(conferenceId, startYear, endYear, lastArticleId, 1000);

        verifyNoMoreInteractions(conferenceRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -50})
    void serviceMethodsThrowException_whenConferenceIdIsNotPositive(int invalidConferenceId) {
        assertThrows(
                IllegalArgumentException.class,
                () -> conferenceService.getConferenceProfile(
                        invalidConferenceId,
                        integer(2010),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> conferenceService.getConferenceYearlyStats(
                        invalidConferenceId,
                        integer(2010),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> conferenceService.getConferenceRanking(invalidConferenceId)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> conferenceService.getConferenceArticles(
                        invalidConferenceId,
                        integer(2010),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> conferenceService.getConferenceArticlesBatch(
                        invalidConferenceId,
                        integer(2010),
                        integer(2020),
                        integer(0),
                        integer(100)
                )
        );

        verifyNoInteractions(conferenceRepository);
    }

    @Test
    void serviceMethodsThrowException_whenStartYearIsGreaterThanEndYear() {
        assertThrows(
                IllegalArgumentException.class,
                () -> conferenceService.getConferenceProfile(
                        1,
                        integer(2025),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> conferenceService.getConferenceYearlyStats(
                        1,
                        integer(2025),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> conferenceService.getConferenceArticles(
                        1,
                        integer(2025),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> conferenceService.getConferenceArticlesBatch(
                        1,
                        integer(2025),
                        integer(2020),
                        integer(0),
                        integer(100)
                )
        );

        verifyNoInteractions(conferenceRepository);
    }

    @Test
    void getConferenceArticlesBatchThrowsException_whenLastArticleIdIsNegative() {
        assertThrows(
                IllegalArgumentException.class,
                () -> conferenceService.getConferenceArticlesBatch(
                        1,
                        integer(2010),
                        integer(2020),
                        integer(-1),
                        integer(100)
                )
        );

        verifyNoInteractions(conferenceRepository);
    }

    private ConferenceArticleDto conferenceArticle(Integer articleId) {
        ConferenceArticleDto dto = mock(ConferenceArticleDto.class);
        when(dto.articleId()).thenReturn(articleId);
        return dto;
    }

    private List<Integer> articleIds(List<ConferenceArticleDto> articles) {
        return articles.stream()
                .map(ConferenceArticleDto::articleId)
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
}