package com.example.orderapp.classes;

public class Order implements OrderComponent {
    private final int menuId;
    private final String dishName;
    private final double dishPrice;
    private int quantity;
    private String notes;

    public Order(int menuId, String dishName, double dishPrice) {
        this.menuId = menuId;
        this.dishName = dishName;
        this.dishPrice = dishPrice;
        this.quantity = 1;
        this.notes = "";
    }

    @Override
    public String getDishName() {
        return dishName;
    }

    @Override
    public double getDishPrice() {
        return dishPrice;
    }

    @Override
    public int getMenuId() {
        return menuId;
    }

    @Override
    public int getQuantity() {
        return quantity;
    }

    @Override
    public void incrementQuantity() {
        this.quantity++;
    }

    @Override
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public double getTotalPrice() {
        return this.dishPrice * this.quantity;
    }

    @Override
    public String getNotes() {
        return notes;
    }

    @Override
    public void setNotes(String notes) {
        this.notes = notes;
    }

}
