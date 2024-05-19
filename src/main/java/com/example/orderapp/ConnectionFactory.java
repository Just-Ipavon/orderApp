package com.example.orderapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    private static final String DB_URL = "jdbc:mysql://database-1.cfcoik2qycvy.eu-north-1.rds.amazonaws.com:3306/restaurant";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "56265626";

    public static Connection createConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
