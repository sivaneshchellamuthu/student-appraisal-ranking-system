package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable appraisal result produced from a student's marks.
 *
 * OOP CONCEPT - INTERFACE / POLYMORPHISM:
 *   GradeResult also implements Gradable, so both a Student (source data) and
 *   a GradeResult (calculated data) can be handled through a Gradable variable.
 */
public class GradeResult implements Gradable {

    private final double total;
    private final double maxTotal;
    private final double percentage;
    private final String grade;
    private final boolean passed;
    private final String performance;
    private final List<Mark> marks;

    public GradeResult(double total, double maxTotal, double percentage,
                       String grade, boolean passed, String performance,
                       List<Mark> marks) {
        this.total = total;
        this.maxTotal = maxTotal;
        this.percentage = percentage;
        this.grade = grade;
        this.passed = passed;
        this.performance = performance;
        this.marks = marks == null
                ? new ArrayList<>()
                : new ArrayList<>(marks);
    }

    @Override
    public double getTotal() {
        return total;
    }

    public double getMaxTotal() {
        return maxTotal;
    }

    @Override
    public double getPercentage() {
        return percentage;
    }

    @Override
    public String getGrade() {
        return grade;
    }

    @Override
    public boolean isPassed() {
        return passed;
    }

    @Override
    public String getPerformance() {
        return performance;
    }

    public List<Mark> getMarks() {
        return Collections.unmodifiableList(marks);
    }

    public int getSubjectCount() {
        return marks.size();
    }

    public String getResultText() {
        return passed ? "PASS" : "FAIL";
    }

    @Override
    public String toString() {
        return String.format("Total %.2f/%.2f, %.2f%%, Grade %s (%s)",
                total, maxTotal, percentage, grade, getResultText());
    }
}
