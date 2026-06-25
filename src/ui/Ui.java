package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public final class Ui {
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat MONEY =
            NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));

    private Ui() {
    }

    public static JPanel page(String title, String subtitle, JComponent actions) {
        JPanel page = new JPanel(new BorderLayout(0, 20));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(24, 26, 24, 26));

        JPanel heading = new JPanel(new BorderLayout(16, 0));
        heading.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.display(Font.BOLD, 27f));
        titleLabel.setForeground(Theme.INK);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(Theme.text(Font.PLAIN, 13f));
        subtitleLabel.setForeground(Theme.MUTED);
        subtitleLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
        text.add(titleLabel);
        text.add(subtitleLabel);
        heading.add(text, BorderLayout.WEST);
        if (actions != null) {
            heading.add(actions, BorderLayout.EAST);
        }
        page.add(heading, BorderLayout.NORTH);
        return page;
    }

    public static JPanel card() {
        JPanel panel = new RoundedPanel(22);
        panel.setLayout(new BorderLayout());
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(new EmptyBorder(17, 17, 19, 17));
        return panel;
    }

    public static JPanel toolbar(Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);
        for (Component component : components) {
            panel.add(component);
        }
        return panel;
    }

    public static JButton primaryButton(String text) {
        return new AnimatedButton(text, AnimatedButton.Type.PRIMARY);
    }

    public static JButton secondaryButton(String text) {
        return new AnimatedButton(text, AnimatedButton.Type.SECONDARY);
    }

    public static JButton dangerButton(String text) {
        return new AnimatedButton(text, AnimatedButton.Type.DANGER);
    }

    public static JTextField field(int columns) {
        JTextField field = new JTextField(columns);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 38));
        return field;
    }

    public static JTextField placeholder(JTextField field, String placeholder) {
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.getAccessibleContext().setAccessibleDescription(placeholder);
        return field;
    }

    public static <T extends JComponent> T describe(T component, String description) {
        component.setToolTipText(description);
        component.getAccessibleContext().setAccessibleDescription(description);
        if (component.getAccessibleContext().getAccessibleName() == null) {
            component.getAccessibleContext().setAccessibleName(description);
        }
        return component;
    }

    public static JTextArea area(int rows, int columns) {
        JTextArea area = new JTextArea(rows, columns);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(8, 10, 8, 10)));
        return area;
    }

    public static JScrollPane scroll(Component component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.SURFACE);
        return scroll;
    }

    public static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static JLabel badge(String text, Color background, Color foreground) {
        JLabel label = new PillLabel(text, background, foreground);
        label.setFont(Theme.text(Font.BOLD, 12f));
        label.setBorder(new EmptyBorder(5, 10, 5, 10));
        return label;
    }

    public static GridBagConstraints gbc(int x, int y) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = x == 1 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        constraints.weightx = x == 1 ? 1 : 0;
        constraints.insets = new Insets(6, 6, 6, 10);
        return constraints;
    }

    public static String money(BigDecimal amount) {
        return MONEY.format(amount == null ? BigDecimal.ZERO : amount) + " đ";
    }

    public static String date(LocalDate value) {
        return value == null ? "" : DATE.format(value);
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    public static BigDecimal decimal(String value, String fieldName) {
        try {
            String normalized = value == null ? "" : value.trim()
                    .replace("\u00A0", "")
                    .replace(" ", "");
            if (normalized.isEmpty()) {
                throw new NumberFormatException();
            }

            int comma = normalized.lastIndexOf(',');
            int dot = normalized.lastIndexOf('.');
            if (comma >= 0 && dot >= 0) {
                if (comma > dot) {
                    normalized = normalized.replace(".", "").replace(',', '.');
                } else {
                    normalized = normalized.replace(",", "");
                }
            } else if (comma >= 0) {
                normalized = normalized.replace(',', '.');
            } else if (dot >= 0 && looksLikeVietnameseGrouping(normalized)) {
                normalized = normalized.replace(".", "");
            }
            return new BigDecimal(normalized);
        } catch (Exception ex) {
            throw new IllegalArgumentException(fieldName + " phải là số hợp lệ.");
        }
    }

    private static boolean looksLikeVietnameseGrouping(String value) {
        String unsigned = value.startsWith("-") || value.startsWith("+")
                ? value.substring(1) : value;
        return unsigned.matches("\\d{1,3}(\\.\\d{3})+");
    }

    public static int integer(String value, String fieldName) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException(fieldName + " phải là số nguyên.");
        }
    }

    public static LocalDate localDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value.trim(), DATE);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    fieldName + " phải theo định dạng dd/MM/yyyy.");
        }
    }

    public static LocalDateTime localDateTime(String value, String fieldName) {
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    fieldName + " phải theo định dạng dd/MM/yyyy HH:mm.");
        }
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent, message, "Cafe24h", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null
                && (current.getMessage() == null || current.getMessage().isBlank())) {
            current = current.getCause();
        }
        String message = current.getMessage();
        JOptionPane.showMessageDialog(
                parent,
                message == null ? "Đã xảy ra lỗi không xác định." : message,
                "Không thể thực hiện",
                JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(
                parent,
                message,
                "Xác nhận",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static int selectedModelRow(JTable table) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            String tableName = table.getAccessibleContext().getAccessibleName();
            if (tableName != null && !tableName.isBlank()) {
                throw new IllegalArgumentException(
                        "Hãy chọn một dòng trong " + tableName + ".");
            }
            throw new IllegalArgumentException("Hãy chọn một dòng dữ liệu.");
        }
        return table.convertRowIndexToModel(viewRow);
    }
}
