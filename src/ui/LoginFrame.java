package ui;

import model.User;
import store.DataStore;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;

public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final DataStore store;
    private final JComboBox<String> roleCombo = new JComboBox<>(new String[]{"Admin", "Faculty"});
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JLabel statusLabel = UiTheme.label(" ", Font.PLAIN, 12, UiTheme.DANGER);
    private JButton loginButton;

    public LoginFrame(DataStore store) {
        this.store = store;

        setTitle("EduRank - Student Appraisal & Ranking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        java.awt.Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(980, screen.width - 100);
        int height = Math.min(620, screen.height - 90);
        setSize(width, height);
        setMinimumSize(new Dimension(Math.min(900, width), Math.min(560, height)));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildBrandPanel(), BorderLayout.WEST);
        add(buildFormPanel(), BorderLayout.CENTER);

        getRootPane().setDefaultButton(loginButton);
        usernameField.setText("admin");
        passwordField.setText("admin123");
        roleCombo.setSelectedItem("Admin");
        usernameField.requestFocusInWindow();
    }

    private JPanel buildBrandPanel() {
        JPanel brand = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x1F3470), 0, getHeight(), new Color(0x4F7CFF)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillOval(-90, getHeight() - 260, 340, 340);
                g2.fillOval(getWidth() - 150, -80, 280, 280);
                g2.dispose();
            }
        };
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setPreferredSize(new Dimension(410, 0));
        brand.setBorder(new EmptyBorder(58, 46, 44, 46));

        JLabel mark = new JLabel("ER", SwingConstants.CENTER);
        mark.setFont(UiTheme.font(Font.BOLD, 22));
        mark.setForeground(Color.WHITE);
        mark.setOpaque(true);
        mark.setBackground(new Color(255, 255, 255, 42));
        mark.setBorder(new EmptyBorder(14, 0, 14, 0));
        mark.setPreferredSize(new Dimension(56, 56));
        mark.setMaximumSize(new Dimension(56, 56));
        mark.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = UiTheme.label("EduRank", Font.BOLD, 34, Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("<html><div style='width:290px;'>Student appraisal and ranking, "
                + "built for a faster and more transparent campus.</div></html>");
        subtitle.setFont(UiTheme.font(Font.PLAIN, 14));
        subtitle.setForeground(new Color(0xD8E2FF));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel features = UiTheme.column();
        features.setAlignmentX(Component.LEFT_ALIGNMENT);
        features.add(feature("Role-based access for admin and faculty"));
        features.add(Box.createRigidArea(new Dimension(0, 12)));
        features.add(feature("Subject-wise and overall rankings"));
        features.add(Box.createRigidArea(new Dimension(0, 12)));
        features.add(feature("Instant rank lookup by register number"));

        JLabel footer = UiTheme.label("Java Swing  |  Local data store", Font.PLAIN, 12, new Color(0xB9C9EE));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);

        brand.add(mark);
        brand.add(Box.createRigidArea(new Dimension(0, 30)));
        brand.add(title);
        brand.add(Box.createRigidArea(new Dimension(0, 10)));
        brand.add(subtitle);
        brand.add(Box.createRigidArea(new Dimension(0, 40)));
        brand.add(features);
        brand.add(Box.createVerticalGlue());
        brand.add(footer);
        return brand;
    }

    private JPanel feature(String text) {
        JLabel label = UiTheme.label("-   " + text, Font.PLAIN, 14, new Color(0xE6EDFF));
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(label, BorderLayout.WEST);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        return panel;
    }

    private JPanel buildFormPanel() {
        JPanel side = new JPanel(new GridBagLayout());
        side.setBackground(UiTheme.CARD);
        side.setBorder(new EmptyBorder(40, 40, 40, 40));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(380, 470));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;

        int row = 0;

        JLabel heading = UiTheme.label("Welcome back", Font.BOLD, 27, UiTheme.TEXT);
        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 6, 0);
        form.add(heading, gc);

        JLabel sub = UiTheme.label("Sign in to open your dashboard.", Font.PLAIN, 13, UiTheme.MUTED);
        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 30, 0);
        form.add(sub, gc);

        JLabel roleLabel = UiTheme.label("ROLE", Font.BOLD, 11, UiTheme.MUTED);
        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 6, 0);
        form.add(roleLabel, gc);

        UiTheme.styleCombo(roleCombo);
        roleCombo.setPreferredSize(new Dimension(380, 42));
        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 16, 0);
        form.add(roleCombo, gc);

        JLabel userLabel = UiTheme.label("USERNAME", Font.BOLD, 11, UiTheme.MUTED);
        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 6, 0);
        form.add(userLabel, gc);

        UiTheme.styleField(usernameField);
        usernameField.setPreferredSize(new Dimension(380, 42));
        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 16, 0);
        form.add(usernameField, gc);

        JLabel passLabel = UiTheme.label("PASSWORD", Font.BOLD, 11, UiTheme.MUTED);
        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 6, 0);
        form.add(passLabel, gc);

        UiTheme.styleField(passwordField);
        passwordField.setPreferredSize(new Dimension(380, 42));
        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 8, 0);
        form.add(passwordField, gc);

        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 8, 0);
        form.add(statusLabel, gc);

        loginButton = UiTheme.primaryButton("Sign in to dashboard");
        loginButton.setPreferredSize(new Dimension(380, 44));
        loginButton.addActionListener(this::handleLogin);
        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 20, 0);
        form.add(loginButton, gc);

        JLabel hint = new JLabel("<html><div style='width:370px;color:#94A3B8;'>"
                + "Demo accounts: <b>admin / admin123</b> (Admin) &nbsp; and &nbsp; "
                + "<b>faculty / faculty123</b> (Faculty).</div></html>");
        hint.setFont(UiTheme.font(Font.PLAIN, 11));
        gc.gridy = row;
        gc.insets = new Insets(0, 0, 0, 0);
        form.add(hint, gc);

        side.add(form);
        return side;
    }

    private void handleLogin(ActionEvent event) {
        String role = (String) roleCombo.getSelectedItem();
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.isBlank() || password.isBlank()) {
            statusLabel.setText("Please enter both username and password.");
            return;
        }

        User user = store.authenticate(username, password);
        if (user == null) {
            statusLabel.setText("Invalid username or password.");
            passwordField.selectAll();
            passwordField.requestFocusInWindow();
            return;
        }
        if (!user.roleLabel().equals(role)) {
            statusLabel.setText("This account is registered as " + user.roleLabel() + ", not " + role + ".");
            return;
        }

        dispose();
        new DashboardFrame(store, user).setVisible(true);
    }
}
