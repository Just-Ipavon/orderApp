package com.example.orderapp;

import com.example.orderapp.classes.CompleteOrder;
import com.example.orderapp.classes.Order;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class PaidOrdersScreen extends Stage {

    private VBox mainLayout;
    private final CompleteOrderDAO orderDAO;
    private final DatabaseFacade databaseFacade = new DatabaseFacade();

    public PaidOrdersScreen() {
        this.orderDAO = new CompleteOrderDAO();
        setTitle("Ordini Pagati");

        mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true);

        loadPaidOrders();

        Scene scene = new Scene(scrollPane, 800, 600);
        setScene(scene);
    }

    private void loadPaidOrders() {
        try {
            databaseFacade.openConnection();
            List<CompleteOrder> paidOrders = orderDAO.getAllCompletedOrders();
            mainLayout.getChildren().clear();

            for (CompleteOrder completeOrder : paidOrders) {
                VBox orderBox = new VBox(10);
                orderBox.setPadding(new Insets(10));
                orderBox.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-padding: 10;");

                Label orderIdLabel = new Label("ID Ordine: " + completeOrder.getOrderId());
                Label tableIdLabel = new Label("ID Tavolo: " + completeOrder.getTableId());
                Label deliveredLabel = new Label("Consegnato: " + (completeOrder.isDelivered() ? "Sì" : "No"));
                Label completedLabel = new Label("Completato: " + (completeOrder.isCompleted() ? "Sì" : "No"));
                Label paymentMethodLabel = new Label("Metodo di Pagamento: " + completeOrder.getPaymentMethod());
                Label transactionDateLabel = new Label("Data Transazione: " + completeOrder.getTransactionDate().toString());

                orderBox.getChildren().addAll(orderIdLabel, tableIdLabel, deliveredLabel, completedLabel, paymentMethodLabel, transactionDateLabel);

                for (Order order : completeOrder.getDishes()) {
                    Label dishLabel = new Label("Piatto: " + order.getDishName() + " - Quantità: " + order.getQuantity() + " - Prezzo: $" + order.getDishPrice());
                    orderBox.getChildren().add(dishLabel);
                }

                mainLayout.getChildren().add(orderBox);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorAlert("Errore durante il caricamento degli ordini pagati", "Si è verificato un errore durante il caricamento degli ordini pagati. Per favore, riprova.");
        } finally {
            databaseFacade.closeConnection();
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
