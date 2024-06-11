package com.example.orderapp.classes;

public class MenuItem {
    private final int menuId;
    private final String name;
    private final double price;

    public MenuItem(int menuId, String name, double price) {
        this.menuId = menuId;
        this.name = name;
        this.price = price;
    }

    public int getMenuId() {
        return menuId;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }
}
