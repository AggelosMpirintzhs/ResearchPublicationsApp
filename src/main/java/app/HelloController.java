package app;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class HelloController {

    private static final String VENUE_FXML_PATH = "/app/venue-view.fxml";
    private static final String YEAR_FXML_PATH = "/app/year-view.fxml";
    private static final String AUTHOR_FXML_PATH = "/app/author-view.fxml";
    private static final String CHARTS_FXML_PATH = "/app/charts-view.fxml";

    // Opens venue page
    @FXML
    private void openVenuePage(ActionEvent event) {
        switchPage(event, VENUE_FXML_PATH);
    }

    // Opens years page
    @FXML
    private void openYearsPage(ActionEvent event) {
        switchPage(event, YEAR_FXML_PATH);
    }

    // Opens author page
    @FXML
    private void openAuthorPage(ActionEvent event) {
        switchPage(event, AUTHOR_FXML_PATH);
    }

    // Opens charts page
    @FXML
    private void openChartsPage(ActionEvent event) {
        switchPage(event, CHARTS_FXML_PATH);
    }

    // Switches current page
    private void switchPage(ActionEvent event, String fxmlPath) {
        try {
            URL fxmlUrl = Objects.requireNonNull(
                    getClass().getResource(fxmlPath),
                    "Cannot find FXML file: " + fxmlPath
            );

            Parent root = FXMLLoader.load(fxmlUrl);

            Node sourceNode = (Node) event.getSource();
            sourceNode.getScene().setRoot(root);

        } catch (IOException exception) {
            System.err.println("FXML loading error for: " + fxmlPath);
            exception.printStackTrace();

        } catch (NullPointerException exception) {
            System.err.println("FXML path not found: " + fxmlPath);
            exception.printStackTrace();

        } catch (RuntimeException exception) {
            System.err.println("Navigation error for: " + fxmlPath);
            exception.printStackTrace();
        }
    }
}