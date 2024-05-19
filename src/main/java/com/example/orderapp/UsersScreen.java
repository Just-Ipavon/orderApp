package com.example.orderapp;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsersScreen extends Stage {

    private DatabaseFacade dbFacade;

    public UsersScreen() {
        this.dbFacade = new DatabaseFacade();
        setTitle("Gestione Utenti");

        TableView<User> tableView = new TableView<>();
        ObservableList<User> users = FXCollections.observableArrayList();

        // Creazione delle colonne della tabella
        TableColumn<User, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<User, String> firstNameColumn = new TableColumn<>("Nome");
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));

        TableColumn<User, String> lastNameColumn = new TableColumn<>("Cognome");
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));

        TableColumn<User, String> mobileColumn = new TableColumn<>("Mobile");
        mobileColumn.setCellValueFactory(new PropertyValueFactory<>("mobile"));

        TableColumn<User, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<User, String> usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<User, Boolean> adminColumn = new TableColumn<>("Admin");
        adminColumn.setCellValueFactory(new PropertyValueFactory<>("admin"));

        // Aggiunta delle colonne alla tabella
        tableView.getColumns().addAll(idColumn, firstNameColumn, lastNameColumn, mobileColumn, emailColumn, usernameColumn, adminColumn);

        // Caricamento dei dati degli utenti dalla tabella 'user'
        try {
            dbFacade.openConnection();
            Connection conn = dbFacade.getConnection();
            String query = "SELECT id, firstname, lastname, mobile, email, username, admin FROM user";
            try (PreparedStatement pstmt = conn.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String firstName = rs.getString("firstname");
                    String lastName = rs.getString("lastname");
                    String mobile = rs.getString("mobile");
                    String email = rs.getString("email");
                    String username = rs.getString("username");
                    boolean admin = rs.getBoolean("admin");

                    users.add(new User(id, firstName, lastName, mobile, email, username, admin));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbFacade.closeConnection();
        }

        // Aggiunta dei dati alla tabella
        tableView.setItems(users);

        VBox layout = new VBox(tableView);
        Scene scene = new Scene(layout, 800, 600);
        setScene(scene);
    }

    public static class User {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty firstName;
        private final SimpleStringProperty lastName;
        private final SimpleStringProperty mobile;
        private final SimpleStringProperty email;
        private final SimpleStringProperty username;
        private final SimpleBooleanProperty admin;

        public User(int id, String firstName, String lastName, String mobile, String email, String username, boolean admin) {
            this.id = new SimpleIntegerProperty(id);
            this.firstName = new SimpleStringProperty(firstName);
            this.lastName = new SimpleStringProperty(lastName);
            this.mobile = new SimpleStringProperty(mobile);
            this.email = new SimpleStringProperty(email);
            this.username = new SimpleStringProperty(username);
            this.admin = new SimpleBooleanProperty(admin);
        }

        public int getId() {
            return id.get();
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
    }
}
