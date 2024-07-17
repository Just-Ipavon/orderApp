package com.example.orderapp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
//Classe per implementare il Facade sul DB
public class DatabaseFacade {
    private Connection connection;
    //Metodo per avviare la connessione
    public Connection openConnection() throws SQLException {
        connection = ConnectionFactory.createConnection();
        return connection;
    }
    //Metodo per chiudere la connessione
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    //getter della connessione
    public Connection getConnection() {
        return connection;
    }
    //Metodo per ritornare i risultati dalla query (con/senza parametri)
    public ResultSet executeQuery(String query) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(query);
        return stmt.executeQuery();
    }
    public ResultSet executeQuery(String query, int param) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setInt(1, param);
        return stmt.executeQuery();
    }
    //Metodo per eseguire l'update su oggetti
    public void executeUpdate(String query, Object... params) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(query);
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        stmt.executeUpdate();
    }
}