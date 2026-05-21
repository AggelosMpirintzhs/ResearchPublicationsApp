package controllers.charts;

import dto.chart.PublisherOptionDto;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import service.charts.PublisherAnalysisService;

import java.util.ArrayList;
import java.util.List;

public class PublisherAnalysisController {

    private static final int DEFAULT_PUBLISHER_SEARCH_LIMIT =
            PublisherAnalysisService.DEFAULT_PUBLISHER_SEARCH_LIMIT;

    private static final int MAX_SELECTED_PUBLISHERS = 10;
    private static final int MIN_SEARCH_LENGTH = 3;
    private static final int SEARCH_DEBOUNCE_MS = 250;

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

    private final PublisherAnalysisService publisherAnalysisService = new PublisherAnalysisService();

    private PublisherOptionDto selectedSearchPublisher;
    private PauseTransition searchDebounce;
    private Task<List<PublisherOptionDto>> publisherSearchTask;

    private String activeQuartileKey;
    private String activeTotalPublisherKey;

    private final List<ChartHighlightHandle> chartHighlightHandles = new ArrayList<>();
    private final List<LegendHighlightHandle> legendHighlightHandles = new ArrayList<>();

    private final List<TotalChartHighlightHandle> totalChartHighlightHandles = new ArrayList<>();
    private final List<TotalLegendHighlightHandle> totalLegendHighlightHandles = new ArrayList<>();

    @FXML
    private TextField publisherFilterField;

    @FXML
    private Button addPublisherButton;

    @FXML
    private Button removeSelectedPublisherButton;

    @FXML
    private Button clearPublisherAnalysisButton;

    @FXML
    private Button loadPublisherAnalysisButton;

    @FXML
    private Label publisherStatusLabel;

    @FXML
    private VBox publisherResultsContainer;

    @FXML
    private ListView<PublisherOptionDto> publisherResultsListView;

    @FXML
    private ListView<PublisherOptionDto> selectedPublishersListView;

    @FXML
    private BarChart<String, Number> publisherQuartileBarChart;

    @FXML
    private CategoryAxis publisherCategoryAxis;

    @FXML
    private NumberAxis publisherCountAxis;

    @FXML
    private VBox publisherLegendBox;

    @FXML
    private FlowPane publisherLegendFlow;

    @FXML
    private VBox publisherTotalChartBox;

    @FXML
    private BarChart<String, Number> publisherTotalBarChart;

    @FXML
    private CategoryAxis publisherTotalCategoryAxis;

    @FXML
    private NumberAxis publisherTotalCountAxis;

    @FXML
    private VBox publisherTotalLegendBox;

    @FXML
    private FlowPane publisherTotalLegendFlow;

    @FXML
    public void initialize() {
        setupSearchField();
        setupPublisherResultsListView();
        setupSelectedPublishersListView();
        setupCharts();

        selectedSearchPublisher = null;
        clearSearchResults();
        clearCharts();

        setStatus("Search for publishers, add up to 10, and load the analysis.");
    }

    @FXML
    private void searchPublishers() {
        executePublisherSearch(true);
    }

    private void searchPublishersRealtime() {
        executePublisherSearch(false);
    }

    private void executePublisherSearch(boolean showAlerts) {
        String searchText = publisherFilterField == null ? "" : publisherFilterField.getText().trim();
        int searchLimit = getSelectedSearchLimit();

        stopCurrentPublisherSearchTaskOnly();

        if (searchText.length() < MIN_SEARCH_LENGTH) {
            selectedSearchPublisher = null;
            clearSearchResults();

            if (showAlerts && searchText.isBlank()) {
                showError("Invalid search", "Type a publisher name first.");
            } else if (showAlerts) {
                showError("Invalid search", "Type at least " + MIN_SEARCH_LENGTH + " characters.");
            }

            return;
        }

        String requestedSearchText = searchText;

        selectedSearchPublisher = null;
        clearSearchResultsOnly();

        Task<List<PublisherOptionDto>> task = new Task<>() {
            @Override
            protected List<PublisherOptionDto> call() {
                return publisherAnalysisService.searchPublishers(requestedSearchText, searchLimit);
            }
        };

        publisherSearchTask = task;

        if (showAlerts) {
            setStatus("Searching publishers...");
        }

        task.setOnSucceeded(event -> {
            if (task.isCancelled()) {
                return;
            }

            String currentSearchText = publisherFilterField == null ? "" : publisherFilterField.getText().trim();

            if (!requestedSearchText.equals(currentSearchText)) {
                return;
            }

            List<PublisherOptionDto> results = task.getValue();

            renderSearchResults(results);

            if (results == null || results.isEmpty()) {
                if (showAlerts) {
                    showInfo("No results found", "No publisher matched the current search text.");
                }

                setStatus("No publishers found.");
                return;
            }

            setStatus("Found " + results.size() + " publishers. Select one and add it to the comparison.");
        });

        task.setOnFailed(event -> {
            Throwable exception = task.getException();

            if (showAlerts) {
                showError("Search error", exception == null ? null : exception.getMessage());
            } else {
                clearSearchResults();
            }

            setStatus("Failed to search publishers.");
        });

        startBackgroundTask(task, "publisher-analysis-search-task");
    }

    @FXML
    private void addSelectedPublisher() {
        addSelectedPublisherFromSearch(selectedSearchPublisher, true);
    }

    private void addSelectedPublisherFromSearch(PublisherOptionDto publisher, boolean showMessages) {
        if (selectedPublishersListView == null) {
            return;
        }

        if (publisher == null) {
            if (showMessages) {
                showError("No publisher selected", "Select a publisher from the search results first.");
            }

            return;
        }

        if (selectedPublishersListView.getItems().size() >= MAX_SELECTED_PUBLISHERS) {
            if (showMessages) {
                showError(
                        "Too many publishers",
                        "You can compare up to " + MAX_SELECTED_PUBLISHERS + " publishers."
                );
            }

            return;
        }

        if (publisherAnalysisService.alreadySelected(selectedPublishersListView.getItems(), publisher)) {
            if (showMessages) {
                showInfo("Already selected", "This publisher is already in the selected list.");
            }

            return;
        }

        selectedPublishersListView.getItems().add(publisher);

        if (publisherResultsListView != null) {
            publisherResultsListView.getSelectionModel().clearSelection();
        }

        selectedSearchPublisher = null;
        setStatus("Publisher added. Load the analysis when your selection is ready.");
    }

    @FXML
    private void removeSelectedPublisher() {
        if (selectedPublishersListView == null) {
            return;
        }

        PublisherOptionDto selected = selectedPublishersListView.getSelectionModel().getSelectedItem();

        if (selected != null) {
            selectedPublishersListView.getItems().remove(selected);
            setStatus("Publisher removed.");

            if (selectedPublishersListView.getItems().isEmpty()) {
                clearCharts();
            }
        }
    }

    @FXML
    private void loadPublisherAnalysis() {
        stopCurrentPublisherSearch();

        if (selectedPublishersListView == null || selectedPublishersListView.getItems().isEmpty()) {
            showError("No selected publishers", "Add at least one publisher before loading the analysis.");
            return;
        }

        List<PublisherOptionDto> selectedPublishers = new ArrayList<>(selectedPublishersListView.getItems());

        Task<List<PublisherAnalysisService.PublisherSummary>> task = new Task<>() {
            @Override
            protected List<PublisherAnalysisService.PublisherSummary> call() {
                return publisherAnalysisService.loadPublisherSummaries(selectedPublishers);
            }
        };

        setLoading(true);
        setStatus("Loading publisher analysis...");

        task.setOnSucceeded(event -> {
            setLoading(false);

            List<PublisherAnalysisService.PublisherSummary> summaries = task.getValue();

            if (summaries == null || summaries.isEmpty()) {
                clearCharts();
                setStatus("No publication data found for the selected publishers.");
                showInfo("No data found", "No publication data was found for the selected publishers.");
                return;
            }

            updateCharts(summaries);
            setStatus("Loaded analysis for " + summaries.size() + " selected publishers.");
        });

        task.setOnFailed(event -> {
            setLoading(false);
            Throwable exception = task.getException();
            setStatus("Failed to load publisher analysis.");
            showError("Loading error", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(task, "publisher-analysis-load-task");
    }

    @FXML
    private void clearPublisherAnalysis() {
        stopCurrentPublisherSearch();

        if (publisherFilterField != null) {
            publisherFilterField.clear();
        }

        clearSearchResults();

        if (selectedPublishersListView != null) {
            selectedPublishersListView.getItems().clear();
        }

        selectedSearchPublisher = null;
        clearCharts();

        setStatus("Search for publishers, add up to 10, and load the analysis.");
    }

    private void setupSearchField() {
        searchDebounce = new PauseTransition(Duration.millis(SEARCH_DEBOUNCE_MS));
        searchDebounce.setOnFinished(event -> searchPublishersRealtime());

        if (publisherFilterField == null) {
            return;
        }

        publisherFilterField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (searchDebounce != null) {
                searchDebounce.playFromStart();
            }
        });

        publisherFilterField.setOnKeyPressed(this::handleEnterAsLoad);
    }

    private void setupPublisherResultsListView() {
        if (publisherResultsListView == null) {
            return;
        }

        publisherResultsListView.setPlaceholder(new Label("Search results will appear here."));
        publisherResultsListView.setOnKeyPressed(this::handleEnterAsLoad);

        publisherResultsListView.setCellFactory(listView -> {
            ListCell<PublisherOptionDto> cell = new ListCell<>() {
                @Override
                protected void updateItem(PublisherOptionDto publisher, boolean empty) {
                    super.updateItem(publisher, empty);

                    if (empty || publisher == null) {
                        setText(null);
                    } else {
                        setText(publisherAnalysisService.formatPublisherForUi(publisher));
                    }
                }
            };

            cell.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2
                        && !cell.isEmpty()
                        && cell.getItem() != null) {

                    selectedSearchPublisher = cell.getItem();
                    addSelectedPublisherFromSearch(cell.getItem(), false);
                }
            });

            return cell;
        });

        publisherResultsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> selectedSearchPublisher = selected
        );
    }

    private void setupSelectedPublishersListView() {
        if (selectedPublishersListView == null) {
            return;
        }

        selectedPublishersListView.setPlaceholder(new Label("Selected publishers will appear here."));
        selectedPublishersListView.setOnKeyPressed(this::handleEnterAsLoad);

        selectedPublishersListView.setCellFactory(listView -> {
            ListCell<PublisherOptionDto> cell = new ListCell<>() {
                @Override
                protected void updateItem(PublisherOptionDto publisher, boolean empty) {
                    super.updateItem(publisher, empty);

                    if (empty || publisher == null) {
                        setText(null);
                    } else {
                        setText(publisherAnalysisService.formatPublisherForUi(publisher));
                    }
                }
            };

            cell.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2
                        && !cell.isEmpty()
                        && cell.getItem() != null) {

                    selectedPublishersListView.getItems().remove(cell.getItem());

                    if (selectedPublishersListView.getItems().isEmpty()) {
                        clearCharts();
                    }
                }
            });

            return cell;
        });
    }

    private void handleEnterAsLoad(KeyEvent event) {
        if (event == null || event.getCode() != KeyCode.ENTER) {
            return;
        }

        loadPublisherAnalysis();
        event.consume();
    }

    private void setupCharts() {
        setupQuartileChart();
        setupTotalChart();
    }

    private void setupQuartileChart() {
        if (publisherQuartileBarChart != null) {
            publisherQuartileBarChart.setAnimated(false);
            publisherQuartileBarChart.setLegendVisible(false);
            publisherQuartileBarChart.setCategoryGap(20);
            publisherQuartileBarChart.setBarGap(3);
            publisherQuartileBarChart.setTitle("");
        }

        if (publisherCategoryAxis != null) {
            publisherCategoryAxis.setLabel("Publisher");
            publisherCategoryAxis.setTickLabelRotation(-35);
        }

        if (publisherCountAxis != null) {
            publisherCountAxis.setLabel("Publications count");
            publisherCountAxis.setAutoRanging(false);
            publisherCountAxis.setForceZeroInRange(true);
            publisherCountAxis.setMinorTickVisible(false);
        }
    }

    private void setupTotalChart() {
        if (publisherTotalBarChart != null) {
            publisherTotalBarChart.setAnimated(false);
            publisherTotalBarChart.setLegendVisible(false);
            publisherTotalBarChart.setCategoryGap(20);
            publisherTotalBarChart.setBarGap(3);
            publisherTotalBarChart.setTitle("");
        }

        if (publisherTotalCategoryAxis != null) {
            publisherTotalCategoryAxis.setLabel("Publisher");
            publisherTotalCategoryAxis.setTickLabelRotation(-35);
        }

        if (publisherTotalCountAxis != null) {
            publisherTotalCountAxis.setLabel("Total publications");
            publisherTotalCountAxis.setAutoRanging(false);
            publisherTotalCountAxis.setForceZeroInRange(true);
            publisherTotalCountAxis.setMinorTickVisible(false);
        }

        setTotalChartVisible(false);
    }

    private int getSelectedSearchLimit() {
        return DEFAULT_PUBLISHER_SEARCH_LIMIT;
    }

    private void renderSearchResults(List<PublisherOptionDto> results) {
        if (publisherResultsListView == null) {
            return;
        }

        publisherResultsListView.getItems().clear();
        publisherResultsListView.getSelectionModel().clearSelection();
        selectedSearchPublisher = null;

        setSearchResultsVisible(true);

        if (results == null || results.isEmpty()) {
            publisherResultsListView.setPlaceholder(new Label("No matching results"));
            return;
        }

        publisherResultsListView.setPlaceholder(new Label("No matching results"));
        publisherResultsListView.getItems().setAll(results);
    }

    private void setSearchResultsVisible(boolean visible) {
        if (publisherResultsContainer != null) {
            publisherResultsContainer.setVisible(visible);
            publisherResultsContainer.setManaged(visible);
        }
    }

    private void clearSearchResults() {
        clearSearchResultsOnly();
        setSearchResultsVisible(false);
    }

    private void clearSearchResultsOnly() {
        if (publisherResultsListView != null) {
            publisherResultsListView.getItems().clear();
            publisherResultsListView.getSelectionModel().clearSelection();
            publisherResultsListView.setPlaceholder(new Label("Search results will appear here."));
        }

        selectedSearchPublisher = null;
    }

    private void stopCurrentPublisherSearch() {
        if (searchDebounce != null) {
            searchDebounce.stop();
        }

        stopCurrentPublisherSearchTaskOnly();
    }

    private void stopCurrentPublisherSearchTaskOnly() {
        if (publisherSearchTask != null && publisherSearchTask.isRunning()) {
            publisherSearchTask.cancel();
        }
    }

    private void updateCharts(List<PublisherAnalysisService.PublisherSummary> summaries) {
        clearCharts();

        if (summaries == null || summaries.isEmpty()) {
            return;
        }

        updatePublisherQuartileChart(summaries);

        boolean showTotalChart = summaries.size() > 1;

        if (showTotalChart) {
            updatePublisherTotalChart(summaries);
        } else {
            clearTotalChartOnly();
        }

        setTotalChartVisible(showTotalChart);
    }

    private void updatePublisherQuartileChart(List<PublisherAnalysisService.PublisherSummary> summaries) {
        if (publisherQuartileBarChart == null) {
            return;
        }

        List<String> publisherCategories = new ArrayList<>();

        for (PublisherAnalysisService.PublisherSummary summary : summaries) {
            publisherCategories.add(publisherAnalysisService.buildPublisherCategory(summary));
        }

        if (publisherCategoryAxis != null) {
            publisherCategoryAxis.getCategories().setAll(publisherCategories);
        }

        double maxValue = 0;

        for (String quartile : publisherAnalysisService.getQuartiles()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(quartile);

            for (PublisherAnalysisService.PublisherSummary summary : summaries) {
                String publisherCategory = publisherAnalysisService.buildPublisherCategory(summary);
                long count = summary.countForQuartile(quartile);

                maxValue = Math.max(maxValue, count);

                XYChart.Data<String, Number> dataPoint =
                        new XYChart.Data<>(publisherCategory, count);

                dataPoint.setExtraValue(
                        new BarPointInfo(
                                summary.publisherName(),
                                quartile,
                                count,
                                summary.totalPublications()
                        )
                );

                series.getData().add(dataPoint);
            }

            publisherQuartileBarChart.getData().add(series);

            int colorIndex = publisherAnalysisService.getQuartiles().indexOf(quartile);
            runAfterChartRender(() -> applyQuartileBarSeriesStyle(series, colorIndex, quartile));
        }

        configureValueAxis(publisherCountAxis, maxValue);
        publisherQuartileBarChart.setTitle("Publications per publisher and quartile");

        updateCustomLegend();
    }

    private void updatePublisherTotalChart(List<PublisherAnalysisService.PublisherSummary> summaries) {
        if (publisherTotalBarChart == null) {
            return;
        }

        clearTotalChartOnly();

        List<String> publisherCategories = new ArrayList<>();

        for (PublisherAnalysisService.PublisherSummary summary : summaries) {
            publisherCategories.add(publisherAnalysisService.buildPublisherCategory(summary));
        }

        if (publisherTotalCategoryAxis != null) {
            publisherTotalCategoryAxis.getCategories().setAll(publisherCategories);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Total publications");

        double maxValue = 0;
        int index = 0;

        for (PublisherAnalysisService.PublisherSummary summary : summaries) {
            String publisherCategory = publisherAnalysisService.buildPublisherCategory(summary);
            long totalPublications = summary.totalPublications();

            maxValue = Math.max(maxValue, totalPublications);

            XYChart.Data<String, Number> dataPoint =
                    new XYChart.Data<>(publisherCategory, totalPublications);

            dataPoint.setExtraValue(
                    new TotalBarPointInfo(
                            summary.publisherName(),
                            totalPublications,
                            publisherAnalysisService.buildTotalPublisherKey(summary)
                    )
            );

            series.getData().add(dataPoint);

            int colorIndex = index;
            PublisherAnalysisService.PublisherSummary currentSummary = summary;
            XYChart.Data<String, Number> currentDataPoint = dataPoint;

            runAfterChartRender(() ->
                    applyTotalBarDataStyle(currentDataPoint, colorIndex, currentSummary)
            );

            index++;
        }

        publisherTotalBarChart.getData().add(series);
        publisherTotalBarChart.setTitle("Total publications per publisher");

        configureValueAxis(publisherTotalCountAxis, maxValue);

        updateTotalChartLegend(summaries);
    }

    private void clearCharts() {
        activeQuartileKey = null;
        chartHighlightHandles.clear();
        legendHighlightHandles.clear();

        activeTotalPublisherKey = null;
        totalChartHighlightHandles.clear();
        totalLegendHighlightHandles.clear();

        if (publisherQuartileBarChart != null) {
            publisherQuartileBarChart.getData().clear();
            publisherQuartileBarChart.setTitle("");
        }

        if (publisherCategoryAxis != null) {
            publisherCategoryAxis.getCategories().clear();
        }

        configureValueAxis(publisherCountAxis, 10);
        updateCustomLegend();

        clearTotalChartOnly();
        setTotalChartVisible(false);
    }

    private void clearTotalChartOnly() {
        activeTotalPublisherKey = null;
        totalChartHighlightHandles.clear();
        totalLegendHighlightHandles.clear();

        if (publisherTotalBarChart != null) {
            publisherTotalBarChart.getData().clear();
            publisherTotalBarChart.setTitle("");
        }

        if (publisherTotalCategoryAxis != null) {
            publisherTotalCategoryAxis.getCategories().clear();
        }

        configureValueAxis(publisherTotalCountAxis, 10);

        if (publisherTotalLegendFlow != null) {
            publisherTotalLegendFlow.getChildren().clear();
        }

        if (publisherTotalLegendBox != null) {
            publisherTotalLegendBox.setVisible(false);
            publisherTotalLegendBox.setManaged(false);
        }
    }

    private void setTotalChartVisible(boolean visible) {
        if (publisherTotalChartBox != null) {
            publisherTotalChartBox.setVisible(visible);
            publisherTotalChartBox.setManaged(visible);
        }

        if (publisherTotalBarChart != null) {
            publisherTotalBarChart.setVisible(visible);
            publisherTotalBarChart.setManaged(visible);
        }
    }

    private void applyQuartileBarSeriesStyle(
            XYChart.Series<String, Number> series,
            int colorIndex,
            String quartile
    ) {
        if (series == null || quartile == null) {
            return;
        }

        String color = getChartColor(colorIndex);
        String quartileKey = quartile;

        Runnable normalStyle = () -> {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node barNode = data.getNode();

                if (barNode != null) {
                    barNode.setStyle(getBarStyle(color, false));

                    Object extraValue = data.getExtraValue();

                    if (extraValue instanceof BarPointInfo info) {
                        Tooltip.install(
                                barNode,
                                new Tooltip(
                                        info.publisher()
                                                + "\nQuartile: " + info.quartile()
                                                + "\nPublications in quartile: " + info.count()
                                                + "\nTotal publications: " + info.totalPublisherPublications()
                                )
                        );
                    }
                }
            }
        };

        Runnable highlightedStyle = () -> {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node barNode = data.getNode();

                if (barNode != null) {
                    barNode.setStyle(getBarStyle(color, true));
                    barNode.toFront();
                }
            }
        };

        normalStyle.run();

        chartHighlightHandles.add(
                new ChartHighlightHandle(
                        quartileKey,
                        normalStyle,
                        highlightedStyle
                )
        );

        for (XYChart.Data<String, Number> data : series.getData()) {
            setupChartHover(data.getNode(), quartileKey);
        }

        applyActiveQuartileHighlight();
    }

    private void applyTotalBarDataStyle(
            XYChart.Data<String, Number> dataPoint,
            int colorIndex,
            PublisherAnalysisService.PublisherSummary summary
    ) {
        if (dataPoint == null || dataPoint.getNode() == null || summary == null) {
            return;
        }

        String color = getChartColor(colorIndex);
        String publisherKey = publisherAnalysisService.buildTotalPublisherKey(summary);
        Node barNode = dataPoint.getNode();

        Runnable normalStyle = () -> {
            barNode.setStyle(getBarStyle(color, false));

            Object extraValue = dataPoint.getExtraValue();

            if (extraValue instanceof TotalBarPointInfo info) {
                Tooltip.install(
                        barNode,
                        new Tooltip(
                                info.publisher()
                                        + "\nTotal publications: " + info.totalPublications()
                        )
                );
            }
        };

        Runnable highlightedStyle = () -> {
            barNode.setStyle(getBarStyle(color, true));
            barNode.toFront();
        };

        normalStyle.run();

        totalChartHighlightHandles.add(
                new TotalChartHighlightHandle(
                        publisherKey,
                        normalStyle,
                        highlightedStyle
                )
        );

        setupTotalChartHover(barNode, publisherKey);
        applyActiveTotalPublisherHighlight();
    }

    private void setupChartHover(Node node, String quartileKey) {
        if (node == null || quartileKey == null || quartileKey.isBlank()) {
            return;
        }

        node.setCursor(Cursor.HAND);
        node.setMouseTransparent(false);

        node.setOnMouseEntered(event -> {
            setActiveQuartileHighlight(quartileKey);
            event.consume();
        });

        node.setOnMouseExited(event -> {
            setActiveQuartileHighlight(null);
            event.consume();
        });
    }

    private void setupTotalChartHover(Node node, String publisherKey) {
        if (node == null || publisherKey == null || publisherKey.isBlank()) {
            return;
        }

        node.setCursor(Cursor.HAND);
        node.setMouseTransparent(false);

        node.setOnMouseEntered(event -> {
            setActiveTotalPublisherHighlight(publisherKey);
            event.consume();
        });

        node.setOnMouseExited(event -> {
            setActiveTotalPublisherHighlight(null);
            event.consume();
        });
    }

    private void setActiveQuartileHighlight(String quartileKey) {
        boolean sameQuartile = activeQuartileKey == null
                ? quartileKey == null
                : activeQuartileKey.equals(quartileKey);

        if (sameQuartile) {
            return;
        }

        activeQuartileKey = quartileKey;
        applyActiveQuartileHighlight();
    }

    private void setActiveTotalPublisherHighlight(String publisherKey) {
        boolean samePublisher = activeTotalPublisherKey == null
                ? publisherKey == null
                : activeTotalPublisherKey.equals(publisherKey);

        if (samePublisher) {
            return;
        }

        activeTotalPublisherKey = publisherKey;
        applyActiveTotalPublisherHighlight();
    }

    private void applyActiveQuartileHighlight() {
        for (ChartHighlightHandle handle : chartHighlightHandles) {
            boolean highlighted = activeQuartileKey != null && activeQuartileKey.equals(handle.quartileKey());

            if (highlighted) {
                handle.highlightedStyle().run();
            } else {
                handle.normalStyle().run();
            }
        }

        for (LegendHighlightHandle handle : legendHighlightHandles) {
            boolean highlighted = activeQuartileKey != null && activeQuartileKey.equals(handle.quartileKey());
            applyLegendStyle(handle, highlighted);
        }
    }

    private void applyActiveTotalPublisherHighlight() {
        for (TotalChartHighlightHandle handle : totalChartHighlightHandles) {
            boolean highlighted = activeTotalPublisherKey != null
                    && activeTotalPublisherKey.equals(handle.publisherKey());

            if (highlighted) {
                handle.highlightedStyle().run();
            } else {
                handle.normalStyle().run();
            }
        }

        for (TotalLegendHighlightHandle handle : totalLegendHighlightHandles) {
            boolean highlighted = activeTotalPublisherKey != null
                    && activeTotalPublisherKey.equals(handle.publisherKey());

            applyTotalLegendStyle(handle, highlighted);
        }
    }

    private void updateCustomLegend() {
        legendHighlightHandles.clear();

        if (publisherLegendBox == null || publisherLegendFlow == null) {
            return;
        }

        publisherLegendFlow.getChildren().clear();

        if (publisherQuartileBarChart == null || publisherQuartileBarChart.getData().isEmpty()) {
            publisherLegendBox.setVisible(false);
            publisherLegendBox.setManaged(false);
            return;
        }

        List<String> quartiles = publisherAnalysisService.getQuartiles();

        for (int index = 0; index < quartiles.size(); index++) {
            String quartile = quartiles.get(index);
            String color = getChartColor(index);

            Region colorDot = new Region();
            colorDot.setMinSize(10, 10);
            colorDot.setPrefSize(10, 10);
            colorDot.setMaxSize(10, 10);
            colorDot.setStyle(getLegendDotStyle(color, false));

            Label nameLabel = new Label(quartile);
            nameLabel.setStyle(getLegendLabelStyle(false));

            HBox legendItem = new HBox(6, colorDot, nameLabel);
            legendItem.setAlignment(Pos.CENTER_LEFT);
            legendItem.setCursor(Cursor.HAND);
            legendItem.setStyle(getLegendItemStyle(color, false));

            Tooltip.install(legendItem, new Tooltip("Hover to highlight " + quartile + " bars."));

            legendItem.setOnMouseEntered(event -> setActiveQuartileHighlight(quartile));
            legendItem.setOnMouseExited(event -> setActiveQuartileHighlight(null));

            legendItem.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                    setActiveQuartileHighlight(quartile);
                    event.consume();
                }
            });

            legendHighlightHandles.add(
                    new LegendHighlightHandle(
                            quartile,
                            legendItem,
                            colorDot,
                            nameLabel,
                            color
                    )
            );

            publisherLegendFlow.getChildren().add(legendItem);
        }

        publisherLegendBox.setVisible(true);
        publisherLegendBox.setManaged(true);

        applyActiveQuartileHighlight();
    }

    private void updateTotalChartLegend(List<PublisherAnalysisService.PublisherSummary> summaries) {
        totalLegendHighlightHandles.clear();

        if (publisherTotalLegendBox == null || publisherTotalLegendFlow == null) {
            return;
        }

        publisherTotalLegendFlow.getChildren().clear();

        if (summaries == null || summaries.size() <= 1) {
            publisherTotalLegendBox.setVisible(false);
            publisherTotalLegendBox.setManaged(false);
            return;
        }

        int index = 0;

        for (PublisherAnalysisService.PublisherSummary summary : summaries) {
            String color = getChartColor(index);
            String publisherKey = publisherAnalysisService.buildTotalPublisherKey(summary);
            String fullName = summary.publisherName();
            String shortName = publisherAnalysisService.shortenName(fullName, 34);

            Region colorDot = new Region();
            colorDot.setMinSize(10, 10);
            colorDot.setPrefSize(10, 10);
            colorDot.setMaxSize(10, 10);
            colorDot.setStyle(getLegendDotStyle(color, false));

            Label nameLabel = new Label(shortName);
            nameLabel.setStyle(getLegendLabelStyle(false));

            HBox legendItem = new HBox(6, colorDot, nameLabel);
            legendItem.setAlignment(Pos.CENTER_LEFT);
            legendItem.setCursor(Cursor.HAND);
            legendItem.setStyle(getLegendItemStyle(color, false));

            Tooltip tooltip = new Tooltip(
                    fullName
                            + "\nTotal publications: " + summary.totalPublications()
                            + "\nHover to highlight. Double click to remove this publisher."
            );

            Tooltip.install(nameLabel, tooltip);
            Tooltip.install(legendItem, tooltip);

            totalLegendHighlightHandles.add(
                    new TotalLegendHighlightHandle(
                            publisherKey,
                            legendItem,
                            colorDot,
                            nameLabel,
                            color
                    )
            );

            PublisherAnalysisService.PublisherSummary currentSummary = summary;

            legendItem.setOnMouseEntered(event -> setActiveTotalPublisherHighlight(publisherKey));
            legendItem.setOnMouseExited(event -> setActiveTotalPublisherHighlight(null));

            legendItem.setOnMouseClicked(event -> {
                if (event.getButton() != MouseButton.PRIMARY) {
                    return;
                }

                if (event.getClickCount() == 2) {
                    removePublisherFromTotalLegend(currentSummary);
                    event.consume();
                    return;
                }

                if (event.getClickCount() == 1) {
                    selectPublisherFromTotalLegend(currentSummary);
                    setActiveTotalPublisherHighlight(publisherKey);
                    event.consume();
                }
            });

            publisherTotalLegendFlow.getChildren().add(legendItem);
            index++;
        }

        publisherTotalLegendBox.setVisible(true);
        publisherTotalLegendBox.setManaged(true);

        applyActiveTotalPublisherHighlight();
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

    private void selectPublisherFromTotalLegend(PublisherAnalysisService.PublisherSummary summary) {
        if (summary == null || selectedPublishersListView == null) {
            return;
        }

        for (PublisherOptionDto selectedPublisher : selectedPublishersListView.getItems()) {
            if (selectedPublisher.publisherId() == summary.publisherId()) {
                selectedPublishersListView.getSelectionModel().select(selectedPublisher);
                selectedPublishersListView.scrollTo(selectedPublisher);
                return;
            }
        }
    }

    private void removePublisherFromTotalLegend(PublisherAnalysisService.PublisherSummary summary) {
        if (summary == null || selectedPublishersListView == null) {
            return;
        }

        PublisherOptionDto publisherToRemove = null;

        for (PublisherOptionDto selectedPublisher : selectedPublishersListView.getItems()) {
            if (selectedPublisher.publisherId() == summary.publisherId()) {
                publisherToRemove = selectedPublisher;
                break;
            }
        }

        if (publisherToRemove == null) {
            return;
        }

        selectedPublishersListView.getItems().remove(publisherToRemove);
        setActiveTotalPublisherHighlight(null);
        setStatus("Publisher removed.");

        if (selectedPublishersListView.getItems().isEmpty()) {
            clearCharts();
            setStatus("No selected publishers. Add publishers and load the analysis.");
            return;
        }

        loadPublisherAnalysis();
    }

    private void applyTotalLegendStyle(TotalLegendHighlightHandle handle, boolean highlighted) {
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

    private void setLoading(boolean loading) {
        if (publisherFilterField != null) {
            publisherFilterField.setDisable(loading);
        }

        if (addPublisherButton != null) {
            addPublisherButton.setDisable(loading);
        }

        if (removeSelectedPublisherButton != null) {
            removeSelectedPublisherButton.setDisable(loading);
        }

        if (clearPublisherAnalysisButton != null) {
            clearPublisherAnalysisButton.setDisable(loading);
        }

        if (loadPublisherAnalysisButton != null) {
            loadPublisherAnalysisButton.setDisable(loading);
        }

        if (publisherResultsListView != null) {
            publisherResultsListView.setDisable(loading);
        }

        if (selectedPublishersListView != null) {
            selectedPublishersListView.setDisable(loading);
        }

        if (publisherQuartileBarChart != null) {
            publisherQuartileBarChart.setDisable(loading);
        }

        if (publisherTotalBarChart != null) {
            publisherTotalBarChart.setDisable(loading);
        }
    }

    private void setStatus(String message) {
        if (publisherStatusLabel != null) {
            publisherStatusLabel.setText(message == null ? "" : message);
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

    private record BarPointInfo(
            String publisher,
            String quartile,
            long count,
            long totalPublisherPublications
    ) {
    }

    private record TotalBarPointInfo(
            String publisher,
            long totalPublications,
            String publisherKey
    ) {
    }

    private record ChartHighlightHandle(
            String quartileKey,
            Runnable normalStyle,
            Runnable highlightedStyle
    ) {
    }

    private record LegendHighlightHandle(
            String quartileKey,
            HBox legendItem,
            Region colorDot,
            Label nameLabel,
            String color
    ) {
    }

    private record TotalChartHighlightHandle(
            String publisherKey,
            Runnable normalStyle,
            Runnable highlightedStyle
    ) {
    }

    private record TotalLegendHighlightHandle(
            String publisherKey,
            HBox legendItem,
            Region colorDot,
            Label nameLabel,
            String color
    ) {
    }
}