package ui.panel;

import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import controller.ShiftController;
import model.LookupItem;
import model.ShiftRecord;
import model.ShiftRegistration;
import security.Authorization;
import security.Permission;
import security.Session;
import ui.Theme;
import ui.Ui;

public final class ShiftPanel extends JPanel implements ui.Refreshable {
    // Tóm tắt: Tải lại đăng ký ca và chốt ca mỗi khi mở lại tab
    @Override
    public void onPageShown() {
        refreshAll();
    }

    private final ShiftController controller = new ShiftController();
    private final boolean canOperate = Authorization.can(
            Session.currentUser(), Permission.SHIFT_OPERATE);
    private final boolean canManage = Authorization.can(
            Session.currentUser(), Permission.SHIFT_MANAGE);

    private final JLabel current = new JLabel();
    private final DefaultTableModel myRegistrationModel = Ui.readOnlyModel(
            "Mã", "Ngày làm", "Ca", "Trạng thái", "Ghi chú",
            "Người duyệt", "Thời gian duyệt");
    private final JTable myRegistrationTable = Ui.table(myRegistrationModel);
    private final DefaultTableModel managementModel = Ui.readOnlyModel(
            "Mã", "Ngày làm", "Ca", "Nhân viên", "Trạng thái",
            "Ghi chú", "Người duyệt");
    private final JTable managementTable = Ui.table(managementModel);
    private final DefaultTableModel closeHistoryModel = Ui.readOnlyModel(
            "Mã chốt", "Ca", "Nhân viên", "Đầu ca", "Hệ thống",
            "Thực tế", "Chênh lệch", "Trạng thái");
    private final JTable closeHistoryTable = Ui.table(closeHistoryModel);

    private List<ShiftRegistration> myRegistrations = List.of();
    private List<ShiftRegistration> managedRegistrations = List.of();

    public ShiftPanel() {
        super(new BorderLayout());
        JButton refresh = Ui.secondaryButton("Tải lại");
        refresh.addActionListener(event -> refreshAll());
        JPanel page = Ui.page(
                "Ca làm việc",
                "Đăng ký, phân công, mở chốt ca và theo dõi lịch sử.",
                refresh);

        JTabbedPane tabs = new JTabbedPane();
        if (canOperate) {
            tabs.addTab("Đăng ký của tôi", myRegistrationTab());
        }
        if (canManage) {
            tabs.addTab("Duyệt & phân công", managementTab());
        }
        if (canOperate) {
            tabs.addTab("Mở & chốt ca", operationTab());
        }
        if (canManage) {
            tabs.addTab("Lịch sử chốt ca", closeHistoryTab());
        }
        page.add(tabs, BorderLayout.CENTER);
        add(page);
        refreshAll();
    }

    private JPanel myRegistrationTab() {
        JPanel card = Ui.card();
        card.add(Ui.scroll(myRegistrationTable), BorderLayout.CENTER);
        JButton register = Ui.primaryButton("Đăng ký ca");
        JButton cancel = Ui.dangerButton("Hủy đăng ký");
        register.addActionListener(event -> register());
        cancel.addActionListener(event -> cancelRegistration());
        card.add(Ui.toolbar(register, cancel), BorderLayout.SOUTH);
        return card;
    }

    private JPanel managementTab() {
        JPanel card = Ui.card();
        card.add(Ui.scroll(managementTable), BorderLayout.CENTER);
        JButton assign = Ui.primaryButton("Phân công ca");
        JButton approve = Ui.secondaryButton("Duyệt / điều chỉnh");
        JButton reject = Ui.dangerButton("Từ chối");
        assign.addActionListener(event -> assign());
        approve.addActionListener(event -> approve());
        reject.addActionListener(event -> reject());
        card.add(Ui.toolbar(assign, approve, reject), BorderLayout.SOUTH);
        return card;
    }

    private JPanel operationTab() {
        JPanel card = Ui.card();
        current.setForeground(Theme.INK);
        card.add(current, BorderLayout.CENTER);
        JButton open = Ui.primaryButton("Mở ca");
        JButton close = Ui.secondaryButton("Chốt ca");
        open.addActionListener(event -> open());
        close.addActionListener(event -> close());
        card.add(Ui.toolbar(open, close), BorderLayout.EAST);
        return card;
    }

    private JPanel closeHistoryTab() {
        JPanel card = Ui.card();
        card.add(Ui.scroll(closeHistoryTable), BorderLayout.CENTER);
        return card;
    }

    private void refreshAll() {
        if (canOperate) {
            refreshMyRegistrations();
            refreshCurrent();
        }
        if (canManage) {
            refreshManagedRegistrations();
            refreshCloseHistory();
        }
    }

    private void refreshMyRegistrations() {
        try {
            myRegistrations = controller.myRegistrations();
            myRegistrationModel.setRowCount(0);
            for (ShiftRegistration registration : myRegistrations) {
                myRegistrationModel.addRow(new Object[] {
                        registration.maDangKy(),
                        Ui.date(registration.ngayLam()),
                        registration.tenCa(),
                        statusText(registration.trangThai()),
                        textOrEmpty(registration.ghiChu()),
                        textOrEmpty(registration.tenNhanVienDuyet()),
                        Ui.dateTime(registration.ngayDuyet())
                });
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void refreshManagedRegistrations() {
        try {
            managedRegistrations = controller.registrations();
            managementModel.setRowCount(0);
            for (ShiftRegistration registration : managedRegistrations) {
                managementModel.addRow(new Object[] {
                        registration.maDangKy(),
                        Ui.date(registration.ngayLam()),
                        registration.tenCa(),
                        registration.tenNhanVien(),
                        statusText(registration.trangThai()),
                        textOrEmpty(registration.ghiChu()),
                        textOrEmpty(registration.tenNhanVienDuyet())
                });
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void refreshCurrent() {
        try {
            ShiftRecord record = controller.current();
            current.setText(record == null
                    ? "<html><b>Chưa mở ca</b><br><span style='color:#6b7280'>"
                            + "Mở ca trước khi bắt đầu thu tiền.</span></html>"
                    : "<html><b>" + record.tenCa() + " · " + record.maChotCa()
                            + "</b><br><span style='color:#6b7280'>Tiền đầu ca: "
                            + Ui.money(record.tienDauCa()) + "</span></html>");
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void refreshCloseHistory() {
        try {
            List<ShiftRecord> records = controller.history();
            closeHistoryModel.setRowCount(0);
            for (ShiftRecord item : records) {
                closeHistoryModel.addRow(new Object[] {
                        item.maChotCa(), item.tenCa(), item.tenNhanVien(),
                        Ui.money(item.tienDauCa()), Ui.money(item.tienHeThong()),
                        Ui.money(item.tienThucTe()), Ui.money(item.chenhLech()),
                        item.trangThaiChot()
                });
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void register() {
        try {
            List<LookupItem> shifts = controller.shiftTypes();
            ensureAvailable(shifts, "Chưa có khung ca để đăng ký.");
            JComboBox<LookupItem> shift =
                    new JComboBox<>(shifts.toArray(LookupItem[]::new));
            JTextField date = Ui.field(14);
            date.setText(Ui.date(LocalDate.now().plusDays(1)));
            JTextArea note = Ui.area(3, 22);
            JPanel form = new JPanel(new java.awt.GridBagLayout());
            addRow(form, 0, "Khung ca", shift);
            addRow(form, 1, "Ngày làm", date);
            addRow(form, 2, "Ghi chú", Ui.scroll(note));
            while (true) {
                if (JOptionPane.showConfirmDialog(
                        this, form, "Đăng ký ca làm",
                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    LookupItem selected = (LookupItem) shift.getSelectedItem();
                    String id = controller.register(
                            selected.id(),
                            Ui.localDate(date.getText(), "Ngày làm"),
                            note.getText());
                    Ui.info(this, "Đã gửi đăng ký " + id + " và đang chờ duyệt.");
                    refreshAll();
                    return;
                } catch (RuntimeException ex) {
                    Ui.error(this, ex);
                }
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void cancelRegistration() {
        try {
            ShiftRegistration registration = selectedMyRegistration();
            requirePending(registration);
            if (Ui.confirm(this, "Hủy đăng ký " + registration.maDangKy()
                    + " cho ngày " + Ui.date(registration.ngayLam()) + "?")) {
                controller.cancel(registration.maDangKy());
                refreshAll();
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void assign() {
        try {
            List<LookupItem> employees = controller.activeEmployees().stream()
                    .filter(item -> !Session.currentUser().maNV().equals(item.id()))
                    .toList();
            List<LookupItem> shifts = controller.shiftTypes();
            ensureAvailable(employees, "Chưa có nhân viên phù hợp để phân công.");
            ensureAvailable(shifts, "Chưa có khung ca để phân công.");
            JComboBox<LookupItem> employee =
                    new JComboBox<>(employees.toArray(LookupItem[]::new));
            JComboBox<LookupItem> shift =
                    new JComboBox<>(shifts.toArray(LookupItem[]::new));
            JTextField date = Ui.field(14);
            date.setText(Ui.date(LocalDate.now().plusDays(1)));
            JTextArea note = Ui.area(3, 22);
            JPanel form = new JPanel(new java.awt.GridBagLayout());
            addRow(form, 0, "Nhân viên", employee);
            addRow(form, 1, "Khung ca", shift);
            addRow(form, 2, "Ngày làm", date);
            addRow(form, 3, "Ghi chú", Ui.scroll(note));
            while (true) {
                if (JOptionPane.showConfirmDialog(
                        this, form, "Phân công ca",
                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    LookupItem selectedEmployee = (LookupItem) employee.getSelectedItem();
                    LookupItem selectedShift = (LookupItem) shift.getSelectedItem();
                    String id = controller.assign(
                            selectedEmployee.id(),
                            selectedShift.id(),
                            Ui.localDate(date.getText(), "Ngày làm"),
                            note.getText());
                    Ui.info(this, "Đã phân công ca với mã " + id + ".");
                    refreshAll();
                    return;
                } catch (RuntimeException ex) {
                    Ui.error(this, ex);
                }
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void approve() {
        try {
            ShiftRegistration registration = selectedManagedRegistration();
            requirePending(registration);
            if (Session.currentUser().maNV().equals(registration.maNV())) {
                throw new IllegalArgumentException(
                        "Bạn không thể duyệt đăng ký ca của chính mình.");
            }
            List<LookupItem> shifts = controller.shiftTypes();
            ensureAvailable(shifts, "Chưa có khung ca để duyệt.");
            JComboBox<LookupItem> shift =
                    new JComboBox<>(shifts.toArray(LookupItem[]::new));
            selectLookup(shift, registration.maCa());
            JTextField date = Ui.field(14);
            date.setText(Ui.date(registration.ngayLam()));
            JPanel form = new JPanel(new java.awt.GridBagLayout());
            addRow(form, 0, "Nhân viên", new JLabel(registration.tenNhanVien()));
            addRow(form, 1, "Khung ca", shift);
            addRow(form, 2, "Ngày làm", date);
            while (true) {
                if (JOptionPane.showConfirmDialog(
                        this, form, "Duyệt / điều chỉnh đăng ký",
                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    LookupItem selected = (LookupItem) shift.getSelectedItem();
                    controller.approve(
                            registration.maDangKy(),
                            selected.id(),
                            Ui.localDate(date.getText(), "Ngày làm"));
                    Ui.info(this, "Đã duyệt đăng ký " + registration.maDangKy() + ".");
                    refreshAll();
                    return;
                } catch (RuntimeException ex) {
                    Ui.error(this, ex);
                }
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void reject() {
        try {
            ShiftRegistration registration = selectedManagedRegistration();
            requirePending(registration);
            if (Session.currentUser().maNV().equals(registration.maNV())) {
                throw new IllegalArgumentException(
                        "Bạn không thể từ chối đăng ký ca của chính mình.");
            }
            JTextArea reason = Ui.area(4, 24);
            JPanel form = new JPanel(new java.awt.GridBagLayout());
            addRow(form, 0, "Nhân viên", new JLabel(registration.tenNhanVien()));
            addRow(form, 1, "Lý do từ chối", Ui.scroll(reason));
            while (true) {
                if (JOptionPane.showConfirmDialog(
                        this, form, "Từ chối đăng ký " + registration.maDangKy(),
                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    controller.reject(registration.maDangKy(), reason.getText());
                    Ui.info(this, "Đã từ chối đăng ký " + registration.maDangKy() + ".");
                    refreshAll();
                    return;
                } catch (RuntimeException ex) {
                    Ui.error(this, ex);
                }
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void open() {
        try {
            if (controller.current() != null) {
                throw new IllegalArgumentException("Bạn đang có một ca chưa chốt.");
            }
            List<LookupItem> shifts = controller.shiftTypes();
            ensureAvailable(shifts, "Chưa có khung ca để mở.");
            JComboBox<LookupItem> shift =
                    new JComboBox<>(shifts.toArray(LookupItem[]::new));
            JTextField cash = Ui.field(15);
            cash.setText("0");
            JPanel form = new JPanel(new java.awt.GridBagLayout());
            addRow(form, 0, "Khung ca", shift);
            addRow(form, 1, "Tiền đầu ca", cash);
            while (true) {
                if (JOptionPane.showConfirmDialog(
                        this, form, "Mở ca làm việc",
                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    LookupItem selected = (LookupItem) shift.getSelectedItem();
                    String id = controller.open(
                            selected.id(), Ui.decimal(cash.getText(), "Tiền đầu ca"));
                    Ui.info(this, "Đã mở ca " + id + ".");
                    refreshAll();
                    return;
                } catch (RuntimeException ex) {
                    Ui.error(this, ex);
                }
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void close() {
        try {
            ShiftRecord record = controller.current();
            if (record == null) {
                throw new IllegalArgumentException("Chưa có ca đang mở.");
            }
            JTextField actual = Ui.field(15);
            JTextField reason = Ui.field(22);
            JPanel form = new JPanel(new java.awt.GridBagLayout());
            addRow(form, 0, "Tiền thực tế", actual);
            addRow(form, 1, "Lý do chênh lệch", reason);
            while (true) {
                if (JOptionPane.showConfirmDialog(
                        this, form, "Chốt ca " + record.maChotCa(),
                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    BigDecimal amount = Ui.decimal(actual.getText(), "Tiền thực tế");
                    controller.close(record.maChotCa(), amount, reason.getText());
                    Ui.info(this, "Đã chốt ca.");
                    refreshAll();
                    return;
                } catch (RuntimeException ex) {
                    Ui.error(this, ex);
                }
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private ShiftRegistration selectedMyRegistration() {
        return myRegistrations.get(Ui.selectedModelRow(myRegistrationTable));
    }

    private ShiftRegistration selectedManagedRegistration() {
        return managedRegistrations.get(Ui.selectedModelRow(managementTable));
    }

    private static void requirePending(ShiftRegistration registration) {
        if (!"ChoDuyet".equals(registration.trangThai())) {
            throw new IllegalArgumentException(
                    "Chỉ có thể thao tác với đăng ký đang chờ duyệt.");
        }
    }

    private static void selectLookup(JComboBox<LookupItem> combo, String id) {
        for (int index = 0; index < combo.getItemCount(); index++) {
            if (combo.getItemAt(index).id().equals(id)) {
                combo.setSelectedIndex(index);
                return;
            }
        }
    }

    private static void ensureAvailable(List<?> values, String message) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String statusText(String status) {
        return switch (status) {
            case "ChoDuyet" -> "Chờ duyệt";
            case "DaDuyet" -> "Đã duyệt";
            case "TuChoi" -> "Từ chối";
            case "DaHuy" -> "Đã hủy";
            default -> status;
        };
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static void addRow(
            JPanel form, int row, String label, java.awt.Component component) {
        form.add(new JLabel(label), Ui.gbc(0, row));
        form.add(component, Ui.gbc(1, row));
    }
}
