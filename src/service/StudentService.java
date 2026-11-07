package service;

import dao.DataStore;
import exception.AppraisalException;
import model.Student;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Student business rules: validation, duplicate handling and clear errors.
 */
public class StudentService {

    private final DataStore dataStore;

    public StudentService() {
        this(new DataStore());
    }

    public StudentService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<Student> getAllStudents() throws AppraisalException {
        try {
            return dataStore.getAllStudents();
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public List<Student> searchStudents(String keyword) throws AppraisalException {
        try {
            return dataStore.searchStudents(keyword);
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public List<Student> filterStudents(String department, String section)
            throws AppraisalException {
        try {
            return dataStore.getStudentsByFilter(department, section);
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public Student getStudent(int studentId) throws AppraisalException {
        try {
            Student student = dataStore.getStudentById(studentId);
            if (student == null) {
                throw new AppraisalException("Student not found.");
            }
            return student;
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public void addStudent(Student student) throws AppraisalException {
        normalizeAndValidate(student);
        try {
            dataStore.insertStudent(student);
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new AppraisalException(
                    "Roll number '" + student.getRollNo()
                    + "' already exists. Please use a unique roll number.", ex);
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public void updateStudent(Student student) throws AppraisalException {
        if (student == null || student.getId() <= 0) {
            throw new AppraisalException("Select a student to update.");
        }
        normalizeAndValidate(student);
        try {
            dataStore.updateStudent(student);
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new AppraisalException(
                    "Roll number '" + student.getRollNo()
                    + "' is already used by another student.", ex);
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public void deleteStudent(int studentId) throws AppraisalException {
        if (studentId <= 0) {
            throw new AppraisalException("Select a student to delete.");
        }
        try {
            dataStore.deleteStudent(studentId);
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public int countStudents() throws AppraisalException {
        try {
            return dataStore.countStudents();
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    /** Distinct departments for report/ranking filters. */
    public List<String> getDepartments() throws AppraisalException {
        TreeSet<String> values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Student student : getAllStudents()) {
            if (student.getDepartment() != null
                    && !student.getDepartment().trim().isEmpty()) {
                values.add(student.getDepartment().trim());
            }
        }
        return new ArrayList<>(values);
    }

    /** Distinct sections for report/ranking filters. */
    public List<String> getSections() throws AppraisalException {
        TreeSet<String> values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Student student : getAllStudents()) {
            if (student.getSection() != null
                    && !student.getSection().trim().isEmpty()) {
                values.add(student.getSection().trim());
            }
        }
        return new ArrayList<>(values);
    }

    private void normalizeAndValidate(Student student) throws AppraisalException {
        if (student == null) {
            throw new AppraisalException("Student details are missing.");
        }

        student.setRollNo(trim(student.getRollNo()));
        student.setName(trim(student.getName()));
        student.setDepartment(trim(student.getDepartment()));
        student.setSection(trim(student.getSection()));
        student.setEmail(trim(student.getEmail()));
        student.setPhone(trim(student.getPhone()));

        if (student.getRollNo().isEmpty()) {
            throw new AppraisalException("Roll number is required.");
        }
        if (student.getRollNo().length() > 20) {
            throw new AppraisalException("Roll number cannot exceed 20 characters.");
        }
        if (student.getName().isEmpty()) {
            throw new AppraisalException("Student name is required.");
        }
        if (student.getName().length() > 100) {
            throw new AppraisalException("Student name cannot exceed 100 characters.");
        }
        if (student.getDepartment().isEmpty()) {
            throw new AppraisalException("Department is required.");
        }
        if (student.getDepartment().length() > 50) {
            throw new AppraisalException("Department cannot exceed 50 characters.");
        }
        if (student.getSection().isEmpty()) {
            throw new AppraisalException("Section is required.");
        }
        if (student.getSection().length() > 5) {
            throw new AppraisalException("Section cannot exceed 5 characters.");
        }
        if (student.getYear() < 1 || student.getYear() > 4) {
            throw new AppraisalException("Year of study must be between 1 and 4.");
        }
        if (!Double.isFinite(student.getAttendance())
                || student.getAttendance() < 0.0 || student.getAttendance() > 100.0) {
            throw new AppraisalException("Attendance must be between 0 and 100.");
        }
        if (student.getEmail().length() > 100) {
            throw new AppraisalException("Email cannot exceed 100 characters.");
        }
        if (!student.getEmail().isEmpty()
                && (!student.getEmail().contains("@")
                || student.getEmail().contains(" "))) {
            throw new AppraisalException("Please enter a valid email address.");
        }
        if (student.getPhone().length() > 15) {
            throw new AppraisalException("Phone cannot exceed 15 characters.");
        }
        if (student.getEmail().isEmpty()) {
            student.setEmail(null);
        }
        if (student.getPhone().isEmpty()) {
            student.setPhone(null);
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static AppraisalException databaseError(SQLException ex) {
        return new AppraisalException(
                "Database error while working with students: " + ex.getMessage(), ex);
    }
}
