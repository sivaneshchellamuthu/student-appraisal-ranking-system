package store;

import model.Mark;
import model.Student;
import model.User;
import util.PasswordUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public class DataStore {

    private final Path dataDir;
    private final Path usersFile;
    private final Path studentsFile;
    private final Path marksFile;

    private final List<User> users = new ArrayList<>();
    private final List<Student> students = new ArrayList<>();
    private final List<Mark> marks = new ArrayList<>();

    public DataStore(Path dataDir) {
        this.dataDir = dataDir;
        this.usersFile = dataDir.resolve("users.csv");
        this.studentsFile = dataDir.resolve("students.csv");
        this.marksFile = dataDir.resolve("marks.csv");
    }

    public void load() throws IOException {
        Files.createDirectories(dataDir);

        if (Files.exists(usersFile)) {
            readUsers();
        } else {
            seedUsers();
            saveUsers();
        }

        if (Files.exists(studentsFile)) {
            readStudents();
            if (Files.exists(marksFile)) {
                readMarks();
            } else {
                saveMarks();
            }
        } else {
            seedStudents();
            seedMarks();
            saveStudents();
            saveMarks();
        }
    }

    public User authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(normalized)
                    && PasswordUtil.verify(password, user.getSalt(), user.getPasswordHash())) {
                return user;
            }
        }
        return null;
    }

    public List<User> getUsers() {
        return List.copyOf(users);
    }

    public User findUser(String username) {
        if (username == null) {
            return null;
        }
        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(username.trim())) {
                return user;
            }
        }
        return null;
    }

    public long countAdmins() {
        return users.stream().filter(u -> u.getRole() == User.Role.ADMIN).count();
    }

    public long countFaculty() {
        return users.stream().filter(u -> u.getRole() == User.Role.FACULTY).count();
    }

    public void addUser(String username, String displayName, String password, User.Role role) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        String name = displayName == null ? "" : displayName.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (!normalized.matches("[a-z0-9._-]{3,30}")) {
            throw new IllegalArgumentException("Username must be 3-30 characters (letters, numbers, . _ -).");
        }
        if (findUser(normalized) != null) {
            throw new IllegalArgumentException("That username already exists.");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Display name is required.");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters.");
        }
        String salt = PasswordUtil.newSalt();
        users.add(new User(normalized, PasswordUtil.hash(password, salt), salt, role, name));
        saveUsers();
    }

    public void deleteUser(String username) {
        User user = findUser(username);
        if (user == null) {
            return;
        }
        users.remove(user);
        saveUsers();
    }

    public List<Student> getStudents() {
        return List.copyOf(students);
    }

    public Student findStudent(String regNo) {
        if (regNo == null) {
            return null;
        }
        for (Student student : students) {
            if (student.getRegNo().equalsIgnoreCase(regNo.trim())) {
                return student;
            }
        }
        return null;
    }

    public void addStudent(String regNo, String name) {
        String normalized = regNo == null ? "" : regNo.trim().toUpperCase(Locale.ROOT);
        String studentName = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Register number is required.");
        }
        if (!normalized.matches("[A-Z0-9/_-]{2,20}")) {
            throw new IllegalArgumentException("Register number must be 2-20 characters (A-Z, 0-9, / _ -).");
        }
        if (studentName.isEmpty()) {
            throw new IllegalArgumentException("Student name is required.");
        }
        if (findStudent(normalized) != null) {
            throw new IllegalArgumentException("Register number " + normalized + " already exists.");
        }
        students.add(new Student(normalized, studentName));
        saveStudents();
    }

    public void deleteStudent(String regNo) {
        Student student = findStudent(regNo);
        if (student == null) {
            return;
        }
        students.remove(student);
        marks.removeIf(m -> m.getRegNo().equalsIgnoreCase(student.getRegNo()));
        saveStudents();
        saveMarks();
    }

    public List<Mark> getMarks() {
        return List.copyOf(marks);
    }

    public List<Mark> marksFor(String regNo) {
        if (regNo == null) {
            return List.of();
        }
        List<Mark> result = new ArrayList<>();
        for (Mark mark : marks) {
            if (mark.getRegNo().equalsIgnoreCase(regNo.trim())) {
                result.add(mark);
            }
        }
        result.sort(Comparator.comparing(Mark::getSubject, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public List<String> getSubjects() {
        TreeSet<String> subjects = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Mark mark : marks) {
            subjects.add(mark.getSubject());
        }
        return new ArrayList<>(subjects);
    }

    public void saveMark(String regNo, String subject, double value) {
        Student student = findStudent(regNo);
        if (student == null) {
            throw new IllegalArgumentException("Student not found for register number " + regNo + ".");
        }
        String subjectName = subject == null ? "" : subject.trim();
        if (subjectName.isEmpty()) {
            throw new IllegalArgumentException("Subject is required.");
        }
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("Mark must be between 0 and 100.");
        }

        Mark existing = null;
        for (Mark mark : marks) {
            if (mark.getRegNo().equalsIgnoreCase(student.getRegNo())
                    && mark.getSubject().equalsIgnoreCase(subjectName)) {
                existing = mark;
                break;
            }
        }
        if (existing != null) {
            int index = marks.indexOf(existing);
            marks.set(index, new Mark(existing.getRegNo(), existing.getSubject(), value));
        } else {
            marks.add(new Mark(student.getRegNo(), subjectName, value));
        }
        saveMarks();
    }

    public void deleteMark(String regNo, String subject) {
        marks.removeIf(mark -> mark.getRegNo().equalsIgnoreCase(regNo)
                && mark.getSubject().equalsIgnoreCase(subject));
        saveMarks();
    }

    private void readUsers() throws IOException {
        users.clear();
        for (String line : Files.readAllLines(usersFile, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            List<String> cols = parseCsv(line);
            if (cols.size() < 5) {
                continue;
            }
            try {
                User.Role role = User.Role.valueOf(cols.get(3).trim().toUpperCase(Locale.ROOT));
                users.add(new User(cols.get(0), cols.get(1), cols.get(2), role, cols.get(4)));
            } catch (IllegalArgumentException ignored) {
                // Skip a malformed row instead of blocking startup.
            }
        }
    }

    private void readStudents() throws IOException {
        students.clear();
        for (String line : Files.readAllLines(studentsFile, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            List<String> cols = parseCsv(line);
            if (cols.size() >= 2 && !cols.get(0).isBlank()) {
                students.add(new Student(cols.get(0), cols.get(1)));
            }
        }
    }

    private void readMarks() throws IOException {
        marks.clear();
        for (String line : Files.readAllLines(marksFile, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            List<String> cols = parseCsv(line);
            if (cols.size() < 3) {
                continue;
            }
            try {
                double value = Double.parseDouble(cols.get(2));
                if (findStudent(cols.get(0)) != null) {
                    marks.add(new Mark(cols.get(0), cols.get(1), value));
                }
            } catch (NumberFormatException ignored) {
                // Skip malformed mark rows.
            }
        }
    }

    private void seedUsers() {
        seedUser("admin", "admin123", User.Role.ADMIN, "System Administrator");
        seedUser("faculty", "faculty123", User.Role.FACULTY, "Prof. Faculty");
    }

    private void seedUser(String username, String password, User.Role role, String displayName) {
        String salt = PasswordUtil.newSalt();
        users.add(new User(username, PasswordUtil.hash(password, salt), salt, role, displayName));
    }

    private void seedStudents() {
        students.add(new Student("21CS001", "Aarav Sharma"));
        students.add(new Student("21CS002", "Diya Patel"));
        students.add(new Student("21CS003", "Rohan Mehta"));
        students.add(new Student("21CS004", "Ananya Iyer"));
        students.add(new Student("21CS005", "Kabir Singh"));
        students.add(new Student("21CS006", "Meera Nair"));
    }

    private void seedMarks() {
        String[] subjects = {"Mathematics", "Data Structures", "DBMS", "Operating Systems"};
        double[][] values = {
                {88, 92, 79, 85},
                {95, 89, 91, 94},
                {72, 78, 81, 69},
                {90, 85, 88, 92},
                {65, 70, 74, 68},
                {84, 88, 90, 86}
        };
        for (int s = 0; s < students.size(); s++) {
            for (int i = 0; i < subjects.length; i++) {
                marks.add(new Mark(students.get(s).getRegNo(), subjects[i], values[s][i]));
            }
        }
    }

    private void saveUsers() {
        List<String> lines = new ArrayList<>();
        for (User user : users) {
            lines.add(joinCsv(user.getUsername(), user.getPasswordHash(), user.getSalt(),
                    user.getRole().name(), user.getDisplayName()));
        }
        writeFile(usersFile, lines);
    }

    private void saveStudents() {
        List<String> lines = new ArrayList<>();
        for (Student student : students) {
            lines.add(joinCsv(student.getRegNo(), student.getName()));
        }
        writeFile(studentsFile, lines);
    }

    private void saveMarks() {
        List<String> lines = new ArrayList<>();
        for (Mark mark : marks) {
            lines.add(joinCsv(mark.getRegNo(), mark.getSubject(), String.valueOf(mark.getValue())));
        }
        writeFile(marksFile, lines);
    }

    private void writeFile(Path path, List<String> lines) {
        try {
            Files.createDirectories(dataDir);
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to save data file: " + path, e);
        }
    }

    private static String joinCsv(String... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapeCsv(values[i]));
        }
        return sb.toString();
    }

    private static String escapeCsv(String value) {
        String text = value == null ? "" : value;
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private static List<String> parseCsv(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }
}
