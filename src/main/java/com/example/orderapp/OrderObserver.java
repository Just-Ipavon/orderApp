package com.example.orderapp;

public interface OrderObserver {
    void onOrderStatusChanged(int orderId, boolean delivered);
    void onOrderDeleted(int orderId);
    void onDishDeleted(int orderId, int menuId);
    void onPaymentProcessed(int orderId, int transactionId, String paymentMethod, double amountReceived);
}
