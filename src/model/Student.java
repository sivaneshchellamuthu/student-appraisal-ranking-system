package model;

public class Student {

    private final String regNo;
    private final String name;

    public Student(String regNo, String name) {
        this.regNo = regNo;
        this.name = name;
    }

    public String getRegNo() {
        return regNo;
    }

    public String getName() {
        return name;
    }
}
