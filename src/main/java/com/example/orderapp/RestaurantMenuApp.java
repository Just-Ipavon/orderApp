package com.example.orderapp;

import javafx.application.Application;
import javafx.stage.Stage;

public class RestaurantMenuApp extends Application {

    @Override
    public void start(Stage stage) {
        MenuScreen menuScreen = new MenuScreen(stage);
        menuScreen.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
