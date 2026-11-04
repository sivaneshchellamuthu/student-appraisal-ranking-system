package ui.panel;

import model.User;
import store.DataStore;
import ui.Refreshable;
import ui.UiTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FacultyPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final DataStore store;
    private final User currentUser;

    private final JTextField usernameField = new JTextField();
    private final JTextField displayNameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JComboBox<String> roleCombo = new JComboBox<>(new String[]{"Faculty", "Admin"});
    private final JLabel statusLabel = UiTheme.label(" ", Font.PLAIN, 12, UiTheme.MUTED);

    private final javax.swing.table.DefaultTableModel tableModel =
            UiTheme.tableModel("Username", "Display Name", "Role");
    private final JTable table = UiTheme.table(tableModel);

    public FacultyPanel(DataStore store, User currentUser) {
        this.store = store;
        this.currentUser = currentUser;

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

        UiTheme.styleField(usernameField);
        usernameField.setPreferredSize(new Dimension(140, 40));

        UiTheme.styleField(displayNameField);
        displayNameField.setPreferredSize(new Dimension(200, 40));

        UiTheme.styleField(passwordField);
        passwordField.setPreferredSize(new Dimension(140, 40));

        UiTheme.styleCombo(roleCombo);
        roleCombo.setPreferredSize(new Dimension(120, 40));

        JButton addButton = UiTheme.primaryButton("Add account");
        addButton.addActionListener(e -> addAccount());
        usernameField.addActionListener(e -> addAccount());
        displayNameField.addActionListener(e -> addAccount());
        passwordField.addActionListener(e -> addAccount());

        row1.add(UiTheme.label("Username", Font.BOLD, 12, UiTheme.MUTED));
        row1.add(usernameField);
        row1.add(Box.createRigidArea(new Dimension(4, 0)));
        row1.add(UiTheme.label("Display Name", Font.BOLD, 12, UiTheme.MUTED));
        row1.add(displayNameField);
        row1.add(Box.createRigidArea(new Dimension(4, 0)));
        row1.add(UiTheme.label("Password", Font.BOLD, 12, UiTheme.MUTED));
        row1.add(passwordField);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row2.setOpaque(false);
        row2.add(UiTheme.label("Role", Font.BOLD, 12, UiTheme.MUTED));
        row2.add(roleCombo);
        row2.add(Box.createRigidArea(new Dimension(4, 0)));
        row2.add(addButton);
        row2.add(UiTheme.label("New accounts can sign in immediately.",
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
        left.add(UiTheme.label("Login Accounts", Font.BOLD, 16, UiTheme.TEXT));
        left.add(UiTheme.label("Admins can manage accounts and data. Faculty can manage students and marks.",
                Font.PLAIN, 11, UiTheme.MUTED));

        JButton deleteButton = UiTheme.dangerButton("Delete selected");
        deleteButton.addActionListener(e -> deleteSelected());

        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(deleteButton, BorderLayout.EAST);

        card.add(toolbar, BorderLayout.NORTH);
        card.add(UiTheme.scroll(table), BorderLayout.CENTER);
        return card;
    }

    private void addAccount() {
        try {
            User.Role role = "Admin".equals(roleCombo.getSelectedItem())
                    ? User.Role.ADMIN : User.Role.FACULTY;
            store.addUser(usernameField.getText(), displayNameField.getText(),
                    new String(passwordField.getPassword()), role);
            statusLabel.setForeground(UiTheme.SUCCESS);
            statusLabel.setText("Account created successfully.");
            usernameField.setText("");
            displayNameField.setText("");
            passwordField.setText("");
            loadTable();
        } catch (IllegalArgumentException ex) {
            statusLabel.setForeground(UiTheme.DANGER);
            statusLabel.setText(ex.getMessage());
        } catch (RuntimeException ex) {
            statusLabel.setForeground(UiTheme.DANGER);
            statusLabel.setText("Could not save: " + ex.getMessage());
        }
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            statusLabel.setForeground(UiTheme.WARNING);
            statusLabel.setText("Select an account in the table first.");
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        String username = String.valueOf(tableModel.getValueAt(row, 0));
        User target = store.findUser(username);
        if (target == null) {
            return;
        }

        if (target.getUsername().equalsIgnoreCase(currentUser.getUsername())) {
            statusLabel.setForeground(UiTheme.DANGER);
            statusLabel.setText("You cannot delete the account you are signed in with.");
            return;
        }
        if (target.getRole() == User.Role.ADMIN && store.countAdmins() <= 1) {
            statusLabel.setForeground(UiTheme.DANGER);
            statusLabel.setText("At least one admin account must remain.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Delete the " + target.roleLabel() + " account \"" + target.getUsername() + "\"?",
                "Delete account", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        store.deleteUser(target.getUsername());
        statusLabel.setForeground(UiTheme.SUCCESS);
        statusLabel.setText("Account \"" + target.getUsername() + "\" deleted.");
        loadTable();
    }

    @Override
    public void refreshData() {
        loadTable();
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        List<User> users = new ArrayList<>(store.getUsers());
        users.sort(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER));
        for (User user : users) {
            tableModel.addRow(new Object[]{user.getUsername(), user.getDisplayName(), user.roleLabel()});
        }
    }
}
