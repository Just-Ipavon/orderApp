package com.example.orderapp;
import java.sql.SQLException;
import java.util.List;

import com.example.orderapp.classes.CompleteOrder;
import com.example.orderapp.classes.Order;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
//Classe per visualizzare la classe degli ordini Pagati
public class PaidOrdersScreen extends Stage {
    private final VBox mainLayout;
    private final CompleteOrderDAO orderDAO;
    private final DatabaseFacade databaseFacade = new DatabaseFacade();
    //Costruttore
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
    //Metodo che carica gli ordini pagati
    private void loadPaidOrders() {
        try {
            databaseFacade.openConnection();
            List<CompleteOrder> paidOrders = orderDAO.getAllCompletedOrders();
            mainLayout.getChildren().clear();
            //Itero sugli ordini
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
                //Itero sui singoli Piatti
                for (Order order : completeOrder.getDishes()) {
                    Label dishLabel = new Label("Piatto: " + order.getDishName() + " - Quantità: " + order.getQuantity() + " - Prezzo: $" + order.getDishPrice());
                    orderBox.getChildren().add(dishLabel);
                }
                mainLayout.getChildren().add(orderBox);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorAlert();
        } finally {
            databaseFacade.closeConnection();
        }
    }
    //Metodo che mostra un alert per gli errori
    private void showErrorAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore durante il caricamento degli ordini pagati");
        alert.setHeaderText(null);
        alert.setContentText("Si è verificato un errore durante il caricamento degli ordini pagati. Per favore, riprova.");
        alert.showAndWait();
    }
}