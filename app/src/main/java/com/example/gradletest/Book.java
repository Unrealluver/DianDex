package com.example.gradletest;

import androidx.annotation.NonNull;

public class Book {
    private String name ;
    private Double price ;

    public Book(String name, Double price){
        this.name = name ;
        this.price = price ;
    }

    @NonNull
    @Override
    public String toString() {
        return "name: " + this.name + " price: " + this.price ;
    }
}
