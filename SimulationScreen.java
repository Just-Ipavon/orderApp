package com.example.orderapp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.orderapp.classes.Order;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

//Schermata simulativa dell'esecuzione del progetto
public class SimulationScreen extends Application {
    private GridPane tableGrid;
    private TextArea logArea;
    private Map<Integer, Button> tableButtons = new HashMap<>();
    private List<Waiter> waiters = new ArrayList<>();
    private ExecutorService executorService;
    private CompleteOrderDAO completeOrderDAO = new CompleteOrderDAO();
    private DatabaseFacade dbFacade = new DatabaseFacade();
    //Metodo start per avviare la schermata
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Simulazione Ristorante");
        //Definizione della schermata per la simulazione
        BorderPane root = new BorderPane();
        tableGrid = new GridPane();
        tableGrid.setPadding(new Insets(10));
        tableGrid.setHgap(10);
        tableGrid.setVgap(10);
        //Definizione dell'area per i log dei camerieri
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        //Posizionamento delle due aree
        root.setCenter(tableGrid);
        root.setBottom(logArea);
        //Inizializzazione della Scene
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        //Import dei tavoli e camerieri dal DB
        loadTablesFromDB();
        loadWaitersFromDB();
        //Oggetto che esegue i thread
        executorService = Executors.newFixedThreadPool(waiters.size());
        for (Waiter waiter : waiters) {
            //Ogni Waiter viene aggiunto all'executor
            executorService.submit(waiter);
        }
        primaryStage.show();
    }
    //Function per l'importazione
    private void loadTablesFromDB() {
        //Gestione eccezioni
        try {
            dbFacade.openConnection(); //Apriamo la connection
            Connection conn = dbFacade.getConnection(); //Otteniamo la connessione
            //Query al DB
            String query = "SELECT table_id FROM tables";//Query al DB
            //Gestione eccezioni
            try (PreparedStatement stmt = conn.prepareStatement(query);
                ResultSet rs = stmt.executeQuery()) {
                int row = 0, col = 0;
                //Iteriamo sui risultati
                while (rs.next()) {
                    int tableId = rs.getInt("table_id");
                    Button tableButton = createTableButton(tableId);
                    tableGrid.add(tableButton, col, row);
                    tableButtons.put(tableId, tableButton);
                    col += 1%4; //Aritmetica modulare sui tavoli
                    /*if (col > 4) {
                        col = 0;
                        row++;
                    }*/
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbFacade.closeConnection();
        }
    }
    //Bottone per la creazione di un tavolo
    private Button createTableButton(int tableId) {
        Button button = new Button("Tavolo " + tableId);
        button.setPrefSize(100, 100);
        button.setStyle("-fx-background-color: red;");
        return button;
    }
    //Function per l'import dei camerieri
    private void loadWaitersFromDB() {
        try {
            dbFacade.openConnection();
            Connection conn = dbFacade.getConnection();
            //Query al DB
            String query = "SELECT user_id FROM users WHERE waiter = TRUE";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int waiterId = rs.getInt("user_id");
                    Waiter waiter = new Waiter(waiterId, this);
                    waiters.add(waiter);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbFacade.closeConnection();
        }
    }

    //Function per l'update dello statolo del tavolo (cambio colore)
    public void updateTableStatus(int tableId, TableStatus status) {
        Platform.runLater(() -> {
            Button tableButton = tableButtons.get(tableId);
            if (tableButton != null) {
                switch (status) {
                    //Caso ROSSO - Tavolo libero
                    case EMPTY:
                        tableButton.setStyle("-fx-background-color: red;");
                        break;
                    //Caso GIALLO - Occupato, aspetta ordine
                    case ORDERED:
                        tableButton.setStyle("-fx-background-color: yellow;");
                        break;
                    //Caso VERDE - Occupato, ordine ricevuto
                    case SERVED:
                        tableButton.setStyle("-fx-background-color: green;");
                        break;
                    //Caso BLUE - Il tavolo sta pagando l'ordine
                    case PAYING:
                        tableButton.setStyle("-fx-background-color: blue;");
                        break;
                }
            }
        });
    }
    //Function che appende il log nell'area dedicata
    public void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    //Function che effettua il servizio dell'ordine al tavolo
    public void serveTable(int tableId, int waiterId) {
        MenuScreen menuScreen = new MenuScreen(new Stage());
        List<Order> orders = menuScreen.simulateOrderCreation();
        completeOrderDAO.submitOrder(tableId, orders);
        updateTableStatus(tableId, TableStatus.ORDERED);
        log("Cameriere " + waiterId + " ha preso l'ordine per il tavolo " + tableId);
    }

    public void deliverOrder(int tableId, int waiterId) {
        completeOrderDAO.updateOrderStatus(tableId, true);
        updateTableStatus(tableId, TableStatus.SERVED);
        log("Cameriere " + waiterId + " ha consegnato l'ordine al tavolo " + tableId);
    }

    public void processPayment(int tableId, int waiterId) {
        updateTableStatus(tableId, TableStatus.PAYING);
        log("Cameriere " + waiterId + " sta processando il pagamento per il tavolo " + tableId);
        String paymentMethod = getRandomPaymentMethod();
        completeOrderDAO.processPaymentTransaction(tableId, paymentMethod);
        updateTableStatus(tableId, TableStatus.EMPTY);
        log("Cameriere " + waiterId + " ha completato il pagamento per il tavolo " + tableId);
    }

    private String getRandomPaymentMethod() {
        String[] methods = {"Contanti", "Carta di credito", "Bancomat"};
        return methods[new Random().nextInt(methods.length)];
    }

    @Override
    public void stop() {
        executorService.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public enum TableStatus {
        EMPTY, ORDERED, SERVED, PAYING
    }

    private class Waiter implements Runnable {
        private int waiterId;
        private SimulationScreen screen;

        public Waiter(int waiterId, SimulationScreen screen) {
            this.waiterId = waiterId;
            this.screen = screen;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    int tableId = getRandomEmptyTable();
                    if (tableId != -1) {
                        screen.serveTable(tableId, waiterId);
                        Thread.sleep(5000); // Simula il tempo di preparazione dell'ordine
                        screen.deliverOrder(tableId, waiterId);
                        Thread.sleep(10000); // Simula il tempo di consumazione
                        screen.processPayment(tableId, waiterId);
                    }
                    Thread.sleep(2000); // Pausa prima di servire il prossimo tavolo
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private int getRandomEmptyTable() {
            List<Integer> emptyTables = new ArrayList<>();
            for (Map.Entry<Integer, Button> entry : tableButtons.entrySet()) {
                if (entry.getValue().getStyle().contains("red")) {
                    emptyTables.add(entry.getKey());
                }
            }
            if (!emptyTables.isEmpty()) {
                return emptyTables.get(new Random().nextInt(emptyTables.size()));
            }
            return -1;
        }
    }
}