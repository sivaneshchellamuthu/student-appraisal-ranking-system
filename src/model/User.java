package model;

public class User {

    public enum Role {
        ADMIN,
        FACULTY
    }

    private final String username;
    private final String passwordHash;
    private final String salt;
    private final Role role;
    private final String displayName;

    public User(String username, String passwordHash, String salt, Role role, String displayName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
        this.displayName = displayName;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public Role getRole() {
        return role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String roleLabel() {
        return role == Role.ADMIN ? "Admin" : "Faculty";
    }
}
