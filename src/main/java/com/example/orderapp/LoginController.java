package com.example.orderapp;
import java.sql.Connection;
import java.sql.SQLException;

import com.example.orderapp.classes.AuthStrategy;
import com.example.orderapp.classes.UserSession;
//Classe per il controllo del Login
public class LoginController {
    private AuthStrategy authStrategy;//Pattern Strategy
    private DatabaseFacade dbFacade;//Pattern Facade
    //Costruttore
    public LoginController(AuthStrategy authStrategy) {
        this.authStrategy = authStrategy;
        this.dbFacade = new DatabaseFacade();
    }
    //Metodo che valida il login, e ritorna la UserSession corrispondente
    public UserSession validateLogin(String userId, String password) {
        UserSession userSession = null;
        try {
            dbFacade.openConnection();
            Connection conn = dbFacade.getConnection();
            userSession = authStrategy.authenticate(userId, password);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbFacade.closeConnection();
        }
        return userSession;
    }
}