package service.charts;

import dto.chart.CategoryOptionDto;
import dto.chart.CategoryTrendDto;
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
class VenueCategoriesServiceTest {

    @Mock
    private JournalService journalService;

    @Mock
    private ConferenceService conferenceService;

    private VenueCategoriesService venueCategoriesService;

    @BeforeEach
    void setUp() throws Exception {
        venueCategoriesService = new VenueCategoriesService();
        injectServiceMocks();
    }

    @Test
    void getAnalysisTypes_returnsExpectedAnalysisTypes() {
        String[] result = venueCategoriesService.getAnalysisTypes();

        assertArrayEquals(
                new String[]{
                        "Conference PrimaryFoR",
                        "Journal BestSubjectArea"
                },
                result
        );
    }

    @Test
    void getCurrentYear_returnsSystemCurrentYear() {
        int result = venueCategoriesService.getCurrentYear();

        assertEquals(LocalDate.now().getYear(), result);
    }

    @Test
    void buildYearList_usesDefaultMinimumYear_whenMinimumYearIsNull() {
        int currentYear = LocalDate.now().getYear();

        List<Integer> result = venueCategoriesService.buildYearList(null);

        assertFalse(result.isEmpty());
        assertEquals(currentYear, result.get(0));
        assertEquals(VenueCategoriesService.DEFAULT_MIN_YEAR, result.get(result.size() - 1));
    }

    @Test
    void buildYearList_usesGivenMinimumYear_whenMinimumYearIsGreaterThanDefault() {
        int currentYear = LocalDate.now().getYear();
        int minimumYear = currentYear - 3;

        List<Integer> result = venueCategoriesService.buildYearList(minimumYear);

        assertEquals(4, result.size());
        assertEquals(currentYear, result.get(0));
        assertEquals(currentYear - 1, result.get(1));
        assertEquals(currentYear - 2, result.get(2));
        assertEquals(minimumYear, result.get(3));
    }

    @Test
    void buildYearList_doesNotGoBelowDefaultMinimumYear() {
        int currentYear = LocalDate.now().getYear();

        List<Integer> result = venueCategoriesService.buildYearList(1800);

        assertFalse(result.isEmpty());
        assertEquals(currentYear, result.get(0));
        assertEquals(VenueCategoriesService.DEFAULT_MIN_YEAR, result.get(result.size() - 1));
    }

    @Test
    void validateYearRange_returnsExpectedRange_whenRangeIsValid() {
        VenueCategoriesService.YearRange result =
                venueCategoriesService.validateYearRange(2010, 2020);

        assertEquals(2010, result.startYear());
        assertEquals(2020, result.endYear());
    }

    @Test
    void validateYearRange_allowsNullYears() {
        VenueCategoriesService.YearRange result =
                venueCategoriesService.validateYearRange(null, null);

        assertNull(result.startYear());
        assertNull(result.endYear());
    }

    @Test
    void validateYearRange_throwsException_whenEndYearIsBeforeStartYear() {
        assertThrows(
                IllegalArgumentException.class,
                () -> venueCategoriesService.validateYearRange(2025, 2020)
        );
    }

    @Test
    void validateAnalysisType_doesNotThrow_whenAnalysisTypeIsValidText() {
        assertDoesNotThrow(
                () -> venueCategoriesService.validateAnalysisType(
                        VenueCategoriesService.ANALYSIS_CONFERENCE_PRIMARY_FOR
                )
        );

        assertDoesNotThrow(
                () -> venueCategoriesService.validateAnalysisType(
                        VenueCategoriesService.ANALYSIS_JOURNAL_BEST_SUBJECT_AREA
                )
        );
    }

    @Test
    void validateAnalysisType_throwsException_whenAnalysisTypeIsNullOrBlank() {
        assertThrows(
                IllegalArgumentException.class,
                () -> venueCategoriesService.validateAnalysisType(null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> venueCategoriesService.validateAnalysisType("   ")
        );
    }

    @Test
    void buildDefaultCategoryOptions_returnsOnlyAllCategoriesOption() {
        List<CategoryOptionDto> result =
                venueCategoriesService.buildDefaultCategoryOptions();

        assertEquals(1, result.size());
        assertEquals("", result.get(0).id());
        assertEquals("All categories", result.get(0).displayText());
    }

    @Test
    void loadCategoryOptions_loadsConferenceCategoriesAndAddsAllCategoriesOption() {
        CategoryOptionDto category1 =
                categoryOption("AI", "Artificial Intelligence", "Artificial Intelligence");

        CategoryOptionDto category2 =
                categoryOption("DB", "Databases", "Databases");

        when(conferenceService.getPrimaryFoRCategories())
                .thenReturn(List.of(category1, category2));

        List<CategoryOptionDto> result =
                venueCategoriesService.loadCategoryOptions(
                        VenueCategoriesService.ANALYSIS_CONFERENCE_PRIMARY_FOR
                );

        assertEquals(3, result.size());
        assertEquals("", result.get(0).id());
        assertEquals("All categories", result.get(0).displayText());
        assertSame(category1, result.get(1));
        assertSame(category2, result.get(2));

        verify(conferenceService).getPrimaryFoRCategories();
        verifyNoInteractions(journalService);
        verifyNoMoreInteractions(conferenceService);
    }

    @Test
    void loadCategoryOptions_loadsJournalCategoriesAndAddsAllCategoriesOption() {
        CategoryOptionDto category1 =
                categoryOption("AI", "Artificial Intelligence", "Artificial Intelligence");

        CategoryOptionDto category2 =
                categoryOption("DB", "Databases", "Databases");

        when(journalService.getBestSubjectAreas())
                .thenReturn(List.of(category1, category2));

        List<CategoryOptionDto> result =
                venueCategoriesService.loadCategoryOptions(
                        VenueCategoriesService.ANALYSIS_JOURNAL_BEST_SUBJECT_AREA
                );

        assertEquals(3, result.size());
        assertEquals("", result.get(0).id());
        assertEquals("All categories", result.get(0).displayText());
        assertSame(category1, result.get(1));
        assertSame(category2, result.get(2));

        verify(journalService).getBestSubjectAreas();
        verifyNoInteractions(conferenceService);
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void loadCategoryOptions_returnsOnlyDefaultOption_whenAnalysisTypeIsUnknown() {
        List<CategoryOptionDto> result =
                venueCategoriesService.loadCategoryOptions("Unknown analysis");

        assertEquals(1, result.size());
        assertEquals("", result.get(0).id());
        assertEquals("All categories", result.get(0).displayText());

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void loadCategoryOptions_handlesNullLoadedCategories() {
        when(conferenceService.getPrimaryFoRCategories())
                .thenReturn(null);

        List<CategoryOptionDto> result =
                venueCategoriesService.loadCategoryOptions(
                        VenueCategoriesService.ANALYSIS_CONFERENCE_PRIMARY_FOR
                );

        assertEquals(1, result.size());
        assertEquals("", result.get(0).id());
        assertEquals("All categories", result.get(0).displayText());

        verify(conferenceService).getPrimaryFoRCategories();
        verifyNoInteractions(journalService);
        verifyNoMoreInteractions(conferenceService);
    }

    @Test
    void getCategoryPromptText_returnsConferencePromptForConferenceAnalysis() {
        String result =
                venueCategoriesService.getCategoryPromptText(
                        VenueCategoriesService.ANALYSIS_CONFERENCE_PRIMARY_FOR
                );

        assertEquals("Select PrimaryFoR category", result);
    }

    @Test
    void getCategoryPromptText_returnsJournalPromptForJournalOrUnknownAnalysis() {
        assertEquals(
                "Select BestSubjectArea category",
                venueCategoriesService.getCategoryPromptText(
                        VenueCategoriesService.ANALYSIS_JOURNAL_BEST_SUBJECT_AREA
                )
        );

        assertEquals(
                "Select BestSubjectArea category",
                venueCategoriesService.getCategoryPromptText("Unknown")
        );
    }

    @Test
    void formatCategoryOptionForUi_returnsEmptyString_whenOptionIsNull() {
        String result =
                venueCategoriesService.formatCategoryOptionForUi(
                        null,
                        VenueCategoriesService.ANALYSIS_CONFERENCE_PRIMARY_FOR
                );

        assertEquals("", result);
    }

    @Test
    void formatCategoryOptionForUi_returnsName_whenAnalysisTypeIsJournalBestSubjectArea() {
        CategoryOptionDto option =
                categoryOption("AI", "Artificial Intelligence", "AI display");

        String result =
                venueCategoriesService.formatCategoryOptionForUi(
                        option,
                        VenueCategoriesService.ANALYSIS_JOURNAL_BEST_SUBJECT_AREA
                );

        assertEquals("Artificial Intelligence", result);
    }

    @Test
    void formatCategoryOptionForUi_returnsDisplayText_whenAnalysisTypeIsConferencePrimaryFor() {
        CategoryOptionDto option =
                categoryOption("AI", "Artificial Intelligence", "AI display");

        String result =
                venueCategoriesService.formatCategoryOptionForUi(
                        option,
                        VenueCategoriesService.ANALYSIS_CONFERENCE_PRIMARY_FOR
                );

        assertEquals("AI display", result);
    }

    @Test
    void getSelectedCategoryFilter_returnsEmptyString_whenSelectedOptionIsNullOrIdIsBlank() {
        assertEquals(
                "",
                venueCategoriesService.getSelectedCategoryFilter(null)
        );

        assertEquals(
                "",
                venueCategoriesService.getSelectedCategoryFilter(
                        categoryOption(null, "Name", "Display")
                )
        );

        assertEquals(
                "",
                venueCategoriesService.getSelectedCategoryFilter(
                        categoryOption("   ", "Name", "Display")
                )
        );
    }

    @Test
    void getSelectedCategoryFilter_returnsSelectedOptionId() {
        CategoryOptionDto option =
                categoryOption("AI", "Artificial Intelligence", "AI display");

        String result =
                venueCategoriesService.getSelectedCategoryFilter(option);

        assertEquals("AI", result);
    }

    @Test
    void loadCategoryTrendData_loadsConferenceTrendRowsAndConvertsToSeries() {
        CategoryTrendDto ai2020 =
                categoryTrend("AI", 2020, 10L);

        CategoryTrendDto ai2021 =
                categoryTrend("AI", 2021, 15L);

        CategoryTrendDto db2020 =
                categoryTrend("DB", 2020, 20L);

        when(conferenceService.getConferencePrimaryFoRYearlyTrends("AI", 2010, 2020))
                .thenReturn(List.of(ai2020, ai2021, db2020));

        List<VenueCategoriesService.CategoryTrendSeries> result =
                venueCategoriesService.loadCategoryTrendData(
                        VenueCategoriesService.ANALYSIS_CONFERENCE_PRIMARY_FOR,
                        "AI",
                        2010,
                        2020
                );

        assertEquals(2, result.size());

        VenueCategoriesService.CategoryTrendSeries firstSeries = result.get(0);
        assertEquals("AI", firstSeries.category());
        assertEquals(25L, firstSeries.totalCount());
        assertEquals(2, firstSeries.points().size());
        assertEquals(2020, firstSeries.points().get(0).year());
        assertEquals(10L, firstSeries.points().get(0).count());
        assertEquals(2021, firstSeries.points().get(1).year());
        assertEquals(15L, firstSeries.points().get(1).count());

        VenueCategoriesService.CategoryTrendSeries secondSeries = result.get(1);
        assertEquals("DB", secondSeries.category());
        assertEquals(20L, secondSeries.totalCount());
        assertEquals(1, secondSeries.points().size());

        verify(conferenceService).getConferencePrimaryFoRYearlyTrends("AI", 2010, 2020);
        verifyNoInteractions(journalService);
        verifyNoMoreInteractions(conferenceService);
    }

    @Test
    void loadCategoryTrendData_loadsJournalTrendRowsAndConvertsToSeries() {
        CategoryTrendDto biology2020 =
                categoryTrend("Biology", 2020, 12L);

        when(journalService.getJournalBestSubjectAreaYearlyTrends("Biology", 2015, 2022))
                .thenReturn(List.of(biology2020));

        List<VenueCategoriesService.CategoryTrendSeries> result =
                venueCategoriesService.loadCategoryTrendData(
                        VenueCategoriesService.ANALYSIS_JOURNAL_BEST_SUBJECT_AREA,
                        "Biology",
                        2015,
                        2022
                );

        assertEquals(1, result.size());
        assertEquals("Biology", result.get(0).category());
        assertEquals(12L, result.get(0).totalCount());
        assertEquals(1, result.get(0).points().size());

        verify(journalService).getJournalBestSubjectAreaYearlyTrends("Biology", 2015, 2022);
        verifyNoInteractions(conferenceService);
        verifyNoMoreInteractions(journalService);
    }

    @Test
    void loadCategoryTrendData_returnsEmptyList_whenAnalysisTypeIsUnknown() {
        List<VenueCategoriesService.CategoryTrendSeries> result =
                venueCategoriesService.loadCategoryTrendData(
                        "Unknown",
                        "",
                        2010,
                        2020
                );

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(journalService);
        verifyNoInteractions(conferenceService);
    }

    @Test
    void loadCategoryTrendData_ignoresNullRowsAndRowsWithBlankCategory() {
        CategoryTrendDto validRow =
                categoryTrend("AI", 2020, 10L);

        CategoryTrendDto blankCategoryRow =
                categoryTrend("   ", 2021, 20L);

        List<CategoryTrendDto> rows = new ArrayList<>();
        rows.add(null);
        rows.add(blankCategoryRow);
        rows.add(validRow);

        when(conferenceService.getConferencePrimaryFoRYearlyTrends("", 2010, 2020))
                .thenReturn(rows);

        List<VenueCategoriesService.CategoryTrendSeries> result =
                venueCategoriesService.loadCategoryTrendData(
                        VenueCategoriesService.ANALYSIS_CONFERENCE_PRIMARY_FOR,
                        "",
                        2010,
                        2020
                );

        assertEquals(1, result.size());
        assertEquals("AI", result.get(0).category());
        assertEquals(10L, result.get(0).totalCount());

        verify(conferenceService).getConferencePrimaryFoRYearlyTrends("", 2010, 2020);
        verifyNoInteractions(journalService);
        verifyNoMoreInteractions(conferenceService);
    }

    @Test
    void loadCategoryTrendData_mergesCountsForSameCategoryAndYear() {
        CategoryTrendDto ai2020First =
                categoryTrend("AI", 2020, 10L);

        CategoryTrendDto ai2020Second =
                categoryTrend("AI", 2020, 5L);

        when(conferenceService.getConferencePrimaryFoRYearlyTrends("", 2010, 2020))
                .thenReturn(List.of(ai2020First, ai2020Second));

        List<VenueCategoriesService.CategoryTrendSeries> result =
                venueCategoriesService.loadCategoryTrendData(
                        VenueCategoriesService.ANALYSIS_CONFERENCE_PRIMARY_FOR,
                        "",
                        2010,
                        2020
                );

        assertEquals(1, result.size());
        assertEquals("AI", result.get(0).category());
        assertEquals(15L, result.get(0).totalCount());
        assertEquals(1, result.get(0).points().size());
        assertEquals(2020, result.get(0).points().get(0).year());
        assertEquals(15L, result.get(0).points().get(0).count());

        verify(conferenceService).getConferencePrimaryFoRYearlyTrends("", 2010, 2020);
        verifyNoInteractions(journalService);
        verifyNoMoreInteractions(conferenceService);
    }

    @Test
    void loadCategoryTrendData_sortsSeriesByTotalCountDescendingThenCategoryName() {
        CategoryTrendDto beta =
                categoryTrend("Beta", 2020, 10L);

        CategoryTrendDto alpha =
                categoryTrend("Alpha", 2020, 10L);

        CategoryTrendDto gamma =
                categoryTrend("Gamma", 2020, 30L);

        when(conferenceService.getConferencePrimaryFoRYearlyTrends("", 2010, 2020))
                .thenReturn(List.of(beta, alpha, gamma));

        List<VenueCategoriesService.CategoryTrendSeries> result =
                venueCategoriesService.loadCategoryTrendData(
                        VenueCategoriesService.ANALYSIS_CONFERENCE_PRIMARY_FOR,
                        "",
                        2010,
                        2020
                );

        assertEquals(3, result.size());
        assertEquals("Gamma", result.get(0).category());
        assertEquals("Alpha", result.get(1).category());
        assertEquals("Beta", result.get(2).category());

        verify(conferenceService).getConferencePrimaryFoRYearlyTrends("", 2010, 2020);
        verifyNoInteractions(journalService);
        verifyNoMoreInteractions(conferenceService);
    }

    @Test
    void loadCategoryTrendData_limitsResultsToMaxVisibleCategories() {
        List<CategoryTrendDto> rows = new ArrayList<>();

        for (int index = 1; index <= 15; index++) {
            rows.add(categoryTrend("Category " + index, 2020, index));
        }

        when(conferenceService.getConferencePrimaryFoRYearlyTrends("", 2010, 2020))
                .thenReturn(rows);

        List<VenueCategoriesService.CategoryTrendSeries> result =
                venueCategoriesService.loadCategoryTrendData(
                        VenueCategoriesService.ANALYSIS_CONFERENCE_PRIMARY_FOR,
                        "",
                        2010,
                        2020
                );

        assertEquals(VenueCategoriesService.MAX_VISIBLE_CATEGORIES, result.size());
        assertEquals("Category 15", result.get(0).category());
        assertEquals("Category 4", result.get(result.size() - 1).category());

        verify(conferenceService).getConferencePrimaryFoRYearlyTrends("", 2010, 2020);
        verifyNoInteractions(journalService);
        verifyNoMoreInteractions(conferenceService);
    }

    @Test
    void getChartTitle_returnsConferenceTitleForConferenceAnalysis() {
        String result =
                venueCategoriesService.getChartTitle(
                        VenueCategoriesService.ANALYSIS_CONFERENCE_PRIMARY_FOR
                );

        assertEquals("Conference categories by PrimaryFoR over years", result);
    }

    @Test
    void getChartTitle_returnsJournalTitleForJournalOrUnknownAnalysis() {
        assertEquals(
                "Journal categories by BestSubjectArea over years",
                venueCategoriesService.getChartTitle(
                        VenueCategoriesService.ANALYSIS_JOURNAL_BEST_SUBJECT_AREA
                )
        );

        assertEquals(
                "Journal categories by BestSubjectArea over years",
                venueCategoriesService.getChartTitle("Unknown")
        );
    }

    @Test
    void getCategoryKey_returnsEmptyString_whenSeriesIsNullOrCategoryIsNull() {
        assertEquals("", venueCategoriesService.getCategoryKey(null));

        VenueCategoriesService.CategoryTrendSeries seriesWithNullCategory =
                new VenueCategoriesService.CategoryTrendSeries(
                        null,
                        List.of(),
                        0L
                );

        assertEquals("", venueCategoriesService.getCategoryKey(seriesWithNullCategory));
    }

    @Test
    void getCategoryKey_returnsCategory() {
        VenueCategoriesService.CategoryTrendSeries series =
                new VenueCategoriesService.CategoryTrendSeries(
                        "AI",
                        List.of(),
                        10L
                );

        assertEquals("AI", venueCategoriesService.getCategoryKey(series));
    }

    private void injectServiceMocks() throws Exception {
        Field journalField = VenueCategoriesService.class.getDeclaredField("journalService");
        journalField.setAccessible(true);
        journalField.set(venueCategoriesService, journalService);

        Field conferenceField = VenueCategoriesService.class.getDeclaredField("conferenceService");
        conferenceField.setAccessible(true);
        conferenceField.set(venueCategoriesService, conferenceService);
    }

    private CategoryOptionDto categoryOption(
            String id,
            String name,
            String displayText
    ) {
        CategoryOptionDto dto = mock(CategoryOptionDto.class);

        lenient().when(dto.id()).thenReturn(id);
        lenient().when(dto.name()).thenReturn(name);
        lenient().when(dto.displayText()).thenReturn(displayText);

        return dto;
    }

    private CategoryTrendDto categoryTrend(
            String category,
            int year,
            long count
    ) {
        CategoryTrendDto dto = mock(CategoryTrendDto.class);

        lenient().when(dto.category()).thenReturn(category);
        lenient().when(dto.year()).thenReturn(year);
        lenient().when(dto.count()).thenReturn(count);

        return dto;
    }
}