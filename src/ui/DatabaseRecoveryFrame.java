package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public final class DatabaseRecoveryFrame extends JFrame {
    public DatabaseRecoveryFrame(int employeeCount) {
        super("Cafe24h · Cần khởi tạo lại dữ liệu");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 520));
        setSize(820, 570);
        setLocationRelativeTo(null);

        JPanel shell = new JPanel(new BorderLayout(0, 24));
        shell.setBorder(new EmptyBorder(42, 54, 42, 54));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new javax.swing.BoxLayout(
                heading, javax.swing.BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("CƠ SỞ DỮ LIỆU CŨ");
        eyebrow.setForeground(Theme.ACCENT);
        eyebrow.setFont(Theme.text(Font.BOLD, 12f));
        JLabel title = new JLabel("Chưa thể đăng nhập");
        title.setForeground(Theme.INK);
        title.setFont(Theme.display(Font.BOLD, 30f));
        JLabel subtitle = new JLabel(
                "Ứng dụng phát hiện " + employeeCount
                        + " nhân viên mẫu nhưng không có tài khoản bảo mật hợp lệ.");
        subtitle.setForeground(Theme.MUTED);
        heading.add(eyebrow);
        heading.add(javax.swing.Box.createVerticalStrut(8));
        heading.add(title);
        heading.add(javax.swing.Box.createVerticalStrut(8));
        heading.add(subtitle);
        shell.add(heading, BorderLayout.NORTH);

        JPanel card = Ui.card();
        JLabel instructions = new JLabel("""
                <html><div style='width:600px'>
                <b>Thực hiện trong SQL Server Management Studio:</b><br><br>
                <b>1.</b> Chạy <code>D:\\OOAD\\BTL_OOAD\\SQL\\Reset_Cafe24hDB.sql</code><br><br>
                <b>2.</b> Chạy <code>D:\\OOAD\\BTL_OOAD\\SQL\\Cafe24hDB.sql</code><br><br>
                <b>3.</b> Quay lại đây và chọn <b>Kiểm tra lại</b>.<br><br>
                Sau khi database mới có 0 nhân viên, ứng dụng sẽ mở màn hình
                tạo tài khoản Chủ quán đầu tiên.
                </div></html>
                """);
        instructions.setForeground(Theme.INK);
        card.add(instructions, BorderLayout.CENTER);
        shell.add(card, BorderLayout.CENTER);

        JButton retry = Ui.primaryButton("Kiểm tra lại");
        retry.addActionListener(event -> {
            dispose();
            App.start();
        });
        JButton close = Ui.secondaryButton("Thoát");
        close.addActionListener(event -> System.exit(0));
        shell.add(Ui.toolbar(retry, close), BorderLayout.SOUTH);
        setContentPane(shell);
    }
}
