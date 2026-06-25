package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import controller.AuthController;

public final class FirstRunFrame extends JFrame {
    private final AuthController controller = new AuthController();
    private final JTextField code = Ui.field(20);
    private final JTextField name = Ui.field(20);
    private final JTextField phone = Ui.field(20);
    private final JTextField email = Ui.field(20);
    private final JPasswordField password = new JPasswordField(20);
    private final JPasswordField confirmation = new JPasswordField(20);

    public FirstRunFrame() {
        super("Cafe24h · Khởi tạo hệ thống");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 650));
        setLocationRelativeTo(null);

        JPanel shell = new JPanel(new BorderLayout());
        shell.setBorder(new EmptyBorder(36, 58, 36, 58));
        JLabel title = new JLabel("Tạo tài khoản Chủ quán đầu tiên");
        title.setFont(Theme.display(java.awt.Font.BOLD, 29f));
        title.setForeground(Theme.INK);
        JLabel subtitle = new JLabel(
                "Danh sách nhân viên đang trống. Tài khoản này sẽ cấu hình và quản trị toàn bộ hệ thống.");
        subtitle.setForeground(Theme.MUTED);
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new javax.swing.BoxLayout(
                heading, javax.swing.BoxLayout.Y_AXIS));
        heading.add(title);
        heading.add(javax.swing.Box.createVerticalStrut(8));
        heading.add(subtitle);
        javax.swing.JButton themeButton = Ui.secondaryButton(
                Theme.isDark() ? "Giao diện sáng" : "Giao diện tối");
        themeButton.addActionListener(event -> {
            Theme.toggle();
            dispose();
            new FirstRunFrame().setVisible(true);
        });
        JPanel headingRow = new JPanel(new BorderLayout(16, 0));
        headingRow.setOpaque(false);
        headingRow.add(heading, BorderLayout.CENTER);
        headingRow.add(themeButton, BorderLayout.EAST);
        shell.add(headingRow, BorderLayout.NORTH);

        JPanel card = Ui.card();
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        addRow(form, 0, "Mã nhân viên", code);
        addRow(form, 1, "Họ và tên", name);
        addRow(form, 2, "Số điện thoại", phone);
        addRow(form, 3, "Email", email);
        addRow(form, 4, "Mật khẩu", password);
        addRow(form, 5, "Nhập lại mật khẩu", confirmation);

        javax.swing.JButton create = Ui.primaryButton("Khởi tạo hệ thống");
        create.addActionListener(event -> create(create));
        form.add(create, Ui.gbc(1, 6));
        card.add(form);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(28, 0, 0, 0));
        center.add(card, BorderLayout.NORTH);
        shell.add(center);
        setContentPane(shell);
    }

    private void create(javax.swing.JButton button) {
        button.setEnabled(false);
        try {
            controller.createFirstOwner(
                    code.getText(),
                    name.getText(),
                    phone.getText(),
                    email.getText(),
                    password.getPassword(),
                    confirmation.getPassword());
            Ui.info(this, "Đã tạo tài khoản Chủ quán. Hãy đăng nhập để bắt đầu.");
            dispose();
            new LoginFrame().setVisible(true);
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
            button.setEnabled(true);
        }
    }

    private static void addRow(
            JPanel form, int row, String label, java.awt.Component component) {
        form.add(new JLabel(label), Ui.gbc(0, row));
        form.add(component, Ui.gbc(1, row));
    }
}
