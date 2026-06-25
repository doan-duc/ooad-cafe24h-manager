package ui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.HierarchyEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

import controller.OrderController;
import model.OrderLine;
import ui.Theme;
import ui.Ui;

public final class KitchenPanel extends JPanel implements ui.Refreshable {
    // Tóm tắt: Tải lại hàng đợi pha chế mỗi khi mở lại tab
    @Override
    public void onPageShown() {
        refresh();
    }

    private final OrderController controller = new OrderController();
    private final DefaultTableModel model = Ui.readOnlyModel(
            "ID", "Bàn", "Món", "SL", "Chờ (phút)", "Ghi chú", "Trạng thái");
    private final JTable table = Ui.table(model);
    private final JButton preparing = Ui.secondaryButton("Bắt đầu pha");
    private final JButton done = Ui.primaryButton("Đã pha xong");
    private final JButton delivered = Ui.secondaryButton("Đã giao cho khách");
    private List<OrderLine> lines = List.of();
    private final Timer autoRefresh = new Timer(30_000, e -> refresh());

    public KitchenPanel() {
        super(new BorderLayout());
        table.getAccessibleContext().setAccessibleName("bảng pha chế");
        JButton refresh = Ui.secondaryButton("Tải lại");
        Ui.describe(refresh, "Cập nhật danh sách món đang chờ pha chế hoặc chờ giao.");
        refresh.addActionListener(event -> refresh());
        JPanel page = Ui.page(
                "Bảng pha chế",
                "Danh sách món cần xử lý, tự động làm mới mỗi 30 giây.",
                refresh);

        JPanel card = Ui.card();
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        Ui.describe(preparing, "Đổi món đã chọn sang trạng thái Đang pha.");
        Ui.describe(done, "Báo món đã pha xong để phục vụ giao cho khách.");
        Ui.describe(delivered, "Đánh dấu món đã giao, món sẽ rời khỏi bảng pha chế.");
        preparing.addActionListener(event -> changeStatus("DangPha"));
        done.addActionListener(event -> changeStatus("DaPha"));
        delivered.addActionListener(event -> changeStatus("DaGiao"));
        table.getSelectionModel().addListSelectionListener(event -> updateActions());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(Ui.badge("Tự động làm mới mỗi 30 giây",
                new Color(236, 253, 245), Theme.PRIMARY_DARK));
        actions.add(preparing);
        actions.add(done);
        actions.add(delivered);
        card.add(actions, BorderLayout.SOUTH);
        page.add(card, BorderLayout.CENTER);
        add(page);
        refresh();

        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    autoRefresh.start();
                } else {
                    autoRefresh.stop();
                }
            }
        });
    }

    private void refresh() {
        try {
            lines = controller.kitchenLines();
            model.setRowCount(0);
            for (OrderLine line : lines) {
                model.addRow(new Object[] {
                        line.maCTHD(), line.tenBan(), line.tenMon(), line.soLuong(),
                        waitingMinutes(line), line.ghiChu(),
                        display(line.trangThaiMon())
                });
            }
            if (!lines.isEmpty()) {
                table.setRowSelectionInterval(0, 0);
            }
            updateActions();
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void updateActions() {
        boolean hasSelection = table.getSelectedRow() >= 0;
        preparing.setEnabled(hasSelection);
        done.setEnabled(hasSelection);
        delivered.setEnabled(hasSelection);
    }

    private void changeStatus(String status) {
        try {
            int row = Ui.selectedModelRow(table);
            controller.kitchenStatus(lines.get(row).maCTHD(), status);
            refresh();
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private static String display(String status) {
        return switch (status) {
            case "ChoPhaChe" -> "Chờ pha chế";
            case "DangPha" -> "Đang pha";
            case "DaPha" -> "Đã pha";
            default -> status;
        };
    }

    private static long waitingMinutes(OrderLine line) {
        if (line.thoiGianTao() == null) {
            return 0;
        }
        return Math.max(0, Duration.between(
                line.thoiGianTao(), LocalDateTime.now()).toMinutes());
    }
}
