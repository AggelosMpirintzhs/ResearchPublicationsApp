package controllers.charts;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import service.charts.ChartsPageService;
import service.charts.ChartsPageService.ChartTab;

import java.io.IOException;
import java.util.Objects;

public class ChartsPageController {

    private final ChartsPageService chartsPageService = new ChartsPageService();

    @FXML
    private Button backButton;

    @FXML
    private TabPane chartsTabPane;

    @FXML
    private Tab venueAnalysisTab;

    @FXML
    private Tab venueCategoriesTab;

    @FXML
    private Tab publisherAnalysisTab;

    @FXML
    private Tab scatterPlotsTab;

    @FXML
    private StackPane venueAnalysisContainer;

    @FXML
    private StackPane venueCategoriesContainer;

    @FXML
    private StackPane publisherAnalysisContainer;

    @FXML
    private StackPane scatterPlotsContainer;

    @FXML
    public void initialize() {
        if (chartsTabPane == null) {
            return;
        }

        chartsTabPane.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldTab, newTab) -> loadSelectedTab(newTab));

        Platform.runLater(() -> loadSelectedTab(chartsTabPane.getSelectionModel().getSelectedItem()));
    }

    private void loadSelectedTab(Tab selectedTab) {
        ChartTab chartTab = resolveChartTab(selectedTab);

        if (chartTab == null) {
            return;
        }

        StackPane container = resolveContainer(chartTab);
        String fxmlPath = chartsPageService.getFxmlPath(chartTab);

        loadTabContent(chartTab, container, fxmlPath);
    }

    private ChartTab resolveChartTab(Tab selectedTab) {
        if (selectedTab == null) {
            return null;
        }

        if (selectedTab == venueAnalysisTab) {
            return ChartTab.VENUE_ANALYSIS;
        }

        if (selectedTab == venueCategoriesTab) {
            return ChartTab.VENUE_CATEGORIES;
        }

        if (selectedTab == publisherAnalysisTab) {
            return ChartTab.PUBLISHER_ANALYSIS;
        }

        if (selectedTab == scatterPlotsTab) {
            return ChartTab.SCATTER_PLOTS;
        }

        return null;
    }

    private StackPane resolveContainer(ChartTab chartTab) {
        if (chartTab == null) {
            return null;
        }

        return switch (chartTab) {
            case VENUE_ANALYSIS -> venueAnalysisContainer;
            case VENUE_CATEGORIES -> venueCategoriesContainer;
            case PUBLISHER_ANALYSIS -> publisherAnalysisContainer;
            case SCATTER_PLOTS -> scatterPlotsContainer;
        };
    }

    private void loadTabContent(ChartTab chartTab, StackPane container, String fxmlPath) {
        if (chartTab == null || container == null || fxmlPath == null || fxmlPath.isBlank()) {
            return;
        }

        if (!chartsPageService.shouldLoadTab(chartTab)) {
            return;
        }

        Label loadingLabel = new Label("Loading...");
        loadingLabel.setStyle("-fx-text-fill: #0f4c81; -fx-font-weight: 800; -fx-padding: 28;");

        container.getChildren().setAll(loadingLabel);

        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(
                            getClass().getResource(fxmlPath),
                            "Cannot find FXML file: " + fxmlPath
                    )
            );

            Parent content = loader.load();

            container.getChildren().setAll(content);
            chartsPageService.markTabAsLoaded(chartTab);

            startExtraTabInitializationIfNeeded(chartTab, loader);

        } catch (IOException | NullPointerException exception) {
            exception.printStackTrace();

            Label errorLabel = new Label("Could not load this tab.");
            errorLabel.setStyle("-fx-text-fill: #b00020; -fx-font-weight: 800; -fx-padding: 28;");

            container.getChildren().setAll(errorLabel);
        }
    }

    private void startExtraTabInitializationIfNeeded(ChartTab chartTab, FXMLLoader loader) {
        if (!chartsPageService.shouldStartVenueCategoriesOptions(chartTab)) {
            return;
        }

        Object controller = loader.getController();

        if (controller instanceof VenueCategoriesController venueCategoriesController) {
            venueCategoriesController.loadCategoryOptionsInBackground();
        }
    }

    @FXML
    private void backToHome() {
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(
                            getClass().getResource(chartsPageService.getHomeFxmlPath()),
                            "Cannot find " + chartsPageService.getHomeFxmlPath()
                    )
            );

            Node node = backButton;
            node.getScene().setRoot(root);

        } catch (IOException | NullPointerException exception) {
            exception.printStackTrace();
        }
    }
}