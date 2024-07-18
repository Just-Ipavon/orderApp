package com.example.orderapp;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

//Classe che implementa il form dei piatti - Menu
public class DishForm extends Stage {
    private TextField nameField;
    private TextField descriptionField;
    private TextField priceField;
    private ImageView imageView;
    private File imageFile;
    private final int categoryId;
    private final Integer dishId;
    private DatabaseFacade dbFacade;
    //Costruttore
    public DishForm(int categoryId, Integer dishId) {
        this.categoryId = categoryId;
        this.dishId = dishId;
        this.dbFacade = new DatabaseFacade();
        setTitle(dishId == null ? "Aggiungi Piatto" : "Modifica Piatto");
        //Gestione dell'UI
        GridPane layout = new GridPane();
        layout.setPadding(new Insets(10));
        layout.setHgap(10);
        layout.setVgap(10);
        //Label di ogni parametro di ciascun piatto
        Label nameLabel = new Label("Nome:");
        nameField = new TextField();
        layout.add(nameLabel, 0, 0);
        layout.add(nameField, 1, 0);
        Label descriptionLabel = new Label("Descrizione:");
        descriptionField = new TextField();
        layout.add(descriptionLabel, 0, 1);
        layout.add(descriptionField, 1, 1);
        Label priceLabel = new Label("Prezzo:");
        priceField = new TextField();
        layout.add(priceLabel, 0, 2);
        layout.add(priceField, 1, 2);
        Label imageLabel = new Label("Immagine:");
        imageView = new ImageView();
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        Button chooseImageButton = new Button("Scegli Immagine");
        chooseImageButton.setOnAction(e -> chooseImage());
        layout.add(imageLabel, 0, 3);
        layout.add(imageView, 1, 3);
        layout.add(chooseImageButton, 2, 3);
        //Button per le azioni sulle modifiche fatte
        Button applyButton = new Button("Applica");
        applyButton.setOnAction(e -> applyChanges());
        Button cancelButton = new Button("Annulla");
        cancelButton.setOnAction(e -> close());
        Button deleteButton = new Button("Cancella");
        deleteButton.setOnAction(e -> deleteDish());
        layout.add(applyButton, 0, 4);
        layout.add(cancelButton, 1, 4);
        if (dishId != null) {
            layout.add(deleteButton, 2, 4);
        }

        if (dishId != null) {
            loadDishDetails();
        }

        Scene scene = new Scene(layout, 400, 300);
        setScene(scene);
    }
    //Metodo per selezionare l'immagine da scegliere
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        imageFile = fileChooser.showOpenDialog(this);
        if (imageFile != null) {
            imageView.setImage(new Image(imageFile.toURI().toString()));
        }
    }
    //Metodo per applicare i cambiamenti fatti
    private void applyChanges() {
        String name = nameField.getText();
        String description = descriptionField.getText();
        double price = Double.parseDouble(priceField.getText());
        String imageName = imageFile != null ? imageFile.getName() : null;
        //Gestione eccezioni
        try {
            dbFacade.openConnection();
            Connection conn = dbFacade.getConnection();
            if (dishId == null) {
                String insertQuery = "INSERT INTO menus (menu_name, menu_description, menu_price, menu_image, category_id) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, description);
                    pstmt.setDouble(3, price);
                    pstmt.setString(4, imageName);
                    pstmt.setInt(5, categoryId);
                    pstmt.executeUpdate();
                }
            } else {
                String updateQuery = "UPDATE menus SET menu_name = ?, menu_description = ?, menu_price = ?, menu_image = ? WHERE menu_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, description);
                    pstmt.setDouble(3, price);
                    pstmt.setString(4, imageName);
                    pstmt.setInt(5, dishId);
                    pstmt.executeUpdate();
                }
            }
            if (imageFile != null) {
                File destFile = new File("com/example/progetto/images/" + imageFile.getName());
                imageFile.renameTo(destFile);
            }
            close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbFacade.closeConnection();
        }
    }
    //Metodo per eliminare un piatto dalla categoria
    private void deleteDish() {
        try {
            dbFacade.openConnection();
            Connection conn = dbFacade.getConnection();
            String deleteQuery = "DELETE FROM menus WHERE menu_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteQuery)) {
                pstmt.setInt(1, dishId);
                pstmt.executeUpdate();
            }
            close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbFacade.closeConnection();
        }
    }
    //Metodo per avere i dettagli del piatto (i "parametri")
    private void loadDishDetails() {
        try {
            dbFacade.openConnection();
            Connection conn = dbFacade.getConnection();
            String query = "SELECT * FROM menus WHERE menu_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, dishId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        nameField.setText(rs.getString("menu_name"));
                        descriptionField.setText(rs.getString("menu_description"));
                        priceField.setText(String.valueOf(rs.getDouble("menu_price")));
                        String imageName = rs.getString("menu_image");
                        if (imageName != null) {
                            imageView.setImage(new Image("file:assets/images/" + imageName));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbFacade.closeConnection();
        }
    }
}