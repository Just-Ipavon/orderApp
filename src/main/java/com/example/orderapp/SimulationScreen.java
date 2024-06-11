package com.example.orderapp;

import com.example.orderapp.classes.MenuItem;
import com.example.orderapp.classes.Order;
import com.example.orderapp.classes.Table;
import com.example.orderapp.classes.Waiter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulationScreen extends Stage {
    private final List<Table> tables = new ArrayList<>();
    private final DatabaseFacade databaseFacade = new DatabaseFacade();
    private final GridPane grid = new GridPane();
    private final VBox summaryBox = new VBox(); // Summary box for logs
    private final ScrollPane summaryScrollPane = new ScrollPane(); // ScrollPane for summary
    private final AtomicInteger activeOrders = new AtomicInteger(0); // Track active orders
    private final AtomicInteger activeWaiters = new AtomicInteger(0); // Track active waiters
    private int numberOfWaiters;
    private final List<Waiter> waiters = new ArrayList<>(); // List of waiters
    private boolean running = true; // Simulation running flag
    private final CompleteOrderDAO completeOrderDAO = new CompleteOrderDAO();

    public SimulationScreen(Stage mainStage) throws SQLException {
        this.setTitle("Simulazione di una serata di lavoro");

        // Get the number of available waiters and their details
        this.numberOfWaiters = getWaitersFromDatabase();

        // Create tables from the database
        createTablesFromDatabase();

        // Configure the layout
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        // Setup ScrollPane for summary
        summaryScrollPane.setContent(summaryBox);
        summaryScrollPane.setFitToWidth(true);
        summaryScrollPane.setPrefHeight(150); // Set fixed height

        summaryBox.setPadding(new Insets(10));
        summaryBox.heightProperty().addListener((observable, oldValue, newValue) -> {
            summaryScrollPane.setVvalue((Double) newValue); // Auto-scroll to the bottom
        });

        BorderPane root = new BorderPane();
        root.setCenter(grid);
        root.setBottom(summaryScrollPane);

        Scene scene = new Scene(root, 800, 600);
        this.setScene(scene);

        // Close the simulation when the window is closed
        this.setOnCloseRequest(event -> running = false);

        // Start the simulation
        startSimulation();
    }

    private int getWaitersFromDatabase() throws SQLException {
        int waiterCount = 0;
        databaseFacade.openConnection();
        try {
            String query = "SELECT id, firstName, lastName FROM user WHERE waiter = TRUE";
            ResultSet rs = databaseFacade.executeQuery(query);
            while (rs.next()) {
                int userId = rs.getInt("id");
                String firstName = rs.getString("firstName");
                String lastName = rs.getString("lastName");
                waiters.add(new Waiter(userId, firstName, lastName));
                waiterCount++;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            databaseFacade.closeConnection();
        }
        return waiterCount;
    }

    private void createTablesFromDatabase() throws SQLException {
        databaseFacade.openConnection();
        try {
            String query = "SELECT table_id, seats FROM tables";
            ResultSet rs = databaseFacade.executeQuery(query);
            int index = 0;
            while (rs.next()) {
                int tableId = rs.getInt("table_id");
                int numberOfSeats = rs.getInt("seats");
                Table table = new Table(tableId, numberOfSeats);
                tables.add(table);

                Label label = new Label("Tavolo " + table.getTableId());
                Rectangle rectangle = new Rectangle(100, 100);
                rectangle.setFill(Color.RED); // Initially all tables are red
                table.setRectangle(rectangle);

                grid.add(label, index % 5, index / 5 * 2); // Position tables in a grid
                grid.add(rectangle, index % 5, index / 5 * 2 + 1);
                index++;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            databaseFacade.closeConnection();
        }
    }

    private void startSimulation() {
        Timer timer = new Timer();

        for (int i = 0; i < numberOfWaiters; i++) {
            final int waiterIndex = i;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        if (running) {
                            try {
                                assignWaiterToRandomTable(waiters.get(waiterIndex));
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
            }, i * 3000); // Staggered start every 3 seconds
        }

        // Continue assigning waiters to tables every 5 seconds
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (running) {
                        for (Waiter waiter : waiters) {
                            if (activeWaiters.get() < numberOfWaiters && running) {
                                try {
                                    assignWaiterToRandomTable(waiter);
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                });
            }
        }, numberOfWaiters * 3000, 5000); // Start after all waiters have been initially assigned and then every 5 seconds
    }

    private void assignWaiterToRandomTable(Waiter waiter) throws SQLException {
        Random random = new Random();
        List<Table> availableTables = new ArrayList<>();
        for (Table table : tables) {
            if (table.getRectangle().getFill() == Color.RED) {
                availableTables.add(table);
            }
        }
        if (!availableTables.isEmpty() && activeOrders.get() < numberOfWaiters) {
            Table table = availableTables.get(random.nextInt(availableTables.size()));
            activeWaiters.incrementAndGet();
            placeRandomOrder(table, waiter);
        }
    }

    private void placeRandomOrder(Table table, Waiter waiter) throws SQLException {
        Random random = new Random();
        List<MenuItem> menuItems = getMenuItemsFromDatabase();
        List<Order> orders = new ArrayList<>();

        // Randomly select a number of dishes to order
        int numItemsToOrder = random.nextInt(menuItems.size()) + 1;
        for (int i = 0; i < numItemsToOrder; i++) {
            MenuItem menuItem = menuItems.get(random.nextInt(menuItems.size()));
            orders.add(new Order(menuItem.getMenuId(), menuItem.getName(), menuItem.getPrice()));
        }

        // Limit the number of orders to the number of seats at the table
        if (orders.size() > table.getNumberOfSeats()) {
            orders = orders.subList(0, table.getNumberOfSeats());
        }

        // Change the table color to yellow
        table.getRectangle().setFill(Color.YELLOW);
        activeOrders.incrementAndGet(); // Increment active orders count

        // Log the waiter taking the order
        logOrderTaken(waiter, table, orders);

        // Wait 5 seconds to confirm the order
        List<Order> finalOrders = orders;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    submitOrder(table.getTableId(), finalOrders);
                    table.getRectangle().setFill(Color.GREEN);
                    // Log the waiter delivering the order
                    logOrderDelivered(waiter, table);

                    // Wait 10 seconds to proceed to payment
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> {
                                processPayment(table);
                                activeWaiters.decrementAndGet();
                            });
                        }
                    }, 10000);
                });
            }
        }, 5000);
    }

    private void submitOrder(int tableId, List<Order> orders) {
        try {
            databaseFacade.openConnection();

            // Inizializza un PreparedStatement per inserire nella tabella orders
            String insertOrderQuery = "INSERT INTO orders (order_time, delivered, table_id) VALUES (?, ?, ?)";
            try (PreparedStatement orderStmt = databaseFacade.getConnection().prepareStatement(insertOrderQuery, Statement.RETURN_GENERATED_KEYS)) {

                Timestamp orderTime = new Timestamp(System.currentTimeMillis());
                orderStmt.setTimestamp(1, orderTime);
                orderStmt.setBoolean(2, false); // Initial state is not delivered
                orderStmt.setInt(3, tableId);
                orderStmt.executeUpdate();

                ResultSet generatedKeys = orderStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int orderId = generatedKeys.getInt(1);

                    // Inserisci gli ordini specifici
                    String insertDishesOrderQuery = "INSERT INTO dishes_order (order_id, menu_id, quantity, note) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement dishStmt = databaseFacade.getConnection().prepareStatement(insertDishesOrderQuery)) {
                        for (Order order : orders) {
                            dishStmt.setInt(1, orderId);
                            dishStmt.setInt(2, order.getMenuId());
                            dishStmt.setInt(3, 1); // Assuming quantity is always 1 for simplicity
                            dishStmt.setString(4, ""); // Assuming no note for simplicity
                            dishStmt.addBatch();
                        }
                        dishStmt.executeBatch();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            databaseFacade.closeConnection();
        }
    }

    private List<MenuItem> getMenuItemsFromDatabase() throws SQLException {
        List<MenuItem> menuItems = new ArrayList<>();
        databaseFacade.openConnection();
        try {
            String query = "SELECT menu_id, menu_name, menu_price FROM menus";
            ResultSet rs = databaseFacade.executeQuery(query);
            while (rs.next()) {
                int menuId = rs.getInt("menu_id");
                String name = rs.getString("menu_name");
                double price = rs.getDouble("menu_price");
                menuItems.add(new MenuItem(menuId, name, price));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            databaseFacade.closeConnection();
        }
        return menuItems;
    }

    private void logOrderTaken(Waiter waiter, Table table, List<Order> orders) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Il cameriere ").append(waiter.getFirstName()).append(" ").append(waiter.getLastName())
                .append(" ha preso un ordine per il Tavolo ").append(table.getTableId()).append(": ");

        for (Order order : orders) {
            logMessage.append(order.getDishName()).append(" ($").append(order.getDishPrice()).append("), ");
        }

        summaryBox.getChildren().add(new Label(logMessage.toString()));
    }

    private void logOrderDelivered(Waiter waiter, Table table) {
        String logMessage = "Il cameriere " + waiter.getFirstName() + " " + waiter.getLastName() +
                " ha consegnato l'ordine al Tavolo " + table.getTableId();
        summaryBox.getChildren().add(new Label(logMessage));
    }

    private void logPaymentProcessed(int tableId) {
        String logMessage = "Il pagamento è stato completato per il Tavolo " + tableId;
        summaryBox.getChildren().add(new Label(logMessage));
    }

    private void processPayment(Table table) {
        int tableId = table.getTableId();
        int orderId = getOrderIdForTable(tableId);
        if (orderId != -1) {
            // Cambia il colore del tavolo a rosso di nuovo
            table.getRectangle().setFill(Color.RED);
            activeOrders.decrementAndGet(); // Decrementa il conteggio degli ordini attivi

            // Scegli casualmente il metodo di pagamento
            String[] paymentMethods = {"contanti", "bancomat", "carta di credito"};
            Random random = new Random();
            String paymentMethod = paymentMethods[random.nextInt(paymentMethods.length)];

            double total = calculateTotal(orderId); // Metodo per calcolare il totale dell'ordine
            double amountReceived = total;
            if ("contanti".equals(paymentMethod)) {
                amountReceived = total + random.nextInt(100); // Importo ricevuto è almeno il totale
            }

            // Usa CompleteOrderDAO per marcare l'ordine come completato e assegnare una transazione
            completeOrderDAO.processPaymentTransaction(orderId, paymentMethod);

            // Log la conclusione del pagamento
            logPaymentProcessed(tableId);

            // Genera la ricevuta
            generateReceipt(orderId, paymentMethod, amountReceived, total);
        }
    }

    private int getOrderIdForTable(int tableId) {
        int orderId = -1;
        try {
            databaseFacade.openConnection();
            String query = "SELECT order_id FROM orders WHERE table_id = ? AND delivered = TRUE AND completed = FALSE";
            try (PreparedStatement stmt = databaseFacade.getConnection().prepareStatement(query)) {
                stmt.setInt(1, tableId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    orderId = rs.getInt("order_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            databaseFacade.closeConnection();
        }
        return orderId;
    }

    private double calculateTotal(int orderId) {
        double total = 0.0;
        try {
            databaseFacade.openConnection();
            String query = "SELECT SUM(menu_price * quantity) AS total FROM dishes_order JOIN menus ON dishes_order.menu_id = menus.menu_id WHERE order_id = ?";
            try (PreparedStatement stmt = databaseFacade.getConnection().prepareStatement(query)) {
                stmt.setInt(1, orderId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    total = rs.getDouble("total");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            databaseFacade.closeConnection();
        }
        return total;
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

            if ("contanti".equals(paymentMethod)) {
                writer.write("Importo Ricevuto: $" + amountReceived + "\n");
                writer.write("Restante: $" + (amountReceived - total) + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}