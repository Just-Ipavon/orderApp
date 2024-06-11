package com.example.orderapp.classes;

public class UserSession {
    private static UserSession instance;

    private String username;
    private boolean isAdmin;

    private UserSession(String username, boolean isAdmin) {
        this.username = username;
        this.isAdmin = isAdmin;
    }

    public static UserSession getInstance(String username, Boolean isAdmin) {
        instance = new UserSession(username, isAdmin);
        return instance;
    }

    public static UserSession getInstance() {
        return instance;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void cleanUserSession() {
        username = "";
        isAdmin = false;
        instance = null;
    }

    @Override
    public String toString() {
        return "UserSession{" +
                "username='" + username + '\'' +
                ", isAdmin=" + isAdmin +
                '}';
    }
}
