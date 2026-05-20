package service;

import dto.year.AvailableYearDto;
import dto.year.YearProfileDto;
import dto.year.YearPublicationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.YearRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YearServiceTest {

    @Mock
    private YearRepository yearRepository;

    private YearService yearService;

    @BeforeEach
    void setUp() {
        yearService = new YearService(yearRepository);
    }

    @Test
    void getAvailableYears_returnsExpectedYears() {
        AvailableYearDto year2020 = availableYear(100L, 60L, 40L);
        AvailableYearDto year2021 = availableYear(120L, 70L, 50L);

        List<AvailableYearDto> expectedYears = List.of(year2020, year2021);

        when(yearRepository.findAvailableYears())
                .thenReturn(expectedYears);

        List<AvailableYearDto> result = yearService.getAvailableYears();

        assertEquals(expectedYears, result);
        assertSame(year2020, result.get(0));
        assertSame(year2021, result.get(1));

        verify(yearRepository).findAvailableYears();
        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void loadYearPageData_usesDefaultPublicationTypeAndReturnsExpectedProfileData() {
        int year = 2020;

        YearProfileDto profile = yearProfile(150L, 90L, 60L);

        when(yearRepository.findYearProfile(year))
                .thenReturn(Optional.of(profile));

        YearService.YearPageData result =
                yearService.loadYearPageData(year, null);

        assertNotNull(result);
        assertEquals(year, result.year());
        assertEquals(YearService.PUBLICATION_TYPE_ALL, result.publicationType());
        assertSame(profile, result.profile());
        assertTrue(result.hasProfile());
        assertEquals(150L, result.expectedPublicationCount());

        verify(yearRepository).findYearProfile(year);
        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void loadYearPageData_normalizesPublicationTypeAndReturnsJournalCount() {
        int year = 2020;

        YearProfileDto profile = yearProfile(150L, 90L, 60L);

        when(yearRepository.findYearProfile(year))
                .thenReturn(Optional.of(profile));

        YearService.YearPageData result =
                yearService.loadYearPageData(year, "  journal  ");

        assertNotNull(result);
        assertEquals(year, result.year());
        assertEquals(YearService.PUBLICATION_TYPE_JOURNAL, result.publicationType());
        assertSame(profile, result.profile());
        assertTrue(result.hasProfile());
        assertEquals(90L, result.expectedPublicationCount());

        verify(yearRepository).findYearProfile(year);
        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void loadYearPageData_returnsEmptyProfileData_whenProfileDoesNotExist() {
        int year = 2020;

        when(yearRepository.findYearProfile(year))
                .thenReturn(Optional.empty());

        YearService.YearPageData result =
                yearService.loadYearPageData(year, YearService.PUBLICATION_TYPE_CONFERENCE);

        assertNotNull(result);
        assertEquals(year, result.year());
        assertEquals(YearService.PUBLICATION_TYPE_CONFERENCE, result.publicationType());
        assertNull(result.profile());
        assertFalse(result.hasProfile());
        assertEquals(0L, result.expectedPublicationCount());

        verify(yearRepository).findYearProfile(year);
        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void getYearProfile_returnsExpectedProfile() {
        int year = 2021;

        YearProfileDto profile = yearProfile(200L, 130L, 70L);

        when(yearRepository.findYearProfile(year))
                .thenReturn(Optional.of(profile));

        Optional<YearProfileDto> result =
                yearService.getYearProfile(year);

        assertTrue(result.isPresent());
        assertSame(profile, result.orElseThrow());

        verify(yearRepository).findYearProfile(year);
        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void getYearProfile_returnsEmptyOptional_whenProfileDoesNotExist() {
        int year = 2021;

        when(yearRepository.findYearProfile(year))
                .thenReturn(Optional.empty());

        Optional<YearProfileDto> result =
                yearService.getYearProfile(year);

        assertTrue(result.isEmpty());

        verify(yearRepository).findYearProfile(year);
        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void getExpectedPublicationCountFromAvailableYear_returnsExpectedCounts() {
        AvailableYearDto yearDto = availableYear(100L, 60L, 40L);

        assertEquals(
                100L,
                yearService.getExpectedPublicationCount(yearDto, YearService.PUBLICATION_TYPE_ALL)
        );

        assertEquals(
                60L,
                yearService.getExpectedPublicationCount(yearDto, YearService.PUBLICATION_TYPE_JOURNAL)
        );

        assertEquals(
                40L,
                yearService.getExpectedPublicationCount(yearDto, YearService.PUBLICATION_TYPE_CONFERENCE)
        );

        assertEquals(
                100L,
                yearService.getExpectedPublicationCount(yearDto, null)
        );

        assertEquals(
                60L,
                yearService.getExpectedPublicationCount(yearDto, "  journal  ")
        );

        verifyNoInteractions(yearRepository);
    }

    @Test
    void getExpectedPublicationCountFromAvailableYear_returnsZeroForNullValues() {
        AvailableYearDto yearDto = availableYear(null, null, null);

        assertEquals(
                0L,
                yearService.getExpectedPublicationCount(yearDto, YearService.PUBLICATION_TYPE_ALL)
        );

        assertEquals(
                0L,
                yearService.getExpectedPublicationCount(yearDto, YearService.PUBLICATION_TYPE_JOURNAL)
        );

        assertEquals(
                0L,
                yearService.getExpectedPublicationCount(yearDto, YearService.PUBLICATION_TYPE_CONFERENCE)
        );

        assertEquals(
                0L,
                yearService.getExpectedPublicationCount((AvailableYearDto) null, YearService.PUBLICATION_TYPE_ALL)
        );

        verifyNoInteractions(yearRepository);
    }

    @Test
    void getExpectedPublicationCountFromYearProfile_returnsExpectedCounts() {
        YearProfileDto profile = yearProfile(300L, 180L, 120L);

        assertEquals(
                300L,
                yearService.getExpectedPublicationCount(profile, YearService.PUBLICATION_TYPE_ALL)
        );

        assertEquals(
                180L,
                yearService.getExpectedPublicationCount(profile, YearService.PUBLICATION_TYPE_JOURNAL)
        );

        assertEquals(
                120L,
                yearService.getExpectedPublicationCount(profile, YearService.PUBLICATION_TYPE_CONFERENCE)
        );

        assertEquals(
                300L,
                yearService.getExpectedPublicationCount(profile, "")
        );

        assertEquals(
                120L,
                yearService.getExpectedPublicationCount(profile, " conference ")
        );

        verifyNoInteractions(yearRepository);
    }

    @Test
    void getExpectedPublicationCountFromYearProfile_returnsZeroForNullValues() {
        YearProfileDto profile = yearProfile(null, null, null);

        assertEquals(
                0L,
                yearService.getExpectedPublicationCount(profile, YearService.PUBLICATION_TYPE_ALL)
        );

        assertEquals(
                0L,
                yearService.getExpectedPublicationCount(profile, YearService.PUBLICATION_TYPE_JOURNAL)
        );

        assertEquals(
                0L,
                yearService.getExpectedPublicationCount(profile, YearService.PUBLICATION_TYPE_CONFERENCE)
        );

        assertEquals(
                0L,
                yearService.getExpectedPublicationCount((AvailableYearDto) null, YearService.PUBLICATION_TYPE_ALL)
        );

        verifyNoInteractions(yearRepository);
    }

    @Test
    void getYearPublications_usesDefaultPublicationTypeAndDefaultFilters() {
        int year = 2020;

        YearPublicationDto publication1 = yearPublication(101);
        YearPublicationDto publication2 = yearPublication(102);

        List<YearPublicationDto> expectedPublications =
                List.of(publication1, publication2);

        when(yearRepository.findYearPublications(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0
        )).thenReturn(expectedPublications);

        List<YearPublicationDto> result =
                yearService.getYearPublications(
                        year,
                        null,
                        null,
                        null,
                        null
                );

        assertEquals(expectedPublications, result);
        assertEquals(ids(101, 102), articleIds(result));

        verify(yearRepository).findYearPublications(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0
        );
        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void getYearPublications_normalizesPublicationTypeAndUsesGivenFilters() {
        int year = 2020;
        int authorId = 15;

        YearPublicationDto publication = yearPublication(201);

        when(yearRepository.findYearPublications(
                year,
                YearService.PUBLICATION_TYPE_JOURNAL,
                0,
                0,
                authorId
        )).thenReturn(List.of(publication));

        List<YearPublicationDto> result =
                yearService.getYearPublications(
                        year,
                        "  journal  ",
                        null,
                        null,
                        authorId
                );

        assertEquals(1, result.size());
        assertEquals(ids(201), articleIds(result));

        verify(yearRepository).findYearPublications(
                year,
                YearService.PUBLICATION_TYPE_JOURNAL,
                0,
                0,
                authorId
        );
        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void getYearPublicationsByJournal_usesJournalPublicationTypeAndJournalFilter() {
        int year = 2020;
        int journalId = 7;

        YearPublicationDto publication = yearPublication(301);

        when(yearRepository.findYearPublications(
                year,
                YearService.PUBLICATION_TYPE_JOURNAL,
                journalId,
                0,
                0
        )).thenReturn(List.of(publication));

        List<YearPublicationDto> result =
                yearService.getYearPublicationsByJournal(year, journalId);

        assertEquals(1, result.size());
        assertEquals(ids(301), articleIds(result));

        verify(yearRepository).findYearPublications(
                year,
                YearService.PUBLICATION_TYPE_JOURNAL,
                journalId,
                0,
                0
        );
        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void getYearPublicationsByConference_usesConferencePublicationTypeAndConferenceFilter() {
        int year = 2020;
        int conferenceId = 9;

        YearPublicationDto publication = yearPublication(401);

        when(yearRepository.findYearPublications(
                year,
                YearService.PUBLICATION_TYPE_CONFERENCE,
                0,
                conferenceId,
                0
        )).thenReturn(List.of(publication));

        List<YearPublicationDto> result =
                yearService.getYearPublicationsByConference(year, conferenceId);

        assertEquals(1, result.size());
        assertEquals(ids(401), articleIds(result));

        verify(yearRepository).findYearPublications(
                year,
                YearService.PUBLICATION_TYPE_CONFERENCE,
                0,
                conferenceId,
                0
        );
        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void getYearPublicationsByAuthor_usesAllPublicationTypeAndAuthorFilter() {
        int year = 2020;
        int authorId = 12;

        YearPublicationDto publication = yearPublication(501);

        when(yearRepository.findYearPublications(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                authorId
        )).thenReturn(List.of(publication));

        List<YearPublicationDto> result =
                yearService.getYearPublicationsByAuthor(year, authorId);

        assertEquals(1, result.size());
        assertEquals(ids(501), articleIds(result));

        verify(yearRepository).findYearPublications(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                authorId
        );
        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void getYearPublicationsBatch_usesDefaultLastArticleIdAndDefaultBatchSize() {
        int year = 2020;

        YearPublicationDto publication1 = yearPublication(101);
        YearPublicationDto publication2 = yearPublication(102);

        List<YearPublicationDto> expectedBatch =
                List.of(publication1, publication2);

        when(yearRepository.findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                0,
                1000
        )).thenReturn(expectedBatch);

        List<YearPublicationDto> result =
                yearService.getYearPublicationsBatch(
                        year,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        assertEquals(expectedBatch, result);
        assertEquals(ids(101, 102), articleIds(result));

        verify(yearRepository).findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                0,
                1000
        );
        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void getYearPublicationsBatch_usesGivenFiltersLastArticleIdAndBatchSize() {
        int year = 2020;
        int journalId = 5;
        int authorId = 14;
        int lastArticleId = 100;
        int batchSize = 250;

        YearPublicationDto publication = yearPublication(150);

        when(yearRepository.findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_JOURNAL,
                journalId,
                0,
                authorId,
                lastArticleId,
                batchSize
        )).thenReturn(List.of(publication));

        List<YearPublicationDto> result =
                yearService.getYearPublicationsBatch(
                        year,
                        YearService.PUBLICATION_TYPE_JOURNAL,
                        journalId,
                        null,
                        authorId,
                        lastArticleId,
                        batchSize
                );

        assertEquals(1, result.size());
        assertEquals(ids(150), articleIds(result));

        verify(yearRepository).findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_JOURNAL,
                journalId,
                0,
                authorId,
                lastArticleId,
                batchSize
        );
        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void getYearPublicationsBatch_capsBatchSizeToMaxBatchSize() {
        int year = 2020;

        when(yearRepository.findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                50,
                5000
        )).thenReturn(List.of());

        List<YearPublicationDto> result =
                yearService.getYearPublicationsBatch(
                        year,
                        YearService.PUBLICATION_TYPE_ALL,
                        null,
                        null,
                        null,
                        50,
                        100_000
                );

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(yearRepository).findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                50,
                5000
        );
        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void getYearPublicationsBatch_usesDefaultBatchSizeWhenBatchSizeIsNullOrInvalid() {
        int year = 2020;

        when(yearRepository.findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                0,
                1000
        )).thenReturn(List.of());

        yearService.getYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                null,
                null,
                null,
                null,
                null
        );

        verify(yearRepository).findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                0,
                1000
        );

        clearInvocations(yearRepository);

        yearService.getYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                null,
                null,
                null,
                null,
                0
        );

        verify(yearRepository).findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                0,
                1000
        );

        clearInvocations(yearRepository);

        yearService.getYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                null,
                null,
                null,
                null,
                -20
        );

        verify(yearRepository).findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                0,
                1000
        );

        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void convenienceBatchMethodsUseExpectedPublicationTypesAndFilters() {
        int year = 2020;

        when(yearRepository.findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                0,
                100
        )).thenReturn(List.of());

        yearService.getAllYearPublicationsBatch(year, 0, 100);

        verify(yearRepository).findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                0,
                100
        );

        clearInvocations(yearRepository);

        when(yearRepository.findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_JOURNAL,
                0,
                0,
                0,
                0,
                100
        )).thenReturn(List.of());

        yearService.getYearJournalPublicationsBatch(year, 0, 100);

        verify(yearRepository).findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_JOURNAL,
                0,
                0,
                0,
                0,
                100
        );

        clearInvocations(yearRepository);

        when(yearRepository.findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_CONFERENCE,
                0,
                0,
                0,
                0,
                100
        )).thenReturn(List.of());

        yearService.getYearConferencePublicationsBatch(year, 0, 100);

        verify(yearRepository).findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_CONFERENCE,
                0,
                0,
                0,
                0,
                100
        );

        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void loadYearPublicationsInBatches_loadsFirstBatchWithInitialSizeThenRegularBatches() {
        int year = 2020;
        int initialBatchSize = 2;
        int regularBatchSize = 3;

        YearPublicationDto publication101 = yearPublication(101);
        YearPublicationDto publication102 = yearPublication(102);
        YearPublicationDto publication205 = yearPublication(205);

        List<YearPublicationDto> firstBatch =
                List.of(publication101, publication102);

        List<YearPublicationDto> secondBatch =
                List.of(publication205);

        when(yearRepository.findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                0,
                initialBatchSize
        )).thenReturn(firstBatch);

        when(yearRepository.findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                102,
                regularBatchSize
        )).thenReturn(secondBatch);

        when(yearRepository.findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                205,
                regularBatchSize
        )).thenReturn(List.of());

        List<List<YearPublicationDto>> loadedBatches = new ArrayList<>();

        yearService.loadYearPublicationsInBatches(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                null,
                null,
                null,
                null,
                initialBatchSize,
                regularBatchSize,
                loadedBatches::add,
                () -> true
        );

        assertEquals(2, loadedBatches.size());
        assertEquals(ids(101, 102), articleIds(loadedBatches.get(0)));
        assertEquals(ids(205), articleIds(loadedBatches.get(1)));

        verify(yearRepository).findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                0,
                initialBatchSize
        );

        verify(yearRepository).findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                102,
                regularBatchSize
        );

        verify(yearRepository).findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                205,
                regularBatchSize
        );

        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void loadYearPublicationsInBatches_stopsWhenShouldContinueReturnsFalse() {
        int year = 2020;

        List<YearPublicationDto> firstBatch =
                List.of(
                        yearPublication(101),
                        yearPublication(102)
                );

        when(yearRepository.findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                0,
                2
        )).thenReturn(firstBatch);

        AtomicInteger checks = new AtomicInteger(0);
        List<List<YearPublicationDto>> loadedBatches = new ArrayList<>();

        yearService.loadYearPublicationsInBatches(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                null,
                null,
                null,
                null,
                2,
                2,
                loadedBatches::add,
                () -> checks.getAndIncrement() < 1
        );

        assertEquals(1, loadedBatches.size());
        assertEquals(ids(101, 102), articleIds(loadedBatches.get(0)));

        verify(yearRepository).findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                0,
                2
        );

        verifyNoMoreInteractions(yearRepository);
    }

    @Test
    void loadYearPublicationsInBatches_stopsWhenLastArticleIdIsNull() {
        int year = 2020;

        List<YearPublicationDto> firstBatch =
                List.of(
                        yearPublication(101),
                        yearPublication(null)
                );

        when(yearRepository.findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                0,
                2
        )).thenReturn(firstBatch);

        List<List<YearPublicationDto>> loadedBatches = new ArrayList<>();

        yearService.loadYearPublicationsInBatches(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                null,
                null,
                null,
                null,
                2,
                2,
                loadedBatches::add,
                () -> true
        );

        assertEquals(1, loadedBatches.size());
        assertEquals(idsWithNull(101, null), articleIds(loadedBatches.get(0)));

        verify(yearRepository).findYearPublicationsBatch(
                year,
                YearService.PUBLICATION_TYPE_ALL,
                0,
                0,
                0,
                0,
                2
        );

        verifyNoMoreInteractions(yearRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -50})
    void serviceMethodsThrowException_whenYearIsNotPositive(int invalidYear) {
        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.loadYearPageData(invalidYear, YearService.PUBLICATION_TYPE_ALL)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.getYearProfile(invalidYear)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.getYearPublications(
                        invalidYear,
                        YearService.PUBLICATION_TYPE_ALL,
                        null,
                        null,
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.getYearPublicationsBatch(
                        invalidYear,
                        YearService.PUBLICATION_TYPE_ALL,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.loadYearPublicationsInBatches(
                        invalidYear,
                        YearService.PUBLICATION_TYPE_ALL,
                        null,
                        null,
                        null,
                        null,
                        100,
                        100,
                        batch -> {
                        },
                        () -> true
                )
        );

        verifyNoInteractions(yearRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "INVALID",
            "ARTICLE",
            "journals",
            "conference article"
    })
    void serviceMethodsThrowException_whenPublicationTypeIsInvalid(String invalidPublicationType) {
        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.loadYearPageData(2020, invalidPublicationType)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.getYearPublications(
                        2020,
                        invalidPublicationType,
                        null,
                        null,
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.getExpectedPublicationCount(
                        yearProfile(10L, 5L, 5L),
                        invalidPublicationType
                )
        );

        verifyNoInteractions(yearRepository);
    }

    @Test
    void serviceMethodsThrowException_whenFilterCombinationIsInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.getYearPublications(
                        2020,
                        YearService.PUBLICATION_TYPE_ALL,
                        1,
                        2,
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.getYearPublications(
                        2020,
                        YearService.PUBLICATION_TYPE_CONFERENCE,
                        1,
                        null,
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.getYearPublications(
                        2020,
                        YearService.PUBLICATION_TYPE_JOURNAL,
                        null,
                        2,
                        null
                )
        );

        verifyNoInteractions(yearRepository);
    }

    @Test
    void serviceMethodsThrowException_whenFilterIdsAreNegative() {
        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.getYearPublications(
                        2020,
                        YearService.PUBLICATION_TYPE_ALL,
                        -1,
                        null,
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.getYearPublications(
                        2020,
                        YearService.PUBLICATION_TYPE_ALL,
                        null,
                        -1,
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.getYearPublications(
                        2020,
                        YearService.PUBLICATION_TYPE_ALL,
                        null,
                        null,
                        -1
                )
        );

        verifyNoInteractions(yearRepository);
    }

    @Test
    void getYearPublicationsBatchThrowsException_whenLastArticleIdIsNegative() {
        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.getYearPublicationsBatch(
                        2020,
                        YearService.PUBLICATION_TYPE_ALL,
                        null,
                        null,
                        null,
                        -1,
                        100
                )
        );

        verifyNoInteractions(yearRepository);
    }

    @Test
    void loadYearPublicationsInBatchesThrowsException_whenCallbackIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.loadYearPublicationsInBatches(
                        2020,
                        YearService.PUBLICATION_TYPE_ALL,
                        null,
                        null,
                        null,
                        null,
                        100,
                        100,
                        null,
                        () -> true
                )
        );

        verifyNoInteractions(yearRepository);
    }

    @Test
    void loadYearPublicationsInBatchesThrowsException_whenShouldContinueIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> yearService.loadYearPublicationsInBatches(
                        2020,
                        YearService.PUBLICATION_TYPE_ALL,
                        null,
                        null,
                        null,
                        null,
                        100,
                        100,
                        batch -> {
                        },
                        null
                )
        );

        verifyNoInteractions(yearRepository);
    }

    private AvailableYearDto availableYear(
            Long totalArticles,
            Long totalJournalArticles,
            Long totalConferenceArticles
    ) {
        AvailableYearDto dto = mock(AvailableYearDto.class);

        lenient().when(dto.totalArticles()).thenReturn(totalArticles);
        lenient().when(dto.totalJournalArticles()).thenReturn(totalJournalArticles);
        lenient().when(dto.totalConferenceArticles()).thenReturn(totalConferenceArticles);

        return dto;
    }

    private YearProfileDto yearProfile(
            Long totalArticles,
            Long totalJournalArticles,
            Long totalConferenceArticles
    ) {
        YearProfileDto dto = mock(YearProfileDto.class);

        lenient().when(dto.totalArticles()).thenReturn(totalArticles);
        lenient().when(dto.totalJournalArticles()).thenReturn(totalJournalArticles);
        lenient().when(dto.totalConferenceArticles()).thenReturn(totalConferenceArticles);

        return dto;
    }

    private YearPublicationDto yearPublication(Integer articleId) {
        YearPublicationDto dto = mock(YearPublicationDto.class);

        lenient().when(dto.articleId()).thenReturn(articleId);

        return dto;
    }

    private List<Integer> articleIds(List<YearPublicationDto> publications) {
        return publications.stream()
                .map(YearPublicationDto::articleId)
                .toList();
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