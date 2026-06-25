/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.model;

public class Store {
    private int storeID;
    private String name;
    private String location;
    private int availableMachines; // Add this line

    // Existing getters/setters...
    public int getStoreID() { return storeID; }
    public void setStoreID(int storeID) { this.storeID = storeID; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    // Add these two methods below:
    public int getAvailableMachines() { 
        return availableMachines; 
    }
    public void setAvailableMachines(int availableMachines) {
        this.availableMachines = availableMachines;
    }
}