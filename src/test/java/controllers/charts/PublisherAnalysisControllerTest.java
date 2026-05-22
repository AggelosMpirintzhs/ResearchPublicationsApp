package controllers.charts;

import dto.chart.PublisherOptionDto;
import javafx.application.Platform;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PublisherAnalysisControllerTest {

    private PublisherAnalysisController controller;

    private TextField publisherFilterField;
    private Button addPublisherButton;
    private Button removeSelectedPublisherButton;
    private Button clearPublisherAnalysisButton;
    private Button loadPublisherAnalysisButton;
    private Label publisherStatusLabel;

    private VBox publisherResultsContainer;
    private ListView<PublisherOptionDto> publisherResultsListView;
    private ListView<PublisherOptionDto> selectedPublishersListView;

    private BarChart<String, Number> publisherQuartileBarChart;
    private CategoryAxis publisherCategoryAxis;
    private NumberAxis publisherCountAxis;
    private VBox publisherLegendBox;
    private FlowPane publisherLegendFlow;

    private VBox publisherTotalChartBox;
    private BarChart<String, Number> publisherTotalBarChart;
    private CategoryAxis publisherTotalCategoryAxis;
    private NumberAxis publisherTotalCountAxis;
    private VBox publisherTotalLegendBox;
    private FlowPane publisherTotalLegendFlow;

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
            controller = new PublisherAnalysisController();

            publisherFilterField = new TextField();
            addPublisherButton = new Button();
            removeSelectedPublisherButton = new Button();
            clearPublisherAnalysisButton = new Button();
            loadPublisherAnalysisButton = new Button();
            publisherStatusLabel = new Label();

            publisherResultsContainer = new VBox();
            publisherResultsListView = new ListView<>();
            selectedPublishersListView = new ListView<>();

            publisherCategoryAxis = new CategoryAxis();
            publisherCountAxis = new NumberAxis();
            publisherQuartileBarChart = new BarChart<>(publisherCategoryAxis, publisherCountAxis);
            publisherLegendBox = new VBox();
            publisherLegendFlow = new FlowPane();

            publisherTotalChartBox = new VBox();
            publisherTotalCategoryAxis = new CategoryAxis();
            publisherTotalCountAxis = new NumberAxis();
            publisherTotalBarChart = new BarChart<>(publisherTotalCategoryAxis, publisherTotalCountAxis);
            publisherTotalLegendBox = new VBox();
            publisherTotalLegendFlow = new FlowPane();

            setField("publisherFilterField", publisherFilterField);
            setField("addPublisherButton", addPublisherButton);
            setField("removeSelectedPublisherButton", removeSelectedPublisherButton);
            setField("clearPublisherAnalysisButton", clearPublisherAnalysisButton);
            setField("loadPublisherAnalysisButton", loadPublisherAnalysisButton);
            setField("publisherStatusLabel", publisherStatusLabel);

            setField("publisherResultsContainer", publisherResultsContainer);
            setField("publisherResultsListView", publisherResultsListView);
            setField("selectedPublishersListView", selectedPublishersListView);

            setField("publisherQuartileBarChart", publisherQuartileBarChart);
            setField("publisherCategoryAxis", publisherCategoryAxis);
            setField("publisherCountAxis", publisherCountAxis);
            setField("publisherLegendBox", publisherLegendBox);
            setField("publisherLegendFlow", publisherLegendFlow);

            setField("publisherTotalChartBox", publisherTotalChartBox);
            setField("publisherTotalBarChart", publisherTotalBarChart);
            setField("publisherTotalCategoryAxis", publisherTotalCategoryAxis);
            setField("publisherTotalCountAxis", publisherTotalCountAxis);
            setField("publisherTotalLegendBox", publisherTotalLegendBox);
            setField("publisherTotalLegendFlow", publisherTotalLegendFlow);

            controller.initialize();
        });
    }

    @Test
    void initializeSetsDefaultStatusAndInitialUiState() throws Exception {
        runOnFxThreadAndWait(() -> {
            assertEquals(
                    "Search for publishers, add up to 10, and load the analysis.",
                    publisherStatusLabel.getText()
            );

            assertFalse(publisherResultsContainer.isVisible());
            assertFalse(publisherResultsContainer.isManaged());

            assertFalse(publisherTotalChartBox.isVisible());
            assertFalse(publisherTotalChartBox.isManaged());

            assertFalse(publisherTotalBarChart.isVisible());
            assertFalse(publisherTotalBarChart.isManaged());

            assertEquals("Publisher", publisherCategoryAxis.getLabel());
            assertEquals("Publications count", publisherCountAxis.getLabel());

            assertEquals("Publisher", publisherTotalCategoryAxis.getLabel());
            assertEquals("Total publications", publisherTotalCountAxis.getLabel());

            assertFalse(publisherQuartileBarChart.getAnimated());
            assertFalse(publisherQuartileBarChart.isLegendVisible());

            assertFalse(publisherTotalBarChart.getAnimated());
            assertFalse(publisherTotalBarChart.isLegendVisible());
        });
    }

    @Test
    void renderSearchResultsShowsContainerAndAddsResults() throws Exception {
        runOnFxThreadAndWait(() -> {
            PublisherOptionDto publisherOne = createPublisherOption(1, "Springer");
            PublisherOptionDto publisherTwo = createPublisherOption(2, "Elsevier");

            invokeRenderSearchResults(List.of(publisherOne, publisherTwo));

            assertTrue(publisherResultsContainer.isVisible());
            assertTrue(publisherResultsContainer.isManaged());

            assertEquals(2, publisherResultsListView.getItems().size());
            assertSame(publisherOne, publisherResultsListView.getItems().get(0));
            assertSame(publisherTwo, publisherResultsListView.getItems().get(1));
        });
    }

    @Test
    void renderSearchResultsWithEmptyListShowsNoMatchingPlaceholder() throws Exception {
        runOnFxThreadAndWait(() -> {
            invokeRenderSearchResults(List.of());

            assertTrue(publisherResultsContainer.isVisible());
            assertTrue(publisherResultsContainer.isManaged());

            assertTrue(publisherResultsListView.getItems().isEmpty());
            assertInstanceOf(Label.class, publisherResultsListView.getPlaceholder());

            Label placeholder = (Label) publisherResultsListView.getPlaceholder();

            assertEquals("No matching results", placeholder.getText());
        });
    }

    @Test
    void clearSearchResultsHidesContainerAndClearsList() throws Exception {
        runOnFxThreadAndWait(() -> {
            PublisherOptionDto publisher = createPublisherOption(1, "Springer");

            invokeRenderSearchResults(List.of(publisher));
            publisherResultsListView.getSelectionModel().select(publisher);

            invokeNoArgPrivateMethod("clearSearchResults");

            assertFalse(publisherResultsContainer.isVisible());
            assertFalse(publisherResultsContainer.isManaged());

            assertTrue(publisherResultsListView.getItems().isEmpty());
            assertNull(publisherResultsListView.getSelectionModel().getSelectedItem());

            assertInstanceOf(Label.class, publisherResultsListView.getPlaceholder());

            Label placeholder = (Label) publisherResultsListView.getPlaceholder();

            assertEquals("Search results will appear here.", placeholder.getText());
        });
    }

    @Test
    void addSelectedPublisherAddsPublisherToSelectedList() throws Exception {
        runOnFxThreadAndWait(() -> {
            PublisherOptionDto publisher = createPublisherOption(1, "Springer");

            invokeAddSelectedPublisherFromSearch(publisher, false);

            assertEquals(1, selectedPublishersListView.getItems().size());
            assertSame(publisher, selectedPublishersListView.getItems().get(0));

            assertEquals(
                    "Publisher added. Load the analysis when your selection is ready.",
                    publisherStatusLabel.getText()
            );
        });
    }

    @Test
    void addSelectedPublisherDoesNotAddNullPublisher() throws Exception {
        runOnFxThreadAndWait(() -> {
            invokeAddSelectedPublisherFromSearch(null, false);

            assertTrue(selectedPublishersListView.getItems().isEmpty());
        });
    }

    @Test
    void addSelectedPublisherDoesNotAddMoreThanTenPublishers() throws Exception {
        runOnFxThreadAndWait(() -> {
            for (int index = 1; index <= 10; index++) {
                invokeAddSelectedPublisherFromSearch(
                        createPublisherOption(index, "Publisher " + index),
                        false
                );
            }

            invokeAddSelectedPublisherFromSearch(
                    createPublisherOption(11, "Publisher 11"),
                    false
            );

            assertEquals(10, selectedPublishersListView.getItems().size());
        });
    }

    @Test
    void removeSelectedPublisherRemovesPublisherFromSelectedList() throws Exception {
        runOnFxThreadAndWait(() -> {
            PublisherOptionDto publisherOne = createPublisherOption(1, "Springer");
            PublisherOptionDto publisherTwo = createPublisherOption(2, "Elsevier");

            selectedPublishersListView.getItems().addAll(publisherOne, publisherTwo);
            selectedPublishersListView.getSelectionModel().select(publisherOne);

            invokeNoArgPrivateMethod("removeSelectedPublisher");

            assertEquals(1, selectedPublishersListView.getItems().size());
            assertSame(publisherTwo, selectedPublishersListView.getItems().get(0));

            assertEquals("Publisher removed.", publisherStatusLabel.getText());
        });
    }

    @Test
    void clearPublisherAnalysisClearsSearchSelectionAndCharts() throws Exception {
        runOnFxThreadAndWait(() -> {
            publisherFilterField.setText("springer");

            PublisherOptionDto publisher = createPublisherOption(1, "Springer");
            publisherResultsListView.getItems().add(publisher);
            selectedPublishersListView.getItems().add(publisher);

            publisherQuartileBarChart.getData().add(new XYChart.Series<>());
            publisherCategoryAxis.getCategories().add("Springer");

            publisherTotalBarChart.getData().add(new XYChart.Series<>());
            publisherTotalCategoryAxis.getCategories().add("Springer");

            invokeNoArgPrivateMethod("clearPublisherAnalysis");

            assertEquals("", publisherFilterField.getText());
            assertTrue(publisherResultsListView.getItems().isEmpty());
            assertTrue(selectedPublishersListView.getItems().isEmpty());

            assertTrue(publisherQuartileBarChart.getData().isEmpty());
            assertTrue(publisherCategoryAxis.getCategories().isEmpty());

            assertTrue(publisherTotalBarChart.getData().isEmpty());
            assertTrue(publisherTotalCategoryAxis.getCategories().isEmpty());

            assertFalse(publisherTotalChartBox.isVisible());
            assertFalse(publisherTotalChartBox.isManaged());

            assertEquals(
                    "Search for publishers, add up to 10, and load the analysis.",
                    publisherStatusLabel.getText()
            );
        });
    }

    @Test
    void clearChartsClearsQuartileAndTotalCharts() throws Exception {
        runOnFxThreadAndWait(() -> {
            publisherQuartileBarChart.getData().add(new XYChart.Series<>());
            publisherCategoryAxis.getCategories().add("Springer");

            publisherTotalBarChart.getData().add(new XYChart.Series<>());
            publisherTotalCategoryAxis.getCategories().add("Elsevier");

            publisherLegendFlow.getChildren().add(new Label("Q1"));
            publisherTotalLegendFlow.getChildren().add(new Label("Total"));

            invokeNoArgPrivateMethod("clearCharts");

            assertTrue(publisherQuartileBarChart.getData().isEmpty());
            assertTrue(publisherCategoryAxis.getCategories().isEmpty());

            assertTrue(publisherTotalBarChart.getData().isEmpty());
            assertTrue(publisherTotalCategoryAxis.getCategories().isEmpty());

            assertTrue(publisherTotalLegendFlow.getChildren().isEmpty());

            assertFalse(publisherTotalLegendBox.isVisible());
            assertFalse(publisherTotalLegendBox.isManaged());

            assertFalse(publisherTotalChartBox.isVisible());
            assertFalse(publisherTotalChartBox.isManaged());

            assertEquals(0.0, publisherCountAxis.getLowerBound());
            assertTrue(publisherCountAxis.getUpperBound() >= 10.0);
            assertTrue(publisherCountAxis.getTickUnit() > 0);

            assertEquals(0.0, publisherTotalCountAxis.getLowerBound());
            assertTrue(publisherTotalCountAxis.getUpperBound() >= 10.0);
            assertTrue(publisherTotalCountAxis.getTickUnit() > 0);
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
            String firstColor = invokeGetChartColor(0);
            String eleventhColor = invokeGetChartColor(10);

            assertEquals("#1f5fa8", firstColor);
            assertEquals(firstColor, eleventhColor);
        });
    }

    @Test
    void setLoadingDisablesAndEnablesControls() throws Exception {
        runOnFxThreadAndWait(() -> {
            invokeSetLoading(true);

            assertTrue(publisherFilterField.isDisabled());
            assertTrue(addPublisherButton.isDisabled());
            assertTrue(removeSelectedPublisherButton.isDisabled());
            assertTrue(clearPublisherAnalysisButton.isDisabled());
            assertTrue(loadPublisherAnalysisButton.isDisabled());
            assertTrue(publisherResultsListView.isDisabled());
            assertTrue(selectedPublishersListView.isDisabled());
            assertTrue(publisherQuartileBarChart.isDisabled());
            assertTrue(publisherTotalBarChart.isDisabled());

            invokeSetLoading(false);

            assertFalse(publisherFilterField.isDisabled());
            assertFalse(addPublisherButton.isDisabled());
            assertFalse(removeSelectedPublisherButton.isDisabled());
            assertFalse(clearPublisherAnalysisButton.isDisabled());
            assertFalse(loadPublisherAnalysisButton.isDisabled());
            assertFalse(publisherResultsListView.isDisabled());
            assertFalse(selectedPublishersListView.isDisabled());
            assertFalse(publisherQuartileBarChart.isDisabled());
            assertFalse(publisherTotalBarChart.isDisabled());
        });
    }

    @Test
    void setStatusWritesEmptyStringWhenMessageIsNull() throws Exception {
        runOnFxThreadAndWait(() -> {
            invokeSetStatus(null);

            assertEquals("", publisherStatusLabel.getText());
        });
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = PublisherAnalysisController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    private void invokeRenderSearchResults(List<PublisherOptionDto> results) throws Exception {
        Method method = PublisherAnalysisController.class.getDeclaredMethod(
                "renderSearchResults",
                List.class
        );

        method.setAccessible(true);
        method.invoke(controller, results);
    }

    private void invokeAddSelectedPublisherFromSearch(
            PublisherOptionDto publisher,
            boolean showMessages
    ) throws Exception {
        Method method = PublisherAnalysisController.class.getDeclaredMethod(
                "addSelectedPublisherFromSearch",
                PublisherOptionDto.class,
                boolean.class
        );

        method.setAccessible(true);
        method.invoke(controller, publisher, showMessages);
    }

    private void invokeConfigureValueAxis(NumberAxis axis, double maxValue) throws Exception {
        Method method = PublisherAnalysisController.class.getDeclaredMethod(
                "configureValueAxis",
                NumberAxis.class,
                double.class
        );

        method.setAccessible(true);
        method.invoke(controller, axis, maxValue);
    }

    private double invokeDoublePrivateMethod(String methodName, double value) throws Exception {
        Method method = PublisherAnalysisController.class.getDeclaredMethod(methodName, double.class);
        method.setAccessible(true);

        return (double) method.invoke(controller, value);
    }

    private String invokeGetChartColor(int index) throws Exception {
        Method method = PublisherAnalysisController.class.getDeclaredMethod("getChartColor", int.class);
        method.setAccessible(true);

        return (String) method.invoke(controller, index);
    }

    private void invokeSetLoading(boolean loading) throws Exception {
        Method method = PublisherAnalysisController.class.getDeclaredMethod("setLoading", boolean.class);
        method.setAccessible(true);
        method.invoke(controller, loading);
    }

    private void invokeSetStatus(String message) throws Exception {
        Method method = PublisherAnalysisController.class.getDeclaredMethod("setStatus", String.class);
        method.setAccessible(true);
        method.invoke(controller, message);
    }

    private void invokeNoArgPrivateMethod(String methodName) throws Exception {
        Method method = PublisherAnalysisController.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(controller);
    }

    private PublisherOptionDto createPublisherOption(int publisherId, String publisherName) throws Exception {
        Constructor<?>[] constructors = PublisherOptionDto.class.getDeclaredConstructors();

        Constructor<?> constructor = java.util.Arrays.stream(constructors)
                .min(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow();

        constructor.setAccessible(true);

        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Parameter[] parameters = constructor.getParameters();

        Object[] arguments = new Object[parameterTypes.length];

        for (int index = 0; index < parameterTypes.length; index++) {
            String parameterName = parameters[index].getName().toLowerCase();

            arguments[index] = createValueForParameter(
                    parameterTypes[index],
                    parameterName,
                    index,
                    publisherId,
                    publisherName
            );
        }

        return (PublisherOptionDto) constructor.newInstance(arguments);
    }

    private Object createValueForParameter(
            Class<?> type,
            String parameterName,
            int index,
            int publisherId,
            String publisherName
    ) {
        if (type == int.class || type == Integer.class) {
            return parameterName.contains("id") || index == 0 ? publisherId : 0;
        }

        if (type == long.class || type == Long.class) {
            return parameterName.contains("id") || index == 0 ? (long) publisherId : 0L;
        }

        if (type == double.class || type == Double.class) {
            return 0.0;
        }

        if (type == float.class || type == Float.class) {
            return 0.0f;
        }

        if (type == boolean.class || type == Boolean.class) {
            return false;
        }

        if (type == String.class) {
            if (parameterName.contains("name") || parameterName.contains("publisher") || index == 1) {
                return publisherName;
            }

            return "";
        }

        if (List.class.isAssignableFrom(type)) {
            return List.of();
        }

        if (Map.class.isAssignableFrom(type)) {
            return Map.of();
        }

        return null;
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