package service;

import dao.DataStore;
import exception.AppraisalException;
import model.Subject;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

/**
 * Subject business rules.
 */
public class SubjectService {

    private final DataStore dataStore;

    public SubjectService() {
        this(new DataStore());
    }

    public SubjectService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<Subject> getAllSubjects() throws AppraisalException {
        try {
            return dataStore.getAllSubjects();
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public Subject getSubject(int subjectId) throws AppraisalException {
        try {
            Subject subject = dataStore.getSubjectById(subjectId);
            if (subject == null) {
                throw new AppraisalException("Subject not found.");
            }
            return subject;
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public void addSubject(Subject subject) throws AppraisalException {
        normalizeAndValidate(subject);
        try {
            dataStore.insertSubject(subject);
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new AppraisalException("Subject code '"
                    + subject.getSubjectCode() + "' already exists.", ex);
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public void updateSubject(Subject subject) throws AppraisalException {
        if (subject == null || subject.getSubjectId() <= 0) {
            throw new AppraisalException("Select a subject to update.");
        }
        normalizeAndValidate(subject);
        try {
            dataStore.updateSubject(subject);
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new AppraisalException("Subject code '"
                    + subject.getSubjectCode()
                    + "' is already used by another subject.", ex);
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public void deleteSubject(int subjectId) throws AppraisalException {
        if (subjectId <= 0) {
            throw new AppraisalException("Select a subject to delete.");
        }
        try {
            dataStore.deleteSubject(subjectId);
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public int countSubjects() throws AppraisalException {
        try {
            return dataStore.countSubjects();
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    private void normalizeAndValidate(Subject subject) throws AppraisalException {
        if (subject == null) {
            throw new AppraisalException("Subject details are missing.");
        }
        subject.setSubjectCode(trim(subject.getSubjectCode()).toUpperCase());
        subject.setSubjectName(trim(subject.getSubjectName()));
        subject.setDepartment(trim(subject.getDepartment()));

        if (subject.getSubjectCode().isEmpty()) {
            throw new AppraisalException("Subject code is required.");
        }
        if (subject.getSubjectCode().length() > 20) {
            throw new AppraisalException("Subject code cannot exceed 20 characters.");
        }
        if (subject.getSubjectName().isEmpty()) {
            throw new AppraisalException("Subject name is required.");
        }
        if (subject.getSubjectName().length() > 100) {
            throw new AppraisalException("Subject name cannot exceed 100 characters.");
        }
        if (subject.getDepartment().isEmpty()) {
            throw new AppraisalException("Department is required.");
        }
        if (subject.getDepartment().length() > 50) {
            throw new AppraisalException("Department cannot exceed 50 characters.");
        }
        if (subject.getYear() < 1 || subject.getYear() > 4) {
            throw new AppraisalException("Year of study must be between 1 and 4.");
        }
        if (subject.getMaxMarks() < 1 || subject.getMaxMarks() > 100) {
            throw new AppraisalException("Maximum marks must be between 1 and 100.");
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static AppraisalException databaseError(SQLException ex) {
        return new AppraisalException(
                "Database error while working with subjects: " + ex.getMessage(), ex);
    }
}
