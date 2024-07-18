package com.example.orderapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

//Pattern Factory per gestire la connessione al DataBase (localhost, prima AWS)
public class ConnectionFactory {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/restaurant";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "562656";

    public static Connection createConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
