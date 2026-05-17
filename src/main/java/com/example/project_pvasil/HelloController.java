package com.example.project_pvasil;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class HelloController {

    @FXML
    private void openVenuePage(ActionEvent event) {
        switchPage(event, "/com/example/project_pvasil/venue-view.fxml");
    }

    @FXML
    private void openYearsPage(ActionEvent event) {
        switchPage(event, "/com/example/project_pvasil/year-view.fxml");
    }

    @FXML
    private void openAuthorPage(ActionEvent event) {
        switchPage(event, "/com/example/project_pvasil/author-view.fxml");
    }

    @FXML
    private void openChartsPage(ActionEvent event) {
        switchPage(event, "/com/example/project_pvasil/charts-view.fxml");
    }

    private void switchPage(ActionEvent event, String fxmlPath) {
        try {
            URL fxmlUrl = Objects.requireNonNull(
                    getClass().getResource(fxmlPath),
                    "Cannot find FXML file: " + fxmlPath
            );

            Parent root = FXMLLoader.load(fxmlUrl);

            ((Node) event.getSource()).getScene().setRoot(root);

        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (RuntimeException exception) {
            exception.printStackTrace();
        }
    }
}