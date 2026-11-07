package service;

import dao.DataStore;
import exception.AppraisalException;
import model.Gradable;
import model.GradeResult;
import model.Mark;
import model.Student;

import java.sql.SQLException;
import java.util.List;

/**
 * Application logic for appraisal: loads the marks, applies the grade scale
 * and returns a GradeResult.
 */
public class AppraisalService {

    private final DataStore dataStore;

    public AppraisalService() {
        this(new DataStore());
    }

    public AppraisalService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public GradeResult appraise(Student student) throws AppraisalException {
        if (student == null || student.getId() <= 0) {
            throw new AppraisalException("Select a student first.");
        }
        try {
            List<Mark> marks = dataStore.getMarksByStudent(student.getId());
            student.setMarks(marks);
            return student.calculateGrade();
        } catch (SQLException ex) {
            throw new AppraisalException(
                    "Database error while calculating grades: " + ex.getMessage(), ex);
        }
    }

    public GradeResult appraise(int studentId) throws AppraisalException {
        if (studentId <= 0) {
            throw new AppraisalException("Select a student first.");
        }
        try {
            Student student = dataStore.getStudentById(studentId);
            if (student == null) {
                throw new AppraisalException("Student not found.");
            }
            return appraise(student);
        } catch (SQLException ex) {
            throw new AppraisalException(
                    "Database error while loading the student: " + ex.getMessage(), ex);
        }
    }

    /**
     * OOP CONCEPT - POLYMORPHISM:
     *   Accepts any Gradable (Student or GradeResult) and uses only the
     *   interface methods, so the caller does not need to know the concrete type.
     */
    public String describe(Gradable gradable) {
        if (gradable == null) {
            return "No appraisal data.";
        }
        return String.format("Total %.2f | %.2f%% | Grade %s | %s | %s",
                gradable.getTotal(), gradable.getPercentage(), gradable.getGrade(),
                gradable.isPassed() ? "PASS" : "FAIL", gradable.getPerformance());
    }
}
