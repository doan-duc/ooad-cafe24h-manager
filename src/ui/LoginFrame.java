package ui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import controller.AuthController;
import model.Employee;

public final class LoginFrame extends JFrame {
    private final AuthController controller = new AuthController();
    private final JTextField login = Ui.field(22);
    private final JPasswordField password = new JPasswordField(22);

    public LoginFrame() {
        super("Cafe24h · Đăng nhập");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);

        // FlatLaf client properties provide consistent field geometry.
        login.putClientProperty("JTextField.placeholderText", "Mã nhân viên hoặc SĐT");
        login.putClientProperty("JTextField.showClearButton", true);
        password.putClientProperty("JTextField.placeholderText", "Mật khẩu");
        password.putClientProperty("JTextField.showRevealButton", true);

        JPanel root = new JPanel(new java.awt.GridLayout(1, 2));
        JPanel story = new HeroPanel();
        story.setLayout(new BorderLayout());
        story.setBorder(new EmptyBorder(58, 52, 58, 52));

        JLabel brand = new JLabel("CAFE24H");
        brand.setForeground(Theme.ACCENT);
        brand.setFont(Theme.display(java.awt.Font.BOLD, 16f));
        story.add(brand, BorderLayout.NORTH);
        
        JLabel message = new JLabel("""
                <html><div style='width:230px'>
                <span style='font-size:24px;color:white'><b>Không gian cà phê.<br>
                Vận hành rõ ràng.</b></span><br><br>
                <span style='font-size:13px;color:#cbd5e1;line-height:1.4'>
                Quản lý bàn, order, pha chế, kho và doanh thu trong một ứng dụng.
                Mỗi vai trò chỉ thấy đúng công việc của mình.
                </span></div></html>
                """);

        JPanel glassCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        glassCard.setOpaque(false);
        glassCard.setBorder(new EmptyBorder(26, 26, 26, 26));
        glassCard.add(message, BorderLayout.CENTER);

        JPanel glassCenter = new JPanel(new GridBagLayout());
        glassCenter.setOpaque(false);
        glassCenter.add(glassCard);
        story.add(glassCenter, BorderLayout.CENTER);

        JLabel footer = new JLabel("SQL Server · Java Swing");
        footer.setForeground(new Color(148, 163, 184));
        story.add(footer, BorderLayout.SOUTH);

        JPanel loginPanel = new JPanel(new BorderLayout());
        loginPanel.setBackground(Theme.CANVAS);
        loginPanel.setBorder(new EmptyBorder(40, 58, 40, 58)); // slightly reduced top padding to accommodate selector
        
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new javax.swing.BoxLayout(
                heading, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Chào mừng trở lại");
        title.setFont(Theme.display(java.awt.Font.BOLD, 31f));
        title.setForeground(Theme.INK);
        JLabel subtitle = new JLabel("Đăng nhập bằng mã nhân viên hoặc số điện thoại.");
        subtitle.setForeground(Theme.MUTED);
        heading.add(title);
        heading.add(javax.swing.Box.createVerticalStrut(8));
        heading.add(subtitle);

        ThemeSelectorPanel themeSelector = new ThemeSelectorPanel(() -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        // Keep the theme selector above the welcome heading.
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setOpaque(false);
        
        JPanel selectorWrapper = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        selectorWrapper.setOpaque(false);
        selectorWrapper.add(themeSelector);
        
        northPanel.add(selectorWrapper, BorderLayout.NORTH);
        
        heading.setBorder(new EmptyBorder(12, 0, 0, 0));
        northPanel.add(heading, BorderLayout.CENTER);
        
        loginPanel.add(northPanel, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.add(new JLabel("Tài khoản"), Ui.gbc(0, 0));
        form.add(login, Ui.gbc(1, 0));
        form.add(new JLabel("Mật khẩu"), Ui.gbc(0, 1));
        form.add(password, Ui.gbc(1, 1));

        JButton submit = Ui.primaryButton("Đăng nhập");
        submit.addActionListener(event -> login(submit));
        password.addActionListener(event -> login(submit));
        form.add(submit, Ui.gbc(1, 2));

        JButton forgot = Ui.secondaryButton("Quên mật khẩu?");
        forgot.addActionListener(event -> forgotPassword());
        JPanel forgotRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        forgotRow.setOpaque(false);
        forgotRow.add(forgot);

        JPanel formWrapper = new JPanel(new BorderLayout(0, 8));
        formWrapper.setOpaque(false);
        formWrapper.add(form, BorderLayout.CENTER);
        formWrapper.add(forgotRow, BorderLayout.SOUTH);
        loginPanel.add(formWrapper, BorderLayout.CENTER);

        root.add(story);
        root.add(loginPanel);
        setContentPane(root);
    }

    /** Left panel of login screen — shows cafe photo with gradient overlay. */
    private static final class HeroPanel extends JPanel {
        private static final BufferedImage HERO =
                ImageCache.loadSync("/ui/images/cafe_hero.jpg");

        private HeroPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth();
            int h = getHeight();
            if (HERO != null) {
                g.drawImage(ImageCache.cover(HERO, w, h), 0, 0, null);
                // Dark overlay for text readability
                g.setComposite(AlphaComposite.SrcOver.derive(0.58f));
                g.setColor(new Color(8, 15, 12));
                g.fillRect(0, 0, w, h);
                // Theme-colour wash from bottom-left
                g.setComposite(AlphaComposite.SrcOver.derive(0.35f));
                g.setPaint(new LinearGradientPaint(
                        new Point2D.Float(0, h), new Point2D.Float(w, 0),
                        new float[]{0f, 1f},
                        new Color[]{
                                new Color(Theme.PRIMARY.getRed(), Theme.PRIMARY.getGreen(),
                                        Theme.PRIMARY.getBlue(), 200),
                                new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(),
                                        Theme.ACCENT.getBlue(), 40)
                        }));
                g.fillRect(0, 0, w, h);
            } else {
                // Fallback gradient
                g.setPaint(new LinearGradientPaint(
                        new Point2D.Float(0, 0), new Point2D.Float(w, h),
                        new float[]{0f, 0.55f, 1f},
                        new Color[]{Theme.SIDEBAR, Theme.CANVAS, Theme.SURFACE}));
                g.fillRect(0, 0, w, h);
            }
            g.dispose();
            // paint children on top — do NOT call super first (would overwrite with bg color)
        }
    }

    private void login(JButton button) {
        button.setEnabled(false);
        try {
            controller.xuLyDangNhap(login.getText(), password.getPassword());
            dispose();
            new MainFrame().setVisible(true);
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
            password.setText("");
            button.setEnabled(true);
        }
    }

    /** UC01.3: multi-step forgot-password dialog (find account → OTP → new password). */
    private void forgotPassword() {
        // Step 1: find account by phone or email
        JTextField contactField = Ui.field(24);
        contactField.putClientProperty("JTextField.placeholderText", "Số điện thoại hoặc email");
        JPanel step1 = new JPanel(new GridBagLayout());
        step1.add(new JLabel("Nhập SĐT hoặc email tài khoản:"), Ui.gbc(0, 0));
        step1.add(contactField, Ui.gbc(1, 0));

        Employee found;
        while (true) {
            if (JOptionPane.showConfirmDialog(
                    this, step1, "Quên mật khẩu — Bước 1/3",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                found = controller.findForReset(contactField.getText());
                break;
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }

        // Step 2: OTP (simulated — display code to user)
        String otpCode = controller.startOtp(found.maNV());
        JOptionPane.showMessageDialog(
                this,
                "<html>Mã OTP của bạn là:<br><b style='font-size:20px'>"
                        + otpCode + "</b><br><small>(Hiệu lực 5 phút)</small></html>",
                "Xác thực OTP — Giả lập",
                JOptionPane.INFORMATION_MESSAGE);

        JTextField otpField = Ui.field(10);
        otpField.putClientProperty("JTextField.placeholderText", "Nhập mã 6 số");
        JPanel step2 = new JPanel(new GridBagLayout());
        step2.add(new JLabel("Nhập mã OTP:"), Ui.gbc(0, 0));
        step2.add(otpField, Ui.gbc(1, 0));

        while (true) {
            if (JOptionPane.showConfirmDialog(
                    this, step2, "Quên mật khẩu — Bước 2/3",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                controller.verifyOtp(found.maNV(), otpField.getText());
                break;
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }

        // Step 3: new password
        JPasswordField newPwd = new JPasswordField(18);
        JPasswordField confirmPwd = new JPasswordField(18);
        newPwd.putClientProperty("JTextField.showRevealButton", true);
        confirmPwd.putClientProperty("JTextField.showRevealButton", true);
        JPanel step3 = new JPanel(new GridBagLayout());
        step3.add(new JLabel("Mật khẩu mới:"), Ui.gbc(0, 0));
        step3.add(newPwd, Ui.gbc(1, 0));
        step3.add(new JLabel("Xác nhận:"), Ui.gbc(0, 1));
        step3.add(confirmPwd, Ui.gbc(1, 1));

        while (true) {
            if (JOptionPane.showConfirmDialog(
                    this, step3, "Quên mật khẩu — Bước 3/3",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                controller.resetPassword(
                        found.maNV(), newPwd.getPassword(), confirmPwd.getPassword());
                JOptionPane.showMessageDialog(
                        this, "Đổi mật khẩu thành công. Hãy đăng nhập lại.",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                return;
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
                newPwd.setText("");
                confirmPwd.setText("");
            }
        }
    }
}
