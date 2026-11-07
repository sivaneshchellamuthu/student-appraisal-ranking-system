package model;

import java.util.List;

/**
 * Central place for the grade scale and pass rules.
 *
 * Keeping the rules here guarantees that the UI, the ranking service and the
 * report service all produce exactly the same grade for the same marks.
 *
 * Grade scale:
 *   O  >= 90
 *   A+ >= 80
 *   A  >= 70
 *   B+ >= 60
 *   B  >= 50
 *   C  >= 40
 *   F  <  40
 *
 * A student passes only when the overall percentage is at least 40 AND no
 * individual subject mark is below 40.
 */
public final class GradeCalculator {

    public static final double PASS_MARK = 40.0;

    private GradeCalculator() {
        // utility class
    }

    public static String gradeFor(double percentage) {
        if (percentage >= 90.0) {
            return "O";
        } else if (percentage >= 80.0) {
            return "A+";
        } else if (percentage >= 70.0) {
            return "A";
        } else if (percentage >= 60.0) {
            return "B+";
        } else if (percentage >= 50.0) {
            return "B";
        } else if (percentage >= 40.0) {
            return "C";
        }
        return "F";
    }

    public static String performanceFor(double percentage) {
        if (percentage >= 90.0) {
            return "Outstanding";
        } else if (percentage >= 80.0) {
            return "Excellent";
        } else if (percentage >= 70.0) {
            return "Very Good";
        } else if (percentage >= 60.0) {
            return "Good";
        } else if (percentage >= 50.0) {
            return "Average";
        } else if (percentage >= 40.0) {
            return "Satisfactory";
        }
        return "Needs Improvement";
    }

    public static boolean isPassed(double percentage, List<Mark> marks) {
        if (marks == null || marks.isEmpty()) {
            return false;
        }
        if (percentage < PASS_MARK) {
            return false;
        }
        for (Mark mark : marks) {
            if (mark.getMarksObtained() < PASS_MARK) {
                return false;
            }
        }
        return true;
    }
}
