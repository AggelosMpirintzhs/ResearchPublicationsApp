package controllers.charts;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import service.charts.VenueAnalysisService;

import java.util.ArrayList;
import java.util.List;

public class VenueAnalysisController {

    private static final String BAR_SINGLE_CATEGORY = " ";

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
            "#2e7d32"
    };

    private static final int MIN_YEAR = VenueAnalysisService.DEFAULT_MIN_YEAR;
    private static final int SEARCH_LIMIT = 20;
    private static final int MAX_SELECTED_VENUES = 10;
    private static final int MIN_SEARCH_LENGTH = 3;
    private static final int SEARCH_DEBOUNCE_MS = 250;

    private final VenueAnalysisService venueAnalysisService = new VenueAnalysisService();

    private Object selectedSearchVenue;
    private PauseTransition searchDebounce;
    private Task<List<Object>> venueSearchTask;

    private String activeVenueKey;

    private final List<ChartHighlightHandle> chartHighlightHandles = new ArrayList<>();
    private final List<LegendHighlightHandle> legendHighlightHandles = new ArrayList<>();

    @FXML
    private ComboBox<String> venueTypeComboBox;

    @FXML
    private TextField venueSearchField;

    @FXML
    private Button addSelectedButton;

    @FXML
    private Button removeSelectedButton;

    @FXML
    private Button clearButton;

    @FXML
    private Button loadChartButton;

    @FXML
    private VBox searchResultsContainer;

    @FXML
    private ListView<Object> searchResultsListView;

    @FXML
    private ListView<VenueAnalysisService.SelectedVenue> selectedVenuesListView;

    @FXML
    private ComboBox<String> metricComboBox;

    @FXML
    private ComboBox<Integer> fromYearComboBox;

    @FXML
    private ComboBox<Integer> toYearComboBox;

    @FXML
    private VBox lineChartLegendBox;

    @FXML
    private FlowPane lineChartLegendFlow;

    @FXML
    private VBox barChartsLegendBox;

    @FXML
    private FlowPane barChartsLegendFlow;

    @FXML
    private LineChart<Number, Number> comparisonLineChart;

    @FXML
    private NumberAxis yearAxis;

    @FXML
    private NumberAxis valueAxis;

    @FXML
    private BarChart<String, Number> totalArticlesBarChart;

    @FXML
    private CategoryAxis totalArticlesCategoryAxis;

    @FXML
    private NumberAxis totalArticlesValueAxis;

    @FXML
    private BarChart<String, Number> avgArticlesBarChart;

    @FXML
    private CategoryAxis avgArticlesCategoryAxis;

    @FXML
    private NumberAxis avgArticlesValueAxis;

    @FXML
    private BarChart<String, Number> avgAuthorEntriesBarChart;

    @FXML
    private CategoryAxis avgAuthorEntriesCategoryAxis;

    @FXML
    private NumberAxis avgAuthorEntriesValueAxis;

    @FXML
    public void initialize() {
        setupVenueTypeComboBox();
        setupMetricComboBox();
        setupYearComboBoxes();
        setupSearchField();
        setupSearchResultsListView();
        setupSelectedVenuesListView();
        setupLineChart();
        setupBarCharts();

        selectedSearchVenue = null;
        clearSearchResults();
        clearCharts();
    }

    @FXML
    private void searchVenues() {
        executeVenueSearch(true);
    }

    private void searchVenuesRealtime() {
        executeVenueSearch(false);
    }

    private void executeVenueSearch(boolean showAlerts) {
        String venueType = getSelectedVenueType();
        String searchText = venueSearchField == null ? "" : venueSearchField.getText().trim();

        stopCurrentVenueSearchTaskOnly();

        if (venueType == null || venueType.isBlank()) {
            clearSearchResults();

            if (showAlerts) {
                showError("No type selected", "You must select Journal or Conference first.");
            }

            return;
        }

        if (searchText.length() < MIN_SEARCH_LENGTH) {
            selectedSearchVenue = null;
            clearSearchResults();

            if (showAlerts && searchText.isBlank()) {
                showError("Invalid search", "You must type a journal or conference name.");
            } else if (showAlerts) {
                showError("Invalid search", "You must type at least 3 characters.");
            }

            return;
        }

        String requestedVenueType = venueType;
        String requestedSearchText = searchText;

        selectedSearchVenue = null;
        clearSearchResultsOnly();

        Task<List<Object>> task = new Task<>() {
            @Override
            protected List<Object> call() {
                return venueAnalysisService.searchVenues(
                        requestedVenueType,
                        requestedSearchText,
                        SEARCH_LIMIT
                );
            }
        };

        venueSearchTask = task;

        if (showAlerts) {
            setLoading(true);
        }

        task.setOnSucceeded(event -> {
            if (showAlerts) {
                setLoading(false);
            }

            if (task.isCancelled()) {
                return;
            }

            String currentSearchText = venueSearchField == null ? "" : venueSearchField.getText().trim();
            String currentVenueType = getSelectedVenueType();

            if (!requestedSearchText.equals(currentSearchText)) {
                return;
            }

            if (!requestedVenueType.equals(currentVenueType)) {
                return;
            }

            List<Object> results = task.getValue();
            renderSearchResults(results);

            if ((results == null || results.isEmpty()) && showAlerts) {
                showInfo(
                        "No results found",
                        "No journal or conference was found for this search text."
                );
            }
        });

        task.setOnFailed(event -> {
            if (showAlerts) {
                setLoading(false);
            }

            Throwable exception = task.getException();

            if (showAlerts) {
                showError("Search error", exception == null ? null : exception.getMessage());
            } else {
                clearSearchResults();
            }
        });

        startBackgroundTask(task, "venue-analysis-search-task");
    }

    @FXML
    private void addSelectedVenue() {
        addSelectedVenueFromSearch(selectedSearchVenue, true);
    }

    private void addSelectedVenueFromSearch(Object venue, boolean showMessages) {
        if (selectedVenuesListView == null) {
            return;
        }

        if (venue == null) {
            if (showMessages) {
                showError(
                        "No venue selected",
                        "Select a result from the list first."
                );
            }

            return;
        }

        String venueType = getSelectedVenueType();

        if (venueType == null || venueType.isBlank()) {
            if (showMessages) {
                showError("No type selected", "You must select Journal or Conference first.");
            }

            return;
        }

        if (selectedVenuesListView.getItems().size() >= MAX_SELECTED_VENUES) {
            if (showMessages) {
                showError(
                        "Too many venues",
                        "You can select up to " + MAX_SELECTED_VENUES + " venues."
                );
            }

            return;
        }

        VenueAnalysisService.SelectedVenue selectedVenue;

        try {
            selectedVenue = venueAnalysisService.createSelectedVenue(venueType, venue);
        } catch (IllegalArgumentException exception) {
            if (showMessages) {
                showError("Invalid venue", exception.getMessage());
            }

            return;
        }

        if (venueAnalysisService.alreadySelected(selectedVenuesListView.getItems(), selectedVenue)) {
            if (showMessages) {
                showInfo("Already selected", "This venue is already in the selected list.");
            }

            return;
        }

        selectedVenuesListView.getItems().add(selectedVenue);

        if (searchResultsListView != null) {
            searchResultsListView.getSelectionModel().clearSelection();
        }

        selectedSearchVenue = null;
    }

    @FXML
    private void removeSelectedVenue() {
        if (selectedVenuesListView == null) {
            return;
        }

        VenueAnalysisService.SelectedVenue selected =
                selectedVenuesListView.getSelectionModel().getSelectedItem();

        if (selected != null) {
            selectedVenuesListView.getItems().remove(selected);
        }
    }

    @FXML
    private void loadVenueAnalysis() {
        if (selectedVenuesListView == null || selectedVenuesListView.getItems().isEmpty()) {
            showError("No selected venues", "Add at least one journal or conference.");
            return;
        }

        String metric = metricComboBox == null ? null : metricComboBox.getValue();

        try {
            venueAnalysisService.validateLineMetric(metric);
        } catch (IllegalArgumentException exception) {
            showError("No metric selected", exception.getMessage());
            return;
        }

        VenueAnalysisService.YearRange yearRange;

        try {
            yearRange = getSelectedYearRange();
        } catch (IllegalArgumentException exception) {
            showError("Invalid years", exception.getMessage());
            return;
        }

        List<VenueAnalysisService.SelectedVenue> selectedVenues =
                new ArrayList<>(selectedVenuesListView.getItems());

        Task<List<VenueAnalysisService.VenueChartSeries>> task = new Task<>() {
            @Override
            protected List<VenueAnalysisService.VenueChartSeries> call() {
                return venueAnalysisService.loadVenueChartSeries(selectedVenues, yearRange);
            }
        };

        setLoading(true);

        task.setOnSucceeded(event -> {
            setLoading(false);

            List<VenueAnalysisService.VenueChartSeries> chartData = task.getValue();

            updateLineChart(chartData, metric);
            updateBarCharts(chartData);
        });

        task.setOnFailed(event -> {
            setLoading(false);
            Throwable exception = task.getException();
            showError("Chart loading error", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(task, "venue-analysis-load-task");
    }

    @FXML
    private void loadChart() {
        loadVenueAnalysis();
    }

    @FXML
    private void clear() {
        stopCurrentVenueSearch();

        if (venueSearchField != null) {
            venueSearchField.clear();
        }

        clearSearchResults();

        if (selectedVenuesListView != null) {
            selectedVenuesListView.getItems().clear();
        }

        if (fromYearComboBox != null) {
            fromYearComboBox.getSelectionModel().clearSelection();
        }

        if (toYearComboBox != null) {
            toYearComboBox.getSelectionModel().clearSelection();
            refreshToYearOptions(null);
        }

        selectedSearchVenue = null;
        clearCharts();
    }

    private void setupVenueTypeComboBox() {
        if (venueTypeComboBox == null) {
            return;
        }

        venueTypeComboBox.getItems().setAll(venueAnalysisService.getVenueTypes());
        venueTypeComboBox.getSelectionModel().select(VenueAnalysisService.TYPE_JOURNAL);

        venueTypeComboBox.setOnAction(event -> {
            stopCurrentVenueSearch();

            selectedSearchVenue = null;
            clearSearchResults();

            String searchText = venueSearchField == null ? "" : venueSearchField.getText().trim();

            if (searchText.length() >= MIN_SEARCH_LENGTH && searchDebounce != null) {
                searchDebounce.playFromStart();
            }
        });
    }

    private void setupMetricComboBox() {
        if (metricComboBox == null) {
            return;
        }

        metricComboBox.getItems().setAll(venueAnalysisService.getLineMetrics());
        metricComboBox.getSelectionModel().select(VenueAnalysisService.METRIC_ARTICLES);
    }

    private void setupYearComboBoxes() {
        setupYearComboBox(fromYearComboBox);
        setupYearComboBox(toYearComboBox);

        if (fromYearComboBox != null) {
            fromYearComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                    refreshToYearOptions(newValue)
            );
        }
    }

    private void setupYearComboBox(ComboBox<Integer> comboBox) {
        if (comboBox == null) {
            return;
        }

        comboBox.setEditable(false);
        comboBox.getItems().setAll(venueAnalysisService.buildYearList(MIN_YEAR));
    }

    private void refreshToYearOptions(Integer fromYear) {
        if (toYearComboBox == null) {
            return;
        }

        Integer currentToYear = toYearComboBox.getValue();

        toYearComboBox.getItems().setAll(venueAnalysisService.buildYearList(fromYear));

        if (currentToYear != null && fromYear != null && currentToYear < fromYear) {
            toYearComboBox.getSelectionModel().clearSelection();
            return;
        }

        if (currentToYear != null && toYearComboBox.getItems().contains(currentToYear)) {
            toYearComboBox.getSelectionModel().select(currentToYear);
        }
    }

    private void setupSearchField() {
        searchDebounce = new PauseTransition(Duration.millis(SEARCH_DEBOUNCE_MS));
        searchDebounce.setOnFinished(event -> searchVenuesRealtime());

        if (venueSearchField == null) {
            return;
        }

        venueSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (searchDebounce != null) {
                searchDebounce.playFromStart();
            }
        });

        venueSearchField.setOnAction(event -> searchVenues());
    }

    private void setupSearchResultsListView() {
        if (searchResultsListView == null) {
            return;
        }

        searchResultsListView.setPlaceholder(new Label("Search results will appear here."));

        searchResultsListView.setCellFactory(listView -> {
            ListCell<Object> cell = new ListCell<>() {
                @Override
                protected void updateItem(Object venue, boolean empty) {
                    super.updateItem(venue, empty);

                    if (empty || venue == null) {
                        setText(null);
                    } else {
                        setText(venueAnalysisService.getRawVenueDisplayName(venue));
                    }
                }
            };

            cell.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2
                        && !cell.isEmpty()
                        && cell.getItem() != null) {

                    selectedSearchVenue = cell.getItem();
                    addSelectedVenueFromSearch(cell.getItem(), false);
                }
            });

            return cell;
        });

        searchResultsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> selectedSearchVenue = selected
        );
    }

    private void setupSelectedVenuesListView() {
        if (selectedVenuesListView == null) {
            return;
        }

        selectedVenuesListView.setPlaceholder(new Label("Selected venues will appear here."));

        selectedVenuesListView.setCellFactory(listView -> {
            ListCell<VenueAnalysisService.SelectedVenue> cell = new ListCell<>() {
                @Override
                protected void updateItem(VenueAnalysisService.SelectedVenue selectedVenue, boolean empty) {
                    super.updateItem(selectedVenue, empty);

                    if (empty || selectedVenue == null) {
                        setText(null);
                    } else {
                        setText(venueAnalysisService.getVenueDisplayName(selectedVenue));
                    }
                }
            };

            cell.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2
                        && !cell.isEmpty()
                        && cell.getItem() != null) {

                    selectedVenuesListView.getItems().remove(cell.getItem());
                }
            });

            return cell;
        });
    }

    private void setupLineChart() {
        if (comparisonLineChart != null) {
            comparisonLineChart.setAnimated(false);
            comparisonLineChart.setCreateSymbols(true);
            comparisonLineChart.setLegendVisible(false);
        }

        if (yearAxis != null) {
            yearAxis.setAutoRanging(false);
            yearAxis.setForceZeroInRange(false);
            yearAxis.setTickLabelRotation(0);
        }

        if (valueAxis != null) {
            valueAxis.setAutoRanging(false);
            valueAxis.setForceZeroInRange(true);
        }
    }

    private void setupBarCharts() {
        setupSingleBarChart(
                totalArticlesBarChart,
                totalArticlesCategoryAxis,
                totalArticlesValueAxis,
                VenueAnalysisService.BAR_TOTAL_ARTICLES
        );

        setupSingleBarChart(
                avgArticlesBarChart,
                avgArticlesCategoryAxis,
                avgArticlesValueAxis,
                VenueAnalysisService.BAR_AVG_ARTICLES_PER_YEAR
        );

        setupSingleBarChart(
                avgAuthorEntriesBarChart,
                avgAuthorEntriesCategoryAxis,
                avgAuthorEntriesValueAxis,
                VenueAnalysisService.BAR_AVG_AUTHOR_ENTRIES_PER_YEAR
        );
    }

    private void setupSingleBarChart(
            BarChart<String, Number> chart,
            CategoryAxis categoryAxis,
            NumberAxis valueAxis,
            String valueLabel
    ) {
        if (chart != null) {
            chart.setAnimated(false);
            chart.setLegendVisible(false);
            chart.setCategoryGap(18);
            chart.setBarGap(4);
        }

        if (categoryAxis != null) {
            categoryAxis.setLabel("");
            categoryAxis.setTickLabelsVisible(false);
            categoryAxis.setTickMarkVisible(false);
        }

        if (valueAxis != null) {
            valueAxis.setLabel(valueLabel);
            valueAxis.setAutoRanging(false);
            valueAxis.setForceZeroInRange(true);
        }
    }

    private void renderSearchResults(List<Object> results) {
        if (searchResultsListView == null) {
            return;
        }

        searchResultsListView.getItems().clear();
        searchResultsListView.getSelectionModel().clearSelection();
        selectedSearchVenue = null;

        setSearchResultsVisible(true);

        if (results == null || results.isEmpty()) {
            searchResultsListView.setPlaceholder(new Label("No matching results"));
            return;
        }

        searchResultsListView.setPlaceholder(new Label("No matching results"));
        searchResultsListView.getItems().setAll(results);
    }

    private void setSearchResultsVisible(boolean visible) {
        if (searchResultsContainer != null) {
            searchResultsContainer.setVisible(visible);
            searchResultsContainer.setManaged(visible);
        }
    }

    private void clearSearchResults() {
        clearSearchResultsOnly();
        setSearchResultsVisible(false);
    }

    private void clearSearchResultsOnly() {
        if (searchResultsListView != null) {
            searchResultsListView.getItems().clear();
            searchResultsListView.getSelectionModel().clearSelection();
            searchResultsListView.setPlaceholder(new Label("Search results will appear here."));
        }

        selectedSearchVenue = null;
    }

    private void stopCurrentVenueSearch() {
        if (searchDebounce != null) {
            searchDebounce.stop();
        }

        stopCurrentVenueSearchTaskOnly();
    }

    private void stopCurrentVenueSearchTaskOnly() {
        if (venueSearchTask != null && venueSearchTask.isRunning()) {
            venueSearchTask.cancel();
        }
    }

    private void updateLineChart(
            List<VenueAnalysisService.VenueChartSeries> allSeries,
            String metric
    ) {
        clearCharts();

        if (allSeries == null || allSeries.isEmpty()) {
            updateCustomLegend(null);
            return;
        }

        Integer minYear = null;
        Integer maxYear = null;
        double maxValue = 0;
        int seriesIndex = 0;

        for (VenueAnalysisService.VenueChartSeries venueSeries : allSeries) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(shortenSeriesName(venueSeries.name()));

            for (VenueAnalysisService.VenueYearlyStats yearlyStats : venueSeries.yearlyStats()) {
                Integer year = yearlyStats.year();
                Long value = venueAnalysisService.getLineMetricValue(yearlyStats, metric);

                if (year == null || value == null) {
                    continue;
                }

                minYear = minYear == null ? year : Math.min(minYear, year);
                maxYear = maxYear == null ? year : Math.max(maxYear, year);
                maxValue = Math.max(maxValue, value);

                series.getData().add(new XYChart.Data<>(year, value));
            }

            if (!series.getData().isEmpty() && comparisonLineChart != null) {
                comparisonLineChart.getData().add(series);

                final int colorIndex = seriesIndex;
                final VenueAnalysisService.VenueChartSeries currentVenueSeries = venueSeries;
                runAfterChartRender(() -> applyLineSeriesColor(series, colorIndex, currentVenueSeries));
            }

            seriesIndex++;
        }

        configureYearAxis(yearAxis, minYear, maxYear);
        configureValueAxis(valueAxis, maxValue);

        if (valueAxis != null) {
            valueAxis.setLabel(metric);
        }

        updateCustomLegend(allSeries);
    }

    private void updateBarCharts(List<VenueAnalysisService.VenueChartSeries> allSeries) {
        updateSingleMetricBarChart(
                totalArticlesBarChart,
                totalArticlesValueAxis,
                allSeries,
                VenueAnalysisService.BAR_TOTAL_ARTICLES
        );

        updateSingleMetricBarChart(
                avgArticlesBarChart,
                avgArticlesValueAxis,
                allSeries,
                VenueAnalysisService.BAR_AVG_ARTICLES_PER_YEAR
        );

        updateSingleMetricBarChart(
                avgAuthorEntriesBarChart,
                avgAuthorEntriesValueAxis,
                allSeries,
                VenueAnalysisService.BAR_AVG_AUTHOR_ENTRIES_PER_YEAR
        );
    }

    private void updateSingleMetricBarChart(
            BarChart<String, Number> chart,
            NumberAxis valueAxis,
            List<VenueAnalysisService.VenueChartSeries> allSeries,
            String metric
    ) {
        if (chart == null) {
            return;
        }

        chart.getData().clear();

        if (allSeries == null || allSeries.isEmpty()) {
            configureValueAxis(valueAxis, 10);
            return;
        }

        double maxValue = 0;
        int seriesIndex = 0;

        for (VenueAnalysisService.VenueChartSeries venueSeries : allSeries) {
            VenueAnalysisService.VenueAggregate aggregate =
                    venueAnalysisService.calculateVenueAggregate(venueSeries);

            String venueName = shortenSeriesName(venueSeries.name());
            double value = venueAnalysisService.getBarMetricValue(aggregate, metric);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(venueName);

            XYChart.Data<String, Number> dataPoint =
                    new XYChart.Data<>(BAR_SINGLE_CATEGORY, value);

            series.getData().add(dataPoint);
            chart.getData().add(series);

            final int colorIndex = seriesIndex;
            final VenueAnalysisService.VenueChartSeries currentVenueSeries = venueSeries;
            runAfterChartRender(() -> applyBarColor(dataPoint, colorIndex, currentVenueSeries));

            maxValue = Math.max(maxValue, value);
            seriesIndex++;
        }

        if (valueAxis != null) {
            valueAxis.setLabel(metric);
        }

        configureValueAxis(valueAxis, maxValue);
    }

    private void clearCharts() {
        clearInteractiveHighlightState();

        if (comparisonLineChart != null) {
            comparisonLineChart.getData().clear();
        }

        clearBarCharts();

        configureYearAxis(yearAxis, MIN_YEAR, venueAnalysisService.getCurrentYear());
        configureValueAxis(valueAxis, 10);

        configureValueAxis(totalArticlesValueAxis, 10);
        configureValueAxis(avgArticlesValueAxis, 10);
        configureValueAxis(avgAuthorEntriesValueAxis, 10);

        updateCustomLegend(null);
    }

    private void clearBarCharts() {
        if (totalArticlesBarChart != null) {
            totalArticlesBarChart.getData().clear();
        }

        if (avgArticlesBarChart != null) {
            avgArticlesBarChart.getData().clear();
        }

        if (avgAuthorEntriesBarChart != null) {
            avgAuthorEntriesBarChart.getData().clear();
        }
    }

    private void configureYearAxis(NumberAxis axis, Integer minYear, Integer maxYear) {
        if (axis == null) {
            return;
        }

        if (minYear == null || maxYear == null) {
            axis.setAutoRanging(true);
            return;
        }

        int lowerYear = minYear;
        int upperYear = maxYear;

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

    private String getChartColor(int index) {
        return CHART_COLORS[index % CHART_COLORS.length];
    }

    private void runAfterChartRender(Runnable action) {
        Platform.runLater(() -> Platform.runLater(action));
    }

    private void applyLineSeriesColor(
            XYChart.Series<Number, Number> series,
            int colorIndex,
            VenueAnalysisService.VenueChartSeries venueSeries
    ) {
        if (series == null || venueSeries == null) {
            return;
        }

        String color = getChartColor(colorIndex);
        String venueKey = venueAnalysisService.getVenueKey(venueSeries);

        Runnable normalStyle = () -> {
            Node line = series.getNode();

            if (line != null) {
                line.setStyle(getLineStyle(color, false));
            }

            for (XYChart.Data<Number, Number> data : series.getData()) {
                Node symbol = data.getNode();

                if (symbol != null) {
                    symbol.setStyle(getLineSymbolStyle(color, false));
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
        registerChartHighlight(venueKey, normalStyle, highlightedStyle);

        setupChartHover(series.getNode(), venueKey);

        for (XYChart.Data<Number, Number> data : series.getData()) {
            setupChartHover(data.getNode(), venueKey);
        }
    }

    private void applyBarColor(
            XYChart.Data<String, Number> dataPoint,
            int colorIndex,
            VenueAnalysisService.VenueChartSeries venueSeries
    ) {
        if (dataPoint == null || dataPoint.getNode() == null || venueSeries == null) {
            return;
        }

        String color = getChartColor(colorIndex);
        String venueKey = venueAnalysisService.getVenueKey(venueSeries);
        Node barNode = dataPoint.getNode();

        Runnable normalStyle = () -> barNode.setStyle(getBarStyle(color, false));
        Runnable highlightedStyle = () -> {
            barNode.setStyle(getBarStyle(color, true));
            barNode.toFront();
        };

        normalStyle.run();
        registerChartHighlight(venueKey, normalStyle, highlightedStyle);
        setupChartHover(barNode, venueKey);
    }

    private void registerChartHighlight(String venueKey, Runnable normalStyle, Runnable highlightedStyle) {
        if (venueKey == null || venueKey.isBlank()) {
            return;
        }

        chartHighlightHandles.add(
                new ChartHighlightHandle(
                        venueKey,
                        normalStyle,
                        highlightedStyle
                )
        );

        applyActiveVenueHighlight();
    }

    private void setupChartHover(Node node, String venueKey) {
        if (node == null || venueKey == null || venueKey.isBlank()) {
            return;
        }

        node.setCursor(Cursor.HAND);
        node.setMouseTransparent(false);

        node.setOnMouseEntered(event -> {
            setActiveVenueHighlight(venueKey);
            event.consume();
        });

        node.setOnMouseExited(event -> {
            setActiveVenueHighlight(null);
            event.consume();
        });
    }

    private void setActiveVenueHighlight(String venueKey) {
        boolean sameVenue = activeVenueKey == null
                ? venueKey == null
                : activeVenueKey.equals(venueKey);

        if (sameVenue) {
            return;
        }

        activeVenueKey = venueKey;
        applyActiveVenueHighlight();
    }

    private void applyActiveVenueHighlight() {
        for (ChartHighlightHandle handle : chartHighlightHandles) {
            boolean highlighted = activeVenueKey != null && activeVenueKey.equals(handle.venueKey());

            if (highlighted) {
                handle.highlightedStyle().run();
            } else {
                handle.normalStyle().run();
            }
        }

        for (LegendHighlightHandle handle : legendHighlightHandles) {
            boolean highlighted = activeVenueKey != null && activeVenueKey.equals(handle.venueKey());
            applyLegendStyle(handle, highlighted);
        }
    }

    private void clearInteractiveHighlightState() {
        activeVenueKey = null;
        chartHighlightHandles.clear();
        legendHighlightHandles.clear();
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
            return "-fx-stroke: " + color + ";" +
                    "-fx-stroke-width: 5.2px;" +
                    "-fx-opacity: 1.0;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.40), 8, 0.25, 0, 0);";
        }

        return "-fx-stroke: " + color + ";" +
                "-fx-stroke-width: 2.4px;" +
                "-fx-opacity: 0.92;";
    }

    private String getLineSymbolStyle(String color, boolean highlighted) {
        if (highlighted) {
            return "-fx-background-color: " + color + ", white;" +
                    "-fx-background-insets: 0, 3;" +
                    "-fx-background-radius: 10px;" +
                    "-fx-padding: 7px;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 7, 0.25, 0, 0);";
        }

        return "-fx-background-color: " + color + ", white;" +
                "-fx-background-insets: 0, 2;" +
                "-fx-background-radius: 7px;" +
                "-fx-padding: 4px;";
    }

    private String getBarStyle(String color, boolean highlighted) {
        if (highlighted) {
            return "-fx-bar-fill: " + color + ";" +
                    "-fx-border-color: #17212b;" +
                    "-fx-border-width: 2px;" +
                    "-fx-border-radius: 3px 3px 0 0;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 8, 0.25, 0, 0);";
        }

        return "-fx-bar-fill: " + color + ";" +
                "-fx-border-width: 0;";
    }

    private String getLegendItemStyle(String color, boolean highlighted) {
        if (highlighted) {
            return "-fx-background-color: rgba(255,255,255,1.0);" +
                    "-fx-border-color: " + color + ";" +
                    "-fx-border-width: 2px;" +
                    "-fx-border-radius: 999px;" +
                    "-fx-background-radius: 999px;" +
                    "-fx-padding: 5 9 5 9;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 6, 0.20, 0, 0);";
        }

        return "-fx-background-color: rgba(255,255,255,0.95);" +
                "-fx-border-color: #b9daf7;" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 999px;" +
                "-fx-background-radius: 999px;" +
                "-fx-padding: 5 9 5 9;";
    }

    private String getLegendDotStyle(String color, boolean highlighted) {
        if (highlighted) {
            return "-fx-background-color: " + color + ";" +
                    "-fx-background-radius: 999px;" +
                    "-fx-border-color: #17212b;" +
                    "-fx-border-radius: 999px;" +
                    "-fx-border-width: 1.5px;";
        }

        return "-fx-background-color: " + color + ";" +
                "-fx-background-radius: 999px;" +
                "-fx-border-width: 0;";
    }

    private String getLegendLabelStyle(boolean highlighted) {
        if (highlighted) {
            return "-fx-text-fill: #17212b;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: 900;";
        }

        return "-fx-text-fill: #0f4c81;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: 700;";
    }

    private void updateCustomLegend(List<VenueAnalysisService.VenueChartSeries> allSeries) {
        legendHighlightHandles.clear();

        updateLegendFlow(lineChartLegendBox, lineChartLegendFlow, allSeries);
        updateLegendFlow(barChartsLegendBox, barChartsLegendFlow, allSeries);

        applyActiveVenueHighlight();
    }

    private void updateLegendFlow(
            VBox legendBox,
            FlowPane legendFlow,
            List<VenueAnalysisService.VenueChartSeries> allSeries
    ) {
        if (legendBox == null || legendFlow == null) {
            return;
        }

        legendFlow.getChildren().clear();

        if (allSeries == null || allSeries.isEmpty()) {
            legendBox.setVisible(false);
            legendBox.setManaged(false);
            return;
        }

        int index = 0;

        for (VenueAnalysisService.VenueChartSeries venueSeries : allSeries) {
            String color = getChartColor(index);
            String venueKey = venueAnalysisService.getVenueKey(venueSeries);
            String fullName = venueSeries.name();
            String shortName = shortenSeriesName(fullName);

            Region colorDot = new Region();
            colorDot.setMinSize(10, 10);
            colorDot.setPrefSize(10, 10);
            colorDot.setMaxSize(10, 10);
            colorDot.setStyle(getLegendDotStyle(color, false));

            Label nameLabel = new Label(shortName);
            nameLabel.setStyle(getLegendLabelStyle(false));

            if (fullName != null && fullName.length() > 45) {
                Tooltip.install(nameLabel, new Tooltip(fullName));
            }

            HBox legendItem = new HBox(6, colorDot, nameLabel);
            legendItem.setAlignment(Pos.CENTER_LEFT);
            legendItem.setCursor(Cursor.HAND);
            legendItem.setStyle(getLegendItemStyle(color, false));

            Tooltip.install(
                    legendItem,
                    new Tooltip("Hover to highlight. Double click to remove this venue.")
            );

            LegendHighlightHandle legendHandle = new LegendHighlightHandle(
                    venueKey,
                    legendItem,
                    colorDot,
                    nameLabel,
                    color
            );

            legendHighlightHandles.add(legendHandle);

            VenueAnalysisService.VenueChartSeries currentVenueSeries = venueSeries;

            legendItem.setOnMouseEntered(event -> setActiveVenueHighlight(venueKey));
            legendItem.setOnMouseExited(event -> setActiveVenueHighlight(null));

            legendItem.setOnMouseClicked(event -> {
                if (event.getButton() != MouseButton.PRIMARY) {
                    return;
                }

                if (event.getClickCount() == 2) {
                    removeVenueFromLegend(currentVenueSeries);
                    event.consume();
                    return;
                }

                if (event.getClickCount() == 1) {
                    selectVenueFromLegend(currentVenueSeries);
                    event.consume();
                }
            });

            legendFlow.getChildren().add(legendItem);
            index++;
        }

        legendBox.setVisible(true);
        legendBox.setManaged(true);
    }

    private void selectVenueFromLegend(VenueAnalysisService.VenueChartSeries venueSeries) {
        if (venueSeries == null || selectedVenuesListView == null) {
            return;
        }

        for (VenueAnalysisService.SelectedVenue selectedVenue : selectedVenuesListView.getItems()) {
            boolean sameType = selectedVenue.type().equals(venueSeries.type());
            boolean sameId = venueAnalysisService.getVenueId(selectedVenue.venue()) == venueSeries.venueId();

            if (sameType && sameId) {
                selectedVenuesListView.getSelectionModel().select(selectedVenue);
                selectedVenuesListView.scrollTo(selectedVenue);
                return;
            }
        }
    }

    private void removeVenueFromLegend(VenueAnalysisService.VenueChartSeries venueSeries) {
        if (venueSeries == null || selectedVenuesListView == null) {
            return;
        }

        VenueAnalysisService.SelectedVenue venueToRemove = null;

        for (VenueAnalysisService.SelectedVenue selectedVenue : selectedVenuesListView.getItems()) {
            boolean sameType = selectedVenue.type().equals(venueSeries.type());
            boolean sameId = venueAnalysisService.getVenueId(selectedVenue.venue()) == venueSeries.venueId();

            if (sameType && sameId) {
                venueToRemove = selectedVenue;
                break;
            }
        }

        if (venueToRemove == null) {
            return;
        }

        selectedVenuesListView.getItems().remove(venueToRemove);
        setActiveVenueHighlight(null);

        if (selectedVenuesListView.getItems().isEmpty()) {
            clearCharts();
        } else {
            loadVenueAnalysis();
        }
    }

    private VenueAnalysisService.YearRange getSelectedYearRange() {
        Integer startYear = getComboBoxYearValue(fromYearComboBox);
        Integer endYear = getComboBoxYearValue(toYearComboBox);

        return venueAnalysisService.validateYearRange(startYear, endYear);
    }

    private Integer getComboBoxYearValue(ComboBox<Integer> comboBox) {
        if (comboBox == null) {
            return null;
        }

        return comboBox.getValue();
    }

    private String getSelectedVenueType() {
        if (venueTypeComboBox == null) {
            return null;
        }

        return venueTypeComboBox.getValue();
    }

    private String shortenSeriesName(String name) {
        if (name == null || name.isBlank()) {
            return "-";
        }

        if (name.length() <= 45) {
            return name;
        }

        return name.substring(0, 42) + "...";
    }

    private void setLoading(boolean loading) {
        if (venueTypeComboBox != null) {
            venueTypeComboBox.setDisable(loading);
        }

        if (venueSearchField != null) {
            venueSearchField.setDisable(loading);
        }

        if (addSelectedButton != null) {
            addSelectedButton.setDisable(loading);
        }

        if (removeSelectedButton != null) {
            removeSelectedButton.setDisable(loading);
        }

        if (clearButton != null) {
            clearButton.setDisable(loading);
        }

        if (loadChartButton != null) {
            loadChartButton.setDisable(loading);
        }

        if (metricComboBox != null) {
            metricComboBox.setDisable(loading);
        }

        if (fromYearComboBox != null) {
            fromYearComboBox.setDisable(loading);
        }

        if (toYearComboBox != null) {
            toYearComboBox.setDisable(loading);
        }

        if (searchResultsListView != null) {
            searchResultsListView.setDisable(loading);
        }

        if (selectedVenuesListView != null) {
            selectedVenuesListView.setDisable(loading);
        }

        if (comparisonLineChart != null) {
            comparisonLineChart.setDisable(loading);
        }

        if (totalArticlesBarChart != null) {
            totalArticlesBarChart.setDisable(loading);
        }

        if (avgArticlesBarChart != null) {
            avgArticlesBarChart.setDisable(loading);
        }

        if (avgAuthorEntriesBarChart != null) {
            avgAuthorEntriesBarChart.setDisable(loading);
        }
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
        alert.setContentText(message == null || message.isBlank() ? "Unknown error." : message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.isBlank() ? "-" : message);
        alert.showAndWait();
    }

    private record ChartHighlightHandle(
            String venueKey,
            Runnable normalStyle,
            Runnable highlightedStyle
    ) {
    }

    private record LegendHighlightHandle(
            String venueKey,
            HBox legendItem,
            Region colorDot,
            Label nameLabel,
            String color
    ) {
    }
}