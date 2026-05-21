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
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import service.charts.VenueCategoriesService;

import java.util.ArrayList;
import java.util.List;

public class VenueCategoriesController {

    private static final int MIN_YEAR = VenueCategoriesService.DEFAULT_MIN_YEAR;

    private static final String CATEGORY_ENTER_HANDLER_INSTALLED_KEY = "venue-categories-enter-handler-installed";
    private static final long CATEGORY_ENTER_LOAD_DEBOUNCE_MILLIS = 300L;

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

    private final VenueCategoriesService venueCategoriesService = new VenueCategoriesService();

    private Task<List<CategoryOptionDto>> categoryOptionsTask;
    private String activeCategoryKey;

    private long lastCategoryEnterLoadTimeMillis = 0L;

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

        try {
            venueCategoriesService.validateAnalysisType(analysisType);
        } catch (IllegalArgumentException exception) {
            showError("No category type selected", exception.getMessage());
            return;
        }

        VenueCategoriesService.YearRange yearRange;

        try {
            yearRange = getSelectedYearRange();
        } catch (IllegalArgumentException exception) {
            showError("Invalid year range", exception.getMessage());
            return;
        }

        String categoryFilter = getSelectedCategoryFilter();

        Task<List<VenueCategoriesService.CategoryTrendSeries>> task = new Task<>() {
            @Override
            protected List<VenueCategoriesService.CategoryTrendSeries> call() {
                return venueCategoriesService.loadCategoryTrendData(
                        analysisType,
                        categoryFilter,
                        yearRange.startYear(),
                        yearRange.endYear()
                );
            }
        };

        setLoading(true);
        setStatus("Loading category trends...");

        task.setOnSucceeded(event -> {
            setLoading(false);

            List<VenueCategoriesService.CategoryTrendSeries> data = task.getValue();

            if (data == null || data.isEmpty()) {
                clearChartOnly();
                setStatus("No category trend data found.");
                showInfo(
                        "No data found",
                        "No yearly trend data was found for the selected filters."
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
            showError("Loading error", exception == null ? null : exception.getMessage());
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

        categoryTypeComboBox.getItems().setAll(venueCategoriesService.getAnalysisTypes());
        categoryTypeComboBox.getSelectionModel().select(VenueCategoriesService.ANALYSIS_CONFERENCE_PRIMARY_FOR);

        categoryTypeComboBox.setOnAction(event -> {
            clearChartOnly();
            refreshCategoryFilterOptions();
        });
    }

    private void setupCategoryFilterComboBox() {
        if (categoryFilterComboBox == null) {
            return;
        }

        categoryFilterComboBox.setCellFactory(listView -> {
            installCategoryListEnterHandler(listView);

            return new ListCell<>() {
                @Override
                protected void updateItem(CategoryOptionDto item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(formatCategoryOptionForUi(item));
                    }
                }
            };
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

        categoryFilterComboBox.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() != KeyCode.ENTER) {
                return;
            }

            event.consume();
            loadSelectedCategoryFromEnter();
        });

        setDefaultCategoryOptions();
    }

    private void installCategoryListEnterHandler(ListView<CategoryOptionDto> listView) {
        if (listView == null) {
            return;
        }

        Object alreadyInstalled = listView.getProperties().get(CATEGORY_ENTER_HANDLER_INSTALLED_KEY);

        if (Boolean.TRUE.equals(alreadyInstalled)) {
            return;
        }

        listView.getProperties().put(CATEGORY_ENTER_HANDLER_INSTALLED_KEY, Boolean.TRUE);

        listView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.ENTER) {
                return;
            }

            CategoryOptionDto selectedItem = listView.getSelectionModel().getSelectedItem();

            if (selectedItem != null && categoryFilterComboBox != null) {
                categoryFilterComboBox.getSelectionModel().select(selectedItem);
            }

            event.consume();
            loadSelectedCategoryFromEnter();
        });
    }

    private void loadSelectedCategoryFromEnter() {
        if (categoryFilterComboBox == null || categoryFilterComboBox.isDisabled()) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastCategoryEnterLoadTimeMillis < CATEGORY_ENTER_LOAD_DEBOUNCE_MILLIS) {
            return;
        }

        lastCategoryEnterLoadTimeMillis = now;

        if (categoryFilterComboBox.isShowing()) {
            categoryFilterComboBox.hide();
        }

        Platform.runLater(this::loadCategoryTrends);
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

        Task<List<CategoryOptionDto>> task = new Task<>() {
            @Override
            protected List<CategoryOptionDto> call() {
                return venueCategoriesService.loadCategoryOptions(selectedType);
            }
        };

        categoryOptionsTask = task;

        task.setOnSucceeded(event -> {
            if (task.isCancelled()) {
                return;
            }

            String currentSelectedType = categoryTypeComboBox == null ? null : categoryTypeComboBox.getValue();

            if (selectedType != null && !selectedType.equals(currentSelectedType)) {
                return;
            }

            categoryFilterComboBox.setDisable(false);

            List<CategoryOptionDto> options = task.getValue();

            if (options == null || options.isEmpty()) {
                options = venueCategoriesService.buildDefaultCategoryOptions();
            }

            categoryFilterComboBox.getItems().setAll(options);
            categoryFilterComboBox.getSelectionModel().selectFirst();
            categoryFilterComboBox.setPromptText(venueCategoriesService.getCategoryPromptText(selectedType));

            setStatus("Loaded " + Math.max(0, options.size() - 1) + " categories.");
        });

        task.setOnFailed(event -> {
            categoryFilterComboBox.setDisable(false);
            setDefaultCategoryOptions();

            Throwable exception = task.getException();

            if (exception != null) {
                exception.printStackTrace();
            }

            setStatus("Could not load categories from database.");
        });

        startBackgroundTask(task, "venue-categories-options-task");
    }

    private void setDefaultCategoryOptions() {
        if (categoryFilterComboBox == null) {
            return;
        }

        String selectedType = categoryTypeComboBox == null ? null : categoryTypeComboBox.getValue();

        categoryFilterComboBox.getItems().setAll(venueCategoriesService.buildDefaultCategoryOptions());
        categoryFilterComboBox.getSelectionModel().selectFirst();
        categoryFilterComboBox.setPromptText(venueCategoriesService.getCategoryPromptText(selectedType));
    }

    private String formatCategoryOptionForUi(CategoryOptionDto option) {
        String selectedType = categoryTypeComboBox == null ? null : categoryTypeComboBox.getValue();
        return venueCategoriesService.formatCategoryOptionForUi(option, selectedType);
    }

    private String getSelectedCategoryFilter() {
        if (categoryFilterComboBox == null) {
            return "";
        }

        return venueCategoriesService.getSelectedCategoryFilter(categoryFilterComboBox.getValue());
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
        comboBox.getItems().setAll(venueCategoriesService.buildYearList(MIN_YEAR));
    }

    private void refreshToYearOptions(Integer fromYear) {
        if (categoryToYearComboBox == null) {
            return;
        }

        Integer currentToYear = categoryToYearComboBox.getValue();

        categoryToYearComboBox.getItems().setAll(venueCategoriesService.buildYearList(fromYear));

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

    private void updateCategoryTrendChart(
            List<VenueCategoriesService.CategoryTrendSeries> allSeries,
            String analysisType
    ) {
        clearChartOnly();

        if (allSeries == null || allSeries.isEmpty()) {
            return;
        }

        Integer minYear = null;
        Integer maxYear = null;
        double maxValue = 0;

        int seriesIndex = 0;

        for (VenueCategoriesService.CategoryTrendSeries categorySeries : allSeries) {
            XYChart.Series<Number, Number> chartSeries = new XYChart.Series<>();
            chartSeries.setName(shortenName(categorySeries.category()));

            for (VenueCategoriesService.CategoryTrendPoint point : categorySeries.points()) {
                minYear = minYear == null ? point.year() : Math.min(minYear, point.year());
                maxYear = maxYear == null ? point.year() : Math.max(maxYear, point.year());
                maxValue = Math.max(maxValue, point.count());

                XYChart.Data<Number, Number> dataPoint =
                        new XYChart.Data<>(point.year(), point.count());

                dataPoint.setExtraValue(point.category());
                chartSeries.getData().add(dataPoint);
            }

            if (!chartSeries.getData().isEmpty() && categoryTrendLineChart != null) {
                categoryTrendLineChart.getData().add(chartSeries);

                final int colorIndex = seriesIndex;
                final VenueCategoriesService.CategoryTrendSeries currentSeries = categorySeries;

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
            categoryTrendLineChart.setTitle(venueCategoriesService.getChartTitle(analysisType));
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

        configureYearAxis(categoryYearAxis, MIN_YEAR, venueCategoriesService.getCurrentYear());
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

    private void applyLineSeriesColor(
            XYChart.Series<Number, Number> series,
            int colorIndex,
            VenueCategoriesService.CategoryTrendSeries categorySeries
    ) {
        if (series == null || categorySeries == null) {
            return;
        }

        String color = getChartColor(colorIndex);
        String categoryKey = venueCategoriesService.getCategoryKey(categorySeries);

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

    private void updateCustomLegend(List<VenueCategoriesService.CategoryTrendSeries> allSeries) {
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

        for (VenueCategoriesService.CategoryTrendSeries categorySeries : allSeries) {
            String color = getChartColor(index);
            String categoryKey = venueCategoriesService.getCategoryKey(categorySeries);

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

    private String shortenName(String name) {
        if (name == null || name.isBlank()) {
            return "-";
        }

        if (name.length() <= 42) {
            return name;
        }

        return name.substring(0, 39) + "...";
    }

    private VenueCategoriesService.YearRange getSelectedYearRange() {
        Integer startYear = categoryFromYearComboBox == null ? null : categoryFromYearComboBox.getValue();
        Integer endYear = categoryToYearComboBox == null ? null : categoryToYearComboBox.getValue();

        return venueCategoriesService.validateYearRange(startYear, endYear);
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
}