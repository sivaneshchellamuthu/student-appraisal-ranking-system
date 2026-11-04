package ui.panel;

import store.DataStore;
import store.RankingService;
import ui.Refreshable;
import ui.UiTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RankPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final DataStore store;
    private final RankingService ranking;

    private final JTextField searchField = new JTextField();
    private final JLabel searchStatus = UiTheme.label(" ", Font.PLAIN, 12, UiTheme.MUTED);

    private final JLabel studentName = UiTheme.label("No student selected", Font.BOLD, 18, UiTheme.TEXT);
    private final JLabel studentReg = UiTheme.label("Enter a register number above to view details.",
            Font.PLAIN, 12, UiTheme.MUTED);
    private final JLabel rankValue = statValue();
    private final JLabel averageValue = statValue();
    private final JLabel subjectCountValue = statValue();

    private final DefaultTableModel detailModel = UiTheme.tableModel("Subject", "Mark", "Subject Rank");
    private final JTable detailTable = UiTheme.table(detailModel);

    private final DefaultTableModel overallModel =
            UiTheme.tableModel("Rank", "Reg No", "Student", "Subjects", "Total", "Average");
    private final JTable overallTable = UiTheme.table(overallModel);

    public RankPanel(DataStore store, RankingService ranking) {
        this.store = store;
        this.ranking = ranking;

        setLayout(new BorderLayout(0, 16));
        setBackground(UiTheme.BG);
        setBorder(BorderFactory.createEmptyBorder());

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(buildSearchCard());
        top.add(Box.createRigidArea(new Dimension(0, 16)));
        top.add(buildDetailCard());
        add(top, BorderLayout.NORTH);

        add(buildOverallCard(), BorderLayout.CENTER);

        overallTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = overallTable.getSelectedRow();
                if (viewRow < 0) {
                    return;
                }
                int row = overallTable.convertRowIndexToModel(viewRow);
                searchField.setText(String.valueOf(overallModel.getValueAt(row, 1)));
                showReport(true);
            }
        });

        refreshData();
    }

    private JLabel statValue() {
        return UiTheme.label("-", Font.BOLD, 20, UiTheme.TEXT);
    }

    private JPanel buildSearchCard() {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 8));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);

        UiTheme.styleField(searchField);
        searchField.setPreferredSize(new Dimension(220, 40));
        searchField.setToolTipText("Enter a register number, e.g. 21CS001");
        searchField.addActionListener(e -> showReport(true));

        JButton searchButton = UiTheme.primaryButton("Get rank");
        searchButton.addActionListener(e -> showReport(true));

        JButton clearButton = UiTheme.secondaryButton("Clear");
        clearButton.addActionListener(e -> {
            searchField.setText("");
            searchStatus.setForeground(UiTheme.MUTED);
            searchStatus.setText(" ");
            clearDetail();
        });

        row.add(UiTheme.label("Register No", Font.BOLD, 12, UiTheme.MUTED));
        row.add(searchField);
        row.add(searchButton);
        row.add(clearButton);
        row.add(UiTheme.label("Tip: type a register number and press Enter.",
                Font.PLAIN, 11, UiTheme.MUTED));

        card.add(row, BorderLayout.CENTER);
        card.add(searchStatus, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildDetailCard() {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 12));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        JPanel head = new JPanel(new BorderLayout(16, 0));
        head.setOpaque(false);

        JPanel identity = UiTheme.column();
        identity.add(studentName);
        identity.add(Box.createRigidArea(new Dimension(0, 4)));
        identity.add(studentReg);

        JPanel stats = new JPanel(new GridLayout(1, 3, 10, 0));
        stats.setOpaque(false);
        stats.add(statBox("OVERALL RANK", rankValue));
        stats.add(statBox("AVERAGE MARK", averageValue));
        stats.add(statBox("SUBJECTS", subjectCountValue));

        head.add(identity, BorderLayout.WEST);
        head.add(stats, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        detailTable.setRowHeight(32);
        detailTable.setAutoCreateRowSorter(false);
        javax.swing.JScrollPane scroll = UiTheme.scroll(detailTable);
        scroll.setPreferredSize(new Dimension(0, 130));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel statBox(String title, JLabel value) {
        JPanel box = new JPanel(new BorderLayout(0, 4));
        box.setBackground(UiTheme.HEADER_BG);
        box.setBorder(new CompoundBorder(new LineBorder(UiTheme.BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)));
        box.setPreferredSize(new Dimension(130, 68));
        box.add(UiTheme.label(title, Font.BOLD, 9, UiTheme.MUTED), BorderLayout.NORTH);
        box.add(value, BorderLayout.CENTER);
        return box;
    }

    private JPanel buildOverallCard() {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(UiTheme.sectionHeader("Overall rank list",
                "Competition ranking by average marks across every recorded subject."), BorderLayout.NORTH);
        card.add(UiTheme.scroll(overallTable), BorderLayout.CENTER);
        return card;
    }

    private void showReport(boolean announceErrors) {
        String regNo = searchField.getText() == null ? "" : searchField.getText().trim();
        if (regNo.isEmpty()) {
            searchStatus.setForeground(UiTheme.WARNING);
            searchStatus.setText("Enter a register number first.");
            return;
        }

        RankingService.StudentReport report = ranking.report(regNo);
        if (report == null) {
            searchStatus.setForeground(UiTheme.DANGER);
            searchStatus.setText("No student found with register number " + regNo + ".");
            clearDetail();
            return;
        }

        if (announceErrors) {
            searchStatus.setForeground(UiTheme.SUCCESS);
            searchStatus.setText("Showing results for " + report.student().getName() + ".");
        }

        studentName.setText(report.student().getName());
        studentReg.setText("Register No: " + report.student().getRegNo());
        rankValue.setText(report.overallRank() == 0
                ? "Not ranked" : "#" + report.overallRank() + " of " + report.classSize());
        averageValue.setText(report.subjectRanks().isEmpty()
                ? "-" : RankingService.formatMark(report.average()));
        subjectCountValue.setText(String.valueOf(report.subjectRanks().size()));

        detailModel.setRowCount(0);
        for (RankingService.SubjectRank subjectRank : report.subjectRanks()) {
            detailModel.addRow(new Object[]{subjectRank.subject(),
                    RankingService.formatMark(subjectRank.mark()),
                    "#" + subjectRank.rank() + " of " + subjectRank.total()});
        }

        highlightRow(report.student().getRegNo());
    }

    private void highlightRow(String regNo) {
        for (int i = 0; i < overallModel.getRowCount(); i++) {
            if (String.valueOf(overallModel.getValueAt(i, 1)).equalsIgnoreCase(regNo)) {
                int viewIndex = overallTable.convertRowIndexToView(i);
                if (viewIndex < 0) {
                    return;
                }
                overallTable.setRowSelectionInterval(viewIndex, viewIndex);
                overallTable.scrollRectToVisible(overallTable.getCellRect(viewIndex, 0, true));
                return;
            }
        }
    }

    private void clearDetail() {
        studentName.setText("No student selected");
        studentReg.setText("Enter a register number above to view details.");
        rankValue.setText("-");
        averageValue.setText("-");
        subjectCountValue.setText("-");
        detailModel.setRowCount(0);
        overallTable.clearSelection();
    }

    @Override
    public void refreshData() {
        overallModel.setRowCount(0);
        for (RankingService.RankRow row : ranking.overallRankList()) {
            overallModel.addRow(new Object[]{"#" + row.rank(), row.regNo(), row.name(),
                    row.subjects(), RankingService.formatMark(row.total()),
                    RankingService.formatMark(row.average())});
        }
        if (!searchField.getText().isBlank() && store.findStudent(searchField.getText()) != null) {
            showReport(false);
        }
    }
}
