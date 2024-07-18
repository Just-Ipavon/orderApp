package com.example.orderapp.classes;
//Con un tipo record possiamo rappresentare contenitori di dati immutabili, senza creare una classe appositamente.
public record MenuItem(int menuId, String name, double price) {
}
