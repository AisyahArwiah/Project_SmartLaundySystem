/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.controller;

import com.DAO.UserDAO;
import com.DAO.StoreDAO;
import com.model.User;
import com.model.Store;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet(urlPatterns = {"/register", "/login", "/logout", "/dashboard"})
public class UserServlet extends HttpServlet {
    private UserDAO userDAO = new UserDAO();
    private StoreDAO storeDAO = new StoreDAO();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String path = request.getServletPath();

        if ("/register".equals(path)) {
            User user = new User();
            user.setName(request.getParameter("name"));
            user.setEmail(request.getParameter("email"));
            user.setPassword(request.getParameter("password"));
            user.setDob(Date.valueOf(request.getParameter("dob")));

            try {
                if (userDAO.registerUser(user)) {
                    response.sendRedirect("login.jsp?success=true");
                } else {
                    request.setAttribute("error", "Registration failed.");
                    request.getRequestDispatcher("register.jsp").forward(request, response);
                }
            } catch (SQLException e) {
                request.setAttribute("error", "Email conflicts exist: " + e.getMessage());
                request.getRequestDispatcher("register.jsp").forward(request, response);
            }
        } 
        
        else if ("/login".equals(path)) {
            String email = request.getParameter("email");
            String password = request.getParameter("password");

            try {
                User user = userDAO.validateLogin(email, password);
                if (user != null) {
                    HttpSession session = request.getSession();
                    session.setAttribute("userID", user.getUserID());
                    session.setAttribute("userName", user.getName());
                    response.sendRedirect("index.jsp");
                } else {
                    request.setAttribute("error", "Invalid structural account matches found.");
                    request.getRequestDispatcher("login.jsp").forward(request, response);
                }
            } catch (SQLException e) {
                throw new ServletException(e);
            }
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String path = request.getServletPath();

        if ("/logout".equals(path)) {
            request.getSession().invalidate();
            response.sendRedirect("login.jsp");
        } 
        
        else if ("/dashboard".equals(path)) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userID") == null) {
                response.sendRedirect("login.jsp");
                return;
            }

            int userID = (int) session.getAttribute("userID");
            try {
                int points = userDAO.getUserPoints(userID);
                List<Store> stores = storeDAO.getAllStores();

                request.setAttribute("points", points);
                request.setAttribute("storesList", stores);
                request.getRequestDispatcher("home.jsp").forward(request, response);
            } catch (SQLException e) {
                throw new ServletException(e);
            }
        }
        
        
    }
}