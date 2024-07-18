package com.example.orderapp;
import javafx.application.Application;
import javafx.stage.Stage;
//Classe per gestire il Menù dell'app
public class RestaurantMenuApp extends Application {
    //Metodo start per mostrare il menù
    @Override
    public void start(Stage stage) {
        MenuScreen menuScreen = new MenuScreen(stage);
        menuScreen.show();
    }
    //Metodo main terziario dell'app
    public static void main(String[] args) {
        launch(args);
    }
}