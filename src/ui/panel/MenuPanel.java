package ui.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import controller.ConfigController;
import controller.BanhangController;
import controller.MenuController;
import model.Ingredient;
import model.LookupItem;
import model.MenuItem;
import model.RecipeLine;
import ui.Ui;

public final class MenuPanel extends JPanel implements ui.Refreshable {
    // Tóm tắt: Tải lại danh sách món mỗi khi mở lại tab
    @Override
    public void onPageShown() {
        refresh();
    }

    private final MenuController controller = new MenuController();
    private final ConfigController configController = new ConfigController();
    private final JTextField search = Ui.field(18);
    private final DefaultTableModel model = Ui.readOnlyModel(
            "Mã", "Tên món", "Danh mục", "Loại", "Đơn giá", "Trạng thái", "Giờ quy đổi");
    private final JTable table = Ui.table(model);
    private List<MenuItem> items = List.of();

    public MenuPanel() {
        super(new BorderLayout());
        table.getAccessibleContext().setAccessibleName("danh sách món và gói giờ");
        Ui.placeholder(search, "Tìm theo tên món, danh mục hoặc loại món");
        JButton find = Ui.secondaryButton("Tìm");
        JButton category = Ui.secondaryButton("Danh mục");
        JButton add = Ui.primaryButton("Thêm món");
        Ui.describe(find, "Lọc menu theo nội dung tìm kiếm.");
        Ui.describe(category, "Tạo danh mục món trước khi thêm món mới.");
        Ui.describe(add, "Thêm món bán, đồ uống hoặc gói giờ.");
        find.addActionListener(event -> refresh());
        search.addActionListener(event -> refresh());
        category.addActionListener(event -> category());
        add.addActionListener(event -> edit(null));
        JPanel page = Ui.page(
                "Menu và định mức",
                "Quản lý món bán, gói giờ và lượng nguyên liệu tiêu hao.",
                Ui.toolbar(search, find, category, add));

        JPanel card = Ui.card();
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        JButton edit = Ui.secondaryButton("Chỉnh sửa");
        JButton recipe = Ui.primaryButton("Định mức nguyên liệu");
        Ui.describe(edit, "Cập nhật thông tin món hoặc gói giờ đang chọn.");
        Ui.describe(recipe, "Khai báo lượng nguyên liệu tiêu hao cho món đang chọn.");
        edit.addActionListener(event -> {
            MenuItem item = selected();
            if (item != null) {
                edit(item);
            }
        });
        recipe.addActionListener(event -> recipe());
        card.add(Ui.toolbar(edit, recipe), BorderLayout.SOUTH);
        page.add(card, BorderLayout.CENTER);
        add(page);
        refresh();
    }

    private void refresh() {
        try {
            items = controller.list(search.getText());
            model.setRowCount(0);
            for (MenuItem item : items) {
                model.addRow(new Object[] {
                        item.maMon(), item.tenMon(), item.tenDanhMuc(), item.loaiMon(),
                        Ui.money(item.donGia()), item.trangThai(),
                        item.soGioQuyDoi() == null ? "" : item.soGioQuyDoi()
                });
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private MenuItem selected() {
        try {
            return items.get(Ui.selectedModelRow(table));
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
            return null;
        }
    }

    private void edit(MenuItem current) {
        boolean insert = current == null;
        try {
            List<LookupItem> categories = controller.categories();
            if (categories.isEmpty()) {
                Ui.info(this, "Hãy tạo ít nhất một danh mục trước.");
                return;
            }
            JTextField code = Ui.field(18);
            JTextField name = Ui.field(18);
            JTextField price = Ui.field(18);
            JTextField image = Ui.field(18);
            Ui.placeholder(code, "Ví dụ: CF001 hoặc GH001");
            Ui.placeholder(name, "Tên món hiển thị khi order");
            Ui.placeholder(price, "Ví dụ: 45000");
            Ui.placeholder(image, "Đường dẫn ảnh, có thể để trống");
            JComboBox<LookupItem> category =
                    new JComboBox<>(categories.toArray(LookupItem[]::new));
            JComboBox<String> type = new JComboBox<>(
                    new String[] {"Đồ ăn", "Đồ uống", "Gói giờ"});
            JComboBox<String> status = new JComboBox<>(
                    new String[] {"Đang bán", "Ngừng bán"});
            JTextField hours = Ui.field(18);
            JTextField expiry = Ui.field(18);
            Ui.placeholder(hours, "Chỉ nhập khi là Gói giờ");
            Ui.placeholder(expiry, "Số ngày hiệu lực của gói giờ");
            if (current != null) {
                code.setText(current.maMon());
                code.setEnabled(false);
                name.setText(current.tenMon());
                price.setText(current.donGia().toPlainString());
                image.setText(current.hinhAnh());
                selectLookup(category, current.maDanhMuc());
                type.setSelectedItem(current.loaiMon());
                status.setSelectedItem(current.trangThai());
                hours.setText(current.soGioQuyDoi() == null
                        ? "" : current.soGioQuyDoi().toPlainString());
                expiry.setText(current.hanSuDung() == null
                        ? "" : current.hanSuDung().toString());
            }
            JPanel form = new JPanel(new java.awt.GridBagLayout());
            addRow(form, 0, "Mã món", code);
            addRow(form, 1, "Tên món", name);
            addRow(form, 2, "Đơn giá", price);
            addRow(form, 3, "Danh mục", category);
            addRow(form, 4, "Loại món", type);
            addRow(form, 5, "Trạng thái", status);
            addRow(form, 6, "Số giờ quy đổi", hours);
            addRow(form, 7, "Hạn sử dụng (ngày)", expiry);
            addRow(form, 8, "Đường dẫn hình", image);
            while (true) {
                if (JOptionPane.showConfirmDialog(
                        this, form, insert ? "Thêm món" : "Sửa món",
                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    LookupItem selectedCategory = (LookupItem) category.getSelectedItem();
                    MenuItem item = new MenuItem(
                            code.getText().trim(),
                            name.getText().trim(),
                            Ui.decimal(price.getText(), "Đơn giá"),
                            blankToNull(image.getText()),
                            (String) status.getSelectedItem(),
                            selectedCategory.id(),
                            selectedCategory.name(),
                            (String) type.getSelectedItem(),
                            hours.getText().isBlank()
                                    ? null : Ui.decimal(hours.getText(), "Số giờ quy đổi"),
                            expiry.getText().isBlank()
                                    ? null : Ui.integer(expiry.getText(), "Hạn sử dụng"));
                    if (insert) {
                        controller.create(item);
                    } else {
                        controller.update(item);
                    }
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

    private void category() {
            JTextField code = Ui.field(16);
            JTextField name = Ui.field(20);
            Ui.placeholder(code, "Ví dụ: DM01");
            Ui.placeholder(name, "Ví dụ: Cà phê, Trà, Gói giờ");
        JPanel form = new JPanel(new java.awt.GridBagLayout());
        addRow(form, 0, "Mã danh mục", code);
        addRow(form, 1, "Tên danh mục", name);
        while (true) {
            if (JOptionPane.showConfirmDialog(
                    this, form, "Thêm danh mục",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                controller.saveCategory(
                        new LookupItem(code.getText().trim(), name.getText().trim()), true);
                Ui.info(this, "Đã thêm danh mục.");
                break;
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }
    }

    private void recipe() {
        MenuItem item = selected();
        if (item == null) {
            return;
        }
        if ("Gói giờ".equals(item.loaiMon())) {
            Ui.info(this, "Gói giờ không sử dụng định mức nguyên liệu.");
            return;
        }
        new RecipeDialog(item).setVisible(true);
    }

    private final class RecipeDialog extends JDialog {
        private final MenuItem item;
        private final DefaultTableModel recipeModel = Ui.readOnlyModel(
                "Mã NL", "Nguyên liệu", "Đơn vị", "Lượng tiêu hao");
        private final JTable recipeTable = Ui.table(recipeModel);
        private List<RecipeLine> lines = List.of();

        private RecipeDialog(MenuItem item) {
            super((java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(
                    MenuPanel.this), "Định mức · " + item.tenMon(), true);
            this.item = item;
            setMinimumSize(new Dimension(720, 470));
            setLocationRelativeTo(MenuPanel.this);
            JPanel root = new JPanel(new BorderLayout(12, 12));
            root.setBorder(new javax.swing.border.EmptyBorder(16, 16, 16, 16));
            root.add(Ui.scroll(recipeTable), BorderLayout.CENTER);
            JButton save = Ui.primaryButton("Thêm hoặc cập nhật");
            JButton remove = Ui.dangerButton("Xóa dòng");
            Ui.describe(save, "Thêm nguyên liệu mới hoặc cập nhật định mức đã có.");
            Ui.describe(remove, "Xóa nguyên liệu đang chọn khỏi định mức món này.");
            save.addActionListener(event -> saveLine());
            remove.addActionListener(event -> removeLine());
            root.add(Ui.toolbar(save, remove), BorderLayout.SOUTH);
            setContentPane(root);
            refreshRecipe();
        }

        private void refreshRecipe() {
            try {
                lines = configController.recipe(item.maMon());
                recipeModel.setRowCount(0);
                for (RecipeLine line : lines) {
                    recipeModel.addRow(new Object[] {
                            line.maNL(), line.tenNL(), line.donViTinh(),
                            line.soLuongTieuHao().stripTrailingZeros().toPlainString()
                    });
                }
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }

        private void saveLine() {
            try {
                List<Ingredient> ingredients = new BanhangController().list("");
                if (ingredients.isEmpty()) {
                    Ui.info(this, "Chưa có nguyên liệu trong kho.");
                    return;
                }
                JComboBox<Ingredient> ingredient =
                        new JComboBox<>(ingredients.toArray(Ingredient[]::new));
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
                JTextField amount = Ui.field(14);
                Ui.placeholder(amount, "Lượng dùng cho một món");
                JPanel form = new JPanel(new java.awt.GridBagLayout());
                addRow(form, 0, "Nguyên liệu", ingredient);
                addRow(form, 1, "Lượng tiêu hao", amount);
                while (true) {
                    if (JOptionPane.showConfirmDialog(
                            this, form, "Lưu định mức",
                            JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                        return;
                    }
                    try {
                        Ingredient selected = (Ingredient) ingredient.getSelectedItem();
                        configController.saveRecipe(new RecipeLine(
                                item.maMon(), item.tenMon(), selected.maNL(), selected.tenNL(),
                                selected.donViTinh(),
                                Ui.decimal(amount.getText(), "Lượng tiêu hao")));
                        refreshRecipe();
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
                RecipeLine line = lines.get(Ui.selectedModelRow(recipeTable));
                if (Ui.confirm(this, "Xóa nguyên liệu " + line.tenNL() + " khỏi định mức?")) {
                    configController.deleteRecipe(item.maMon(), line.maNL());
                    refreshRecipe();
                }
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }
    }

    private static void selectLookup(JComboBox<LookupItem> box, String id) {
        for (int index = 0; index < box.getItemCount(); index++) {
            if (box.getItemAt(index).id().equals(id)) {
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
