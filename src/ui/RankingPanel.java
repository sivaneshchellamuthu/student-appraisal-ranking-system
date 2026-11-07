package ui;

import exception.AppraisalException;
import model.RankEntry;
import service.RankingService;
import service.ReportService;
import service.StudentService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.print.PrinterException;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Ranking screen with department/section filters, CSV export and printing.
 */
public class RankingPanel extends JPanel {

    private final StudentService studentService = new StudentService();
    private final RankingService rankingService = new RankingService();
    private final ReportService reportService = new ReportService();

    private final JComboBox<String> departmentBox = new JComboBox<>();
    private final JComboBox<String> sectionBox = new JComboBox<>();
    private final DefaultTableModel tableModel = UiUtils.readOnlyModel(
            "Rank", "Roll No", "Name", "Department", "Year", "Section",
            "Total", "Percentage", "Attendance", "Grade", "Result");
    private final JTable table = new JTable(tableModel);
    private final JLabel statusLabel = new JLabel(" ");
    private List<RankEntry> currentEntries = new ArrayList<>();

    public RankingPanel() {
        buildUi();
        refreshFilters();
        generateRanking();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(UiUtils.BACKGROUND);
        add(UiUtils.header("Ranking Generation",
                "Rank by percentage; equal percentages share a rank "
                + "(tie-break: total, then attendance)"),
                BorderLayout.NORTH);

        JPanel content = UiUtils.contentPanel();

        JPanel filters = UiUtils.toolbar();
        filters.add(UiUtils.bodyLabel("Department:"));
        departmentBox.setPreferredSize(new java.awt.Dimension(150, 28));
        filters.add(departmentBox);
        filters.add(UiUtils.bodyLabel("Section:"));
        sectionBox.setPreferredSize(new java.awt.Dimension(100, 28));
        filters.add(sectionBox);

        JButton generateButton = UiUtils.button("Generate Ranking");
        JButton refreshButton = UiUtils.secondaryButton("Refresh Filters");
        JButton exportButton = UiUtils.secondaryButton("Export CSV");
        JButton printButton = UiUtils.secondaryButton("Print");
        filters.add(generateButton);
        filters.add(refreshButton);
        filters.add(exportButton);
        filters.add(printButton);
        content.add(filters, BorderLayout.NORTH);

        UiUtils.styleTable(table);
        content.add(UiUtils.scroll(table), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(UiUtils.MUTED);
        statusLabel.setBorder(new EmptyBorder(0, 18, 10, 18));
        add(statusLabel, BorderLayout.SOUTH);

        generateButton.addActionListener(event -> generateRanking());
        refreshButton.addActionListener(event -> {
            refreshFilters();
            generateRanking();
        });
        exportButton.addActionListener(event -> exportCsv());
        printButton.addActionListener(event -> printTable());
    }

    private void refreshFilters() {
        String selectedDepartment = (String) departmentBox.getSelectedItem();
        String selectedSection = (String) sectionBox.getSelectedItem();
        try {
            List<String> departments = studentService.getDepartments();
            List<String> sections = studentService.getSections();

            departmentBox.removeAllItems();
            departmentBox.addItem("All");
            for (String department : departments) {
                departmentBox.addItem(department);
            }
            sectionBox.removeAllItems();
            sectionBox.addItem("All");
            for (String section : sections) {
                sectionBox.addItem(section);
            }

            selectValue(departmentBox, selectedDepartment);
            selectValue(sectionBox, selectedSection);
        } catch (AppraisalException ex) {
            statusLabel.setForeground(UiUtils.DANGER);
            statusLabel.setText(ex.getMessage());
        }
    }

    private void selectValue(JComboBox<String> box, String value) {
        if (value != null) {
            for (int index = 0; index < box.getItemCount(); index++) {
                if (value.equals(box.getItemAt(index))) {
                    box.setSelectedIndex(index);
                    return;
                }
            }
        }
        if (box.getItemCount() > 0) {
            box.setSelectedIndex(0);
        }
    }

    private void generateRanking() {
        String department = (String) departmentBox.getSelectedItem();
        String section = (String) sectionBox.getSelectedItem();
        try {
            currentEntries = rankingService.generateRanking(department, section);
            tableModel.setRowCount(0);
            for (RankEntry entry : currentEntries) {
                tableModel.addRow(new Object[]{
                        entry.getRank(),
                        entry.getRollNo(),
                        entry.getStudentName(),
                        entry.getDepartment(),
                        entry.getYear(),
                        entry.getSection(),
                        String.format("%.2f", entry.getTotal()),
                        String.format("%.2f%%", entry.getPercentage()),
                        String.format("%.2f%%", entry.getAttendance()),
                        entry.getGrade(),
                        entry.getResultText()
                });
            }
            statusLabel.setForeground(UiUtils.SUCCESS);
            statusLabel.setText(currentEntries.size()
                    + " student(s) ranked. Equal percentage = shared rank.");
        } catch (AppraisalException ex) {
            statusLabel.setForeground(UiUtils.DANGER);
            statusLabel.setText(ex.getMessage());
            UiUtils.error(this, ex.getMessage());
        }
    }

    private void exportCsv() {
        if (currentEntries.isEmpty()) {
            UiUtils.info(this, "Generate a ranking first.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Rank List as CSV");
        chooser.setSelectedFile(new File("class_rank_list.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }
        try {
            reportService.exportRankListCsv(file, currentEntries);
            UiUtils.info(this, "Rank list exported to:\n" + file.getAbsolutePath());
        } catch (AppraisalException ex) {
            UiUtils.error(this, ex.getMessage());
        }
    }

    private void printTable() {
        if (currentEntries.isEmpty()) {
            UiUtils.info(this, "Generate a ranking first.");
            return;
        }
        try {
            table.print(JTable.PrintMode.FIT_WIDTH,
                    new MessageFormat("Class Rank List"),
                    new MessageFormat("Page {0}"));
        } catch (PrinterException ex) {
            UiUtils.error(this, "Could not print the ranking: " + ex.getMessage());
        }
    }
}
