package com.example.orderapp;

import com.example.orderapp.classes.CompleteOrder;
import com.example.orderapp.classes.Order;
import com.example.orderapp.classes.OrderObserver;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class OrdersScreen extends Stage implements OrderObserver {

    private VBox mainLayout;
    private final CompleteOrderDAO orderDAO;
    private final DatabaseFacade databaseFacade = new DatabaseFacade();

    public OrdersScreen(Stage primaryStage) {
        this.orderDAO = new CompleteOrderDAO();
        setTitle("Ordini");

        mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true);

        loadOrders();

        // Creating the button to view paid orders
        Button paidOrdersButton = new Button("Ordini Pagati");
        paidOrdersButton.setOnAction(e -> showPaidOrdersScreen());

        // Align the button to the bottom right
        HBox buttonContainer = new HBox(paidOrdersButton);
        buttonContainer.setAlignment(Pos.BOTTOM_RIGHT);
        buttonContainer.setPadding(new Insets(10));

        VBox mainContainer = new VBox(scrollPane, buttonContainer);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Scene scene = new Scene(mainContainer, 800, 600);
        setScene(scene);
    }

    private void loadOrders() {
        try {
            databaseFacade.openConnection();
            List<CompleteOrder> orders = orderDAO.getAllNonCompletedOrders();
            mainLayout.getChildren().clear();

            for (CompleteOrder completeOrder : orders) {
                VBox orderBox = new VBox(10);
                orderBox.setPadding(new Insets(10));
                orderBox.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-padding: 10;");

                Label orderIdLabel = new Label("ID Ordine: " + completeOrder.getOrderId());
                Label tableIdLabel = new Label("ID Tavolo: " + completeOrder.getTableId());
                CheckBox deliveredCheckBox = new CheckBox("Consegnato");
                deliveredCheckBox.setSelected(completeOrder.isDelivered());
                deliveredCheckBox.setOnAction(e -> updateOrderStatus(completeOrder.getOrderId(), deliveredCheckBox.isSelected()));

                HBox headerBox = new HBox(10);
                headerBox.getChildren().addAll(orderIdLabel, tableIdLabel, deliveredCheckBox);
                orderBox.getChildren().add(headerBox);

                for (Order order : completeOrder.getDishes()) {
                    HBox dishBox = new HBox(10);
                    Label dishLabel = new Label("Piatto: " + order.getDishName() + " - Quantità: " + order.getQuantity() + " - Prezzo: $" + order.getDishPrice());
                    Button deleteButton = new Button("Elimina");
                    deleteButton.setOnAction(e -> deleteDish(completeOrder.getOrderId(), order.getMenuId()));
                    dishBox.getChildren().addAll(dishLabel, deleteButton);
                    orderBox.getChildren().add(dishBox);
                }

                Button deleteOrderButton = new Button("Elimina Ordine");
                deleteOrderButton.setOnAction(e -> deleteOrder(completeOrder.getOrderId()));

                Button payButton = new Button("Paga");
                payButton.setOnAction(e -> showPaymentDialog(completeOrder.getOrderId(), completeOrder.getTotalPrice(), completeOrder.getTableId()));

                HBox buttonBox = new HBox(10);
                buttonBox.getChildren().addAll(deleteOrderButton, payButton);
                orderBox.getChildren().add(buttonBox);

                mainLayout.getChildren().add(orderBox);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorAlert("Errore durante il caricamento degli ordini", "Si è verificato un errore durante il caricamento degli ordini. Per favore, riprova.");
        } finally {
            databaseFacade.closeConnection();
        }
    }

    private void updateOrderStatus(int orderId, boolean isDelivered) {
        orderDAO.updateOrderStatus(orderId, isDelivered);
        onOrderStatusChanged(orderId, isDelivered);
    }

    private void deleteOrder(int orderId) {
        orderDAO.deleteOrder(orderId);
        onOrderDeleted(orderId);
    }

    private void deleteDish(int orderId, int menuId) {
        orderDAO.deleteDish(orderId, menuId);
        onDishDeleted(orderId, menuId);
    }

    private void showPaymentDialog(int orderId, double totalPrice, int tableId) {
        Stage dialog = new Stage();
        dialog.setTitle("Pagamento");

        VBox dialogVBox = new VBox(10);
        dialogVBox.setPadding(new Insets(10));

        Label totalLabel = new Label("Totale: $" + totalPrice);
        TextField amountReceivedField = new TextField();
        amountReceivedField.setPromptText("Importo ricevuto");

        ComboBox<String> paymentMethodBox = new ComboBox<>();
        paymentMethodBox.getItems().addAll("Contanti", "Carta di Credito", "Bancomat");
        paymentMethodBox.setValue("Contanti");

        Button processPaymentButton = new Button("Processa Pagamento");
        processPaymentButton.setOnAction(e -> {
            String paymentMethod = paymentMethodBox.getValue();
            if ("Contanti".equals(paymentMethod)) {
                String amountReceivedStr = amountReceivedField.getText();
                if (!isNumeric(amountReceivedStr)) {
                    showErrorAlert("Errore di input", "L'importo ricevuto deve essere un numero.");
                    return;
                }
                double amountReceived = Double.parseDouble(amountReceivedStr);
                if (amountReceived < totalPrice) {
                    showErrorAlert("Errore di pagamento", "L'importo ricevuto è inferiore al totale.");
                    return;
                }
                processPayment(orderId, paymentMethod, amountReceived, totalPrice, tableId);
            } else {
                processPayment(orderId, paymentMethod, totalPrice, totalPrice, tableId);
            }
            dialog.close();
        });

        dialogVBox.getChildren().addAll(totalLabel, amountReceivedField, paymentMethodBox, processPaymentButton);

        Scene dialogScene = new Scene(dialogVBox, 300, 200);
        dialog.setScene(dialogScene);
        dialog.show();
    }

    private void processPayment(int orderId, String paymentMethod, double amountReceived, double totalPrice, int tableId) {
        int transactionId = orderDAO.processPaymentTransaction(orderId, paymentMethod);
        if (transactionId != -1) {
            generateReceipt(orderId, paymentMethod, amountReceived, totalPrice);
            onPaymentProcessed(orderId, transactionId, paymentMethod, amountReceived);
        } else {
            showErrorAlert("Errore di pagamento", "Si è verificato un errore durante l'elaborazione del pagamento. Per favore, riprova.");
        }
    }

    private void generateReceipt(int orderId, String paymentMethod, double amountReceived, double total) {
        String directoryPath = "receipts";
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(directoryPath + "/receipt_" + orderId + ".txt"))) {
            writer.write("Ricevuta per Ordine #" + orderId + "\n");
            writer.write("Metodo di Pagamento: " + paymentMethod + "\n");
            writer.write("Totale: $" + total + "\n");

            if ("Contanti".equals(paymentMethod)) {
                writer.write("Importo Ricevuto: $" + amountReceived + "\n");
                writer.write("Restante: $" + (amountReceived - total) + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showPaidOrdersScreen() {
        PaidOrdersScreen paidOrdersScreen = new PaidOrdersScreen();
        paidOrdersScreen.show();
    }

    @Override
    public void onOrderStatusChanged(int orderId, boolean isDelivered) {
        System.out.println("Order ID: " + orderId + " delivery status changed to: " + isDelivered);
        Platform.runLater(this::loadOrders);
    }

    @Override
    public void onOrderDeleted(int orderId) {
        System.out.println("Order ID: " + orderId + " has been deleted.");
        Platform.runLater(this::loadOrders);
    }

    @Override
    public void onDishDeleted(int orderId, int menuId) {
        System.out.println("Dish from order ID: " + orderId + " and menu ID: " + menuId + " has been deleted.");
        Platform.runLater(this::loadOrders);
    }

    @Override
    public void onPaymentProcessed(int orderId, int transactionId, String paymentMethod, double amountReceived) {
        System.out.println("Payment processed for order ID: " + orderId + " with transaction ID: " + transactionId + ". Payment Method: " + paymentMethod + ". Amount received: $" + amountReceived);
        Platform.runLater(this::loadOrders);
    }
}
