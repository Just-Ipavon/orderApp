package com.example.orderapp.classes;

public interface OrderComponent {
    int getMenuId();
    String getDishName();
    double getDishPrice();
    int getQuantity();
    void incrementQuantity();
    void setQuantity(int quantity);
    double getTotalPrice();
    String getNotes();
    void setNotes(String notes);
}
