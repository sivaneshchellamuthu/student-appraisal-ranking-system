package model;

/**
 * OOP CONCEPT - INTERFACE / POLYMORPHISM:
 *   Any entity that can be graded implements this interface.
 *   Student implements it from marks; GradeResult implements it from a
 *   completed appraisal. The UI/service can therefore treat both objects
 *   through the same Gradable reference.
 */
public interface Gradable {

    double getTotal();

    double getPercentage();

    String getGrade();

    boolean isPassed();

    String getPerformance();
}
