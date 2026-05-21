package service.charts;

import dto.chart.CategoryOptionDto;
import dto.chart.CategoryTrendDto;
import service.ConferenceService;
import service.JournalService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class VenueCategoriesService {

    public static final String ANALYSIS_CONFERENCE_PRIMARY_FOR = "Conference PrimaryFoR";
    public static final String ANALYSIS_JOURNAL_BEST_SUBJECT_AREA = "Journal BestSubjectArea";

    public static final int DEFAULT_MIN_YEAR = 1900;
    public static final int MAX_VISIBLE_CATEGORIES = 12;

    private static final String[] ANALYSIS_TYPES = {
            ANALYSIS_CONFERENCE_PRIMARY_FOR,
            ANALYSIS_JOURNAL_BEST_SUBJECT_AREA
    };

    private final JournalService journalService = new JournalService();
    private final ConferenceService conferenceService = new ConferenceService();

    public String[] getAnalysisTypes() {
        return ANALYSIS_TYPES;
    }

    public int getCurrentYear() {
        return LocalDate.now().getYear();
    }

    public List<Integer> buildYearList(Integer minimumYear) {
        int currentYear = getCurrentYear();
        int min = minimumYear == null ? DEFAULT_MIN_YEAR : Math.max(DEFAULT_MIN_YEAR, minimumYear);

        List<Integer> years = new ArrayList<>();

        for (int year = currentYear; year >= min; year--) {
            years.add(year);
        }

        return years;
    }

    public YearRange validateYearRange(Integer startYear, Integer endYear) {
        if (startYear != null && endYear != null && endYear < startYear) {
            throw new IllegalArgumentException("To year must be greater than or equal to From year.");
        }

        return new YearRange(startYear, endYear);
    }

    public void validateAnalysisType(String analysisType) {
        if (analysisType == null || analysisType.isBlank()) {
            throw new IllegalArgumentException("You must select Conference PrimaryFoR or Journal BestSubjectArea.");
        }
    }

    public List<CategoryOptionDto> loadCategoryOptions(String analysisType) {
        List<CategoryOptionDto> loadedCategories;

        if (ANALYSIS_CONFERENCE_PRIMARY_FOR.equals(analysisType)) {
            loadedCategories = conferenceService.getPrimaryFoRCategories();
        } else if (ANALYSIS_JOURNAL_BEST_SUBJECT_AREA.equals(analysisType)) {
            loadedCategories = journalService.getBestSubjectAreas();
        } else {
            loadedCategories = new ArrayList<>();
        }

        return buildCategoryOptionsWithAll(loadedCategories);
    }

    public List<CategoryOptionDto> buildDefaultCategoryOptions() {
        List<CategoryOptionDto> options = new ArrayList<>();
        options.add(new CategoryOptionDto("", "All categories"));
        return options;
    }

    private List<CategoryOptionDto> buildCategoryOptionsWithAll(List<CategoryOptionDto> loadedCategories) {
        List<CategoryOptionDto> options = buildDefaultCategoryOptions();

        if (loadedCategories != null) {
            options.addAll(loadedCategories);
        }

        return options;
    }

    public String getCategoryPromptText(String analysisType) {
        if (ANALYSIS_CONFERENCE_PRIMARY_FOR.equals(analysisType)) {
            return "Select PrimaryFoR category";
        }

        return "Select BestSubjectArea category";
    }

    public String formatCategoryOptionForUi(CategoryOptionDto option, String analysisType) {
        if (option == null) {
            return "";
        }

        if (ANALYSIS_JOURNAL_BEST_SUBJECT_AREA.equals(analysisType)) {
            return option.name();
        }

        return option.displayText();
    }

    public String getSelectedCategoryFilter(CategoryOptionDto selectedOption) {
        if (selectedOption == null || selectedOption.id() == null || selectedOption.id().isBlank()) {
            return "";
        }

        return selectedOption.id();
    }

    public List<CategoryTrendSeries> loadCategoryTrendData(
            String analysisType,
            String categoryFilter,
            Integer fromYear,
            Integer toYear
    ) {
        List<CategoryTrendDto> rows;

        if (ANALYSIS_CONFERENCE_PRIMARY_FOR.equals(analysisType)) {
            rows = conferenceService.getConferencePrimaryFoRYearlyTrends(
                    categoryFilter,
                    fromYear,
                    toYear
            );
        } else if (ANALYSIS_JOURNAL_BEST_SUBJECT_AREA.equals(analysisType)) {
            rows = journalService.getJournalBestSubjectAreaYearlyTrends(
                    categoryFilter,
                    fromYear,
                    toYear
            );
        } else {
            rows = new ArrayList<>();
        }

        return convertTrendDtosToSeries(rows);
    }

    private List<CategoryTrendSeries> convertTrendDtosToSeries(List<CategoryTrendDto> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Map<Integer, Long>> grouped = new TreeMap<>();

        for (CategoryTrendDto row : rows) {
            if (row == null || row.category() == null || row.category().isBlank()) {
                continue;
            }

            grouped
                    .computeIfAbsent(row.category(), key -> new TreeMap<>())
                    .merge(row.year(), row.count(), Long::sum);
        }

        List<CategoryTrendSeries> series = new ArrayList<>();

        for (Map.Entry<String, Map<Integer, Long>> entry : grouped.entrySet()) {
            List<CategoryTrendPoint> points = new ArrayList<>();
            long totalCount = 0;

            for (Map.Entry<Integer, Long> pointEntry : entry.getValue().entrySet()) {
                long count = pointEntry.getValue() == null ? 0 : pointEntry.getValue();

                points.add(
                        new CategoryTrendPoint(
                                entry.getKey(),
                                pointEntry.getKey(),
                                count
                        )
                );

                totalCount += count;
            }

            if (!points.isEmpty()) {
                series.add(
                        new CategoryTrendSeries(
                                entry.getKey(),
                                points,
                                totalCount
                        )
                );
            }
        }

        series.sort(
                Comparator.comparingLong(CategoryTrendSeries::totalCount)
                        .reversed()
                        .thenComparing(CategoryTrendSeries::category)
        );

        if (series.size() > MAX_VISIBLE_CATEGORIES) {
            return new ArrayList<>(series.subList(0, MAX_VISIBLE_CATEGORIES));
        }

        return series;
    }

    public String getChartTitle(String analysisType) {
        if (ANALYSIS_CONFERENCE_PRIMARY_FOR.equals(analysisType)) {
            return "Conference categories by PrimaryFoR over years";
        }

        return "Journal categories by BestSubjectArea over years";
    }

    public String getCategoryKey(CategoryTrendSeries categorySeries) {
        if (categorySeries == null || categorySeries.category() == null) {
            return "";
        }

        return categorySeries.category();
    }

    public record CategoryTrendPoint(
            String category,
            int year,
            long count
    ) {
    }

    public record CategoryTrendSeries(
            String category,
            List<CategoryTrendPoint> points,
            long totalCount
    ) {
    }

    public record YearRange(
            Integer startYear,
            Integer endYear
    ) {
    }
}