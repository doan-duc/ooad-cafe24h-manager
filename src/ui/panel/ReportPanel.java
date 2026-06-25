package ui.panel;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import controller.ReportController;
import model.BestSeller;
import model.RevenueSummary;
import model.TableUsage;
import ui.Theme;
import ui.Ui;

public final class ReportPanel extends JPanel implements ui.Refreshable {
    // Tóm tắt: Tải lại báo cáo mỗi khi mở lại tab (cập nhật doanh thu mới nhất)
    @Override
    public void onPageShown() {
        refresh();
    }

    private final ReportController controller = new ReportController();
    private final JTextField from = Ui.field(12);
    private final JTextField to = Ui.field(12);
    private final JLabel invoiceCount = valueLabel();
    private final JLabel hourRevenue = valueLabel();
    private final JLabel itemRevenue = valueLabel();
    private final JLabel packageRevenue = valueLabel();
    private final JLabel discount = valueLabel();
    private final JLabel totalRevenue = valueLabel();
    private final DefaultTableModel model = Ui.readOnlyModel(
            "Mã món", "Tên món", "Loại", "Số lượng bán", "Doanh thu");
    private final JTable table = Ui.table(model);
    private final JTextField usageFrom = Ui.field(12);
    private final JTextField usageTo = Ui.field(12);
    private final DefaultTableModel usageModel = Ui.readOnlyModel(
            "Mã bàn", "Tên bàn", "Khu vực", "Số phiên", "Tổng giờ", "TB giờ/phiên");
    private final JTable usageTable = Ui.table(usageModel);

    public ReportPanel() {
        super(new BorderLayout());
        LocalDate today = LocalDate.now();
        from.setText(Ui.date(today.withDayOfMonth(1)));
        to.setText(Ui.date(today));
        usageFrom.setText(Ui.date(today.withDayOfMonth(1)));
        usageTo.setText(Ui.date(today));

        JButton load = Ui.primaryButton("Xem báo cáo");
        load.addActionListener(event -> refresh());

        JButton btnToday = shortcutButton("Hôm nay");
        btnToday.addActionListener(e -> {
            from.setText(Ui.date(today));
            to.setText(Ui.date(today));
            refresh();
        });
        JButton btnWeek = shortcutButton("7 ngày qua");
        btnWeek.addActionListener(e -> {
            from.setText(Ui.date(today.minusDays(6)));
            to.setText(Ui.date(today));
            refresh();
        });
        JButton btnMonth = shortcutButton("Tháng này");
        btnMonth.addActionListener(e -> {
            from.setText(Ui.date(today.withDayOfMonth(1)));
            to.setText(Ui.date(today));
            refresh();
        });
        JButton btnLastMonth = shortcutButton("Tháng trước");
        btnLastMonth.addActionListener(e -> {
            LocalDate first = today.minusMonths(1).withDayOfMonth(1);
            LocalDate last = today.withDayOfMonth(1).minusDays(1);
            from.setText(Ui.date(first));
            to.setText(Ui.date(last));
            refresh();
        });

        JPanel page = Ui.page(
                "Báo cáo kinh doanh",
                "Tổng hợp doanh thu, món bán chạy và thời gian sử dụng bàn.",
                Ui.toolbar(btnToday, btnWeek, btnMonth, btnLastMonth,
                        new JLabel("  |  Từ"), from, new JLabel("Đến"), to, load));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Doanh thu", revenueTab());
        tabs.addTab("Thời gian dùng bàn", tableUsageTab());
        page.add(tabs, BorderLayout.CENTER);
        add(page);
        refresh();
    }

    private JPanel revenueTab() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        JPanel metrics = new JPanel(new GridLayout(1, 6, 12, 0));
        metrics.setOpaque(false);
        metrics.add(metric("Số hóa đơn", invoiceCount));
        metrics.add(metric("Doanh thu giờ", hourRevenue));
        metrics.add(metric("Doanh thu F&B", itemRevenue));
        metrics.add(metric("Doanh thu gói giờ", packageRevenue));
        metrics.add(metric("Giảm giá", discount));
        metrics.add(metric("Tổng doanh thu", totalRevenue));
        body.add(metrics, BorderLayout.NORTH);
        JPanel card = Ui.card();
        card.add(new JLabel("Top 10 món bán chạy"), BorderLayout.NORTH);
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        body.add(card, BorderLayout.CENTER);
        return body;
    }

    private JPanel tableUsageTab() {
        JPanel card = Ui.card();
        JButton loadUsage = Ui.primaryButton("Xem");
        loadUsage.addActionListener(event -> refreshUsage());
        card.add(Ui.toolbar(
                new JLabel("Từ"), usageFrom, new JLabel("Đến"), usageTo, loadUsage),
                BorderLayout.NORTH);
        card.add(Ui.scroll(usageTable), BorderLayout.CENTER);
        return card;
    }

    private void refreshUsage() {
        try {
            LocalDate start = Ui.localDate(usageFrom.getText(), "Ngày bắt đầu");
            LocalDate end = Ui.localDate(usageTo.getText(), "Ngày kết thúc");
            List<TableUsage> rows = controller.tableUsage(start, end);
            usageModel.setRowCount(0);
            for (TableUsage row : rows) {
                usageModel.addRow(new Object[] {
                        row.maBan(), row.tenBan(), row.tenKhuVuc(),
                        row.tongPhien(),
                        row.tongGio().stripTrailingZeros().toPlainString(),
                        row.tbGio().stripTrailingZeros().toPlainString()
                });
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void refresh() {
        LocalDate start;
        LocalDate end;
        try {
            start = Ui.localDate(from.getText(), "Ngày bắt đầu");
            end = Ui.localDate(to.getText(), "Ngày kết thúc");
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
            return;
        }
        setPlaceholders("...");
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                RevenueSummary summary = controller.revenue(start, end);
                List<BestSeller> items = controller.bestSellers(start, end);
                return new Object[] { summary, items };
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] result = get();
                    RevenueSummary summary = (RevenueSummary) result[0];
                    List<BestSeller> items = (List<BestSeller>) result[1];
                    invoiceCount.setText(Long.toString(summary.soHoaDon()));
                    hourRevenue.setText(Ui.money(summary.doanhThuGio()));
                    itemRevenue.setText(Ui.money(summary.doanhThuFnb()));
                    packageRevenue.setText(Ui.money(summary.doanhThuGoiGio()));
                    discount.setText(Ui.money(summary.tongGiamGia()));
                    totalRevenue.setText(Ui.money(summary.tongDoanhThu()));
                    model.setRowCount(0);
                    for (BestSeller item : items) {
                        model.addRow(new Object[] {
                                item.maMon(), item.tenMon(), item.loaiMon(),
                                item.soLuongBan(), Ui.money(item.doanhThu())
                        });
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    setPlaceholders("?");
                    Ui.error(ReportPanel.this, ex.getCause());
                }
            }
        }.execute();
    }

    private void setPlaceholders(String text) {
        invoiceCount.setText(text);
        hourRevenue.setText(text);
        itemRevenue.setText(text);
        packageRevenue.setText(text);
        discount.setText(text);
        totalRevenue.setText(text);
    }

    private static JPanel metric(String title, JLabel value) {
        JPanel card = Ui.card();
        JLabel label = new JLabel(title.toUpperCase());
        label.setForeground(Theme.MUTED);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new javax.swing.BoxLayout(
                content, javax.swing.BoxLayout.Y_AXIS));
        content.add(label);
        content.add(javax.swing.Box.createVerticalStrut(10));
        content.add(value);
        card.add(content);
        return card;
    }

    private static JLabel valueLabel() {
        JLabel label = new JLabel("0");
        label.setForeground(Theme.INK);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 20f));
        label.setBorder(new EmptyBorder(0, 0, 4, 0));
        return label;
    }

    private static JButton shortcutButton(String text) {
        JButton button = Ui.secondaryButton(text);
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 12f));
        return button;
    }
}
