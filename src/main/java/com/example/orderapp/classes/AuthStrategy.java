package com.example.orderapp.classes;

public interface AuthStrategy {
    UserSession authenticate(String userId, String password);
}
