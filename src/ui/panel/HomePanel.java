package ui.panel;

import store.DataStore;
import store.RankingService;
import ui.Refreshable;
import ui.UiTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

public class HomePanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final DataStore store;
    private final RankingService ranking;

    private final JLabel studentValue = statValue();
    private final JLabel subjectValue = statValue();
    private final JLabel markValue = statValue();
    private final JLabel facultyValue = statValue();

    private final DefaultTableModel topModel = UiTheme.tableModel("Rank", "Reg No", "Student", "Average");
    private final DefaultTableModel subjectModel = UiTheme.tableModel("Subject", "Average", "Students");

    public HomePanel(DataStore store, RankingService ranking) {
        this.store = store;
        this.ranking = ranking;

        setLayout(new BorderLayout(0, 16));
        setBackground(UiTheme.BG);
        setBorder(BorderFactory.createEmptyBorder());

        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.add(statCard("STUDENTS", studentValue, UiTheme.ACCENT));
        stats.add(statCard("SUBJECTS", subjectValue, new Color(0x8B5CF6)));
        stats.add(statCard("MARKS RECORDED", markValue, UiTheme.SUCCESS));
        stats.add(statCard("FACULTY ACCOUNTS", facultyValue, UiTheme.WARNING));
        add(stats, BorderLayout.NORTH);

        JPanel grid = new JPanel(new BorderLayout(16, 0));
        grid.setOpaque(false);

        JPanel topCard = UiTheme.card();
        topCard.setLayout(new BorderLayout(0, 14));
        topCard.add(UiTheme.sectionHeader("Top performers",
                "Students ranked by average marks across all recorded subjects."), BorderLayout.NORTH);
        JTable topTable = UiTheme.table(topModel);
        JScrollPane topScroll = UiTheme.scroll(topTable);
        topCard.add(topScroll, BorderLayout.CENTER);
        topCard.add(UiTheme.label("Showing the first 5 positions of the overall rank list.",
                Font.PLAIN, 11, UiTheme.MUTED), BorderLayout.SOUTH);

        JPanel subjectCard = UiTheme.card();
        subjectCard.setLayout(new BorderLayout(0, 14));
        subjectCard.setPreferredSize(new Dimension(360, 0));
        subjectCard.add(UiTheme.sectionHeader("Subject averages",
                "Class average for every subject recorded."), BorderLayout.NORTH);
        JTable subjectTable = UiTheme.table(subjectModel);
        subjectCard.add(UiTheme.scroll(subjectTable), BorderLayout.CENTER);

        grid.add(topCard, BorderLayout.CENTER);
        grid.add(subjectCard, BorderLayout.EAST);
        add(grid, BorderLayout.CENTER);

        refreshData();
    }

    private JLabel statValue() {
        return UiTheme.label("0", Font.BOLD, 24, UiTheme.TEXT);
    }

    private JPanel statCard(String title, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(UiTheme.CARD);
        card.setBorder(new CompoundBorder(new LineBorder(UiTheme.BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18)));

        JPanel bar = new JPanel();
        bar.setBackground(accent);
        bar.setPreferredSize(new Dimension(5, 0));

        JPanel content = UiTheme.column();
        content.add(UiTheme.label(title, Font.BOLD, 10, UiTheme.MUTED));
        content.add(Box.createRigidArea(new Dimension(0, 6)));
        content.add(value);

        card.add(bar, BorderLayout.WEST);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void refreshData() {
        studentValue.setText(String.valueOf(store.getStudents().size()));
        subjectValue.setText(String.valueOf(store.getSubjects().size()));
        markValue.setText(String.valueOf(store.getMarks().size()));
        facultyValue.setText(String.valueOf(store.countFaculty()));

        topModel.setRowCount(0);
        int shown = 0;
        for (RankingService.RankRow row : ranking.overallRankList()) {
            topModel.addRow(new Object[]{"#" + row.rank(), row.regNo(), row.name(),
                    RankingService.formatMark(row.average())});
            shown++;
            if (shown == 5) {
                break;
            }
        }

        subjectModel.setRowCount(0);
        for (RankingService.SubjectStat stat : ranking.subjectStats()) {
            subjectModel.addRow(new Object[]{stat.subject(),
                    RankingService.formatMark(stat.average()), stat.students()});
        }
    }
}
