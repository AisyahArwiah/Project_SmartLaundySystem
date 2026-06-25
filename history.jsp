/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.DAO;

import com.model.Store;
import com.model.Machine;
import com.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StoreDAO {

    public List<Store> getAllStores() throws SQLException {
        List<Store> list = new ArrayList<>();
        String query = "SELECT s.*, " +
                       "(SELECT COUNT(*) FROM machines m WHERE m.storeID = s.storeID AND m.machineStatus = 'Available') AS available_count " +
                       "FROM laundry_stores s";
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Store s = new Store();
                s.setStoreID(rs.getInt("storeID"));
                s.setName(rs.getString("name"));
                s.setLocation(rs.getString("location"));
                
                // NOTE: If this line is red, open com.model.Store and add the setAvailableMachines method!
                s.setAvailableMachines(rs.getInt("available_count"));
                
                list.add(s);
            }
        }
        return list;
    }

    public String getStoreNameById(int storeID) throws SQLException {
        String query = "SELECT name FROM laundry_stores WHERE storeID = ?";
        // FIXED: Included PreparedStatement inside the try-with-resources statement block
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, storeID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        }
        return "";
    }

    public List<Machine> getMachinesByStore(int storeID) throws SQLException {
        List<Machine> list = new ArrayList<>();
        String query = "SELECT * FROM machines WHERE storeID = ?";
        // FIXED: Included PreparedStatement inside the try-with-resources statement block
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, storeID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Machine m = new Machine();
                    m.setMachineID(rs.getString("machineID"));
                    m.setStoreID(rs.getInt("storeID"));
                    m.setMachineType(rs.getString("machineType"));
                    m.setMachineStatus(rs.getString("machineStatus"));
                    m.setTimer(rs.getInt("timer"));
                    list.add(m);
                }
            }
        }
        return list;
    }
}