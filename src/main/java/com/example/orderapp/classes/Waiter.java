package com.example.orderapp.classes;

public class Waiter {
    private final int userId;
    private final String firstName;
    private final String lastName;

    public Waiter(int userId, String firstName, String lastName) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public int getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
