package com.example.orderapp;

import com.example.orderapp.classes.Order;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompleteOrderDAO {

    private DatabaseFacade dbFacade;

    public CompleteOrderDAO() {
        this.dbFacade = new DatabaseFacade();
    }

    private List<OrderObserver> observers = new ArrayList<>();

    public void addObserver(OrderObserver observer) {
        observers.add(observer);
    }

    public List<CompleteOrder> getAllNonCompletedOrders() {
        List<CompleteOrder> orders = new ArrayList<>();
        try {
            dbFacade.openConnection();
            Connection conn = dbFacade.getConnection();
            String query = "SELECT po.order_id, po.table_id, po.delivered, po.completed, io.menu_id, io.quantity, m.menu_name, m.menu_price " +
                    "FROM orders po " +
                    "JOIN dishes_order io ON po.order_id = io.order_id " +
                    "JOIN menus m ON io.menu_id = m.menu_id " +
                    "WHERE po.completed = FALSE " +
                    "ORDER BY po.order_id";

            try (PreparedStatement pstmt = conn.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {

                CompleteOrder currentOrder = null;
                int currentOrderId = -1;

                while (rs.next()) {
                    int orderId = rs.getInt("order_id");
                    int tableId = rs.getInt("table_id");
                    boolean delivered = rs.getBoolean("delivered");
                    boolean completed = rs.getBoolean("completed");
                    int menuId = rs.getInt("menu_id");
                    int quantity = rs.getInt("quantity");
                    String dishName = rs.getString("menu_name");
                    double dishPrice = rs.getDouble("menu_price");

                    if (currentOrder == null || orderId != currentOrderId) {
                        currentOrder = new CompleteOrder(orderId, tableId);
                        currentOrder.setDelivered(delivered);
                        currentOrder.setCompleted(completed);
                        orders.add(currentOrder);
                        currentOrderId = orderId;
                    }

                    Order order = new Order(menuId, dishName, dishPrice);
                    order.setQuantity(quantity);
                    currentOrder.addDish(order);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbFacade.closeConnection();
        }
        return orders;
    }

    public List<CompleteOrder> getAllCompletedOrders() {
        List<CompleteOrder> orders = new ArrayList<>();
        try {
            dbFacade.openConnection();
            Connection conn = dbFacade.getConnection();
            String query = "SELECT po.order_id, po.table_id, po.delivered, po.completed, io.menu_id, io.quantity, m.menu_name, m.menu_price, " +
                    "t.payment_method, t.transaction_date " +
                    "FROM orders po " +
                    "JOIN dishes_order io ON po.order_id = io.order_id " +
                    "JOIN menus m ON io.menu_id = m.menu_id " +
                    "JOIN transaction t ON po.order_id = t.id_transaction " +
                    "WHERE po.completed = TRUE " +
                    "ORDER BY po.order_id";

            try (PreparedStatement pstmt = conn.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {

                CompleteOrder currentOrder = null;
                int currentOrderId = -1;

                while (rs.next()) {
                    int orderId = rs.getInt("order_id");
                    int tableId = rs.getInt("table_id");
                    boolean delivered = rs.getBoolean("delivered");
                    boolean completed = rs.getBoolean("completed");
                    int menuId = rs.getInt("menu_id");
                    int quantity = rs.getInt("quantity");
                    String dishName = rs.getString("menu_name");
                    double dishPrice = rs.getDouble("menu_price");
                    String paymentMethod = rs.getString("payment_method");
                    Timestamp transactionDate = rs.getTimestamp("transaction_date");

                    if (currentOrder == null || orderId != currentOrderId) {
                        currentOrder = new CompleteOrder(orderId, tableId);
                        currentOrder.setDelivered(delivered);
                        currentOrder.setCompleted(completed);
                        currentOrder.setPaymentMethod(paymentMethod);
                        currentOrder.setTransactionDate(transactionDate);
                        orders.add(currentOrder);
                        currentOrderId = orderId;
                    }

                    Order order = new Order(menuId, dishName, dishPrice);
                    order.setQuantity(quantity);
                    currentOrder.addDish(order);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbFacade.closeConnection();
        }
        return orders;
    }

    public void updateOrderStatus(int orderId, boolean delivered) {
        try {
            dbFacade.openConnection();
            Connection conn = dbFacade.getConnection();
            String query = "UPDATE orders SET delivered = ? WHERE order_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setBoolean(1, delivered);
                pstmt.setInt(2, orderId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbFacade.closeConnection();
        }
    }

    public void deleteOrder(int orderId) {
        try {
            dbFacade.openConnection();
            Connection conn = dbFacade.getConnection();
            conn.setAutoCommit(false);

            String deleteInOrderQuery = "DELETE FROM dishes_order WHERE order_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteInOrderQuery)) {
                pstmt.setInt(1, orderId);
                pstmt.executeUpdate();
            }

            String deleteOrderQuery = "DELETE FROM orders WHERE order_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteOrderQuery)) {
                pstmt.setInt(1, orderId);
                pstmt.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                dbFacade.getConnection().rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            dbFacade.closeConnection();
        }
    }

    public void deleteDish(int orderId, int menuId) {
        try {
            dbFacade.openConnection();
            Connection conn = dbFacade.getConnection();
            String query = "DELETE FROM dishes_order WHERE order_id = ? AND menu_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, orderId);
                pstmt.setInt(2, menuId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbFacade.closeConnection();
        }
    }

    public int processPaymentTransaction(int orderId, String paymentMethod, int tableId) {
        int transactionId = -1;
        try {
            dbFacade.openConnection();
            Connection conn = dbFacade.getConnection();
            conn.setAutoCommit(false);

            // Insert into transactions table
            String transactionQuery = "INSERT INTO transaction (payment_method, table_id, transaction_date) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(transactionQuery, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, paymentMethod);
                pstmt.setInt(2, tableId);
                pstmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                pstmt.executeUpdate();
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    transactionId = rs.getInt(1);
                }
            }

            // Update the order status to completed
            String updateOrderQuery = "UPDATE orders SET completed = TRUE WHERE order_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateOrderQuery)) {
                pstmt.setInt(1, orderId);
                pstmt.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                dbFacade.getConnection().rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            dbFacade.closeConnection();
        }
        return transactionId;
    }

    public List<Order> getOrderDetails(int orderId) {
        List<Order> orders = new ArrayList<>();
        try {
            dbFacade.openConnection();
            Connection conn = dbFacade.getConnection();
            String query = "SELECT io.menu_id, io.quantity, m.menu_name, m.menu_price " +
                    "FROM dishes_order io " +
                    "JOIN menus m ON io.menu_id = m.menu_id " +
                    "WHERE io.order_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, orderId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        int menuId = rs.getInt("menu_id");
                        int quantity = rs.getInt("quantity");
                        String dishName = rs.getString("menu_name");
                        double dishPrice = rs.getDouble("menu_price");

                        Order order = new Order(menuId, dishName, dishPrice);
                        order.setQuantity(quantity);
                        orders.add(order);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbFacade.closeConnection();
        }
        return orders;
    }
}
