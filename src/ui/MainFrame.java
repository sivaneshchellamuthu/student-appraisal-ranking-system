package ui;

import model.User;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Main application window.
 *
 * Role-based access is applied here: the User Management tab is only created
 * for ADMIN accounts. A FACULTY account never receives the panel at all.
 */
public class MainFrame extends JFrame {

    private final User currentUser;

    public MainFrame(User currentUser) {
        this.currentUser = currentUser;
        buildUi();
    }

    private void buildUi() {
        setTitle("Student Appraisal and Ranking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1250, 780);
        setMinimumSize(new Dimension(1050, 650));
        setLocationRelativeTo(null);

        JPanel header = UiUtils.header("Student Appraisal and Ranking System",
                "Signed in as " + currentUser.getName() + " ("
                + currentUser.getRole() + ")");

        JPanel userArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        userArea.setOpaque(false);
        JLabel roleLabel = new JLabel(currentUser.getRole().name());
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        roleLabel.setForeground(new java.awt.Color(205, 220, 240));
        JButton logoutButton = UiUtils.secondaryButton("Logout");
        logoutButton.addActionListener(event -> logout());
        userArea.add(roleLabel);
        userArea.add(logoutButton);
        header.add(userArea, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBorder(new EmptyBorder(8, 8, 8, 8));
        tabs.addTab("Dashboard", new DashboardPanel(currentUser));
        tabs.addTab("Students", new StudentPanel());
        tabs.addTab("Marks", new MarksPanel(currentUser));
        tabs.addTab("Ranking", new RankingPanel());
        tabs.addTab("Reports", new ReportPanel());

        // Role-based access: only ADMIN gets the User Management tab.
        if (currentUser.isAdmin()) {
            tabs.addTab("Users", new UserPanel(currentUser));
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiUtils.BACKGROUND);
        root.add(header, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void logout() {
        if (!UiUtils.confirm(this, "Log out of the system?")) {
            return;
        }
        dispose();
        new LoginFrame().setVisible(true);
    }
}
