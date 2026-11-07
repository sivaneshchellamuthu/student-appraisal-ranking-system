package dao;

import model.Mark;
import model.Student;
import model.Subject;
import model.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The single data-access class for the whole application.
 *
 * Every SQL statement and every JDBC call lives in this class. The service
 * and UI layers never touch java.sql directly.
 *
 * All statements use PreparedStatement and try-with-resources so connections,
 * statements and result sets are always closed, even when an exception occurs.
 */
public class DataStore {

    // ==================================================================
    // DATABASE CONFIGURATION - change these three values for your PC
    // ==================================================================
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/student_db"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
    // ==================================================================

    private static final String STUDENT_COLUMNS =
            "student_id, roll_no, name, department, study_year, section, "
            + "attendance, email, phone";

    /**
     * Opens a new connection. Callers always use try-with-resources.
     */
    private Connection getConnection() throws SQLException {
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException ex) {
            throw new SQLException(
                    "MySQL Connector/J was not found. Copy mysql-connector-j.jar "
                    + "into the lib folder and add it to the classpath.", ex);
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /** Used by the login screen to show whether the database is reachable. */
    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(2);
        } catch (SQLException ex) {
            return false;
        }
    }

    // ==================================================================
    // USERS
    // ==================================================================

    public User findUserByUsername(String username) throws SQLException {
        String sql = "SELECT user_id, username, password_hash, full_name, "
                + "user_role, active, created_at FROM users WHERE username = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return mapUser(results);
                }
            }
        }
        return null;
    }

    public User findUserById(int userId) throws SQLException {
        String sql = "SELECT user_id, username, password_hash, full_name, "
                + "user_role, active, created_at FROM users WHERE user_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return mapUser(results);
                }
            }
        }
        return null;
    }

    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT user_id, username, password_hash, full_name, "
                + "user_role, active, created_at FROM users ORDER BY username";
        List<User> users = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                users.add(mapUser(results));
            }
        }
        return users;
    }

    public void insertUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name, "
                + "user_role, active) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getName());
            statement.setString(4, user.getRole().name());
            statement.setBoolean(5, user.isActive());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
        }
    }

    public void updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, password_hash = ?, "
                + "full_name = ?, user_role = ?, active = ? WHERE user_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getName());
            statement.setString(4, user.getRole().name());
            statement.setBoolean(5, user.isActive());
            statement.setInt(6, user.getId());
            statement.executeUpdate();
        }
    }

    public void deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    public int countUsers() throws SQLException {
        return count("SELECT COUNT(*) FROM users");
    }

    public int countUsersByRole(User.Role role) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE user_role = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role.name());
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? results.getInt(1) : 0;
            }
        }
    }

    // ==================================================================
    // STUDENTS
    // ==================================================================

    public List<Student> getAllStudents() throws SQLException {
        String sql = "SELECT " + STUDENT_COLUMNS + " FROM students ORDER BY roll_no";
        return readStudents(sql, null);
    }

    public List<Student> searchStudents(String keyword) throws SQLException {
        if (!hasText(keyword)) {
            return getAllStudents();
        }
        String sql = "SELECT " + STUDENT_COLUMNS + " FROM students "
                + "WHERE roll_no LIKE ? OR name LIKE ? OR department LIKE ? "
                + "OR section LIKE ? ORDER BY roll_no";
        String pattern = "%" + keyword.trim() + "%";
        return readStudents(sql, new String[]{pattern, pattern, pattern, pattern});
    }

    /**
     * Returns students filtered by department and/or section. A null or blank
     * filter (or the text "All") means no restriction.
     */
    public List<Student> getStudentsByFilter(String department, String section)
            throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT " + STUDENT_COLUMNS + " FROM students WHERE 1 = 1");
        List<String> parameters = new ArrayList<>();

        if (hasValue(department)) {
            sql.append(" AND department = ?");
            parameters.add(department.trim());
        }
        if (hasValue(section)) {
            sql.append(" AND section = ?");
            parameters.add(section.trim());
        }
        sql.append(" ORDER BY roll_no");

        return readStudents(sql.toString(), parameters.toArray(new String[0]));
    }

    public Student getStudentById(int studentId) throws SQLException {
        String sql = "SELECT " + STUDENT_COLUMNS + " FROM students WHERE student_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? mapStudent(results) : null;
            }
        }
    }

    public Student getStudentByRollNo(String rollNo) throws SQLException {
        String sql = "SELECT " + STUDENT_COLUMNS + " FROM students WHERE roll_no = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, rollNo);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? mapStudent(results) : null;
            }
        }
    }

    public void insertStudent(Student student) throws SQLException {
        String sql = "INSERT INTO students (roll_no, name, department, "
                + "study_year, section, attendance, email, phone) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindStudent(statement, student);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    student.setId(keys.getInt(1));
                }
            }
        }
    }

    public void updateStudent(Student student) throws SQLException {
        String sql = "UPDATE students SET roll_no = ?, name = ?, department = ?, "
                + "study_year = ?, section = ?, attendance = ?, email = ?, "
                + "phone = ? WHERE student_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindStudent(statement, student);
            statement.setInt(9, student.getId());
            statement.executeUpdate();
        }
    }

    public void deleteStudent(int studentId) throws SQLException {
        String sql = "DELETE FROM students WHERE student_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.executeUpdate();
        }
    }

    public int countStudents() throws SQLException {
        return count("SELECT COUNT(*) FROM students");
    }

    // ==================================================================
    // SUBJECTS
    // ==================================================================

    public List<Subject> getAllSubjects() throws SQLException {
        String sql = "SELECT subject_id, subject_code, subject_name, department, "
                + "study_year, max_marks FROM subjects ORDER BY subject_code";
        List<Subject> subjects = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                subjects.add(mapSubject(results));
            }
        }
        return subjects;
    }

    public Subject getSubjectById(int subjectId) throws SQLException {
        String sql = "SELECT subject_id, subject_code, subject_name, department, "
                + "study_year, max_marks FROM subjects WHERE subject_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, subjectId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? mapSubject(results) : null;
            }
        }
    }

    public void insertSubject(Subject subject) throws SQLException {
        String sql = "INSERT INTO subjects (subject_code, subject_name, department, "
                + "study_year, max_marks) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindSubject(statement, subject);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    subject.setSubjectId(keys.getInt(1));
                }
            }
        }
    }

    public void updateSubject(Subject subject) throws SQLException {
        String sql = "UPDATE subjects SET subject_code = ?, subject_name = ?, "
                + "department = ?, study_year = ?, max_marks = ? "
                + "WHERE subject_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindSubject(statement, subject);
            statement.setInt(6, subject.getSubjectId());
            statement.executeUpdate();
        }
    }

    public void deleteSubject(int subjectId) throws SQLException {
        String sql = "DELETE FROM subjects WHERE subject_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, subjectId);
            statement.executeUpdate();
        }
    }

    public int countSubjects() throws SQLException {
        return count("SELECT COUNT(*) FROM subjects");
    }

    // ==================================================================
    // MARKS
    // ==================================================================

    public List<Mark> getMarksByStudent(int studentId) throws SQLException {
        String sql = "SELECT m.mark_id, m.student_id, m.subject_id, "
                + "m.marks_obtained, m.entered_by, s.subject_code, "
                + "s.subject_name, s.max_marks "
                + "FROM marks m JOIN subjects s ON m.subject_id = s.subject_id "
                + "WHERE m.student_id = ? ORDER BY s.subject_code";
        List<Mark> marks = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    Mark mark = new Mark();
                    mark.setMarkId(results.getInt("mark_id"));
                    mark.setStudentId(results.getInt("student_id"));
                    mark.setSubjectId(results.getInt("subject_id"));
                    mark.setMarksObtained(results.getDouble("marks_obtained"));
                    mark.setEnteredBy(results.getInt("entered_by"));
                    mark.setSubjectCode(results.getString("subject_code"));
                    mark.setSubjectName(results.getString("subject_name"));
                    mark.setMaxMarks(results.getDouble("max_marks"));
                    marks.add(mark);
                }
            }
        }
        return marks;
    }

    /**
     * Inserts a mark or updates the existing mark for the same
     * student/subject pair. The UNIQUE key in the schema makes this safe.
     */
    public void upsertMark(int studentId, int subjectId, double marksObtained,
                           int enteredBy) throws SQLException {
        String sql = "INSERT INTO marks (student_id, subject_id, marks_obtained, "
                + "entered_by) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE marks_obtained = ?, entered_by = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, subjectId);
            statement.setDouble(3, marksObtained);
            if (enteredBy > 0) {
                statement.setInt(4, enteredBy);
            } else {
                statement.setNull(4, Types.INTEGER);
            }
            statement.setDouble(5, marksObtained);
            if (enteredBy > 0) {
                statement.setInt(6, enteredBy);
            } else {
                statement.setNull(6, Types.INTEGER);
            }
            statement.executeUpdate();
        }
    }

    public void deleteMark(int markId) throws SQLException {
        String sql = "DELETE FROM marks WHERE mark_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, markId);
            statement.executeUpdate();
        }
    }

    public int countMarks() throws SQLException {
        return count("SELECT COUNT(*) FROM marks");
    }

    // ==================================================================
    // PRIVATE HELPERS
    // ==================================================================

    private int count(String sql) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            return results.next() ? results.getInt(1) : 0;
        }
    }

    private List<Student> readStudents(String sql, String[] parameters)
            throws SQLException {
        List<Student> students = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameters != null) {
                for (int i = 0; i < parameters.length; i++) {
                    statement.setString(i + 1, parameters[i]);
                }
            }
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    students.add(mapStudent(results));
                }
            }
        }
        return students;
    }

    private void bindStudent(PreparedStatement statement, Student student)
            throws SQLException {
        statement.setString(1, student.getRollNo());
        statement.setString(2, student.getName());
        statement.setString(3, student.getDepartment());
        statement.setInt(4, student.getYear());
        statement.setString(5, student.getSection());
        statement.setDouble(6, student.getAttendance());
        statement.setString(7, student.getEmail());
        statement.setString(8, student.getPhone());
    }

    private void bindSubject(PreparedStatement statement, Subject subject)
            throws SQLException {
        statement.setString(1, subject.getSubjectCode());
        statement.setString(2, subject.getSubjectName());
        statement.setString(3, subject.getDepartment());
        statement.setInt(4, subject.getYear());
        statement.setInt(5, subject.getMaxMarks());
    }

    private Student mapStudent(ResultSet results) throws SQLException {
        return new Student(
                results.getInt("student_id"),
                results.getString("roll_no"),
                results.getString("name"),
                results.getString("department"),
                results.getInt("study_year"),
                results.getString("section"),
                results.getDouble("attendance"),
                results.getString("email"),
                results.getString("phone"));
    }

    private Subject mapSubject(ResultSet results) throws SQLException {
        return new Subject(
                results.getInt("subject_id"),
                results.getString("subject_code"),
                results.getString("subject_name"),
                results.getString("department"),
                results.getInt("study_year"),
                results.getInt("max_marks"));
    }

    private User mapUser(ResultSet results) throws SQLException {
        User user = new User();
        user.setId(results.getInt("user_id"));
        user.setUsername(results.getString("username"));
        user.setPasswordHash(results.getString("password_hash"));
        user.setName(results.getString("full_name"));
        user.setRole(User.Role.fromDatabase(results.getString("user_role")));
        user.setActive(results.getBoolean("active"));
        Timestamp created = results.getTimestamp("created_at");
        user.setCreatedAt(created == null ? ""
                : created.toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        return user;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean hasValue(String value) {
        return hasText(value) && !"All".equalsIgnoreCase(value.trim());
    }
}

