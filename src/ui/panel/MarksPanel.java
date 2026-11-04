package ui.panel;

import model.Mark;
import model.Student;
import store.DataStore;
import store.RankingService;
import ui.Refreshable;
import ui.UiTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MarksPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private record StudentItem(String regNo, String name) {
        @Override
        public String toString() {
            return name.isEmpty() ? regNo : regNo + "  -  " + name;
        }
    }

    private final DataStore store;

    private final JComboBox<StudentItem> studentCombo = new JComboBox<>();
    private final JComboBox<String> subjectCombo = new JComboBox<>();
    private final JTextField markField = new JTextField();
    private final JTextField searchField = new JTextField();
    private final JLabel statusLabel = UiTheme.label(" ", Font.PLAIN, 12, UiTheme.MUTED);

    private final javax.swing.table.DefaultTableModel tableModel =
            UiTheme.tableModel("Reg No", "Student", "Subject", "Mark");
    private final JTable table = UiTheme.table(tableModel);

    public MarksPanel(DataStore store) {
        this.store = store;

        setLayout(new BorderLayout(0, 16));
        setBackground(UiTheme.BG);
        setBorder(BorderFactory.createEmptyBorder());

        add(buildForm(), BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);

        refreshData();
    }

    private JPanel buildForm() {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 10));

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new javax.swing.BoxLayout(rows, javax.swing.BoxLayout.Y_AXIS));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row1.setOpaque(false);

        UiTheme.styleCombo(studentCombo);
        studentCombo.setPreferredSize(new Dimension(280, 40));

        subjectCombo.setEditable(true);
        UiTheme.styleCombo(subjectCombo);
        subjectCombo.setPreferredSize(new Dimension(220, 40));

        UiTheme.styleField(markField);
        markField.setPreferredSize(new Dimension(120, 40));
        markField.setToolTipText("Enter a mark between 0 and 100");

        JButton saveButton = UiTheme.primaryButton("Save mark");
        saveButton.addActionListener(e -> saveMark());
        markField.addActionListener(e -> saveMark());

        row1.add(UiTheme.label("Student", Font.BOLD, 12, UiTheme.MUTED));
        row1.add(studentCombo);
        row1.add(Box.createRigidArea(new Dimension(6, 0)));
        row1.add(UiTheme.label("Subject", Font.BOLD, 12, UiTheme.MUTED));
        row1.add(subjectCombo);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row2.setOpaque(false);
        row2.add(UiTheme.label("Mark (0-100)", Font.BOLD, 12, UiTheme.MUTED));
        row2.add(markField);
        row2.add(saveButton);
        row2.add(UiTheme.label("Type a new subject or pick an existing one. Existing marks are updated.",
                Font.PLAIN, 11, UiTheme.MUTED));

        rows.add(row1);
        rows.add(Box.createRigidArea(new Dimension(0, 8)));
        rows.add(row2);

        card.add(rows, BorderLayout.CENTER);
        card.add(statusLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildTableCard() {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 12));

        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(UiTheme.label("Recorded Marks", Font.BOLD, 16, UiTheme.TEXT));

        UiTheme.styleField(searchField);
        searchField.setPreferredSize(new Dimension(240, 38));
        searchField.setToolTipText("Search by register number, name or subject");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                loadTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                loadTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                loadTable();
            }
        });
        left.add(searchField);

        JButton deleteButton = UiTheme.dangerButton("Delete selected");
        deleteButton.addActionListener(e -> deleteSelected());

        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(deleteButton, BorderLayout.EAST);

        card.add(toolbar, BorderLayout.NORTH);
        card.add(UiTheme.scroll(table), BorderLayout.CENTER);
        return card;
    }

    private void saveMark() {
        StudentItem student = (StudentItem) studentCombo.getSelectedItem();
        if (student == null) {
            statusLabel.setForeground(UiTheme.DANGER);
            statusLabel.setText("Add a student first, then record marks.");
            return;
        }
        Object editorValue = subjectCombo.getEditor().getItem();
        String subject = editorValue == null ? "" : editorValue.toString().trim();
        String markText = markField.getText().trim();

        try {
            if (markText.isEmpty()) {
                throw new IllegalArgumentException("Enter a mark between 0 and 100.");
            }
            double mark = Double.parseDouble(markText);
            store.saveMark(student.regNo(), subject, mark);
            statusLabel.setForeground(UiTheme.SUCCESS);
            statusLabel.setText("Saved " + subject + " = " + RankingService.formatMark(mark)
                    + " for " + student.regNo() + ".");
            markField.setText("");
            reloadSubjects(subject);
            loadTable();
            markField.requestFocusInWindow();
        } catch (NumberFormatException ex) {
            statusLabel.setForeground(UiTheme.DANGER);
            statusLabel.setText("Mark must be a number between 0 and 100.");
        } catch (IllegalArgumentException ex) {
            statusLabel.setForeground(UiTheme.DANGER);
            statusLabel.setText(ex.getMessage());
        } catch (RuntimeException ex) {
            statusLabel.setForeground(UiTheme.DANGER);
            statusLabel.setText("Could not save: " + ex.getMessage());
        }
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            statusLabel.setForeground(UiTheme.WARNING);
            statusLabel.setText("Select a mark row in the table first.");
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        String regNo = String.valueOf(tableModel.getValueAt(row, 0));
        String subject = String.valueOf(tableModel.getValueAt(row, 2));

        int choice = JOptionPane.showConfirmDialog(this,
                "Delete the " + subject + " mark for " + regNo + "?",
                "Delete mark", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        store.deleteMark(regNo, subject);
        statusLabel.setForeground(UiTheme.SUCCESS);
        statusLabel.setText("Mark deleted.");
        loadTable();
    }

    @Override
    public void refreshData() {
        reloadStudents();
        reloadSubjects(null);
        loadTable();
    }

    private void reloadStudents() {
        StudentItem selected = (StudentItem) studentCombo.getSelectedItem();
        String selectedReg = selected == null ? null : selected.regNo();

        studentCombo.removeAllItems();
        List<Student> students = new ArrayList<>(store.getStudents());
        students.sort(Comparator.comparing(Student::getRegNo, String.CASE_INSENSITIVE_ORDER));
        for (Student student : students) {
            studentCombo.addItem(new StudentItem(student.getRegNo(), student.getName()));
        }

        if (selectedReg != null) {
            for (int i = 0; i < studentCombo.getItemCount(); i++) {
                if (studentCombo.getItemAt(i).regNo().equalsIgnoreCase(selectedReg)) {
                    studentCombo.setSelectedIndex(i);
                    return;
                }
            }
        }
        if (studentCombo.getItemCount() > 0) {
            studentCombo.setSelectedIndex(0);
        }
    }

    private void reloadSubjects(String preferred) {
        Object current = subjectCombo.getEditor().getItem();
        String keep = preferred != null ? preferred : current == null ? "" : current.toString();

        subjectCombo.removeAllItems();
        for (String subject : store.getSubjects()) {
            subjectCombo.addItem(subject);
        }
        subjectCombo.getEditor().setItem(keep);
    }

    private void loadTable() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        tableModel.setRowCount(0);

        List<Mark> marks = new ArrayList<>(store.getMarks());
        marks.sort(Comparator.comparing(Mark::getRegNo, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Mark::getSubject, String.CASE_INSENSITIVE_ORDER));

        for (Mark mark : marks) {
            Student student = store.findStudent(mark.getRegNo());
            String name = student == null ? "" : student.getName();
            if (!query.isEmpty()
                    && !mark.getRegNo().toLowerCase().contains(query)
                    && !mark.getSubject().toLowerCase().contains(query)
                    && !name.toLowerCase().contains(query)) {
                continue;
            }
            tableModel.addRow(new Object[]{mark.getRegNo(), name, mark.getSubject(),
                    RankingService.formatMark(mark.getValue())});
        }
    }
}
