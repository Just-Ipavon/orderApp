package com.example.orderapp;

import com.example.orderapp.classes.MenuItem;
import com.example.orderapp.classes.Table;
import com.example.orderapp.classes.Order;
import com.example.orderapp.classes.Waiter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulationScreen extends Stage {
    private final List<Table> tables = new ArrayList<>();
    private final DatabaseFacade databaseFacade = new DatabaseFacade();
    private final GridPane grid = new GridPane();
    private final VBox summaryBox = new VBox();
    private final ScrollPane summaryScrollPane = new ScrollPane();
    private final AtomicInteger activeOrders = new AtomicInteger(0);
    private final AtomicInteger activeWaiters = new AtomicInteger(0);
    private final int numberOfWaiters;
    private final List<Waiter> waiters = new ArrayList<>();
    private boolean running = true;
    private final GridPane waiterGrid = new GridPane();
    private final Map<Waiter, Rectangle> waiterRectangles = new HashMap<>();

    // Costruttore della finestra di simulazione
    public SimulationScreen() throws SQLException {
        this.setTitle("Simulazione di una serata di lavoro");

        // Recupera i camerieri dal database e crea i tavoli
        this.numberOfWaiters = getWaitersFromDatabase();
        createTablesFromDatabase();

        // Impostazioni della griglia dei tavoli e del riepilogo
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        setupSummaryScrollPane();
        setupWaiterGrid();

        // Layout della finestra principale
        BorderPane root = new BorderPane();
        VBox centerBox = new VBox(10);
        centerBox.getChildren().addAll(waiterGrid, grid);
        root.setCenter(centerBox);
        root.setBottom(summaryScrollPane);
        BorderPane.setMargin(summaryScrollPane, new Insets(10));

        Scene scene = new Scene(root, 800, 800);
        this.setScene(scene);

        // Gestione della chiusura della finestra
        this.setOnCloseRequest(event -> running = false);

        // Avvia la simulazione
        startSimulation();
    }

    // Impostazioni dello scroll pane per il riepilogo degli eventi
    private void setupSummaryScrollPane() {
        summaryScrollPane.setContent(summaryBox);
        summaryScrollPane.setFitToWidth(true);
        summaryScrollPane.setPrefViewportHeight(200);
        summaryScrollPane.setPrefViewportWidth(780);
        summaryScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        summaryScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        summaryBox.setPadding(new Insets(10));
        summaryBox.setFillWidth(true);
        summaryBox.setPrefWidth(760);
    }

    // Impostazioni della griglia per visualizzare i camerieri
    private void setupWaiterGrid() {
        waiterGrid.setPadding(new Insets(10));
        waiterGrid.setHgap(10);
        waiterGrid.setVgap(10);

        for (int i = 0; i < waiters.size(); i++) {
            Waiter waiter = waiters.get(i);
            Rectangle waiterRect = new Rectangle(30, 30);
            waiterRect.setFill(Color.RED);
            Label initials = new Label(getInitials(waiter));
            initials.setTextFill(Color.WHITE);
            StackPane waiterPane = new StackPane(waiterRect, initials);
            waiterGrid.add(waiterPane, i, 0);
            waiterRectangles.put(waiter, waiterRect);
        }
    }

    // Ottiene le iniziali del nome e cognome di un cameriere
    private String getInitials(Waiter waiter) {
        return waiter.firstName().charAt(0) + waiter.lastName().substring(0, 1);
    }

    // Recupera i camerieri dal database e li aggiunge alla lista
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

    // Crea i tavoli dalla base di dati e li aggiunge alla lista
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

                Rectangle rectangle = new Rectangle(100, 100);
                rectangle.setFill(Color.RED);
                table.setRectangle(rectangle);

                Label label = new Label("" + table.getTableId());
                label.setTextFill(Color.WHITE);
                StackPane tablePane = new StackPane(rectangle, label);
                tablePane.setAlignment(Pos.CENTER);

                grid.add(tablePane, index % 5, index / 5);
                index++;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            databaseFacade.closeConnection();
        }
    }
    //fa iniziare la simulazione
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
            }, i * 3000L);
        }

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
        }, numberOfWaiters * 3000L, 5000);
    }
    //assegna un cameriere ad un tavolo random
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
            waiterRectangles.get(waiter).setFill(Color.GREEN);
            placeRandomOrder(table, waiter);
        }
    }
    //genera un ordine random
    private void placeRandomOrder(Table table, Waiter waiter) {
        Random random = new Random();
        List<MenuItem> menuItems = getMenuItemsFromDatabase();
        List<Order> orders = new ArrayList<>();

        int numItemsToOrder = random.nextInt(menuItems.size()) + 1;
        for (int i = 0; i < numItemsToOrder; i++) {
            MenuItem menuItem = menuItems.get(random.nextInt(menuItems.size()));
            orders.add(new Order(menuItem.menuId(), menuItem.name(), menuItem.price()));
        }

        if (orders.size() > table.getNumberOfSeats()) {
            orders = orders.subList(0, table.getNumberOfSeats());
        }

        table.getRectangle().setFill(Color.YELLOW);
        activeOrders.incrementAndGet();

        logOrderTaken(waiter, table, orders);

        List<Order> finalOrders = orders;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    int orderId = submitOrder(table.getTableId(), finalOrders);
                    table.getRectangle().setFill(Color.GREEN);
                    logOrderDelivered(waiter, table);

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> {
                                processPayment(table, orderId, finalOrders);
                                activeWaiters.decrementAndGet();
                                waiterRectangles.get(waiter).setFill(Color.RED);
                            });
                        }
                    }, 10000);
                });
            }
        }, 5000);
    }

    // Invia l'ordine al database
    private int submitOrder(int tableId, List<Order> orders) {
        int orderId = -1;
        try {
            databaseFacade.openConnection();

            String insertPlacedOrderQuery = "INSERT INTO orders (order_time, delivered, completed, table_id) VALUES (?, ?, ?, ?)";
            String insertOrderQuery = "INSERT INTO dishes_order (order_id, menu_id, quantity, note) VALUES (?, ?, ?, ?)";

            try (PreparedStatement placedOrderStmt = databaseFacade.getConnection().prepareStatement(insertPlacedOrderQuery, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement orderStmt = databaseFacade.getConnection().prepareStatement(insertOrderQuery)) {

                placedOrderStmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                placedOrderStmt.setBoolean(2, false);
                placedOrderStmt.setBoolean(3, false);
                placedOrderStmt.setInt(4, tableId);
                placedOrderStmt.executeUpdate();

                ResultSet generatedKeys = placedOrderStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    orderId = generatedKeys.getInt(1);

                    for (Order order : orders) {
                        orderStmt.setInt(1, orderId);
                        orderStmt.setInt(2, order.getMenuId());
                        orderStmt.setInt(3, 1);
                        orderStmt.setString(4, "");
                        orderStmt.addBatch();
                    }
                    orderStmt.executeBatch();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            databaseFacade.closeConnection();
        }
        return orderId;
    }
    //processa il pagamento
    private void processPayment(Table table, int orderId, List<Order> orders) {
        activeOrders.decrementAndGet();
        table.getRectangle().setFill(Color.RED);

        double total = orders.stream().mapToDouble(Order::getDishPrice).sum();
        String paymentMethod = new Random().nextBoolean() ? "Contanti" : "Carta";
        double amountReceived = Math.ceil(total);

        generateReceipt(orderId, paymentMethod, amountReceived, total);
    }
    //genera lo scontrino
    private void generateReceipt(int orderId, String paymentMethod, double amountReceived, double total) {
        String directoryPath = "receipts";
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = directoryPath + File.separator + "receipt_" + orderId + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("Ricevuta per l'ordine #" + orderId);
            writer.newLine();
            writer.write("Data: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            writer.newLine();
            writer.write("Totale: €" + String.format("%.2f", total));
            writer.newLine();
            writer.write("Metodo di pagamento: " + paymentMethod);
            writer.newLine();
            writer.write("Importo ricevuto: €" + String.format("%.2f", amountReceived));
            writer.newLine();
            writer.write("Resto: €" + String.format("%.2f", amountReceived - total));
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //ottiene i piatti dal database
    private List<MenuItem> getMenuItemsFromDatabase() {
        List<MenuItem> menuItems = new ArrayList<>();
        try {
            databaseFacade.openConnection();
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
    //segna l'ordine preso in un log
    private void logOrderTaken(Waiter waiter, Table table, List<Order> orders) {
        StringBuilder log = new StringBuilder("Il cameriere " + waiter.firstName() + " " + waiter.lastName() +
                " ha preso un ordine dal tavolo " + table.getTableId() + ":\n");
        for (Order order : orders) {
            log.append(String.format("\n- %s (x%d) - €%.2f", order.getDishName(), order.getQuantity(), order.getDishPrice()));
        }
        addToSummaryBox(log.toString());
        writeLogToFile(log.toString());
    }
    //segna l'ordine consegnato in un log
    private void logOrderDelivered(Waiter waiter, Table table) {
        String log = "Il cameriere " + waiter.firstName() + " " + waiter.lastName() +
                " ha consegnato l'ordine al tavolo " + table.getTableId() + ".\n";
        addToSummaryBox(log);
        writeLogToFile(log);
    }
    //genera una sezione con i log
    private void addToSummaryBox(String text) {
        Label logLabel = new Label(text);
        logLabel.setWrapText(true);
        summaryBox.getChildren().add(logLabel);
        summaryBox.layout();
        summaryScrollPane.setVvalue(1.0);
    }
    //scrive i log su un file
    private void writeLogToFile(String log) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File("order_logs.txt"), true))) {

            writer.write(log);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}