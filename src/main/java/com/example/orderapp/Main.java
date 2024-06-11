package com.example.orderapp;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    private UserSession userSession;

    // Costruttore per compatibilità con Application.launch
    public Main() {
        // Inizializza con una sessione utente di default (potrebbe essere null)
        this.userSession = UserSession.getInstance();
    }

    // Costruttore per inizializzare con una sessione utente specifica
    public Main(UserSession userSession) {
        this.userSession = userSession;
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Ristorante");

        // Creazione dei pulsanti
        Button btnMenu = new Button("Menù dei piatti");
        Button btnOrdini = new Button("Ordinazioni");
        Button btnLogout = new Button("Logout");
        Button btnModificaMenu = new Button("Modifica Menu");
        Button btnUtenti = new Button("Utenti registrati");
        Button btnSimulazione = new Button("Simulazione");

        // Creazione del layout
        VBox mainButtonsLayout = new VBox(10);
        mainButtonsLayout.getChildren().addAll(btnMenu, btnOrdini);

        // Verifica se l'utente è admin
        if (userSession.isAdmin()) {
            mainButtonsLayout.getChildren().addAll(btnModificaMenu, btnUtenti);
        }

        VBox layout = new VBox(10);
        layout.getChildren().addAll(mainButtonsLayout, btnSimulazione, btnLogout);

        // Logica per il logout
        btnLogout.setOnAction(e -> {
            userSession = UserSession.getInstance(null, false); // Reset della sessione
            primaryStage.close();
            try {
                new LoginUI().start(new Stage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Logica per il pulsante "Menù dei piatti"
        btnMenu.setOnAction(e -> {
            try {
                new RestaurantMenuApp().start(primaryStage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Logica per il pulsante "Ordinazioni"
        btnOrdini.setOnAction(e -> {
            try {
                new OrdersScreen(primaryStage).show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Logica per il pulsante "Modifica Menu"
        btnModificaMenu.setOnAction(e -> {
            try {
                new EditMenuScreen(primaryStage).show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Logica per il pulsante "Utenti registrati"
        btnUtenti.setOnAction(e -> {
            try {
                new UsersScreen(userSession.isAdmin()).show(); // Passa se l'utente è admin
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Logica per il pulsante "Simulazione"
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
