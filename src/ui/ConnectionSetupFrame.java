package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import db.DatabaseConfig;
import db.DatabaseConfigRepository;
import db.Db;

public final class ConnectionSetupFrame extends JFrame {
    private final JTextField server = Ui.field(20);
    private final JTextField port = Ui.field(8);
    private final JTextField database = Ui.field(20);
    private final JComboBox<String> authentication =
            new JComboBox<>(new String[] {"Windows", "SQL Server"});
    private final JLabel authenticationHint = new JLabel();
    private final JLabel usernameLabel = new JLabel("Tài khoản SQL");
    private final JLabel passwordLabel = new JLabel("Mật khẩu SQL");
    private final JTextField username = Ui.field(20);
    private final JPasswordField password = new JPasswordField(20);

    public ConnectionSetupFrame() {
        this(null);
    }

    public ConnectionSetupFrame(String initialError) {
        super("Cafe24h · Kết nối SQL Server");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 650));
        setSize(new Dimension(900, 760));

        DatabaseConfig defaults = DatabaseConfig.defaults();
        server.setText(defaults.server());
        port.setText(Integer.toString(defaults.port()));
        database.setText(defaults.database());
        authentication.addActionListener(event -> updateAuthenticationFields());

        JPanel shell = new JPanel(new BorderLayout());
        shell.setBorder(new EmptyBorder(36, 58, 36, 58));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new javax.swing.BoxLayout(
                heading, javax.swing.BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("CAFE24H");
        brand.setForeground(Theme.PRIMARY);
        brand.setFont(Theme.display(java.awt.Font.BOLD, 14f));
        JLabel title = new JLabel("Kết nối cơ sở dữ liệu");
        title.setForeground(Theme.INK);
        title.setFont(Theme.display(java.awt.Font.BOLD, 30f));
        JLabel subtitle = new JLabel(
                "Thiết lập một lần để ứng dụng kết nối SQL Server.");
        subtitle.setForeground(Theme.MUTED);
        heading.add(brand);
        heading.add(javax.swing.Box.createVerticalStrut(8));
        heading.add(title);
        heading.add(javax.swing.Box.createVerticalStrut(7));
        heading.add(subtitle);
        javax.swing.JButton themeButton = Ui.secondaryButton(
                Theme.isDark() ? "Giao diện sáng" : "Giao diện tối");
        themeButton.addActionListener(event -> {
            Theme.toggle();
            dispose();
            new ConnectionSetupFrame(initialError).setVisible(true);
        });
        JPanel headingRow = new JPanel(new BorderLayout(16, 0));
        headingRow.setOpaque(false);
        headingRow.add(heading, BorderLayout.CENTER);
        JPanel themeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        themeWrap.setOpaque(false);
        themeWrap.add(themeButton);
        headingRow.add(themeWrap, BorderLayout.EAST);
        shell.add(headingRow, BorderLayout.NORTH);

        JPanel formCard = Ui.card();
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        addRow(form, 0, "Máy chủ", server);
        addRow(form, 1, "Cổng", port);
        addRow(form, 2, "Cơ sở dữ liệu", database);
        addRow(form, 3, "Xác thực", authentication);
        authenticationHint.setForeground(Theme.MUTED);
        form.add(authenticationHint, Ui.gbc(1, 4));

        usernameLabel.setForeground(Theme.INK);
        passwordLabel.setForeground(Theme.INK);
        form.add(usernameLabel, Ui.gbc(0, 5));
        form.add(username, Ui.gbc(1, 5));
        form.add(passwordLabel, Ui.gbc(0, 6));
        form.add(password, Ui.gbc(1, 6));

        JLabel hint = new JLabel(
                "<html>Khuyên dùng Windows Authentication trên máy cá nhân. "
                        + "Cơ sở dữ liệu mặc định là <b>Cafe24hDB</b>.</html>");
        hint.setForeground(Theme.MUTED);
        form.add(hint, Ui.gbc(1, 7));

        if (initialError != null && !initialError.isBlank()) {
            JLabel error = new JLabel(initialError);
            error.setForeground(Theme.DANGER);
            form.add(error, Ui.gbc(1, 8));
        }

        javax.swing.JButton connect = Ui.primaryButton("Kiểm tra và lưu kết nối");
        connect.addActionListener(event -> connect(connect));
        form.add(connect, Ui.gbc(1, 9));
        formCard.add(form, BorderLayout.CENTER);
        formCard.setBorder(new EmptyBorder(22, 24, 24, 24));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(28, 0, 0, 0));
        center.add(formCard, BorderLayout.NORTH);
        shell.add(center, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(shell);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        setContentPane(scroll);
        updateAuthenticationFields();
        setLocationRelativeTo(null);
    }

    private void connect(javax.swing.JButton button) {
        button.setEnabled(false);
        try {
            DatabaseConfig config = new DatabaseConfig(
                    server.getText().trim(),
                    Ui.integer(port.getText(), "Cổng"),
                    database.getText().trim(),
                    authentication.getSelectedIndex() == 0 ? "windows" : "sql",
                    username.getText().trim(),
                    new String(password.getPassword()),
                    true,
                    true);
            Db.testOrThrow(config);
            DatabaseConfigRepository.save(config);
            Db.configure(config);
            dispose();
            App.start();
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
            button.setEnabled(true);
        }
    }

    private void updateAuthenticationFields() {
        boolean sql = authentication.getSelectedIndex() == 1;
        usernameLabel.setVisible(sql);
        username.setVisible(sql);
        passwordLabel.setVisible(sql);
        password.setVisible(sql);
        authenticationHint.setText(sql
                ? "Nhập tài khoản được tạo trong SQL Server."
                : "Dùng tài khoản Windows hiện tại, không cần nhập mật khẩu SQL.");
        username.setToolTipText(sql ? "Tên đăng nhập SQL Server" : null);
        password.setToolTipText(sql ? "Mật khẩu SQL Server" : null);
        revalidate();
        repaint();
        if (sql) {
            javax.swing.SwingUtilities.invokeLater(username::requestFocusInWindow);
        }
    }

    private static void addRow(
            JPanel form, int row, String label, java.awt.Component component) {
        JLabel text = new JLabel(label);
        text.setForeground(Theme.INK);
        form.add(text, Ui.gbc(0, row));
        form.add(component, Ui.gbc(1, row));
    }
}
