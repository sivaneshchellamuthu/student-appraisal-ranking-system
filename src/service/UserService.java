package service;

import dao.DataStore;
import exception.AppraisalException;
import model.User;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

/**
 * User management rules. Every operation requires an ADMIN account, giving a
 * second line of defence behind the hidden Users tab in the UI.
 */
public class UserService {

    private final DataStore dataStore;

    public UserService() {
        this(new DataStore());
    }

    public UserService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<User> getAllUsers(User actingUser) throws AppraisalException {
        requireAdmin(actingUser);
        try {
            return dataStore.getAllUsers();
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public void createUser(User actingUser, String username, String fullName,
                           String role, String password, boolean active)
            throws AppraisalException {
        requireAdmin(actingUser);
        String cleanUsername = validateUsername(username);
        String cleanName = validateFullName(fullName);
        User.Role userRole = parseRole(role);
        String cleanPassword = validatePassword(password);

        User user = new User();
        user.setUsername(cleanUsername);
        user.setName(cleanName);
        user.setRole(userRole);
        user.setActive(active);
        user.setPasswordHash(AuthService.hashPassword(cleanPassword));

        try {
            dataStore.insertUser(user);
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new AppraisalException("Username '" + cleanUsername
                    + "' already exists.", ex);
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    /**
     * Updates a user. An empty newPassword keeps the existing password.
     */
    public void updateUser(User actingUser, int userId, String username,
                           String fullName, String role, String newPassword,
                           boolean active) throws AppraisalException {
        requireAdmin(actingUser);
        if (userId <= 0) {
            throw new AppraisalException("Select a user to update.");
        }
        String cleanUsername = validateUsername(username);
        String cleanName = validateFullName(fullName);
        User.Role userRole = parseRole(role);
        boolean changePassword = newPassword != null && !newPassword.isEmpty();
        String cleanPassword = changePassword ? validatePassword(newPassword) : null;

        try {
            User existing = dataStore.findUserById(userId);
            if (existing == null) {
                throw new AppraisalException("User not found.");
            }
            if (userId == actingUser.getId() && userRole != User.Role.ADMIN) {
                throw new AppraisalException(
                        "You cannot remove your own ADMIN role.");
            }
            if (existing.isAdmin() && userRole != User.Role.ADMIN
                    && dataStore.countUsersByRole(User.Role.ADMIN) <= 1) {
                throw new AppraisalException(
                        "The system must keep at least one ADMIN account.");
            }
            if (existing.isAdmin() && !active
                    && dataStore.countUsersByRole(User.Role.ADMIN) <= 1) {
                throw new AppraisalException(
                        "The last ADMIN account cannot be disabled.");
            }

            existing.setUsername(cleanUsername);
            existing.setName(cleanName);
            existing.setRole(userRole);
            existing.setActive(active);
            if (changePassword) {
                existing.setPasswordHash(AuthService.hashPassword(cleanPassword));
            }
            dataStore.updateUser(existing);
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new AppraisalException("Username '" + cleanUsername
                    + "' is already used by another account.", ex);
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public void deleteUser(User actingUser, int userId) throws AppraisalException {
        requireAdmin(actingUser);
        if (userId <= 0) {
            throw new AppraisalException("Select a user to delete.");
        }
        if (userId == actingUser.getId()) {
            throw new AppraisalException("You cannot delete your own account.");
        }
        try {
            User existing = dataStore.findUserById(userId);
            if (existing == null) {
                throw new AppraisalException("User not found.");
            }
            if (existing.isAdmin()
                    && dataStore.countUsersByRole(User.Role.ADMIN) <= 1) {
                throw new AppraisalException(
                        "The last ADMIN account cannot be deleted.");
            }
            dataStore.deleteUser(userId);
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public int countUsers(User actingUser) throws AppraisalException {
        requireAdmin(actingUser);
        try {
            return dataStore.countUsers();
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    private void requireAdmin(User actingUser) throws AppraisalException {
        if (actingUser == null || !actingUser.isAdmin()) {
            throw new AppraisalException(
                    "Access denied. Only ADMIN users can manage accounts.");
        }
    }

    private String validateUsername(String username) throws AppraisalException {
        String value = username == null ? "" : username.trim();
        if (!value.matches("[A-Za-z0-9._-]{3,50}")) {
            throw new AppraisalException("Username must be 3-50 characters and "
                    + "may contain letters, numbers, dot, underscore or hyphen.");
        }
        return value;
    }

    private String validateFullName(String fullName) throws AppraisalException {
        String value = fullName == null ? "" : fullName.trim();
        if (value.isEmpty()) {
            throw new AppraisalException("Full name is required.");
        }
        if (value.length() > 100) {
            throw new AppraisalException("Full name cannot exceed 100 characters.");
        }
        return value;
    }

    private String validatePassword(String password) throws AppraisalException {
        if (password == null || password.length() < 6) {
            throw new AppraisalException("Password must be at least 6 characters.");
        }
        return password;
    }

    private User.Role parseRole(String role) throws AppraisalException {
        if (role == null) {
            throw new AppraisalException("Select a role.");
        }
        try {
            return User.Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AppraisalException("Role must be ADMIN or FACULTY.");
        }
    }

    private static AppraisalException databaseError(SQLException ex) {
        return new AppraisalException(
                "Database error while working with users: " + ex.getMessage(), ex);
    }
}
