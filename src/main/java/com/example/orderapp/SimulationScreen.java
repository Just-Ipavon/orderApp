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
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

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
                            assignWaiterToRandomTable(waiters.get(waiterIndex));
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
                                assignWaiterToRandomTable(waiter);
                            }
                        }
                    }
                });
            }
        }, numberOfWaiters * 3000, 5000); // Start after all waiters have been initially assigned and then every 5 seconds
    }

    private void assignWaiterToRandomTable(Waiter waiter) {
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

    private void placeRandomOrder(Table table, Waiter waiter) {
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

                ResultSet rs = placedOrderStmt.getGeneratedKeys();
                int orderId = 0;
                if (rs.next()) {
                    orderId = rs.getInt(1);
                }
                rs.close();

                for (Order order : orders) {
                    orderStmt.setInt(1, orderId);
                    orderStmt.setInt(2, order.getMenuId());
                    orderStmt.setInt(3, 1); // Assume quantity is 1
                    orderStmt.setString(4, order.getNotes());
                    orderStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            databaseFacade.closeConnection();
        }
    }

    private void processPayment(Table table) {
        try {
            databaseFacade.openConnection();

            // Set order completion flag to true and process the payment transaction
            String paymentMethod = "credit card"; // Example payment method
            int orderId = processPaymentTransaction(table.getTableId(), paymentMethod);

            table.getRectangle().setFill(Color.RED);
            activeOrders.decrementAndGet(); // Decrement active orders count

            // Log the table payment
            logPayment(table);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            databaseFacade.closeConnection();
        }
    }

    private int processPaymentTransaction(int tableId, String paymentMethod) throws SQLException {
        int transactionId = -1;

        String selectOrderIdQuery = "SELECT order_id FROM orders WHERE table_id = ? AND completed = FALSE";
        try (PreparedStatement selectOrderStmt = databaseFacade.getConnection().prepareStatement(selectOrderIdQuery)) {
            selectOrderStmt.setInt(1, tableId);
            ResultSet rs = selectOrderStmt.executeQuery();
            if (rs.next()) {
                int orderId = rs.getInt("order_id");

                // Insert into transactions table
                String transactionQuery = "INSERT INTO transaction (payment_method, table_id, transaction_date) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = databaseFacade.getConnection().prepareStatement(transactionQuery, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, paymentMethod);
                    pstmt.setInt(2, tableId);
                    pstmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                    pstmt.executeUpdate();
                    ResultSet rsTrans = pstmt.getGeneratedKeys();
                    if (rsTrans.next()) {
                        transactionId = rsTrans.getInt(1);
                    }
                }

                // Update the order status to completed
                String updateOrderQuery = "UPDATE orders SET completed = TRUE WHERE order_id = ?";
                try (PreparedStatement pstmt = databaseFacade.getConnection().prepareStatement(updateOrderQuery)) {
                    pstmt.setInt(1, orderId);
                    pstmt.executeUpdate();
                }
            }
        }
        return transactionId;
    }

    private List<MenuItem> getMenuItemsFromDatabase() {
        List<MenuItem> menuItems = new ArrayList<>();
        try {
            databaseFacade.openConnection();

            String query = "SELECT * FROM menus";
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
        logMessage.append(String.format("Cameriere %s %s ha preso l'ordine al tavolo %d: ",
                waiter.getFirstName(), waiter.getLastName(), table.getTableId()));

        for (Order order : orders) {
            logMessage.append(String.format("\n- %s (x%d) - €%.2f", order.getDishName(), order.getQuantity(), order.getDishPrice()));
        }
        logSummary(logMessage.toString());
    }

    private void logOrderDelivered(Waiter waiter, Table table) {
        logSummary(String.format("Cameriere %s %s ha consegnato l'ordine al tavolo %d.",
                waiter.getFirstName(), waiter.getLastName(), table.getTableId()));
    }

    private void logPayment(Table table) {
        logSummary(String.format("Il tavolo %d ha pagato l'ordine e se ne va.", table.getTableId()));
    }

    private void logSummary(String message) {
        Text text = new Text(message + "\n");
        text.setWrappingWidth(summaryScrollPane.getWidth() - 20); // Set wrapping width
        TextFlow textFlow = new TextFlow(text);
        summaryBox.getChildren().add(textFlow);
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static void launch(String[] args) {
    }
}

