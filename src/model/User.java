package model;

/**
 * System login account.
 *
 * OOP CONCEPT - INHERITANCE: extends Person (full name, email, phone).
 * OOP CONCEPT - ENCAPSULATION: password hash and role are private.
 * OOP CONCEPT - POLYMORPHISM: overrides Person.getDetails().
 */
public class User extends Person {

    /** Allowed system roles. */
    public enum Role {
        ADMIN,
        FACULTY;

        public static Role fromDatabase(String value) {
            if (value == null) {
                return FACULTY;
            }
            try {
                return Role.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return FACULTY;
            }
        }

        @Override
        public String toString() {
            return name();
        }
    }

    private String username;
    private String passwordHash;
    private Role role;
    private boolean active;
    private String createdAt;

    public User() {
        super();
        this.role = Role.FACULTY;
        this.active = true;
    }

    public User(int id, String username, String passwordHash, String fullName,
                Role role, boolean active) {
        super(id, fullName, "", "");
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /**
     * OOP CONCEPT - POLYMORPHISM: overrides Person.getDetails().
     */
    @Override
    public String getDetails() {
        return String.format("User[%s, %s, %s]", username, getName(), role);
    }

    @Override
    public String toString() {
        return getName() + " (" + username + ")";
    }
}
