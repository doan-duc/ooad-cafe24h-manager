package ui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.LocalDate;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import controller.CustomerController;
import model.Customer;
import model.InvoiceHistory;
import model.MenuItem;
import ui.Theme;
import ui.Ui;

public final class CustomerPanel extends JPanel implements ui.Refreshable {
    // Tóm tắt: Tải lại danh sách thành viên mỗi khi mở lại tab (cập nhật điểm/hạng mới nhất sau thanh toán)
    @Override
    public void onPageShown() {
        refresh();
    }

    private static final List<String> PAYMENT_METHODS =
            List.of("Tiền mặt", "Chuyển khoản QR", "Thẻ");
    private final CustomerController controller = new CustomerController();
    private final JTextField search = Ui.field(20);
    private final DefaultTableModel model = Ui.readOnlyModel(
            "Thành viên", "Điện thoại", "Hạng", "Điểm", "Số dư giờ");
    private final JTable table = Ui.table(model);
    private List<Customer> customers = List.of();

    public CustomerPanel() {
        super(new BorderLayout());
        table.getAccessibleContext().setAccessibleName("danh sách thành viên");
        Ui.placeholder(search, "Tìm theo số điện thoại hoặc tên thành viên");
        JButton searchButton = Ui.secondaryButton("Tìm");
        JButton add = Ui.primaryButton("Đăng ký thành viên");
        Ui.describe(searchButton, "Lọc danh sách thành viên theo nội dung tìm kiếm.");
        Ui.describe(add, "Tạo hồ sơ thành viên mới bằng số điện thoại.");
        searchButton.addActionListener(event -> refresh());
        search.addActionListener(event -> refresh());
        add.addActionListener(event -> edit(null));
        JPanel page = Ui.page(
                "Thành viên",
                "Chỉ lưu khách đã đăng ký thành viên; khách booking vãng lai không nằm ở đây.",
                Ui.toolbar(search, searchButton, add));

        JPanel card = Ui.card();
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        JButton edit = Ui.secondaryButton("Chỉnh sửa");
        JButton topUp = Ui.primaryButton("Nạp gói giờ");
        JButton history = Ui.secondaryButton("Lịch sử hóa đơn");
        Ui.describe(edit, "Cập nhật thông tin thành viên đang chọn.");
        Ui.describe(topUp, "Bán gói giờ cho thành viên đang chọn và tạo hóa đơn.");
        Ui.describe(history, "Xem các hóa đơn và giao dịch giờ của thành viên đang chọn.");
        edit.addActionListener(event -> {
            Customer customer = selected();
            if (customer != null) {
                edit(customer);
            }
        });
        topUp.addActionListener(event -> topUp());
        history.addActionListener(event -> history());
        card.add(Ui.toolbar(edit, topUp, history), BorderLayout.SOUTH);
        page.add(card, BorderLayout.CENTER);
        add(page);
        refresh();
    }

    private void refresh() {
        try {
            customers = controller.search(search.getText());
            model.setRowCount(0);
            for (Customer customer : customers) {
                model.addRow(new Object[] {
                        customer.hoTen(), customer.soDienThoai(),
                        customer.hangThanhVien(), customer.diemTichLuy(),
                        customer.soDuGio().stripTrailingZeros().toPlainString()
                });
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private Customer selected() {
        try {
            return customers.get(Ui.selectedModelRow(table));
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
            return null;
        }
    }

    private void edit(Customer current) {
        boolean insert = current == null;
        JTextField code = Ui.field(18);
        JTextField name = Ui.field(18);
        JTextField phone = Ui.field(18);
        JTextField email = Ui.field(18);
        JTextField birthDate = Ui.field(18);
        Ui.placeholder(name, "Họ tên thành viên");
        Ui.placeholder(phone, "Số điện thoại dùng để nhận diện thành viên");
        Ui.placeholder(email, "Email, có thể để trống");
        Ui.placeholder(birthDate, "dd/MM/yyyy, có thể để trống");
        JComboBox<String> tier = new JComboBox<>(
                new String[] {"Đồng", "Bạc", "Vàng"});
        JTextField points = Ui.field(18);
        JTextField hours = Ui.field(18);
        if (current != null) {
            code.setText(current.maKH());
            code.setEnabled(false);
            name.setText(current.hoTen());
            phone.setText(current.soDienThoai());
            email.setText(current.email());
            birthDate.setText(Ui.date(current.ngaySinh()));
            tier.setSelectedItem(current.hangThanhVien());
            points.setText(Integer.toString(current.diemTichLuy()));
            hours.setText(current.soDuGio().toPlainString());
        } else {
            tier.setSelectedItem("Đồng");
            points.setText("0");
            hours.setText("0");
        }
        points.setEnabled(false);
        hours.setEnabled(false);
        JPanel form = new JPanel(new java.awt.GridBagLayout());
        addRow(form, 0, "Họ tên", name);
        addRow(form, 1, "Số điện thoại", phone);
        addRow(form, 2, "Email", email);
        addRow(form, 3, "Ngày sinh (dd/MM/yyyy)", birthDate);
        addRow(form, 4, "Hạng thành viên", tier);
        addRow(form, 5, "Điểm tích lũy", points);
        addRow(form, 6, "Số dư giờ", hours);
        while (true) {
            if (JOptionPane.showConfirmDialog(
                    this, form, insert ? "Đăng ký thành viên" : "Sửa thành viên",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                LocalDate birth = birthDate.getText().isBlank()
                        ? null : Ui.localDate(birthDate.getText(), "Ngày sinh");
                Customer customer = new Customer(
                        code.getText().trim(),
                        name.getText().trim(),
                        blankToNull(phone.getText()),
                        blankToNull(email.getText()),
                        birth,
                        (String) tier.getSelectedItem(),
                        Ui.integer(points.getText(), "Điểm tích lũy"),
                        Ui.decimal(hours.getText(), "Số dư giờ"));
                if (insert) {
                    controller.create(customer);
                } else {
                    controller.update(customer);
                }
                refresh();
                break;
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }
    }

    private void topUp() {
        Customer customer = selected();
        if (customer == null) {
            return;
        }
        try {
            List<MenuItem> packages = controller.hourPackages();
            if (packages.isEmpty()) {
                Ui.info(this,
                        "Chưa có gói giờ đang bán. Hãy tạo món loại Gói giờ trong Menu.");
                return;
            }
            new TopUpDialog(customer, packages, PAYMENT_METHODS).setVisible(true);
            refresh();
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private final class TopUpDialog extends JDialog {
        private final Customer customer;
        private final JComboBox<MenuItem> packageBox;
        private final JComboBox<String> methodBox;
        private final JPanel packageDetail = new JPanel(new BorderLayout(0, 6));
        private boolean done = false;

        TopUpDialog(Customer customer, List<MenuItem> packages, List<String> methods) {
            super((java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(
                    CustomerPanel.this), "Nạp gói giờ · " + customer.hoTen(), true);
            this.customer = customer;
            packageBox = new JComboBox<>(packages.toArray(MenuItem[]::new));
            methodBox = new JComboBox<>(methods.toArray(String[]::new));

            setSize(520, 420);
            setMinimumSize(new Dimension(460, 380));
            setLocationRelativeTo(CustomerPanel.this);

            JPanel root = new JPanel(new BorderLayout(0, 16));
            root.setBorder(new EmptyBorder(20, 20, 16, 20));

            root.add(buildMemberCard(), BorderLayout.NORTH);

            JPanel center = new JPanel(new BorderLayout(0, 12));
            center.setOpaque(false);
            center.add(buildPackageCard(), BorderLayout.NORTH);
            center.add(buildPaymentCard(), BorderLayout.CENTER);
            root.add(center, BorderLayout.CENTER);

            JButton close = Ui.secondaryButton("Đóng");
            JButton confirm = Ui.primaryButton("Xác nhận nạp giờ");
            close.addActionListener(e -> dispose());
            confirm.addActionListener(e -> doTopUp(confirm));
            JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
            footer.setOpaque(false);
            footer.add(close);
            footer.add(confirm);
            root.add(footer, BorderLayout.SOUTH);

            setContentPane(root);
            packageBox.addActionListener(e -> updatePackageDetail());
            updatePackageDetail();
        }

        private JPanel buildMemberCard() {
            JPanel card = Ui.card();
            card.setBackground(Theme.softPrimary());
            JPanel row = new JPanel(new BorderLayout(0, 4));
            row.setOpaque(false);

            JLabel name = new JLabel(customer.hoTen());
            name.setFont(Theme.display(Font.BOLD, 17f));
            name.setForeground(Theme.INK);

            String tierColor = switch (customer.hangThanhVien()) {
                case "Vàng" -> "#b45309";
                case "Bạc" -> "#4b5563";
                default -> "#92400e";
            };
            JLabel meta = new JLabel("<html><span style='color:" + tierColor + "'><b>"
                    + customer.hangThanhVien() + "</b></span>"
                    + "  ·  " + customer.diemTichLuy() + " điểm"
                    + "  ·  Số dư hiện tại: <b>"
                    + customer.soDuGio().stripTrailingZeros().toPlainString()
                    + " giờ</b></html>");
            meta.setForeground(Theme.MUTED);

            row.add(name, BorderLayout.NORTH);
            row.add(meta, BorderLayout.CENTER);
            card.add(row, BorderLayout.CENTER);
            return card;
        }

        private JPanel buildPackageCard() {
            JPanel card = Ui.card();
            JPanel content = new JPanel(new BorderLayout(0, 10));
            content.setOpaque(false);

            JLabel heading = new JLabel("CHỌN GÓI GIỜ");
            heading.setForeground(Theme.MUTED);
            heading.setFont(heading.getFont().deriveFont(Font.BOLD, 10f));
            content.add(heading, BorderLayout.NORTH);

            JPanel selector = new JPanel(new BorderLayout(8, 0));
            selector.setOpaque(false);
            selector.add(packageBox, BorderLayout.CENTER);
            content.add(selector, BorderLayout.CENTER);

            packageDetail.setOpaque(false);
            content.add(packageDetail, BorderLayout.SOUTH);
            card.add(content, BorderLayout.CENTER);
            return card;
        }

        private JPanel buildPaymentCard() {
            JPanel card = Ui.card();
            JPanel content = new JPanel(new java.awt.GridBagLayout());
            content.setOpaque(false);

            JLabel heading = new JLabel("PHƯƠNG THỨC THANH TOÁN");
            heading.setForeground(Theme.MUTED);
            heading.setFont(heading.getFont().deriveFont(Font.BOLD, 10f));
            java.awt.GridBagConstraints topSpan = new java.awt.GridBagConstraints();
            topSpan.gridx = 0; topSpan.gridy = 0; topSpan.gridwidth = 2;
            topSpan.anchor = java.awt.GridBagConstraints.WEST;
            topSpan.insets = new java.awt.Insets(0, 0, 10, 0);
            content.add(heading, topSpan);

            content.add(new JLabel("Thanh toán bằng"), Ui.gbc(0, 1));
            content.add(methodBox, Ui.gbc(1, 1));
            card.add(content, BorderLayout.CENTER);
            return card;
        }

        private void updatePackageDetail() {
            packageDetail.removeAll();
            MenuItem pkg = (MenuItem) packageBox.getSelectedItem();
            if (pkg == null) {
                packageDetail.revalidate();
                packageDetail.repaint();
                return;
            }

            String hours = pkg.soGioQuyDoi() != null
                    ? pkg.soGioQuyDoi().stripTrailingZeros().toPlainString() + " giờ"
                    : "?";
            String expiry = pkg.hanSuDung() != null
                    ? pkg.hanSuDung() + " ngày"
                    : "Không giới hạn";

            Color pillBg = Theme.isDark()
                    ? new Color(25, 74, 58) : new Color(220, 252, 231);

            JPanel pills = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
            pills.setOpaque(false);
            pills.add(Ui.badge("⏱ " + hours, pillBg, Theme.PRIMARY_DARK));
            pills.add(Ui.badge("📅 HSD: " + expiry, pillBg, Theme.PRIMARY_DARK));
            pills.add(Ui.badge("💰 " + Ui.money(pkg.donGia()), pillBg, Theme.PRIMARY_DARK));

            JLabel note = new JLabel("<html><span style='color:#6b7280;font-size:12px'>"
                    + "Sau khi nạp, <b>" + hours + "</b> sẽ được cộng vào tài khoản, "
                    + "hết hạn sau <b>" + expiry + "</b> kể từ ngày nạp."
                    + "</span></html>");

            packageDetail.add(pills, BorderLayout.NORTH);
            packageDetail.add(note, BorderLayout.CENTER);
            packageDetail.revalidate();
            packageDetail.repaint();
        }

        private void doTopUp(JButton button) {
            if (done) {
                return;
            }
            MenuItem pkg = (MenuItem) packageBox.getSelectedItem();
            String method = (String) methodBox.getSelectedItem();
            if (pkg == null || method == null) {
                return;
            }
            if (!Ui.confirm(this,
                    "<html>Nạp <b>" + pkg.tenMon() + "</b> cho <b>"
                            + customer.hoTen() + "</b><br>"
                            + "Thanh toán: <b>" + Ui.money(pkg.donGia())
                            + "</b> qua " + method + "</html>")) {
                return;
            }
            try {
                done = true;
                button.setEnabled(false);
                String[] result = controller.topUp(
                        customer.maKH(), pkg.maMon(), method);
                Ui.info(this, "<html><b>Nạp giờ thành công!</b><br>"
                        + "Hóa đơn: " + result[0] + "<br>"
                        + "Đã thêm " + pkg.soGioQuyDoi().stripTrailingZeros().toPlainString()
                        + " giờ vào tài khoản " + customer.hoTen() + ".</html>");
                dispose();
            } catch (RuntimeException ex) {
                done = false;
                button.setEnabled(true);
                Ui.error(this, ex);
            }
        }
    }

    private void history() {
        Customer customer = selected();
        if (customer == null) {
            return;
        }
        try {
            List<InvoiceHistory> invoices = controller.history(customer.maKH());
            DefaultTableModel historyModel = Ui.readOnlyModel(
                    "Hóa đơn", "Ngày", "Loại", "Tiền giờ", "Tiền món", "Giảm", "Tổng");
            for (InvoiceHistory invoice : invoices) {
                historyModel.addRow(new Object[] {
                        invoice.maHD(), Ui.dateTime(invoice.ngayLap()), invoice.loaiHoaDon(),
                        Ui.money(invoice.tienGio()), Ui.money(invoice.tienMon()),
                        Ui.money(invoice.tienGiam()), Ui.money(invoice.tongTien())
                });
            }
            JDialog dialog = new JDialog(
                    (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this),
                    "Lịch sử · " + customer.hoTen(), true);
            dialog.setContentPane(Ui.scroll(Ui.table(historyModel)));
            dialog.setMinimumSize(new Dimension(850, 430));
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private static void addRow(
            JPanel form, int row, String label, java.awt.Component component) {
        form.add(new JLabel(label), Ui.gbc(0, row));
        form.add(component, Ui.gbc(1, row));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
