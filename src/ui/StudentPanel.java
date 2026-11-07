package ui;

import exception.AppraisalException;
import model.Student;
import service.StudentService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Student management screen: add, edit, delete, search and view students.
 */
public class StudentPanel extends JPanel {

    private final StudentService studentService = new StudentService();
    private final DefaultTableModel tableModel = UiUtils.readOnlyModel(
            "ID", "Roll No", "Name", "Department", "Year", "Section",
            "Attendance", "Email", "Phone");
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(22);
    private final JLabel statusLabel = new JLabel(" ");

    public StudentPanel() {
        buildUi();
        loadStudents("");
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(UiUtils.BACKGROUND);
        add(UiUtils.header("Student Management",
                "Maintain roll number, name, department, year, section and attendance"),
                BorderLayout.NORTH);

        JPanel content = UiUtils.contentPanel();

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel searchBar = UiUtils.toolbar();
        searchBar.add(UiUtils.bodyLabel("Search:"));
        searchBar.add(searchField);
        JButton searchButton = UiUtils.button("Search");
        JButton clearButton = UiUtils.secondaryButton("Clear");
        searchBar.add(searchButton);
        searchBar.add(clearButton);

        JPanel actionBar = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.RIGHT, 8, 0));
        actionBar.setOpaque(false);
        actionBar.setBorder(new EmptyBorder(10, 0, 10, 0));
        JButton addButton = UiUtils.button("Add Student");
        JButton editButton = UiUtils.button("Edit");
        JButton deleteButton = UiUtils.secondaryButton("Delete");
        JButton refreshButton = UiUtils.secondaryButton("Refresh");
        actionBar.add(addButton);
        actionBar.add(editButton);
        actionBar.add(deleteButton);
        actionBar.add(refreshButton);

        top.add(searchBar, BorderLayout.WEST);
        top.add(actionBar, BorderLayout.EAST);
        content.add(top, BorderLayout.NORTH);

        table.setAutoCreateRowSorter(true);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        UiUtils.styleTable(table);
        table.removeColumn(table.getColumnModel().getColumn(0));
        content.add(UiUtils.scroll(table), BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        statusLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        statusLabel.setForeground(UiUtils.MUTED);
        statusLabel.setBorder(new EmptyBorder(0, 18, 10, 18));
        add(statusLabel, BorderLayout.SOUTH);

        searchButton.addActionListener(event -> loadStudents(searchField.getText()));
        searchField.addActionListener(event -> loadStudents(searchField.getText()));
        clearButton.addActionListener(event -> {
            searchField.setText("");
            loadStudents("");
        });
        refreshButton.addActionListener(event -> loadStudents(searchField.getText()));
        addButton.addActionListener(event -> showStudentDialog(null));
        editButton.addActionListener(event -> editSelected());
        deleteButton.addActionListener(event -> deleteSelected());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    editSelected();
                }
            }
        });
    }

    private void loadStudents(String keyword) {
        try {
            List<Student> students = keyword == null || keyword.trim().isEmpty()
                    ? studentService.getAllStudents()
                    : studentService.searchStudents(keyword);
            tableModel.setRowCount(0);
            for (Student student : students) {
                tableModel.addRow(new Object[]{
                        student.getId(),
                        student.getRollNo(),
                        student.getName(),
                        student.getDepartment(),
                        student.getYear(),
                        student.getSection(),
                        String.format("%.2f", student.getAttendance()),
                        student.getEmail() == null ? "" : student.getEmail(),
                        student.getPhone() == null ? "" : student.getPhone()
                });
            }
            statusLabel.setForeground(UiUtils.SUCCESS);
            statusLabel.setText(students.size() + " student(s) listed.");
        } catch (AppraisalException ex) {
            statusLabel.setForeground(UiUtils.DANGER);
            statusLabel.setText(ex.getMessage());
            UiUtils.error(this, ex.getMessage());
        }
    }

    private int selectedStudentId() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return -1;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        Object value = tableModel.getValueAt(modelRow, 0);
        return value == null ? -1 : Integer.parseInt(value.toString());
    }

    private void editSelected() {
        int studentId = selectedStudentId();
        if (studentId <= 0) {
            UiUtils.info(this, "Select a student to edit.");
            return;
        }
        try {
            showStudentDialog(studentService.getStudent(studentId));
        } catch (AppraisalException ex) {
            UiUtils.error(this, ex.getMessage());
        }
    }

    private void deleteSelected() {
        int studentId = selectedStudentId();
        if (studentId <= 0) {
            UiUtils.info(this, "Select a student to delete.");
            return;
        }
        int viewRow = table.getSelectedRow();
        int modelRow = table.convertRowIndexToModel(viewRow);
        String rollNo = String.valueOf(tableModel.getValueAt(modelRow, 1));
        String name = String.valueOf(tableModel.getValueAt(modelRow, 2));
        if (!UiUtils.confirm(this, "Delete student " + rollNo + " (" + name
                + ")?\nAll marks for this student will also be deleted.")) {
            return;
        }
        try {
            studentService.deleteStudent(studentId);
            loadStudents(searchField.getText());
        } catch (AppraisalException ex) {
            UiUtils.error(this, ex.getMessage());
        }
    }

    private void showStudentDialog(Student existing) {
        boolean editing = existing != null;
        JTextField rollField = new JTextField(18);
        JTextField nameField = new JTextField(18);
        JTextField departmentField = new JTextField(18);
        JComboBox<String> yearBox = new JComboBox<>(
                new String[]{"1", "2", "3", "4"});
        JTextField sectionField = new JTextField(18);
        JTextField attendanceField = new JTextField(18);
        JTextField emailField = new JTextField(18);
        JTextField phoneField = new JTextField(18);

        if (editing) {
            rollField.setText(existing.getRollNo());
            nameField.setText(existing.getName());
            departmentField.setText(existing.getDepartment());
            yearBox.setSelectedItem(String.valueOf(existing.getYear()));
            sectionField.setText(existing.getSection());
            attendanceField.setText(String.valueOf(existing.getAttendance()));
            emailField.setText(existing.getEmail() == null ? "" : existing.getEmail());
            phoneField.setText(existing.getPhone() == null ? "" : existing.getPhone());
        } else {
            attendanceField.setText("0");
        }

        JPanel form = UiUtils.formPanel();
        UiUtils.addFormRow(form, 0, "Roll No *", rollField);
        UiUtils.addFormRow(form, 1, "Name *", nameField);
        UiUtils.addFormRow(form, 2, "Department *", departmentField);
        UiUtils.addFormRow(form, 3, "Year *", yearBox);
        UiUtils.addFormRow(form, 4, "Section *", sectionField);
        UiUtils.addFormRow(form, 5, "Attendance % *", attendanceField);
        UiUtils.addFormRow(form, 6, "Email", emailField);
        UiUtils.addFormRow(form, 7, "Phone", phoneField);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(this, form,
                    editing ? "Edit Student" : "Add Student",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }

            double attendance;
            try {
                attendance = Double.parseDouble(attendanceField.getText().trim());
            } catch (NumberFormatException ex) {
                UiUtils.error(this, "Attendance must be a number between 0 and 100.");
                continue;
            }

            Student student = new Student();
            if (editing) {
                student.setId(existing.getId());
            }
            student.setRollNo(rollField.getText());
            student.setName(nameField.getText());
            student.setDepartment(departmentField.getText());
            student.setYear(Integer.parseInt(
                    String.valueOf(yearBox.getSelectedItem())));
            student.setSection(sectionField.getText());
            student.setAttendance(attendance);
            student.setEmail(emailField.getText());
            student.setPhone(phoneField.getText());

            try {
                if (editing) {
                    studentService.updateStudent(student);
                } else {
                    studentService.addStudent(student);
                }
                loadStudents(searchField.getText());
                return;
            } catch (AppraisalException ex) {
                UiUtils.error(this, ex.getMessage());
            }
        }
    }
}
