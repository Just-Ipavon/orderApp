package com.example.orderapp;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MenuScreen extends Stage {
    private final List<Order> orders = new ArrayList<>();
    private final Stage mainStage;
    private final DatabaseFacade databaseFacade = new DatabaseFacade();

    public MenuScreen(Stage mainStage) {
        this.mainStage = mainStage;
        this.setTitle("Menu del Ristorante");
        VBox mainLayout = new VBox();
        Parent root = this.createMenuUIFromDatabase();
        mainLayout.getChildren().add(root);

        Button viewCartButton = new Button("Visualizza carrello");
        viewCartButton.setOnAction(e -> this.viewCart());
        mainLayout.getChildren().add(viewCartButton);

        Button backButton = new Button("Torna al menù principale");
        backButton.setOnAction(e -> {
            this.close();
            new Main(UserSession.getInstance(null, null)).start(mainStage);
        });
        mainLayout.getChildren().add(backButton);

        Scene scene = new Scene(mainLayout, 1600, 900);
        this.setScene(scene);
    }

    private Parent createMenuUIFromDatabase() {
        Accordion menuLayout = new Accordion();
        VBox mainLayout = new VBox(menuLayout);
        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true);

        try {
            databaseFacade.openConnection();
            Connection conn = databaseFacade.getConnection();

            String categoryQuery = "SELECT * FROM menu_categories";
            try (PreparedStatement pstmt = conn.prepareStatement(categoryQuery);
                 ResultSet categoryRS = pstmt.executeQuery()) {

                while (categoryRS.next()) {
                    String categoryName = categoryRS.getString("category_name");
                    String categoryImagePathName = categoryRS.getString("category_image");
                    Image categoryImage = loadImage(categoryImagePathName);
                    ImageView categoryImageView = new ImageView(categoryImage);
                    categoryImageView.setFitWidth(100);
                    categoryImageView.setFitHeight(100);

                    VBox dishBox = new VBox();
                    dishBox.setPadding(new Insets(10));
                    dishBox.setSpacing(10);
                    TitledPane categoryPane = new TitledPane(categoryName, dishBox);
                    categoryPane.setGraphic(categoryImageView);
                    menuLayout.getPanes().add(categoryPane);

                    int categoryId = categoryRS.getInt("category_id");
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
                                Image image = loadImage(imagePathName);
                                ImageView imageView = new ImageView(image);
                                imageView.setFitWidth(100);
                                imageView.setFitHeight(100);

                                Label dishLabel = new Label(dishName + " - $" + dishPrice);
                                Label dishDescriptionLabel = new Label(dishDescription);

                                TextField notesField = new TextField();
                                notesField.setPromptText("Aggiungi note");

                                Button orderButton = new Button("Aggiungi all ordine");
                                orderButton.setOnAction(e -> addToOrder(menuId, dishName, dishPrice, notesField.getText()));

                                VBox dishDetailsBox = new VBox(dishLabel, imageView, dishDescriptionLabel, notesField, orderButton);
                                dishBox.getChildren().add(dishDetailsBox);
                            }
                        }
                    }
                }

                Button submitOrderButton = new Button("Avvia Ordinazione");
                submitOrderButton.setOnAction(e -> selectTable());
                mainLayout.getChildren().add(submitOrderButton);
            }
        } catch (Exception e) {
            System.err.println("Si è verificato un errore: " + e.getMessage());
            e.printStackTrace();
        } finally {
            databaseFacade.closeConnection();
        }

        return scrollPane;
    }

    private Image loadImage(String imagePathName) {
        String imagePath = "/assets/" + imagePathName;
        try {
            return new Image(this.getClass().getResourceAsStream(imagePath));
        } catch (Exception e) {
            return new Image(this.getClass().getResourceAsStream("/assets/error.png"));
        }
    }

    private void addToOrder(int menuId, String dishName, double dishPrice, String notes) {
        for (Order order : orders) {
            if (order.getMenuId() == menuId) {
                order.incrementQuantity();
                order.setNotes(notes);
                return;
            }
        }
        Order order = new Order(menuId, dishName, dishPrice);
        order.setNotes(notes);
        orders.add(order);
    }

    private void submitOrder(int tableId) {
        try {
            databaseFacade.openConnection();
            Connection conn = databaseFacade.getConnection();
            conn.setAutoCommit(false);

            String insertPlacedOrderQuery = "INSERT INTO orders (order_id, order_time, delivered, table_id) VALUES (?, ?, ?, ?)";
            String insertOrderQuery = "INSERT INTO dishes_order (order_id, menu_id, quantity, note) VALUES (?, ?, ?, ?)";

            try (PreparedStatement placedOrderStmt = conn.prepareStatement(insertPlacedOrderQuery);
                 PreparedStatement orderStmt = conn.prepareStatement(insertOrderQuery)) {

                int orderId = generateOrderId();
                Timestamp orderTime = new Timestamp(System.currentTimeMillis());
                boolean delivered = false;

                placedOrderStmt.setInt(1, orderId);
                placedOrderStmt.setTimestamp(2, orderTime);
                placedOrderStmt.setBoolean(3, delivered);
                placedOrderStmt.setInt(4, tableId);
                placedOrderStmt.executeUpdate();

                for (Order order : orders) {
                    orderStmt.setInt(1, orderId);
                    orderStmt.setInt(2, order.getMenuId());
                    orderStmt.setInt(3, order.getQuantity());
                    orderStmt.setString(4, order.getNotes());
                    orderStmt.executeUpdate();
                }

                conn.commit();
                orders.clear();
                System.out.println("Ordine inserito con successo.");
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Errore durante l'inserimento dell'ordine: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Errore nella connessione al database: " + e.getMessage());
            e.printStackTrace();
        } finally {
            databaseFacade.closeConnection();
        }
    }

    private void selectTable() {
        Stage tableStage = new Stage();
        tableStage.setTitle("Seleziona Tavolo");

        VBox layout = new VBox();
        Label instructions = new Label("Seleziona un tavolo:");

        ComboBox<Integer> tableComboBox = new ComboBox<>();
        try {
            databaseFacade.openConnection();
            Connection conn = databaseFacade.getConnection();

            String query = "SELECT table_id FROM tables";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tableComboBox.getItems().add(rs.getInt("table_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            databaseFacade.closeConnection();
        }

        Button confirmButton = new Button("Conferma");
        confirmButton.setOnAction(e -> {
            Integer selectedTable = tableComboBox.getValue();
            if (selectedTable != null) {
                submitOrder(selectedTable);
                tableStage.close();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Attenzione");
                alert.setHeaderText(null);
                alert.setContentText("Per favore, seleziona un tavolo.");
                alert.showAndWait();
            }
        });

        layout.getChildren().addAll(instructions, tableComboBox, confirmButton);
        Scene scene = new Scene(layout, 300, 200);
        tableStage.setScene(scene);
        tableStage.show();
    }

    private int generateOrderId() {
        long timestamp = System.currentTimeMillis();
        int orderId = Math.abs((int) timestamp);
        System.out.println("Generated Order ID: " + orderId);
        return orderId;
    }

    private void viewCart() {
        Stage cartStage = new Stage();
        cartStage.setTitle("Carrello");
        VBox layout = new VBox();
        double total = 0.0;

        for (Order order : orders) {
            String text = "Piatto: " + order.getDishName() + ", Quantità: " + order.getQuantity() + ", Prezzo: $" + order.getDishPrice() + ", Note: " + order.getNotes();
            Label label = new Label(text);
            layout.getChildren().add(label);
            total += order.getQuantity() * order.getDishPrice();
        }

        Label totalLabel = new Label("Totale: $" + total);
        layout.getChildren().add(totalLabel);
        Scene scene = new Scene(layout);
        cartStage.setScene(scene);
        cartStage.show();
    }
}
