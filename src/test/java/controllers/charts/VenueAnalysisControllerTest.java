package controllers.charts;

import javafx.application.Platform;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.charts.VenueAnalysisService;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class VenueAnalysisControllerTest {

    private VenueAnalysisController venueAnalysisController;

    @BeforeAll
    static void startJavaFxRuntime() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException exception) {
            latch.countDown();
        }

        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("JavaFX platform did not start.");
        }
    }

    @BeforeEach
    void setUp() {
        venueAnalysisController = new VenueAnalysisController();
    }

    @Test
    void setupVenueTypeComboBox_addsVenueTypesAndSelectsJournal() throws Exception {
        runOnFxThread(() -> {
            ComboBox<String> venueTypeComboBox = new ComboBox<>();
            setPrivateField("venueTypeComboBox", venueTypeComboBox);

            invokePrivate(
                    "setupVenueTypeComboBox",
                    new Class<?>[]{}
            );

            assertEquals(2, venueTypeComboBox.getItems().size());
            assertTrue(venueTypeComboBox.getItems().contains(VenueAnalysisService.TYPE_JOURNAL));
            assertTrue(venueTypeComboBox.getItems().contains(VenueAnalysisService.TYPE_CONFERENCE));
            assertEquals(VenueAnalysisService.TYPE_JOURNAL, venueTypeComboBox.getValue());
        });
    }

    @Test
    void setupMetricComboBox_addsMetricsAndSelectsArticlesMetric() throws Exception {
        runOnFxThread(() -> {
            ComboBox<String> metricComboBox = new ComboBox<>();
            setPrivateField("metricComboBox", metricComboBox);

            invokePrivate(
                    "setupMetricComboBox",
                    new Class<?>[]{}
            );

            assertEquals(3, metricComboBox.getItems().size());
            assertTrue(metricComboBox.getItems().contains(VenueAnalysisService.METRIC_ARTICLES));
            assertTrue(metricComboBox.getItems().contains(VenueAnalysisService.METRIC_AUTHOR_ENTRIES));
            assertTrue(metricComboBox.getItems().contains(VenueAnalysisService.METRIC_DISTINCT_AUTHORS));
            assertEquals(VenueAnalysisService.METRIC_ARTICLES, metricComboBox.getValue());
        });
    }

    @Test
    void setupYearComboBox_addsDescendingYears() throws Exception {
        runOnFxThread(() -> {
            ComboBox<Integer> comboBox = new ComboBox<>();

            invokePrivate(
                    "setupYearComboBox",
                    new Class<?>[]{ComboBox.class},
                    comboBox
            );

            int currentYear = LocalDate.now().getYear();

            assertFalse(comboBox.getItems().isEmpty());
            assertEquals(currentYear, comboBox.getItems().get(0));
            assertEquals(VenueAnalysisService.DEFAULT_MIN_YEAR, comboBox.getItems().get(comboBox.getItems().size() - 1));
            assertFalse(comboBox.isEditable());
        });
    }

    @Test
    void refreshToYearOptions_keepsCurrentToYear_whenItIsStillValid() throws Exception {
        runOnFxThread(() -> {
            ComboBox<Integer> toYearComboBox = new ComboBox<>();
            toYearComboBox.getItems().setAll(2024, 2023, 2022, 2021, 2020);
            toYearComboBox.setValue(2022);

            setPrivateField("toYearComboBox", toYearComboBox);

            invokePrivate(
                    "refreshToYearOptions",
                    new Class<?>[]{Integer.class},
                    2020
            );

            assertEquals(2022, toYearComboBox.getValue());
            assertTrue(toYearComboBox.getItems().contains(2020));
            assertFalse(toYearComboBox.getItems().contains(2019));
        });
    }

    @Test
    void refreshToYearOptions_removesInvalidToYearFromAvailableOptions_whenCurrentToYearIsBeforeFromYear() throws Exception {
        runOnFxThread(() -> {
            ComboBox<Integer> toYearComboBox = new ComboBox<>();
            toYearComboBox.getItems().setAll(2024, 2023, 2022, 2021, 2020);
            toYearComboBox.setValue(2020);

            setPrivateField("toYearComboBox", toYearComboBox);

            invokePrivate(
                    "refreshToYearOptions",
                    new Class<?>[]{Integer.class},
                    2022
            );

            assertFalse(toYearComboBox.getItems().contains(2020));
            assertTrue(toYearComboBox.getItems().contains(2022));
            assertTrue(toYearComboBox.getItems().contains(2023));
            assertTrue(toYearComboBox.getItems().contains(2024));
        });
    }

    @Test
    void getSelectedYearRange_returnsSelectedYears() throws Exception {
        runOnFxThread(() -> {
            ComboBox<Integer> fromYearComboBox = new ComboBox<>();
            ComboBox<Integer> toYearComboBox = new ComboBox<>();

            fromYearComboBox.setValue(2010);
            toYearComboBox.setValue(2020);

            setPrivateField("fromYearComboBox", fromYearComboBox);
            setPrivateField("toYearComboBox", toYearComboBox);

            Object result = invokePrivate(
                    "getSelectedYearRange",
                    new Class<?>[]{}
            );

            assertEquals(2010, invokeNoArgMethod(result, "startYear"));
            assertEquals(2020, invokeNoArgMethod(result, "endYear"));
        });
    }

    @Test
    void getSelectedYearRange_throwsException_whenEndYearIsBeforeStartYear() throws Exception {
        runOnFxThread(() -> {
            ComboBox<Integer> fromYearComboBox = new ComboBox<>();
            ComboBox<Integer> toYearComboBox = new ComboBox<>();

            fromYearComboBox.setValue(2020);
            toYearComboBox.setValue(2010);

            setPrivateField("fromYearComboBox", fromYearComboBox);
            setPrivateField("toYearComboBox", toYearComboBox);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> invokePrivate(
                            "getSelectedYearRange",
                            new Class<?>[]{}
                    )
            );
        });
    }

    @Test
    void getSelectedVenueType_returnsComboBoxValue() throws Exception {
        runOnFxThread(() -> {
            ComboBox<String> venueTypeComboBox = new ComboBox<>();
            venueTypeComboBox.setValue(VenueAnalysisService.TYPE_CONFERENCE);

            setPrivateField("venueTypeComboBox", venueTypeComboBox);

            assertEquals(
                    VenueAnalysisService.TYPE_CONFERENCE,
                    invokePrivate(
                            "getSelectedVenueType",
                            new Class<?>[]{}
                    )
            );
        });
    }

    @Test
    void getComboBoxYearValue_returnsNullWhenComboBoxIsNullAndValueWhenPresent() throws Exception {
        assertNull(
                invokePrivate(
                        "getComboBoxYearValue",
                        new Class<?>[]{ComboBox.class},
                        (Object) null
                )
        );

        runOnFxThread(() -> {
            ComboBox<Integer> comboBox = new ComboBox<>();
            comboBox.setValue(2020);

            assertEquals(
                    2020,
                    invokePrivate(
                            "getComboBoxYearValue",
                            new Class<?>[]{ComboBox.class},
                            comboBox
                    )
            );
        });
    }

    @Test
    void setupLineChart_configuresChartAndAxes() throws Exception {
        runOnFxThread(() -> {
            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);

            setPrivateField("comparisonLineChart", chart);
            setPrivateField("yearAxis", xAxis);
            setPrivateField("valueAxis", yAxis);

            invokePrivate(
                    "setupLineChart",
                    new Class<?>[]{}
            );

            assertFalse(chart.getAnimated());
            assertTrue(chart.getCreateSymbols());
            assertFalse(chart.isLegendVisible());

            assertFalse(xAxis.isAutoRanging());
            assertFalse(xAxis.isForceZeroInRange());
            assertEquals(0.0, xAxis.getTickLabelRotation());

            assertFalse(yAxis.isAutoRanging());
            assertTrue(yAxis.isForceZeroInRange());
        });
    }

    @Test
    void setupSingleBarChart_configuresChartAndAxes() throws Exception {
        runOnFxThread(() -> {
            CategoryAxis categoryAxis = new CategoryAxis();
            NumberAxis valueAxis = new NumberAxis();
            BarChart<String, Number> chart = new BarChart<>(categoryAxis, valueAxis);

            invokePrivate(
                    "setupSingleBarChart",
                    new Class<?>[]{BarChart.class, CategoryAxis.class, NumberAxis.class, String.class},
                    chart,
                    categoryAxis,
                    valueAxis,
                    VenueAnalysisService.BAR_TOTAL_ARTICLES
            );

            assertFalse(chart.getAnimated());
            assertFalse(chart.isLegendVisible());
            assertEquals(18.0, chart.getCategoryGap());
            assertEquals(4.0, chart.getBarGap());

            assertEquals("", categoryAxis.getLabel());
            assertFalse(categoryAxis.isTickLabelsVisible());
            assertFalse(categoryAxis.isTickMarkVisible());

            assertEquals(VenueAnalysisService.BAR_TOTAL_ARTICLES, valueAxis.getLabel());
            assertFalse(valueAxis.isAutoRanging());
            assertTrue(valueAxis.isForceZeroInRange());
        });
    }

    @Test
    void configureYearAxis_expandsSingleYearRange() throws Exception {
        runOnFxThread(() -> {
            NumberAxis axis = new NumberAxis();

            invokePrivate(
                    "configureYearAxis",
                    new Class<?>[]{NumberAxis.class, Integer.class, Integer.class},
                    axis,
                    2020,
                    2020
            );

            assertFalse(axis.isAutoRanging());
            assertEquals(2019.0, axis.getLowerBound());
            assertEquals(2021.0, axis.getUpperBound());
            assertEquals(1.0, axis.getTickUnit());
            assertFalse(axis.isMinorTickVisible());
        });
    }

    @Test
    void configureYearAxis_usesAutoRanging_whenYearsAreNull() throws Exception {
        runOnFxThread(() -> {
            NumberAxis axis = new NumberAxis();

            invokePrivate(
                    "configureYearAxis",
                    new Class<?>[]{NumberAxis.class, Integer.class, Integer.class},
                    axis,
                    null,
                    null
            );

            assertTrue(axis.isAutoRanging());
        });
    }

    @Test
    void calculateYearTickUnit_returnsExpectedValues() throws Exception {
        assertEquals(
                1,
                invokePrivate(
                        "calculateYearTickUnit",
                        new Class<?>[]{int.class},
                        10
                )
        );

        assertEquals(
                5,
                invokePrivate(
                        "calculateYearTickUnit",
                        new Class<?>[]{int.class},
                        25
                )
        );

        assertEquals(
                10,
                invokePrivate(
                        "calculateYearTickUnit",
                        new Class<?>[]{int.class},
                        60
                )
        );

        assertEquals(
                20,
                invokePrivate(
                        "calculateYearTickUnit",
                        new Class<?>[]{int.class},
                        100
                )
        );
    }

    @Test
    void configureValueAxis_setsNiceBounds() throws Exception {
        runOnFxThread(() -> {
            NumberAxis axis = new NumberAxis();

            invokePrivate(
                    "configureValueAxis",
                    new Class<?>[]{NumberAxis.class, double.class},
                    axis,
                    47.0
            );

            assertFalse(axis.isAutoRanging());
            assertEquals(0.0, axis.getLowerBound());
            assertTrue(axis.getUpperBound() >= 47.0);
            assertTrue(axis.getTickUnit() > 0);
            assertFalse(axis.isMinorTickVisible());
        });
    }

    @Test
    void calculateNiceMethods_returnPositiveValues() throws Exception {
        double upperBound = (double) invokePrivate(
                "calculateNiceUpperBound",
                new Class<?>[]{double.class},
                47.0
        );

        double tickUnit = (double) invokePrivate(
                "calculateNiceTickUnit",
                new Class<?>[]{double.class},
                upperBound
        );

        double rawTickUnit = (double) invokePrivate(
                "calculateNiceRawTickUnit",
                new Class<?>[]{double.class},
                6.7
        );

        assertTrue(upperBound >= 47.0);
        assertTrue(tickUnit > 0.0);
        assertTrue(rawTickUnit > 0.0);
    }

    @Test
    void getChartColor_cyclesThroughColors() throws Exception {
        String firstColor = (String) invokePrivate(
                "getChartColor",
                new Class<?>[]{int.class},
                0
        );

        String repeatedColor = (String) invokePrivate(
                "getChartColor",
                new Class<?>[]{int.class},
                10
        );

        assertEquals("#1f5fa8", firstColor);
        assertEquals(firstColor, repeatedColor);
    }

    @Test
    void shortenSeriesName_returnsExpectedValues() throws Exception {
        assertEquals(
                "-",
                invokePrivate(
                        "shortenSeriesName",
                        new Class<?>[]{String.class},
                        (Object) null
                )
        );

        assertEquals(
                "-",
                invokePrivate(
                        "shortenSeriesName",
                        new Class<?>[]{String.class},
                        "   "
                )
        );

        assertEquals(
                "Short name",
                invokePrivate(
                        "shortenSeriesName",
                        new Class<?>[]{String.class},
                        "Short name"
                )
        );

        String longName = "This is a very long venue name that should definitely be shortened";

        String result = (String) invokePrivate(
                "shortenSeriesName",
                new Class<?>[]{String.class},
                longName
        );

        assertTrue(result.endsWith("..."));
        assertEquals(45, result.length());
    }

    @Test
    void styleMethods_returnStylesContainingColor() throws Exception {
        String lineStyle = (String) invokePrivate(
                "getLineStyle",
                new Class<?>[]{String.class, boolean.class},
                "#123456",
                true
        );

        String symbolStyle = (String) invokePrivate(
                "getLineSymbolStyle",
                new Class<?>[]{String.class, boolean.class},
                "#123456",
                true
        );

        String barStyle = (String) invokePrivate(
                "getBarStyle",
                new Class<?>[]{String.class, boolean.class},
                "#123456",
                true
        );

        String legendItemStyle = (String) invokePrivate(
                "getLegendItemStyle",
                new Class<?>[]{String.class, boolean.class},
                "#123456",
                true
        );

        String legendDotStyle = (String) invokePrivate(
                "getLegendDotStyle",
                new Class<?>[]{String.class, boolean.class},
                "#123456",
                true
        );

        String legendLabelStyle = (String) invokePrivate(
                "getLegendLabelStyle",
                new Class<?>[]{boolean.class},
                true
        );

        assertTrue(lineStyle.contains("#123456"));
        assertTrue(symbolStyle.contains("#123456"));
        assertTrue(barStyle.contains("#123456"));
        assertTrue(legendItemStyle.contains("#123456"));
        assertTrue(legendDotStyle.contains("#123456"));
        assertTrue(legendLabelStyle.contains("-fx-font-weight"));
    }

    @Test
    void renderSearchResults_showsResultsAndClearsSelection() throws Exception {
        runOnFxThread(() -> {
            ListView<Object> resultsListView = new ListView<>();
            VBox resultsContainer = new VBox();

            Object result1 = new Object();
            Object result2 = new Object();

            setPrivateField("searchResultsListView", resultsListView);
            setPrivateField("searchResultsContainer", resultsContainer);
            setPrivateField("selectedSearchVenue", result1);

            invokePrivate(
                    "renderSearchResults",
                    new Class<?>[]{List.class},
                    List.of(result1, result2)
            );

            assertTrue(resultsContainer.isVisible());
            assertTrue(resultsContainer.isManaged());
            assertEquals(2, resultsListView.getItems().size());
            assertNull(getPrivateField("selectedSearchVenue"));
        });
    }

    @Test
    void clearSearchResults_clearsListAndHidesContainer() throws Exception {
        runOnFxThread(() -> {
            ListView<Object> resultsListView = new ListView<>();
            VBox resultsContainer = new VBox();

            Object result = new Object();
            resultsListView.getItems().add(result);

            setPrivateField("searchResultsListView", resultsListView);
            setPrivateField("searchResultsContainer", resultsContainer);
            setPrivateField("selectedSearchVenue", result);

            invokePrivate(
                    "clearSearchResults",
                    new Class<?>[]{}
            );

            assertTrue(resultsListView.getItems().isEmpty());
            assertFalse(resultsContainer.isVisible());
            assertFalse(resultsContainer.isManaged());
            assertNull(getPrivateField("selectedSearchVenue"));
        });
    }

    @Test
    void updateLineChart_addsSeriesAndConfiguresAxesAndLegend() throws Exception {
        runOnFxThread(() -> {
            NumberAxis yearAxis = new NumberAxis();
            NumberAxis valueAxis = new NumberAxis();
            LineChart<Number, Number> lineChart = new LineChart<>(yearAxis, valueAxis);

            VBox lineLegendBox = new VBox();
            FlowPane lineLegendFlow = new FlowPane();
            VBox barLegendBox = new VBox();
            FlowPane barLegendFlow = new FlowPane();

            setPrivateField("comparisonLineChart", lineChart);
            setPrivateField("yearAxis", yearAxis);
            setPrivateField("valueAxis", valueAxis);
            setPrivateField("lineChartLegendBox", lineLegendBox);
            setPrivateField("lineChartLegendFlow", lineLegendFlow);
            setPrivateField("barChartsLegendBox", barLegendBox);
            setPrivateField("barChartsLegendFlow", barLegendFlow);

            VenueAnalysisService.VenueChartSeries series =
                    venueChartSeries(
                            "Journal: Data Journal",
                            VenueAnalysisService.TYPE_JOURNAL,
                            5,
                            List.of(
                                    yearlyStats(2020, 10L, 30L, 8L),
                                    yearlyStats(2021, 20L, 50L, 15L)
                            )
                    );

            invokePrivate(
                    "updateLineChart",
                    new Class<?>[]{List.class, String.class},
                    List.of(series),
                    VenueAnalysisService.METRIC_ARTICLES
            );

            assertEquals(1, lineChart.getData().size());
            assertEquals("Journal: Data Journal", lineChart.getData().get(0).getName());
            assertEquals(2, lineChart.getData().get(0).getData().size());

            assertEquals(2020, lineChart.getData().get(0).getData().get(0).getXValue().intValue());
            assertEquals(10L, lineChart.getData().get(0).getData().get(0).getYValue().longValue());

            assertFalse(yearAxis.isAutoRanging());
            assertTrue(valueAxis.getUpperBound() >= 20.0);
            assertEquals(VenueAnalysisService.METRIC_ARTICLES, valueAxis.getLabel());

            assertTrue(lineLegendBox.isVisible());
            assertTrue(lineLegendBox.isManaged());
            assertEquals(1, lineLegendFlow.getChildren().size());

            assertTrue(barLegendBox.isVisible());
            assertTrue(barLegendBox.isManaged());
            assertEquals(1, barLegendFlow.getChildren().size());
        });
    }

    @Test
    void updateBarCharts_addsBarSeriesAndConfiguresValueAxes() throws Exception {
        runOnFxThread(() -> {
            CategoryAxis totalCategoryAxis = new CategoryAxis();
            NumberAxis totalValueAxis = new NumberAxis();
            BarChart<String, Number> totalChart =
                    new BarChart<>(totalCategoryAxis, totalValueAxis);

            CategoryAxis avgCategoryAxis = new CategoryAxis();
            NumberAxis avgValueAxis = new NumberAxis();
            BarChart<String, Number> avgChart =
                    new BarChart<>(avgCategoryAxis, avgValueAxis);

            CategoryAxis avgAuthorCategoryAxis = new CategoryAxis();
            NumberAxis avgAuthorValueAxis = new NumberAxis();
            BarChart<String, Number> avgAuthorChart =
                    new BarChart<>(avgAuthorCategoryAxis, avgAuthorValueAxis);

            setPrivateField("totalArticlesBarChart", totalChart);
            setPrivateField("totalArticlesValueAxis", totalValueAxis);
            setPrivateField("avgArticlesBarChart", avgChart);
            setPrivateField("avgArticlesValueAxis", avgValueAxis);
            setPrivateField("avgAuthorEntriesBarChart", avgAuthorChart);
            setPrivateField("avgAuthorEntriesValueAxis", avgAuthorValueAxis);

            VenueAnalysisService.VenueChartSeries series =
                    venueChartSeries(
                            "Journal: Data Journal",
                            VenueAnalysisService.TYPE_JOURNAL,
                            5,
                            List.of(
                                    yearlyStats(2020, 10L, 30L, 8L),
                                    yearlyStats(2021, 20L, 50L, 15L)
                            )
                    );

            invokePrivate(
                    "updateBarCharts",
                    new Class<?>[]{List.class},
                    List.of(series)
            );

            assertEquals(1, totalChart.getData().size());
            assertEquals(1, avgChart.getData().size());
            assertEquals(1, avgAuthorChart.getData().size());

            assertEquals(30.0, totalChart.getData().get(0).getData().get(0).getYValue().doubleValue());
            assertEquals(15.0, avgChart.getData().get(0).getData().get(0).getYValue().doubleValue());
            assertEquals(40.0, avgAuthorChart.getData().get(0).getData().get(0).getYValue().doubleValue());

            assertEquals(VenueAnalysisService.BAR_TOTAL_ARTICLES, totalValueAxis.getLabel());
            assertEquals(VenueAnalysisService.BAR_AVG_ARTICLES_PER_YEAR, avgValueAxis.getLabel());
            assertEquals(VenueAnalysisService.BAR_AVG_AUTHOR_ENTRIES_PER_YEAR, avgAuthorValueAxis.getLabel());
        });
    }

    @Test
    void clearCharts_resetsLineChartBarChartsAxesAndLegends() throws Exception {
        runOnFxThread(() -> {
            NumberAxis yearAxis = new NumberAxis();
            NumberAxis valueAxis = new NumberAxis();
            LineChart<Number, Number> lineChart = new LineChart<>(yearAxis, valueAxis);

            CategoryAxis totalCategoryAxis = new CategoryAxis();
            NumberAxis totalValueAxis = new NumberAxis();
            BarChart<String, Number> totalChart =
                    new BarChart<>(totalCategoryAxis, totalValueAxis);

            CategoryAxis avgCategoryAxis = new CategoryAxis();
            NumberAxis avgValueAxis = new NumberAxis();
            BarChart<String, Number> avgChart =
                    new BarChart<>(avgCategoryAxis, avgValueAxis);

            CategoryAxis avgAuthorCategoryAxis = new CategoryAxis();
            NumberAxis avgAuthorValueAxis = new NumberAxis();
            BarChart<String, Number> avgAuthorChart =
                    new BarChart<>(avgAuthorCategoryAxis, avgAuthorValueAxis);

            VBox lineLegendBox = new VBox();
            FlowPane lineLegendFlow = new FlowPane();
            VBox barLegendBox = new VBox();
            FlowPane barLegendFlow = new FlowPane();

            lineChart.getData().add(new XYChart.Series<>());
            totalChart.getData().add(new XYChart.Series<>());
            avgChart.getData().add(new XYChart.Series<>());
            avgAuthorChart.getData().add(new XYChart.Series<>());

            lineLegendFlow.getChildren().add(new Label("Old legend"));
            barLegendFlow.getChildren().add(new Label("Old legend"));

            setPrivateField("comparisonLineChart", lineChart);
            setPrivateField("yearAxis", yearAxis);
            setPrivateField("valueAxis", valueAxis);

            setPrivateField("totalArticlesBarChart", totalChart);
            setPrivateField("totalArticlesValueAxis", totalValueAxis);
            setPrivateField("avgArticlesBarChart", avgChart);
            setPrivateField("avgArticlesValueAxis", avgValueAxis);
            setPrivateField("avgAuthorEntriesBarChart", avgAuthorChart);
            setPrivateField("avgAuthorEntriesValueAxis", avgAuthorValueAxis);

            setPrivateField("lineChartLegendBox", lineLegendBox);
            setPrivateField("lineChartLegendFlow", lineLegendFlow);
            setPrivateField("barChartsLegendBox", barLegendBox);
            setPrivateField("barChartsLegendFlow", barLegendFlow);

            setPrivateField("activeVenueKey", "Journal#5");

            invokePrivate(
                    "clearCharts",
                    new Class<?>[]{}
            );

            assertTrue(lineChart.getData().isEmpty());
            assertTrue(totalChart.getData().isEmpty());
            assertTrue(avgChart.getData().isEmpty());
            assertTrue(avgAuthorChart.getData().isEmpty());

            assertFalse(lineLegendBox.isVisible());
            assertFalse(lineLegendBox.isManaged());
            assertTrue(lineLegendFlow.getChildren().isEmpty());

            assertFalse(barLegendBox.isVisible());
            assertFalse(barLegendBox.isManaged());
            assertTrue(barLegendFlow.getChildren().isEmpty());

            assertNull(getPrivateField("activeVenueKey"));
            assertTrue(valueAxis.getUpperBound() >= 10.0);
            assertTrue(totalValueAxis.getUpperBound() >= 10.0);
        });
    }

    @Test
    void setLoading_disablesAndEnablesControlsAndCharts() throws Exception {
        runOnFxThread(() -> {
            ComboBox<String> venueTypeComboBox = new ComboBox<>();
            TextField searchField = new TextField();
            Button addButton = new Button();
            Button removeButton = new Button();
            Button clearButton = new Button();
            Button loadButton = new Button();
            ComboBox<String> metricComboBox = new ComboBox<>();
            ComboBox<Integer> fromYearComboBox = new ComboBox<>();
            ComboBox<Integer> toYearComboBox = new ComboBox<>();
            ListView<Object> searchResultsListView = new ListView<>();
            ListView<VenueAnalysisService.SelectedVenue> selectedVenuesListView = new ListView<>();

            NumberAxis lineXAxis = new NumberAxis();
            NumberAxis lineYAxis = new NumberAxis();
            LineChart<Number, Number> lineChart = new LineChart<>(lineXAxis, lineYAxis);

            CategoryAxis totalCategoryAxis = new CategoryAxis();
            NumberAxis totalValueAxis = new NumberAxis();
            BarChart<String, Number> totalChart =
                    new BarChart<>(totalCategoryAxis, totalValueAxis);

            CategoryAxis avgCategoryAxis = new CategoryAxis();
            NumberAxis avgValueAxis = new NumberAxis();
            BarChart<String, Number> avgChart =
                    new BarChart<>(avgCategoryAxis, avgValueAxis);

            CategoryAxis avgAuthorCategoryAxis = new CategoryAxis();
            NumberAxis avgAuthorValueAxis = new NumberAxis();
            BarChart<String, Number> avgAuthorChart =
                    new BarChart<>(avgAuthorCategoryAxis, avgAuthorValueAxis);

            setPrivateField("venueTypeComboBox", venueTypeComboBox);
            setPrivateField("venueSearchField", searchField);
            setPrivateField("addSelectedButton", addButton);
            setPrivateField("removeSelectedButton", removeButton);
            setPrivateField("clearButton", clearButton);
            setPrivateField("loadChartButton", loadButton);
            setPrivateField("metricComboBox", metricComboBox);
            setPrivateField("fromYearComboBox", fromYearComboBox);
            setPrivateField("toYearComboBox", toYearComboBox);
            setPrivateField("searchResultsListView", searchResultsListView);
            setPrivateField("selectedVenuesListView", selectedVenuesListView);
            setPrivateField("comparisonLineChart", lineChart);
            setPrivateField("totalArticlesBarChart", totalChart);
            setPrivateField("avgArticlesBarChart", avgChart);
            setPrivateField("avgAuthorEntriesBarChart", avgAuthorChart);

            invokePrivate(
                    "setLoading",
                    new Class<?>[]{boolean.class},
                    true
            );

            assertTrue(venueTypeComboBox.isDisabled());
            assertTrue(searchField.isDisabled());
            assertTrue(addButton.isDisabled());
            assertTrue(removeButton.isDisabled());
            assertTrue(clearButton.isDisabled());
            assertTrue(loadButton.isDisabled());
            assertTrue(metricComboBox.isDisabled());
            assertTrue(fromYearComboBox.isDisabled());
            assertTrue(toYearComboBox.isDisabled());
            assertTrue(searchResultsListView.isDisabled());
            assertTrue(selectedVenuesListView.isDisabled());
            assertTrue(lineChart.isDisabled());
            assertTrue(totalChart.isDisabled());
            assertTrue(avgChart.isDisabled());
            assertTrue(avgAuthorChart.isDisabled());

            invokePrivate(
                    "setLoading",
                    new Class<?>[]{boolean.class},
                    false
            );

            assertFalse(venueTypeComboBox.isDisabled());
            assertFalse(searchField.isDisabled());
            assertFalse(addButton.isDisabled());
            assertFalse(removeButton.isDisabled());
            assertFalse(clearButton.isDisabled());
            assertFalse(loadButton.isDisabled());
            assertFalse(metricComboBox.isDisabled());
            assertFalse(fromYearComboBox.isDisabled());
            assertFalse(toYearComboBox.isDisabled());
            assertFalse(searchResultsListView.isDisabled());
            assertFalse(selectedVenuesListView.isDisabled());
            assertFalse(lineChart.isDisabled());
            assertFalse(totalChart.isDisabled());
            assertFalse(avgChart.isDisabled());
            assertFalse(avgAuthorChart.isDisabled());
        });
    }

    private VenueAnalysisService.VenueChartSeries venueChartSeries(
            String name,
            String type,
            int venueId,
            List<VenueAnalysisService.VenueYearlyStats> yearlyStats
    ) {
        return new VenueAnalysisService.VenueChartSeries(
                name,
                type,
                venueId,
                yearlyStats
        );
    }

    private VenueAnalysisService.VenueYearlyStats yearlyStats(
            Integer year,
            Long totalArticles,
            Long totalAuthorEntries,
            Long distinctAuthors
    ) {
        return new VenueAnalysisService.VenueYearlyStats(
                year,
                totalArticles,
                totalAuthorEntries,
                distinctAuthors
        );
    }

    private Object invokePrivate(
            String methodName,
            Class<?>[] parameterTypes,
            Object... arguments
    ) throws Exception {
        Method method = VenueAnalysisController.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);

        try {
            return method.invoke(venueAnalysisController, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            if (cause instanceof Error error) {
                throw error;
            }

            throw exception;
        }
    }

    private Object invokeNoArgMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);

        try {
            return method.invoke(target);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            if (cause instanceof Error error) {
                throw error;
            }

            throw exception;
        }
    }

    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = VenueAnalysisController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(venueAnalysisController, value);
    }

    private Object getPrivateField(String fieldName) throws Exception {
        Field field = VenueAnalysisController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(venueAnalysisController);
    }

    private static void runOnFxThread(ThrowingRunnable action) throws Exception {
        callOnFxThread(() -> {
            action.run();
            return null;
        });
    }

    private static <T> T callOnFxThread(Callable<T> action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return action.call();
        }

        FutureTask<T> futureTask = new FutureTask<>(action);
        Platform.runLater(futureTask);

        try {
            return futureTask.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();

            if (cause instanceof Exception checkedException) {
                throw checkedException;
            }

            if (cause instanceof Error error) {
                throw error;
            }

            throw new RuntimeException(cause);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}