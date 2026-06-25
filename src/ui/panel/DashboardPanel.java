package ui.panel;
import ui.PulseBadge;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.util.concurrent.ExecutionException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import controller.OperationsController;
import model.Employee;
import model.OperationsSnapshot;
import security.Authorization;
import security.Permission;
import security.Session;
import ui.Theme;
import ui.Ui;

public final class DashboardPanel extends JPanel {
    public DashboardPanel() {
        super(new BorderLayout());
        Employee user = Session.currentUser();
        JPanel page = Ui.page(
                "Xin chào, " + user.hoTen(),
                "Không gian làm việc dành cho " + user.tenVaiTro() + ".",
                new PulseBadge(
                        "Đang hoạt động",
                        Theme.isDark()
                                ? new Color(25, 74, 58)
                                : new Color(220, 252, 231),
                        Theme.PRIMARY_DARK));

        JPanel body = new JPanel(new BorderLayout(0, 20));
        body.setOpaque(false);

        JPanel hero = Ui.card();
        hero.setBackground(Theme.softPrimary());
        JLabel heroText = new JLabel(
                "<html><div style='width:700px'>"
                        + "<span style='font-size:22px;color:"
                        + html(Theme.INK)
                        + "'><b>Mỗi người đúng việc, mọi ca rõ ràng.</b></span><br><br>"
                        + "<span style='font-size:13px;color:"
                        + html(Theme.MUTED)
                        + "'>Menu bên trái được tạo từ vai trò của tài khoản. "
                        + "Các thao tác quan trọng còn được kiểm tra quyền thêm một lần "
                        + "tại controller trước khi gửi xuống SQL Server.</span>"
                        + "</div></html>");
        hero.add(heroText, BorderLayout.CENTER);
        body.add(hero, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);

        JPanel grid = new JPanel(new GridLayout(1, 5, 12, 0));
        grid.setOpaque(false);
        grid.setPreferredSize(new Dimension(0, 170));

        boolean canViewRevenue = Authorization.can(user, Permission.REPORT_VIEW);
        JLabel banLabel = addCard(grid, "Bàn đang dùng", "—",
                "Phiên sử dụng đang hoạt động.");
        JLabel monLabel = addCard(grid, "Món chờ xử lý", "—",
                "Chờ pha, đang pha hoặc chờ giao.");
        JLabel khoLabel = addCard(grid, "Kho cần chú ý", "—",
                "Nguyên liệu chạm mức cảnh báo.");
        JLabel caLabel = addCard(grid, "Ca đang mở", "—",
                "Nhân viên đang vận hành.");
        JLabel revenueLabel = addCard(grid, "Doanh thu hôm nay",
                canViewRevenue ? "—" : "Theo quyền",
                canViewRevenue
                        ? "Tổng hóa đơn đã thanh toán."
                        : "Chỉ quản lý và chủ quán được xem.");
        content.add(grid, BorderLayout.NORTH);

        JPanel lower = new JPanel(new GridLayout(1, 2, 16, 0));
        lower.setOpaque(false);
        lower.add(guideCard(user));
        JLabel priorityText = new JLabel(
                "<html><div style='width:400px;color:" + html(Theme.MUTED) + "'>"
                        + "<b>1.</b> Đang tải dữ liệu vận hành..."
                        + "</div></html>");
        priorityText.setBorder(new EmptyBorder(14, 0, 0, 0));
        lower.add(operationsCard(priorityText));
        content.add(lower, BorderLayout.CENTER);
        body.add(content, BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        add(page);

        new SwingWorker<OperationsSnapshot, Void>() {
            @Override
            protected OperationsSnapshot doInBackground() {
                return new OperationsController().snapshot();
            }

            @Override
            protected void done() {
                try {
                    OperationsSnapshot snapshot = get();
                    banLabel.setText(Integer.toString(snapshot.banDangDung()));
                    monLabel.setText(Integer.toString(snapshot.monChoXuLy()));
                    khoLabel.setText(Integer.toString(snapshot.nguyenLieuSapHet()));
                    caLabel.setText(Integer.toString(snapshot.caDangMo()));
                    if (canViewRevenue) {
                        revenueLabel.setText(Ui.money(snapshot.doanhThuHomNay()));
                    }
                    priorityText.setText(
                            "<html><div style='width:400px;color:" + html(Theme.MUTED) + "'>"
                                    + buildPriorityText(snapshot)
                                    + "</div></html>");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    banLabel.setText("?");
                    monLabel.setText("?");
                    khoLabel.setText("?");
                    caLabel.setText("?");
                    if (canViewRevenue) {
                        revenueLabel.setText("?");
                    }
                }
            }
        }.execute();
    }

    private static JLabel addCard(
            JPanel grid, String eyebrow, String initialValue, String description) {
        JPanel card = Ui.card();
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new javax.swing.BoxLayout(
                content, javax.swing.BoxLayout.Y_AXIS));
        JLabel small = new JLabel(eyebrow.toUpperCase());
        small.setForeground(Theme.MUTED);
        small.setFont(small.getFont().deriveFont(Font.BOLD, 10f));
        JLabel main = new JLabel(initialValue);
        main.setForeground(Theme.INK);
        main.setFont(Theme.display(Font.BOLD, 22f));
        main.setBorder(new EmptyBorder(9, 0, 9, 0));
        JLabel detail = new JLabel(
                "<html><div style='width:220px'>" + description + "</div></html>");
        detail.setForeground(Theme.MUTED);
        content.add(small);
        content.add(main);
        content.add(detail);
        card.add(content, BorderLayout.CENTER);
        grid.add(card);
        return main;
    }

    private static JPanel guideCard(Employee user) {
        JPanel card = Ui.card();
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new javax.swing.BoxLayout(
                content, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Bắt đầu vận hành");
        title.setForeground(Theme.INK);
        title.setFont(Theme.display(Font.BOLD, 19f));
        JLabel text = new JLabel("<html><div style='width:400px;color:"
                + html(Theme.MUTED) + "'>"
                + guideText(user) + "</div></html>");
        text.setBorder(new EmptyBorder(14, 0, 0, 0));
        content.add(title);
        content.add(text);
        card.add(content, BorderLayout.NORTH);
        return card;
    }

    private static JPanel operationsCard(JLabel text) {
        JPanel card = Ui.card();
        card.setBackground(Theme.softAccent());
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new javax.swing.BoxLayout(
                content, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Ưu tiên trong ca");
        title.setForeground(Theme.INK);
        title.setFont(Theme.display(Font.BOLD, 19f));
        content.add(title);
        content.add(text);
        card.add(content, BorderLayout.NORTH);
        return card;
    }

    private static String buildPriorityText(OperationsSnapshot snapshot) {
        if (snapshot.monChoXuLy() > 0) {
            return "<b>1.</b> Bảng pha chế đang có "
                    + snapshot.monChoXuLy() + " món cần xử lý.<br><br>"
                    + "<b>2.</b> Ưu tiên món chờ lâu và món đã pha chưa giao.<br><br>"
                    + "<b>3.</b> Làm mới dữ liệu sau mỗi đợt phục vụ.";
        }
        if (snapshot.nguyenLieuSapHet() > 0) {
            return "<b>1.</b> Có " + snapshot.nguyenLieuSapHet()
                    + " nguyên liệu ở mức cảnh báo.<br><br>"
                    + "<b>2.</b> Kiểm tra tồn thực tế trước khi nhận thêm order.<br><br>"
                    + "<b>3.</b> Lập phiếu nhập hoặc kiểm kê khi cần.";
        }
        return "<b>1.</b> Chưa có cảnh báo vận hành nổi bật.<br><br>"
                + "<b>2.</b> Theo dõi bàn đang dùng và booking sắp tới.<br><br>"
                + "<b>3.</b> Làm mới tổng quan khi chuyển ca.";
    }

    private static String guideText(Employee user) {
        if (Authorization.can(user, Permission.EMPLOYEE_MANAGE)) {
            return "<b>1.</b> Tạo khu vực và bàn trong Thiết lập cửa hàng.<br><br>"
                    + "<b>2.</b> Khai báo nguyên liệu, menu và định mức.<br><br>"
                    + "<b>3.</b> Tạo tài khoản cho từng vị trí công việc.";
        }
        if (Authorization.can(user, Permission.ORDER_CREATE)) {
            return "<b>1.</b> Mở ca làm việc.<br><br>"
                    + "<b>2.</b> Check-in khách tại Sơ đồ bàn.<br><br>"
                    + "<b>3.</b> Gọi món, thanh toán và đánh dấu bàn cần dọn.";
        }
        if (Authorization.can(user, Permission.KITCHEN_OPERATE)) {
            return "<b>1.</b> Mở ca làm việc.<br><br>"
                    + "<b>2.</b> Theo dõi món mới tại Bảng pha chế.<br><br>"
                    + "<b>3.</b> Cập nhật pha chế và kiểm kê tồn kho.";
        }
        return "<b>1.</b> Theo dõi trạng thái bàn.<br><br>"
                + "<b>2.</b> Duyệt phiếu kiểm kê.<br><br>"
                + "<b>3.</b> Xem báo cáo và lịch sử ca.";
    }

    private static String html(Color color) {
        return String.format("#%02x%02x%02x",
                color.getRed(), color.getGreen(), color.getBlue());
    }
}
