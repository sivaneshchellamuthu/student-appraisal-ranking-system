package service;

import dao.DataStore;
import exception.AppraisalException;
import model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

/**
 * Authentication and password hashing.
 * Passwords are never stored in plain text; only their SHA-256 hash is saved.
 */
public class AuthService {

    private final DataStore dataStore;

    public AuthService() {
        this(new DataStore());
    }

    public AuthService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Validates a username/password pair and returns the logged-in user.
     */
    public User login(String username, String password) throws AppraisalException {
        if (username == null || username.trim().isEmpty()) {
            throw new AppraisalException("Please enter your username.");
        }
        if (password == null || password.isEmpty()) {
            throw new AppraisalException("Please enter your password.");
        }

        try {
            User user = dataStore.findUserByUsername(username.trim());
            if (user == null) {
                throw new AppraisalException("Invalid username or password.");
            }
            if (!user.isActive()) {
                throw new AppraisalException("This account has been disabled. "
                        + "Please contact the administrator.");
            }
            String enteredHash = hashPassword(password);
            String storedHash = user.getPasswordHash() == null
                    ? "" : user.getPasswordHash().toLowerCase();
            if (!MessageDigest.isEqual(
                    enteredHash.getBytes(StandardCharsets.UTF_8),
                    storedHash.getBytes(StandardCharsets.UTF_8))) {
                throw new AppraisalException("Invalid username or password.");
            }
            return user;
        } catch (SQLException ex) {
            throw new AppraisalException("Database error during login: "
                    + ex.getMessage(), ex);
        }
    }

    /** Quick database reachability check for the login screen. */
    public boolean testConnection() {
        return dataStore.testConnection();
    }

    /**
     * Returns the lowercase hexadecimal SHA-256 digest of a password.
     */
    public static String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }
}
