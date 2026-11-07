package model;

/**
 * One row in the ranking table.
 * Ranking is calculated at runtime and is never stored in the database.
 */
public class RankEntry {

    private Student student;
    private double total;
    private double percentage;
    private double attendance;
    private String grade;
    private boolean passed;
    private int rank;

    public RankEntry() {
    }

    public RankEntry(Student student, double total, double percentage,
                     double attendance, String grade, boolean passed) {
        this.student = student;
        this.total = total;
        this.percentage = percentage;
        this.attendance = attendance;
        this.grade = grade;
        this.passed = passed;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public double getAttendance() {
        return attendance;
    }

    public void setAttendance(double attendance) {
        this.attendance = attendance;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getResultText() {
        return passed ? "PASS" : "FAIL";
    }

    public String getRollNo() {
        return student == null ? "" : student.getRollNo();
    }

    public String getStudentName() {
        return student == null ? "" : student.getName();
    }

    public String getDepartment() {
        return student == null ? "" : student.getDepartment();
    }

    public int getYear() {
        return student == null ? 0 : student.getYear();
    }

    public String getSection() {
        return student == null ? "" : student.getSection();
    }

    @Override
    public String toString() {
        return rank + ". " + getRollNo() + " - " + getStudentName()
                + " (" + String.format("%.2f", percentage) + "%)";
    }
}
