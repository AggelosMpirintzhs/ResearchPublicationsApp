package controllers.charts;

import javafx.application.Platform;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.charts.ScatterPlotsService;

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

class ScatterPlotsControllerTest {

    private ScatterPlotsController scatterPlotsController;

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
        scatterPlotsController = new ScatterPlotsController();
    }

    @Test
    void setupVenueTypeComboBox_addsVenueTypesAndSelectsJournal() throws Exception {
        runOnFxThread(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            setPrivateField("scatterVenueTypeComboBox", comboBox);

            invokePrivate(
                    "setupVenueTypeComboBox",
                    new Class<?>[]{}
            );

            assertEquals(2, comboBox.getItems().size());
            assertTrue(comboBox.getItems().contains(ScatterPlotsService.TYPE_JOURNAL));
            assertTrue(comboBox.getItems().contains(ScatterPlotsService.TYPE_CONFERENCE));
            assertEquals(ScatterPlotsService.TYPE_JOURNAL, comboBox.getValue());
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
            assertEquals(ScatterPlotsService.DEFAULT_MIN_YEAR, comboBox.getItems().get(comboBox.getItems().size() - 1));
            assertFalse(comboBox.isEditable());
        });
    }

    @Test
    void refreshToYearOptions_keepsCurrentToYear_whenItIsStillValid() throws Exception {
        runOnFxThread(() -> {
            ComboBox<Integer> toYearComboBox = new ComboBox<>();
            toYearComboBox.getItems().setAll(2024, 2023, 2022, 2021, 2020);
            toYearComboBox.setValue(2022);

            setPrivateField("scatterToYearComboBox", toYearComboBox);

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

            setPrivateField("scatterToYearComboBox", toYearComboBox);

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

            setPrivateField("scatterFromYearComboBox", fromYearComboBox);
            setPrivateField("scatterToYearComboBox", toYearComboBox);

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

            setPrivateField("scatterFromYearComboBox", fromYearComboBox);
            setPrivateField("scatterToYearComboBox", toYearComboBox);

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
    void setupRankingControls_addsDefaultMetricsAndLimits() throws Exception {
        runOnFxThread(() -> {
            ComboBox<String> xMetricComboBox = new ComboBox<>();
            ComboBox<String> yMetricComboBox = new ComboBox<>();
            ComboBox<String> limitComboBox = new ComboBox<>();

            setPrivateField("rankingXMetricComboBox", xMetricComboBox);
            setPrivateField("rankingYMetricComboBox", yMetricComboBox);
            setPrivateField("rankingLimitComboBox", limitComboBox);

            invokePrivate(
                    "setupRankingControls",
                    new Class<?>[]{}
            );

            assertFalse(xMetricComboBox.getItems().isEmpty());
            assertFalse(yMetricComboBox.getItems().isEmpty());

            assertEquals("Total Docs", xMetricComboBox.getValue());
            assertEquals("Cites / Doc 2y", yMetricComboBox.getValue());

            assertTrue(limitComboBox.getItems().contains("All"));
            assertTrue(limitComboBox.getItems().contains("100"));
            assertEquals("100", limitComboBox.getValue());
        });
    }

    @Test
    void getSelectedRankingLimit_returnsResolvedLimit() throws Exception {
        runOnFxThread(() -> {
            ComboBox<String> limitComboBox = new ComboBox<>();
            setPrivateField("rankingLimitComboBox", limitComboBox);

            limitComboBox.setValue("300");

            assertEquals(
                    300,
                    invokePrivate(
                            "getSelectedRankingLimit",
                            new Class<?>[]{}
                    )
            );

            limitComboBox.setValue("All");

            assertNull(
                    invokePrivate(
                            "getSelectedRankingLimit",
                            new Class<?>[]{}
                    )
            );

            limitComboBox.setValue("bad value");

            assertEquals(
                    100,
                    invokePrivate(
                            "getSelectedRankingLimit",
                            new Class<?>[]{}
                    )
            );
        });
    }

    @Test
    void getSelectedVenueType_returnsComboBoxValue() throws Exception {
        runOnFxThread(() -> {
            ComboBox<String> venueTypeComboBox = new ComboBox<>();
            venueTypeComboBox.setValue(ScatterPlotsService.TYPE_CONFERENCE);

            setPrivateField("scatterVenueTypeComboBox", venueTypeComboBox);

            assertEquals(
                    ScatterPlotsService.TYPE_CONFERENCE,
                    invokePrivate(
                            "getSelectedVenueType",
                            new Class<?>[]{}
                    )
            );
        });
    }

    @Test
    void setupVenueScatterChart_configuresChartAndAxes() throws Exception {
        runOnFxThread(() -> {
            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            ScatterChart<Number, Number> chart = new ScatterChart<>(xAxis, yAxis);

            setPrivateField("venueScatterChart", chart);
            setPrivateField("scatterArticlesAxis", xAxis);
            setPrivateField("scatterAuthorsAxis", yAxis);

            invokePrivate(
                    "setupVenueScatterChart",
                    new Class<?>[]{}
            );

            assertFalse(chart.getAnimated());
            assertFalse(chart.isLegendVisible());
            assertEquals("", chart.getTitle());

            assertFalse(xAxis.isAutoRanging());
            assertTrue(xAxis.isForceZeroInRange());
            assertFalse(xAxis.isMinorTickVisible());
            assertEquals("Articles / year", xAxis.getLabel());

            assertFalse(yAxis.isAutoRanging());
            assertTrue(yAxis.isForceZeroInRange());
            assertFalse(yAxis.isMinorTickVisible());
            assertEquals("Avg authors / article", yAxis.getLabel());
        });
    }

    @Test
    void setupRankingScatterChart_configuresChartAndAxes() throws Exception {
        runOnFxThread(() -> {
            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            ScatterChart<Number, Number> chart = new ScatterChart<>(xAxis, yAxis);

            setPrivateField("journalRankingScatterChart", chart);
            setPrivateField("rankingXAxis", xAxis);
            setPrivateField("rankingYAxis", yAxis);

            invokePrivate(
                    "setupRankingScatterChart",
                    new Class<?>[]{}
            );

            assertFalse(chart.getAnimated());
            assertFalse(chart.isLegendVisible());
            assertEquals("", chart.getTitle());

            assertFalse(xAxis.isAutoRanging());
            assertTrue(xAxis.isForceZeroInRange());
            assertFalse(xAxis.isMinorTickVisible());
            assertEquals("X metric", xAxis.getLabel());

            assertFalse(yAxis.isAutoRanging());
            assertTrue(yAxis.isForceZeroInRange());
            assertFalse(yAxis.isMinorTickVisible());
            assertEquals("Y metric", yAxis.getLabel());
        });
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
            assertTrue(axis.getTickUnit() > 0.0);
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
    void shortenName_returnsExpectedValues() throws Exception {
        assertEquals(
                "-",
                invokePrivate(
                        "shortenName",
                        new Class<?>[]{String.class, int.class},
                        null,
                        10
                )
        );

        assertEquals(
                "-",
                invokePrivate(
                        "shortenName",
                        new Class<?>[]{String.class, int.class},
                        "   ",
                        10
                )
        );

        assertEquals(
                "Short",
                invokePrivate(
                        "shortenName",
                        new Class<?>[]{String.class, int.class},
                        "Short",
                        10
                )
        );

        assertEquals(
                "Very Lo...",
                invokePrivate(
                        "shortenName",
                        new Class<?>[]{String.class, int.class},
                        "Very Long Name",
                        10
                )
        );
    }

    @Test
    void formatNumber_returnsIntegerWithoutDecimalsAndDecimalWithTwoDigits() throws Exception {
        assertEquals(
                "10",
                invokePrivate(
                        "formatNumber",
                        new Class<?>[]{double.class},
                        10.0
                )
        );

        assertEquals(
                "10.35",
                invokePrivate(
                        "formatNumber",
                        new Class<?>[]{double.class},
                        10.345
                )
        );
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
    void styleMethods_returnStylesContainingColor() throws Exception {
        String scatterStyle = (String) invokePrivate(
                "getScatterStyle",
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

        assertTrue(scatterStyle.contains("#123456"));
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

            setPrivateField("scatterSearchResultsListView", resultsListView);
            setPrivateField("scatterSearchResultsContainer", resultsContainer);
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

            setPrivateField("scatterSearchResultsListView", resultsListView);
            setPrivateField("scatterSearchResultsContainer", resultsContainer);
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
    void updateRankingScatterChart_addsSeriesAndConfiguresAxes() throws Exception {
        runOnFxThread(() -> {
            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            ScatterChart<Number, Number> chart = new ScatterChart<>(xAxis, yAxis);

            setPrivateField("journalRankingScatterChart", chart);
            setPrivateField("rankingXAxis", xAxis);
            setPrivateField("rankingYAxis", yAxis);

            List<ScatterPlotsService.RankingScatterPoint> points =
                    List.of(
                            new ScatterPlotsService.RankingScatterPoint(
                                    "Journal A",
                                    "SJR",
                                    "H index",
                                    1.5,
                                    20.0
                            ),
                            new ScatterPlotsService.RankingScatterPoint(
                                    "Journal B",
                                    "SJR",
                                    "H index",
                                    2.5,
                                    30.0
                            )
                    );

            invokePrivate(
                    "updateRankingScatterChart",
                    new Class<?>[]{List.class, String.class, String.class},
                    points,
                    "SJR",
                    "H index"
            );

            assertEquals(1, chart.getData().size());
            assertEquals("Journals", chart.getData().get(0).getName());
            assertEquals(2, chart.getData().get(0).getData().size());

            assertEquals("SJR", xAxis.getLabel());
            assertEquals("H index", yAxis.getLabel());
            assertEquals("SJR vs H index", chart.getTitle());

            assertTrue(xAxis.getUpperBound() >= 2.5);
            assertTrue(yAxis.getUpperBound() >= 30.0);
        });
    }

    @Test
    void clearRankingScatterOnly_resetsRankingChartAndAxes() throws Exception {
        runOnFxThread(() -> {
            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            ScatterChart<Number, Number> chart = new ScatterChart<>(xAxis, yAxis);

            chart.setTitle("Old title");
            chart.getData().add(new javafx.scene.chart.XYChart.Series<>());

            setPrivateField("journalRankingScatterChart", chart);
            setPrivateField("rankingXAxis", xAxis);
            setPrivateField("rankingYAxis", yAxis);

            invokePrivate(
                    "clearRankingScatterOnly",
                    new Class<?>[]{}
            );

            assertTrue(chart.getData().isEmpty());
            assertEquals("", chart.getTitle());
            assertEquals("X metric", xAxis.getLabel());
            assertEquals("Y metric", yAxis.getLabel());
            assertTrue(xAxis.getUpperBound() >= 10.0);
            assertTrue(yAxis.getUpperBound() >= 10.0);
        });
    }

    @Test
    void updateVenueScatterChart_addsVenueSeriesAndLegend() throws Exception {
        runOnFxThread(() -> {
            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            ScatterChart<Number, Number> chart = new ScatterChart<>(xAxis, yAxis);

            VBox legendBox = new VBox();
            FlowPane legendFlow = new FlowPane();
            Label statusLabel = new Label();

            setPrivateField("venueScatterChart", chart);
            setPrivateField("scatterArticlesAxis", xAxis);
            setPrivateField("scatterAuthorsAxis", yAxis);
            setPrivateField("venueScatterLegendBox", legendBox);
            setPrivateField("venueScatterLegendFlow", legendFlow);
            setPrivateField("venueScatterStatusLabel", statusLabel);

            ScatterPlotsService.VenueScatterPoint point1 =
                    new ScatterPlotsService.VenueScatterPoint(
                            "Journal: Data Journal",
                            ScatterPlotsService.TYPE_JOURNAL,
                            5,
                            2020,
                            10.0,
                            3.0
                    );

            ScatterPlotsService.VenueScatterPoint point2 =
                    new ScatterPlotsService.VenueScatterPoint(
                            "Journal: Data Journal",
                            ScatterPlotsService.TYPE_JOURNAL,
                            5,
                            2021,
                            20.0,
                            4.0
                    );

            ScatterPlotsService.VenueScatterSeries series =
                    new ScatterPlotsService.VenueScatterSeries(
                            "Journal: Data Journal",
                            ScatterPlotsService.TYPE_JOURNAL,
                            5,
                            List.of(point1, point2)
                    );

            invokePrivate(
                    "updateVenueScatterChart",
                    new Class<?>[]{List.class},
                    List.of(series)
            );

            assertEquals(1, chart.getData().size());
            assertEquals(2, chart.getData().get(0).getData().size());
            assertEquals("Articles / year vs average authors / article", chart.getTitle());

            assertTrue(xAxis.getUpperBound() >= 20.0);
            assertTrue(yAxis.getUpperBound() >= 4.0);

            assertTrue(legendBox.isVisible());
            assertTrue(legendBox.isManaged());
            assertEquals(1, legendFlow.getChildren().size());

            assertEquals("Loaded 2 yearly points.", statusLabel.getText());
        });
    }

    @Test
    void clearVenueScatterOnly_resetsVenueChartAndLegend() throws Exception {
        runOnFxThread(() -> {
            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            ScatterChart<Number, Number> chart = new ScatterChart<>(xAxis, yAxis);

            VBox legendBox = new VBox();
            FlowPane legendFlow = new FlowPane();

            chart.setTitle("Old title");
            chart.getData().add(new javafx.scene.chart.XYChart.Series<>());
            legendFlow.getChildren().add(new Label("Old legend"));

            setPrivateField("venueScatterChart", chart);
            setPrivateField("scatterArticlesAxis", xAxis);
            setPrivateField("scatterAuthorsAxis", yAxis);
            setPrivateField("venueScatterLegendBox", legendBox);
            setPrivateField("venueScatterLegendFlow", legendFlow);
            setPrivateField("activeVenueKey", "Journal#5");

            invokePrivate(
                    "clearVenueScatterOnly",
                    new Class<?>[]{}
            );

            assertTrue(chart.getData().isEmpty());
            assertEquals("", chart.getTitle());
            assertFalse(legendBox.isVisible());
            assertFalse(legendBox.isManaged());
            assertTrue(legendFlow.getChildren().isEmpty());
            assertNull(getPrivateField("activeVenueKey"));
        });
    }

    @Test
    void statusMethodsSetEmptyStringForNullAndMessageForText() throws Exception {
        runOnFxThread(() -> {
            Label venueStatusLabel = new Label();
            Label rankingStatusLabel = new Label();

            setPrivateField("venueScatterStatusLabel", venueStatusLabel);
            setPrivateField("rankingStatusLabel", rankingStatusLabel);

            invokePrivate(
                    "setVenueStatus",
                    new Class<?>[]{String.class},
                    (Object) null
            );

            invokePrivate(
                    "setRankingStatus",
                    new Class<?>[]{String.class},
                    (Object) null
            );

            assertEquals("", venueStatusLabel.getText());
            assertEquals("", rankingStatusLabel.getText());

            invokePrivate(
                    "setVenueStatus",
                    new Class<?>[]{String.class},
                    "Venue ready"
            );

            invokePrivate(
                    "setRankingStatus",
                    new Class<?>[]{String.class},
                    "Ranking ready"
            );

            assertEquals("Venue ready", venueStatusLabel.getText());
            assertEquals("Ranking ready", rankingStatusLabel.getText());
        });
    }

    @Test
    void setVenueLoading_disablesAndEnablesVenueControls() throws Exception {
        runOnFxThread(() -> {
            ComboBox<String> venueTypeComboBox = new ComboBox<>();
            TextField searchField = new TextField();
            Button addButton = new Button();
            Button removeButton = new Button();
            Button clearButton = new Button();
            Button loadButton = new Button();
            ComboBox<Integer> fromYearComboBox = new ComboBox<>();
            ComboBox<Integer> toYearComboBox = new ComboBox<>();
            ListView<Object> resultsListView = new ListView<>();
            ListView<ScatterPlotsService.SelectedVenue> selectedListView = new ListView<>();

            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            ScatterChart<Number, Number> chart = new ScatterChart<>(xAxis, yAxis);

            setPrivateField("scatterVenueTypeComboBox", venueTypeComboBox);
            setPrivateField("scatterVenueSearchField", searchField);
            setPrivateField("scatterAddSelectedButton", addButton);
            setPrivateField("scatterRemoveSelectedButton", removeButton);
            setPrivateField("scatterClearSelectedButton", clearButton);
            setPrivateField("scatterLoadVenueButton", loadButton);
            setPrivateField("scatterFromYearComboBox", fromYearComboBox);
            setPrivateField("scatterToYearComboBox", toYearComboBox);
            setPrivateField("scatterSearchResultsListView", resultsListView);
            setPrivateField("scatterSelectedVenuesListView", selectedListView);
            setPrivateField("venueScatterChart", chart);

            invokePrivate(
                    "setVenueLoading",
                    new Class<?>[]{boolean.class},
                    true
            );

            assertTrue(venueTypeComboBox.isDisabled());
            assertTrue(searchField.isDisabled());
            assertTrue(addButton.isDisabled());
            assertTrue(removeButton.isDisabled());
            assertTrue(clearButton.isDisabled());
            assertTrue(loadButton.isDisabled());
            assertTrue(fromYearComboBox.isDisabled());
            assertTrue(toYearComboBox.isDisabled());
            assertTrue(resultsListView.isDisabled());
            assertTrue(selectedListView.isDisabled());
            assertTrue(chart.isDisabled());

            invokePrivate(
                    "setVenueLoading",
                    new Class<?>[]{boolean.class},
                    false
            );

            assertFalse(venueTypeComboBox.isDisabled());
            assertFalse(searchField.isDisabled());
            assertFalse(addButton.isDisabled());
            assertFalse(removeButton.isDisabled());
            assertFalse(clearButton.isDisabled());
            assertFalse(loadButton.isDisabled());
            assertFalse(fromYearComboBox.isDisabled());
            assertFalse(toYearComboBox.isDisabled());
            assertFalse(resultsListView.isDisabled());
            assertFalse(selectedListView.isDisabled());
            assertFalse(chart.isDisabled());
        });
    }

    @Test
    void setRankingLoading_disablesAndEnablesRankingControls() throws Exception {
        runOnFxThread(() -> {
            ComboBox<String> xMetricComboBox = new ComboBox<>();
            ComboBox<String> yMetricComboBox = new ComboBox<>();
            ComboBox<String> limitComboBox = new ComboBox<>();
            Button loadButton = new Button();
            Button clearButton = new Button();

            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            ScatterChart<Number, Number> chart = new ScatterChart<>(xAxis, yAxis);

            setPrivateField("rankingXMetricComboBox", xMetricComboBox);
            setPrivateField("rankingYMetricComboBox", yMetricComboBox);
            setPrivateField("rankingLimitComboBox", limitComboBox);
            setPrivateField("loadRankingScatterButton", loadButton);
            setPrivateField("clearRankingScatterButton", clearButton);
            setPrivateField("journalRankingScatterChart", chart);

            invokePrivate(
                    "setRankingLoading",
                    new Class<?>[]{boolean.class},
                    true
            );

            assertTrue(xMetricComboBox.isDisabled());
            assertTrue(yMetricComboBox.isDisabled());
            assertTrue(limitComboBox.isDisabled());
            assertTrue(loadButton.isDisabled());
            assertTrue(clearButton.isDisabled());
            assertTrue(chart.isDisabled());

            invokePrivate(
                    "setRankingLoading",
                    new Class<?>[]{boolean.class},
                    false
            );

            assertFalse(xMetricComboBox.isDisabled());
            assertFalse(yMetricComboBox.isDisabled());
            assertFalse(limitComboBox.isDisabled());
            assertFalse(loadButton.isDisabled());
            assertFalse(clearButton.isDisabled());
            assertFalse(chart.isDisabled());
        });
    }

    @Test
    void getPageScrollValue_returnsZeroWhenScrollPaneIsNullAndActualValueWhenPresent() throws Exception {
        assertEquals(
                0.0,
                invokePrivate(
                        "getPageScrollValue",
                        new Class<?>[]{}
                )
        );

        runOnFxThread(() -> {
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setVvalue(0.45);

            setPrivateField("scatterPageScrollPane", scrollPane);

            assertEquals(
                    0.45,
                    (double) invokePrivate(
                            "getPageScrollValue",
                            new Class<?>[]{}
                    )
            );
        });
    }

    private Object invokePrivate(
            String methodName,
            Class<?>[] parameterTypes,
            Object... arguments
    ) throws Exception {
        Method method = ScatterPlotsController.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);

        try {
            return method.invoke(scatterPlotsController, arguments);
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
        Field field = ScatterPlotsController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(scatterPlotsController, value);
    }

    private Object getPrivateField(String fieldName) throws Exception {
        Field field = ScatterPlotsController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(scatterPlotsController);
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