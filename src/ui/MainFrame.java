package ui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import controller.AuthController;
import model.Employee;
import security.Authorization;
import security.Permission;
import security.Session;
import ui.panel.BookingPanel;
import ui.panel.ConfigPanel;
import ui.panel.CustomerPanel;
import ui.panel.DashboardPanel;
import ui.panel.EmployeePanel;
import ui.panel.InventoryPanel;
import ui.panel.KitchenPanel;
import ui.panel.MenuPanel;
import ui.panel.ReportPanel;
import ui.panel.ShiftPanel;
import ui.panel.TableMapPanel;

public final class MainFrame extends JFrame {
    private final AuthController authController = new AuthController();
    private final CardLayout cards = new CardLayout();
    private final AnimatedCardPanel content = new AnimatedCardPanel(cards);
    private final JPanel navigation = new JPanel();
    private final Map<String, AnimatedNavButton> buttons = new LinkedHashMap<>();
    private final Map<String, Supplier<JComponent>> factories = new LinkedHashMap<>();
    private final Map<String, JComponent> pages = new LinkedHashMap<>();

    public MainFrame() {
        super("Cafe24h · Quản lý vận hành");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 740));
        setExtendedState(MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        Employee user = Session.currentUser();
        JPanel sidebar = buildSidebar(user);
        content.setOpaque(false);

        AmbientPanel root = new AmbientPanel();
        root.setLayout(new BorderLayout());
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        register("Tổng quan", DashboardPanel::new);
        registerIf(Permission.TABLE_VIEW, "Sơ đồ bàn", TableMapPanel::new);
        registerIf(Permission.KITCHEN_OPERATE, "Pha chế", KitchenPanel::new);
        registerIf(Permission.CUSTOMER_MANAGE, "Thành viên", CustomerPanel::new);
        registerIf(Permission.BOOKING_MANAGE, "Booking", BookingPanel::new);
        registerIf(Permission.INVENTORY_VIEW, "Kho nguyên liệu", InventoryPanel::new);
        registerIf(Permission.MENU_MANAGE, "Menu và định mức", MenuPanel::new);
        registerIf(Permission.EMPLOYEE_VIEW, "Nhân viên", EmployeePanel::new);
        registerIf(Permission.REPORT_VIEW, "Báo cáo", ReportPanel::new);
        if (Authorization.can(user, Permission.SHIFT_OPERATE)
                || Authorization.can(user, Permission.SHIFT_MANAGE)) {
            register("Ca làm việc", ShiftPanel::new);
        }
        registerIf(Permission.CONFIG_MANAGE, "Thiết lập cửa hàng", ConfigPanel::new);
        showPage("Tổng quan");
    }

    private JPanel buildSidebar(Employee user) {
        JPanel sidebar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color sideColor = Theme.isDark()
                        ? new Color(Theme.SIDEBAR.getRed(), Theme.SIDEBAR.getGreen(), Theme.SIDEBAR.getBlue(), 180)
                        : new Color(Theme.SIDEBAR.getRed(), Theme.SIDEBAR.getGreen(), Theme.SIDEBAR.getBlue(), 160);
                g2.setColor(sideColor);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.BORDER);
                g2.setComposite(AlphaComposite.SrcOver.derive(0.35f));
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(244, 0));
        sidebar.setBorder(new EmptyBorder(26, 18, 20, 18));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new javax.swing.BoxLayout(brand, javax.swing.BoxLayout.Y_AXIS));
        JLabel logo = new JLabel("CAFE24H");
        logo.setForeground(Theme.ACCENT);
        logo.setFont(Theme.display(Font.BOLD, 18f));
        JLabel caption = new JLabel("WORKSPACE");
        caption.setForeground(new Color(148, 163, 184));
        caption.setFont(caption.getFont().deriveFont(Font.BOLD, 10f));
        brand.add(logo);
        brand.add(javax.swing.Box.createVerticalStrut(3));
        brand.add(caption);
        sidebar.add(brand, BorderLayout.NORTH);

        navigation.setOpaque(false);
        navigation.setLayout(new javax.swing.BoxLayout(
                navigation, javax.swing.BoxLayout.Y_AXIS));
        navigation.setBorder(new EmptyBorder(34, 0, 18, 0));
        sidebar.add(navigation, BorderLayout.CENTER);

        JPanel profile = new JPanel();
        profile.setOpaque(false);
        profile.setLayout(new javax.swing.BoxLayout(
                profile, javax.swing.BoxLayout.Y_AXIS));
        profile.setBorder(BorderFactory.createMatteBorder(
                1, 0, 0, 0, Theme.BORDER));
        JLabel name = new JLabel(user.hoTen());
        name.setForeground(Color.WHITE);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 13f));
        name.setBorder(new EmptyBorder(15, 2, 2, 2));
        JLabel role = new JLabel(user.tenVaiTro());
        role.setForeground(new Color(148, 163, 184));
        role.setBorder(new EmptyBorder(0, 2, 10, 2));
        ThemeSelectorPanel themeSelector = new ThemeSelectorPanel(() -> {
            dispose();
            new MainFrame().setVisible(true);
        });

        JButton changePassword = new AnimatedButton("Đổi mật khẩu", AnimatedButton.Type.SECONDARY);
        changePassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        changePassword.addActionListener(event -> changePassword());

        JButton logout = new AnimatedButton("Đăng xuất", AnimatedButton.Type.SECONDARY);
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        logout.addActionListener(event -> logout());
        profile.add(name);
        profile.add(role);
        profile.add(themeSelector);
        profile.add(javax.swing.Box.createVerticalStrut(7));
        profile.add(changePassword);
        profile.add(javax.swing.Box.createVerticalStrut(4));
        profile.add(logout);
        sidebar.add(profile, BorderLayout.SOUTH);
        return sidebar;
    }

    private void registerIf(
            Permission permission, String name, Supplier<JComponent> factory) {
        if (Authorization.can(Session.currentUser(), permission)) {
            register(name, factory);
        }
    }

    private void register(String name, Supplier<JComponent> factory) {
        factories.put(name, factory);
        AnimatedNavButton button = new AnimatedNavButton(name);
        button.setFont(Theme.text(Font.BOLD, 13f));
        button.addActionListener(event -> showPage(name));
        navigation.add(button);
        navigation.add(javax.swing.Box.createVerticalStrut(4));
        buttons.put(name, button);
    }

    private void showPage(String name) {
        JComponent page = pages.get(name);
        if ("Tổng quan".equals(name) && page != null) {
            content.remove(page);
            pages.remove(name);
            page = null;
        }
        if (page == null) {
            try {
                page = factories.get(name).get();
                page.setOpaque(false);
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
                return;
            }
            pages.put(name, page);
            content.add(page, name);
        }
        // Làm tươi dữ liệu mỗi lần mở lại tab để không hiển thị dữ liệu cũ đã cache
        if (page instanceof Refreshable refreshable) {
            try {
                refreshable.onPageShown();
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }
        cards.show(content, name);
        content.animateIn();
        buttons.forEach((key, button) ->
                button.setSelectedState(key.equals(name)));
    }

    /** UC01.4: change password for the currently logged-in user (verify old → OTP → new). */
    private void changePassword() {
        // Step 1: enter current password + new password
        JPasswordField currentPwd = new JPasswordField(18);
        JPasswordField newPwd = new JPasswordField(18);
        JPasswordField confirmPwd = new JPasswordField(18);
        currentPwd.putClientProperty("JTextField.showRevealButton", true);
        newPwd.putClientProperty("JTextField.showRevealButton", true);
        confirmPwd.putClientProperty("JTextField.showRevealButton", true);

        JPanel step1 = new JPanel(new java.awt.GridBagLayout());
        step1.add(new JLabel("Mật khẩu hiện tại:"), Ui.gbc(0, 0));
        step1.add(currentPwd, Ui.gbc(1, 0));
        step1.add(new JLabel("Mật khẩu mới:"), Ui.gbc(0, 1));
        step1.add(newPwd, Ui.gbc(1, 1));
        step1.add(new JLabel("Xác nhận mật khẩu mới:"), Ui.gbc(0, 2));
        step1.add(confirmPwd, Ui.gbc(1, 2));

        String otpCode;
        char[] savedNewPwd;
        char[] savedConfirm;
        while (true) {
            if (JOptionPane.showConfirmDialog(
                    this, step1, "Đổi mật khẩu — Bước 1/2",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                otpCode = authController.initiatePasswordChange(currentPwd.getPassword());
                savedNewPwd = newPwd.getPassword();
                savedConfirm = confirmPwd.getPassword();
                break;
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
                currentPwd.setText("");
            }
        }

        // Step 2: OTP (simulated — display code to user)
        JOptionPane.showMessageDialog(
                this,
                "<html>Mã OTP của bạn là:<br><b style='font-size:20px'>"
                        + otpCode + "</b><br><small>(Hiệu lực 5 phút)</small></html>",
                "Xác thực OTP — Giả lập",
                JOptionPane.INFORMATION_MESSAGE);

        JTextField otpField = Ui.field(10);
        otpField.putClientProperty("JTextField.placeholderText", "Nhập mã 6 số");
        JPanel step2 = new JPanel(new java.awt.GridBagLayout());
        step2.add(new JLabel("Nhập mã OTP:"), Ui.gbc(0, 0));
        step2.add(otpField, Ui.gbc(1, 0));

        Employee user = Session.currentUser();
        while (true) {
            if (JOptionPane.showConfirmDialog(
                    this, step2, "Đổi mật khẩu — Bước 2/2",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                authController.verifyOtp(user.maNV(), otpField.getText());
                authController.resetPassword(user.maNV(), savedNewPwd, savedConfirm);
                JOptionPane.showMessageDialog(
                        this, "Đổi mật khẩu thành công.",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                return;
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }
    }

    private void logout() {
        if (!Ui.confirm(this, "Đăng xuất khỏi tài khoản hiện tại?")) {
            return;
        }
        Session.logout();
        dispose();
        new LoginFrame().setVisible(true);
    }

}
