package com.controller;

import com.DAO.StoreDAO;
import com.DAO.TransactionDAO;
import com.model.Machine;
import com.model.Transaction;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;        
import java.sql.PreparedStatement;  
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;        
import java.util.HashMap;          
import java.util.Map;              
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet(urlPatterns = {"/store", "/checkout", "/paymentCallback", "/timer", "/history"})
public class LaundryOperationServlet extends HttpServlet {
    
    // =========================================================================
    // MATCHES YOUR TOMCAT WEB APP CONTEXT PATH NAME
    private static final String APP_BASE_URL = "http://localhost:8080/LaundrySmart";
    // =========================================================================

    private StoreDAO storeDAO = new StoreDAO();
    private TransactionDAO txnDAO = new TransactionDAO();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userID") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        String path = request.getServletPath();
        try {
            if ("/store".equals(path)) {
                int storeID = Integer.parseInt(request.getParameter("id"));
                request.setAttribute("storeName", storeDAO.getStoreNameById(storeID));
                request.setAttribute("machinesList", storeDAO.getMachinesByStore(storeID));
                
                // -------------------------------------------------------------
                // EXTRACT COMPILING REVIEW STREAM CONTEXT
                // -------------------------------------------------------------
                List<Map<String, Object>> reviewsList = new ArrayList<>();
                String reviewsSql = "SELECT rating, comment, username FROM reviews WHERE store_id = ? ORDER BY created_at DESC";
                
                try (Connection conn = com.util.DBConnection.getConnection();
                     PreparedStatement psReviews = conn.prepareStatement(reviewsSql)) {
                    
                    psReviews.setInt(1, storeID);
                    try (ResultSet rsReviews = psReviews.executeQuery()) {
                        while (rsReviews.next()) {
                            Map<String, Object> reviewMap = new HashMap<>();
                            reviewMap.put("rating", Integer.valueOf(rsReviews.getInt("rating")));
                            reviewMap.put("comment", rsReviews.getString("comment"));
                            reviewMap.put("username", rsReviews.getString("username"));
                            reviewsList.add(reviewMap);
                        }
                    }
                } catch (Exception ex) {
                    System.out.println("Could not load reviews feed layout: " + ex.getMessage());
                    ex.printStackTrace();
                }
                
                request.setAttribute("reviewsList", reviewsList);
                request.getRequestDispatcher("store.jsp").forward(request, response);
            } 
            else if ("/timer".equals(path)) {
                int userID = (int) session.getAttribute("userID");
                ResultSet rs = txnDAO.getActiveTimerDetails(userID);
                if (rs != null && rs.next()) {
                    request.setAttribute("activeTimer", true);
                    request.setAttribute("machID", rs.getString("machineID"));
                    request.setAttribute("machTimer", rs.getInt("timer"));
                    request.setAttribute("machType", rs.getString("machineType"));
                    request.setAttribute("storeName", rs.getString("name"));
                } else {
                    request.setAttribute("activeTimer", false);
                }
                request.getRequestDispatcher("timer.jsp").forward(request, response);
            } 
            else if ("/history".equals(path)) {
                request.setAttribute("historyList", txnDAO.getUserHistory((int) session.getAttribute("userID")));
                request.getRequestDispatcher("history.jsp").forward(request, response);
            }
            // =================================================================
            // TOYYIBPAY REDIRECT HANDLING:
            // =================================================================
            else if ("/paymentCallback".equals(path)) {
                String statusId = request.getParameter("status_id"); 
                String txnID = request.getParameter("order_id");
                String billCode = request.getParameter("billcode");

                if ("1".equals(statusId)) {
                    if (txnDAO.finalizeToyyibpaySuccess(txnID, billCode)) {
                        try {
                            int userID = (int) session.getAttribute("userID");
                            double amountSpent = 5.00; 
                            if (request.getParameter("amount") != null) {
                                amountSpent = Double.parseDouble(request.getParameter("amount"));
                            }
                            processUserPointsAndTiers(userID, amountSpent);
                        } catch (Exception ex) {
                            System.out.println("Loyalty generation bypassed: " + ex.getMessage());
                        }
                        response.sendRedirect("timer");
                    } else {
                        response.getWriter().write("CRITICAL ERROR: Bank cleared, but local DB failed to update.");
                    }
                } else {
                    // Wiped transaction trace clean on failure
                    txnDAO.markTransactionFailed(txnID);
                    
                    response.sendRedirect("store?id=1"); 
                }
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userID") == null) {
            response.sendRedirect("login.jsp");
            return;
        }
        
        String path = request.getServletPath();
        if ("/checkout".equals(path)) {
            String machineID = request.getParameter("machineID");
            double amount = Double.parseDouble(request.getParameter("amount"));
            int userID = (int) session.getAttribute("userID");
            String userName = (String) session.getAttribute("userName");
            String txnID = "TXN-" + (100000 + new Random().nextInt(900000));

            try {
                Transaction txn = new Transaction();
                txn.setTransactionID(txnID);
                txn.setUserID(userID);
                txn.setMachineID(machineID);
                txn.setAmount(amount);
                txnDAO.createPendingTransaction(txn);

                String billCode = requestToyyibpayBillCode(txnID, amount, userName, machineID);

                if (!billCode.isEmpty()) {
                    response.sendRedirect("https://dev.toyyibpay.com/" + billCode);
                } else {
                    response.getWriter().write("Failed to contact ToyyibPay API Server.");
                }
            } catch (SQLException e) {
                throw new ServletException(e);
            }
        }
        // =========================================================================
        // FIXED: Catch inbound ToyyibPay webhook POST triggers smoothly
        // =========================================================================
        else if ("/paymentCallback".equals(path)) {
            doGet(request, response);
        }
    }

    private String requestToyyibpayBillCode(String txnID, double amount, String custName, String machID) {
        try {
            String secretKey = "01z0lib1-aqee-hrkt-cddh-y8myc1xeyey4";
            String categoryCode = "w48e8eqw";
            int amountInCents = (int) Math.round(amount * 100); 

            URL url = new URL("https://dev.toyyibpay.com/index.php/api/createBill");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            StringBuilder post = new StringBuilder();
            post.append("userSecretKey=").append(secretKey);
            post.append("&categoryCode=").append(categoryCode);
            post.append("&billName=").append(URLEncoder.encode("CleanCo Unit: " + machID, "UTF-8"));
            post.append("&billDescription=").append(URLEncoder.encode("Instant Laundry Activation", "UTF-8"));
            post.append("&billPriceSetting=1");
            post.append("&billPayorInfo=1");
            post.append("&billAmount=").append(amountInCents);
            post.append("&billReturnUrl=").append(URLEncoder.encode(APP_BASE_URL + "/paymentCallback", "UTF-8"));
            post.append("&billExternalReferenceNo=").append(URLEncoder.encode(txnID, "UTF-8"));
            post.append("&billTo=").append(URLEncoder.encode(custName, "UTF-8"));
            post.append("&billEmail=customer@cleanco.com"); 
            post.append("&billPhone=0198887766");

            OutputStream os = conn.getOutputStream();
            os.write(post.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder res = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) res.append(line);
            br.close();

            String json = res.toString();
            if (json.contains("BillCode")) {
                return json.split("\"BillCode\":\"")[1].split("\"")[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void processUserPointsAndTiers(int userID, double amountSpent) {
        String selectQuery = "SELECT points, membership_tier FROM users WHERE userID = ?";
        String updateQuery = "UPDATE users SET points = ?, membership_tier = ? WHERE userID = ?";
        
        try (Connection conn = com.util.DBConnection.getConnection();
             PreparedStatement psSelect = conn.prepareStatement(selectQuery)) {
            
            psSelect.setInt(1, userID);
            try (ResultSet rs = psSelect.executeQuery()) {
                if (rs.next()) {
                    int currentPoints = rs.getInt("points");
                    String currentTier = rs.getString("membership_tier");
                    if (currentTier == null) currentTier = "Bronze";
                    
                    double multiplier = 1.0;
                    if ("Silver".equalsIgnoreCase(currentTier)) {
                        multiplier = 1.5;
                    } else if ("Gold".equalsIgnoreCase(currentTier)) {
                        multiplier = 2.0;
                    }
                    
                    int pointsEarned = (int) Math.round(amountSpent * multiplier);
                    int totalPoints = currentPoints + pointsEarned;
                    
                    String finalTier = "Bronze";
                    if (totalPoints >= 500) {
                        finalTier = "Gold";
                    } else if (totalPoints >= 150) {
                        finalTier = "Silver";
                    }
                    
                    try (PreparedStatement psUpdate = conn.prepareStatement(updateQuery)) {
                        psUpdate.setInt(1, totalPoints);
                        psUpdate.setString(2, finalTier);
                        psUpdate.setInt(3, userID);
                        psUpdate.executeUpdate();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error calculating membership allocation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}