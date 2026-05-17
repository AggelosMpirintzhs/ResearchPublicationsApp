package com.example.project_pvasil;

import db.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        DatabaseManager.initialize();

        URL fxmlUrl = Objects.requireNonNull(
                HelloApplication.class.getResource("hello-view.fxml"),
                "Cannot find hello-view.fxml"
        );

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Scene scene = new Scene(loader.load(), 1200, 800);

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        HelloApplication.class.getResource("app.css"),
                        "Cannot find app.css"
                ).toExternalForm()
        );

        stage.setTitle("Research Publications App");
        stage.setMinWidth(1050);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        DatabaseManager.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}