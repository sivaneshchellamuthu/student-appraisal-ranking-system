package ui;

import exception.AppraisalException;
import model.RankEntry;
import model.Student;
import service.ReportService;
import service.StudentService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.print.PrinterException;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Report generation screen.
 *
 * Produces an individual report card, the class rank list and the topper list
 * in a print-friendly text view. The same data can be exported to CSV.
 */
public class ReportPanel extends JPanel {

    private enum Mode {
        NONE,
        REPORT_CARD,
        RANK_LIST
    }

    private final StudentService studentService = new StudentService();
    private final ReportService reportService = new ReportService();

    private final JComboBox<Student> studentBox = new JComboBox<>();
    private final JTextArea reportArea = new JTextArea();
    private final JLabel statusLabel = new JLabel(" ");

    private Mode mode = Mode.NONE;
    private String currentTitle = "";
    private ReportService.ReportCard currentCard;
    private List<RankEntry> currentEntries = new ArrayList<>();

    public ReportPanel() {
        buildUi();
        refreshStudents();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(UiUtils.BACKGROUND);
        add(UiUtils.header("Report Generation",
                "Report card, class rank list and topper list with CSV export "
                + "and printing"),
                BorderLayout.NORTH);

        JPanel content = UiUtils.contentPanel();

        JPanel controls = new JPanel(new GridLayout(2, 1, 0, 2));
        controls.setOpaque(false);

        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        firstRow.setOpaque(false);
        firstRow.add(UiUtils.bodyLabel("Student:"));
        studentBox.setPreferredSize(new Dimension(340, 28));
        firstRow.add(studentBox);
        JButton reportCardButton = UiUtils.button("Individual Report Card");
        firstRow.add(reportCardButton);

        JPanel secondRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        secondRow.setOpaque(false);
        JButton rankListButton = UiUtils.button("Class Rank List");
        JButton topperButton = UiUtils.button("Topper List (Top 10)");
        JButton refreshButton = UiUtils.secondaryButton("Refresh Students");
        JButton exportButton = UiUtils.secondaryButton("Export CSV");
        JButton printButton = UiUtils.secondaryButton("Print");
        secondRow.add(rankListButton);
        secondRow.add(topperButton);
        secondRow.add(refreshButton);
        secondRow.add(exportButton);
        secondRow.add(printButton);

        controls.add(firstRow);
        controls.add(secondRow);
        content.add(controls, BorderLayout.NORTH);

        reportArea.setEditable(false);
        reportArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        reportArea.setTabSize(4);
        reportArea.setBackground(java.awt.Color.WHITE);
        reportArea.setBorder(new EmptyBorder(12, 14, 12, 14));
        JScrollPane scroll = UiUtils.scroll(reportArea);
        content.add(scroll, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(UiUtils.MUTED);
        statusLabel.setBorder(new EmptyBorder(0, 18, 10, 18));
        add(statusLabel, BorderLayout.SOUTH);

        reportCardButton.addActionListener(event -> showReportCard());
        rankListButton.addActionListener(event -> showRankList());
        topperButton.addActionListener(event -> showTopperList());
        refreshButton.addActionListener(event -> refreshStudents());
        exportButton.addActionListener(event -> exportCsv());
        printButton.addActionListener(event -> printReport());

        reportArea.setText("Select a student or choose a report type.\n\n"
                + "The printed view uses a monospaced font so columns stay aligned.");
    }

    private void refreshStudents() {
        Integer selectedId = null;
        Student selected = (Student) studentBox.getSelectedItem();
        if (selected != null) {
            selectedId = selected.getId();
        }
        try {
            List<Student> students = studentService.getAllStudents();
            studentBox.removeAllItems();
            for (Student student : students) {
                studentBox.addItem(student);
            }
            if (selectedId != null) {
                for (int index = 0; index < studentBox.getItemCount(); index++) {
                    Student item = studentBox.getItemAt(index);
                    if (item.getId() == selectedId) {
                        studentBox.setSelectedIndex(index);
                        break;
                    }
                }
            } else if (studentBox.getItemCount() > 0) {
                studentBox.setSelectedIndex(0);
            }
            statusLabel.setForeground(UiUtils.SUCCESS);
            statusLabel.setText(students.size() + " student(s) loaded.");
        } catch (AppraisalException ex) {
            statusLabel.setForeground(UiUtils.DANGER);
            statusLabel.setText(ex.getMessage());
            UiUtils.error(this, ex.getMessage());
        }
    }

    private void showReportCard() {
        Student student = (Student) studentBox.getSelectedItem();
        if (student == null) {
            UiUtils.info(this, "Select a student first.");
            return;
        }
        try {
            currentCard = reportService.getReportCard(student.getId());
            currentEntries = new ArrayList<>();
            mode = Mode.REPORT_CARD;
            currentTitle = "Individual Report Card - " + student.getRollNo();
            setReportText(reportService.buildReportCardText(currentCard));
            statusLabel.setForeground(UiUtils.SUCCESS);
            statusLabel.setText("Report card generated for " + student.getRollNo() + ".");
        } catch (AppraisalException ex) {
            UiUtils.error(this, ex.getMessage());
        }
    }

    private void showRankList() {
        try {
            currentEntries = reportService.getClassRankList(null, null);
            currentCard = null;
            mode = Mode.RANK_LIST;
            currentTitle = "Class Rank List";
            setReportText(reportService.buildRankListText(
                    "CLASS RANK LIST", null, null, currentEntries));
            statusLabel.setForeground(UiUtils.SUCCESS);
            statusLabel.setText("Class rank list generated.");
        } catch (AppraisalException ex) {
            UiUtils.error(this, ex.getMessage());
        }
    }

    private void showTopperList() {
        try {
            currentEntries = reportService.getTopperList(10, null, null);
            currentCard = null;
            mode = Mode.RANK_LIST;
            currentTitle = "Topper List (Top 10)";
            setReportText(reportService.buildRankListText(
                    "TOPPER LIST (TOP 10)", null, null, currentEntries));
            statusLabel.setForeground(UiUtils.SUCCESS);
            statusLabel.setText("Topper list generated.");
        } catch (AppraisalException ex) {
            UiUtils.error(this, ex.getMessage());
        }
    }

    private void setReportText(String text) {
        reportArea.setText(text);
        reportArea.setCaretPosition(0);
    }

    private void exportCsv() {
        if (mode == Mode.NONE) {
            UiUtils.info(this, "Generate a report first.");
            return;
        }
        if (mode == Mode.REPORT_CARD && currentCard == null) {
            UiUtils.info(this, "Generate a report card first.");
            return;
        }
        if (mode == Mode.RANK_LIST && currentEntries.isEmpty()) {
            UiUtils.info(this, "The current list is empty.");
            return;
        }

        String defaultName = "class_rank_list.csv";
        if (mode == Mode.REPORT_CARD) {
            defaultName = "report_card_" + currentCard.getStudent().getRollNo() + ".csv";
        } else if (currentTitle.startsWith("TOPPER")) {
            defaultName = "topper_list.csv";
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Report as CSV");
        chooser.setSelectedFile(new File(defaultName));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        try {
            if (mode == Mode.REPORT_CARD) {
                reportService.exportReportCardCsv(file, currentCard);
            } else {
                reportService.exportRankListCsv(file, currentEntries);
            }
            UiUtils.info(this, "Report exported to:\n" + file.getAbsolutePath());
        } catch (AppraisalException ex) {
            UiUtils.error(this, ex.getMessage());
        }
    }

    private void printReport() {
        if (mode == Mode.NONE) {
            UiUtils.info(this, "Generate a report first.");
            return;
        }
        try {
            reportArea.print(new MessageFormat(currentTitle),
                    new MessageFormat("Page {0}"));
        } catch (PrinterException ex) {
            UiUtils.error(this, "Could not print the report: " + ex.getMessage());
        }
    }
}
