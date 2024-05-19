package com.example.orderapp;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EditMenuScreen extends Stage {
    private final Stage mainStage;
    private final DatabaseFacade databaseFacade = new DatabaseFacade();

    public EditMenuScreen(Stage mainStage) {
        this.mainStage = mainStage;
        setTitle("Modifica Menu");
        VBox mainLayout = new VBox();
        mainLayout.getChildren().add(createMenuUIFromDatabase());

        Button addDishButton = new Button("Aggiungi Piatto");
        addDishButton.setOnAction(e -> {
            try {
                showDishForm(null);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
        mainLayout.getChildren().add(addDishButton);

        Button backButton = new Button("Torna al menù principale");
        backButton.setOnAction(e -> {
            this.close();
            new Main(UserSession.getInstance(null, null)).start(mainStage);
        });
        mainLayout.getChildren().add(backButton);

        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true);
        Scene scene = new Scene(scrollPane, 1600, 900);
        setScene(scene);
    }

    private Parent createMenuUIFromDatabase() {
        Accordion menuLayout = new Accordion();

        try (Connection conn = databaseFacade.openConnection()) {
            String categoryQuery = "SELECT * FROM menu_categories";
            try (PreparedStatement pstmt = conn.prepareStatement(categoryQuery);
                 ResultSet categoryRS = pstmt.executeQuery()) {

                while (categoryRS.next()) {
                    String categoryName = categoryRS.getString("category_name");
                    String categoryImagePathName = categoryRS.getString("category_image");
                    String categoryImagePath = "assets/" + categoryImagePathName;

                    Image categoryImage = loadImage(categoryImagePath);
                    ImageView categoryImageView = new ImageView(categoryImage);
                    categoryImageView.setFitWidth(100);
                    categoryImageView.setFitHeight(100);

                    VBox dishBox = new VBox();
                    dishBox.setPadding(new Insets(10));
                    dishBox.setSpacing(10);
                    TitledPane categoryPane = new TitledPane(categoryName, dishBox);
                    categoryPane.setGraphic(categoryImageView);
                    menuLayout.getPanes().add(categoryPane);

                    loadDishes(conn, categoryRS.getInt("category_id"), dishBox);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return menuLayout;
    }

    private void loadDishes(Connection conn, int categoryId, VBox dishBox) {
        String dishQuery = "SELECT * FROM menus WHERE category_id = ?";
        try (PreparedStatement dishStmt = conn.prepareStatement(dishQuery)) {
            dishStmt.setInt(1, categoryId);
            try (ResultSet dishRS = dishStmt.executeQuery()) {
                while (dishRS.next()) {
                    String dishName = dishRS.getString("menu_name");
                    double dishPrice = dishRS.getDouble("menu_price");
                    String dishDescription = dishRS.getString("menu_description");
                    int menuId = dishRS.getInt("menu_id");

                    String imagePathName = dishRS.getString("menu_image");
                    String imagePath = "assets/" + imagePathName;

                    Image image = loadImage(imagePath);
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(100);
                    imageView.setFitHeight(100);

                    Label dishLabel = new Label(dishName + " - $" + dishPrice);
                    Label dishDescriptionLabel = new Label(dishDescription);

                    HBox dishButtons = new HBox();
                    dishButtons.setSpacing(10);

                    Button modifyButton = new Button("Modifica");
                    modifyButton.setOnAction(e -> {
                        try {
                            showDishForm(menuId);
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }
                    });

                    Button deleteButton = new Button("Elimina");
                    deleteButton.setOnAction(e -> {
                        try {
                            deleteDish(menuId);
                            refreshScreen();
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }
                    });

                    dishButtons.getChildren().addAll(modifyButton, deleteButton);

                    VBox dishDetails = new VBox();
                    dishDetails.getChildren().addAll(dishLabel, dishDescriptionLabel, imageView, dishButtons);
                    dishBox.getChildren().add(dishDetails);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Image loadImage(String imagePath) {
        try {
            URL imageURL = getClass().getResource("/" + imagePath);
            if (imageURL == null) {
                System.err.println("Immagine non trovata: " + imagePath);
                return new Image(getClass().getResource("/assets/error.png").toExternalForm());
            } else {
                return new Image(imageURL.toExternalForm());
            }
        } catch (IllegalArgumentException e) {
            return new Image(getClass().getResource("/assets/error.png").toExternalForm());
        }
    }

    private void showDishForm(Integer menuId) throws SQLException {
        Stage dishFormStage = new Stage();
        dishFormStage.setTitle(menuId == null ? "Aggiungi Piatto" : "Modifica Piatto");

        VBox layout = new VBox();
        layout.setPadding(new Insets(10));
        layout.setSpacing(10);

        TextField dishNameField = new TextField();
        dishNameField.setPromptText("Nome Piatto");

        TextField dishPriceField = new TextField();
        dishPriceField.setPromptText("Prezzo");

        TextArea dishDescriptionArea = new TextArea();
        dishDescriptionArea.setPromptText("Descrizione");

        FileChooser fileChooser = new FileChooser();
        Button chooseImageButton = new Button("Scegli Immagine");
        Label imagePathLabel = new Label();

        ComboBox<String> categoryComboBox = new ComboBox<>();

        try (Connection conn = databaseFacade.openConnection()) {
            String categoryQuery = "SELECT category_name FROM menu_categories";
            try (PreparedStatement pstmt = conn.prepareStatement(categoryQuery);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    categoryComboBox.getItems().add(rs.getString("category_name"));
                }
            }

            if (menuId != null) {
                String query = "SELECT * FROM menus WHERE menu_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setInt(1, menuId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            dishNameField.setText(rs.getString("menu_name"));
                            dishPriceField.setText(Double.toString(rs.getDouble("menu_price")));
                            dishDescriptionArea.setText(rs.getString("menu_description"));
                            imagePathLabel.setText("assets/" + rs.getString("menu_image"));

                            String categoryNameQuery = "SELECT category_name FROM menu_categories WHERE category_id = ?";
                            try (PreparedStatement categoryNameStmt = conn.prepareStatement(categoryNameQuery)) {
                                categoryNameStmt.setInt(1, rs.getInt("category_id"));
                                try (ResultSet categoryNameRS = categoryNameStmt.executeQuery()) {
                                    if (categoryNameRS.next()) {
                                        categoryComboBox.setValue(categoryNameRS.getString("category_name"));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        chooseImageButton.setOnAction(e -> {
            File selectedFile = fileChooser.showOpenDialog(dishFormStage);
            if (selectedFile != null) {
                imagePathLabel.setText(selectedFile.getAbsolutePath());
            }
        });

        Button applyButton = new Button("Applica");
        applyButton.setOnAction(e -> {
            String dishName = dishNameField.getText();
            double dishPrice = Double.parseDouble(dishPriceField.getText());
            String dishDescription = dishDescriptionArea.getText();
            String imagePath = imagePathLabel.getText();
            String categoryName = categoryComboBox.getValue();

            try (Connection conn = databaseFacade.openConnection()) {
                String categoryIdQuery = "SELECT category_id FROM menu_categories WHERE category_name = ?";
                int categoryId;
                try (PreparedStatement pstmt = conn.prepareStatement(categoryIdQuery)) {
                    pstmt.setString(1, categoryName);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            categoryId = rs.getInt("category_id");
                        } else {
                            throw new RuntimeException("Categoria non trovata");
                        }
                    }
                }

                if (menuId == null) {
                    addDish(conn, dishName, dishPrice, dishDescription, imagePath, categoryId);
                } else {
                    updateDish(conn, menuId, dishName, dishPrice, dishDescription, imagePath, categoryId);
                }
                dishFormStage.close();
                refreshScreen();
            } catch (SQLException | IOException ex) {
                ex.printStackTrace();
            }
        });

        layout.getChildren().addAll(dishNameField, dishPriceField, dishDescriptionArea, categoryComboBox, chooseImageButton, imagePathLabel, applyButton);

        Scene scene = new Scene(layout, 300, 400);
        dishFormStage.setScene(scene);
        dishFormStage.show();
    }

    private void addDish(Connection conn, String name, double price, String description, String imagePath, int categoryId) throws SQLException, IOException {
        String insertQuery = "INSERT INTO menus (menu_name, menu_price, menu_description, menu_image, category_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            pstmt.setString(1, name);
            pstmt.setDouble(2, price);
            pstmt.setString(3, description);

            String imageFileName = new File(imagePath).getName();
            String newImagePath = "assets/" + imageFileName;
            Files.copy(new File(imagePath).toPath(), new File(newImagePath).toPath(), StandardCopyOption.REPLACE_EXISTING);
            pstmt.setString(4, imageFileName);

            pstmt.setInt(5, categoryId);
            pstmt.executeUpdate();
        }
    }

    private void updateDish(Connection conn, int menuId, String name, double price, String description, String imagePath, int categoryId) throws SQLException, IOException {
        String updateQuery = "UPDATE menus SET menu_name = ?, menu_price = ?, menu_description = ?, menu_image = ?, category_id = ? WHERE menu_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
            pstmt.setString(1, name);
            pstmt.setDouble(2, price);
            pstmt.setString(3, description);

            if (imagePath != null && !imagePath.isEmpty()) {
                String imageFileName = new File(imagePath).getName();
                String newImagePath = "assets/" + imageFileName;
                try {
                    Files.copy(new File(imagePath).toPath(), new File(newImagePath).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    pstmt.setString(4, imageFileName);
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Errore nel caricamento dell'immagine. Verrà utilizzata un'immagine di errore.");
                    alert.showAndWait();
                    pstmt.setString(4, "error.png");
                }
            }

            pstmt.setInt(5, categoryId);
            pstmt.setInt(6, menuId);
            pstmt.executeUpdate();
        }
    }

    private void deleteDish(int menuId) throws SQLException {
        String deleteQuery = "DELETE FROM menus WHERE menu_id = ?";
        try (Connection conn = databaseFacade.openConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteQuery)) {
            pstmt.setInt(1, menuId);
            pstmt.executeUpdate();
        }
    }

    private void refreshScreen() {
        ScrollPane scrollPane = (ScrollPane) getScene().getRoot();
        VBox mainLayout = (VBox) scrollPane.getContent();
        mainLayout.getChildren().clear();
        mainLayout.getChildren().add(createMenuUIFromDatabase());

        Button addDishButton = new Button("Aggiungi Piatto");
        addDishButton.setOnAction(e -> {
            try {
                showDishForm(null);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
        mainLayout.getChildren().add(addDishButton);

        Button backButton = new Button("Torna al menù principale");
        backButton.setOnAction(e -> {
            this.close();
            new Main(UserSession.getInstance(null, null)).start(mainStage);
        });
        mainLayout.getChildren().add(backButton);
    }
}
