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

            String insertPlacedOrderQuery = "INSERT INTO orders (order_time, delivered, completed, table_id) VALUES (?, ?, ?, ?)";
            String insertOrderQuery = "INSERT INTO dishes_order (order_id, menu_id, quantity, note) VALUES (?, ?, ?, ?)";

            try (PreparedStatement placedOrderStmt = databaseFacade.getConnection().prepareStatement(insertPlacedOrderQuery, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement orderStmt = databaseFacade.getConnection().prepareStatement(insertOrderQuery)) {

                Timestamp orderTime = new Timestamp(System.currentTimeMillis());
                placedOrderStmt.setTimestamp(1, orderTime);
                placedOrderStmt.setBoolean(2, false);
                placedOrderStmt.setBoolean(3, false);
                placedOrderStmt.setInt(4, tableId);
                placedOrderStmt.executeUpdate();

                ResultSet generatedKeys = placedOrderStmt.getGeneratedKeys();
                int orderId = 0;
                if (generatedKeys.next()) {
                    orderId = generatedKeys.getInt(1);
                }

                for (Order order : orders) {
                    orderStmt.setInt(1, orderId);
                    orderStmt.setInt(2, order.getMenuId());
                    orderStmt.setInt(3, 1);
                    orderStmt.setString(4, null);
                    orderStmt.executeUpdate();
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
        logMessage.append("Il cameriere ")
                .append(waiter.getFirstName())
                .append(" ")
                .append(waiter.getLastName())
                .append(" ha preso l'ordine per il tavolo ")
                .append(table.getTableId())
                .append(": ");

        for (Order order : orders) {
            logMessage.append(order.getDishName()).append(", ");
        }

        logMessage.setLength(logMessage.length() - 2); // Remove the last comma and space
        logMessage.append(".");

        Label logLabel = new Label(logMessage.toString());
        summaryBox.getChildren().add(logLabel);

        logToFile(logMessage.toString());
    }

    private void logOrderDelivered(Waiter waiter, Table table) {
        String logMessage = "Il cameriere " + waiter.getFirstName() + " " + waiter.getLastName() + " ha consegnato l'ordine al tavolo " + table.getTableId() + ".";
        Label logLabel = new Label(logMessage);
        summaryBox.getChildren().add(logLabel);

        logToFile(logMessage);
    }

    private void logToFile(String logMessage) {
        String logFilePath = "summary.log";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(logMessage);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processPayment(Table table) {
        int tableId = table.getTableId();
        int orderId = getOrderIdForTable(tableId);
        if (orderId != -1) {
            // Change the table color to red again
            table.getRectangle().setFill(Color.RED);
            activeOrders.decrementAndGet(); // Decrement active orders count

            // Use CompleteOrderDAO to mark the order as completed and assign a transaction
            String paymentMethod = "bancomat"; // Example payment method, can be dynamic based on simulation requirements
            completeOrderDAO.processPaymentTransaction(orderId, paymentMethod);

            // Log the payment completion
            logPaymentProcessed(tableId);
        }
    }

    private int getOrderIdForTable(int tableId) {
        int orderId = -1;
        try {
            databaseFacade.openConnection();
            String query = "SELECT order_id FROM orders WHERE table_id = ? AND completed = FALSE";
            try (PreparedStatement pstmt = databaseFacade.getConnection().prepareStatement(query)) {
                pstmt.setInt(1, tableId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        orderId = rs.getInt("order_id");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            databaseFacade.closeConnection();
        }
        return orderId;
    }

    private void logPaymentProcessed(int tableId) {
        String logMessage = "Il pagamento per il tavolo " + tableId + " è stato elaborato.";
        Label logLabel = new Label(logMessage);
        summaryBox.getChildren().add(logLabel);

        logToFile(logMessage);
    }
}
