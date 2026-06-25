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
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import controller.ConfigController;
import model.Area;
import model.TableConfig;
import model.Voucher;

public final class ConfigPanel extends JPanel implements ui.Refreshable {
    // Tóm tắt: Tải lại cấu hình khu vực/bàn/voucher mỗi khi mở lại tab
    @Override
    public void onPageShown() {
        refreshAll();
    }

    private final ConfigController controller = new ConfigController();
    private final DefaultTableModel areaModel =
            Ui.readOnlyModel("Mã", "Tên khu vực", "Mô tả");
    private final JTable areaTable = Ui.table(areaModel);
    private final DefaultTableModel tableModel =
            Ui.readOnlyModel("Mã", "Tên bàn", "Khu vực", "Vị trí", "Sức chứa", "Trạng thái");
    private final JTable tableTable = Ui.table(tableModel);
    private final DefaultTableModel voucherModel =
            Ui.readOnlyModel("Mã", "Tên voucher", "Loại giảm", "Giá trị", "Bắt đầu", "Kết thúc", "Trạng thái");
    private final JTable voucherTable = Ui.table(voucherModel);
    private List<Area> areas = List.of();
    private List<TableConfig> tables = List.of();
    private List<Voucher> vouchers = List.of();

    public ConfigPanel() {
        super(new BorderLayout());
        JPanel page = Ui.page(
                "Thiết lập cửa hàng",
                "Cấu hình không gian và chính sách bán hàng trước khi vận hành.",
                null);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Khu vực", areaTab());
        tabs.addTab("Bàn", tableTab());
        tabs.addTab("Voucher", voucherTab());
        page.add(tabs, BorderLayout.CENTER);
        add(page);
        refreshAll();
    }

    private JPanel areaTab() {
        JPanel card = Ui.card();
        card.add(Ui.scroll(areaTable), BorderLayout.CENTER);
        JButton add = Ui.primaryButton("Thêm khu vực");
        JButton edit = Ui.secondaryButton("Chỉnh sửa");
        add.addActionListener(event -> editArea(null));
        edit.addActionListener(event -> {
            Area area = selectedArea();
            if (area != null) {
                editArea(area);
            }
        });
        card.add(Ui.toolbar(add, edit), BorderLayout.SOUTH);
        return card;
    }

    private JPanel tableTab() {
        JPanel card = Ui.card();
        card.add(Ui.scroll(tableTable), BorderLayout.CENTER);
        JButton add = Ui.primaryButton("Thêm bàn");
        JButton edit = Ui.secondaryButton("Chỉnh sửa");
        add.addActionListener(event -> editTable(null));
        edit.addActionListener(event -> {
            TableConfig table = selectedTable();
            if (table != null) {
                editTable(table);
            }
        });
        card.add(Ui.toolbar(add, edit), BorderLayout.SOUTH);
        return card;
    }

    private JPanel voucherTab() {
        JPanel card = Ui.card();
        card.add(Ui.scroll(voucherTable), BorderLayout.CENTER);
        JButton add = Ui.primaryButton("Thêm voucher");
        JButton edit = Ui.secondaryButton("Chỉnh sửa");
        add.addActionListener(event -> editVoucher(null));
        edit.addActionListener(event -> {
            Voucher voucher = selectedVoucher();
            if (voucher != null) {
                editVoucher(voucher);
            }
        });
        card.add(Ui.toolbar(add, edit), BorderLayout.SOUTH);
        return card;
    }

    private void refreshAll() {
        try {
            areas = controller.areas();
            areaModel.setRowCount(0);
            for (Area area : areas) {
                areaModel.addRow(new Object[] {
                        area.maKhuVuc(), area.tenKhuVuc(), area.moTa()
                });
            }
            tables = controller.tables();
            tableModel.setRowCount(0);
            for (TableConfig table : tables) {
                tableModel.addRow(new Object[] {
                        table.maBan(), table.tenBan(), table.tenKhuVuc(),
                        table.loaiViTri(), table.sucChua(), table.trangThai()
                });
            }
            vouchers = controller.vouchers();
            voucherModel.setRowCount(0);
            for (Voucher voucher : vouchers) {
                voucherModel.addRow(new Object[] {
                        voucher.maVoucher(), voucher.tenVoucher(), voucher.loaiGiam(),
                        voucher.giaTriGiam(), Ui.dateTime(voucher.ngayBatDau()),
                        Ui.dateTime(voucher.ngayKetThuc()), voucher.trangThai()
                });
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private Area selectedArea() {
        try {
            return areas.get(Ui.selectedModelRow(areaTable));
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
            return null;
        }
    }

    private TableConfig selectedTable() {
        try {
            return tables.get(Ui.selectedModelRow(tableTable));
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
            return null;
        }
    }

    private Voucher selectedVoucher() {
        try {
            return vouchers.get(Ui.selectedModelRow(voucherTable));
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
            return null;
        }
    }

    private void editArea(Area current) {
        boolean insert = current == null;
        JTextField code = Ui.field(17);
        JTextField name = Ui.field(20);
        JTextField description = Ui.field(22);
        if (current != null) {
            code.setText(current.maKhuVuc());
            code.setEnabled(false);
            name.setText(current.tenKhuVuc());
            description.setText(current.moTa());
        }
        JPanel form = new JPanel(new java.awt.GridBagLayout());
        addRow(form, 0, "Mã khu vực", code);
        addRow(form, 1, "Tên khu vực", name);
        addRow(form, 2, "Mô tả", description);
        while (true) {
            if (JOptionPane.showConfirmDialog(
                    this, form, insert ? "Thêm khu vực" : "Sửa khu vực",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                controller.saveArea(new Area(
                        code.getText().trim(), name.getText().trim(),
                        description.getText().trim()), insert);
                refreshAll();
                break;
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }
    }

    private void editTable(TableConfig current) {
        boolean insert = current == null;
        if (areas.isEmpty()) {
            Ui.info(this, "Hãy tạo khu vực trước khi tạo bàn.");
            return;
        }
        JTextField code = Ui.field(17);
        JTextField name = Ui.field(20);
        JTextField location = Ui.field(20);
        JTextField capacity = Ui.field(10);
        JComboBox<String> status = new JComboBox<>(
                new String[] {"Trống", "Đang phục vụ", "Đã đặt", "Cần dọn"});
        JComboBox<Area> area = new JComboBox<>(areas.toArray(Area[]::new));
        if (current != null) {
            code.setText(current.maBan());
            code.setEnabled(false);
            name.setText(current.tenBan());
            location.setText(current.loaiViTri());
            capacity.setText(Integer.toString(current.sucChua()));
            status.setSelectedItem(current.trangThai());
            status.setEnabled(false);
            selectArea(area, current.maKhuVuc());
        } else {
            capacity.setText("4");
        }
        JPanel form = new JPanel(new java.awt.GridBagLayout());
        addRow(form, 0, "Mã bàn", code);
        addRow(form, 1, "Tên bàn", name);
        addRow(form, 2, "Khu vực", area);
        addRow(form, 3, "Loại vị trí", location);
        addRow(form, 4, "Sức chứa", capacity);
        addRow(form, 5, "Trạng thái", status);
        while (true) {
            if (JOptionPane.showConfirmDialog(
                    this, form, insert ? "Thêm bàn" : "Sửa bàn",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                Area selectedArea = (Area) area.getSelectedItem();
                controller.saveTable(new TableConfig(
                        code.getText().trim(),
                        name.getText().trim(),
                        location.getText().trim(),
                        Ui.integer(capacity.getText(), "Sức chứa"),
                        current == null ? "Trống" : current.trangThai(),
                        selectedArea.maKhuVuc(),
                        selectedArea.tenKhuVuc()), insert);
                refreshAll();
                break;
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }
    }

    private void editVoucher(Voucher current) {
        boolean insert = current == null;
        JTextField code = Ui.field(17);
        JTextField name = Ui.field(20);
        JComboBox<String> type = new JComboBox<>(
                new String[] {"Tiền mặt", "Phần trăm"});
        JTextField value = Ui.field(14);
        JTextField start = Ui.field(18);
        JTextField end = Ui.field(18);
        JComboBox<String> status = new JComboBox<>(
                new String[] {"Hoạt động", "Tạm khóa", "Hết hạn"});
        if (current != null) {
            code.setText(current.maVoucher());
            code.setEnabled(false);
            name.setText(current.tenVoucher());
            type.setSelectedItem(current.loaiGiam());
            value.setText(current.giaTriGiam().toPlainString());
            start.setText(Ui.dateTime(current.ngayBatDau()));
            end.setText(Ui.dateTime(current.ngayKetThuc()));
            status.setSelectedItem(current.trangThai());
        } else {
            LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
            start.setText(Ui.dateTime(now));
            end.setText(Ui.dateTime(now.plusMonths(1)));
        }
        JPanel form = new JPanel(new java.awt.GridBagLayout());
        addRow(form, 0, "Mã voucher", code);
        addRow(form, 1, "Tên voucher", name);
        addRow(form, 2, "Loại giảm", type);
        addRow(form, 3, "Giá trị", value);
        addRow(form, 4, "Bắt đầu", start);
        addRow(form, 5, "Kết thúc", end);
        addRow(form, 6, "Trạng thái", status);
        while (true) {
            if (JOptionPane.showConfirmDialog(
                    this, form, insert ? "Thêm voucher" : "Sửa voucher",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                controller.saveVoucher(new Voucher(
                        code.getText().trim(),
                        name.getText().trim(),
                        (String) type.getSelectedItem(),
                        Ui.decimal(value.getText(), "Giá trị giảm"),
                        Ui.localDateTime(start.getText(), "Ngày bắt đầu"),
                        Ui.localDateTime(end.getText(), "Ngày kết thúc"),
                        (String) status.getSelectedItem()), insert);
                refreshAll();
                break;
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }
    }

    private static void selectArea(JComboBox<Area> box, String id) {
        for (int index = 0; index < box.getItemCount(); index++) {
            if (box.getItemAt(index).maKhuVuc().equals(id)) {
                box.setSelectedIndex(index);
                return;
            }
        }
    }

    private static void addRow(
            JPanel form, int row, String label, java.awt.Component component) {
        form.add(new JLabel(label), Ui.gbc(0, row));
        form.add(component, Ui.gbc(1, row));
    }
}
