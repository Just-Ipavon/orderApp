package com.example.orderapp;

import com.example.orderapp.classes.UserSession;

public interface AuthStrategy {
    UserSession authenticate(String userId, String password);
}
