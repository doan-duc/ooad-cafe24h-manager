package ui.panel;
import ui.Ui;

import java.awt.BorderLayout;
import java.time.LocalDateTime;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import controller.BookingController;
import controller.TableController;
import model.Booking;
import model.TableInfo;

public final class BookingPanel extends JPanel implements ui.Refreshable {
    // Tóm tắt: Tải lại danh sách đặt bàn mỗi khi mở lại tab
    @Override
    public void onPageShown() {
        refresh();
    }

    private final BookingController controller = new BookingController();
    private final DefaultTableModel model = Ui.readOnlyModel(
            "Khách", "Điện thoại", "Bàn", "Bắt đầu", "Kết thúc", "Trạng thái");
    private final JTable table = Ui.table(model);
    private List<Booking> bookings = List.of();

    public BookingPanel() {
        super(new BorderLayout());
        table.getAccessibleContext().setAccessibleName("danh sách booking");
        JButton add = Ui.primaryButton("Giữ bàn");
        JButton refresh = Ui.secondaryButton("Tải lại");
        Ui.describe(add, "Nhập số điện thoại, chọn bàn và khung giờ để giữ chỗ.");
        Ui.describe(refresh, "Cập nhật danh sách booking mới nhất.");
        add.addActionListener(event -> create());
        refresh.addActionListener(event -> refresh());
        JPanel page = Ui.page(
                "Booking",
                "Giữ bàn bằng số điện thoại; khách tới chỉ cần mở bàn đã đặt và nhận khách.",
                Ui.toolbar(refresh, add));
        JPanel card = Ui.card();
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        JButton cancel = Ui.dangerButton("Hủy booking đã chọn");
        Ui.describe(cancel, "Hủy booking đang ở trạng thái Đã đặt.");
        cancel.addActionListener(event -> cancel());
        card.add(cancel, BorderLayout.SOUTH);
        page.add(card, BorderLayout.CENTER);
        add(page);
        refresh();
    }

    private void refresh() {
        try {
            bookings = controller.list();
            model.setRowCount(0);
            for (Booking booking : bookings) {
                model.addRow(new Object[] {
                        booking.tenKhachHang(), booking.soDienThoai(), booking.tenBan(),
                        Ui.dateTime(booking.thoiGianBatDau()),
                        Ui.dateTime(booking.thoiGianKetThuc()), booking.trangThai()
                });
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void create() {
        try {
            List<TableInfo> tables = new TableController().list().stream()
                    .filter(item -> "Trống".equals(item.trangThai())
                            || "Đã đặt".equals(item.trangThai()))
                    .toList();
            if (tables.isEmpty()) {
                Ui.info(this, "Không có bàn phù hợp để giữ chỗ. Hãy kiểm tra sơ đồ bàn.");
                return;
            }
            JComboBox<TableInfo> tableBox = new JComboBox<>(tables.toArray(TableInfo[]::new));
            tableBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
                @Override
                public java.awt.Component getListCellRendererComponent(
                        javax.swing.JList<?> list, Object value, int index,
                        boolean selected, boolean focus) {
                    super.getListCellRendererComponent(list, value, index, selected, focus);
                    if (value instanceof TableInfo table) {
                        setText(table.tenBan() + " · " + table.tenKhuVuc()
                                + " · " + table.sucChua() + " chỗ · "
                                + table.trangThai());
                    }
                    return this;
                }
            });

            JTextField start = Ui.field(18);
            JTextField end = Ui.field(18);
            Ui.placeholder(start, "dd/MM/yyyy HH:mm");
            Ui.placeholder(end, "dd/MM/yyyy HH:mm");
            LocalDateTime defaultStart = LocalDateTime.now()
                    .plusHours(1).withSecond(0).withNano(0);
            start.setText(Ui.dateTime(defaultStart));
            end.setText(Ui.dateTime(defaultStart.plusHours(2)));

            JTextField name = Ui.field(18);
            JTextField phone = Ui.field(18);
            Ui.placeholder(phone, "SĐT khách hoặc thành viên");
            Ui.placeholder(name, "Tên khách nếu chưa là thành viên");

            JPanel form = new JPanel(new java.awt.GridBagLayout());
            addRow(form, 0, "Số điện thoại", phone);
            addRow(form, 1, "Họ tên", name);
            addRow(form, 2, "Bàn", tableBox);
            addRow(form, 3, "Bắt đầu", start);
            addRow(form, 4, "Kết thúc", end);
            while (true) {
                if (JOptionPane.showConfirmDialog(
                        this, form, "Giữ bàn cho khách",
                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    TableInfo selectedTable = (TableInfo) tableBox.getSelectedItem();
                    String phoneText = phone.getText().trim();
                    String nameText = name.getText().isBlank()
                            ? phoneText : name.getText().trim();
                    controller.createForGuest(
                            phoneText,
                            nameText,
                            selectedTable.maBan(),
                            Ui.localDateTime(start.getText(), "Thời gian bắt đầu"),
                            Ui.localDateTime(end.getText(), "Thời gian kết thúc"));
                    Ui.info(this, "Đã giữ " + selectedTable.tenBan()
                            + " cho " + nameText
                            + ". Bàn sẽ hiện Đã đặt trên Sơ đồ bàn.");
                    refresh();
                    break;
                } catch (RuntimeException ex) {
                    Ui.error(this, ex);
                }
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void cancel() {
        try {
            Booking booking = bookings.get(Ui.selectedModelRow(table));
            if (!"Đã đặt".equals(booking.trangThai())) {
                throw new IllegalArgumentException("Chỉ có thể hủy booking đang ở trạng thái Đã đặt.");
            }
            if (Ui.confirm(this, "Hủy booking của " + booking.tenKhachHang()
                    + " tại " + booking.tenBan() + "?")) {
                controller.cancel(booking.maDatPhong());
                refresh();
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private static void addRow(
            JPanel form, int row, String label, java.awt.Component component) {
        form.add(new JLabel(label), Ui.gbc(0, row));
        form.add(component, Ui.gbc(1, row));
    }
}
