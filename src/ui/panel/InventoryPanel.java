package ui.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import controller.BanhangController;
import model.Ingredient;
import model.PurchaseLine;
import model.PurchaseReceipt;
import model.StockCount;
import model.StockCountLine;
import security.Authorization;
import security.Permission;
import security.Session;
import ui.Ui;

public final class InventoryPanel extends JPanel implements ui.Refreshable {
    // Tóm tắt: Tải lại tồn kho và phiếu nhập mỗi khi mở lại tab (cập nhật sau khi pha chế trừ kho)
    @Override
    public void onPageShown() {
        refreshAll();
    }

    private final BanhangController controller = new BanhangController();
    private final JTextField search = Ui.field(18);
    private final DefaultTableModel ingredientModel = Ui.readOnlyModel(
            "Mã", "Nguyên liệu", "Đơn vị", "Tồn kho", "Mức cảnh báo", "Tình trạng");
    private final JTable ingredientTable = Ui.table(ingredientModel);
    private final DefaultTableModel countModel = Ui.readOnlyModel(
            "Mã phiếu", "Ngày kiểm", "Người kiểm", "Trạng thái",
            "Số dòng", "Chênh lệch", "Người duyệt", "Lý do từ chối");
    private final JTable countTable = Ui.table(countModel);
    private final JTextField receiptFrom = Ui.field(12);
    private final JTextField receiptTo = Ui.field(12);
    private final DefaultTableModel receiptModel = Ui.readOnlyModel(
            "Mã phiếu", "Ngày nhập", "Nhà cung cấp", "Người lập",
            "Tổng tiền", "Trạng thái", "Ghi chú");
    private final JTable receiptTable = Ui.table(receiptModel);
    private final JButton detailButton = Ui.secondaryButton("Xem chi tiết");
    private final JButton submitButton = Ui.secondaryButton("Gửi duyệt");
    private final JButton approveButton = Ui.primaryButton("Duyệt");
    private final JButton rejectButton = Ui.dangerButton("Từ chối");
    private List<Ingredient> ingredients = List.of();
    private List<StockCount> counts = List.of();
    private boolean lifecycleSupported;

    public InventoryPanel() {
        super(new BorderLayout());
        LocalDate today = LocalDate.now();
        receiptFrom.setText(Ui.date(today.withDayOfMonth(1)));
        receiptTo.setText(Ui.date(today));

        JButton refresh = Ui.secondaryButton("Tải lại");
        refresh.addActionListener(event -> refreshAll());
        JPanel page = Ui.page(
                "Kho nguyên liệu",
                "Theo dõi tồn, nhập kho, kiểm kê và phê duyệt điều chỉnh.",
                refresh);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Tồn kho", ingredientTab());
        tabs.addTab("Phiếu kiểm kê", countTab());
        tabs.addTab("Lịch sử nhập kho", receiptHistoryTab());
        page.add(tabs, BorderLayout.CENTER);
        add(page);
        refreshAll();
    }

    private JPanel ingredientTab() {
        JPanel card = Ui.card();
        JPanel top = Ui.toolbar(search, Ui.secondaryButton("Tìm"));
        ((JButton) top.getComponent(1)).addActionListener(event -> refreshIngredients());
        search.addActionListener(event -> refreshIngredients());
        card.add(top, BorderLayout.NORTH);
        card.add(Ui.scroll(ingredientTable), BorderLayout.CENTER);

        boolean canOperate = Authorization.can(
                Session.currentUser(), Permission.INVENTORY_OPERATE);
        JButton add = Ui.primaryButton("Thêm nguyên liệu");
        JButton edit = Ui.secondaryButton("Chỉnh sửa");
        JButton receipt = Ui.secondaryButton("Lập phiếu nhập");
        JButton cancelReceipt = Ui.dangerButton("Hủy phiếu nhập");
        JButton count = Ui.secondaryButton("Tạo kiểm kê");
        add.setEnabled(canOperate);
        edit.setEnabled(canOperate);
        receipt.setEnabled(canOperate);
        cancelReceipt.setEnabled(canOperate);
        count.setEnabled(canOperate);
        add.addActionListener(event -> editIngredient(null));
        edit.addActionListener(event -> {
            Ingredient ingredient = selectedIngredient();
            if (ingredient != null) {
                editIngredient(ingredient);
            }
        });
        receipt.addActionListener(event -> new PurchaseDialog().setVisible(true));
        cancelReceipt.addActionListener(event -> cancelPurchaseReceipt());
        count.addActionListener(event -> createCount());
        card.add(Ui.toolbar(add, edit, receipt, cancelReceipt, count),
                BorderLayout.SOUTH);
        return card;
    }

    private void cancelPurchaseReceipt() {
        JTextField receiptId = Ui.field(18);
        JTextField reason = Ui.field(28);
        JPanel form = new JPanel(new java.awt.GridBagLayout());
        addRow(form, 0, "Mã phiếu nhập", receiptId);
        addRow(form, 1, "Lý do hủy", reason);
        while (true) {
            if (JOptionPane.showConfirmDialog(
                    this, form, "Hủy phiếu nhập kho",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                controller.cancelPurchaseReceipt(
                        receiptId.getText(), reason.getText());
                Ui.info(this, "Đã hủy phiếu nhập và hoàn lại tồn kho.");
                refreshAll();
                return;
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }
    }

    private JPanel countTab() {
        JPanel card = Ui.card();
        card.add(Ui.scroll(countTable), BorderLayout.CENTER);
        detailButton.addActionListener(event -> showCountDetails());
        submitButton.addActionListener(event -> submit());
        approveButton.addActionListener(event -> approve());
        rejectButton.addActionListener(event -> reject());
        countTable.getSelectionModel().addListSelectionListener(
                event -> updateCountActions());
        countTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && countTable.getSelectedRow() >= 0) {
                    showCountDetails();
                }
            }
        });
        card.add(Ui.toolbar(
                detailButton, submitButton, approveButton, rejectButton),
                BorderLayout.SOUTH);
        updateCountActions();
        return card;
    }

    private JPanel receiptHistoryTab() {
        JPanel card = Ui.card();
        JButton load = Ui.primaryButton("Xem");
        load.addActionListener(event -> refreshReceipts());
        card.add(Ui.toolbar(
                new JLabel("Từ"), receiptFrom, new JLabel("Đến"), receiptTo, load),
                BorderLayout.NORTH);
        card.add(Ui.scroll(receiptTable), BorderLayout.CENTER);
        return card;
    }

    private void refreshReceipts() {
        try {
            LocalDate from = Ui.localDate(receiptFrom.getText(), "Ngày bắt đầu");
            LocalDate to = Ui.localDate(receiptTo.getText(), "Ngày kết thúc");
            List<PurchaseReceipt> receipts = controller.listPurchaseReceipts(from, to);
            receiptModel.setRowCount(0);
            for (PurchaseReceipt r : receipts) {
                receiptModel.addRow(new Object[] {
                        r.maPhieuNK(),
                        Ui.dateTime(r.ngayNhap()),
                        r.nhaCungCap(),
                        r.tenNguoiLap(),
                        Ui.money(r.tongTien()),
                        r.trangThai(),
                        r.ghiChu() == null ? "" : r.ghiChu()
                });
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void refreshAll() {
        refreshIngredients();
        refreshCounts();
    }

    private void refreshIngredients() {
        try {
            ingredients = controller.list(search.getText());
            ingredientModel.setRowCount(0);
            for (Ingredient ingredient : ingredients) {
                ingredientModel.addRow(new Object[] {
                        ingredient.maNL(), ingredient.tenNL(), ingredient.donViTinh(),
                        ingredient.soLuongTon().stripTrailingZeros().toPlainString(),
                        ingredient.mucCanhBao().stripTrailingZeros().toPlainString(),
                        ingredient.trangThaiTon()
                });
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void refreshCounts() {
        try {
            lifecycleSupported = controller.supportsStockCountLifecycle();
            counts = controller.stockCounts();
            countModel.setRowCount(0);
            for (StockCount count : counts) {
                countModel.addRow(new Object[] {
                        count.maPhieuKK(), Ui.dateTime(count.ngayKiemKe()),
                        count.tenNguoiKiem(), count.trangThai(), count.soDong(),
                        decimal(count.tongChenhLech()),
                        count.tenNguoiDuyet() == null ? "" : count.tenNguoiDuyet(),
                        count.lyDoTuChoi() == null ? "" : count.lyDoTuChoi()
                });
            }
            updateCountActions();
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private Ingredient selectedIngredient() {
        try {
            return ingredients.get(Ui.selectedModelRow(ingredientTable));
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
            return null;
        }
    }

    private void editIngredient(Ingredient current) {
        boolean insert = current == null;
        JTextField code = Ui.field(16);
        JTextField name = Ui.field(20);
        JTextField unit = Ui.field(14);
        JTextField stock = Ui.field(14);
        JTextField warning = Ui.field(14);
        if (current != null) {
            code.setText(current.maNL());
            code.setEnabled(false);
            name.setText(current.tenNL());
            unit.setText(current.donViTinh());
            stock.setText(current.soLuongTon().toPlainString());
            stock.setEnabled(false);
            warning.setText(current.mucCanhBao().toPlainString());
        } else {
            stock.setText("0");
            warning.setText("0");
        }
        JPanel form = new JPanel(new java.awt.GridBagLayout());
        addRow(form, 0, "Mã nguyên liệu", code);
        addRow(form, 1, "Tên nguyên liệu", name);
        addRow(form, 2, "Đơn vị tính", unit);
        addRow(form, 3, "Tồn ban đầu", stock);
        addRow(form, 4, "Mức cảnh báo", warning);
        while (true) {
            if (JOptionPane.showConfirmDialog(
                    this, form, insert ? "Thêm nguyên liệu" : "Sửa nguyên liệu",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                Ingredient ingredient = new Ingredient(
                        code.getText().trim(),
                        name.getText().trim(),
                        unit.getText().trim(),
                        Ui.decimal(stock.getText(), "Tồn ban đầu"),
                        Ui.decimal(warning.getText(), "Mức cảnh báo"),
                        "");
                if (insert) {
                    controller.createIngredient(ingredient);
                } else {
                    controller.updateIngredient(ingredient);
                }
                refreshIngredients();
                break;
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }
    }

    private void createCount() {
        try {
            Ingredient initial = null;
            int selected = ingredientTable.getSelectedRow();
            if (selected >= 0) {
                initial = ingredients.get(
                        ingredientTable.convertRowIndexToModel(selected));
            }
            new StockCountDialog(initial).setVisible(true);
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private StockCount selectedCount() {
        return counts.get(Ui.selectedModelRow(countTable));
    }

    private StockCount selectedCountOrNull() {
        int selected = countTable.getSelectedRow();
        return selected < 0
                ? null
                : counts.get(countTable.convertRowIndexToModel(selected));
    }

    private void updateCountActions() {
        StockCount count = selectedCountOrNull();
        boolean canOperate = Authorization.can(
                Session.currentUser(), Permission.INVENTORY_OPERATE);
        boolean canApprove = Authorization.can(
                Session.currentUser(), Permission.INVENTORY_APPROVE);
        boolean isOwner = count != null
                && Session.currentUser().maNV().equals(count.maNV());
        detailButton.setEnabled(count != null);
        submitButton.setEnabled(lifecycleSupported && canOperate
                && count != null
                && (count.isDraft() || count.isRejected()) && isOwner);
        approveButton.setEnabled(canApprove && count != null
                && count.isPending() && !isOwner);
        rejectButton.setEnabled(lifecycleSupported && canApprove
                && count != null && count.isPending() && !isOwner);
        submitButton.setToolTipText(lifecycleSupported
                ? null
                : "Cơ sở dữ liệu hiện tại chưa hỗ trợ trạng thái Nháp.");
        rejectButton.setToolTipText(lifecycleSupported
                ? null
                : "Cơ sở dữ liệu hiện tại chưa hỗ trợ trạng thái Từ chối.");
    }

    private void showCountDetails() {
        try {
            StockCount count = selectedCount();
            List<StockCountLine> lines =
                    controller.stockCountLines(count.maPhieuKK());
            DefaultTableModel model = Ui.readOnlyModel(
                    "Mã", "Nguyên liệu", "Đơn vị", "Sổ sách",
                    "Thực tế", "Chênh lệch", "Lý do");
            for (StockCountLine line : lines) {
                model.addRow(new Object[] {
                        line.maNL(), line.tenNL(), line.donViTinh(),
                        decimal(line.soLuongSoSach()),
                        decimal(line.soLuongThucTe()),
                        decimal(line.chenhLech()),
                        line.lyDo() == null ? "" : line.lyDo()
                });
            }
            JTable table = Ui.table(model);
            JPanel header = new JPanel(new java.awt.GridBagLayout());
            addRow(header, 0, "Mã phiếu", new JLabel(count.maPhieuKK()));
            addRow(header, 1, "Trạng thái", new JLabel(count.trangThai()));
            addRow(header, 2, "Người kiểm", new JLabel(count.tenNguoiKiem()));
            addRow(header, 3, "Ghi chú", new JLabel(
                    count.ghiChu() == null ? "" : count.ghiChu()));
            if (count.lyDoTuChoi() != null && !count.lyDoTuChoi().isBlank()) {
                addRow(header, 4, "Lý do từ chối", new JLabel(count.lyDoTuChoi()));
            }
            JPanel root = new JPanel(new BorderLayout(0, 12));
            root.setBorder(new javax.swing.border.EmptyBorder(12, 12, 12, 12));
            root.add(header, BorderLayout.NORTH);
            root.add(Ui.scroll(table), BorderLayout.CENTER);
            JOptionPane pane = new JOptionPane(
                    root, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION);
            JDialog dialog = pane.createDialog(
                    this, "Chi tiết phiếu kiểm kê " + count.maPhieuKK());
            dialog.setMinimumSize(new Dimension(900, 520));
            dialog.setSize(900, 520);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void submit() {
        try {
            StockCount count = selectedCount();
            if (Ui.confirm(this, "Gửi phiếu " + count.maPhieuKK()
                    + " sang trạng thái Chờ duyệt?")) {
                controller.submit(count.maPhieuKK());
                refreshCounts();
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void approve() {
        try {
            StockCount count = selectedCount();
            if (Ui.confirm(this, "Duyệt phiếu " + count.maPhieuKK()
                    + " và cập nhật tồn kho?")) {
                controller.approve(count.maPhieuKK());
                refreshAll();
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void reject() {
        try {
            StockCount count = selectedCount();
            JTextField reason = Ui.field(28);
            JPanel form = new JPanel(new java.awt.GridBagLayout());
            addRow(form, 0, "Lý do từ chối", reason);
            while (true) {
                if (JOptionPane.showConfirmDialog(
                        this, form, "Từ chối phiếu " + count.maPhieuKK(),
                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    controller.reject(count.maPhieuKK(), reason.getText());
                    Ui.info(this, "Đã từ chối phiếu " + count.maPhieuKK() + ".");
                    refreshCounts();
                    return;
                } catch (IllegalArgumentException ex) {
                    Ui.error(this, ex);
                }
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private final class StockCountDialog extends JDialog {
        private final JTextField note = Ui.field(28);
        private final DefaultTableModel lineModel = Ui.readOnlyModel(
                "Mã", "Nguyên liệu", "Đơn vị", "Sổ sách",
                "Thực tế", "Chênh lệch", "Lý do");
        private final JTable lineTable = Ui.table(lineModel);
        private final List<Ingredient> available;
        private final List<StockCountLine> lines = new ArrayList<>();
        private boolean saving;

        private StockCountDialog(Ingredient initial) {
            super((java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(
                    InventoryPanel.this), "Lập phiếu kiểm kê", true);
            available = controller.list("");
            JPanel root = new JPanel(new BorderLayout(12, 12));
            root.setBorder(new javax.swing.border.EmptyBorder(16, 16, 16, 16));
            JPanel header = new JPanel(new java.awt.GridBagLayout());
            addRow(header, 0, "Ghi chú phiếu", note);
            root.add(header, BorderLayout.NORTH);
            root.add(Ui.scroll(lineTable), BorderLayout.CENTER);

            JButton add = Ui.secondaryButton("Thêm dòng");
            JButton edit = Ui.secondaryButton("Sửa dòng");
            JButton remove = Ui.dangerButton("Xóa dòng");
            JButton draft = Ui.secondaryButton("Lưu nháp");
            JButton submit = Ui.primaryButton("Lưu và gửi duyệt");
            add.addActionListener(event -> editLine(-1));
            edit.addActionListener(event -> {
                try {
                    editLine(Ui.selectedModelRow(lineTable));
                } catch (RuntimeException ex) {
                    Ui.error(this, ex);
                }
            });
            remove.addActionListener(event -> removeLine());
            draft.addActionListener(event -> save(false));
            submit.addActionListener(event -> save(true));
            draft.setEnabled(lifecycleSupported);
            if (!lifecycleSupported) {
                draft.setToolTipText(
                        "Cơ sở dữ liệu hiện tại chưa hỗ trợ trạng thái Nháp.");
            }
            root.add(Ui.toolbar(add, edit, remove, draft, submit), BorderLayout.SOUTH);
            setContentPane(root);
            setMinimumSize(new Dimension(980, 580));
            setSize(980, 580);
            setLocationRelativeTo(InventoryPanel.this);
            if (initial != null) {
                lines.add(toCountLine(
                        initial, initial.soLuongTon(), ""));
                refreshLines();
            }
        }

        private void editLine(int index) {
            StockCountLine current = index < 0 ? null : lines.get(index);
            List<Ingredient> choices = available.stream()
                    .filter(ingredient -> current != null
                            && ingredient.maNL().equals(current.maNL())
                            || lines.stream().noneMatch(line ->
                                    line.maNL().equals(ingredient.maNL())))
                    .toList();
            if (choices.isEmpty()) {
                Ui.info(this, "Tất cả nguyên liệu đã có trong phiếu.");
                return;
            }
            JComboBox<Ingredient> ingredient =
                    new JComboBox<>(choices.toArray(Ingredient[]::new));
            ingredient.setRenderer(new javax.swing.DefaultListCellRenderer() {
                @Override
                public java.awt.Component getListCellRendererComponent(
                        javax.swing.JList<?> list, Object value, int itemIndex,
                        boolean selected, boolean focus) {
                    super.getListCellRendererComponent(
                            list, value, itemIndex, selected, focus);
                    if (value instanceof Ingredient item) {
                        setText(item.tenNL() + " · " + item.donViTinh()
                                + " · tồn " + decimal(item.soLuongTon()));
                    }
                    return this;
                }
            });
            JTextField actual = Ui.field(14);
            JTextField reason = Ui.field(24);
            if (current != null) {
                choices.stream()
                        .filter(item -> item.maNL().equals(current.maNL()))
                        .findFirst()
                        .ifPresent(ingredient::setSelectedItem);
                ingredient.setEnabled(false);
                actual.setText(current.soLuongThucTe().toPlainString());
                reason.setText(current.lyDo() == null ? "" : current.lyDo());
            } else {
                Ingredient selected = (Ingredient) ingredient.getSelectedItem();
                actual.setText(selected.soLuongTon().toPlainString());
                ingredient.addActionListener(event -> {
                    Ingredient item = (Ingredient) ingredient.getSelectedItem();
                    if (item != null) {
                        actual.setText(item.soLuongTon().toPlainString());
                    }
                });
            }
            JPanel form = new JPanel(new java.awt.GridBagLayout());
            addRow(form, 0, "Nguyên liệu", ingredient);
            addRow(form, 1, "Số thực tế", actual);
            addRow(form, 2, "Lý do chênh lệch", reason);
            while (true) {
                if (JOptionPane.showConfirmDialog(
                        this, form,
                        current == null ? "Thêm dòng kiểm kê" : "Sửa dòng kiểm kê",
                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    Ingredient selected = (Ingredient) ingredient.getSelectedItem();
                    BigDecimal quantity = Ui.decimal(
                            actual.getText(), "Số thực tế");
                    if (quantity.signum() < 0) {
                        throw new IllegalArgumentException(
                                "Số thực tế không được âm.");
                    }
                    StockCountLine line =
                            toCountLine(selected, quantity, reason.getText());
                    if (current == null) {
                        lines.add(line);
                    } else {
                        lines.set(index, line);
                    }
                    refreshLines();
                    return;
                } catch (RuntimeException ex) {
                    Ui.error(this, ex);
                }
            }
        }

        private void removeLine() {
            try {
                lines.remove(Ui.selectedModelRow(lineTable));
                refreshLines();
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }

        private void refreshLines() {
            lineModel.setRowCount(0);
            for (StockCountLine line : lines) {
                lineModel.addRow(new Object[] {
                        line.maNL(), line.tenNL(), line.donViTinh(),
                        decimal(line.soLuongSoSach()),
                        decimal(line.soLuongThucTe()),
                        decimal(line.chenhLech()),
                        line.lyDo() == null ? "" : line.lyDo()
                });
            }
        }

        private void save(boolean submitForApproval) {
            if (saving) {
                return;
            }
            saving = true;
            try {
                String id = controller.createStockCount(
                        note.getText(), lines, submitForApproval);
                Ui.info(this, submitForApproval
                        ? "Đã tạo phiếu " + id + " và gửi duyệt."
                        : "Đã lưu nháp phiếu " + id + ".");
                dispose();
                refreshAll();
            } catch (RuntimeException ex) {
                saving = false;
                Ui.error(this, ex);
            }
        }

        private StockCountLine toCountLine(
                Ingredient ingredient, BigDecimal actual, String reason) {
            return new StockCountLine(
                    ingredient.maNL(),
                    ingredient.tenNL(),
                    ingredient.donViTinh(),
                    ingredient.soLuongTon(),
                    actual,
                    actual.subtract(ingredient.soLuongTon()),
                    reason == null ? "" : reason.trim());
        }
    }

    private final class PurchaseDialog extends JDialog {
        private final JTextField supplier = Ui.field(20);
        private final JTextField note = Ui.field(20);
        private final DefaultTableModel lineModel =
                Ui.readOnlyModel("Mã", "Nguyên liệu", "Số lượng", "Đơn giá");
        private final JTable lineTable = Ui.table(lineModel);
        private final List<PurchaseLine> lines = new ArrayList<>();
        private boolean saving;

        private PurchaseDialog() {
            super((java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(
                    InventoryPanel.this), "Lập phiếu nhập kho", true);
            setMinimumSize(new Dimension(760, 520));
            setLocationRelativeTo(InventoryPanel.this);
            JPanel root = new JPanel(new BorderLayout(12, 12));
            root.setBorder(new javax.swing.border.EmptyBorder(16, 16, 16, 16));
            JPanel header = new JPanel(new java.awt.GridBagLayout());
            addRow(header, 0, "Nhà cung cấp", supplier);
            addRow(header, 1, "Ghi chú", note);
            root.add(header, BorderLayout.NORTH);
            root.add(Ui.scroll(lineTable), BorderLayout.CENTER);
            JButton add = Ui.secondaryButton("Thêm dòng");
            JButton remove = Ui.dangerButton("Xóa dòng");
            JButton save = Ui.primaryButton("Lưu phiếu nhập");
            add.addActionListener(event -> addLine());
            remove.addActionListener(event -> removeLine());
            save.addActionListener(event -> save());
            root.add(Ui.toolbar(add, remove, save), BorderLayout.SOUTH);
            setContentPane(root);
        }

        private void addLine() {
            try {
                List<Ingredient> available = controller.list("");
                if (available.isEmpty()) {
                    Ui.info(this, "Chưa có nguyên liệu.");
                    return;
                }
                JComboBox<Ingredient> ingredient =
                        new JComboBox<>(available.toArray(Ingredient[]::new));
                ingredient.setRenderer(new javax.swing.DefaultListCellRenderer() {
                    @Override
                    public java.awt.Component getListCellRendererComponent(
                            javax.swing.JList<?> list, Object value, int index,
                            boolean selected, boolean focus) {
                        super.getListCellRendererComponent(list, value, index, selected, focus);
                        if (value instanceof Ingredient current) {
                            setText(current.tenNL() + " · " + current.donViTinh());
                        }
                        return this;
                    }
                });
                JTextField quantity = Ui.field(14);
                JTextField price = Ui.field(14);
                JPanel form = new JPanel(new java.awt.GridBagLayout());
                addRow(form, 0, "Nguyên liệu", ingredient);
                addRow(form, 1, "Số lượng", quantity);
                addRow(form, 2, "Đơn giá nhập", price);
                while (true) {
                    if (JOptionPane.showConfirmDialog(
                            this, form, "Thêm dòng nhập",
                            JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                        return;
                    }
                    try {
                        Ingredient selected = (Ingredient) ingredient.getSelectedItem();
                        if (lines.stream().anyMatch(
                                existing -> existing.maNL().equals(selected.maNL()))) {
                            throw new IllegalArgumentException(
                                    "Nguyên liệu này đã có trong phiếu nhập.");
                        }
                        PurchaseLine line = new PurchaseLine(
                                selected.maNL(),
                                Ui.decimal(quantity.getText(), "Số lượng"),
                                Ui.decimal(price.getText(), "Đơn giá"));
                        if (line.soLuong().signum() <= 0 || line.donGia().signum() < 0) {
                            throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
                        }
                        lines.add(line);
                        lineModel.addRow(new Object[] {
                                selected.maNL(), selected.tenNL(),
                                line.soLuong().stripTrailingZeros().toPlainString(),
                                Ui.money(line.donGia())
                        });
                        break;
                    } catch (RuntimeException ex) {
                        Ui.error(this, ex);
                    }
                }
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }

        private void removeLine() {
            try {
                int row = Ui.selectedModelRow(lineTable);
                lines.remove(row);
                lineModel.removeRow(row);
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }

        private void save() {
            if (saving) {
                return;
            }
            saving = true;
            try {
                String id = controller.createPurchaseReceipt(
                        supplier.getText(), note.getText(), lines);
                Ui.info(this, "Đã lập phiếu nhập " + id + ".");
                dispose();
                refreshAll();
            } catch (RuntimeException ex) {
                saving = false;
                Ui.error(this, ex);
            }
        }
    }

    private static void addRow(
            JPanel form, int row, String label, java.awt.Component component) {
        form.add(new JLabel(label), Ui.gbc(0, row));
        form.add(component, Ui.gbc(1, row));
    }

    private static String decimal(BigDecimal value) {
        return value == null
                ? ""
                : value.stripTrailingZeros().toPlainString();
    }
}
