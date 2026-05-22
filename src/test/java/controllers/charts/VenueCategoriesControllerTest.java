package controllers.charts;

import dto.chart.CategoryOptionDto;
import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.charts.VenueCategoriesService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VenueCategoriesControllerTest {

    private VenueCategoriesController controller;

    private ComboBox<String> categoryTypeComboBox;
    private ComboBox<CategoryOptionDto> categoryFilterComboBox;
    private ComboBox<Integer> categoryFromYearComboBox;
    private ComboBox<Integer> categoryToYearComboBox;

    private Button loadCategoryTrendButton;
    private Button clearCategoryTrendButton;
    private Label categoryStatusLabel;

    private LineChart<Number, Number> categoryTrendLineChart;
    private NumberAxis categoryYearAxis;
    private NumberAxis categoryCountAxis;

    private VBox categoryLegendBox;
    private FlowPane categoryLegendFlow;

    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException exception) {
            latch.countDown();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @BeforeEach
    void setUp() throws Exception {
        runOnFxThreadAndWait(() -> {
            controller = new VenueCategoriesController();

            categoryTypeComboBox = new ComboBox<>();
            categoryFilterComboBox = new ComboBox<>();
            categoryFromYearComboBox = new ComboBox<>();
            categoryToYearComboBox = new ComboBox<>();

            loadCategoryTrendButton = new Button();
            clearCategoryTrendButton = new Button();
            categoryStatusLabel = new Label();

            categoryYearAxis = new NumberAxis();
            categoryCountAxis = new NumberAxis();
            categoryTrendLineChart = new LineChart<>(categoryYearAxis, categoryCountAxis);

            categoryLegendBox = new VBox();
            categoryLegendFlow = new FlowPane();

            setField("categoryTypeComboBox", categoryTypeComboBox);
            setField("categoryFilterComboBox", categoryFilterComboBox);
            setField("categoryFromYearComboBox", categoryFromYearComboBox);
            setField("categoryToYearComboBox", categoryToYearComboBox);

            setField("loadCategoryTrendButton", loadCategoryTrendButton);
            setField("clearCategoryTrendButton", clearCategoryTrendButton);
            setField("categoryStatusLabel", categoryStatusLabel);

            setField("categoryTrendLineChart", categoryTrendLineChart);
            setField("categoryYearAxis", categoryYearAxis);
            setField("categoryCountAxis", categoryCountAxis);

            setField("categoryLegendBox", categoryLegendBox);
            setField("categoryLegendFlow", categoryLegendFlow);

            controller.initialize();
        });
    }

    @Test
    void initializeSetsDefaultStatusAndComboBoxValues() throws Exception {
        runOnFxThreadAndWait(() -> {
            assertEquals("Open this tab to load category filters.", categoryStatusLabel.getText());

            assertFalse(categoryTypeComboBox.getItems().isEmpty());
            assertEquals(
                    VenueCategoriesService.ANALYSIS_CONFERENCE_PRIMARY_FOR,
                    categoryTypeComboBox.getValue()
            );

            assertFalse(categoryFilterComboBox.getItems().isEmpty());
            assertNotNull(categoryFilterComboBox.getValue());

            assertFalse(categoryFromYearComboBox.isEditable());
            assertFalse(categoryToYearComboBox.isEditable());

            assertFalse(categoryFromYearComboBox.getItems().isEmpty());
            assertFalse(categoryToYearComboBox.getItems().isEmpty());
        });
    }

    @Test
    void initializeConfiguresChartCorrectly() throws Exception {
        runOnFxThreadAndWait(() -> {
            assertFalse(categoryTrendLineChart.getAnimated());
            assertTrue(categoryTrendLineChart.getCreateSymbols());
            assertFalse(categoryTrendLineChart.isLegendVisible());
            assertEquals("", categoryTrendLineChart.getTitle());

            assertEquals("Year", categoryYearAxis.getLabel());
            assertFalse(categoryYearAxis.isAutoRanging());
            assertFalse(categoryYearAxis.isForceZeroInRange());
            assertFalse(categoryYearAxis.isMinorTickVisible());

            assertEquals("Venues count", categoryCountAxis.getLabel());
            assertFalse(categoryCountAxis.isAutoRanging());
            assertTrue(categoryCountAxis.isForceZeroInRange());
            assertFalse(categoryCountAxis.isMinorTickVisible());
        });
    }

    @Test
    void clearCategoryTrendsClearsYearSelectionsChartAndStatus() throws Exception {
        runOnFxThreadAndWait(() -> {
            categoryFromYearComboBox.getSelectionModel().select(0);
            categoryToYearComboBox.getSelectionModel().select(0);

            categoryTrendLineChart.setTitle("Old title");
            categoryLegendFlow.getChildren().add(new Label("Old legend"));

            invokeNoArgPrivateMethod("clearCategoryTrends");

            assertNull(categoryFromYearComboBox.getValue());
            assertNull(categoryToYearComboBox.getValue());

            assertTrue(categoryTrendLineChart.getData().isEmpty());
            assertEquals("", categoryTrendLineChart.getTitle());

            assertTrue(categoryLegendFlow.getChildren().isEmpty());
            assertFalse(categoryLegendBox.isVisible());
            assertFalse(categoryLegendBox.isManaged());

            assertEquals("Choose category type and load yearly trends.", categoryStatusLabel.getText());
        });
    }

    @Test
    void clearChartOnlyClearsChartAndHidesLegend() throws Exception {
        runOnFxThreadAndWait(() -> {
            categoryTrendLineChart.setTitle("Old chart title");
            categoryLegendFlow.getChildren().add(new Label("Old legend"));

            invokeNoArgPrivateMethod("clearChartOnly");

            assertTrue(categoryTrendLineChart.getData().isEmpty());
            assertEquals("", categoryTrendLineChart.getTitle());

            assertTrue(categoryLegendFlow.getChildren().isEmpty());
            assertFalse(categoryLegendBox.isVisible());
            assertFalse(categoryLegendBox.isManaged());

            assertEquals(0.0, categoryCountAxis.getLowerBound());
            assertTrue(categoryCountAxis.getUpperBound() >= 10.0);
            assertTrue(categoryCountAxis.getTickUnit() > 0);
        });
    }

    @Test
    void setDefaultCategoryOptionsAddsDefaultOptionAndSelectsFirst() throws Exception {
        runOnFxThreadAndWait(() -> {
            categoryFilterComboBox.getItems().clear();

            invokeNoArgPrivateMethod("setDefaultCategoryOptions");

            assertFalse(categoryFilterComboBox.getItems().isEmpty());
            assertNotNull(categoryFilterComboBox.getValue());
            assertEquals(
                    categoryFilterComboBox.getItems().get(0),
                    categoryFilterComboBox.getValue()
            );
            assertNotNull(categoryFilterComboBox.getPromptText());
        });
    }

    @Test
    void getSelectedCategoryFilterReturnsNonNullValueForDefaultSelection() throws Exception {
        runOnFxThreadAndWait(() -> {
            Object result = invokeNoArgPrivateMethodWithReturn("getSelectedCategoryFilter");

            assertNotNull(result);
            assertInstanceOf(String.class, result);
        });
    }

    @Test
    void refreshToYearOptionsKeepsValidSelectedYear() throws Exception {
        runOnFxThreadAndWait(() -> {
            Integer fromYear = categoryFromYearComboBox.getItems().get(0);
            Integer validToYear = categoryToYearComboBox.getItems()
                    .stream()
                    .filter(year -> year >= fromYear)
                    .findFirst()
                    .orElseThrow();

            categoryToYearComboBox.getSelectionModel().select(validToYear);

            invokeRefreshToYearOptions(fromYear);

            assertEquals(validToYear, categoryToYearComboBox.getValue());
            assertTrue(categoryToYearComboBox.getItems().contains(validToYear));
        });
    }

    @Test
    void refreshToYearOptionsClearsInvalidSelectedYear() throws Exception {
        runOnFxThreadAndWait(() -> {
            Integer lastYear = categoryToYearComboBox.getItems()
                    .get(categoryToYearComboBox.getItems().size() - 1);

            Integer invalidToYear = categoryToYearComboBox.getItems().get(0);

            if (invalidToYear >= lastYear) {
                return;
            }

            categoryToYearComboBox.getSelectionModel().select(invalidToYear);

            invokeRefreshToYearOptions(lastYear);

            assertNull(categoryToYearComboBox.getValue());
        });
    }

    @Test
    void configureYearAxisUsesAutoRangeWhenYearsAreNull() throws Exception {
        runOnFxThreadAndWait(() -> {
            NumberAxis axis = new NumberAxis();

            invokeConfigureYearAxis(axis, null, null);

            assertTrue(axis.isAutoRanging());
        });
    }

    @Test
    void configureYearAxisExpandsSingleYearRange() throws Exception {
        runOnFxThreadAndWait(() -> {
            NumberAxis axis = new NumberAxis();

            invokeConfigureYearAxis(axis, 2020, 2020);

            assertFalse(axis.isAutoRanging());
            assertEquals(2019.0, axis.getLowerBound());
            assertEquals(2021.0, axis.getUpperBound());
            assertEquals(1.0, axis.getTickUnit());
            assertFalse(axis.isMinorTickVisible());
        });
    }

    @Test
    void configureYearAxisUsesGivenRange() throws Exception {
        runOnFxThreadAndWait(() -> {
            NumberAxis axis = new NumberAxis();

            invokeConfigureYearAxis(axis, 2000, 2020);

            assertFalse(axis.isAutoRanging());
            assertEquals(2000.0, axis.getLowerBound());
            assertEquals(2020.0, axis.getUpperBound());
            assertEquals(5.0, axis.getTickUnit());
            assertFalse(axis.isMinorTickVisible());
        });
    }

    @Test
    void calculateYearTickUnitReturnsExpectedValues() throws Exception {
        runOnFxThreadAndWait(() -> {
            assertEquals(1, invokeIntPrivateMethod("calculateYearTickUnit", 10));
            assertEquals(5, invokeIntPrivateMethod("calculateYearTickUnit", 25));
            assertEquals(10, invokeIntPrivateMethod("calculateYearTickUnit", 60));
            assertEquals(20, invokeIntPrivateMethod("calculateYearTickUnit", 61));
        });
    }

    @Test
    void configureValueAxisUsesZeroLowerBoundAndPositiveUpperBound() throws Exception {
        runOnFxThreadAndWait(() -> {
            NumberAxis axis = new NumberAxis();

            invokeConfigureValueAxis(axis, 123);

            assertFalse(axis.isAutoRanging());
            assertEquals(0.0, axis.getLowerBound());
            assertTrue(axis.getUpperBound() >= 123);
            assertTrue(axis.getTickUnit() > 0);
            assertFalse(axis.isMinorTickVisible());
        });
    }

    @Test
    void calculateNiceUpperBoundReturnsDefaultForZeroOrNegativeValues() throws Exception {
        runOnFxThreadAndWait(() -> {
            assertEquals(10.0, invokeDoublePrivateMethod("calculateNiceUpperBound", -5.0));
            assertEquals(10.0, invokeDoublePrivateMethod("calculateNiceUpperBound", 0.0));
        });
    }

    @Test
    void calculateNiceTickUnitReturnsDefaultForZeroOrNegativeUpperBound() throws Exception {
        runOnFxThreadAndWait(() -> {
            assertEquals(1.0, invokeDoublePrivateMethod("calculateNiceTickUnit", -10.0));
            assertEquals(1.0, invokeDoublePrivateMethod("calculateNiceTickUnit", 0.0));
        });
    }

    @Test
    void calculateNiceRawTickUnitReturnsExpectedNiceValues() throws Exception {
        runOnFxThreadAndWait(() -> {
            assertEquals(1.0, invokeDoublePrivateMethod("calculateNiceRawTickUnit", 0.0));
            assertEquals(1.0, invokeDoublePrivateMethod("calculateNiceRawTickUnit", 0.8));
            assertEquals(2.0, invokeDoublePrivateMethod("calculateNiceRawTickUnit", 1.5));
            assertEquals(2.5, invokeDoublePrivateMethod("calculateNiceRawTickUnit", 2.2));
            assertEquals(5.0, invokeDoublePrivateMethod("calculateNiceRawTickUnit", 4.0));
            assertEquals(10.0, invokeDoublePrivateMethod("calculateNiceRawTickUnit", 8.0));
        });
    }

    @Test
    void getChartColorCyclesThroughAvailableColors() throws Exception {
        runOnFxThreadAndWait(() -> {
            assertEquals("#1f5fa8", invokeGetChartColor(0));
            assertEquals("#f5a623", invokeGetChartColor(1));
            assertEquals("#1f5fa8", invokeGetChartColor(12));
        });
    }

    @Test
    void shortenNameHandlesNullBlankShortAndLongNames() throws Exception {
        runOnFxThreadAndWait(() -> {
            assertEquals("-", invokeShortenName(null));
            assertEquals("-", invokeShortenName("   "));
            assertEquals("Short category", invokeShortenName("Short category"));

            String longName = "A".repeat(50);
            String shortened = invokeShortenName(longName);

            assertEquals(42, shortened.length());
            assertTrue(shortened.endsWith("..."));
        });
    }

    @Test
    void setLoadingDisablesAndEnablesControls() throws Exception {
        runOnFxThreadAndWait(() -> {
            invokeSetLoading(true);

            assertTrue(categoryTypeComboBox.isDisabled());
            assertTrue(categoryFilterComboBox.isDisabled());
            assertTrue(categoryFromYearComboBox.isDisabled());
            assertTrue(categoryToYearComboBox.isDisabled());
            assertTrue(loadCategoryTrendButton.isDisabled());
            assertTrue(clearCategoryTrendButton.isDisabled());
            assertTrue(categoryTrendLineChart.isDisabled());

            invokeSetLoading(false);

            assertFalse(categoryTypeComboBox.isDisabled());
            assertFalse(categoryFilterComboBox.isDisabled());
            assertFalse(categoryFromYearComboBox.isDisabled());
            assertFalse(categoryToYearComboBox.isDisabled());
            assertFalse(loadCategoryTrendButton.isDisabled());
            assertFalse(clearCategoryTrendButton.isDisabled());
            assertFalse(categoryTrendLineChart.isDisabled());
        });
    }

    @Test
    void setStatusWritesEmptyStringWhenMessageIsNull() throws Exception {
        runOnFxThreadAndWait(() -> {
            invokeSetStatus(null);

            assertEquals("", categoryStatusLabel.getText());
        });
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = VenueCategoriesController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    private Object invokeNoArgPrivateMethodWithReturn(String methodName) throws Exception {
        Method method = VenueCategoriesController.class.getDeclaredMethod(methodName);
        method.setAccessible(true);

        return method.invoke(controller);
    }

    private void invokeNoArgPrivateMethod(String methodName) throws Exception {
        Method method = VenueCategoriesController.class.getDeclaredMethod(methodName);
        method.setAccessible(true);

        method.invoke(controller);
    }

    private void invokeRefreshToYearOptions(Integer fromYear) throws Exception {
        Method method = VenueCategoriesController.class.getDeclaredMethod(
                "refreshToYearOptions",
                Integer.class
        );

        method.setAccessible(true);
        method.invoke(controller, fromYear);
    }

    private void invokeConfigureYearAxis(NumberAxis axis, Integer minYear, Integer maxYear) throws Exception {
        Method method = VenueCategoriesController.class.getDeclaredMethod(
                "configureYearAxis",
                NumberAxis.class,
                Integer.class,
                Integer.class
        );

        method.setAccessible(true);
        method.invoke(controller, axis, minYear, maxYear);
    }

    private int invokeIntPrivateMethod(String methodName, int value) throws Exception {
        Method method = VenueCategoriesController.class.getDeclaredMethod(methodName, int.class);
        method.setAccessible(true);

        return (int) method.invoke(controller, value);
    }

    private void invokeConfigureValueAxis(NumberAxis axis, double maxValue) throws Exception {
        Method method = VenueCategoriesController.class.getDeclaredMethod(
                "configureValueAxis",
                NumberAxis.class,
                double.class
        );

        method.setAccessible(true);
        method.invoke(controller, axis, maxValue);
    }

    private double invokeDoublePrivateMethod(String methodName, double value) throws Exception {
        Method method = VenueCategoriesController.class.getDeclaredMethod(methodName, double.class);
        method.setAccessible(true);

        return (double) method.invoke(controller, value);
    }

    private String invokeGetChartColor(int index) throws Exception {
        Method method = VenueCategoriesController.class.getDeclaredMethod("getChartColor", int.class);
        method.setAccessible(true);

        return (String) method.invoke(controller, index);
    }

    private String invokeShortenName(String name) throws Exception {
        Method method = VenueCategoriesController.class.getDeclaredMethod("shortenName", String.class);
        method.setAccessible(true);

        return (String) method.invoke(controller, name);
    }

    private void invokeSetLoading(boolean loading) throws Exception {
        Method method = VenueCategoriesController.class.getDeclaredMethod("setLoading", boolean.class);
        method.setAccessible(true);

        method.invoke(controller, loading);
    }

    private void invokeSetStatus(String message) throws Exception {
        Method method = VenueCategoriesController.class.getDeclaredMethod("setStatus", String.class);
        method.setAccessible(true);

        method.invoke(controller, message);
    }

    private static void runOnFxThreadAndWait(ThrowingRunnable action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                error.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        if (error.get() != null) {
            if (error.get() instanceof Exception exception) {
                throw exception;
            }

            if (error.get() instanceof AssertionError assertionError) {
                throw assertionError;
            }

            throw new RuntimeException(error.get());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}