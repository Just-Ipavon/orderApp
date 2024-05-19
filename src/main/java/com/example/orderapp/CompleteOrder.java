package com.example.orderapp;

import java.util.ArrayList;
import java.util.List;

public class CompleteOrder {
    private final int orderId;
    private final int tableId;
    private boolean delivered;
    private boolean completed;
    private List<Order> dishes;

    public CompleteOrder(int orderId, int tableId) {
        this.orderId = orderId;
        this.tableId = tableId;
        this.delivered = false;
        this.completed = false;
        this.dishes = new ArrayList<>();
    }

    public int getOrderId() {
        return orderId;
    }

    public int getTableId() {
        return tableId;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public List<Order> getDishes() {
        return dishes;
    }

    public void addDish(Order order) {
        this.dishes.add(order);
    }

    public double getTotalPrice() {
        return dishes.stream().mapToDouble(Order::getTotalPrice).sum();
    }
}
