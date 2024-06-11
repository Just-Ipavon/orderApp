package com.example.orderapp;

import com.example.orderapp.classes.UserSession;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    private UserSession userSession;

    public Main() {
        this.userSession = UserSession.getInstance();
    }

    public Main(UserSession userSession) {
        this.userSession = userSession;
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Ristorante");

        // Create buttons
        Button btnMenu = new Button("Menù dei piatti");
        Button btnOrdini = new Button("Ordinazioni");
        Button btnLogout = new Button("Logout");
        Button btnModificaMenu = new Button("Modifica Menu");
        Button btnUtenti = new Button("Utenti registrati");
        Button btnSimulazione = new Button("Simulazione");

        // Create layout
        VBox mainButtonsLayout = new VBox(10);
        mainButtonsLayout.getChildren().addAll(btnMenu, btnOrdini);

        // Check if user is admin
        if (userSession.isAdmin()) {
            mainButtonsLayout.getChildren().addAll(btnModificaMenu, btnUtenti);
        }

        VBox layout = new VBox(10);
        layout.getChildren().addAll(mainButtonsLayout, btnSimulazione, btnLogout);

        // Logout logic
        btnLogout.setOnAction(e -> {
            userSession.cleanUserSession(); // Reset the session
            primaryStage.close();
            try {
                new LoginUI().start(new Stage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // "Menù dei piatti" button logic
        btnMenu.setOnAction(e -> {
            try {
                new RestaurantMenuApp().start(primaryStage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // "Ordinazioni" button logic
        btnOrdini.setOnAction(e -> {
            try {
                new OrdersScreen(primaryStage).show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // "Modifica Menu" button logic
        btnModificaMenu.setOnAction(e -> {
            try {
                new EditMenuScreen(primaryStage).show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // "Utenti registrati" button logic
        btnUtenti.setOnAction(e -> {
            try {
                new UsersScreen(userSession.isAdmin()).show(); // Pass if the user is admin
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // "Simulazione" button logic
        btnSimulazione.setOnAction(e -> {
            try {
                new SimulationScreen(primaryStage).show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Scene scene = new Scene(layout, 300, 250);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
