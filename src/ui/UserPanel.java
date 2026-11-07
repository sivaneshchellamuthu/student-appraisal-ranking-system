package ui;

import exception.AppraisalException;
import model.User;
import service.UserService;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;

/**
 * Admin-only user management screen.
 * MainFrame does not add this tab for FACULTY accounts, and UserService also
 * rejects every operation from a non-admin user.
 */
public class UserPanel extends JPanel {

    private final User currentUser;
    private final UserService userService = new UserService();
    private final DefaultTableModel tableModel = UiUtils.readOnlyModel(
            "ID", "Username", "Full Name", "Role", "Active", "Created");
    private final JTable table = new JTable(tableModel);
    private final JLabel statusLabel = new JLabel(" ");

    public UserPanel(User currentUser) {
        this.currentUser = currentUser;
        buildUi();
        loadUsers();
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(UiUtils.BACKGROUND);
        add(UiUtils.header("User Management",
                "Create, edit and delete accounts and assign ADMIN or FACULTY roles"),
                BorderLayout.NORTH);

        JPanel content = UiUtils.contentPanel();

        JPanel toolbar = UiUtils.toolbar();
        JButton addButton = UiUtils.button("Add User");
        JButton editButton = UiUtils.button("Edit");
        JButton deleteButton = UiUtils.secondaryButton("Delete");
        JButton refreshButton = UiUtils.secondaryButton("Refresh");
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(deleteButton);
        toolbar.add(refreshButton);
        content.add(toolbar, BorderLayout.NORTH);

        table.setAutoCreateRowSorter(true);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        UiUtils.styleTable(table);
        table.removeColumn(table.getColumnModel().getColumn(0));
        content.add(UiUtils.scroll(table), BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(UiUtils.MUTED);
        statusLabel.setBorder(new EmptyBorder(0, 18, 10, 18));
        add(statusLabel, BorderLayout.SOUTH);

        addButton.addActionListener(event -> showUserDialog(null));
        editButton.addActionListener(event -> editSelected());
        deleteButton.addActionListener(event -> deleteSelected());
        refreshButton.addActionListener(event -> loadUsers());
    }

    private void loadUsers() {
        try {
            List<User> users = userService.getAllUsers(currentUser);
            tableModel.setRowCount(0);
            for (User user : users) {
                tableModel.addRow(new Object[]{
                        user.getId(),
                        user.getUsername(),
                        user.getName(),
                        user.getRole().name(),
                        user.isActive() ? "Yes" : "No",
                        user.getCreatedAt()
                });
            }
            statusLabel.setForeground(UiUtils.SUCCESS);
            statusLabel.setText(users.size() + " user account(s) loaded.");
        } catch (AppraisalException ex) {
            statusLabel.setForeground(UiUtils.DANGER);
            statusLabel.setText(ex.getMessage());
            UiUtils.error(this, ex.getMessage());
        }
    }

    private User selectedUser() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        User user = new User();
        user.setId(Integer.parseInt(
                String.valueOf(tableModel.getValueAt(modelRow, 0))));
        user.setUsername(String.valueOf(tableModel.getValueAt(modelRow, 1)));
        user.setName(String.valueOf(tableModel.getValueAt(modelRow, 2)));
        user.setRole(User.Role.fromDatabase(
                String.valueOf(tableModel.getValueAt(modelRow, 3))));
        user.setActive("Yes".equals(
                String.valueOf(tableModel.getValueAt(modelRow, 4))));
        return user;
    }

    private void editSelected() {
        User selected = selectedUser();
        if (selected == null) {
            UiUtils.info(this, "Select a user to edit.");
            return;
        }
        showUserDialog(selected);
    }

    private void deleteSelected() {
        User selected = selectedUser();
        if (selected == null) {
            UiUtils.info(this, "Select a user to delete.");
            return;
        }
        if (!UiUtils.confirm(this, "Delete user '" + selected.getUsername()
                + "' (" + selected.getName() + ")?")) {
            return;
        }
        try {
            userService.deleteUser(currentUser, selected.getId());
            loadUsers();
        } catch (AppraisalException ex) {
            UiUtils.error(this, ex.getMessage());
        }
    }

    private void showUserDialog(User existing) {
        boolean editing = existing != null;
        JTextField usernameField = new JTextField(18);
        JTextField fullNameField = new JTextField(18);
        JComboBox<String> roleBox = new JComboBox<>(
                new String[]{"ADMIN", "FACULTY"});
        JPasswordField passwordField = new JPasswordField(18);
        JCheckBox activeBox = new JCheckBox("Account is active", true);
        activeBox.setOpaque(false);
        activeBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        if (editing) {
            usernameField.setText(existing.getUsername());
            fullNameField.setText(existing.getName());
            roleBox.setSelectedItem(existing.getRole().name());
            activeBox.setSelected(existing.isActive());
        }

        JPanel form = UiUtils.formPanel();
        UiUtils.addFormRow(form, 0, "Username *", usernameField);
        UiUtils.addFormRow(form, 1, "Full Name *", fullNameField);
        UiUtils.addFormRow(form, 2, "Role *", roleBox);
        UiUtils.addFormRow(form, 3, "Password *", passwordField);

        JLabel hint = new JLabel(editing
                ? "Leave the password blank to keep the current password."
                : "Minimum 6 characters. Stored as a SHA-256 hash.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(UiUtils.MUTED);
        UiUtils.addFormRow(form, 4, "", hint);
        UiUtils.addFormRow(form, 5, "Status", activeBox);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(this, form,
                    editing ? "Edit User" : "Add User",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }

            char[] rawPassword = passwordField.getPassword();
            String password = new String(rawPassword);
            try {
                String role = String.valueOf(roleBox.getSelectedItem());
                if (editing) {
                    userService.updateUser(currentUser, existing.getId(),
                            usernameField.getText(), fullNameField.getText(),
                            role, password, activeBox.isSelected());
                } else {
                    userService.createUser(currentUser,
                            usernameField.getText(), fullNameField.getText(),
                            role, password, activeBox.isSelected());
                }
                loadUsers();
                return;
            } catch (AppraisalException ex) {
                UiUtils.error(this, ex.getMessage());
            } finally {
                Arrays.fill(rawPassword, '\0');
            }
        }
    }
}
