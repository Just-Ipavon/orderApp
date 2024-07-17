package com.example.orderapp;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

//Classe per gestire il menu, mediante Categorie
public class CategoryForm extends Stage {
    private TextField categoryNameField;
    private ImageView categoryImageView;
    private File selectedImageFile;
    private DatabaseFacade dbFacade; //Pattern Facade per la connesstione al DB
    //Costruttore
    public CategoryForm() {
        setTitle("Aggiungi/Modifica Categoria");
        dbFacade = new DatabaseFacade();  // Inizializza la connessione al database
        BorderPane mainLayout = new BorderPane();
        VBox formLayout = new VBox(10);
        formLayout.setPadding(new Insets(10));
        //Gestione dei campi del form
        categoryNameField = new TextField();
        categoryNameField.setPromptText("Nome Categoria");
        //Sequenza di passi da compiere (immagine,nome,...)
        Button chooseImageButton = new Button("Scegli Immagine");
        chooseImageButton.setOnAction(e -> chooseImage());
        categoryImageView = new ImageView();
        categoryImageView.setFitWidth(150);
        categoryImageView.setFitHeight(150);
        Button applyButton = new Button("Applica");
        applyButton.setOnAction(e -> applyChanges());
        Button cancelButton = new Button("Annulla");
        cancelButton.setOnAction(e -> close());
        HBox buttonsLayout = new HBox(10);
        buttonsLayout.getChildren().addAll(applyButton, cancelButton);
        formLayout.getChildren().addAll(categoryNameField, chooseImageButton, categoryImageView, buttonsLayout);
        mainLayout.setCenter(formLayout);
        //Gestione della Scena effettiva da mostrare nell'UI
        Scene scene = new Scene(mainLayout, 400, 300);
        setScene(scene);
    }
    //Scelta dell'immagine da mostrare
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
        selectedImageFile = fileChooser.showOpenDialog(this);
        if (selectedImageFile != null) {
            Image image = new Image(selectedImageFile.toURI().toString());
            categoryImageView.setImage(image);
        }
    }
    //Function che applica i cambiamenti al form delle categorie
    private void applyChanges() {
        String categoryName = categoryNameField.getText();
        if (!categoryName.isEmpty() && selectedImageFile != null) {
            try {
                dbFacade.openConnection();  // Apri la connessione
                Connection conn = dbFacade.getConnection();
                //Query al DB
                String insertQuery = "INSERT INTO menu_categories (category_name, category_image) VALUES (?, ?)";
                //Gestione errori
                try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                    pstmt.setString(1, categoryName);
                    pstmt.setString(2, selectedImageFile.getName()); // Assume il nome del file come nome dell'immagine nel database
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                dbFacade.closeConnection();  // Chiudi la connessione
            }
            close();
        } else {
            // Avviso se i campi non sono stati compilati
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attenzione");
            alert.setHeaderText(null);
            alert.setContentText("Inserisci il nome della categoria e seleziona un'immagine.");
            alert.showAndWait();
        }
    }
}