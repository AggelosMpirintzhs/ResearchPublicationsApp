package service;

import dto.author.AuthorProfileDto;
import dto.author.AuthorPublicationDto;
import dto.author.AuthorSearchResultDto;
import dto.author.AuthorYearlyStatsByTypeDto;
import dto.author.AuthorYearlyStatsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.AuthorRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    private AuthorService authorService;

    @BeforeEach
    void setUp() {
        authorService = new AuthorService(authorRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "ab",
            "a!"
    })
    void searchAuthors_returnsEmptyListAndDoesNotCallRepository_whenSearchTextIsTooShort(String searchText) {
        List<AuthorSearchResultDto> result =
                authorService.searchAuthors(searchText, integer(10));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(authorRepository);
    }

    @Test
    void searchAuthors_returnsEmptyListAndDoesNotCallRepository_whenSearchTextIsNull() {
        List<AuthorSearchResultDto> result =
                authorService.searchAuthors(null, integer(10));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(authorRepository);
    }

    @Test
    void searchAuthors_callsRepository_whenNormalizedSearchTextHasMinimumLengthAfterReplacingSymbols() {
        when(authorRepository.searchAuthors("a b", 10))
                .thenReturn(List.of());

        List<AuthorSearchResultDto> result =
                authorService.searchAuthors("  a-b  ", integer(10));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(authorRepository).searchAuthors("a b", 10);
        verifyNoMoreInteractions(authorRepository);
    }

    @Test
    void searchAuthors_normalizesSearchTextAndSortsResultsByRelevance() {
        AuthorSearchResultDto exactMatch = authorSearchResult("John Smith");
        AuthorSearchResultDto startsWithMatch = authorSearchResult("John Smith Jr");
        AuthorSearchResultDto middleWordMatch = authorSearchResult("Maria John Smith");
        AuthorSearchResultDto containsMatch = authorSearchResult("TheJohn Smith");
        AuthorSearchResultDto allWordsMatch = authorSearchResult("Smith and John");
        AuthorSearchResultDto noMatch = authorSearchResult("Alex Brown");

        when(authorRepository.searchAuthors("john smith", 10))
                .thenReturn(List.of(
                        allWordsMatch,
                        containsMatch,
                        exactMatch,
                        noMatch,
                        middleWordMatch,
                        startsWithMatch
                ));

        List<AuthorSearchResultDto> result =
                authorService.searchAuthors("  JOHN---SMITH!! ", integer(10));

        assertEquals(6, result.size());

        assertSame(exactMatch, result.get(0));
        assertSame(startsWithMatch, result.get(1));
        assertSame(middleWordMatch, result.get(2));
        assertSame(containsMatch, result.get(3));
        assertSame(allWordsMatch, result.get(4));
        assertSame(noMatch, result.get(5));

        assertEquals(
                List.of(
                        "John Smith",
                        "John Smith Jr",
                        "Maria John Smith",
                        "TheJohn Smith",
                        "Smith and John",
                        "Alex Brown"
                ),
                result.stream()
                        .map(AuthorSearchResultDto::authorName)
                        .toList()
        );

        verify(authorRepository).searchAuthors("john smith", 10);
        verifyNoMoreInteractions(authorRepository);
    }

    @Test
    void searchAuthors_returnsEmptyList_whenRepositoryReturnsNull() {
        when(authorRepository.searchAuthors("john", 20))
                .thenReturn(null);

        List<AuthorSearchResultDto> result =
                authorService.searchAuthors("john", null);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(authorRepository).searchAuthors("john", 20);
        verifyNoMoreInteractions(authorRepository);
    }

    @Test
    void searchAuthors_usesDefaultLimit_whenLimitIsNullOrInvalid() {
        when(authorRepository.searchAuthors("john", 20))
                .thenReturn(List.of());

        authorService.searchAuthors("john", null);
        verify(authorRepository).searchAuthors("john", 20);

        clearInvocations(authorRepository);

        authorService.searchAuthors("john", integer(0));
        verify(authorRepository).searchAuthors("john", 20);

        clearInvocations(authorRepository);

        authorService.searchAuthors("john", integer(-5));
        verify(authorRepository).searchAuthors("john", 20);

        verifyNoMoreInteractions(authorRepository);
    }

    @Test
    void searchAuthors_capsLimitToMaxSearchLimit() {
        when(authorRepository.searchAuthors("john", 50))
                .thenReturn(List.of());

        List<AuthorSearchResultDto> result =
                authorService.searchAuthors("john", integer(500));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(authorRepository).searchAuthors("john", 50);
        verifyNoMoreInteractions(authorRepository);
    }

    @Test
    void loadAuthorPageData_returnsExpectedProfileAndStats() {
        int authorId = 7;
        Integer startYear = integer(2010);
        Integer endYear = integer(2020);

        AuthorProfileDto profile = mock(AuthorProfileDto.class);
        AuthorYearlyStatsDto yearlyStats1 = mock(AuthorYearlyStatsDto.class);
        AuthorYearlyStatsDto yearlyStats2 = mock(AuthorYearlyStatsDto.class);
        AuthorYearlyStatsByTypeDto yearlyStatsByType1 = mock(AuthorYearlyStatsByTypeDto.class);

        List<AuthorYearlyStatsDto> yearlyStats =
                List.of(yearlyStats1, yearlyStats2);

        List<AuthorYearlyStatsByTypeDto> yearlyStatsByType =
                List.of(yearlyStatsByType1);

        when(authorRepository.findAuthorProfile(authorId, startYear, endYear))
                .thenReturn(Optional.of(profile));

        when(authorRepository.findAuthorYearlyStats(authorId, startYear, endYear))
                .thenReturn(yearlyStats);

        when(authorRepository.findAuthorYearlyStatsByType(authorId, startYear, endYear))
                .thenReturn(yearlyStatsByType);

        AuthorService.AuthorPageData result =
                authorService.loadAuthorPageData(authorId, startYear, endYear);

        assertNotNull(result);
        assertTrue(result.hasProfile());
        assertSame(profile, result.profile());
        assertEquals(yearlyStats, result.yearlyStats());
        assertEquals(yearlyStatsByType, result.yearlyStatsByType());

        verify(authorRepository).findAuthorProfile(authorId, startYear, endYear);
        verify(authorRepository).findAuthorYearlyStats(authorId, startYear, endYear);
        verify(authorRepository).findAuthorYearlyStatsByType(authorId, startYear, endYear);
        verifyNoMoreInteractions(authorRepository);
    }

    @Test
    void loadAuthorPageData_returnsPageDataWithoutProfile_whenRepositoryDoesNotFindProfile() {
        int authorId = 7;
        Integer startYear = integer(2010);
        Integer endYear = integer(2020);

        List<AuthorYearlyStatsDto> yearlyStats =
                List.of(mock(AuthorYearlyStatsDto.class));

        List<AuthorYearlyStatsByTypeDto> yearlyStatsByType =
                List.of(mock(AuthorYearlyStatsByTypeDto.class));

        when(authorRepository.findAuthorProfile(authorId, startYear, endYear))
                .thenReturn(Optional.empty());

        when(authorRepository.findAuthorYearlyStats(authorId, startYear, endYear))
                .thenReturn(yearlyStats);

        when(authorRepository.findAuthorYearlyStatsByType(authorId, startYear, endYear))
                .thenReturn(yearlyStatsByType);

        AuthorService.AuthorPageData result =
                authorService.loadAuthorPageData(authorId, startYear, endYear);

        assertNotNull(result);
        assertFalse(result.hasProfile());
        assertNull(result.profile());
        assertEquals(0L, result.expectedPublicationCount());
        assertEquals(yearlyStats, result.yearlyStats());
        assertEquals(yearlyStatsByType, result.yearlyStatsByType());

        verify(authorRepository).findAuthorProfile(authorId, startYear, endYear);
        verify(authorRepository).findAuthorYearlyStats(authorId, startYear, endYear);
        verify(authorRepository).findAuthorYearlyStatsByType(authorId, startYear, endYear);
        verifyNoMoreInteractions(authorRepository);
    }

    @Test
    void authorPageDataExpectedPublicationCount_returnsTotalArticles_whenProfileExists() {
        AuthorProfileDto profile = mock(AuthorProfileDto.class);

        when(profile.totalArticles()).thenReturn(25L);

        AuthorService.AuthorPageData pageData =
                new AuthorService.AuthorPageData(
                        profile,
                        List.of(),
                        List.of()
                );

        assertTrue(pageData.hasProfile());
        assertEquals(25L, pageData.expectedPublicationCount());

        verify(profile).totalArticles();
        verifyNoMoreInteractions(profile);
    }

    @Test
    void authorPageDataExpectedPublicationCount_returnsZero_whenProfileTotalArticlesIsNull() {
        AuthorProfileDto profile = mock(AuthorProfileDto.class);

        when(profile.totalArticles()).thenReturn(null);

        AuthorService.AuthorPageData pageData =
                new AuthorService.AuthorPageData(
                        profile,
                        List.of(),
                        List.of()
                );

        assertTrue(pageData.hasProfile());
        assertEquals(0L, pageData.expectedPublicationCount());

        verify(profile).totalArticles();
        verifyNoMoreInteractions(profile);
    }

    @Test
    void getAuthorProfile_usesDefaultYearRange_whenYearsAreNull() {
        int authorId = 3;
        AuthorProfileDto profile = mock(AuthorProfileDto.class);

        when(authorRepository.findAuthorProfile(authorId, 0, 9999))
                .thenReturn(Optional.of(profile));

        Optional<AuthorProfileDto> result =
                authorService.getAuthorProfile(authorId, null, null);

        assertTrue(result.isPresent());
        assertSame(profile, result.orElseThrow());

        verify(authorRepository).findAuthorProfile(authorId, 0, 9999);
        verifyNoMoreInteractions(authorRepository);
    }

    @Test
    void getAuthorYearlyStats_returnsExpectedStats() {
        int authorId = 4;
        Integer startYear = integer(2015);
        Integer endYear = integer(2021);

        AuthorYearlyStatsDto stats1 = mock(AuthorYearlyStatsDto.class);
        AuthorYearlyStatsDto stats2 = mock(AuthorYearlyStatsDto.class);

        List<AuthorYearlyStatsDto> expectedStats =
                List.of(stats1, stats2);

        when(authorRepository.findAuthorYearlyStats(authorId, startYear, endYear))
                .thenReturn(expectedStats);

        List<AuthorYearlyStatsDto> result =
                authorService.getAuthorYearlyStats(authorId, startYear, endYear);

        assertEquals(expectedStats, result);
        assertSame(stats1, result.get(0));
        assertSame(stats2, result.get(1));

        verify(authorRepository).findAuthorYearlyStats(authorId, startYear, endYear);
        verifyNoMoreInteractions(authorRepository);
    }

    @Test
    void getAuthorYearlyStatsByType_returnsExpectedStats() {
        int authorId = 4;
        Integer startYear = integer(2015);
        Integer endYear = integer(2021);

        AuthorYearlyStatsByTypeDto stats1 = mock(AuthorYearlyStatsByTypeDto.class);
        AuthorYearlyStatsByTypeDto stats2 = mock(AuthorYearlyStatsByTypeDto.class);

        List<AuthorYearlyStatsByTypeDto> expectedStats =
                List.of(stats1, stats2);

        when(authorRepository.findAuthorYearlyStatsByType(authorId, startYear, endYear))
                .thenReturn(expectedStats);

        List<AuthorYearlyStatsByTypeDto> result =
                authorService.getAuthorYearlyStatsByType(authorId, startYear, endYear);

        assertEquals(expectedStats, result);
        assertSame(stats1, result.get(0));
        assertSame(stats2, result.get(1));

        verify(authorRepository).findAuthorYearlyStatsByType(authorId, startYear, endYear);
        verifyNoMoreInteractions(authorRepository);
    }

    @Test
    void getAuthorPublicationsBatch_usesDefaultLastArticleIdAndDefaultBatchSize() {
        int authorId = 8;

        AuthorPublicationDto publication1 = authorPublication(integer(101));
        AuthorPublicationDto publication2 = authorPublication(integer(102));

        List<AuthorPublicationDto> expectedBatch =
                List.of(publication1, publication2);

        when(authorRepository.findAuthorPublicationsBatch(authorId, 0, 9999, 0, 1000))
                .thenReturn(expectedBatch);

        List<AuthorPublicationDto> result =
                authorService.getAuthorPublicationsBatch(
                        authorId,
                        null,
                        null,
                        null,
                        null
                );

        assertEquals(expectedBatch, result);
        assertEquals(ids(101, 102), articleIds(result));

        verify(authorRepository).findAuthorPublicationsBatch(authorId, 0, 9999, 0, 1000);
        verifyNoMoreInteractions(authorRepository);
    }

    @Test
    void getAuthorPublicationsBatch_capsBatchSizeToMaxBatchSize() {
        int authorId = 8;
        Integer startYear = integer(2010);
        Integer endYear = integer(2020);
        Integer lastArticleId = integer(50);
        Integer veryLargeBatchSize = integer(100_000);

        when(authorRepository.findAuthorPublicationsBatch(authorId, startYear, endYear, lastArticleId, 5000))
                .thenReturn(List.of());

        List<AuthorPublicationDto> result =
                authorService.getAuthorPublicationsBatch(
                        authorId,
                        startYear,
                        endYear,
                        lastArticleId,
                        veryLargeBatchSize
                );

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(authorRepository).findAuthorPublicationsBatch(authorId, startYear, endYear, lastArticleId, 5000);
        verifyNoMoreInteractions(authorRepository);
    }

    @Test
    void loadAuthorPublicationsInBatches_loadsAllBatchesAndUpdatesLastArticleId() {
        int authorId = 9;
        Integer startYear = integer(2010);
        Integer endYear = integer(2020);
        Integer batchSize = integer(2);

        AuthorPublicationDto publication101 = authorPublication(integer(101));
        AuthorPublicationDto publication102 = authorPublication(integer(102));
        AuthorPublicationDto publication205 = authorPublication(integer(205));

        List<AuthorPublicationDto> firstBatch =
                List.of(publication101, publication102);

        List<AuthorPublicationDto> secondBatch =
                List.of(publication205);

        when(authorRepository.findAuthorPublicationsBatch(authorId, startYear, endYear, 0, batchSize))
                .thenReturn(firstBatch);

        when(authorRepository.findAuthorPublicationsBatch(authorId, startYear, endYear, 102, batchSize))
                .thenReturn(secondBatch);

        when(authorRepository.findAuthorPublicationsBatch(authorId, startYear, endYear, 205, batchSize))
                .thenReturn(List.of());

        List<List<AuthorPublicationDto>> loadedBatches = new ArrayList<>();

        authorService.loadAuthorPublicationsInBatches(
                authorId,
                startYear,
                endYear,
                null,
                batchSize,
                loadedBatches::add,
                () -> true
        );

        assertEquals(2, loadedBatches.size());

        assertEquals(ids(101, 102), articleIds(loadedBatches.get(0)));
        assertEquals(ids(205), articleIds(loadedBatches.get(1)));

        verify(authorRepository).findAuthorPublicationsBatch(authorId, startYear, endYear, 0, batchSize);
        verify(authorRepository).findAuthorPublicationsBatch(authorId, startYear, endYear, 102, batchSize);
        verify(authorRepository).findAuthorPublicationsBatch(authorId, startYear, endYear, 205, batchSize);
        verifyNoMoreInteractions(authorRepository);
    }

    @Test
    void loadAuthorPublicationsInBatches_stopsWhenShouldContinueReturnsFalse() {
        int authorId = 9;
        Integer startYear = integer(2010);
        Integer endYear = integer(2020);
        Integer batchSize = integer(2);

        List<AuthorPublicationDto> firstBatch =
                List.of(
                        authorPublication(integer(101)),
                        authorPublication(integer(102))
                );

        when(authorRepository.findAuthorPublicationsBatch(authorId, startYear, endYear, 0, batchSize))
                .thenReturn(firstBatch);

        List<List<AuthorPublicationDto>> loadedBatches = new ArrayList<>();

        AtomicInteger checks = new AtomicInteger(0);

        authorService.loadAuthorPublicationsInBatches(
                authorId,
                startYear,
                endYear,
                null,
                batchSize,
                loadedBatches::add,
                () -> checks.getAndIncrement() < 1
        );

        assertEquals(1, loadedBatches.size());
        assertEquals(ids(101, 102), articleIds(loadedBatches.get(0)));

        verify(authorRepository).findAuthorPublicationsBatch(authorId, startYear, endYear, 0, batchSize);
        verifyNoMoreInteractions(authorRepository);
    }

    @Test
    void loadAuthorPublicationsInBatches_stopsWhenLastArticleIdIsNull() {
        int authorId = 9;
        Integer startYear = integer(2010);
        Integer endYear = integer(2020);
        Integer batchSize = integer(2);

        List<AuthorPublicationDto> firstBatch =
                List.of(
                        authorPublication(integer(101)),
                        authorPublication(null)
                );

        when(authorRepository.findAuthorPublicationsBatch(authorId, startYear, endYear, 0, batchSize))
                .thenReturn(firstBatch);

        List<List<AuthorPublicationDto>> loadedBatches = new ArrayList<>();

        authorService.loadAuthorPublicationsInBatches(
                authorId,
                startYear,
                endYear,
                null,
                batchSize,
                loadedBatches::add,
                () -> true
        );

        assertEquals(1, loadedBatches.size());
        assertEquals(idsWithNull(Integer.valueOf(101), null), articleIds(loadedBatches.get(0)));

        verify(authorRepository).findAuthorPublicationsBatch(authorId, startYear, endYear, 0, batchSize);
        verifyNoMoreInteractions(authorRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -50})
    void serviceMethodsThrowException_whenAuthorIdIsNotPositive(int invalidAuthorId) {
        assertThrows(
                IllegalArgumentException.class,
                () -> authorService.loadAuthorPageData(
                        invalidAuthorId,
                        integer(2010),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> authorService.getAuthorProfile(
                        invalidAuthorId,
                        integer(2010),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> authorService.getAuthorYearlyStats(
                        invalidAuthorId,
                        integer(2010),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> authorService.getAuthorYearlyStatsByType(
                        invalidAuthorId,
                        integer(2010),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> authorService.getAuthorPublicationsBatch(
                        invalidAuthorId,
                        integer(2010),
                        integer(2020),
                        integer(0),
                        integer(100)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> authorService.loadAuthorPublicationsInBatches(
                        invalidAuthorId,
                        integer(2010),
                        integer(2020),
                        integer(0),
                        integer(100),
                        batch -> {
                        },
                        () -> true
                )
        );

        verifyNoInteractions(authorRepository);
    }

    @Test
    void serviceMethodsThrowException_whenStartYearIsGreaterThanEndYear() {
        assertThrows(
                IllegalArgumentException.class,
                () -> authorService.loadAuthorPageData(
                        1,
                        integer(2025),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> authorService.getAuthorProfile(
                        1,
                        integer(2025),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> authorService.getAuthorYearlyStats(
                        1,
                        integer(2025),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> authorService.getAuthorYearlyStatsByType(
                        1,
                        integer(2025),
                        integer(2020)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> authorService.getAuthorPublicationsBatch(
                        1,
                        integer(2025),
                        integer(2020),
                        integer(0),
                        integer(100)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> authorService.loadAuthorPublicationsInBatches(
                        1,
                        integer(2025),
                        integer(2020),
                        integer(0),
                        integer(100),
                        batch -> {
                        },
                        () -> true
                )
        );

        verifyNoInteractions(authorRepository);
    }

    @Test
    void getAuthorPublicationsBatchThrowsException_whenLastArticleIdIsNegative() {
        assertThrows(
                IllegalArgumentException.class,
                () -> authorService.getAuthorPublicationsBatch(
                        1,
                        integer(2010),
                        integer(2020),
                        integer(-1),
                        integer(100)
                )
        );

        verifyNoInteractions(authorRepository);
    }

    @Test
    void loadAuthorPublicationsInBatchesThrowsException_whenLastArticleIdIsNegative() {
        assertThrows(
                IllegalArgumentException.class,
                () -> authorService.loadAuthorPublicationsInBatches(
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

        verifyNoInteractions(authorRepository);
    }

    @Test
    void loadAuthorPublicationsInBatchesThrowsException_whenCallbackIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> authorService.loadAuthorPublicationsInBatches(
                        1,
                        integer(2010),
                        integer(2020),
                        integer(0),
                        integer(100),
                        null,
                        () -> true
                )
        );

        verifyNoInteractions(authorRepository);
    }

    @Test
    void loadAuthorPublicationsInBatchesThrowsException_whenShouldContinueIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> authorService.loadAuthorPublicationsInBatches(
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

        verifyNoInteractions(authorRepository);
    }

    private AuthorSearchResultDto authorSearchResult(String authorName) {
        AuthorSearchResultDto dto = mock(AuthorSearchResultDto.class);
        when(dto.authorName()).thenReturn(authorName);
        return dto;
    }

    private AuthorPublicationDto authorPublication(Integer articleId) {
        AuthorPublicationDto dto = mock(AuthorPublicationDto.class);
        when(dto.articleId()).thenReturn(articleId);
        return dto;
    }

    private List<Integer> articleIds(List<AuthorPublicationDto> publications) {
        return publications.stream()
                .map(AuthorPublicationDto::articleId)
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

    private List<Integer> idsWithNull(Integer firstValue, Integer secondValue) {
        List<Integer> result = new ArrayList<>();
        result.add(firstValue);
        result.add(secondValue);
        return result;
    }
}