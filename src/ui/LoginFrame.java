package ui;

import exception.AppraisalException;
import model.User;
import service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.Arrays;

/**
 * Login screen. The role (ADMIN or FACULTY) is read from the database and
 * passed to the main window, which uses it to hide the Users tab.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService = new AuthService();
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JLabel statusLabel = new JLabel("Checking database connection...");

    public LoginFrame() {
        buildUi();
        checkDatabase();
    }

    private void buildUi() {
        setTitle("Student Appraisal and Ranking System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(820, 480);
        setMinimumSize(new Dimension(760, 440));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(createBrandPanel(), BorderLayout.WEST);
        root.add(createFormPanel(), BorderLayout.CENTER);
        setContentPane(root);

        getRootPane().setDefaultButton(loginButton);
    }

    private JButton loginButton;
    private JButton exitButton;

    private JPanel createBrandPanel() {
        JPanel brand = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(18, 42, 79),
                        getWidth(), getHeight(), new Color(0, 108, 176)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        brand.setPreferredSize(new Dimension(315, 0));
        brand.setLayout(new GridBagLayout());

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel badge = new JLabel("CGB1201");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 13));
        badge.setForeground(new Color(150, 210, 255));

        JLabel title = new JLabel(
                "<html>Student Appraisal<br>and Ranking<br>System</html>");
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel(
                "<html>Java Programming course project<br>"
                + "Swing UI - JDBC - MySQL</html>");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(205, 225, 245));

        text.add(badge);
        text.add(Box.createVerticalStrut(14));
        text.add(title);
        text.add(Box.createVerticalStrut(18));
        text.add(subtitle);
        text.add(Box.createVerticalStrut(34));

        JLabel modules = new JLabel(
                "<html>Students - Subjects - Marks<br>"
                + "Appraisal - Ranking - Reports<br>"
                + "Secure role-based user management</html>");
        modules.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        modules.setForeground(new Color(175, 205, 235));
        text.add(modules);

        brand.add(text);
        return brand;
    }

    private JPanel createFormPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(10, 45, 10, 45));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 4, 0);

        JLabel heading = new JLabel("Welcome back");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 24));
        heading.setForeground(UiUtils.TEXT);
        form.add(heading, constraints);

        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 22, 0);
        JLabel subheading = new JLabel("Sign in to continue");
        subheading.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subheading.setForeground(UiUtils.MUTED);
        form.add(subheading, constraints);

        constraints.gridwidth = 1;
        constraints.gridy = 2;
        constraints.insets = new Insets(6, 0, 6, 10);
        form.add(label("Username"), constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        configureField(usernameField);
        form.add(usernameField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(6, 0, 6, 10);
        form.add(label("Password"), constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        configureField(passwordField);
        form.add(passwordField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(20, 0, 10, 0);

        JPanel buttons = new JPanel(new BorderLayout(10, 0));
        buttons.setOpaque(false);
        loginButton = UiUtils.button("Login");
        loginButton.setPreferredSize(new Dimension(0, 38));
        exitButton = UiUtils.secondaryButton("Exit");
        exitButton.setPreferredSize(new Dimension(90, 38));
        loginButton.addActionListener(event -> attemptLogin());
        exitButton.addActionListener(event -> System.exit(0));

        buttons.add(loginButton, BorderLayout.CENTER);
        buttons.add(exitButton, BorderLayout.EAST);
        form.add(buttons, constraints);

        constraints.gridy = 5;
        constraints.insets = new Insets(6, 0, 4, 0);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(UiUtils.MUTED);
        form.add(statusLabel, constraints);

        constraints.gridy = 6;
        constraints.insets = new Insets(16, 0, 0, 0);
        JLabel hint = new JLabel(
                "<html>First run: execute schema.sql, then sign in with "
                + "<b>admin / admin123</b>.</html>");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(UiUtils.MUTED);
        form.add(hint, constraints);

        usernameField.addActionListener(event -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(event -> attemptLogin());

        wrapper.add(form);
        return wrapper;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(UiUtils.TEXT);
        return label;
    }

    private void configureField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(230, 34));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 208, 218)),
                new EmptyBorder(4, 8, 4, 8)));
    }

    private void checkDatabase() {
        Thread thread = new Thread(() -> {
            boolean connected = authService.testConnection();
            SwingUtilities.invokeLater(() -> {
                if (connected) {
                    statusLabel.setText("Database connected (student_db)");
                    statusLabel.setForeground(UiUtils.SUCCESS);
                } else {
                    statusLabel.setText("Database not reachable - check MySQL "
                            + "and DataStore credentials");
                    statusLabel.setForeground(UiUtils.DANGER);
                }
            });
        }, "database-check");
        thread.setDaemon(true);
        thread.start();
    }

    private void attemptLogin() {
        usernameField.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        char[] rawPassword = passwordField.getPassword();
        try {
            User user = authService.login(usernameField.getText(),
                    new String(rawPassword));
            dispose();
            new MainFrame(user).setVisible(true);
        } catch (AppraisalException ex) {
            UiUtils.error(this, ex.getMessage());
            passwordField.selectAll();
            passwordField.requestFocusInWindow();
        } finally {
            Arrays.fill(rawPassword, '\0');
            usernameField.setCursor(Cursor.getDefaultCursor());
        }
    }
}
