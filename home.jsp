package com.DAO;

import com.model.Transaction;
import com.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    /**
     * STAGE 1: Saves the user's laundry attempt as 'Pending' right before they teleport to Toyyibpay
     */
    public void createPendingTransaction(Transaction txn) throws SQLException {
        String sql = "INSERT INTO transactions (transactionID, userID, machineID, amount, transactionStatus) VALUES (?, ?, ?, ?, 'Pending')";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txn.getTransactionID());
            ps.setInt(2, txn.getUserID());
            ps.setString(3, txn.getMachineID());
            ps.setDouble(4, txn.getAmount());
            ps.executeUpdate();
        }
    }

    /**
     * STAGE 2: Bounced back from Toyyibpay with Success status. Execute the 4-table update!
     */
    public boolean finalizeToyyibpaySuccess(String txnID, String toyyibBillCode) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false); 
            try {
                // 1. Double check the transaction exists and grab its data
                String checkSql = "SELECT userID, machineID, amount FROM transactions WHERE transactionID = ? AND transactionStatus = 'Pending'";
                PreparedStatement psCheck = conn.prepareStatement(checkSql);
                psCheck.setString(1, txnID);
                ResultSet rs = psCheck.executeQuery();
                
                if (!rs.next()) {
                    conn.rollback();
                    return false; // Transaction already resolved or fake ID
                }

                int userID = rs.getInt("userID");
                String machID = rs.getString("machineID");
                double amount = rs.getDouble("amount");

                // 2. Mark Transaction Completed
                PreparedStatement psTxn = conn.prepareStatement("UPDATE transactions SET transactionStatus = 'Completed' WHERE transactionID = ?");
                psTxn.setString(1, txnID);
                psTxn.executeUpdate();

                // 3. Lock physical washing machine
                PreparedStatement psMach = conn.prepareStatement("UPDATE machines SET machineStatus = 'In Use', timer = 20 WHERE machineID = ?");
                psMach.setString(1, machID);
                psMach.executeUpdate();

                // 4. Save to your friend's Payments Ledger
                PreparedStatement psPay = conn.prepareStatement("INSERT INTO payments (paymentID, transactionID, paymentMethod, paymentStatus) VALUES (?, ?, 'toyyibPay FPX', 'Success')");
                psPay.setString(1, "TP-" + toyyibBillCode);
                psPay.setString(2, txnID);
                psPay.executeUpdate();

                // 5. Grant Loyalty Points
                PreparedStatement psPts = conn.prepareStatement("UPDATE loyalty_accounts SET points = points + ? WHERE userID = ?");
                psPts.setInt(1, (int) Math.floor(amount));
                psPts.setInt(2, userID);
                psPts.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void markTransactionFailed(String txnID) throws SQLException {
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE transactions SET transactionStatus = 'Failed' WHERE transactionID = ?")) {
            ps.setString(1, txnID); ps.executeUpdate();
        }
    }

    public List<Transaction> getUserHistory(int userID) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM transactions WHERE userID = ? ORDER BY date DESC")) {
            ps.setInt(1, userID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Transaction t = new Transaction();
                t.setTransactionID(rs.getString("transactionID"));
                t.setUserID(rs.getInt("userID"));
                t.setMachineID(rs.getString("machineID"));
                t.setDate(rs.getTimestamp("date"));
                t.setAmount(rs.getDouble("amount"));
                t.setTransactionStatus(rs.getString("transactionStatus"));
                list.add(t);
            }
        }
        return list;
    }

    public ResultSet getActiveTimerDetails(int userID) throws SQLException {
        Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT t.machineID, m.timer, m.machineType, s.name FROM transactions t " +
            "JOIN machines m ON t.machineID = m.machineID " +
            "JOIN laundry_stores s ON m.storeID = s.storeID " +
            "WHERE t.userID = ? AND m.machineStatus = 'In Use' ORDER BY t.date DESC LIMIT 1"
        );
        ps.setInt(1, userID);
        return ps.executeQuery(); 
    }
    
}