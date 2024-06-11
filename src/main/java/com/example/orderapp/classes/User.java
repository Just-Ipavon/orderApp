package com.example.orderapp.classes;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class User {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty firstName;
    private final SimpleStringProperty lastName;
    private final SimpleStringProperty mobile;
    private final SimpleStringProperty email;
    private final SimpleStringProperty username;
    private final SimpleBooleanProperty admin;
    private final SimpleBooleanProperty waiter;

    public User(int id, String firstName, String lastName, String mobile, String email, String username, boolean admin, boolean waiter) {
        this.id = new SimpleIntegerProperty(id);
        this.firstName = new SimpleStringProperty(firstName);
        this.lastName = new SimpleStringProperty(lastName);
        this.mobile = new SimpleStringProperty(mobile);
        this.email = new SimpleStringProperty(email);
        this.username = new SimpleStringProperty(username);
        this.admin = new SimpleBooleanProperty(admin);
        this.waiter = new SimpleBooleanProperty(waiter);
    }

    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public String getFirstName() {
        return firstName.get();
    }

    public String getLastName() {
        return lastName.get();
    }

    public String getMobile() {
        return mobile.get();
    }

    public String getEmail() {
        return email.get();
    }

    public String getUsername() {
        return username.get();
    }

    public boolean isAdmin() {
        return admin.get();
    }

    public boolean isWaiter() {
        return waiter.get();
    }
}
