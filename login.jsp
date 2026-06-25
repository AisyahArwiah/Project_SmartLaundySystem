/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.model;

public class Machine {
    // Constants
    public static final String STATUS_FREE = "Available";
    public static final String STATUS_BUSY = "In Use";
    public static final String STATUS_MAINTENANCE = "Under Maintenance";

    private String machineID;
    private int storeID;
    private String machineType;
    private String machineStatus;
    private int timer;

    // --- FIX YOUR GETTERS AND SETTERS HERE ---

    public String getMachineID() {
        return machineID;
    }

    public void setMachineID(String machineID) {
        // REMOVE: throw new UnsupportedOperationException("Not supported yet.");
        this.machineID = machineID; // CHANGE TO THIS
    }

    public int getStoreID() {
        return storeID;
    }

    public void setStoreID(int storeID) {
        this.storeID = storeID;
    }

    public String getMachineType() {
        return machineType;
    }

    public void setMachineType(String machineType) {
        this.machineType = machineType;
    }

    public String getMachineStatus() {
        return machineStatus;
    }

    public void setMachineStatus(String machineStatus) {
        this.machineStatus = machineStatus;
    }

    public int getTimer() {
        return timer;
    }

    public void setTimer(int timer) {
        this.timer = timer;
    }
}