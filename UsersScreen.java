package com.example.orderapp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.example.orderapp.classes.User;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
//Classe per gestire la schermata utente
public class UsersScreen extends Stage {
    private DatabaseFacade dbFacade; //Facade per connessione al DB
    private boolean currentUserIsAdmin; //Flag

    public UsersScreen(boolean currentUserIsAdmin) {
        this.dbFacade = new DatabaseFacade();
        this.currentUserIsAdmin = currentUserIsAdmin;
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
        TableColumn<User, Boolean> waiterColumn = new TableColumn<>("Waiter");
        waiterColumn.setCellValueFactory(new PropertyValueFactory<>("waiter"));
        // Aggiunta delle colonne alla tabella
        tableView.getColumns().addAll(idColumn, firstNameColumn, lastNameColumn, mobileColumn, emailColumn, usernameColumn, adminColumn, waiterColumn);
        // Caricamento dei dati degli utenti dalla tabella 'user'
        try {
            dbFacade.openConnection();
            Connection conn = dbFacade.getConnection();
            String query = "SELECT id, firstname, lastname, mobile, email, username, admin, waiter FROM user"; //Query al DB
            try (PreparedStatement pstmt = conn.prepareStatement(query);
                ResultSet rs = pstmt.executeQuery()) {
                //Itero sulle tuple risultati
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String firstName = rs.getString("firstname");
                    String lastName = rs.getString("lastname");
                    String mobile = rs.getString("mobile");
                    String email = rs.getString("email");
                    String username = rs.getString("username");
                    boolean admin = rs.getBoolean("admin");
                    boolean waiter = rs.getBoolean("waiter");
                    users.add(new User(id, firstName, lastName, mobile, email, username, admin, waiter));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbFacade.closeConnection();
        }
        // Aggiunta dei dati alla tabella
        tableView.setItems(users);
        VBox layout = new VBox();
        // Pulsante per aggiungere un nuovo utente, visibile solo agli admin
        if (currentUserIsAdmin) {
            Button addButton = new Button("Aggiungi Utente");
            addButton.setOnAction(event -> showAddUserDialog(users));
            layout.getChildren().add(addButton);
        }
        layout.getChildren().add(tableView);
        Scene scene = new Scene(layout, 800, 600);
        setScene(scene);
    }
    //Dialog di aggiunta Utente
    private void showAddUserDialog(ObservableList<User> users) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Aggiungi Utente");
        //Button di aggiunta
        ButtonType addButtonType = new ButtonType("Aggiungi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        //Creazione dei campi
        TextField firstNameField = new TextField();
        firstNameField.setPromptText("Nome");
        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Cognome");
        TextField mobileField = new TextField();
        mobileField.setPromptText("Mobile");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        CheckBox adminCheckBox = new CheckBox("Admin");
        CheckBox waiterCheckBox = new CheckBox("Waiter");
        VBox content = new VBox();
        content.getChildren().addAll(firstNameField, lastNameField, mobileField, emailField, usernameField, adminCheckBox, waiterCheckBox);
        dialog.getDialogPane().setContent(content);
        //Conversione in oggetto Utente
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return new User(
                        0, //L'ID è dato dal DB
                        firstNameField.getText(),
                        lastNameField.getText(),
                        mobileField.getText(),
                        emailField.getText(),
                        usernameField.getText(),
                        adminCheckBox.isSelected(),
                        waiterCheckBox.isSelected()
                );
            }
            return null;
        });
        //Aggiunge l'Utente al DB
        dialog.showAndWait().ifPresent(user -> {
            try {
                dbFacade.openConnection();
                Connection conn = dbFacade.getConnection();
                //Query al DB
                String query = "INSERT INTO user (firstname, lastname, mobile, email, username, admin, waiter) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, user.getFirstName());
                    pstmt.setString(2, user.getLastName());
                    pstmt.setString(3, user.getMobile());
                    pstmt.setString(4, user.getEmail());
                    pstmt.setString(5, user.getUsername());
                    pstmt.setBoolean(6, user.isAdmin());
                    pstmt.setBoolean(7, user.isWaiter());
                    pstmt.executeUpdate();
                    //Ottiene il nuovo ID e inserisce i dati
                    ResultSet rs = pstmt.getGeneratedKeys();
                    if (rs.next()) {
                        user.setId(rs.getInt(1));
                    }
                    users.add(user);//Aggiunta effettiva
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                dbFacade.closeConnection();
            }
        });
    }
}