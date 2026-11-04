package ui;

import model.User;
import store.DataStore;
import store.RankingService;
import ui.panel.FacultyPanel;
import ui.panel.HomePanel;
import ui.panel.MarksPanel;
import ui.panel.RankPanel;
import ui.panel.StudentsPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final DataStore store;
    private final User currentUser;
    private final RankingService ranking;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Map<String, Refreshable> refreshables = new LinkedHashMap<>();
    private final Map<String, String> pageTitles = new LinkedHashMap<>();
    private final Map<String, String> pageSubtitles = new LinkedHashMap<>();

    private final JLabel pageTitle = UiTheme.label("Dashboard", Font.BOLD, 22, UiTheme.TEXT);
    private final JLabel pageSubtitle = UiTheme.label("", Font.PLAIN, 13, UiTheme.MUTED);

    private String activeCard = "home";

    public DashboardFrame(DataStore store, User currentUser) {
        this.store = store;
        this.currentUser = currentUser;
        this.ranking = new RankingService(store);

        setTitle("EduRank - Student Appraisal & Ranking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        java.awt.Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(1240, screen.width - 100);
        int height = Math.min(780, screen.height - 90);
        setSize(width, height);
        setMinimumSize(new Dimension(Math.min(1180, width), Math.min(660, height)));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);
        add(buildMainArea(), BorderLayout.CENTER);

        showCard("home");
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(UiTheme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(238, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(26, 18, 20, 18));

        JLabel logo = UiTheme.label("EduRank", Font.BOLD, 25, Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tag = UiTheme.label("RANKING SYSTEM", Font.BOLD, 10, UiTheme.SIDEBAR_MUTED);
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(logo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 2)));
        sidebar.add(tag);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        addNav(sidebar, "home", "Dashboard",
                "Overview of students, subjects and top performers.",
                new HomePanel(store, ranking));
        addNav(sidebar, "students", "Students",
                "Register students and review their academic summary.",
                new StudentsPanel(store));
        addNav(sidebar, "marks", "Marks",
                "Record or update subject marks for a student.",
                new MarksPanel(store));
        addNav(sidebar, "rank", "Rank List",
                "Look up any register number and see the live ranking.",
                new RankPanel(store, ranking));

        if (currentUser.getRole() == User.Role.ADMIN) {
            addNav(sidebar, "faculty", "Faculty",
                    "Manage admin and faculty login accounts.",
                    new FacultyPanel(store, currentUser));
        }

        sidebar.add(Box.createVerticalGlue());

        JPanel userCard = new JPanel();
        userCard.setLayout(new BoxLayout(userCard, BoxLayout.Y_AXIS));
        userCard.setBackground(UiTheme.SIDEBAR_HOVER);
        userCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        userCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        userCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel userName = UiTheme.label(currentUser.getDisplayName(), Font.BOLD, 13, Color.WHITE);
        userName.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel userMeta = UiTheme.label("@" + currentUser.getUsername() + "  |  " + currentUser.roleLabel(),
                Font.PLAIN, 11, UiTheme.SIDEBAR_MUTED);
        userMeta.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton logout = new JButton("Log out");
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.setFont(UiTheme.font(Font.BOLD, 12));
        logout.setForeground(UiTheme.SIDEBAR_TEXT);
        logout.setBackground(UiTheme.SIDEBAR_HOVER);
        logout.setOpaque(true);
        logout.setBorder(new EmptyBorder(8, 0, 6, 0));
        logout.setBorderPainted(false);
        logout.setFocusPainted(false);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        logout.addActionListener(e -> logout());

        userCard.add(userName);
        userCard.add(Box.createRigidArea(new Dimension(0, 3)));
        userCard.add(userMeta);
        userCard.add(Box.createRigidArea(new Dimension(0, 6)));
        userCard.add(logout);

        sidebar.add(userCard);
        return sidebar;
    }

    private void addNav(JPanel sidebar, String card, String title, String subtitle, JPanel panel) {
        cardPanel.add(panel, card);
        if (panel instanceof Refreshable refreshable) {
            refreshables.put(card, refreshable);
        }
        pageTitles.put(card, title);
        pageSubtitles.put(card, subtitle);

        JButton button = navButton(title);
        button.addActionListener(e -> showCard(card));
        navButtons.put(card, button);

        sidebar.add(button);
        sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
    }

    private JButton navButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UiTheme.font(Font.PLAIN, 14));
        button.setForeground(UiTheme.SIDEBAR_TEXT);
        button.setBackground(UiTheme.SIDEBAR);
        button.setOpaque(true);
        button.setBorder(new EmptyBorder(12, 16, 12, 16));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(JButton.LEFT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!text.equals(pageTitles.get(activeCard))) {
                    button.setBackground(UiTheme.SIDEBAR_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!text.equals(pageTitles.get(activeCard))) {
                    button.setBackground(UiTheme.SIDEBAR);
                }
            }
        });
        return button;
    }

    private JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(UiTheme.BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiTheme.BG);
        header.setBorder(new EmptyBorder(24, 28, 8, 28));

        JPanel titles = UiTheme.column();
        titles.add(pageTitle);
        titles.add(Box.createRigidArea(new Dimension(0, 4)));
        titles.add(pageSubtitle);

        JLabel badge = UiTheme.label(currentUser.roleLabel().toUpperCase(), Font.BOLD, 11,
                currentUser.getRole() == User.Role.ADMIN ? UiTheme.ACCENT_DARK : UiTheme.SUCCESS);
        badge.setOpaque(true);
        badge.setBackground(currentUser.getRole() == User.Role.ADMIN
                ? new Color(0xE8EFFF) : new Color(0xE7F8F0));
        badge.setBorder(new EmptyBorder(7, 14, 7, 14));

        header.add(titles, BorderLayout.WEST);
        header.add(badge, BorderLayout.EAST);

        cardPanel.setBackground(UiTheme.BG);
        cardPanel.setBorder(new EmptyBorder(8, 20, 20, 20));

        main.add(header, BorderLayout.NORTH);
        main.add(cardPanel, BorderLayout.CENTER);
        return main;
    }

    private void showCard(String card) {
        activeCard = card;
        cardLayout.show(cardPanel, card);
        pageTitle.setText(pageTitles.getOrDefault(card, ""));
        pageSubtitle.setText(pageSubtitles.getOrDefault(card, ""));

        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean active = entry.getKey().equals(card);
            JButton button = entry.getValue();
            button.setBackground(active ? UiTheme.SIDEBAR_ACTIVE : UiTheme.SIDEBAR);
            button.setForeground(active ? Color.WHITE : UiTheme.SIDEBAR_TEXT);
            button.setFont(UiTheme.font(active ? Font.BOLD : Font.PLAIN, 14));
        }

        Refreshable refreshable = refreshables.get(card);
        if (refreshable != null) {
            refreshable.refreshData();
        }
    }

    private void logout() {
        int choice = JOptionPane.showConfirmDialog(this, "Log out of EduRank?", "Confirm logout",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame(store).setVisible(true);
        }
    }
}
