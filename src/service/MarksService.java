package service;

import dao.DataStore;
import exception.AppraisalException;
import model.Mark;
import model.Student;
import model.Subject;
import model.User;

import java.sql.SQLException;
import java.util.List;

/**
 * Marks entry rules. Every mark is validated against the 0-100 requirement
 * and against the subject's own maximum marks.
 */
public class MarksService {

    private final DataStore dataStore;

    public MarksService() {
        this(new DataStore());
    }

    public MarksService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<Mark> getMarksForStudent(int studentId) throws AppraisalException {
        if (studentId <= 0) {
            throw new AppraisalException("Select a student first.");
        }
        try {
            return dataStore.getMarksByStudent(studentId);
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public void saveMark(Student student, Subject subject, double marksObtained,
                         User enteredBy) throws AppraisalException {
        if (student == null || student.getId() <= 0) {
            throw new AppraisalException("Select a student.");
        }
        if (subject == null || subject.getSubjectId() <= 0) {
            throw new AppraisalException("Select a subject.");
        }
        if (Double.isNaN(marksObtained) || Double.isInfinite(marksObtained)) {
            throw new AppraisalException("Enter a valid number for marks.");
        }
        if (marksObtained < 0.0 || marksObtained > 100.0) {
            throw new AppraisalException("Marks must be between 0 and 100.");
        }
        if (marksObtained > subject.getMaxMarks()) {
            throw new AppraisalException("Marks cannot exceed the subject maximum of "
                    + subject.getMaxMarks() + ".");
        }

        int enteredById = enteredBy == null ? 0 : enteredBy.getId();
        try {
            dataStore.upsertMark(student.getId(), subject.getSubjectId(),
                    marksObtained, enteredById);
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public void deleteMark(int markId) throws AppraisalException {
        if (markId <= 0) {
            throw new AppraisalException("Select a mark row to delete.");
        }
        try {
            dataStore.deleteMark(markId);
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public int countMarks() throws AppraisalException {
        try {
            return dataStore.countMarks();
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    private static AppraisalException databaseError(SQLException ex) {
        return new AppraisalException(
                "Database error while working with marks: " + ex.getMessage(), ex);
    }
}
