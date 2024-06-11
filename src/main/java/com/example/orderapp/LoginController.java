package com.example.orderapp;

import com.example.orderapp.classes.UserSession;

import java.sql.Connection;
import java.sql.SQLException;

public class LoginController {
    private AuthStrategy authStrategy;
    private DatabaseFacade dbFacade;

    public LoginController(AuthStrategy authStrategy) {
        this.authStrategy = authStrategy;
        this.dbFacade = new DatabaseFacade();
    }

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
