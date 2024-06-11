package com.example.orderapp.classes;

import javafx.scene.shape.Rectangle;

public class Table {
    private final int tableId;
    private final int numberOfSeats;
    private Rectangle rectangle;

    public Table(int tableId, int numberOfSeats) {
        this.tableId = tableId;
        this.numberOfSeats = numberOfSeats;
    }

    public int getTableId() {
        return tableId;
    }

    public int getNumberOfSeats() {
        return numberOfSeats;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public void setRectangle(Rectangle rectangle) {
        this.rectangle = rectangle;
    }
}
