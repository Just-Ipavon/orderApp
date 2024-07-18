package com.example.orderapp;
import com.example.orderapp.classes.UserSession;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
//Classe (secondaria) main dell'app
public class Main extends Application {
    private final UserSession userSession;
    //Metodo Main secondario - senza parametri
    public Main() {
        this.userSession = UserSession.getInstance();
    }
    //Metodo Main secondario - con parametro UserSession
    public Main(UserSession userSession) {
        this.userSession = userSession;
    }
    //Metodo Start del Main - Costruisce l'App dopo il Login
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Ristorante");
        //Crea lo Stage iniziale dell'app con tutte le possibili azioni
        Button btnMenu = new Button("Menù dei piatti");
        Button btnOrdini = new Button("Ordinazioni");
        Button btnLogout = new Button("Logout");
        Button btnModificaMenu = new Button("Modifica Menu");
        Button btnUtenti = new Button("Utenti registrati");
        Button btnSimulazione = new Button("Simulazione");
        VBox mainButtonsLayout = new VBox(10);
        mainButtonsLayout.getChildren().addAll(btnMenu, btnOrdini);
        //Controllo sulla session
        if (userSession.isAdmin()) {
            //Aggiungo i button corrispondenti all'admin
            mainButtonsLayout.getChildren().addAll(btnModificaMenu, btnUtenti);
        }
        VBox layout = new VBox(10);
        layout.getChildren().addAll(mainButtonsLayout, btnSimulazione, btnLogout);
        //Logica dei button
        btnLogout.setOnAction(e -> {
            userSession.cleanUserSession(); // Reset the session
            primaryStage.close();
            try {
                new LoginUI().start(new Stage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        btnMenu.setOnAction(e -> {
            try {
                new RestaurantMenuApp().start(primaryStage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        btnOrdini.setOnAction(e -> {
            try {
                new OrdersScreen().show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        btnModificaMenu.setOnAction(e -> {
            try {
                new EditMenuScreen(primaryStage).show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        btnUtenti.setOnAction(e -> {
            try {
                new UsersScreen(userSession.isAdmin()).show(); // se l'user è admin
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        // pulsante "Simulazione" logica
        btnSimulazione.setOnAction(e -> {
            try {
                new SimulationScreen().show();
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
