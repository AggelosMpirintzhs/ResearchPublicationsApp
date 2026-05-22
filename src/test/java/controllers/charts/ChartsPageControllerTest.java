package controllers.charts;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.charts.ChartsPageService.ChartTab;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ChartsPageControllerTest {

    private ChartsPageController controller;

    private Tab venueAnalysisTab;
    private Tab venueCategoriesTab;
    private Tab publisherAnalysisTab;
    private Tab scatterPlotsTab;

    private StackPane venueAnalysisContainer;
    private StackPane venueCategoriesContainer;
    private StackPane publisherAnalysisContainer;
    private StackPane scatterPlotsContainer;

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
            controller = new ChartsPageController();

            venueAnalysisTab = new Tab("Venue Analysis");
            venueCategoriesTab = new Tab("Venue Categories");
            publisherAnalysisTab = new Tab("Publisher Analysis");
            scatterPlotsTab = new Tab("Scatter Plots");

            venueAnalysisContainer = new StackPane();
            venueCategoriesContainer = new StackPane();
            publisherAnalysisContainer = new StackPane();
            scatterPlotsContainer = new StackPane();

            setField("venueAnalysisTab", venueAnalysisTab);
            setField("venueCategoriesTab", venueCategoriesTab);
            setField("publisherAnalysisTab", publisherAnalysisTab);
            setField("scatterPlotsTab", scatterPlotsTab);

            setField("venueAnalysisContainer", venueAnalysisContainer);
            setField("venueCategoriesContainer", venueCategoriesContainer);
            setField("publisherAnalysisContainer", publisherAnalysisContainer);
            setField("scatterPlotsContainer", scatterPlotsContainer);
        });
    }

    @Test
    void initializeDoesNothingWhenTabPaneIsNull() throws Exception {
        runOnFxThreadAndWait(() -> {
            ChartsPageController controllerWithoutTabPane = new ChartsPageController();

            assertDoesNotThrow(controllerWithoutTabPane::initialize);
        });
    }

    @Test
    void resolveChartTabReturnsCorrectChartTabForEachKnownTab() throws Exception {
        runOnFxThreadAndWait(() -> {
            assertEquals(ChartTab.VENUE_ANALYSIS, invokeResolveChartTab(venueAnalysisTab));
            assertEquals(ChartTab.VENUE_CATEGORIES, invokeResolveChartTab(venueCategoriesTab));
            assertEquals(ChartTab.PUBLISHER_ANALYSIS, invokeResolveChartTab(publisherAnalysisTab));
            assertEquals(ChartTab.SCATTER_PLOTS, invokeResolveChartTab(scatterPlotsTab));
        });
    }

    @Test
    void resolveChartTabReturnsNullForNullOrUnknownTab() throws Exception {
        runOnFxThreadAndWait(() -> {
            Tab unknownTab = new Tab("Unknown");

            assertNull(invokeResolveChartTab(null));
            assertNull(invokeResolveChartTab(unknownTab));
        });
    }

    @Test
    void resolveContainerReturnsCorrectContainerForEachChartTab() throws Exception {
        runOnFxThreadAndWait(() -> {
            assertSame(venueAnalysisContainer, invokeResolveContainer(ChartTab.VENUE_ANALYSIS));
            assertSame(venueCategoriesContainer, invokeResolveContainer(ChartTab.VENUE_CATEGORIES));
            assertSame(publisherAnalysisContainer, invokeResolveContainer(ChartTab.PUBLISHER_ANALYSIS));
            assertSame(scatterPlotsContainer, invokeResolveContainer(ChartTab.SCATTER_PLOTS));
        });
    }

    @Test
    void resolveContainerReturnsNullWhenChartTabIsNull() throws Exception {
        runOnFxThreadAndWait(() -> {
            assertNull(invokeResolveContainer(null));
        });
    }

    @Test
    void loadTabContentDoesNothingWhenArgumentsAreInvalid() throws Exception {
        runOnFxThreadAndWait(() -> {
            Label existingLabel = new Label("Existing content");
            StackPane container = new StackPane(existingLabel);

            invokeLoadTabContent(null, container, "/missing.fxml");

            assertEquals(1, container.getChildren().size());
            assertSame(existingLabel, container.getChildren().getFirst());

            assertDoesNotThrow(() -> invokeLoadTabContent(ChartTab.VENUE_ANALYSIS, null, "/missing.fxml"));
            assertDoesNotThrow(() -> invokeLoadTabContent(ChartTab.VENUE_ANALYSIS, container, null));
            assertDoesNotThrow(() -> invokeLoadTabContent(ChartTab.VENUE_ANALYSIS, container, "   "));
        });
    }

    @Test
    void loadTabContentShowsErrorLabelWhenFxmlCannotBeLoaded() throws Exception {
        runOnFxThreadAndWait(() -> {
            StackPane container = new StackPane();

            invokeLoadTabContent(
                    ChartTab.VENUE_ANALYSIS,
                    container,
                    "/fxml/file-that-does-not-exist.fxml"
            );

            assertEquals(1, container.getChildren().size());
            assertInstanceOf(Label.class, container.getChildren().getFirst());

            Label errorLabel = (Label) container.getChildren().getFirst();

            assertEquals("Could not load this tab.", errorLabel.getText());
            assertTrue(errorLabel.getStyle().contains("#b00020"));
        });
    }

    @Test
    void initializeRegistersTabSelectionListener() throws Exception {
        runOnFxThreadAndWait(() -> {
            TabPane tabPane = new TabPane(
                    venueAnalysisTab,
                    venueCategoriesTab,
                    publisherAnalysisTab,
                    scatterPlotsTab
            );

            setField("chartsTabPane", tabPane);

            assertDoesNotThrow(controller::initialize);

            tabPane.getSelectionModel().select(scatterPlotsTab);

            assertEquals(scatterPlotsTab, tabPane.getSelectionModel().getSelectedItem());
        });
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = ChartsPageController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    private ChartTab invokeResolveChartTab(Tab tab) throws Exception {
        Method method = ChartsPageController.class.getDeclaredMethod("resolveChartTab", Tab.class);
        method.setAccessible(true);

        return (ChartTab) method.invoke(controller, tab);
    }

    private StackPane invokeResolveContainer(ChartTab chartTab) throws Exception {
        Method method = ChartsPageController.class.getDeclaredMethod("resolveContainer", ChartTab.class);
        method.setAccessible(true);

        return (StackPane) method.invoke(controller, chartTab);
    }

    private void invokeLoadTabContent(ChartTab chartTab, StackPane container, String fxmlPath) throws Exception {
        Method method = ChartsPageController.class.getDeclaredMethod(
                "loadTabContent",
                ChartTab.class,
                StackPane.class,
                String.class
        );

        method.setAccessible(true);
        method.invoke(controller, chartTab, container, fxmlPath);
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