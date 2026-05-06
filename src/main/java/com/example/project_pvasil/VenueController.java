package com.example.project_pvasil;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Objects;

public class VenueController {

    @FXML
    private void backToHome(ActionEvent event) {
        switchPage(event, "hello-view.fxml");
    }

    private void switchPage(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(
                            getClass().getResource(fxmlFile),
                            "Cannot find " + fxmlFile
                    )
            );

            ((Node) event.getSource()).getScene().setRoot(root);

        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}