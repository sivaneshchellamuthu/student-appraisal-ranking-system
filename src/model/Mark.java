package model;

public class Mark {

    private final String regNo;
    private final String subject;
    private final double value;

    public Mark(String regNo, String subject, double value) {
        this.regNo = regNo;
        this.subject = subject;
        this.value = value;
    }

    public String getRegNo() {
        return regNo;
    }

    public String getSubject() {
        return subject;
    }

    public double getValue() {
        return value;
    }
}
