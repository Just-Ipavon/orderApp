package com.example.orderapp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.example.orderapp.classes.AuthStrategy;
import com.example.orderapp.classes.UserSession;
//Classe che implementa Strategy per il Login
public class DatabaseAuthStrategy implements AuthStrategy {
    @Override
    public UserSession authenticate(String userId, String password) {
        UserSession userSession = null; //Usersession inizializzata a null, acquisterà valore alla connessione
        DatabaseFacade dbFacade = new DatabaseFacade(); //Connessione al DB
        //Gestione eccezioni
        try {
            dbFacade.openConnection(); //Apro la connessione al DB
            Connection conn = dbFacade.getConnection();
            //Query al DB
            String query = "SELECT admin FROM user WHERE username = ? AND password = ?";
            try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
                preparedStatement.setString(1, userId);
                preparedStatement.setString(2, password);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    boolean isAdmin = resultSet.getBoolean("admin");
                    userSession = UserSession.getInstance(userId, isAdmin);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbFacade.closeConnection();
        }
        return userSession;
    }
}
