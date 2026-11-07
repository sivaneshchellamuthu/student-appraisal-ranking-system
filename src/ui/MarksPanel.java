package ui;

import exception.AppraisalException;
import model.Mark;
import model.Student;
import model.Subject;
import model.User;
import service.MarksService;
import service.StudentService;
import service.SubjectService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Subject and marks management.
 *
 * The inner tabbed pane contains:
 *   - Enter Marks: select a student and subject, then save/update a mark.
 *   - Subjects: add, edit and delete subjects.
 */
public class MarksPanel extends JPanel {

    private final User currentUser;
    private final StudentService studentService = new StudentService();
    private final SubjectService subjectService = new SubjectService();
    private final MarksService marksService = new MarksService();

    private final JComboBox<Student> studentBox = new JComboBox<>();
    private final JComboBox<Subject> subjectBox = new JComboBox<>();
    private final JTextField marksField = new JTextField(10);

    private final DefaultTableModel marksModel = UiUtils.readOnlyModel(
            "Mark ID", "Subject ID", "Code", "Subject", "Max", "Obtained", "Grade");
    private final JTable marksTable = new JTable(marksModel);

    private final DefaultTableModel subjectModel = UiUtils.readOnlyModel(
            "Subject ID", "Code", "Subject Name", "Department", "Year", "Max Marks");
    private final JTable subjectTable = new JTable(subjectModel);

    private final JLabel marksStatus = new JLabel(" ");
    private final JLabel subjectStatus = new JLabel(" ");
    private boolean updatingCombos;

    public MarksPanel(User currentUser) {
        this.currentUser = currentUser;
        buildUi();
        refreshLists();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(UiUtils.BACKGROUND);
        add(UiUtils.header("Subject and Marks Management",
                "Add subjects and record marks per student per subject (0-100)"),
                BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.addTab("Enter Marks", buildMarksTab());
        tabs.addTab("Subjects", buildSubjectsTab());

        JPanel content = UiUtils.contentPanel();
        content.add(tabs, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private JPanel buildMarksTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setBackground(UiUtils.BACKGROUND);
        tab.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel form = UiUtils.formPanel();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 5, 5, 12);

        form.add(fieldLabel("Student"), constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        studentBox.setPreferredSize(new Dimension(420, 30));
        form.add(studentBox, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0;
        form.add(fieldLabel("Subject"), constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        subjectBox.setPreferredSize(new Dimension(420, 30));
        form.add(subjectBox, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0;
        form.add(fieldLabel("Marks (0-100)"), constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0;
        form.add(marksField, constraints);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        JButton saveButton = UiUtils.button("Save / Update Mark");
        JButton clearButton = UiUtils.secondaryButton("Clear");
        JButton deleteButton = UiUtils.secondaryButton("Delete Selected");
        JButton refreshButton = UiUtils.secondaryButton("Refresh Lists");
        buttons.add(saveButton);
        buttons.add(clearButton);
        buttons.add(deleteButton);
        buttons.add(refreshButton);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(12, 5, 0, 5);
        form.add(buttons, constraints);

        tab.add(form, BorderLayout.NORTH);

        UiUtils.styleTable(marksTable);
        marksTable.removeColumn(marksTable.getColumnModel().getColumn(0));
        marksTable.removeColumn(marksTable.getColumnModel().getColumn(0));
        tab.add(UiUtils.scroll(marksTable), BorderLayout.CENTER);

        marksStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        marksStatus.setForeground(UiUtils.MUTED);
        tab.add(marksStatus, BorderLayout.SOUTH);

        studentBox.addActionListener(event -> {
            if (!updatingCombos) {
                loadMarks();
            }
        });
        saveButton.addActionListener(event -> saveMark());
        clearButton.addActionListener(event -> clearMarkForm());
        deleteButton.addActionListener(event -> deleteSelectedMark());
        refreshButton.addActionListener(event -> refreshLists());
        marksTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                markSelected();
            }
        });

        return tab;
    }

    private JPanel buildSubjectsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBackground(UiUtils.BACKGROUND);
        tab.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel toolbar = UiUtils.toolbar();
        JButton addButton = UiUtils.button("Add Subject");
        JButton editButton = UiUtils.button("Edit");
        JButton deleteButton = UiUtils.secondaryButton("Delete");
        JButton refreshButton = UiUtils.secondaryButton("Refresh");
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(deleteButton);
        toolbar.add(refreshButton);
        tab.add(toolbar, BorderLayout.NORTH);

        subjectTable.setAutoCreateRowSorter(true);
        UiUtils.styleTable(subjectTable);
        subjectTable.removeColumn(subjectTable.getColumnModel().getColumn(0));
        tab.add(UiUtils.scroll(subjectTable), BorderLayout.CENTER);

        subjectStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subjectStatus.setForeground(UiUtils.MUTED);
        tab.add(subjectStatus, BorderLayout.SOUTH);

        addButton.addActionListener(event -> showSubjectDialog(null));
        editButton.addActionListener(event -> editSelectedSubject());
        deleteButton.addActionListener(event -> deleteSelectedSubject());
        refreshButton.addActionListener(event -> {
            loadSubjects();
            refreshLists();
        });

        return tab;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(UiUtils.TEXT);
        return label;
    }

    // ------------------------------------------------------------------
    // MARKS
    // ------------------------------------------------------------------

    private void refreshLists() {
        Integer selectedStudentId = selectedStudentId();
        Integer selectedSubjectId = selectedSubjectId();
        updatingCombos = true;
        try {
            List<Student> students = studentService.getAllStudents();
            List<Subject> subjects = subjectService.getAllSubjects();

            studentBox.removeAllItems();
            for (Student student : students) {
                studentBox.addItem(student);
            }
            subjectBox.removeAllItems();
            for (Subject subject : subjects) {
                subjectBox.addItem(subject);
            }

            selectStudent(selectedStudentId);
            selectSubject(selectedSubjectId);

            marksStatus.setForeground(UiUtils.SUCCESS);
            marksStatus.setText(students.size() + " student(s) and "
                    + subjects.size() + " subject(s) loaded.");
        } catch (AppraisalException ex) {
            marksStatus.setForeground(UiUtils.DANGER);
            marksStatus.setText(ex.getMessage());
            UiUtils.error(this, ex.getMessage());
        } finally {
            updatingCombos = false;
        }
        loadSubjects();
        loadMarks();
    }

    private void selectStudent(Integer studentId) {
        if (studentId != null) {
            for (int index = 0; index < studentBox.getItemCount(); index++) {
                Student item = studentBox.getItemAt(index);
                if (item != null && item.getId() == studentId) {
                    studentBox.setSelectedIndex(index);
                    return;
                }
            }
        }
        if (studentBox.getItemCount() > 0) {
            studentBox.setSelectedIndex(0);
        }
    }

    private void selectSubject(Integer subjectId) {
        if (subjectId != null) {
            for (int index = 0; index < subjectBox.getItemCount(); index++) {
                Subject item = subjectBox.getItemAt(index);
                if (item != null && item.getSubjectId() == subjectId) {
                    subjectBox.setSelectedIndex(index);
                    return;
                }
            }
        }
        if (subjectBox.getItemCount() > 0) {
            subjectBox.setSelectedIndex(0);
        }
    }

    private Integer selectedStudentId() {
        Student student = (Student) studentBox.getSelectedItem();
        return student == null ? null : student.getId();
    }

    private Integer selectedSubjectId() {
        Subject subject = (Subject) subjectBox.getSelectedItem();
        return subject == null ? null : subject.getSubjectId();
    }

    private void loadMarks() {
        Student student = (Student) studentBox.getSelectedItem();
        marksModel.setRowCount(0);
        if (student == null) {
            marksStatus.setText("No students available.");
            return;
        }
        try {
            List<Mark> marks = marksService.getMarksForStudent(student.getId());
            for (Mark mark : marks) {
                marksModel.addRow(new Object[]{
                        mark.getMarkId(),
                        mark.getSubjectId(),
                        mark.getSubjectCode(),
                        mark.getSubjectName(),
                        String.format("%.2f", mark.getMaxMarks()),
                        String.format("%.2f", mark.getMarksObtained()),
                        mark.getGrade()
                });
            }
            marksStatus.setForeground(UiUtils.SUCCESS);
            marksStatus.setText(marks.size() + " mark(s) for " + student.getRollNo()
                    + " - " + student.getName());
        } catch (AppraisalException ex) {
            marksStatus.setForeground(UiUtils.DANGER);
            marksStatus.setText(ex.getMessage());
        }
    }

    private void markSelected() {
        int viewRow = marksTable.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        int modelRow = marksTable.convertRowIndexToModel(viewRow);
        int subjectId = Integer.parseInt(
                String.valueOf(marksModel.getValueAt(modelRow, 1)));
        selectSubject(subjectId);
        marksField.setText(String.valueOf(marksModel.getValueAt(modelRow, 5)));
    }

    private void saveMark() {
        Student student = (Student) studentBox.getSelectedItem();
        Subject subject = (Subject) subjectBox.getSelectedItem();
        if (student == null || subject == null) {
            UiUtils.info(this, "Select a student and a subject first.");
            return;
        }
        double value;
        try {
            value = Double.parseDouble(marksField.getText().trim());
        } catch (NumberFormatException ex) {
            UiUtils.error(this, "Enter a numeric mark between 0 and 100.");
            return;
        }
        try {
            marksService.saveMark(student, subject, value, currentUser);
            loadMarks();
            marksStatus.setForeground(UiUtils.SUCCESS);
            marksStatus.setText("Mark saved for " + student.getRollNo()
                    + " in " + subject.getSubjectCode() + ".");
        } catch (AppraisalException ex) {
            UiUtils.error(this, ex.getMessage());
        }
    }

    private void clearMarkForm() {
        marksField.setText("");
        marksTable.clearSelection();
        if (studentBox.getItemCount() > 0) {
            studentBox.setSelectedIndex(0);
        }
        if (subjectBox.getItemCount() > 0) {
            subjectBox.setSelectedIndex(0);
        }
        marksField.requestFocusInWindow();
    }

    private void deleteSelectedMark() {
        int viewRow = marksTable.getSelectedRow();
        if (viewRow < 0) {
            UiUtils.info(this, "Select a mark row to delete.");
            return;
        }
        int modelRow = marksTable.convertRowIndexToModel(viewRow);
        int markId = Integer.parseInt(
                String.valueOf(marksModel.getValueAt(modelRow, 0)));
        if (!UiUtils.confirm(this, "Delete this mark?")) {
            return;
        }
        try {
            marksService.deleteMark(markId);
            loadMarks();
        } catch (AppraisalException ex) {
            UiUtils.error(this, ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // SUBJECTS
    // ------------------------------------------------------------------

    private void loadSubjects() {
        try {
            List<Subject> subjects = subjectService.getAllSubjects();
            subjectModel.setRowCount(0);
            for (Subject subject : subjects) {
                subjectModel.addRow(new Object[]{
                        subject.getSubjectId(),
                        subject.getSubjectCode(),
                        subject.getSubjectName(),
                        subject.getDepartment(),
                        subject.getYear(),
                        subject.getMaxMarks()
                });
            }
            subjectStatus.setForeground(UiUtils.SUCCESS);
            subjectStatus.setText(subjects.size() + " subject(s) listed.");
        } catch (AppraisalException ex) {
            subjectStatus.setForeground(UiUtils.DANGER);
            subjectStatus.setText(ex.getMessage());
        }
    }

    private int selectedSubjectRowId() {
        int viewRow = subjectTable.getSelectedRow();
        if (viewRow < 0) {
            return -1;
        }
        int modelRow = subjectTable.convertRowIndexToModel(viewRow);
        return Integer.parseInt(String.valueOf(subjectModel.getValueAt(modelRow, 0)));
    }

    private void editSelectedSubject() {
        int subjectId = selectedSubjectRowId();
        if (subjectId <= 0) {
            UiUtils.info(this, "Select a subject to edit.");
            return;
        }
        try {
            showSubjectDialog(subjectService.getSubject(subjectId));
        } catch (AppraisalException ex) {
            UiUtils.error(this, ex.getMessage());
        }
    }

    private void deleteSelectedSubject() {
        int subjectId = selectedSubjectRowId();
        if (subjectId <= 0) {
            UiUtils.info(this, "Select a subject to delete.");
            return;
        }
        if (!UiUtils.confirm(this, "Delete this subject?\nAll marks entered "
                + "for this subject will also be deleted.")) {
            return;
        }
        try {
            subjectService.deleteSubject(subjectId);
            loadSubjects();
            refreshLists();
        } catch (AppraisalException ex) {
            UiUtils.error(this, ex.getMessage());
        }
    }

    private void showSubjectDialog(Subject existing) {
        boolean editing = existing != null;
        JTextField codeField = new JTextField(18);
        JTextField nameField = new JTextField(18);
        JTextField departmentField = new JTextField(18);
        JComboBox<String> yearBox = new JComboBox<>(
                new String[]{"1", "2", "3", "4"});
        JTextField maxMarksField = new JTextField(18);

        if (editing) {
            codeField.setText(existing.getSubjectCode());
            nameField.setText(existing.getSubjectName());
            departmentField.setText(existing.getDepartment());
            yearBox.setSelectedItem(String.valueOf(existing.getYear()));
            maxMarksField.setText(String.valueOf(existing.getMaxMarks()));
        } else {
            maxMarksField.setText("100");
        }

        JPanel form = UiUtils.formPanel();
        UiUtils.addFormRow(form, 0, "Subject Code *", codeField);
        UiUtils.addFormRow(form, 1, "Subject Name *", nameField);
        UiUtils.addFormRow(form, 2, "Department *", departmentField);
        UiUtils.addFormRow(form, 3, "Year *", yearBox);
        UiUtils.addFormRow(form, 4, "Maximum Marks *", maxMarksField);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(this, form,
                    editing ? "Edit Subject" : "Add Subject",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }

            int maxMarks;
            try {
                maxMarks = Integer.parseInt(maxMarksField.getText().trim());
            } catch (NumberFormatException ex) {
                UiUtils.error(this, "Maximum marks must be a whole number "
                        + "between 1 and 100.");
                continue;
            }

            Subject subject = new Subject();
            if (editing) {
                subject.setSubjectId(existing.getSubjectId());
            }
            subject.setSubjectCode(codeField.getText());
            subject.setSubjectName(nameField.getText());
            subject.setDepartment(departmentField.getText());
            subject.setYear(Integer.parseInt(
                    String.valueOf(yearBox.getSelectedItem())));
            subject.setMaxMarks(maxMarks);

            try {
                if (editing) {
                    subjectService.updateSubject(subject);
                } else {
                    subjectService.addSubject(subject);
                }
                loadSubjects();
                refreshLists();
                return;
            } catch (AppraisalException ex) {
                UiUtils.error(this, ex.getMessage());
            }
        }
    }
}
