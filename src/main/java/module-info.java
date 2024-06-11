module com.example.orderapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires java.sql;
    requires java.desktop;

    opens com.example.orderapp to javafx.fxml;
    exports com.example.orderapp;
    exports com.example.orderapp.classes;
    opens com.example.orderapp.classes to javafx.fxml;
}