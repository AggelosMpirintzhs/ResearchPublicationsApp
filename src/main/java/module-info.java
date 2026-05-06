module com.example.project_pvasil {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires com.zaxxer.hikari;
    requires java.sql;

    opens com.example.project_pvasil to javafx.fxml;
    exports com.example.project_pvasil;
}