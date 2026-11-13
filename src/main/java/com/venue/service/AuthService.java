package com.venue.service;
import com.venue.dao.UserDAO;
import com.venue.model.User;
import com.venue.util.PasswordUtil;

public class AuthService {
    private UserDAO userDAO = new UserDAO();
    public User login(String username, String password) {
        return userDAO.authenticate(username, PasswordUtil.hashPassword(password));
    }
}
