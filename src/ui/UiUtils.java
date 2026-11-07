package ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Small reusable Swing helpers so every screen looks the same.
 */
public final class UiUtils {

    public static final Color PRIMARY = new Color(28, 53, 94);
    public static final Color ACCENT = new Color(0, 112, 192);
    public static final Color BACKGROUND = new Color(244, 246, 249);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(33, 37, 41);
    public static final Color MUTED = new Color(100, 108, 118);
    public static final Color SUCCESS = new Color(25, 111, 61);
    public static final Color DANGER = new Color(180, 40, 40);

    private static final Font TITLE_FONT =
            new Font("Segoe UI", Font.BOLD, 20);
    private static final Font SUBTITLE_FONT =
            new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font BODY_FONT =
            new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font BUTTON_FONT =
            new Font("Segoe UI", Font.BOLD, 12);

    private UiUtils() {
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION;
    }

    /**
     * Standard dark page header used at the top of every module.
     */
    public static JPanel header(String title, String subtitle) {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(PRIMARY);
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new javax.swing.BoxLayout(
                textPanel, javax.swing.BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(SUBTITLE_FONT);
        subtitleLabel.setForeground(new Color(205, 216, 231));

        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);
        header.add(textPanel, BorderLayout.CENTER);
        return header;
    }

    public static JButton button(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(7, 14, 7, 14));
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(new Color(225, 230, 237));
        button.setForeground(TEXT);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(7, 14, 7, 14));
        return button;
    }

    public static JLabel bodyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY_FONT);
        label.setForeground(TEXT);
        return label;
    }

    /** Table model whose cells cannot be edited directly. */
    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.setFont(BODY_FONT);
        table.setSelectionBackground(new Color(210, 228, 247));
        table.setSelectionForeground(TEXT);
        table.setGridColor(new Color(225, 229, 235));
        table.getTableHeader().setFont(
                new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(235, 239, 244));
        table.getTableHeader().setForeground(TEXT);
        table.getTableHeader().setPreferredSize(
                new Dimension(0, 30));
    }

    public static JScrollPane scroll(Component component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(BorderFactory.createLineBorder(
                new Color(215, 220, 227)));
        return scrollPane;
    }

    /** White form panel with a two-column GridBagLayout. */
    public static JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(CARD);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        return panel;
    }

    public static void addFormRow(JPanel panel, int row, String labelText,
                                  JComponent field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(5, 5, 5, 12);

        JLabel label = new JLabel(labelText);
        label.setFont(BODY_FONT);
        label.setForeground(TEXT);
        panel.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(5, 5, 5, 5);
        panel.add(field, fieldConstraints);
    }

    public static JPanel toolbar() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(
                java.awt.FlowLayout.LEFT, 8, 0));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(10, 0, 10, 0));
        return panel;
    }

    public static JPanel contentPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        return panel;
    }
}
