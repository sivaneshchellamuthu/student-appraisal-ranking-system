package ui;

import exception.AppraisalException;
import model.RankEntry;
import model.User;
import service.MarksService;
import service.RankingService;
import service.StudentService;
import service.SubjectService;
import service.UserService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Landing screen after login. Shows live counts and the current top students.
 */
public class DashboardPanel extends JPanel {

    private final User currentUser;
    private final StudentService studentService = new StudentService();
    private final SubjectService subjectService = new SubjectService();
    private final MarksService marksService = new MarksService();
    private final UserService userService = new UserService();
    private final RankingService rankingService = new RankingService();

    private final JLabel studentCount = valueLabel();
    private final JLabel subjectCount = valueLabel();
    private final JLabel marksCount = valueLabel();
    private final JLabel userCount = valueLabel();
    private final JTextArea topperArea = new JTextArea(8, 40);
    private final JLabel statusLabel = new JLabel(" ");

    public DashboardPanel(User currentUser) {
        this.currentUser = currentUser;
        buildUi();
        refreshData();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(UiUtils.BACKGROUND);

        JPanel header = UiUtils.header("Dashboard",
                "Signed in as " + currentUser.getName() + " ("
                + currentUser.getRole() + ") - "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        JButton refreshButton = UiUtils.button("Refresh");
        refreshButton.addActionListener(event -> refreshData());
        header.add(refreshButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel cards = new JPanel(new GridLayout(1, 0, 16, 0));
        cards.setOpaque(false);
        cards.add(createCard("Students", studentCount));
        cards.add(createCard("Subjects", subjectCount));
        cards.add(createCard("Marks entered", marksCount));
        if (currentUser.isAdmin()) {
            cards.add(createCard("System users", userCount));
        }
        cards.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 95));
        content.add(cards);
        content.add(Box.createVerticalStrut(20));

        JLabel topTitle = new JLabel("Top 5 students by percentage");
        topTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        topTitle.setForeground(UiUtils.TEXT);
        topTitle.setAlignmentX(LEFT_ALIGNMENT);
        content.add(topTitle);
        content.add(Box.createVerticalStrut(8));

        topperArea.setEditable(false);
        topperArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        topperArea.setBackground(Color.WHITE);
        topperArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        topperArea.setLineWrap(false);
        JScrollPane topperScroll = UiUtils.scroll(topperArea);
        topperScroll.setAlignmentX(LEFT_ALIGNMENT);
        topperScroll.setMaximumSize(new java.awt.Dimension(
                Integer.MAX_VALUE, 210));
        content.add(topperScroll);
        content.add(Box.createVerticalStrut(16));

        JPanel legend = new JPanel(new BorderLayout(0, 6));
        legend.setBackground(Color.WHITE);
        legend.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 232)),
                new EmptyBorder(12, 14, 12, 14)));
        legend.setAlignmentX(LEFT_ALIGNMENT);
        legend.setMaximumSize(new java.awt.Dimension(
                Integer.MAX_VALUE, 95));

        JLabel legendTitle = new JLabel("Grade scale and pass rule");
        legendTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        legendTitle.setForeground(UiUtils.TEXT);

        JLabel legendText = new JLabel(
                "<html>O &gt;= 90 &nbsp;|&nbsp; A+ &gt;= 80 &nbsp;|&nbsp; "
                + "A &gt;= 70 &nbsp;|&nbsp; B+ &gt;= 60 &nbsp;|&nbsp; "
                + "B &gt;= 50 &nbsp;|&nbsp; C &gt;= 40 &nbsp;|&nbsp; F &lt; 40"
                + "<br>PASS requires overall percentage &gt;= 40 and no "
                + "individual subject mark below 40.</html>");
        legendText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        legendText.setForeground(UiUtils.MUTED);

        legend.add(legendTitle, BorderLayout.NORTH);
        legend.add(legendText, BorderLayout.CENTER);
        content.add(legend);

        add(content, BorderLayout.CENTER);

        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(UiUtils.MUTED);
        statusLabel.setBorder(new EmptyBorder(0, 18, 10, 18));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void refreshData() {
        try {
            studentCount.setText(String.valueOf(studentService.countStudents()));
            subjectCount.setText(String.valueOf(subjectService.countSubjects()));
            marksCount.setText(String.valueOf(marksService.countMarks()));
            if (currentUser.isAdmin()) {
                userCount.setText(String.valueOf(userService.countUsers(currentUser)));
            }

            List<RankEntry> toppers = rankingService.getToppers(5, null, null);
            if (toppers.isEmpty()) {
                topperArea.setText("No students found. Add students and marks to "
                        + "see the ranking preview.");
            } else {
                StringBuilder text = new StringBuilder();
                for (RankEntry entry : toppers) {
                    text.append(String.format("%-4s %-12s %-24s %7.2f%%   Grade %s%n",
                            "#" + entry.getRank(), entry.getRollNo(),
                            entry.getStudentName(), entry.getPercentage(),
                            entry.getGrade()));
                }
                topperArea.setText(text.toString());
                topperArea.setCaretPosition(0);
            }
            statusLabel.setForeground(UiUtils.SUCCESS);
            statusLabel.setText("Dashboard updated.");
        } catch (AppraisalException ex) {
            statusLabel.setForeground(UiUtils.DANGER);
            statusLabel.setText(ex.getMessage());
        }
    }

    private static JLabel valueLabel() {
        JLabel label = new JLabel("0");
        label.setFont(new Font("Segoe UI", Font.BOLD, 30));
        label.setForeground(UiUtils.PRIMARY);
        return label;
    }

    private static JPanel createCard(String title, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 232)),
                new EmptyBorder(14, 16, 14, 16)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(UiUtils.MUTED);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }
}
