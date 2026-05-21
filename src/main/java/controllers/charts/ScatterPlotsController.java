package controllers.charts;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import service.charts.ScatterPlotsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScatterPlotsController {

    private static final int MIN_YEAR = ScatterPlotsService.DEFAULT_MIN_YEAR;
    private static final int SEARCH_LIMIT = 20;
    private static final int MAX_SELECTED_VENUES = 10;
    private static final int MIN_SEARCH_LENGTH = 3;
    private static final int SEARCH_DEBOUNCE_MS = 250;
    private static final int DEFAULT_RANKING_LIMIT = 100;

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

    private final ScatterPlotsService scatterPlotsService = new ScatterPlotsService();

    private Object selectedSearchVenue;
    private PauseTransition searchDebounce;
    private Task<List<Object>> venueSearchTask;

    private String activeVenueKey;

    private final List<ChartHighlightHandle> chartHighlightHandles = new ArrayList<>();
    private final List<LegendHighlightHandle> legendHighlightHandles = new ArrayList<>();

    @FXML
    private ScrollPane scatterPageScrollPane;

    @FXML
    private ComboBox<String> scatterVenueTypeComboBox;

    @FXML
    private TextField scatterVenueSearchField;

    @FXML
    private Button scatterAddSelectedButton;

    @FXML
    private Button scatterRemoveSelectedButton;

    @FXML
    private Button scatterClearSelectedButton;

    @FXML
    private Button scatterLoadVenueButton;

    @FXML
    private VBox scatterSearchResultsContainer;

    @FXML
    private ListView<Object> scatterSearchResultsListView;

    @FXML
    private ListView<ScatterPlotsService.SelectedVenue> scatterSelectedVenuesListView;

    @FXML
    private ComboBox<Integer> scatterFromYearComboBox;

    @FXML
    private ComboBox<Integer> scatterToYearComboBox;

    @FXML
    private Label venueScatterStatusLabel;

    @FXML
    private ScatterChart<Number, Number> venueScatterChart;

    @FXML
    private NumberAxis scatterArticlesAxis;

    @FXML
    private NumberAxis scatterAuthorsAxis;

    @FXML
    private VBox venueScatterLegendBox;

    @FXML
    private FlowPane venueScatterLegendFlow;

    @FXML
    private ComboBox<String> rankingXMetricComboBox;

    @FXML
    private ComboBox<String> rankingYMetricComboBox;

    @FXML
    private ComboBox<String> rankingLimitComboBox;

    @FXML
    private Button loadRankingScatterButton;

    @FXML
    private Button clearRankingScatterButton;

    @FXML
    private Label rankingStatusLabel;

    @FXML
    private ScatterChart<Number, Number> journalRankingScatterChart;

    @FXML
    private NumberAxis rankingXAxis;

    @FXML
    private NumberAxis rankingYAxis;

    @FXML
    public void initialize() {
        setupVenueTypeComboBox();
        setupYearComboBoxes();
        setupSearchField();
        setupSearchResultsListView();
        setupSelectedVenuesListView();
        setupVenueScatterChart();

        setupRankingControls();
        setupRankingScatterChart();

        selectedSearchVenue = null;
        clearSearchResults();
        clearVenueScatterOnly();
        clearRankingScatterOnly();

        setVenueStatus("Choose journals/conferences and load venue scatter.");
        setRankingStatus("Choose ranking metrics and top N limit, then load journal ranking scatter.");
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
        String searchText = scatterVenueSearchField == null ? "" : scatterVenueSearchField.getText().trim();

        stopCurrentVenueSearchTaskOnly();

        if (venueType == null || venueType.isBlank()) {
            clearSearchResults();

            if (showAlerts) {
                showError("No venue type selected", "You must select Journal or Conference first.");
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
                return scatterPlotsService.searchVenues(
                        requestedVenueType,
                        requestedSearchText,
                        SEARCH_LIMIT
                );
            }
        };

        venueSearchTask = task;

        if (showAlerts) {
            setVenueLoading(true);
        }

        task.setOnSucceeded(event -> {
            if (showAlerts) {
                setVenueLoading(false);
            }

            if (task.isCancelled()) {
                return;
            }

            String currentSearchText = scatterVenueSearchField == null ? "" : scatterVenueSearchField.getText().trim();
            String currentVenueType = getSelectedVenueType();

            if (!requestedSearchText.equals(currentSearchText)) {
                return;
            }

            if (!requestedVenueType.equals(currentVenueType)) {
                return;
            }

            List<Object> results = task.getValue();
            renderSearchResults(results);

            if (results == null || results.isEmpty()) {
                if (showAlerts) {
                    showInfo(
                            "No results found",
                            "No journal or conference was found for this search text."
                    );
                }
            }
        });

        task.setOnFailed(event -> {
            if (showAlerts) {
                setVenueLoading(false);
            }

            Throwable exception = task.getException();

            if (showAlerts) {
                showError("Search failed", exception == null ? null : exception.getMessage());
            } else {
                clearSearchResults();
            }
        });

        startBackgroundTask(task, "scatter-venue-search-task");
    }

    @FXML
    private void addSelectedVenue() {
        addSelectedVenueFromSearch(selectedSearchVenue, true);
    }

    private void addSelectedVenueFromSearch(Object venue, boolean showMessages) {
        if (scatterSelectedVenuesListView == null) {
            return;
        }

        if (venue == null) {
            if (showMessages) {
                showError(
                        "No venue selected",
                        "You must select a result from the list first."
                );
            }

            return;
        }

        String venueType = getSelectedVenueType();

        if (venueType == null || venueType.isBlank()) {
            if (showMessages) {
                showError("No venue type selected", "You must select Journal or Conference first.");
            }

            return;
        }

        if (scatterSelectedVenuesListView.getItems().size() >= MAX_SELECTED_VENUES) {
            if (showMessages) {
                showError(
                        "Too many venues",
                        "To keep the scatter plot readable, you can select up to "
                                + MAX_SELECTED_VENUES + " venues."
                );
            }

            return;
        }

        ScatterPlotsService.SelectedVenue selectedVenue;

        try {
            selectedVenue = scatterPlotsService.createSelectedVenue(venueType, venue);
        } catch (IllegalArgumentException exception) {
            if (showMessages) {
                showError("Invalid venue", exception.getMessage());
            }

            return;
        }

        if (scatterPlotsService.alreadySelected(scatterSelectedVenuesListView.getItems(), selectedVenue)) {
            if (showMessages) {
                showInfo("Already selected", "This venue already exists in the selected list.");
            }

            return;
        }

        scatterSelectedVenuesListView.getItems().add(selectedVenue);

        if (scatterSearchResultsListView != null) {
            scatterSearchResultsListView.getSelectionModel().clearSelection();
        }

        selectedSearchVenue = null;
    }

    @FXML
    private void removeSelectedVenue() {
        if (scatterSelectedVenuesListView == null) {
            return;
        }

        ScatterPlotsService.SelectedVenue selected =
                scatterSelectedVenuesListView.getSelectionModel().getSelectedItem();

        if (selected != null) {
            scatterSelectedVenuesListView.getItems().remove(selected);
        }
    }

    @FXML
    private void clearVenueScatter() {
        stopCurrentVenueSearch();

        if (scatterVenueSearchField != null) {
            scatterVenueSearchField.clear();
        }

        clearSearchResults();

        if (scatterSelectedVenuesListView != null) {
            scatterSelectedVenuesListView.getItems().clear();
        }

        if (scatterFromYearComboBox != null) {
            scatterFromYearComboBox.getSelectionModel().clearSelection();
        }

        if (scatterToYearComboBox != null) {
            scatterToYearComboBox.getSelectionModel().clearSelection();
            refreshToYearOptions(null);
        }

        selectedSearchVenue = null;
        clearVenueScatterOnly();
        setVenueStatus("Choose journals/conferences and load venue scatter.");
    }

    @FXML
    private void loadVenueScatter() {
        if (scatterSelectedVenuesListView == null || scatterSelectedVenuesListView.getItems().isEmpty()) {
            showError("No selected venues", "You must add at least one journal or conference.");
            return;
        }

        ScatterPlotsService.YearRange yearRange;

        try {
            yearRange = getSelectedYearRange();
        } catch (IllegalArgumentException exception) {
            showError("Invalid years", exception.getMessage());
            return;
        }

        List<ScatterPlotsService.SelectedVenue> selectedVenues =
                new ArrayList<>(scatterSelectedVenuesListView.getItems());

        final double scrollBeforeLoad = getPageScrollValue();

        Task<List<ScatterPlotsService.VenueScatterSeries>> task = new Task<>() {
            @Override
            protected List<ScatterPlotsService.VenueScatterSeries> call() {
                return scatterPlotsService.loadVenueScatterSeries(selectedVenues, yearRange);
            }
        };

        setVenueLoading(true);
        setVenueStatus("Loading venue scatter plot...");

        task.setOnSucceeded(event -> {
            setVenueLoading(false);

            List<ScatterPlotsService.VenueScatterSeries> chartData = task.getValue();

            if (chartData == null || chartData.isEmpty()) {
                clearVenueScatterOnly();
                setVenueStatus("No venue scatter data found.");
                restorePageScrollValue(scrollBeforeLoad);
                return;
            }

            updateVenueScatterChart(chartData);
            setVenueStatus("Loaded " + chartData.size() + " venue series.");
            restorePageScrollValue(scrollBeforeLoad);
        });

        task.setOnFailed(event -> {
            setVenueLoading(false);
            Throwable exception = task.getException();
            setVenueStatus("Failed to load venue scatter plot.");
            restorePageScrollValue(scrollBeforeLoad);
            showError("Failed to load scatter plot", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(task, "scatter-venue-load-task");
    }

    @FXML
    private void loadRankingScatter() {
        String xMetric = rankingXMetricComboBox == null ? null : rankingXMetricComboBox.getValue();
        String yMetric = rankingYMetricComboBox == null ? null : rankingYMetricComboBox.getValue();
        Integer requestedLimit = getSelectedRankingLimit();

        try {
            scatterPlotsService.validateRankingMetrics(xMetric, yMetric);
        } catch (IllegalArgumentException exception) {
            showError("Invalid ranking metrics", exception.getMessage());
            return;
        }

        Task<List<ScatterPlotsService.RankingScatterPoint>> task = new Task<>() {
            @Override
            protected List<ScatterPlotsService.RankingScatterPoint> call() {
                return scatterPlotsService.loadJournalRankingScatterData(
                        xMetric,
                        yMetric,
                        requestedLimit
                );
            }
        };

        setRankingLoading(true);
        setRankingStatus("Loading journal ranking scatter...");

        task.setOnSucceeded(event -> {
            setRankingLoading(false);

            List<ScatterPlotsService.RankingScatterPoint> points = task.getValue();

            if (points == null || points.isEmpty()) {
                clearRankingScatterOnly();
                setRankingStatus("No ranking scatter data found.");
                showInfo(
                        "No data found",
                        "No journals were found with values for both selected metrics."
                );
                return;
            }

            updateRankingScatterChart(points, xMetric, yMetric);

            if (requestedLimit == null) {
                setRankingStatus("Loaded all " + points.size() + " journal points.");
            } else {
                setRankingStatus("Loaded top " + points.size() + " journal points.");
            }
        });

        task.setOnFailed(event -> {
            setRankingLoading(false);
            Throwable exception = task.getException();
            setRankingStatus("Failed to load ranking scatter.");
            showError("Failed to load ranking scatter", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(task, "ranking-scatter-load-task");
    }

    @FXML
    private void clearRankingScatter() {
        if (rankingXMetricComboBox != null) {
            rankingXMetricComboBox.getSelectionModel().select("Total Docs");
        }

        if (rankingYMetricComboBox != null) {
            rankingYMetricComboBox.getSelectionModel().select("Cites / Doc 2y");
        }

        if (rankingLimitComboBox != null) {
            rankingLimitComboBox.getSelectionModel().select(String.valueOf(DEFAULT_RANKING_LIMIT));
        }

        clearRankingScatterOnly();
        setRankingStatus("Choose ranking metrics and top N limit, then load journal ranking scatter.");
    }

    private void setupVenueTypeComboBox() {
        if (scatterVenueTypeComboBox == null) {
            return;
        }

        scatterVenueTypeComboBox.getItems().setAll(scatterPlotsService.getVenueTypes());
        scatterVenueTypeComboBox.getSelectionModel().select(ScatterPlotsService.TYPE_JOURNAL);

        scatterVenueTypeComboBox.setOnAction(event -> {
            stopCurrentVenueSearch();

            selectedSearchVenue = null;
            clearSearchResults();

            String searchText = scatterVenueSearchField == null ? "" : scatterVenueSearchField.getText().trim();

            if (searchText.length() >= MIN_SEARCH_LENGTH && searchDebounce != null) {
                searchDebounce.playFromStart();
            }
        });
    }

    private void setupYearComboBoxes() {
        setupYearComboBox(scatterFromYearComboBox);
        setupYearComboBox(scatterToYearComboBox);

        if (scatterFromYearComboBox != null) {
            scatterFromYearComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                    refreshToYearOptions(newValue)
            );
        }
    }

    private void setupYearComboBox(ComboBox<Integer> comboBox) {
        if (comboBox == null) {
            return;
        }

        comboBox.setEditable(false);
        comboBox.getItems().setAll(scatterPlotsService.buildYearList(MIN_YEAR));
    }

    private void refreshToYearOptions(Integer fromYear) {
        if (scatterToYearComboBox == null) {
            return;
        }

        Integer currentToYear = scatterToYearComboBox.getValue();

        scatterToYearComboBox.getItems().setAll(scatterPlotsService.buildYearList(fromYear));

        if (currentToYear != null && fromYear != null && currentToYear < fromYear) {
            scatterToYearComboBox.getSelectionModel().clearSelection();
            return;
        }

        if (currentToYear != null && scatterToYearComboBox.getItems().contains(currentToYear)) {
            scatterToYearComboBox.getSelectionModel().select(currentToYear);
        }
    }

    private void setupSearchField() {
        searchDebounce = new PauseTransition(Duration.millis(SEARCH_DEBOUNCE_MS));
        searchDebounce.setOnFinished(event -> searchVenuesRealtime());

        if (scatterVenueSearchField == null) {
            return;
        }

        scatterVenueSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (searchDebounce != null) {
                searchDebounce.playFromStart();
            }
        });

        scatterVenueSearchField.setOnAction(event -> searchVenues());
    }

    private void setupSearchResultsListView() {
        if (scatterSearchResultsListView == null) {
            return;
        }

        scatterSearchResultsListView.setPlaceholder(new Label("Search results will appear here."));

        scatterSearchResultsListView.setCellFactory(listView -> {
            ListCell<Object> cell = new ListCell<>() {
                @Override
                protected void updateItem(Object venue, boolean empty) {
                    super.updateItem(venue, empty);

                    if (empty || venue == null) {
                        setText(null);
                    } else {
                        setText(scatterPlotsService.getRawVenueDisplayName(venue));
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

        scatterSearchResultsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> selectedSearchVenue = selected
        );
    }

    private void setupSelectedVenuesListView() {
        if (scatterSelectedVenuesListView == null) {
            return;
        }

        scatterSelectedVenuesListView.setPlaceholder(new Label("Selected venues will appear here."));

        scatterSelectedVenuesListView.setCellFactory(listView -> {
            ListCell<ScatterPlotsService.SelectedVenue> cell = new ListCell<>() {
                @Override
                protected void updateItem(ScatterPlotsService.SelectedVenue selectedVenue, boolean empty) {
                    super.updateItem(selectedVenue, empty);

                    if (empty || selectedVenue == null) {
                        setText(null);
                    } else {
                        setText(scatterPlotsService.getVenueDisplayName(selectedVenue));
                    }
                }
            };

            cell.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2
                        && !cell.isEmpty()
                        && cell.getItem() != null) {

                    scatterSelectedVenuesListView.getItems().remove(cell.getItem());
                }
            });

            return cell;
        });
    }

    private void setupVenueScatterChart() {
        if (venueScatterChart != null) {
            venueScatterChart.setAnimated(false);
            venueScatterChart.setLegendVisible(false);
            venueScatterChart.setTitle("");
        }

        if (scatterArticlesAxis != null) {
            scatterArticlesAxis.setAutoRanging(false);
            scatterArticlesAxis.setForceZeroInRange(true);
            scatterArticlesAxis.setMinorTickVisible(false);
            scatterArticlesAxis.setLabel("Articles / year");
        }

        if (scatterAuthorsAxis != null) {
            scatterAuthorsAxis.setAutoRanging(false);
            scatterAuthorsAxis.setForceZeroInRange(true);
            scatterAuthorsAxis.setMinorTickVisible(false);
            scatterAuthorsAxis.setLabel("Avg authors / article");
        }
    }

    private void setupRankingControls() {
        if (rankingXMetricComboBox != null) {
            rankingXMetricComboBox.getItems().setAll(scatterPlotsService.getRankingMetrics());
            rankingXMetricComboBox.getSelectionModel().select("Total Docs");
        }

        if (rankingYMetricComboBox != null) {
            rankingYMetricComboBox.getItems().setAll(scatterPlotsService.getRankingMetrics());
            rankingYMetricComboBox.getSelectionModel().select("Cites / Doc 2y");
        }

        if (rankingLimitComboBox != null) {
            rankingLimitComboBox.getItems().setAll("All", "50", "100", "150", "300", "500", "1000");
            rankingLimitComboBox.getSelectionModel().select(String.valueOf(DEFAULT_RANKING_LIMIT));
        }
    }

    private void setupRankingScatterChart() {
        if (journalRankingScatterChart != null) {
            journalRankingScatterChart.setAnimated(false);
            journalRankingScatterChart.setLegendVisible(false);
            journalRankingScatterChart.setTitle("");
        }

        if (rankingXAxis != null) {
            rankingXAxis.setAutoRanging(false);
            rankingXAxis.setForceZeroInRange(true);
            rankingXAxis.setMinorTickVisible(false);
            rankingXAxis.setLabel("X metric");
        }

        if (rankingYAxis != null) {
            rankingYAxis.setAutoRanging(false);
            rankingYAxis.setForceZeroInRange(true);
            rankingYAxis.setMinorTickVisible(false);
            rankingYAxis.setLabel("Y metric");
        }
    }

    private void renderSearchResults(List<Object> results) {
        if (scatterSearchResultsListView == null) {
            return;
        }

        scatterSearchResultsListView.getItems().clear();
        scatterSearchResultsListView.getSelectionModel().clearSelection();
        selectedSearchVenue = null;

        setSearchResultsVisible(true);

        if (results == null || results.isEmpty()) {
            scatterSearchResultsListView.setPlaceholder(new Label("No matching results"));
            return;
        }

        scatterSearchResultsListView.setPlaceholder(new Label("No matching results"));
        scatterSearchResultsListView.getItems().setAll(results);
    }

    private void setSearchResultsVisible(boolean visible) {
        if (scatterSearchResultsContainer != null) {
            scatterSearchResultsContainer.setVisible(visible);
            scatterSearchResultsContainer.setManaged(visible);
        }
    }

    private void clearSearchResults() {
        clearSearchResultsOnly();
        setSearchResultsVisible(false);
    }

    private void clearSearchResultsOnly() {
        if (scatterSearchResultsListView != null) {
            scatterSearchResultsListView.getItems().clear();
            scatterSearchResultsListView.getSelectionModel().clearSelection();
            scatterSearchResultsListView.setPlaceholder(new Label("Search results will appear here."));
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

    private void updateVenueScatterChart(List<ScatterPlotsService.VenueScatterSeries> allSeries) {
        clearVenueScatterOnly();

        if (allSeries == null || allSeries.isEmpty()) {
            return;
        }

        double maxArticlesPerYear = 0;
        double maxAvgAuthorsPerArticle = 0;
        int seriesIndex = 0;
        int pointCount = 0;

        for (ScatterPlotsService.VenueScatterSeries venueSeries : allSeries) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(shortenName(venueSeries.name(), 45));

            for (ScatterPlotsService.VenueScatterPoint point : venueSeries.points()) {
                maxArticlesPerYear = Math.max(maxArticlesPerYear, point.articlesPerYear());
                maxAvgAuthorsPerArticle = Math.max(maxAvgAuthorsPerArticle, point.avgAuthorsPerArticle());

                XYChart.Data<Number, Number> dataPoint =
                        new XYChart.Data<>(point.articlesPerYear(), point.avgAuthorsPerArticle());

                dataPoint.setExtraValue(point);
                series.getData().add(dataPoint);
                pointCount++;
            }

            if (!series.getData().isEmpty() && venueScatterChart != null) {
                venueScatterChart.getData().add(series);

                final int colorIndex = seriesIndex;
                final ScatterPlotsService.VenueScatterSeries currentVenueSeries = venueSeries;
                runAfterChartRender(() -> applyVenueScatterColor(series, colorIndex, currentVenueSeries));
            }

            seriesIndex++;
        }

        configureValueAxis(scatterArticlesAxis, maxArticlesPerYear);
        configureValueAxis(scatterAuthorsAxis, maxAvgAuthorsPerArticle);

        if (venueScatterChart != null) {
            venueScatterChart.setTitle("Articles / year vs average authors / article");
        }

        updateVenueLegend(allSeries);
        setVenueStatus("Loaded " + pointCount + " yearly points.");
    }

    private void clearVenueScatterOnly() {
        activeVenueKey = null;
        chartHighlightHandles.clear();
        legendHighlightHandles.clear();

        if (venueScatterChart != null) {
            venueScatterChart.getData().clear();
            venueScatterChart.setTitle("");
        }

        configureValueAxis(scatterArticlesAxis, 10);
        configureValueAxis(scatterAuthorsAxis, 10);

        updateVenueLegend(null);
    }

    private void updateRankingScatterChart(
            List<ScatterPlotsService.RankingScatterPoint> points,
            String xMetric,
            String yMetric
    ) {
        clearRankingScatterOnly();

        if (points == null || points.isEmpty() || journalRankingScatterChart == null) {
            return;
        }

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Journals");

        double maxX = 0;
        double maxY = 0;

        for (ScatterPlotsService.RankingScatterPoint point : points) {
            maxX = Math.max(maxX, point.xValue());
            maxY = Math.max(maxY, point.yValue());

            XYChart.Data<Number, Number> dataPoint =
                    new XYChart.Data<>(point.xValue(), point.yValue());

            dataPoint.setExtraValue(point);
            series.getData().add(dataPoint);
        }

        journalRankingScatterChart.getData().add(series);

        configureValueAxis(rankingXAxis, maxX);
        configureValueAxis(rankingYAxis, maxY);

        if (rankingXAxis != null) {
            rankingXAxis.setLabel(xMetric);
        }

        if (rankingYAxis != null) {
            rankingYAxis.setLabel(yMetric);
        }

        journalRankingScatterChart.setTitle(xMetric + " vs " + yMetric);

        runAfterChartRender(() -> applyRankingScatterStyle(series));
    }

    private void clearRankingScatterOnly() {
        if (journalRankingScatterChart != null) {
            journalRankingScatterChart.getData().clear();
            journalRankingScatterChart.setTitle("");
        }

        configureValueAxis(rankingXAxis, 10);
        configureValueAxis(rankingYAxis, 10);

        if (rankingXAxis != null) {
            rankingXAxis.setLabel("X metric");
        }

        if (rankingYAxis != null) {
            rankingYAxis.setLabel("Y metric");
        }
    }

    private void applyRankingScatterStyle(XYChart.Series<Number, Number> series) {
        if (series == null) {
            return;
        }

        String color = "#1f5fa8";

        for (XYChart.Data<Number, Number> data : series.getData()) {
            Node node = data.getNode();

            if (node == null) {
                continue;
            }

            node.setStyle(getScatterStyle(color, false));
            node.setCursor(Cursor.HAND);

            Object extraValue = data.getExtraValue();

            if (extraValue instanceof ScatterPlotsService.RankingScatterPoint point) {
                Tooltip.install(
                        node,
                        new Tooltip(
                                point.journalName()
                                        + "\n" + point.xMetric() + ": " + formatNumber(point.xValue())
                                        + "\n" + point.yMetric() + ": " + formatNumber(point.yValue())
                        )
                );
            }

            node.setOnMouseEntered(event -> {
                node.setStyle(getScatterStyle(color, true));
                node.toFront();
                event.consume();
            });

            node.setOnMouseExited(event -> {
                node.setStyle(getScatterStyle(color, false));
                event.consume();
            });
        }
    }

    private void applyVenueScatterColor(
            XYChart.Series<Number, Number> series,
            int colorIndex,
            ScatterPlotsService.VenueScatterSeries venueSeries
    ) {
        if (series == null || venueSeries == null) {
            return;
        }

        String color = getChartColor(colorIndex);
        String venueKey = scatterPlotsService.getVenueKey(venueSeries);

        Runnable normalStyle = () -> {
            for (XYChart.Data<Number, Number> data : series.getData()) {
                Node scatterNode = data.getNode();

                if (scatterNode != null) {
                    scatterNode.setStyle(getScatterStyle(color, false));

                    Object extraValue = data.getExtraValue();

                    if (extraValue instanceof ScatterPlotsService.VenueScatterPoint point) {
                        Tooltip.install(
                                scatterNode,
                                new Tooltip(
                                        point.venueName()
                                                + "\nYear: " + point.year()
                                                + "\nArticles: " + formatNumber(point.articlesPerYear())
                                                + "\nAvg authors/article: " + formatNumber(point.avgAuthorsPerArticle())
                                )
                        );
                    }
                }
            }
        };

        Runnable highlightedStyle = () -> {
            for (XYChart.Data<Number, Number> data : series.getData()) {
                Node scatterNode = data.getNode();

                if (scatterNode != null) {
                    scatterNode.setStyle(getScatterStyle(color, true));
                    scatterNode.toFront();
                }
            }
        };

        normalStyle.run();

        chartHighlightHandles.add(
                new ChartHighlightHandle(
                        venueKey,
                        normalStyle,
                        highlightedStyle
                )
        );

        for (XYChart.Data<Number, Number> data : series.getData()) {
            setupChartHover(data.getNode(), venueKey);
        }

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

    private void updateVenueLegend(List<ScatterPlotsService.VenueScatterSeries> allSeries) {
        legendHighlightHandles.clear();

        if (venueScatterLegendBox == null || venueScatterLegendFlow == null) {
            return;
        }

        venueScatterLegendFlow.getChildren().clear();

        if (allSeries == null || allSeries.isEmpty()) {
            venueScatterLegendBox.setVisible(false);
            venueScatterLegendBox.setManaged(false);
            return;
        }

        int index = 0;

        for (ScatterPlotsService.VenueScatterSeries venueSeries : allSeries) {
            String color = getChartColor(index);
            String venueKey = scatterPlotsService.getVenueKey(venueSeries);

            Region colorDot = new Region();
            colorDot.setMinSize(10, 10);
            colorDot.setPrefSize(10, 10);
            colorDot.setMaxSize(10, 10);
            colorDot.setStyle(getLegendDotStyle(color, false));

            Label nameLabel = new Label(shortenName(venueSeries.name(), 45));
            nameLabel.setStyle(getLegendLabelStyle(false));

            Tooltip.install(nameLabel, new Tooltip(venueSeries.name()));

            HBox legendItem = new HBox(6, colorDot, nameLabel);
            legendItem.setAlignment(Pos.CENTER_LEFT);
            legendItem.setCursor(Cursor.HAND);
            legendItem.setStyle(getLegendItemStyle(color, false));

            legendItem.setOnMouseEntered(event -> setActiveVenueHighlight(venueKey));
            legendItem.setOnMouseExited(event -> setActiveVenueHighlight(null));

            legendItem.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2) {

                    removeVenueFromLegendAndReload(venueSeries);
                    event.consume();
                }
            });

            LegendHighlightHandle handle = new LegendHighlightHandle(
                    venueKey,
                    legendItem,
                    colorDot,
                    nameLabel,
                    color
            );

            legendHighlightHandles.add(handle);
            venueScatterLegendFlow.getChildren().add(legendItem);

            index++;
        }

        venueScatterLegendBox.setVisible(true);
        venueScatterLegendBox.setManaged(true);

        applyActiveVenueHighlight();
    }

    private void removeVenueFromLegendAndReload(ScatterPlotsService.VenueScatterSeries venueSeries) {
        if (venueSeries == null || scatterSelectedVenuesListView == null) {
            return;
        }

        boolean removed = scatterSelectedVenuesListView.getItems().removeIf(selectedVenue ->
                selectedVenue.type().equals(venueSeries.type())
                        && scatterPlotsService.getVenueId(selectedVenue.venue()) == venueSeries.venueId()
        );

        if (!removed) {
            return;
        }

        if (scatterSelectedVenuesListView.getItems().isEmpty()) {
            clearVenueScatterOnly();
            setVenueStatus("No selected venues. Add journals/conferences and load venue scatter.");
            return;
        }

        loadVenueScatter();
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

    private String getScatterStyle(String color, boolean highlighted) {
        if (highlighted) {
            return "-fx-background-color: " + color + ", white;" +
                    "-fx-background-insets: 0, 3;" +
                    "-fx-background-radius: 11px;" +
                    "-fx-padding: 8px;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 8, 0.25, 0, 0);";
        }

        return "-fx-background-color: " + color + ", white;" +
                "-fx-background-insets: 0, 2;" +
                "-fx-background-radius: 8px;" +
                "-fx-padding: 5px;";
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

    private ScatterPlotsService.YearRange getSelectedYearRange() {
        Integer startYear = scatterFromYearComboBox == null ? null : scatterFromYearComboBox.getValue();
        Integer endYear = scatterToYearComboBox == null ? null : scatterToYearComboBox.getValue();

        return scatterPlotsService.validateYearRange(startYear, endYear);
    }

    private Integer getSelectedRankingLimit() {
        String selectedLimit = rankingLimitComboBox == null ? null : rankingLimitComboBox.getValue();
        return scatterPlotsService.resolveRankingLimit(selectedLimit, DEFAULT_RANKING_LIMIT);
    }

    private String getSelectedVenueType() {
        if (scatterVenueTypeComboBox == null) {
            return null;
        }

        return scatterVenueTypeComboBox.getValue();
    }

    private String shortenName(String name, int maxLength) {
        if (name == null || name.isBlank()) {
            return "-";
        }

        if (name.length() <= maxLength) {
            return name;
        }

        return name.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.round(value)) < 0.000001) {
            return String.valueOf(Math.round(value));
        }

        return String.format(Locale.US, "%.2f", value);
    }

    private double getPageScrollValue() {
        if (scatterPageScrollPane == null) {
            return 0;
        }

        return scatterPageScrollPane.getVvalue();
    }

    private void restorePageScrollValue(double value) {
        if (scatterPageScrollPane == null) {
            return;
        }

        Platform.runLater(() -> {
            scatterPageScrollPane.setVvalue(value);
            Platform.runLater(() -> scatterPageScrollPane.setVvalue(value));
        });
    }

    private void setVenueLoading(boolean loading) {
        if (scatterVenueTypeComboBox != null) {
            scatterVenueTypeComboBox.setDisable(loading);
        }

        if (scatterVenueSearchField != null) {
            scatterVenueSearchField.setDisable(loading);
        }

        if (scatterAddSelectedButton != null) {
            scatterAddSelectedButton.setDisable(loading);
        }

        if (scatterRemoveSelectedButton != null) {
            scatterRemoveSelectedButton.setDisable(loading);
        }

        if (scatterClearSelectedButton != null) {
            scatterClearSelectedButton.setDisable(loading);
        }

        if (scatterLoadVenueButton != null) {
            scatterLoadVenueButton.setDisable(loading);
        }

        if (scatterFromYearComboBox != null) {
            scatterFromYearComboBox.setDisable(loading);
        }

        if (scatterToYearComboBox != null) {
            scatterToYearComboBox.setDisable(loading);
        }

        if (scatterSearchResultsListView != null) {
            scatterSearchResultsListView.setDisable(loading);
        }

        if (scatterSelectedVenuesListView != null) {
            scatterSelectedVenuesListView.setDisable(loading);
        }

        if (venueScatterChart != null) {
            venueScatterChart.setDisable(loading);
        }
    }

    private void setRankingLoading(boolean loading) {
        if (rankingXMetricComboBox != null) {
            rankingXMetricComboBox.setDisable(loading);
        }

        if (rankingYMetricComboBox != null) {
            rankingYMetricComboBox.setDisable(loading);
        }

        if (rankingLimitComboBox != null) {
            rankingLimitComboBox.setDisable(loading);
        }

        if (loadRankingScatterButton != null) {
            loadRankingScatterButton.setDisable(loading);
        }

        if (clearRankingScatterButton != null) {
            clearRankingScatterButton.setDisable(loading);
        }

        if (journalRankingScatterChart != null) {
            journalRankingScatterChart.setDisable(loading);
        }
    }

    private void setVenueStatus(String message) {
        if (venueScatterStatusLabel != null) {
            venueScatterStatusLabel.setText(message == null ? "" : message);
        }
    }

    private void setRankingStatus(String message) {
        if (rankingStatusLabel != null) {
            rankingStatusLabel.setText(message == null ? "" : message);
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