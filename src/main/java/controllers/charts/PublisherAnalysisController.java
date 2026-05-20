package controllers.charts;

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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import service.JournalService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PublisherAnalysisController {

    private static final String QUARTILE_Q1 = "Q1";
    private static final String QUARTILE_Q2 = "Q2";
    private static final String QUARTILE_Q3 = "Q3";
    private static final String QUARTILE_Q4 = "Q4";

    private static final List<String> QUARTILES = List.of(
            QUARTILE_Q1,
            QUARTILE_Q2,
            QUARTILE_Q3,
            QUARTILE_Q4
    );

    private static final int DEFAULT_TOP_N = 15;

    private static final String[] CHART_COLORS = {
            "#1f5fa8",
            "#f5a623",
            "#4caf50",
            "#d13f64"
    };

    private final JournalService journalService = new JournalService();

    private String activeQuartileKey;

    private final List<ChartHighlightHandle> chartHighlightHandles = new ArrayList<>();
    private final List<LegendHighlightHandle> legendHighlightHandles = new ArrayList<>();

    @FXML
    private TextField publisherFilterField;

    @FXML
    private ComboBox<Integer> publisherLimitComboBox;

    @FXML
    private Button loadPublisherAnalysisButton;

    @FXML
    private Button clearPublisherAnalysisButton;

    @FXML
    private Label publisherStatusLabel;

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
    public void initialize() {
        setupLimitComboBox();
        setupBarChart();

        clearPublisherAnalysis();
        setStatus("Choose publisher filter or load top publishers.");
    }

    @FXML
    private void loadPublisherAnalysis() {
        String publisherFilter = publisherFilterField == null ? "" : publisherFilterField.getText().trim();
        Integer limit = publisherLimitComboBox == null ? DEFAULT_TOP_N : publisherLimitComboBox.getValue();

        if (limit == null || limit <= 0) {
            limit = DEFAULT_TOP_N;
        }

        Integer requestedLimit = limit;

        Task<List<PublisherQuartileSummary>> task = new Task<>() {
            @Override
            protected List<PublisherQuartileSummary> call() {
                return loadPublisherQuartileData(publisherFilter, requestedLimit);
            }
        };

        setLoading(true);
        setStatus("Loading publisher analysis...");

        task.setOnSucceeded(event -> {
            setLoading(false);

            List<PublisherQuartileSummary> data = task.getValue();

            if (data == null || data.isEmpty()) {
                clearChartOnly();
                setStatus("No publisher data found. Check if the Service/DAO query has been connected.");
                showInfo(
                        "Δεν βρέθηκαν δεδομένα",
                        "Το tab είναι έτοιμο, αλλά πρέπει να συνδεθεί με query στο JournalService/DAO για να επιστρέφει πραγματικά δεδομένα."
                );
                return;
            }

            updatePublisherQuartileChart(data);
            setStatus("Loaded " + data.size() + " publishers.");
        });

        task.setOnFailed(event -> {
            setLoading(false);
            Throwable exception = task.getException();
            setStatus("Failed to load publisher analysis.");
            showError("Σφάλμα φόρτωσης", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(task, "publisher-analysis-load-task");
    }

    @FXML
    private void clearPublisherAnalysis() {
        if (publisherFilterField != null) {
            publisherFilterField.clear();
        }

        if (publisherLimitComboBox != null) {
            publisherLimitComboBox.getSelectionModel().select(Integer.valueOf(DEFAULT_TOP_N));
        }

        clearChartOnly();
        setStatus("Choose publisher filter or load top publishers.");
    }

    private void setupLimitComboBox() {
        if (publisherLimitComboBox == null) {
            return;
        }

        publisherLimitComboBox.getItems().setAll(5, 10, 15, 20, 30, 50);
        publisherLimitComboBox.getSelectionModel().select(Integer.valueOf(DEFAULT_TOP_N));
    }

    private void setupBarChart() {
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
            publisherCountAxis.setLabel("Journals count");
            publisherCountAxis.setAutoRanging(false);
            publisherCountAxis.setForceZeroInRange(true);
            publisherCountAxis.setMinorTickVisible(false);
        }
    }

    /*
     * Το controller είναι έτοιμο.
     *
     * Για πραγματικά δεδομένα, μετά πρόσθεσε στο JournalService ένα από αυτά:
     *
     * getPublisherQuartileStats(String publisherFilter, Integer limit)
     * getJournalPublisherQuartileStats(String publisherFilter, Integer limit)
     * getPublisherQuartileAnalysis(String publisherFilter, Integer limit)
     *
     * Το return μπορεί να είναι List από DTO/record με πεδία ή methods:
     * publisher
     * quartile / bestQuartile
     * count / journalCount / totalJournals / value
     *
     * Παράδειγμα row:
     * publisher = "Elsevier"
     * quartile = "Q1"
     * count = 120
     */
    private List<PublisherQuartileSummary> loadPublisherQuartileData(String publisherFilter, Integer limit) {
        List<Object> rawRows = invokePublisherServiceMethod(
                List.of(
                        "getPublisherQuartileStats",
                        "getJournalPublisherQuartileStats",
                        "getPublisherQuartileAnalysis",
                        "getPublisherQuartileCounts",
                        "getJournalPublishersByQuartile"
                ),
                publisherFilter,
                limit
        );

        return convertRawRowsToPublisherSummaries(rawRows, publisherFilter, limit);
    }

    private List<Object> invokePublisherServiceMethod(
            List<String> methodNames,
            String publisherFilter,
            Integer limit
    ) {
        if (methodNames == null || methodNames.isEmpty()) {
            return new ArrayList<>();
        }

        for (String methodName : methodNames) {
            List<Object> result;

            result = tryInvokeServiceMethod(
                    journalService,
                    methodName,
                    new Class<?>[]{String.class, Integer.class},
                    new Object[]{publisherFilter, limit}
            );

            if (result != null) {
                return result;
            }

            result = tryInvokeServiceMethod(
                    journalService,
                    methodName,
                    new Class<?>[]{String.class, int.class},
                    new Object[]{publisherFilter, limit == null ? DEFAULT_TOP_N : limit}
            );

            if (result != null) {
                return result;
            }

            result = tryInvokeServiceMethod(
                    journalService,
                    methodName,
                    new Class<?>[]{String.class},
                    new Object[]{publisherFilter}
            );

            if (result != null) {
                return result;
            }

            result = tryInvokeServiceMethod(
                    journalService,
                    methodName,
                    new Class<?>[]{Integer.class},
                    new Object[]{limit}
            );

            if (result != null) {
                return result;
            }

            result = tryInvokeServiceMethod(
                    journalService,
                    methodName,
                    new Class<?>[]{int.class},
                    new Object[]{limit == null ? DEFAULT_TOP_N : limit}
            );

            if (result != null) {
                return result;
            }

            result = tryInvokeServiceMethod(
                    journalService,
                    methodName,
                    new Class<?>[]{},
                    new Object[]{}
            );

            if (result != null) {
                return result;
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

    private List<PublisherQuartileSummary> convertRawRowsToPublisherSummaries(
            List<Object> rawRows,
            String publisherFilter,
            Integer limit
    ) {
        if (rawRows == null || rawRows.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, PublisherQuartileAccumulator> grouped = new LinkedHashMap<>();

        for (Object row : rawRows) {
            if (row == null) {
                continue;
            }

            String publisher = readStringValue(
                    row,
                    List.of(
                            "publisher",
                            "publisherName",
                            "publisher_name",
                            "name",
                            "label"
                    )
            );

            String quartile = readStringValue(
                    row,
                    List.of(
                            "quartile",
                            "bestQuartile",
                            "best_quartile",
                            "bestQuartileName",
                            "best_quartile_name"
                    )
            );

            Long count = readLongValue(
                    row,
                    List.of(
                            "count",
                            "journalCount",
                            "journal_count",
                            "totalJournals",
                            "total_journals",
                            "total",
                            "value"
                    )
            );

            if ((publisher == null || publisher.isBlank())
                    && publisherFilter != null
                    && !publisherFilter.isBlank()) {
                publisher = publisherFilter;
            }

            if (publisher == null || publisher.isBlank() || quartile == null || quartile.isBlank()) {
                continue;
            }

            String normalizedQuartile = normalizeQuartile(quartile);

            if (!QUARTILES.contains(normalizedQuartile)) {
                continue;
            }

            long safeCount = count == null ? 0 : count;

            grouped
                    .computeIfAbsent(publisher, PublisherQuartileAccumulator::new)
                    .add(normalizedQuartile, safeCount);
        }

        List<PublisherQuartileSummary> summaries = new ArrayList<>();

        for (PublisherQuartileAccumulator accumulator : grouped.values()) {
            if (accumulator.totalCount() > 0) {
                summaries.add(accumulator.toSummary());
            }
        }

        summaries.sort(
                Comparator.comparingLong(PublisherQuartileSummary::totalCount)
                        .reversed()
                        .thenComparing(PublisherQuartileSummary::publisher)
        );

        int safeLimit = limit == null || limit <= 0 ? DEFAULT_TOP_N : limit;

        if (summaries.size() > safeLimit) {
            return new ArrayList<>(summaries.subList(0, safeLimit));
        }

        return summaries;
    }

    private String normalizeQuartile(String quartile) {
        if (quartile == null) {
            return "";
        }

        String normalized = quartile.trim().toUpperCase();

        if (normalized.contains("Q1")) {
            return QUARTILE_Q1;
        }

        if (normalized.contains("Q2")) {
            return QUARTILE_Q2;
        }

        if (normalized.contains("Q3")) {
            return QUARTILE_Q3;
        }

        if (normalized.contains("Q4")) {
            return QUARTILE_Q4;
        }

        return normalized;
    }

    private String readStringValue(Object row, List<String> names) {
        Object value = readValue(row, names);

        if (value == null) {
            return null;
        }

        return String.valueOf(value).trim();
    }

    private Long readLongValue(Object row, List<String> names) {
        Object value = readValue(row, names);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return Math.round(Double.parseDouble(String.valueOf(value).trim()));
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

    private void updatePublisherQuartileChart(List<PublisherQuartileSummary> summaries) {
        clearChartOnly();

        if (summaries == null || summaries.isEmpty() || publisherQuartileBarChart == null) {
            return;
        }

        List<String> publisherCategories = new ArrayList<>();

        for (PublisherQuartileSummary summary : summaries) {
            publisherCategories.add(shortenName(summary.publisher(), 28));
        }

        if (publisherCategoryAxis != null) {
            publisherCategoryAxis.getCategories().setAll(publisherCategories);
        }

        double maxValue = 0;

        for (String quartile : QUARTILES) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(quartile);

            for (PublisherQuartileSummary summary : summaries) {
                String publisherName = shortenName(summary.publisher(), 28);
                long count = summary.countForQuartile(quartile);

                maxValue = Math.max(maxValue, count);

                XYChart.Data<String, Number> dataPoint =
                        new XYChart.Data<>(publisherName, count);

                dataPoint.setExtraValue(
                        new BarPointInfo(
                                summary.publisher(),
                                quartile,
                                count,
                                summary.totalCount()
                        )
                );

                series.getData().add(dataPoint);
            }

            publisherQuartileBarChart.getData().add(series);

            int colorIndex = QUARTILES.indexOf(quartile);
            runAfterChartRender(() -> applyBarSeriesColor(series, colorIndex, quartile));
        }

        configureValueAxis(publisherCountAxis, maxValue);

        if (publisherQuartileBarChart != null) {
            publisherQuartileBarChart.setTitle("Journals per publisher and quartile");
        }

        updateCustomLegend();
    }

    private void clearChartOnly() {
        activeQuartileKey = null;
        chartHighlightHandles.clear();
        legendHighlightHandles.clear();

        if (publisherQuartileBarChart != null) {
            publisherQuartileBarChart.getData().clear();
            publisherQuartileBarChart.setTitle("");
        }

        if (publisherCategoryAxis != null) {
            publisherCategoryAxis.getCategories().clear();
        }

        configureValueAxis(publisherCountAxis, 10);
        updateCustomLegend();
    }

    private void applyBarSeriesColor(
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
                                                + "\nJournals in quartile: " + info.count()
                                                + "\nTotal journals: " + info.totalPublisherCount()
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

        for (int index = 0; index < QUARTILES.size(); index++) {
            String quartile = QUARTILES.get(index);
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

    private String shortenName(String name, int maxLength) {
        if (name == null || name.isBlank()) {
            return "-";
        }

        if (name.length() <= maxLength) {
            return name;
        }

        return name.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private void setLoading(boolean loading) {
        if (publisherFilterField != null) {
            publisherFilterField.setDisable(loading);
        }

        if (publisherLimitComboBox != null) {
            publisherLimitComboBox.setDisable(loading);
        }

        if (loadPublisherAnalysisButton != null) {
            loadPublisherAnalysisButton.setDisable(loading);
        }

        if (clearPublisherAnalysisButton != null) {
            clearPublisherAnalysisButton.setDisable(loading);
        }

        if (publisherQuartileBarChart != null) {
            publisherQuartileBarChart.setDisable(loading);
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

    private static class PublisherQuartileAccumulator {

        private final String publisher;
        private long q1;
        private long q2;
        private long q3;
        private long q4;

        private PublisherQuartileAccumulator(String publisher) {
            this.publisher = publisher;
        }

        private void add(String quartile, long count) {
            if (QUARTILE_Q1.equals(quartile)) {
                q1 += count;
            } else if (QUARTILE_Q2.equals(quartile)) {
                q2 += count;
            } else if (QUARTILE_Q3.equals(quartile)) {
                q3 += count;
            } else if (QUARTILE_Q4.equals(quartile)) {
                q4 += count;
            }
        }

        private long totalCount() {
            return q1 + q2 + q3 + q4;
        }

        private PublisherQuartileSummary toSummary() {
            return new PublisherQuartileSummary(
                    publisher,
                    q1,
                    q2,
                    q3,
                    q4
            );
        }
    }

    private record PublisherQuartileSummary(
            String publisher,
            long q1,
            long q2,
            long q3,
            long q4
    ) {

        private long countForQuartile(String quartile) {
            if (QUARTILE_Q1.equals(quartile)) {
                return q1;
            }

            if (QUARTILE_Q2.equals(quartile)) {
                return q2;
            }

            if (QUARTILE_Q3.equals(quartile)) {
                return q3;
            }

            if (QUARTILE_Q4.equals(quartile)) {
                return q4;
            }

            return 0;
        }

        private long totalCount() {
            return q1 + q2 + q3 + q4;
        }
    }

    private record BarPointInfo(
            String publisher,
            String quartile,
            long count,
            long totalPublisherCount
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
}