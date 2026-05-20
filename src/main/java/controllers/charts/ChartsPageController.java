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

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ChartsPageController {

    private static final String HOME_FXML_PATH = "/app/hello-view.fxml";

    private static final String VENUE_ANALYSIS_FXML = "/app/charts/venue-analysis-tab.fxml";
    private static final String VENUE_CATEGORIES_FXML = "/app/charts/venue-categories-tab.fxml";
    private static final String PUBLISHER_ANALYSIS_FXML = "/app/charts/publisher-analysis-tab.fxml";
    private static final String SCATTER_PLOTS_FXML = "/app/charts/scatter-plots-tab.fxml";

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

    private final Set<Tab> loadedTabs = new HashSet<>();

    private boolean venueCategoriesOptionsStarted = false;

    @FXML
    public void initialize() {
        if (chartsTabPane == null) {
            return;
        }

        chartsTabPane.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldTab, newTab) -> loadSelectedTab(newTab));

        /*
         * Σημαντικό:
         * Το Platform.runLater κάνει το αρχικό tab να φορτώσει αφού πρώτα εμφανιστεί η σελίδα.
         * Έτσι το πάτημα από την αρχική στο Charts δεν μπλοκάρει από τα tab FXML.
         */
        Platform.runLater(() -> loadSelectedTab(chartsTabPane.getSelectionModel().getSelectedItem()));
    }

    private void loadSelectedTab(Tab selectedTab) {
        if (selectedTab == null) {
            return;
        }

        if (selectedTab == venueAnalysisTab) {
            loadTabContent(
                    venueAnalysisTab,
                    venueAnalysisContainer,
                    VENUE_ANALYSIS_FXML
            );
            return;
        }

        if (selectedTab == venueCategoriesTab) {
            loadTabContent(
                    venueCategoriesTab,
                    venueCategoriesContainer,
                    VENUE_CATEGORIES_FXML
            );
            return;
        }

        if (selectedTab == publisherAnalysisTab) {
            loadTabContent(
                    publisherAnalysisTab,
                    publisherAnalysisContainer,
                    PUBLISHER_ANALYSIS_FXML
            );
            return;
        }

        if (selectedTab == scatterPlotsTab) {
            loadTabContent(
                    scatterPlotsTab,
                    scatterPlotsContainer,
                    SCATTER_PLOTS_FXML
            );
        }
    }

    private void loadTabContent(Tab tab, StackPane container, String fxmlPath) {
        if (tab == null || container == null || fxmlPath == null || fxmlPath.isBlank()) {
            return;
        }

        if (loadedTabs.contains(tab)) {
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
            loadedTabs.add(tab);

            if (tab == venueCategoriesTab) {
                Object controller = loader.getController();

                if (controller instanceof VenueCategoriesController venueCategoriesController
                        && !venueCategoriesOptionsStarted) {

                    venueCategoriesOptionsStarted = true;
                    venueCategoriesController.loadCategoryOptionsInBackground();
                }
            }

        } catch (IOException | NullPointerException exception) {
            exception.printStackTrace();

            Label errorLabel = new Label("Could not load this tab.");
            errorLabel.setStyle("-fx-text-fill: #b00020; -fx-font-weight: 800; -fx-padding: 28;");

            container.getChildren().setAll(errorLabel);
        }
    }

    @FXML
    private void backToHome() {
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(
                            getClass().getResource(HOME_FXML_PATH),
                            "Cannot find " + HOME_FXML_PATH
                    )
            );

            Node node = backButton;
            node.getScene().setRoot(root);

        } catch (IOException | NullPointerException exception) {
            exception.printStackTrace();
        }
    }
}