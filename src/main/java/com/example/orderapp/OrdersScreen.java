package com.example.orderapp;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class OrdersScreen extends Stage implements OrderObserver {

    private final Stage mainStage;
    private VBox mainLayout;
    private final CompleteOrderDAO orderDAO;
    private final DatabaseFacade databaseFacade = new DatabaseFacade();

    public OrdersScreen(Stage mainStage) {
        this.mainStage = mainStage;
        this.orderDAO = new CompleteOrderDAO();
        setTitle("Gestione Ordini");

        mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true);

        loadOrders();

        Scene scene = new Scene(scrollPane, 800, 600);
        setScene(scene);

        // Register this screen as an observer
        orderDAO.addObserver(this);
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

                CheckBox deliveredCheckbox = new CheckBox("Ordine consegnato");
                deliveredCheckbox.setSelected(completeOrder.isDelivered());
                deliveredCheckbox.setOnAction(e -> {
                    orderDAO.updateOrderStatus(completeOrder.getOrderId(), deliveredCheckbox.isSelected());
                    // Notify the observer about the status change
                    onOrderStatusChanged(completeOrder.getOrderId(), deliveredCheckbox.isSelected());
                });

                Button deleteOrderButton = new Button("Cancella Ordine");
                deleteOrderButton.setOnAction(e -> {
                    orderDAO.deleteOrder(completeOrder.getOrderId());
                    // Notify the observer about the order deletion
                    onOrderDeleted(completeOrder.getOrderId());
                });

                Button paymentButton = new Button("Procedi al Pagamento");
                paymentButton.setOnAction(e -> initiatePayment(completeOrder));

                HBox orderHeader = new HBox(10);
                orderHeader.getChildren().addAll(new Label("ID Ordine: " + completeOrder.getOrderId()), deliveredCheckbox, deleteOrderButton, paymentButton);

                orderBox.getChildren().add(orderHeader);

                for (Order order : completeOrder.getDishes()) {
                    HBox dishBox = new HBox(10);
                    Label dishLabel = new Label("Piatto: " + order.getDishName() + " - Quantità: " + order.getQuantity() + " - Prezzo: $" + order.getDishPrice());
                    Button deleteDishButton = new Button("Cancella Piatto");
                    deleteDishButton.setOnAction(e -> {
                        orderDAO.deleteDish(completeOrder.getOrderId(), order.getMenuId());
                        // Notify the observer about the dish deletion
                        onDishDeleted(completeOrder.getOrderId(), order.getMenuId());
                    });

                    dishBox.getChildren().addAll(dishLabel, deleteDishButton);
                    orderBox.getChildren().add(dishBox);
                }

                Label totalLabel = new Label("Totale: $" + completeOrder.getTotalPrice());
                orderBox.getChildren().add(totalLabel);

                mainLayout.getChildren().add(orderBox);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorAlert("Errore durante il caricamento degli ordini", "Si è verificato un errore durante il caricamento degli ordini. Per favore, riprova.");
        } finally {
            databaseFacade.closeConnection();
        }
    }

    private void initiatePayment(CompleteOrder completeOrder) {
        if (!completeOrder.isDelivered()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attenzione");
            alert.setHeaderText(null);
            alert.setContentText("L'ordine non è stato ancora consegnato.");
            alert.showAndWait();
            return;
        }

        Stage paymentStage = new Stage();
        paymentStage.setTitle("Procedi al Pagamento");

        VBox paymentLayout = new VBox(10);
        paymentLayout.setPadding(new Insets(10));

        Label paymentMethodLabel = new Label("Seleziona il metodo di pagamento:");
        ComboBox<String> paymentMethodComboBox = new ComboBox<>();
        paymentMethodComboBox.getItems().addAll("Carta", "Contanti");

        Label amountReceivedLabel = new Label("Importo ricevuto:");
        TextField amountReceivedField = new TextField();
        amountReceivedField.setDisable(true);

        paymentMethodComboBox.setOnAction(e -> {
            if ("Contanti".equals(paymentMethodComboBox.getValue())) {
                amountReceivedField.setDisable(false);
            } else {
                amountReceivedField.setDisable(true);
            }
        });

        // Adding the summary of dishes and total price
        Label orderSummaryLabel = new Label("Riepilogo Ordine:");
        VBox orderSummaryBox = new VBox(5);
        for (Order order : completeOrder.getDishes()) {
            Label dishLabel = new Label("Piatto: " + order.getDishName() + " - Quantità: " + order.getQuantity() + " - Prezzo: $" + order.getDishPrice());
            orderSummaryBox.getChildren().add(dishLabel);
        }
        Label totalLabel = new Label("Totale: $" + completeOrder.getTotalPrice());

        Button confirmButton = new Button("Conferma Pagamento");
        confirmButton.setOnAction(e -> {
            String paymentMethod = paymentMethodComboBox.getValue();
            if (paymentMethod != null) {
                if ("Contanti".equals(paymentMethod)) {
                    String amountReceivedStr = amountReceivedField.getText();
                    if (amountReceivedStr.isEmpty() || !isNumeric(amountReceivedStr)) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Attenzione");
                        alert.setHeaderText(null);
                        alert.setContentText("Inserisci un importo valido.");
                        alert.showAndWait();
                        return;
                    }
                    double amountReceived = Double.parseDouble(amountReceivedStr);
                    processPayment(completeOrder, paymentMethod, amountReceived);
                } else {
                    processPayment(completeOrder, paymentMethod, 0);
                }
                paymentStage.close();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Attenzione");
                alert.setHeaderText(null);
                alert.setContentText("Seleziona un metodo di pagamento.");
                alert.showAndWait();
            }
        });

        paymentLayout.getChildren().addAll(paymentMethodLabel, paymentMethodComboBox, amountReceivedLabel, amountReceivedField, orderSummaryLabel, orderSummaryBox, totalLabel, confirmButton);
        Scene scene = new Scene(paymentLayout, 300, 400);
        paymentStage.setScene(scene);
        paymentStage.show();
    }

    private void processPayment(CompleteOrder completeOrder, String paymentMethod, double amountReceived) {
        try {
            databaseFacade.openConnection();
            int transactionId = orderDAO.processPaymentTransaction(completeOrder.getOrderId(), paymentMethod, completeOrder.getTableId());
            if (transactionId != -1) {
                generateReceipt(completeOrder.getOrderId(), transactionId, completeOrder.getTableId(), paymentMethod, amountReceived);
                // Notify the observer about the payment processing
                onPaymentProcessed(completeOrder.getOrderId(), transactionId, paymentMethod, amountReceived);
            }
            loadOrders(); // Refresh the orders screen to remove completed orders
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            showErrorAlert("Errore durante il pagamento", "Si è verificato un errore durante il pagamento. Per favore, riprova.");
        } finally {
            databaseFacade.closeConnection();
        }
    }

    private void generateReceipt(int orderId, int transactionId, int tableId, String paymentMethod, double amountReceived) throws IOException {
        String receiptDirectory = "receipts";
        Files.createDirectories(Paths.get(receiptDirectory)); // Crea la directory se non esiste

        String receiptFileName = receiptDirectory + "/receipt_" + transactionId + ".txt";
        try (FileWriter writer = new FileWriter(receiptFileName)) {
            writer.write("Scontrino\n");
            writer.write("ID Transazione: " + transactionId + "\n");
            writer.write("ID Ordine: " + orderId + "\n");
            writer.write("ID Tavolo: " + tableId + "\n");
            writer.write("Data: " + new Timestamp(System.currentTimeMillis()) + "\n");
            writer.write("Metodo di Pagamento: " + paymentMethod + "\n");
            writer.write("Dettagli Ordine:\n");

            double total = 0;
            List<Order> orders = orderDAO.getOrderDetails(orderId);

            for (Order order : orders) {
                String dishName = order.getDishName();
                int quantity = order.getQuantity();
                double price = order.getDishPrice();
                total += quantity * price;
                writer.write(dishName + " - Quantità: " + quantity + " - Prezzo: $" + price + "\n");
            }

            writer.write("Totale: $" + total + "\n");
            if ("Contanti".equals(paymentMethod)) {
                double change = amountReceived - total;
                writer.write("Importo Ricevuto: $" + amountReceived + "\n");
                writer.write("Resto: $" + change + "\n");
            }

            System.out.println("Scontrino generato: " + receiptFileName); // Messaggio di debug
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Errore durante la generazione dello scontrino", "Si è verificato un errore durante la generazione dello scontrino. Per favore, riprova.");
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Observer methods

    @Override
    public void onOrderStatusChanged(int orderId, boolean delivered) {
        // Refresh the orders screen when the status of an order changes
        loadOrders();
    }

    @Override
    public void onOrderDeleted(int orderId) {
        // Refresh the orders screen when an order is deleted
        loadOrders();
    }

    @Override
    public void onDishDeleted(int orderId, int menuId) {
        // Refresh the orders screen when a dish is deleted
        loadOrders();
    }

    @Override
    public void onPaymentProcessed(int orderId, int transactionId, String paymentMethod, double amountReceived) {
        // Refresh the orders screen when a payment is processed
        loadOrders();
    }
}
