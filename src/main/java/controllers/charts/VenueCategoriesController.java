package controllers.charts;

import dto.chart.CategoryOptionDto;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import service.ConferenceService;
import service.JournalService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class VenueCategoriesController {

    private static final String ANALYSIS_CONFERENCE_PRIMARY_FOR = "Conference PrimaryFoR";
    private static final String ANALYSIS_JOURNAL_BEST_SUBJECT_AREA = "Journal BestSubjectArea";

    private static final int MIN_YEAR = 1900;
    private static final int MAX_VISIBLE_CATEGORIES = 12;

    private static final String[] CHART_COLORS = {
            "#1f5fa8",
            "#f5a623",
            "#4caf50",
            "#36a9c9",
            "#4666d8",
            "#9c46d4",
            "#d13f64",
            "#8c8c8c",
            "#ff9800",
            "#2e7d32",
            "#00695c",
            "#ad1457"
    };

    private final JournalService journalService = new JournalService();
    private final ConferenceService conferenceService = new ConferenceService();

    private Task<List<CategoryOptionDto>> categoryOptionsTask;
    private String activeCategoryKey;

    private final List<ChartHighlightHandle> chartHighlightHandles = new ArrayList<>();
    private final List<LegendHighlightHandle> legendHighlightHandles = new ArrayList<>();

    @FXML
    private ComboBox<String> categoryTypeComboBox;

    @FXML
    private ComboBox<CategoryOptionDto> categoryFilterComboBox;

    @FXML
    private ComboBox<Integer> categoryFromYearComboBox;

    @FXML
    private ComboBox<Integer> categoryToYearComboBox;

    @FXML
    private Button loadCategoryTrendButton;

    @FXML
    private Button clearCategoryTrendButton;

    @FXML
    private Label categoryStatusLabel;

    @FXML
    private LineChart<Number, Number> categoryTrendLineChart;

    @FXML
    private NumberAxis categoryYearAxis;

    @FXML
    private NumberAxis categoryCountAxis;

    @FXML
    private VBox categoryLegendBox;

    @FXML
    private FlowPane categoryLegendFlow;

    @FXML
    public void initialize() {
        setupCategoryTypeComboBox();
        setupCategoryFilterComboBox();
        setupYearComboBoxes();
        setupChart();

        clearCategoryTrends();
        setDefaultCategoryOptions();
        setStatus("Open this tab to load category filters.");
    }

    public void loadCategoryOptionsInBackground() {
        refreshCategoryFilterOptions();
    }

    @FXML
    private void loadCategoryTrends() {
        String analysisType = categoryTypeComboBox == null ? null : categoryTypeComboBox.getValue();

        if (analysisType == null || analysisType.isBlank()) {
            showError("Δεν επιλέχθηκε τύπος", "Πρέπει να επιλέξεις Conference PrimaryFoR ή Journal BestSubjectArea.");
            return;
        }

        YearRange yearRange;

        try {
            yearRange = getSelectedYearRange();
        } catch (IllegalArgumentException exception) {
            showError("Λάθος χρονιές", exception.getMessage());
            return;
        }

        String categoryFilter = getSelectedCategoryFilter();
        String categoryLabel = getSelectedCategoryLabel();

        Task<List<CategoryTrendSeries>> task = new Task<>() {
            @Override
            protected List<CategoryTrendSeries> call() {
                return loadCategoryTrendData(
                        analysisType,
                        categoryFilter,
                        categoryLabel,
                        yearRange.startYear(),
                        yearRange.endYear()
                );
            }
        };

        setLoading(true);
        setStatus("Loading category trends...");

        task.setOnSucceeded(event -> {
            setLoading(false);

            List<CategoryTrendSeries> data = task.getValue();

            if (data == null || data.isEmpty()) {
                clearChartOnly();
                setStatus("No category trend data found. Check if the trend query has been connected.");
                showInfo(
                        "Δεν βρέθηκαν δεδομένα",
                        "Οι κατηγορίες φορτώνονται, αλλά χρειάζεται να συνδέσουμε και τα yearly trend queries."
                );
                return;
            }

            updateCategoryTrendChart(data, analysisType);
            setStatus("Loaded " + data.size() + " category series.");
        });

        task.setOnFailed(event -> {
            setLoading(false);
            Throwable exception = task.getException();
            setStatus("Failed to load category trends.");
            showError("Σφάλμα φόρτωσης", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(task, "venue-categories-load-task");
    }

    @FXML
    private void clearCategoryTrends() {
        if (categoryFilterComboBox != null && !categoryFilterComboBox.getItems().isEmpty()) {
            categoryFilterComboBox.getSelectionModel().selectFirst();
        }

        if (categoryFromYearComboBox != null) {
            categoryFromYearComboBox.getSelectionModel().clearSelection();
        }

        if (categoryToYearComboBox != null) {
            categoryToYearComboBox.getSelectionModel().clearSelection();
            refreshToYearOptions(null);
        }

        clearChartOnly();
        setStatus("Choose category type and load yearly trends.");
    }

    private void setupCategoryTypeComboBox() {
        if (categoryTypeComboBox == null) {
            return;
        }

        categoryTypeComboBox.getItems().setAll(
                ANALYSIS_CONFERENCE_PRIMARY_FOR,
                ANALYSIS_JOURNAL_BEST_SUBJECT_AREA
        );

        categoryTypeComboBox.getSelectionModel().select(ANALYSIS_CONFERENCE_PRIMARY_FOR);

        categoryTypeComboBox.setOnAction(event -> {
            clearChartOnly();
            refreshCategoryFilterOptions();
        });
    }

    private void setupCategoryFilterComboBox() {
        if (categoryFilterComboBox == null) {
            return;
        }

        categoryFilterComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(CategoryOptionDto item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatCategoryOptionForUi(item));
                }
            }
        });

        categoryFilterComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(CategoryOptionDto item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatCategoryOptionForUi(item));
                }
            }
        });

        setDefaultCategoryOptions();
    }

    private void refreshCategoryFilterOptions() {
        if (categoryFilterComboBox == null) {
            return;
        }

        if (categoryOptionsTask != null && categoryOptionsTask.isRunning()) {
            categoryOptionsTask.cancel();
        }

        String selectedType = categoryTypeComboBox == null ? null : categoryTypeComboBox.getValue();

        setDefaultCategoryOptions();
        categoryFilterComboBox.setDisable(true);
        setStatus("Loading categories...");

        categoryOptionsTask = new Task<>() {
            @Override
            protected List<CategoryOptionDto> call() {
                if (ANALYSIS_CONFERENCE_PRIMARY_FOR.equals(selectedType)) {
                    return conferenceService.getPrimaryFoRCategories();
                }

                return journalService.getBestSubjectAreas();
            }
        };

        categoryOptionsTask.setOnSucceeded(event -> {
            if (categoryOptionsTask.isCancelled()) {
                return;
            }

            String currentSelectedType = categoryTypeComboBox == null ? null : categoryTypeComboBox.getValue();

            if (selectedType != null && !selectedType.equals(currentSelectedType)) {
                return;
            }

            categoryFilterComboBox.setDisable(false);

            List<CategoryOptionDto> loadedCategories = categoryOptionsTask.getValue();

            List<CategoryOptionDto> options = new ArrayList<>();
            options.add(new CategoryOptionDto("", "All categories"));

            if (loadedCategories != null) {
                options.addAll(loadedCategories);
            }

            categoryFilterComboBox.getItems().setAll(options);
            categoryFilterComboBox.getSelectionModel().selectFirst();

            if (ANALYSIS_CONFERENCE_PRIMARY_FOR.equals(selectedType)) {
                categoryFilterComboBox.setPromptText("Select PrimaryFoR category");
            } else {
                categoryFilterComboBox.setPromptText("Select BestSubjectArea category");
            }

            setStatus("Loaded " + Math.max(0, options.size() - 1) + " categories.");
        });

        categoryOptionsTask.setOnFailed(event -> {
            categoryFilterComboBox.setDisable(false);
            setDefaultCategoryOptions();

            Throwable exception = categoryOptionsTask.getException();

            if (exception != null) {
                exception.printStackTrace();
            }

            setStatus("Could not load categories from database.");
        });

        startBackgroundTask(categoryOptionsTask, "venue-categories-options-task");
    }

    private void setDefaultCategoryOptions() {
        if (categoryFilterComboBox == null) {
            return;
        }

        categoryFilterComboBox.getItems().setAll(
                new CategoryOptionDto("", "All categories")
        );

        categoryFilterComboBox.getSelectionModel().selectFirst();

        String selectedType = categoryTypeComboBox == null ? null : categoryTypeComboBox.getValue();

        if (ANALYSIS_CONFERENCE_PRIMARY_FOR.equals(selectedType)) {
            categoryFilterComboBox.setPromptText("Select PrimaryFoR category");
        } else {
            categoryFilterComboBox.setPromptText("Select BestSubjectArea category");
        }
    }

    private String formatCategoryOptionForUi(CategoryOptionDto option) {
        if (option == null) {
            return "";
        }

        String selectedType = categoryTypeComboBox == null ? null : categoryTypeComboBox.getValue();

        if (ANALYSIS_JOURNAL_BEST_SUBJECT_AREA.equals(selectedType)) {
            return option.name();
        }

        return option.displayText();
    }

    private String getSelectedCategoryFilter() {
        if (categoryFilterComboBox == null) {
            return "";
        }

        CategoryOptionDto selected = categoryFilterComboBox.getValue();

        if (selected == null || selected.id() == null || selected.id().isBlank()) {
            return "";
        }

        return selected.id();
    }

    private String getSelectedCategoryLabel() {
        if (categoryFilterComboBox == null) {
            return "";
        }

        CategoryOptionDto selected = categoryFilterComboBox.getValue();

        if (selected == null || selected.name() == null || selected.name().isBlank()) {
            return "";
        }

        return selected.name();
    }

    private void setupYearComboBoxes() {
        setupYearComboBox(categoryFromYearComboBox);
        setupYearComboBox(categoryToYearComboBox);

        if (categoryFromYearComboBox != null) {
            categoryFromYearComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                    refreshToYearOptions(newValue)
            );
        }
    }

    private void setupYearComboBox(ComboBox<Integer> comboBox) {
        if (comboBox == null) {
            return;
        }

        comboBox.setEditable(false);
        comboBox.getItems().setAll(buildYearList(MIN_YEAR));
    }

    private List<Integer> buildYearList(int minimumYear) {
        int currentYear = LocalDate.now().getYear();
        int min = Math.max(MIN_YEAR, minimumYear);

        List<Integer> years = new ArrayList<>();

        for (int year = currentYear; year >= min; year--) {
            years.add(Integer.valueOf(year));
        }

        return years;
    }

    private void refreshToYearOptions(Integer fromYear) {
        if (categoryToYearComboBox == null) {
            return;
        }

        Integer currentToYear = categoryToYearComboBox.getValue();
        int minimumYear = fromYear == null ? MIN_YEAR : fromYear.intValue();

        categoryToYearComboBox.getItems().setAll(buildYearList(minimumYear));

        if (currentToYear != null && fromYear != null && currentToYear < fromYear) {
            categoryToYearComboBox.getSelectionModel().clearSelection();
            return;
        }

        if (currentToYear != null && categoryToYearComboBox.getItems().contains(currentToYear)) {
            categoryToYearComboBox.getSelectionModel().select(currentToYear);
        }
    }

    private void setupChart() {
        if (categoryTrendLineChart != null) {
            categoryTrendLineChart.setAnimated(false);
            categoryTrendLineChart.setCreateSymbols(true);
            categoryTrendLineChart.setLegendVisible(false);
            categoryTrendLineChart.setTitle("");
        }

        if (categoryYearAxis != null) {
            categoryYearAxis.setAutoRanging(false);
            categoryYearAxis.setForceZeroInRange(false);
            categoryYearAxis.setMinorTickVisible(false);
            categoryYearAxis.setLabel("Year");
        }

        if (categoryCountAxis != null) {
            categoryCountAxis.setAutoRanging(false);
            categoryCountAxis.setForceZeroInRange(true);
            categoryCountAxis.setMinorTickVisible(false);
            categoryCountAxis.setLabel("Venues count");
        }
    }

    private List<CategoryTrendSeries> loadCategoryTrendData(
            String analysisType,
            String categoryFilter,
            String categoryLabel,
            Integer fromYear,
            Integer toYear
    ) {
        List<Object> rawRows;

        if (ANALYSIS_CONFERENCE_PRIMARY_FOR.equals(analysisType)) {
            rawRows = invokeTrendServiceMethod(
                    conferenceService,
                    List.of(
                            "getConferencePrimaryFoRYearlyTrends",
                            "getConferencePrimaryForYearlyTrends",
                            "getConferenceCategoryYearlyTrends"
                    ),
                    categoryFilter,
                    fromYear,
                    toYear
            );
        } else if (ANALYSIS_JOURNAL_BEST_SUBJECT_AREA.equals(analysisType)) {
            rawRows = invokeTrendServiceMethod(
                    journalService,
                    List.of(
                            "getJournalBestSubjectAreaYearlyTrends",
                            "getJournalSubjectAreaYearlyTrends",
                            "getJournalCategoryYearlyTrends"
                    ),
                    categoryFilter,
                    fromYear,
                    toYear
            );
        } else {
            rawRows = new ArrayList<>();
        }

        return convertRawRowsToSeries(rawRows, categoryFilter, categoryLabel);
    }

    private List<Object> invokeTrendServiceMethod(
            Object service,
            List<String> methodNames,
            String categoryFilter,
            Integer fromYear,
            Integer toYear
    ) {
        if (service == null || methodNames == null || methodNames.isEmpty()) {
            return new ArrayList<>();
        }

        for (String methodName : methodNames) {
            List<Object> result;

            result = tryInvokeServiceMethod(
                    service,
                    methodName,
                    new Class<?>[]{String.class, Integer.class, Integer.class},
                    new Object[]{categoryFilter, fromYear, toYear}
            );

            if (result != null) {
                return result;
            }

            result = tryInvokeServiceMethod(
                    service,
                    methodName,
                    new Class<?>[]{String.class, int.class, int.class},
                    new Object[]{
                            categoryFilter,
                            fromYear == null ? MIN_YEAR : fromYear.intValue(),
                            toYear == null ? LocalDate.now().getYear() : toYear.intValue()
                    }
            );

            if (result != null) {
                return result;
            }

            if (categoryFilter == null || categoryFilter.isBlank()) {
                result = tryInvokeServiceMethod(
                        service,
                        methodName,
                        new Class<?>[]{Integer.class, Integer.class},
                        new Object[]{fromYear, toYear}
                );

                if (result != null) {
                    return result;
                }

                result = tryInvokeServiceMethod(
                        service,
                        methodName,
                        new Class<?>[]{int.class, int.class},
                        new Object[]{
                                fromYear == null ? MIN_YEAR : fromYear.intValue(),
                                toYear == null ? LocalDate.now().getYear() : toYear.intValue()
                        }
                );

                if (result != null) {
                    return result;
                }
            }
        }

        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private List<Object> tryInvokeServiceMethod(
            Object service,
            String methodName,
            Class<?>[] parameterTypes,
            Object[] arguments
    ) {
        try {
            Method method = service.getClass().getMethod(methodName, parameterTypes);
            Object result = method.invoke(service, arguments);

            if (result instanceof List<?> list) {
                return new ArrayList<>((List<Object>) list);
            }

            return new ArrayList<>();

        } catch (NoSuchMethodException exception) {
            return null;
        } catch (Exception exception) {
            throw new RuntimeException("Could not execute service method: " + methodName, exception);
        }
    }

    private List<CategoryTrendSeries> convertRawRowsToSeries(
            List<Object> rawRows,
            String categoryFilter,
            String categoryLabel
    ) {
        if (rawRows == null || rawRows.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Map<Integer, Long>> grouped = new TreeMap<>();

        for (Object row : rawRows) {
            if (row == null) {
                continue;
            }

            String category = readStringValue(
                    row,
                    List.of(
                            "category",
                            "categoryName",
                            "category_name",
                            "primaryFoR",
                            "primaryFor",
                            "primaryFoRName",
                            "primaryForName",
                            "primary_for",
                            "primaryFoR_name",
                            "primary_for_name",
                            "bestSubjectArea",
                            "bestSubjectAreaName",
                            "best_subject_area",
                            "best_subject_area_name",
                            "subjectArea",
                            "subjectAreaName",
                            "subject_area",
                            "subject_area_name",
                            "name",
                            "label"
                    )
            );

            Integer year = readIntegerValue(
                    row,
                    List.of(
                            "year",
                            "publicationYear",
                            "publication_year"
                    )
            );

            Long count = readLongValue(
                    row,
                    List.of(
                            "count",
                            "venueCount",
                            "venue_count",
                            "totalVenues",
                            "total_venues",
                            "total",
                            "value"
                    )
            );

            if ((category == null || category.isBlank())
                    && categoryLabel != null
                    && !categoryLabel.isBlank()) {
                category = categoryLabel;
            }

            if ((category == null || category.isBlank())
                    && categoryFilter != null
                    && !categoryFilter.isBlank()) {
                category = categoryFilter;
            }

            if (category == null || category.isBlank() || year == null || count == null) {
                continue;
            }

            grouped
                    .computeIfAbsent(category, key -> new TreeMap<>())
                    .merge(year, count, Long::sum);
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
                                pointEntry.getKey().intValue(),
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

    private void updateCategoryTrendChart(List<CategoryTrendSeries> allSeries, String analysisType) {
        clearChartOnly();

        if (allSeries == null || allSeries.isEmpty()) {
            return;
        }

        Integer minYear = null;
        Integer maxYear = null;
        double maxValue = 0;

        int seriesIndex = 0;

        for (CategoryTrendSeries categorySeries : allSeries) {
            XYChart.Series<Number, Number> chartSeries = new XYChart.Series<>();
            chartSeries.setName(shortenName(categorySeries.category()));

            for (CategoryTrendPoint point : categorySeries.points()) {
                minYear = minYear == null ? Integer.valueOf(point.year()) : Integer.valueOf(Math.min(minYear, point.year()));
                maxYear = maxYear == null ? Integer.valueOf(point.year()) : Integer.valueOf(Math.max(maxYear, point.year()));
                maxValue = Math.max(maxValue, point.count());

                XYChart.Data<Number, Number> dataPoint =
                        new XYChart.Data<>(Integer.valueOf(point.year()), Long.valueOf(point.count()));

                dataPoint.setExtraValue(point.category());
                chartSeries.getData().add(dataPoint);
            }

            if (!chartSeries.getData().isEmpty() && categoryTrendLineChart != null) {
                categoryTrendLineChart.getData().add(chartSeries);

                final int colorIndex = seriesIndex;
                final CategoryTrendSeries currentSeries = categorySeries;

                runAfterChartRender(() -> applyLineSeriesColor(chartSeries, colorIndex, currentSeries));
            }

            seriesIndex++;
        }

        configureYearAxis(categoryYearAxis, minYear, maxYear);
        configureValueAxis(categoryCountAxis, maxValue);

        if (categoryCountAxis != null) {
            categoryCountAxis.setLabel("Venues count");
        }

        if (categoryTrendLineChart != null) {
            if (ANALYSIS_CONFERENCE_PRIMARY_FOR.equals(analysisType)) {
                categoryTrendLineChart.setTitle("Conference categories by PrimaryFoR over years");
            } else {
                categoryTrendLineChart.setTitle("Journal categories by BestSubjectArea over years");
            }
        }

        updateCustomLegend(allSeries);
    }

    private void clearChartOnly() {
        activeCategoryKey = null;
        chartHighlightHandles.clear();
        legendHighlightHandles.clear();

        if (categoryTrendLineChart != null) {
            categoryTrendLineChart.getData().clear();
            categoryTrendLineChart.setTitle("");
        }

        configureYearAxis(categoryYearAxis, Integer.valueOf(MIN_YEAR), Integer.valueOf(LocalDate.now().getYear()));
        configureValueAxis(categoryCountAxis, 10);

        updateCustomLegend(null);
    }

    private void configureYearAxis(NumberAxis axis, Integer minYear, Integer maxYear) {
        if (axis == null) {
            return;
        }

        if (minYear == null || maxYear == null) {
            axis.setAutoRanging(true);
            return;
        }

        int lowerYear = minYear.intValue();
        int upperYear = maxYear.intValue();

        if (lowerYear == upperYear) {
            lowerYear = lowerYear - 1;
            upperYear = upperYear + 1;
        }

        int yearRange = upperYear - lowerYear;
        int tickUnit = calculateYearTickUnit(yearRange);

        axis.setAutoRanging(false);
        axis.setLowerBound(lowerYear);
        axis.setUpperBound(upperYear);
        axis.setTickUnit(tickUnit);
        axis.setMinorTickVisible(false);
    }

    private int calculateYearTickUnit(int yearRange) {
        if (yearRange <= 10) {
            return 1;
        }

        if (yearRange <= 25) {
            return 5;
        }

        if (yearRange <= 60) {
            return 10;
        }

        return 20;
    }

    private void configureValueAxis(NumberAxis axis, double maxValue) {
        if (axis == null) {
            return;
        }

        double upperBound = calculateNiceUpperBound(maxValue);
        double tickUnit = calculateNiceTickUnit(upperBound);

        axis.setAutoRanging(false);
        axis.setLowerBound(0);
        axis.setUpperBound(upperBound);
        axis.setTickUnit(tickUnit);
        axis.setMinorTickVisible(false);
    }

    private double calculateNiceUpperBound(double maxValue) {
        if (maxValue <= 0) {
            return 10;
        }

        double paddedValue = maxValue * 1.08;
        double roughTickUnit = paddedValue / 7.0;
        double tickUnit = calculateNiceRawTickUnit(roughTickUnit);

        return Math.ceil(paddedValue / tickUnit) * tickUnit;
    }

    private double calculateNiceTickUnit(double upperBound) {
        if (upperBound <= 0) {
            return 1;
        }

        double roughTickUnit = upperBound / 7.0;
        return calculateNiceRawTickUnit(roughTickUnit);
    }

    private double calculateNiceRawTickUnit(double value) {
        if (value <= 0) {
            return 1;
        }

        double magnitude = Math.pow(10, Math.floor(Math.log10(value)));
        double normalized = value / magnitude;

        double niceNormalized;

        if (normalized <= 1) {
            niceNormalized = 1;
        } else if (normalized <= 2) {
            niceNormalized = 2;
        } else if (normalized <= 2.5) {
            niceNormalized = 2.5;
        } else if (normalized <= 5) {
            niceNormalized = 5;
        } else {
            niceNormalized = 10;
        }

        return niceNormalized * magnitude;
    }

    private void applyLineSeriesColor(
            XYChart.Series<Number, Number> series,
            int colorIndex,
            CategoryTrendSeries categorySeries
    ) {
        if (series == null || categorySeries == null) {
            return;
        }

        String color = getChartColor(colorIndex);
        String categoryKey = getCategoryKey(categorySeries);

        Runnable normalStyle = () -> {
            Node line = series.getNode();

            if (line != null) {
                line.setStyle(getLineStyle(color, false));
            }

            for (XYChart.Data<Number, Number> data : series.getData()) {
                Node symbol = data.getNode();

                if (symbol != null) {
                    symbol.setStyle(getLineSymbolStyle(color, false));

                    Tooltip.install(
                            symbol,
                            new Tooltip(
                                    categorySeries.category()
                                            + "\nYear: " + data.getXValue()
                                            + "\nCount: " + data.getYValue()
                            )
                    );
                }
            }
        };

        Runnable highlightedStyle = () -> {
            Node line = series.getNode();

            if (line != null) {
                line.setStyle(getLineStyle(color, true));
                line.toFront();
            }

            for (XYChart.Data<Number, Number> data : series.getData()) {
                Node symbol = data.getNode();

                if (symbol != null) {
                    symbol.setStyle(getLineSymbolStyle(color, true));
                    symbol.toFront();
                }
            }
        };

        normalStyle.run();

        chartHighlightHandles.add(
                new ChartHighlightHandle(
                        categoryKey,
                        normalStyle,
                        highlightedStyle
                )
        );

        setupChartHover(series.getNode(), categoryKey);

        for (XYChart.Data<Number, Number> data : series.getData()) {
            setupChartHover(data.getNode(), categoryKey);
        }

        applyActiveCategoryHighlight();
    }

    private void setupChartHover(Node node, String categoryKey) {
        if (node == null || categoryKey == null || categoryKey.isBlank()) {
            return;
        }

        node.setCursor(Cursor.HAND);
        node.setMouseTransparent(false);

        node.setOnMouseEntered(event -> {
            setActiveCategoryHighlight(categoryKey);
            event.consume();
        });

        node.setOnMouseExited(event -> {
            setActiveCategoryHighlight(null);
            event.consume();
        });
    }

    private void setActiveCategoryHighlight(String categoryKey) {
        boolean sameCategory = activeCategoryKey == null
                ? categoryKey == null
                : activeCategoryKey.equals(categoryKey);

        if (sameCategory) {
            return;
        }

        activeCategoryKey = categoryKey;
        applyActiveCategoryHighlight();
    }

    private void applyActiveCategoryHighlight() {
        for (ChartHighlightHandle handle : chartHighlightHandles) {
            boolean highlighted = activeCategoryKey != null && activeCategoryKey.equals(handle.categoryKey());

            if (highlighted) {
                handle.highlightedStyle().run();
            } else {
                handle.normalStyle().run();
            }
        }

        for (LegendHighlightHandle handle : legendHighlightHandles) {
            boolean highlighted = activeCategoryKey != null && activeCategoryKey.equals(handle.categoryKey());
            applyLegendStyle(handle, highlighted);
        }
    }

    private void updateCustomLegend(List<CategoryTrendSeries> allSeries) {
        legendHighlightHandles.clear();

        if (categoryLegendBox == null || categoryLegendFlow == null) {
            return;
        }

        categoryLegendFlow.getChildren().clear();

        if (allSeries == null || allSeries.isEmpty()) {
            categoryLegendBox.setVisible(false);
            categoryLegendBox.setManaged(false);
            return;
        }

        int index = 0;

        for (CategoryTrendSeries categorySeries : allSeries) {
            String color = getChartColor(index);
            String categoryKey = getCategoryKey(categorySeries);

            Region colorDot = new Region();
            colorDot.setMinSize(10, 10);
            colorDot.setPrefSize(10, 10);
            colorDot.setMaxSize(10, 10);
            colorDot.setStyle(getLegendDotStyle(color, false));

            Label nameLabel = new Label(shortenName(categorySeries.category()));
            nameLabel.setStyle(getLegendLabelStyle(false));

            Tooltip.install(
                    nameLabel,
                    new Tooltip(categorySeries.category() + "\nTotal count: " + categorySeries.totalCount())
            );

            HBox legendItem = new HBox(6, colorDot, nameLabel);
            legendItem.setAlignment(Pos.CENTER_LEFT);
            legendItem.setCursor(Cursor.HAND);
            legendItem.setStyle(getLegendItemStyle(color, false));

            legendItem.setOnMouseEntered(event -> setActiveCategoryHighlight(categoryKey));
            legendItem.setOnMouseExited(event -> setActiveCategoryHighlight(null));

            legendItem.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                    setActiveCategoryHighlight(categoryKey);
                    event.consume();
                }
            });

            legendHighlightHandles.add(
                    new LegendHighlightHandle(
                            categoryKey,
                            legendItem,
                            colorDot,
                            nameLabel,
                            color
                    )
            );

            categoryLegendFlow.getChildren().add(legendItem);
            index++;
        }

        categoryLegendBox.setVisible(true);
        categoryLegendBox.setManaged(true);

        applyActiveCategoryHighlight();
    }

    private void applyLegendStyle(LegendHighlightHandle handle, boolean highlighted) {
        if (handle == null) {
            return;
        }

        if (handle.legendItem() != null) {
            handle.legendItem().setStyle(getLegendItemStyle(handle.color(), highlighted));
        }

        if (handle.colorDot() != null) {
            double dotSize = highlighted ? 13 : 10;

            handle.colorDot().setMinSize(dotSize, dotSize);
            handle.colorDot().setPrefSize(dotSize, dotSize);
            handle.colorDot().setMaxSize(dotSize, dotSize);
            handle.colorDot().setStyle(getLegendDotStyle(handle.color(), highlighted));
        }

        if (handle.nameLabel() != null) {
            handle.nameLabel().setStyle(getLegendLabelStyle(highlighted));
        }
    }

    private String getLineStyle(String color, boolean highlighted) {
        if (highlighted) {
            return "-fx-stroke: " + color + ";"
                    + "-fx-stroke-width: 5.2px;"
                    + "-fx-opacity: 1.0;"
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.40), 8, 0.25, 0, 0);";
        }

        return "-fx-stroke: " + color + ";"
                + "-fx-stroke-width: 2.4px;"
                + "-fx-opacity: 0.92;";
    }

    private String getLineSymbolStyle(String color, boolean highlighted) {
        if (highlighted) {
            return "-fx-background-color: " + color + ", white;"
                    + "-fx-background-insets: 0, 3;"
                    + "-fx-background-radius: 10px;"
                    + "-fx-padding: 7px;"
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 7, 0.25, 0, 0);";
        }

        return "-fx-background-color: " + color + ", white;"
                + "-fx-background-insets: 0, 2;"
                + "-fx-background-radius: 7px;"
                + "-fx-padding: 4px;";
    }

    private String getLegendItemStyle(String color, boolean highlighted) {
        if (highlighted) {
            return "-fx-background-color: rgba(255,255,255,1.0);"
                    + "-fx-border-color: " + color + ";"
                    + "-fx-border-width: 2px;"
                    + "-fx-border-radius: 999px;"
                    + "-fx-background-radius: 999px;"
                    + "-fx-padding: 5 9 5 9;"
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 6, 0.20, 0, 0);";
        }

        return "-fx-background-color: rgba(255,255,255,0.95);"
                + "-fx-border-color: #b9daf7;"
                + "-fx-border-width: 1px;"
                + "-fx-border-radius: 999px;"
                + "-fx-background-radius: 999px;"
                + "-fx-padding: 5 9 5 9;";
    }

    private String getLegendDotStyle(String color, boolean highlighted) {
        if (highlighted) {
            return "-fx-background-color: " + color + ";"
                    + "-fx-background-radius: 999px;"
                    + "-fx-border-color: #17212b;"
                    + "-fx-border-radius: 999px;"
                    + "-fx-border-width: 1.5px;";
        }

        return "-fx-background-color: " + color + ";"
                + "-fx-background-radius: 999px;"
                + "-fx-border-width: 0;";
    }

    private String getLegendLabelStyle(boolean highlighted) {
        if (highlighted) {
            return "-fx-text-fill: #17212b;"
                    + "-fx-font-size: 11px;"
                    + "-fx-font-weight: 900;";
        }

        return "-fx-text-fill: #0f4c81;"
                + "-fx-font-size: 11px;"
                + "-fx-font-weight: 700;";
    }

    private String getChartColor(int index) {
        return CHART_COLORS[index % CHART_COLORS.length];
    }

    private String getCategoryKey(CategoryTrendSeries categorySeries) {
        if (categorySeries == null || categorySeries.category() == null) {
            return "";
        }

        return categorySeries.category();
    }

    private String shortenName(String name) {
        if (name == null || name.isBlank()) {
            return "-";
        }

        if (name.length() <= 42) {
            return name;
        }

        return name.substring(0, 39) + "...";
    }

    private YearRange getSelectedYearRange() {
        Integer startYear = categoryFromYearComboBox == null ? null : categoryFromYearComboBox.getValue();
        Integer endYear = categoryToYearComboBox == null ? null : categoryToYearComboBox.getValue();

        if (startYear != null && endYear != null && endYear < startYear) {
            throw new IllegalArgumentException("Το To year πρέπει να είναι μεγαλύτερο ή ίσο από το From year.");
        }

        return new YearRange(startYear, endYear);
    }

    private String readStringValue(Object row, List<String> names) {
        Object value = readValue(row, names);

        if (value == null) {
            return null;
        }

        return String.valueOf(value).trim();
    }

    private Integer readIntegerValue(Object row, List<String> names) {
        Object value = readValue(row, names);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return Integer.valueOf(number.intValue());
        }

        try {
            return Integer.valueOf(Integer.parseInt(String.valueOf(value).trim()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Long readLongValue(Object row, List<String> names) {
        Object value = readValue(row, names);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return Long.valueOf(number.longValue());
        }

        try {
            return Long.valueOf(Math.round(Double.parseDouble(String.valueOf(value).trim())));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Object readValue(Object row, List<String> names) {
        for (String name : names) {
            Object value = tryReadMethod(row, name);

            if (value != null) {
                return value;
            }

            value = tryReadMethod(row, "get" + capitalize(name));

            if (value != null) {
                return value;
            }

            value = tryReadField(row, name);

            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private Object tryReadMethod(Object row, String methodName) {
        try {
            Method method = row.getClass().getMethod(methodName);
            return method.invoke(row);
        } catch (Exception exception) {
            return null;
        }
    }

    private Object tryReadField(Object row, String fieldName) {
        try {
            Field field = row.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(row);
        } catch (Exception exception) {
            return null;
        }
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private void setLoading(boolean loading) {
        if (categoryTypeComboBox != null) {
            categoryTypeComboBox.setDisable(loading);
        }

        if (categoryFilterComboBox != null) {
            categoryFilterComboBox.setDisable(loading);
        }

        if (categoryFromYearComboBox != null) {
            categoryFromYearComboBox.setDisable(loading);
        }

        if (categoryToYearComboBox != null) {
            categoryToYearComboBox.setDisable(loading);
        }

        if (loadCategoryTrendButton != null) {
            loadCategoryTrendButton.setDisable(loading);
        }

        if (clearCategoryTrendButton != null) {
            clearCategoryTrendButton.setDisable(loading);
        }

        if (categoryTrendLineChart != null) {
            categoryTrendLineChart.setDisable(loading);
        }
    }

    private void setStatus(String message) {
        if (categoryStatusLabel != null) {
            categoryStatusLabel.setText(message == null ? "" : message);
        }
    }

    private void runAfterChartRender(Runnable action) {
        Platform.runLater(() -> Platform.runLater(action));
    }

    private void startBackgroundTask(Task<?> task, String threadName) {
        Thread thread = new Thread(task, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.isBlank() ? "Άγνωστο σφάλμα." : message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.isBlank() ? "-" : message);
        alert.showAndWait();
    }

    private record CategoryTrendPoint(
            String category,
            int year,
            long count
    ) {
    }

    private record CategoryTrendSeries(
            String category,
            List<CategoryTrendPoint> points,
            long totalCount
    ) {
    }

    private record ChartHighlightHandle(
            String categoryKey,
            Runnable normalStyle,
            Runnable highlightedStyle
    ) {
    }

    private record LegendHighlightHandle(
            String categoryKey,
            HBox legendItem,
            Region colorDot,
            Label nameLabel,
            String color
    ) {
    }

    private record YearRange(
            Integer startYear,
            Integer endYear
    ) {
    }
}