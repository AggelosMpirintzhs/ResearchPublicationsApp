package service.charts;

import java.util.EnumSet;
import java.util.Set;

public class ChartsPageService {

    public static final String HOME_FXML_PATH = "/app/hello-view.fxml";

    private static final String VENUE_ANALYSIS_FXML = "/app/charts/venue-analysis-tab.fxml";
    private static final String VENUE_CATEGORIES_FXML = "/app/charts/venue-categories-tab.fxml";
    private static final String PUBLISHER_ANALYSIS_FXML = "/app/charts/publisher-analysis-tab.fxml";
    private static final String SCATTER_PLOTS_FXML = "/app/charts/scatter-plots-tab.fxml";

    private final Set<ChartTab> loadedTabs = EnumSet.noneOf(ChartTab.class);

    private boolean venueCategoriesOptionsStarted = false;

    public String getHomeFxmlPath() {
        return HOME_FXML_PATH;
    }

    public String getFxmlPath(ChartTab chartTab) {
        if (chartTab == null) {
            return null;
        }

        return switch (chartTab) {
            case VENUE_ANALYSIS -> VENUE_ANALYSIS_FXML;
            case VENUE_CATEGORIES -> VENUE_CATEGORIES_FXML;
            case PUBLISHER_ANALYSIS -> PUBLISHER_ANALYSIS_FXML;
            case SCATTER_PLOTS -> SCATTER_PLOTS_FXML;
        };
    }

    public boolean shouldLoadTab(ChartTab chartTab) {
        return chartTab != null && !loadedTabs.contains(chartTab);
    }

    public void markTabAsLoaded(ChartTab chartTab) {
        if (chartTab != null) {
            loadedTabs.add(chartTab);
        }
    }

    public boolean shouldStartVenueCategoriesOptions(ChartTab chartTab) {
        if (chartTab != ChartTab.VENUE_CATEGORIES) {
            return false;
        }

        if (venueCategoriesOptionsStarted) {
            return false;
        }

        venueCategoriesOptionsStarted = true;
        return true;
    }

    public enum ChartTab {
        VENUE_ANALYSIS,
        VENUE_CATEGORIES,
        PUBLISHER_ANALYSIS,
        SCATTER_PLOTS
    }
}