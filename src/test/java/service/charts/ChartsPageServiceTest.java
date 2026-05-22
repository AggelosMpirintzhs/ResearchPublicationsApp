package service.charts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class ChartsPageServiceTest {

    private ChartsPageService chartsPageService;

    @BeforeEach
    void setUp() {
        chartsPageService = new ChartsPageService();
    }

    @Test
    void getHomeFxmlPath_returnsExpectedPath() {
        String result = chartsPageService.getHomeFxmlPath();

        assertEquals("/app/hello-view.fxml", result);
    }

    @Test
    void getFxmlPath_returnsNull_whenChartTabIsNull() {
        String result = chartsPageService.getFxmlPath(null);

        assertNull(result);
    }

    @Test
    void getFxmlPath_returnsVenueAnalysisPath() {
        String result =
                chartsPageService.getFxmlPath(ChartsPageService.ChartTab.VENUE_ANALYSIS);

        assertEquals("/app/charts/venue-analysis-tab.fxml", result);
    }

    @Test
    void getFxmlPath_returnsVenueCategoriesPath() {
        String result =
                chartsPageService.getFxmlPath(ChartsPageService.ChartTab.VENUE_CATEGORIES);

        assertEquals("/app/charts/venue-categories-tab.fxml", result);
    }

    @Test
    void getFxmlPath_returnsPublisherAnalysisPath() {
        String result =
                chartsPageService.getFxmlPath(ChartsPageService.ChartTab.PUBLISHER_ANALYSIS);

        assertEquals("/app/charts/publisher-analysis-tab.fxml", result);
    }

    @Test
    void getFxmlPath_returnsScatterPlotsPath() {
        String result =
                chartsPageService.getFxmlPath(ChartsPageService.ChartTab.SCATTER_PLOTS);

        assertEquals("/app/charts/scatter-plots-tab.fxml", result);
    }

    @ParameterizedTest
    @EnumSource(ChartsPageService.ChartTab.class)
    void shouldLoadTab_returnsTrue_whenTabHasNotBeenLoaded(ChartsPageService.ChartTab chartTab) {
        boolean result = chartsPageService.shouldLoadTab(chartTab);

        assertTrue(result);
    }

    @Test
    void shouldLoadTab_returnsFalse_whenChartTabIsNull() {
        boolean result = chartsPageService.shouldLoadTab(null);

        assertFalse(result);
    }

    @ParameterizedTest
    @EnumSource(ChartsPageService.ChartTab.class)
    void shouldLoadTab_returnsFalse_afterTabIsMarkedAsLoaded(ChartsPageService.ChartTab chartTab) {
        chartsPageService.markTabAsLoaded(chartTab);

        boolean result = chartsPageService.shouldLoadTab(chartTab);

        assertFalse(result);
    }

    @Test
    void markTabAsLoaded_doesNothing_whenChartTabIsNull() {
        assertDoesNotThrow(() -> chartsPageService.markTabAsLoaded(null));
    }

    @Test
    void markTabAsLoaded_affectsOnlyTheGivenTab() {
        chartsPageService.markTabAsLoaded(ChartsPageService.ChartTab.VENUE_ANALYSIS);

        assertFalse(chartsPageService.shouldLoadTab(ChartsPageService.ChartTab.VENUE_ANALYSIS));
        assertTrue(chartsPageService.shouldLoadTab(ChartsPageService.ChartTab.VENUE_CATEGORIES));
        assertTrue(chartsPageService.shouldLoadTab(ChartsPageService.ChartTab.PUBLISHER_ANALYSIS));
        assertTrue(chartsPageService.shouldLoadTab(ChartsPageService.ChartTab.SCATTER_PLOTS));
    }

    @Test
    void markTabAsLoaded_canBeCalledMultipleTimesForSameTab() {
        chartsPageService.markTabAsLoaded(ChartsPageService.ChartTab.VENUE_ANALYSIS);
        chartsPageService.markTabAsLoaded(ChartsPageService.ChartTab.VENUE_ANALYSIS);
        chartsPageService.markTabAsLoaded(ChartsPageService.ChartTab.VENUE_ANALYSIS);

        assertFalse(chartsPageService.shouldLoadTab(ChartsPageService.ChartTab.VENUE_ANALYSIS));
    }

    @Test
    void shouldStartVenueCategoriesOptions_returnsFalse_whenChartTabIsNull() {
        boolean result = chartsPageService.shouldStartVenueCategoriesOptions(null);

        assertFalse(result);
    }

    @Test
    void shouldStartVenueCategoriesOptions_returnsFalse_whenTabIsNotVenueCategories() {
        assertFalse(
                chartsPageService.shouldStartVenueCategoriesOptions(
                        ChartsPageService.ChartTab.VENUE_ANALYSIS
                )
        );

        assertFalse(
                chartsPageService.shouldStartVenueCategoriesOptions(
                        ChartsPageService.ChartTab.PUBLISHER_ANALYSIS
                )
        );

        assertFalse(
                chartsPageService.shouldStartVenueCategoriesOptions(
                        ChartsPageService.ChartTab.SCATTER_PLOTS
                )
        );
    }

    @Test
    void shouldStartVenueCategoriesOptions_returnsTrueOnlyFirstTimeForVenueCategories() {
        boolean firstResult =
                chartsPageService.shouldStartVenueCategoriesOptions(
                        ChartsPageService.ChartTab.VENUE_CATEGORIES
                );

        boolean secondResult =
                chartsPageService.shouldStartVenueCategoriesOptions(
                        ChartsPageService.ChartTab.VENUE_CATEGORIES
                );

        boolean thirdResult =
                chartsPageService.shouldStartVenueCategoriesOptions(
                        ChartsPageService.ChartTab.VENUE_CATEGORIES
                );

        assertTrue(firstResult);
        assertFalse(secondResult);
        assertFalse(thirdResult);
    }

    @Test
    void shouldStartVenueCategoriesOptions_isIndependentFromLoadedTabs() {
        chartsPageService.markTabAsLoaded(ChartsPageService.ChartTab.VENUE_CATEGORIES);

        boolean result =
                chartsPageService.shouldStartVenueCategoriesOptions(
                        ChartsPageService.ChartTab.VENUE_CATEGORIES
                );

        assertTrue(result);
        assertFalse(chartsPageService.shouldLoadTab(ChartsPageService.ChartTab.VENUE_CATEGORIES));
    }
}