package com.example.orderapp;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class LoginUI extends Application {

    private LoginController loginController;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Login");

        loginController = new LoginController(new DatabaseAuthStrategy());

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);

        // User ID Label and Text Field
        Label userIdLabel = new Label("User ID:");
        GridPane.setConstraints(userIdLabel, 0, 0);
        TextField userIdInput = new TextField();
        GridPane.setConstraints(userIdInput, 1, 0);

        // Password Label and Password Field
        Label passwordLabel = new Label("Password:");
        GridPane.setConstraints(passwordLabel, 0, 1);
        PasswordField passwordInput = new PasswordField();
        GridPane.setConstraints(passwordInput, 1, 1);

        // Login Button
        Button loginButton = new Button("Login");
        GridPane.setConstraints(loginButton, 1, 2);
        loginButton.setOnAction(e -> {
            String userId = userIdInput.getText();
            String password = passwordInput.getText();
            UserSession userSession = loginController.validateLogin(userId, password);
            if (userSession != null) {
                UserSession.getInstance(userId, userSession.isAdmin()); // Set the new session
                loadMainScreen(primaryStage, userSession);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Login Failed");
                alert.setHeaderText(null);
                alert.setContentText("Invalid UserID or Password.");
                alert.showAndWait();
            }
        });

        grid.getChildren().addAll(userIdLabel, userIdInput, passwordLabel, passwordInput, loginButton);

        Scene scene = new Scene(grid, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadMainScreen(Stage primaryStage, UserSession userSession) {
        Main mainApp = new Main(userSession);
        mainApp.start(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
