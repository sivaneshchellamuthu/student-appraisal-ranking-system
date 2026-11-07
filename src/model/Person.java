package model;

/**
 * Base entity for every human in the system.
 *
 * OOP CONCEPT - ABSTRACTION:
 *   Person represents the common idea of a person and cannot be instantiated.
 *
 * OOP CONCEPT - INHERITANCE:
 *   Student and User both extend this class and reuse the common fields.
 *
 * OOP CONCEPT - ENCAPSULATION:
 *   Fields are private and exposed only through getters/setters.
 */
public abstract class Person {

    private int id;
    private String name;
    private String email;
    private String phone;

    protected Person() {
        this(0, "", "", "");
    }

    protected Person(int id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    /**
     * OOP CONCEPT - POLYMORPHISM:
     *   Student and User provide their own version of getDetails().
     */
    public abstract String getDetails();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return name == null ? "" : name;
    }
}
