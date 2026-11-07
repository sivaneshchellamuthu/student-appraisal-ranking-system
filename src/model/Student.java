package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Student entity.
 *
 * OOP CONCEPT - INHERITANCE: extends Person (name, email, phone).
 * OOP CONCEPT - ENCAPSULATION: all fields are private.
 * OOP CONCEPT - INTERFACE / POLYMORPHISM: implements Gradable and computes
 *   total, percentage, grade, pass/fail and performance from the marks list.
 */
public class Student extends Person implements Gradable {

    private String rollNo;
    private String department;
    private int year;
    private String section;
    private double attendance;
    private List<Mark> marks = new ArrayList<>();

    public Student() {
        super();
    }

    public Student(int id, String rollNo, String name, String department, int year,
                   String section, double attendance, String email, String phone) {
        super(id, name, email, phone);
        this.rollNo = rollNo;
        this.department = department;
        this.year = year;
        this.section = section;
        this.attendance = attendance;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public double getAttendance() {
        return attendance;
    }

    public void setAttendance(double attendance) {
        this.attendance = attendance;
    }

    public List<Mark> getMarks() {
        return marks;
    }

    public void setMarks(List<Mark> marks) {
        this.marks = marks == null ? new ArrayList<>() : marks;
    }

    public void addMark(Mark mark) {
        marks.add(mark);
    }

    public void clearMarks() {
        marks.clear();
    }

    /** Maximum marks obtainable across all recorded subjects. */
    public double getMaxTotal() {
        double total = 0.0;
        for (Mark mark : marks) {
            total += mark.getMaxMarks();
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Gradable implementation (polymorphic behaviour)
    // ------------------------------------------------------------------

    @Override
    public double getTotal() {
        double total = 0.0;
        for (Mark mark : marks) {
            total += mark.getMarksObtained();
        }
        return total;
    }

    @Override
    public double getPercentage() {
        double max = getMaxTotal();
        if (max <= 0.0) {
            return 0.0;
        }
        return (getTotal() / max) * 100.0;
    }

    @Override
    public String getGrade() {
        return GradeCalculator.gradeFor(getPercentage());
    }

    @Override
    public boolean isPassed() {
        return GradeCalculator.isPassed(getPercentage(), marks);
    }

    @Override
    public String getPerformance() {
        return GradeCalculator.performanceFor(getPercentage());
    }

    /**
     * Convenience factory used by the service layer.
     */
    public GradeResult calculateGrade() {
        return new GradeResult(getTotal(), getMaxTotal(), getPercentage(),
                getGrade(), isPassed(), getPerformance(), marks);
    }

    /**
     * OOP CONCEPT - POLYMORPHISM: overrides Person.getDetails().
     */
    @Override
    public String getDetails() {
        return String.format("Student[%s, %s, %s Year %d - %s]",
                rollNo, getName(), department, year, section);
    }

    @Override
    public String toString() {
        return rollNo + " - " + getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Student other)) {
            return false;
        }
        return rollNo != null && rollNo.equalsIgnoreCase(other.rollNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rollNo == null ? "" : rollNo.toLowerCase());
    }
}
