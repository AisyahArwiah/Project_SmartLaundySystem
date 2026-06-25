package com.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.util.DBConnection;

@WebServlet("/submitReview")
public class SubmitReviewServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String storeIdParam = request.getParameter("storeID");
        String ratingParam = request.getParameter("rating");
        String commentParam = request.getParameter("comment");
        
        if (storeIdParam != null && ratingParam != null && commentParam != null) {
            try {
                int storeID = Integer.parseInt(storeIdParam.trim());
                int rating = Integer.parseInt(ratingParam.trim());
                String comment = commentParam.trim();
                
                // Grab the real account name directly from the active user session!
                HttpSession session = request.getSession(false);
                String username = "Customer"; // Fallback default
                
                if (session != null && session.getAttribute("userName") != null) {
                    username = (String) session.getAttribute("userName");
                }
                
                // MATCHED: Table name set back to "reviews"
                String sql = "INSERT INTO reviews (store_id, rating, comment, username) VALUES (?, ?, ?, ?)";
                
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    
                    ps.setInt(1, storeID);
                    ps.setInt(2, rating);
                    ps.setString(3, comment);
                    ps.setString(4, username); // Binds "Elsa Fadlin" cleanly!
                    ps.executeUpdate();
                }
                
                response.sendRedirect("store?id=" + storeID);
                return;
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        response.sendRedirect("dashboard");
    }
}