package ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class UiTheme {

    public static final Color BG = new Color(0xF3F5FA);
    public static final Color CARD = Color.WHITE;
    public static final Color SIDEBAR = new Color(0x182132);
    public static final Color SIDEBAR_HOVER = new Color(0x223049);
    public static final Color SIDEBAR_ACTIVE = new Color(0x2C3F60);
    public static final Color SIDEBAR_TEXT = new Color(0xC9D6EA);
    public static final Color SIDEBAR_MUTED = new Color(0x8FA6C8);
    public static final Color ACCENT = new Color(0x4F7CFF);
    public static final Color ACCENT_DARK = new Color(0x3B62D9);
    public static final Color TEXT = new Color(0x1E293B);
    public static final Color MUTED = new Color(0x64748B);
    public static final Color BORDER = new Color(0xE2E8F0);
    public static final Color SUCCESS = new Color(0x16A34A);
    public static final Color DANGER = new Color(0xDC2626);
    public static final Color WARNING = new Color(0xD97706);
    public static final Color HEADER_BG = new Color(0xF8FAFC);
    public static final Color SELECT = new Color(0xE8EFFF);
    public static final Color ZEBRA = new Color(0xFAFBFD);
    public static final Color SOFT_ACCENT = new Color(0xE8EFFF);

    public static final String FONT_NAME = "Segoe UI";

    private UiTheme() {
    }

    public static Font font(int style, float size) {
        return new Font(FONT_NAME, style, Math.round(size));
    }

    public static void applyDefaults() {
        String[] keys = {
                "Label.font", "Button.font", "TextField.font", "PasswordField.font",
                "ComboBox.font", "Table.font", "TableHeader.font", "OptionPane.font",
                "OptionPane.messageFont", "OptionPane.buttonFont", "Spinner.font",
                "CheckBox.font", "RadioButton.font", "TitledBorder.font", "ToolTip.font"
        };
        for (String key : keys) {
            UIManager.put(key, font(Font.PLAIN, 13));
        }
        UIManager.put("Table.showGrid", Boolean.FALSE);
        UIManager.put("TableHeader.background", HEADER_BG);
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("Table.selectionBackground", SELECT);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("OptionPane.background", CARD);
        UIManager.put("Panel.background", BG);
    }

    public static JLabel label(String text, int style, float size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(style, size));
        label.setForeground(color);
        return label;
    }

    public static JPanel column() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        return panel;
    }

    public static JPanel row() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.X_AXIS));
        return panel;
    }

    public static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD);
        panel.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true),
                new EmptyBorder(18, 20, 18, 20)));
        return panel;
    }

    public static JPanel softCard() {
        JPanel panel = new JPanel();
        panel.setBackground(HEADER_BG);
        panel.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true),
                new EmptyBorder(12, 14, 12, 14)));
        return panel;
    }

    public static JPanel sectionHeader(String title, String subtitle) {
        JPanel panel = column();
        panel.add(label(title, Font.BOLD, 16, TEXT));
        if (subtitle != null && !subtitle.isEmpty()) {
            panel.add(javax.swing.Box.createRigidArea(new Dimension(0, 4)));
            panel.add(label(subtitle, Font.PLAIN, 12, MUTED));
        }
        return panel;
    }

    public static void styleField(JTextField field) {
        field.setFont(font(Font.PLAIN, 14));
        field.setForeground(TEXT);
        field.setBackground(Color.WHITE);
        field.setCaretColor(TEXT);
        field.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true),
                new EmptyBorder(9, 12, 9, 12)));
        field.setPreferredSize(new Dimension(180, 40));
    }

    public static void styleCombo(JComboBox<?> combo) {
        combo.setFont(font(Font.PLAIN, 14));
        combo.setForeground(TEXT);
        combo.setBackground(Color.WHITE);
        combo.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true),
                new EmptyBorder(3, 8, 3, 8)));
        combo.setPreferredSize(new Dimension(200, 40));
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static JButton primaryButton(String text) {
        JButton button = baseButton(text, ACCENT, Color.WHITE);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(ACCENT_DARK);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(ACCENT);
                }
            }
        });
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = baseButton(text, CARD, TEXT);
        button.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true),
                new EmptyBorder(10, 16, 10, 16)));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(HEADER_BG);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(CARD);
                }
            }
        });
        return button;
    }

    public static JButton dangerButton(String text) {
        JButton button = baseButton(text, DANGER, Color.WHITE);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(0xB91C1C));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(DANGER);
                }
            }
        });
        return button;
    }

    private static JButton baseButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(font(Font.BOLD, 13));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setOpaque(true);
        button.setBorder(new EmptyBorder(11, 18, 11, 18));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        styleTable(table);
        return table;
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(38);
        table.setFont(font(Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.setBackground(CARD);
        table.setSelectionBackground(SELECT);
        table.setSelectionForeground(TEXT);
        table.setGridColor(BORDER);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setFocusable(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(font(Font.BOLD, 11));
        header.setBackground(HEADER_BG);
        header.setForeground(MUTED);
        header.setPreferredSize(new Dimension(0, 40));
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setOpaque(true);
        headerRenderer.setBackground(HEADER_BG);
        headerRenderer.setForeground(MUTED);
        headerRenderer.setFont(font(Font.BOLD, 11));
        headerRenderer.setBorder(new EmptyBorder(0, 12, 0, 12));
        header.setDefaultRenderer(headerRenderer);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? CARD : ZEBRA);
                    setForeground(TEXT);
                }
                return component;
            }
        };
        table.setDefaultRenderer(Object.class, cellRenderer);
    }

    public static JScrollPane scroll(JTable table) {
        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getViewport().setBackground(CARD);
        pane.setBackground(CARD);
        return pane;
    }

    public static JScrollPane scroll(Component component) {
        JScrollPane pane = new JScrollPane(component);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getViewport().setBackground(BG);
        pane.setBackground(BG);
        return pane;
    }
}
