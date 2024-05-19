package com.example.orderapp;

public interface AuthStrategy {
    UserSession authenticate(String userId, String password);
}
