package com.example.orderapp;

abstract class OrderDecorator implements OrderComponent {
    protected final OrderComponent decoratedOrder;

    public OrderDecorator(OrderComponent decoratedOrder) {
        this.decoratedOrder = decoratedOrder;
    }

    @Override
    public int getMenuId() {
        return decoratedOrder.getMenuId();
    }

    @Override
    public String getDishName() {
        return decoratedOrder.getDishName();
    }

    @Override
    public double getDishPrice() {
        return decoratedOrder.getDishPrice();
    }

    @Override
    public int getQuantity() {
        return decoratedOrder.getQuantity();
    }

    @Override
    public void incrementQuantity() {
        decoratedOrder.incrementQuantity();
    }

    @Override
    public void setQuantity(int quantity) {
        decoratedOrder.setQuantity(quantity);
    }

    @Override
    public double getTotalPrice() {
        return decoratedOrder.getTotalPrice();
    }

    @Override
    public String getNotes() {
        return decoratedOrder.getNotes();
    }

    @Override
    public void setNotes(String notes) {
        decoratedOrder.setNotes(notes);
    }
}
