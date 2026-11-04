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

public class StudentsPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final DataStore store;

    private final JTextField regField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField searchField = new JTextField();
    private final JLabel statusLabel = UiTheme.label(" ", Font.PLAIN, 12, UiTheme.MUTED);

    private final javax.swing.table.DefaultTableModel tableModel =
            UiTheme.tableModel("Reg No", "Student Name", "Subjects", "Average");
    private final JTable table = UiTheme.table(tableModel);

    public StudentsPanel(DataStore store) {
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

        UiTheme.styleField(regField);
        regField.setPreferredSize(new Dimension(160, 40));
        regField.setToolTipText("Example: 21CS007");

        UiTheme.styleField(nameField);
        nameField.setPreferredSize(new Dimension(280, 40));

        JButton addButton = UiTheme.primaryButton("Add student");
        JButton clearButton = UiTheme.secondaryButton("Clear");
        addButton.addActionListener(e -> addStudent());
        clearButton.addActionListener(e -> {
            regField.setText("");
            nameField.setText("");
            statusLabel.setForeground(UiTheme.MUTED);
            statusLabel.setText(" ");
            regField.requestFocusInWindow();
        });
        regField.addActionListener(e -> addStudent());
        nameField.addActionListener(e -> addStudent());

        row1.add(UiTheme.label("Register No", Font.BOLD, 12, UiTheme.MUTED));
        row1.add(regField);
        row1.add(Box.createRigidArea(new Dimension(6, 0)));
        row1.add(UiTheme.label("Student Name", Font.BOLD, 12, UiTheme.MUTED));
        row1.add(nameField);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row2.setOpaque(false);
        row2.add(addButton);
        row2.add(clearButton);
        row2.add(UiTheme.label("Register numbers are stored in uppercase and must be unique.",
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
        left.add(UiTheme.label("Student Directory", Font.BOLD, 16, UiTheme.TEXT));

        UiTheme.styleField(searchField);
        searchField.setPreferredSize(new Dimension(240, 38));
        searchField.setToolTipText("Search by register number or name");
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

    private void addStudent() {
        try {
            store.addStudent(regField.getText(), nameField.getText());
            statusLabel.setForeground(UiTheme.SUCCESS);
            statusLabel.setText("Student added successfully.");
            regField.setText("");
            nameField.setText("");
            loadTable();
            regField.requestFocusInWindow();
        } catch (IllegalArgumentException ex) {
            statusLabel.setForeground(UiTheme.DANGER);
            statusLabel.setText(ex.getMessage());
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            statusLabel.setForeground(UiTheme.WARNING);
            statusLabel.setText("Select a student in the table first.");
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        String regNo = String.valueOf(tableModel.getValueAt(row, 0));
        String name = String.valueOf(tableModel.getValueAt(row, 1));

        int choice = JOptionPane.showConfirmDialog(this,
                "Delete " + name + " (" + regNo + ") and all recorded marks?",
                "Delete student", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        store.deleteStudent(regNo);
        statusLabel.setForeground(UiTheme.SUCCESS);
        statusLabel.setText("Student " + regNo + " deleted.");
        loadTable();
    }

    @Override
    public void refreshData() {
        loadTable();
    }

    private void loadTable() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        tableModel.setRowCount(0);

        List<Student> students = new ArrayList<>(store.getStudents());
        students.sort(Comparator.comparing(Student::getRegNo, String.CASE_INSENSITIVE_ORDER));

        for (Student student : students) {
            if (!query.isEmpty()
                    && !student.getRegNo().toLowerCase().contains(query)
                    && !student.getName().toLowerCase().contains(query)) {
                continue;
            }
            List<Mark> marks = store.marksFor(student.getRegNo());
            double total = 0;
            for (Mark mark : marks) {
                total += mark.getValue();
            }
            String average = marks.isEmpty() ? "-"
                    : RankingService.formatMark(RankingService.round2(total / marks.size()));
            tableModel.addRow(new Object[]{student.getRegNo(), student.getName(),
                    marks.size(), average});
        }
    }

    private void showError(RuntimeException ex) {
        statusLabel.setForeground(UiTheme.DANGER);
        statusLabel.setText("Could not save: " + ex.getMessage());
    }
}
