package ui.panel;
import ui.ImageCache;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import controller.BookingController;
import controller.CustomerController;
import controller.OrderController;
import controller.TableController;
import model.Booking;
import model.CheckoutPreview;
import model.CheckoutReceipt;
import model.Customer;
import model.MenuItem;
import model.OrderLine;
import model.OrderRequestLine;
import model.TableInfo;
import security.Authorization;
import security.Permission;
import security.Session;
import ui.Theme;
import ui.Ui;

public final class TableMapPanel extends JPanel implements ui.Refreshable {
    // Tóm tắt: Tải lại sơ đồ bàn mỗi khi mở lại tab
    @Override
    public void onPageShown() {
        refresh();
    }

    private static final List<String> PAYMENT_METHODS =
            List.of("Tiền mặt", "Chuyển khoản QR", "Thẻ");
    private final TableController controller = new TableController();
    private final JPanel tableGrid = new JPanel(new GridLayout(0, 3, 14, 14));
    private final JLabel summary = new JLabel();

    public TableMapPanel() {
        super(new BorderLayout());
        JButton refresh = Ui.secondaryButton("Tải lại");
        Ui.describe(refresh, "Cập nhật trạng thái bàn, order và thanh toán mới nhất.");
        refresh.addActionListener(event -> refresh());
        JPanel page = Ui.page(
                "Sơ đồ bàn",
                "Theo dõi trạng thái, check-in, gọi món và thanh toán tại một nơi.",
                refresh);

        JPanel legend = Ui.toolbar(
                Ui.badge("Trống", Theme.softPrimary(), Theme.PRIMARY_DARK),
                Ui.badge("Đang phục vụ", Theme.softAccent(), Theme.ACCENT),
                Ui.badge("Đã đặt", Theme.isDark() ? new Color(40, 20, 70) : new Color(243, 232, 255), new Color(139, 92, 246)),
                Ui.badge("Cần dọn", Theme.isDark() ? new Color(60, 20, 25) : new Color(254, 226, 226), Theme.DANGER),
                summary);
        legend.setBorder(new EmptyBorder(0, 0, 4, 0));

        tableGrid.setOpaque(false);
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(legend, BorderLayout.NORTH);
        body.add(Ui.scroll(tableGrid), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        add(page);
        refresh();
    }

    private void refresh() {
        summary.setText("  Đang tải...");
        new javax.swing.SwingWorker<List<TableInfo>, Void>() {
            @Override
            protected List<TableInfo> doInBackground() {
                return controller.list();
            }

            @Override
            protected void done() {
                try {
                    List<TableInfo> tables = get();
                    tableGrid.removeAll();
                    for (TableInfo table : tables) {
                        tableGrid.add(createTableCard(table));
                    }
                    if (tables.isEmpty()) {
                        JLabel empty = new JLabel(
                                "<html><div style='padding:30px'>Chưa có bàn. "
                                        + "Chủ quán cần tạo khu vực và bàn trong mục Thiết lập cửa hàng.</div></html>");
                        empty.setForeground(Theme.MUTED);
                        tableGrid.add(empty);
                    }
                    long available = tables.stream()
                            .filter(item -> "Trống".equals(item.trangThai()))
                            .count();
                    summary.setText("  " + available + "/" + tables.size() + " bàn trống");
                    summary.setForeground(Theme.MUTED);
                    tableGrid.revalidate();
                    tableGrid.repaint();
                } catch (java.util.concurrent.ExecutionException ex) {
                    summary.setText("  Lỗi tải dữ liệu");
                    Ui.error(TableMapPanel.this, (Exception) ex.getCause());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private JPanel createTableCard(TableInfo table) {
        Color accent = statusColor(table.trangThai());
        TableCardPanel card = new TableCardPanel(table, accent);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new javax.swing.BoxLayout(
                content, javax.swing.BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(10, 2, 10, 2));
        JLabel name = new JLabel(table.tenBan());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 18f));
        name.setForeground(Theme.INK);
        JLabel area = new JLabel(table.tenKhuVuc() + " · " + table.loaiViTri()
                + " · " + table.sucChua() + " chỗ");
        area.setForeground(Theme.MUTED);
        JLabel status = new JLabel(table.trangThai());
        status.setForeground(accent.darker());
        status.setFont(status.getFont().deriveFont(Font.BOLD, 13f));
        status.setBorder(new EmptyBorder(10, 0, 7, 0));
        content.add(name);
        content.add(area);
        content.add(status);

        if (table.thoiGianVao() != null) {
            JLabel details = new JLabel(
                    (table.tenKhachHang() == null ? "Khách vãng lai" : table.tenKhachHang())
                            + " · " + table.soPhutDaDung() + " phút · "
                            + Ui.money(table.tienMon()));
            details.setForeground(Theme.MUTED);
            content.add(details);
        }
        card.add(new TableShapeView(table, accent), BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);

        JButton action = Ui.secondaryButton(actionText(table));
        Ui.describe(action, actionHint(table));
        action.setEnabled(canAct(table));
        action.addActionListener(event -> handle(table));
        card.add(action, BorderLayout.SOUTH);
        return card;
    }

    private static final class TableShapeView extends JComponent {
        private final TableInfo table;
        private final Color accent;

        private TableShapeView(TableInfo table, Color accent) {
            this.table = table;
            this.accent = accent;
            setOpaque(false);
            setPreferredSize(new Dimension(0, 128));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int centerX = width / 2;
            int tableW = Math.min(132, Math.max(92, width - 116));
            int tableH = 56;
            int tableX = centerX - tableW / 2;
            int tableY = 36;
            int chair = 22;
            int seats = Math.max(1, Math.min(10, table.sucChua()));

            Color tableFill = Theme.isDark()
                    ? new Color(28, 42, 53)
                    : new Color(245, 248, 247);
            Color chairFill = new Color(
                    accent.getRed(), accent.getGreen(), accent.getBlue(),
                    Theme.isDark() ? 110 : 75);

            g.setStroke(new BasicStroke(2f));
            g.setColor(new Color(accent.getRed(), accent.getGreen(),
                    accent.getBlue(), Theme.isDark() ? 50 : 30));
            g.fillRoundRect(tableX - 12, tableY - 12,
                    tableW + 24, tableH + 24, 34, 34);

            for (int index = 0; index < seats; index++) {
                double angle = -Math.PI / 2d + index * (Math.PI * 2d / seats);
                int radiusX = tableW / 2 + 28;
                int radiusY = tableH / 2 + 27;
                int x = Math.round(centerX + (float) Math.cos(angle) * radiusX) - chair / 2;
                int y = Math.round(tableY + tableH / 2f
                        + (float) Math.sin(angle) * radiusY) - chair / 2;
                g.setColor(chairFill);
                g.fillRoundRect(x, y, chair, chair, 10, 10);
                g.setColor(accent);
                g.drawRoundRect(x, y, chair, chair, 10, 10);
            }

            g.setColor(tableFill);
            g.fillRoundRect(tableX, tableY, tableW, tableH, 26, 26);
            g.setColor(accent);
            g.drawRoundRect(tableX, tableY, tableW, tableH, 26, 26);

            String label = table.tenBan();
            g.setFont(Theme.display(Font.BOLD, 18f));
            int textW = g.getFontMetrics().stringWidth(label);
            g.setColor(Theme.INK);
            g.drawString(label, centerX - textW / 2,
                    tableY + tableH / 2 + g.getFontMetrics().getAscent() / 2 - 3);
            g.dispose();
        }
    }

    private static final class MenuTilePanel extends JPanel implements javax.swing.Scrollable {
        private MenuTilePanel() {
            super(new TileGridLayout(160, 164, 10));
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(
                Rectangle visibleRect, int orientation, int direction) {
            return 28;
        }

        @Override
        public int getScrollableBlockIncrement(
                Rectangle visibleRect, int orientation, int direction) {
            return Math.max(visibleRect.height - 36, 164);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static final class TileGridLayout implements LayoutManager {
        private final int minimumTileWidth;
        private final int tileHeight;
        private final int gap;

        private TileGridLayout(int minimumTileWidth, int tileHeight, int gap) {
            this.minimumTileWidth = minimumTileWidth;
            this.tileHeight = tileHeight;
            this.gap = gap;
        }

        @Override
        public void addLayoutComponent(String name, Component component) {
        }

        @Override
        public void removeLayoutComponent(Component component) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets insets = parent.getInsets();
                int availableWidth = Math.max(
                        parent.getWidth() - insets.left - insets.right,
                        minimumTileWidth * 2 + gap);
                int columns = columnsFor(availableWidth);
                int rows = Math.max(1,
                        (int) Math.ceil(parent.getComponentCount() / (double) columns));
                return new Dimension(
                        availableWidth + insets.left + insets.right,
                        rows * tileHeight + Math.max(0, rows - 1) * gap
                                + insets.top + insets.bottom);
            }
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            Insets insets = parent.getInsets();
            return new Dimension(
                    minimumTileWidth + insets.left + insets.right,
                    tileHeight + insets.top + insets.bottom);
        }

        @Override
        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets insets = parent.getInsets();
                int availableWidth = Math.max(
                        parent.getWidth() - insets.left - insets.right,
                        minimumTileWidth);
                int columns = columnsFor(availableWidth);
                int tileWidth = (availableWidth - (columns - 1) * gap) / columns;
                for (int index = 0; index < parent.getComponentCount(); index++) {
                    Component child = parent.getComponent(index);
                    int row = index / columns;
                    int column = index % columns;
                    int x = insets.left + column * (tileWidth + gap);
                    int y = insets.top + row * (tileHeight + gap);
                    child.setBounds(x, y, tileWidth, tileHeight);
                }
            }
        }

        private int columnsFor(int availableWidth) {
            return Math.max(1, (availableWidth + gap) / (minimumTileWidth + gap));
        }
    }

    private boolean canAct(TableInfo table) {
        if ("Cần dọn".equals(table.trangThai())) {
            return Authorization.can(Session.currentUser(), Permission.TABLE_OPERATE);
        }
        if ("Đang phục vụ".equals(table.trangThai())) {
            return Authorization.can(Session.currentUser(), Permission.ORDER_CREATE)
                    || Authorization.can(Session.currentUser(), Permission.PAYMENT);
        }
        return Authorization.can(Session.currentUser(), Permission.TABLE_OPERATE);
    }

    private String actionText(TableInfo table) {
        return switch (table.trangThai()) {
            case "Đang phục vụ" -> "Gọi món / thanh toán";
            case "Cần dọn" -> "Bàn đã sẵn sàng";
            case "Đã đặt" -> "Nhận khách đặt chỗ";
            default -> "Đón khách và gọi món";
        };
    }

    private String actionHint(TableInfo table) {
        return switch (table.trangThai()) {
            case "Đang phục vụ" -> "Mở order hiện tại để thêm món hoặc thanh toán.";
            case "Cần dọn" -> "Đổi bàn về trạng thái Trống sau khi đã dọn.";
            case "Đã đặt" -> "Check-in khách đã đặt và gửi món đầu tiên.";
            default -> "Chọn món đầu tiên, bắt đầu tính giờ và tạo hóa đơn.";
        };
    }

    private void handle(TableInfo table) {
        try {
            switch (table.trangThai()) {
                case "Cần dọn" -> {
                    controller.markClean(table.maBan());
                    refresh();
                }
                case "Đang phục vụ" -> new SessionDialog(table).setVisible(true);
                default -> checkIn(table);
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void checkIn(TableInfo table) {
        new QuickCheckInDialog(table).setVisible(true);
    }

    private Booking pendingBooking(TableInfo table) {
        if (!"Đã đặt".equals(table.trangThai())) {
            return null;
        }
        return new BookingController().pendingForTable(table.maBan())
                .orElseThrow(() -> new IllegalStateException(
                        "Bàn đang đánh dấu Đã đặt nhưng không tìm thấy booking còn hiệu lực. "
                                + "Hãy tải lại dữ liệu hoặc kiểm tra màn hình Booking."));
    }

    private static Color statusColor(String status) {
        return switch (status) {
            case "Đang phục vụ" -> Theme.ACCENT;
            case "Đã đặt" -> new Color(139, 92, 246);
            case "Cần dọn" -> Theme.DANGER;
            default -> Theme.PRIMARY;
        };
    }

    private final class QuickCheckInDialog extends JDialog {
        private final TableInfo table;
        private final Booking reservedBooking;
        private final OrderController orderController = new OrderController();
        private final JTextField memberPhone = Ui.field(14);
        private final JLabel bookingInfo = new JLabel();
        private final JTextField menuSearch = Ui.field(20);
        private final DefaultListModel<String> categoryModel = new DefaultListModel<>();
        private final JList<String> categoryList = new JList<>(categoryModel);
        private final MenuTilePanel menuTiles = new MenuTilePanel();
        private final DefaultTableModel cartModel =
                Ui.readOnlyModel("Món", "SL", "Đơn giá", "Thành tiền", "Ghi chú");
        private final JTable cartTable = Ui.table(cartModel);
        private final JLabel total = new JLabel();
        private final JLabel cartHint = new JLabel();
        private final List<OrderRequestLine> cartLines = new ArrayList<>();
        private List<MenuItem> allItems = List.of();
        private List<MenuItem> filteredItems = List.of();
        private boolean submitting;

        private QuickCheckInDialog(TableInfo table) {
            super((java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(
                    TableMapPanel.this), "Đón khách tại " + table.tenBan(), true);
            this.table = table;
            this.reservedBooking = pendingBooking(table);
            if (reservedBooking != null) {
                setTitle("Nhận khách đặt chỗ tại " + table.tenBan());
            }
            setMinimumSize(new Dimension(1120, 700));
            setSize(1280, 760);
            Ui.placeholder(memberPhone, "Nhập SĐT nếu khách là thành viên");
            if (reservedBooking != null) {
                memberPhone.setText(reservedBooking.soDienThoai());
                memberPhone.setEnabled(false);
                bookingInfo.setText(reservedBooking.tenKhachHang() + " · "
                        + reservedBooking.soDienThoai() + " · "
                        + Ui.dateTime(reservedBooking.thoiGianBatDau()) + " - "
                        + Ui.dateTime(reservedBooking.thoiGianKetThuc()));
            } else {
                bookingInfo.setText("Không có booking, khách vãng lai hoặc khách thành viên.");
            }
            bookingInfo.setForeground(Theme.MUTED);

            JPanel root = new JPanel(new BorderLayout(14, 14));
            root.setBorder(new EmptyBorder(18, 18, 18, 18));

            JPanel heading = new JPanel(new BorderLayout(18, 0));
            heading.setOpaque(false);
            JLabel title = new JLabel(
                    "<html><b>" + table.tenBan() + "</b><br>"
                            + "<span style='font-size:11px'>"
                            + (reservedBooking == null
                                    ? "Chọn món trước khi bắt đầu tính giờ"
                                    : "Booking đã được tự nhận theo bàn, không cần nhập mã")
                            + "</span></html>");
            title.setFont(title.getFont().deriveFont(18f));
            heading.add(title, BorderLayout.WEST);
            JPanel guest = new JPanel(new java.awt.GridBagLayout());
            guest.setOpaque(false);
            guest.add(new JLabel("SĐT thành viên"), Ui.gbc(0, 0));
            guest.add(memberPhone, Ui.gbc(1, 0));
            guest.add(new JLabel("Booking"), Ui.gbc(0, 1));
            guest.add(bookingInfo, Ui.gbc(1, 1));
            heading.add(guest, BorderLayout.EAST);
            root.add(heading, BorderLayout.NORTH);

            JPanel categories = Ui.card();
            categories.setPreferredSize(new Dimension(180, 0));
            categories.add(new JLabel("Danh mục"), BorderLayout.NORTH);
            categoryList.setSelectionMode(
                    javax.swing.ListSelectionModel.SINGLE_SELECTION);
            categoryList.addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting()) {
                    filterMenu();
                }
            });
            categories.add(Ui.scroll(categoryList), BorderLayout.CENTER);

            JPanel menu = Ui.card();
            JPanel menuHeader = new JPanel(new BorderLayout(0, 8));
            menuHeader.setOpaque(false);
            JLabel menuTitle = new JLabel("Chọn món");
            menuTitle.setFont(Theme.display(Font.BOLD, 18f));
            menuHeader.add(menuTitle, BorderLayout.NORTH);
            Ui.placeholder(menuSearch, "Tìm món, danh mục hoặc loại món");
            menuSearch.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    filterMenu();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    filterMenu();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    filterMenu();
                }
            });
            menuHeader.add(menuSearch, BorderLayout.CENTER);
            menu.add(menuHeader, BorderLayout.NORTH);
            menuTiles.setOpaque(false);
            JScrollPane menuScroll = Ui.scroll(menuTiles);
            menuScroll.getVerticalScrollBar().setUnitIncrement(18);
            menu.add(menuScroll, BorderLayout.CENTER);

            JPanel cart = Ui.card();
            cart.setPreferredSize(new Dimension(420, 0));
            JPanel cartHeader = new JPanel(new BorderLayout());
            cartHeader.setOpaque(false);
            JLabel cartTitle = new JLabel("Hóa đơn tạm tính");
            cartTitle.setFont(Theme.display(Font.BOLD, 18f));
            cartHint.setForeground(Theme.MUTED);
            cartHeader.add(cartTitle, BorderLayout.WEST);
            cartHeader.add(cartHint, BorderLayout.EAST);
            cart.add(cartHeader, BorderLayout.NORTH);
            cart.add(Ui.scroll(cartTable), BorderLayout.CENTER);
            cartTable.getAccessibleContext().setAccessibleName("hóa đơn tạm tính");
            JButton decrease = Ui.secondaryButton("-");
            Ui.describe(decrease, "Giảm số lượng món đang chọn.");
            decrease.addActionListener(event -> changeSelectedQuantity(-1));
            JButton increase = Ui.secondaryButton("+");
            Ui.describe(increase, "Tăng số lượng món đang chọn.");
            increase.addActionListener(event -> changeSelectedQuantity(1));
            JButton note = Ui.secondaryButton("Ghi chú");
            Ui.describe(note, "Thêm ghi chú pha chế cho món đang chọn.");
            note.addActionListener(event -> editSelectedNote());
            JButton remove = Ui.dangerButton("Bỏ món");
            Ui.describe(remove, "Xóa món đang chọn khỏi hóa đơn tạm tính.");
            remove.addActionListener(event -> removeSelectedItem());
            cart.add(Ui.toolbar(decrease, increase, note, remove), BorderLayout.SOUTH);

            JPanel selection = new JPanel(new BorderLayout(12, 0));
            selection.setOpaque(false);
            selection.add(categories, BorderLayout.WEST);
            selection.add(menu, BorderLayout.CENTER);
            selection.add(cart, BorderLayout.EAST);
            root.add(selection, BorderLayout.CENTER);

            total.setFont(total.getFont().deriveFont(Font.BOLD, 15f));
            JButton close = Ui.secondaryButton("Đóng");
            close.addActionListener(event -> dispose());
            JButton start = Ui.primaryButton("Bắt đầu tính giờ và gửi món");
            Ui.describe(start, "Tạo hóa đơn, bắt đầu tính giờ và gửi toàn bộ món sang pha chế.");
            start.addActionListener(event -> submit(start));
            JPanel footer = new JPanel(new BorderLayout());
            footer.setOpaque(false);
            footer.add(total, BorderLayout.WEST);
            footer.add(Ui.toolbar(close, start), BorderLayout.EAST);
            root.add(footer, BorderLayout.SOUTH);

            setContentPane(root);
            setLocationRelativeTo(TableMapPanel.this);
            loadMenu();
            refreshCart();
        }

        private void loadMenu() {
            allItems = orderController.layThongTinMon("").stream()
                    .filter(item -> !"Gói giờ".equals(item.loaiMon()))
                    .toList();
            categoryModel.clear();
            categoryModel.addElement("Tất cả");
            LinkedHashSet<String> categories = new LinkedHashSet<>();
            for (MenuItem item : allItems) {
                categories.add(item.tenDanhMuc());
            }
            categories.forEach(categoryModel::addElement);
            categoryList.setSelectedIndex(0);
        }

        private void filterMenu() {
            String category = categoryList.getSelectedValue();
            String keyword = menuSearch.getText() == null
                    ? "" : menuSearch.getText().trim().toLowerCase(java.util.Locale.ROOT);
            filteredItems = allItems.stream()
                    .filter(item -> category == null || "Tất cả".equals(category)
                            || category.equals(item.tenDanhMuc()))
                    .filter(item -> keyword.isEmpty()
                            || contains(item.tenMon(), keyword)
                            || contains(item.tenDanhMuc(), keyword)
                            || contains(item.loaiMon(), keyword))
                    .toList();
            menuTiles.removeAll();
            for (MenuItem item : filteredItems) {
                menuTiles.add(new MenuTile(item));
            }
            if (filteredItems.isEmpty()) {
                JLabel empty = new JLabel(
                        "<html><div style='padding:24px'>Không tìm thấy món phù hợp.</div></html>");
                empty.setForeground(Theme.MUTED);
                menuTiles.add(empty);
            }
            menuTiles.revalidate();
            menuTiles.repaint();
        }

        private static boolean contains(String value, String keyword) {
            return value != null
                    && value.toLowerCase(java.util.Locale.ROOT).contains(keyword);
        }

        private void addItem(MenuItem item) {
            for (int index = 0; index < cartLines.size(); index++) {
                OrderRequestLine line = cartLines.get(index);
                if (line.maMon().equals(item.maMon()) && line.ghiChu().isBlank()) {
                    cartLines.set(index, new OrderRequestLine(
                            line.maMon(), line.tenMon(), line.soLuong() + 1, line.ghiChu()));
                    refreshCart();
                    selectCartRow(index);
                    return;
                }
            }
            cartLines.add(new OrderRequestLine(
                    item.maMon(), item.tenMon(), 1, ""));
            refreshCart();
            selectCartRow(cartLines.size() - 1);
        }

        private void removeSelectedItem() {
            try {
                cartLines.remove(Ui.selectedModelRow(cartTable));
                refreshCart();
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }

        private void changeSelectedQuantity(int delta) {
            try {
                int row = Ui.selectedModelRow(cartTable);
                OrderRequestLine line = cartLines.get(row);
                int next = line.soLuong() + delta;
                if (next <= 0) {
                    cartLines.remove(row);
                } else {
                    cartLines.set(row, new OrderRequestLine(
                            line.maMon(), line.tenMon(), next, line.ghiChu()));
                }
                refreshCart();
                if (!cartLines.isEmpty()) {
                    selectCartRow(Math.min(row, cartLines.size() - 1));
                }
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }

        private void editSelectedNote() {
            try {
                int row = Ui.selectedModelRow(cartTable);
                OrderRequestLine line = cartLines.get(row);
                JTextField note = Ui.field(22);
                note.setText(line.ghiChu());
                Ui.placeholder(note, "Ví dụ: ít đá, ít đường, mang sau");
                JPanel form = new JPanel(new java.awt.GridBagLayout());
                form.add(new JLabel(line.tenMon()), Ui.gbc(0, 0));
                form.add(note, Ui.gbc(1, 0));
                if (JOptionPane.showConfirmDialog(
                        this, form, "Ghi chú pha chế",
                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                cartLines.set(row, new OrderRequestLine(
                        line.maMon(), line.tenMon(), line.soLuong(), note.getText().trim()));
                refreshCart();
                selectCartRow(row);
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }

        private void selectCartRow(int row) {
            if (row >= 0 && row < cartModel.getRowCount()) {
                int viewRow = cartTable.convertRowIndexToView(row);
                if (viewRow >= 0) {
                    cartTable.setRowSelectionInterval(viewRow, viewRow);
                }
            }
        }

        private String memberIdFromPhone() {
            String phone = memberPhone.getText() == null ? "" : memberPhone.getText().trim();
            if (phone.isBlank()) {
                return null;
            }
            Customer member = new CustomerController().findMemberByPhone(phone);
            if (member == null) {
                throw new IllegalArgumentException(
                        "Không tìm thấy thành viên theo số điện thoại này. "
                                + "Nếu khách chưa đăng ký, hãy để trống hoặc đăng ký ở màn hình Thành viên.");
            }
            return member.maKH();
        }

        private void refreshCart() {
            cartModel.setRowCount(0);
            BigDecimal amount = BigDecimal.ZERO;
            int quantity = 0;
            for (OrderRequestLine line : cartLines) {
                MenuItem item = allItems.stream()
                        .filter(candidate -> candidate.maMon().equals(line.maMon()))
                        .findFirst()
                        .orElseThrow();
                BigDecimal lineTotal =
                        item.donGia().multiply(BigDecimal.valueOf(line.soLuong()));
                amount = amount.add(lineTotal);
                quantity += line.soLuong();
                cartModel.addRow(new Object[] {
                        line.tenMon(), line.soLuong(), Ui.money(item.donGia()),
                        Ui.money(lineTotal), line.ghiChu()
                });
            }
            total.setText("Tiền món tạm tính: " + Ui.money(amount));
            cartHint.setText(quantity == 0 ? "Chưa chọn món" : quantity + " món");
            menuTiles.repaint();
        }

        private int quantityFor(MenuItem item) {
            int quantity = 0;
            for (OrderRequestLine line : cartLines) {
                if (line.maMon().equals(item.maMon())) {
                    quantity += line.soLuong();
                }
            }
            return quantity;
        }

        private final class MenuTile extends JPanel {
            private static final int IMG_H = 88;
            private final MenuItem item;
            private java.awt.image.BufferedImage tileImg;

            private MenuTile(MenuItem item) {
                super(new BorderLayout(0, 0));
                this.item = item;
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setMinimumSize(new Dimension(160, 164));
                setPreferredSize(new Dimension(180, 164));

                JPanel text = new JPanel();
                text.setOpaque(false);
                text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
                text.setBorder(new EmptyBorder(8, 12, 8, 12));

                JLabel name = new JLabel(
                        "<html><div style='width:130px'><b>" + item.tenMon()
                                + "</b></div></html>");
                name.setForeground(Theme.INK);
                name.setFont(Theme.text(Font.BOLD, 13f));
                JLabel meta = new JLabel("<html><div style='width:130px'>"
                        + item.tenDanhMuc()
                        + "</div></html>");
                meta.setForeground(Theme.MUTED);
                meta.setFont(Theme.text(Font.PLAIN, 11f));
                JLabel price = new JLabel(Ui.money(item.donGia()));
                price.setForeground(Theme.ACCENT);
                price.setFont(Theme.display(Font.BOLD, 15f));

                text.add(name);
                text.add(javax.swing.Box.createVerticalStrut(2));
                text.add(meta);
                text.add(javax.swing.Box.createVerticalStrut(4));
                text.add(price);

                // Spacer for image area at top
                JPanel imgPlaceholder = new JPanel();
                imgPlaceholder.setOpaque(false);
                imgPlaceholder.setPreferredSize(new Dimension(0, IMG_H));

                add(imgPlaceholder, BorderLayout.NORTH);
                add(text, BorderLayout.CENTER);

                // Load image asynchronously
                if (item.hinhAnh() != null && !item.hinhAnh().isBlank()) {
                    ImageCache.load(item.hinhAnh(), img -> {
                        tileImg = img;
                        repaint();
                    });
                }

                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent event) {
                        if (event.getButton() == MouseEvent.BUTTON1) {
                            addItem(item);
                        }
                    }
                });
                Ui.describe(this, "Bấm để thêm " + item.tenMon() + " vào hóa đơn tạm tính.");
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);

                int w = getWidth() - 1;
                int h = getHeight() - 1;
                int selected = quantityFor(item);

                // Card background
                g.setColor(Theme.secondarySurface());
                g.fillRoundRect(0, 0, w, h, 18, 18);

                // Image area (top, clipped to rounded top corners)
                java.awt.Shape savedClip = g.getClip();
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, 18, 18));
                if (tileImg != null) {
                    java.awt.image.BufferedImage cropped =
                            ImageCache.cover(tileImg, w, IMG_H);
                    g.drawImage(cropped, 0, 0, null);
                    // Subtle bottom-fade from image into card
                    g.setPaint(new java.awt.GradientPaint(
                            0, IMG_H - 22, new Color(0, 0, 0, 0),
                            0, IMG_H, Theme.secondarySurface()));
                    g.fillRect(0, IMG_H - 22, w, 22);
                } else {
                    // Placeholder: colored gradient with initials
                    int hash = item.maMon().hashCode();
                    int r = 40 + Math.abs(hash % 60);
                    int gg2 = 60 + Math.abs((hash >> 8) % 80);
                    int b = 50 + Math.abs((hash >> 16) % 60);
                    Color bg1 = new Color(r, gg2, b);
                    Color bg2 = new Color(Math.min(r + 30, 200), Math.min(gg2 + 30, 200), Math.min(b + 30, 200));
                    g.setPaint(new java.awt.GradientPaint(0, 0, bg1, w, IMG_H, bg2));
                    g.fillRect(0, 0, w, IMG_H);
                    // Category chip
                    String cat = item.tenDanhMuc() == null ? "" : item.tenDanhMuc();
                    g.setFont(Theme.text(Font.BOLD, 11f));
                    int catW = g.getFontMetrics().stringWidth(cat) + 16;
                    g.setColor(new Color(0, 0, 0, 55));
                    g.fillRoundRect(8, IMG_H - 26, catW, 20, 10, 10);
                    g.setColor(Color.WHITE);
                    g.drawString(cat, 16, IMG_H - 11);
                }
                g.setClip(savedClip);

                // Card border
                g.setColor(selected > 0 ? Theme.ACCENT : Theme.BORDER);
                g.setStroke(new BasicStroke(selected > 0 ? 2f : 1f));
                g.drawRoundRect(0, 0, w, h, 18, 18);

                // Quantity badge
                if (selected > 0) {
                    String badge = "×" + selected;
                    g.setFont(Theme.text(Font.BOLD, 12f));
                    int badgeW = Math.max(30, g.getFontMetrics().stringWidth(badge) + 16);
                    g.setColor(Theme.ACCENT);
                    g.fillRoundRect(w - badgeW - 7, 8, badgeW, 24, 12, 12);
                    g.setColor(Theme.isDark() ? Color.BLACK : Color.WHITE);
                    g.drawString(badge,
                            w - badgeW - 7 + (badgeW - g.getFontMetrics().stringWidth(badge)) / 2,
                            26);
                }

                g.dispose();
                super.paintComponent(graphics);
            }
        }

        private void submit(JButton button) {
            if (submitting) {
                return;
            }
            if (cartLines.isEmpty()) {
                Ui.error(this, new IllegalArgumentException(
                        "Hãy chọn ít nhất một món cho khách."));
                return;
            }
            if (!Ui.confirm(this,
                    "Bắt đầu tính giờ ngay bây giờ và gửi "
                            + cartLines.size() + " dòng món?")) {
                return;
            }
            submitting = true;
            button.setEnabled(false);
            try {
                String[] result = controller.checkInWithOrder(
                        table.maBan(),
                        reservedBooking == null ? memberIdFromPhone() : reservedBooking.maKH(),
                        reservedBooking == null ? null : reservedBooking.maDatPhong(),
                        cartLines);
                Ui.info(this,
                        "Đã bắt đầu tính giờ và tạo hóa đơn " + result[1] + ".");
                dispose();
                refresh();
            } catch (RuntimeException ex) {
                submitting = false;
                button.setEnabled(true);
                Ui.error(this, ex);
            }
        }
    }

    private final class SessionDialog extends JDialog {
        private final TableInfo table;
        private final OrderController orderController = new OrderController();
        private final DefaultTableModel orderModel = Ui.readOnlyModel(
                "Món", "SL", "Đơn giá", "Thành tiền", "Trạng thái");
        private final JTable orderTable = Ui.table(orderModel);
        private final JTextField menuSearch = Ui.field(20);
        private final DefaultListModel<String> categoryModel = new DefaultListModel<>();
        private final JList<String> categoryList = new JList<>(categoryModel);
        private final MenuTilePanel menuTiles = new MenuTilePanel();
        private final JLabel orderHint = new JLabel();
        private List<MenuItem> menuItems = List.of();
        private List<MenuItem> filteredMenuItems = List.of();
        private List<OrderLine> orderLines = List.of();
        private boolean checkoutInProgress;

        private SessionDialog(TableInfo table) {
            super((java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(
                    TableMapPanel.this), "Phiên " + table.tenBan(), true);
            this.table = table;
            setMinimumSize(new Dimension(1120, 680));
            setLocationRelativeTo(TableMapPanel.this);

            JPanel root = new JPanel(new BorderLayout(14, 14));
            root.setBorder(new EmptyBorder(18, 18, 18, 18));
            JLabel title = new JLabel(table.tenBan() + " · Hóa đơn " + table.maHD());
            title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
            root.add(title, BorderLayout.NORTH);

            JPanel categories = Ui.card();
            categories.setPreferredSize(new Dimension(180, 0));
            categories.add(new JLabel("Danh mục"), BorderLayout.NORTH);
            categoryList.setSelectionMode(
                    javax.swing.ListSelectionModel.SINGLE_SELECTION);
            categoryList.addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting()) {
                    filterMenu();
                }
            });
            categories.add(Ui.scroll(categoryList), BorderLayout.CENTER);

            JPanel menuCard = Ui.card();
            JPanel menuHeader = new JPanel(new BorderLayout(0, 8));
            menuHeader.setOpaque(false);
            JLabel menuTitle = new JLabel("Gọi thêm món");
            menuTitle.setFont(Theme.display(Font.BOLD, 18f));
            menuHeader.add(menuTitle, BorderLayout.NORTH);
            Ui.placeholder(menuSearch, "Tìm món, danh mục hoặc loại món");
            menuSearch.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    filterMenu();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    filterMenu();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    filterMenu();
                }
            });
            menuCard.add(menuHeader, BorderLayout.NORTH);
            menuHeader.add(menuSearch, BorderLayout.CENTER);
            JScrollPane menuScroll = Ui.scroll(menuTiles);
            menuScroll.getVerticalScrollBar().setUnitIncrement(18);
            menuCard.add(menuScroll, BorderLayout.CENTER);

            JPanel orderCard = Ui.card();
            orderCard.setPreferredSize(new Dimension(440, 0));
            JPanel orderHeader = new JPanel(new BorderLayout());
            orderHeader.setOpaque(false);
            JLabel orderTitle = new JLabel("Order của bàn");
            orderTitle.setFont(Theme.display(Font.BOLD, 18f));
            orderHint.setForeground(Theme.MUTED);
            orderHeader.add(orderTitle, BorderLayout.WEST);
            orderHeader.add(orderHint, BorderLayout.EAST);
            orderCard.add(orderHeader, BorderLayout.NORTH);
            orderCard.add(Ui.scroll(orderTable), BorderLayout.CENTER);
            JButton decrease = Ui.secondaryButton("-");
            Ui.describe(decrease, "Giảm số lượng món đang chọn.");
            decrease.addActionListener(event -> changeSelectedLineQuantity(-1));
            JButton increase = Ui.secondaryButton("+");
            Ui.describe(increase, "Tăng số lượng món đang chọn.");
            increase.addActionListener(event -> changeSelectedLineQuantity(1));
            JButton note = Ui.secondaryButton("Ghi chú");
            Ui.describe(note, "Sửa ghi chú pha chế cho món đang chọn.");
            note.addActionListener(event -> editSelectedLineNote());
            JButton cancel = Ui.dangerButton("Hủy món đã chọn");
            Ui.describe(cancel, "Hủy dòng món đang chọn nếu bếp chưa hoàn tất.");
            cancel.addActionListener(event -> cancelLine());
            orderCard.add(Ui.toolbar(decrease, increase, note, cancel), BorderLayout.SOUTH);

            JPanel selection = new JPanel(new BorderLayout(12, 0));
            selection.setOpaque(false);
            selection.add(categories, BorderLayout.WEST);
            selection.add(menuCard, BorderLayout.CENTER);
            selection.add(orderCard, BorderLayout.EAST);
            root.add(selection, BorderLayout.CENTER);

            JButton pay = Ui.primaryButton("Thanh toán và kết thúc phiên");
            Ui.describe(pay, "Tính tiền giờ, tiền món, áp dụng voucher và chuyển bàn sang cần dọn.");
            pay.setEnabled(Authorization.can(Session.currentUser(), Permission.PAYMENT));
            pay.addActionListener(event -> checkout());
            JButton close = Ui.secondaryButton("Đóng");
            close.addActionListener(event -> dispose());
            JPanel footer = Ui.toolbar(close, pay);
            root.add(footer, BorderLayout.SOUTH);
            setContentPane(root);
            loadMenu();
            refreshOrderLines();
        }

        private void loadMenu() {
            try {
                menuItems = orderController.layThongTinMon("").stream()
                        .filter(item -> !"Gói giờ".equals(item.loaiMon()))
                        .toList();
                categoryModel.clear();
                categoryModel.addElement("Tất cả");
                LinkedHashSet<String> categories = new LinkedHashSet<>();
                for (MenuItem item : menuItems) {
                    categories.add(item.tenDanhMuc());
                }
                categories.forEach(categoryModel::addElement);
                categoryList.setSelectedIndex(0);
                filterMenu();
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }

        private void refreshOrderLines() {
            try {
                String selectedCategory = categoryList.getSelectedValue();
                orderLines = orderController.invoiceLines(table.maHD());
                orderModel.setRowCount(0);
                int quantity = 0;
                for (OrderLine line : orderLines) {
                    if (!"DaHuy".equals(line.trangThaiMon())) {
                        quantity += line.soLuong();
                    }
                    orderModel.addRow(new Object[] {
                            line.tenMon(), line.soLuong(),
                            Ui.money(line.donGia()), Ui.money(line.thanhTien()),
                            displayStatus(line.trangThaiMon())
                    });
                }
                orderHint.setText(quantity == 0 ? "Chưa có món" : quantity + " món");
                if (selectedCategory != null) {
                    for (int index = 0; index < categoryModel.size(); index++) {
                        if (selectedCategory.equals(categoryModel.get(index))) {
                            categoryList.setSelectedIndex(index);
                            break;
                        }
                    }
                }
                menuTiles.repaint();
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }

        private void filterMenu() {
            String category = categoryList.getSelectedValue();
            String keyword = menuSearch.getText() == null
                    ? "" : menuSearch.getText().trim().toLowerCase(java.util.Locale.ROOT);
            filteredMenuItems = menuItems.stream()
                    .filter(item -> category == null || "Tất cả".equals(category)
                            || category.equals(item.tenDanhMuc()))
                    .filter(item -> keyword.isEmpty()
                            || contains(item.tenMon(), keyword)
                            || contains(item.tenDanhMuc(), keyword)
                            || contains(item.loaiMon(), keyword))
                    .toList();
            menuTiles.removeAll();
            for (MenuItem item : filteredMenuItems) {
                menuTiles.add(new SessionMenuTile(item));
            }
            if (filteredMenuItems.isEmpty()) {
                JLabel empty = new JLabel(
                        "<html><div style='padding:24px'>Không tìm thấy món phù hợp.</div></html>");
                empty.setForeground(Theme.MUTED);
                menuTiles.add(empty);
            }
            menuTiles.revalidate();
            menuTiles.repaint();
        }

        private void addItem(MenuItem item) {
            try {
                orderController.xuLyGoiMon(table.maHD(), item.maMon(), 1, "");
                refreshOrderLines();
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }

        private void changeSelectedLineQuantity(int delta) {
            try {
                OrderLine line = selectedOrderLine();
                int next = line.soLuong() + delta;
                if (next <= 0) {
                    if (Ui.confirm(this, "Hủy món " + line.tenMon() + "?")) {
                        orderController.cancel(line.maCTHD());
                        refreshOrderLines();
                    }
                    return;
                }
                orderController.update(line.maCTHD(), next, line.ghiChu());
                refreshOrderLines();
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }

        private void editSelectedLineNote() {
            try {
                OrderLine line = selectedOrderLine();
                JTextField note = Ui.field(22);
                note.setText(line.ghiChu());
                Ui.placeholder(note, "Ví dụ: ít đá, ít đường, mang sau");
                JPanel form = new JPanel(new java.awt.GridBagLayout());
                form.add(new JLabel(line.tenMon()), Ui.gbc(0, 0));
                form.add(note, Ui.gbc(1, 0));
                if (JOptionPane.showConfirmDialog(
                        this, form, "Ghi chú pha chế",
                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                orderController.update(line.maCTHD(), line.soLuong(), note.getText());
                refreshOrderLines();
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }

        private OrderLine selectedOrderLine() {
            return orderLines.get(Ui.selectedModelRow(orderTable));
        }

        private int quantityFor(MenuItem item) {
            int quantity = 0;
            for (OrderLine line : orderLines) {
                if (line.maMon().equals(item.maMon())
                        && !"DaHuy".equals(line.trangThaiMon())) {
                    quantity += line.soLuong();
                }
            }
            return quantity;
        }

        private boolean contains(String value, String keyword) {
            return value != null
                    && value.toLowerCase(java.util.Locale.ROOT).contains(keyword);
        }

        private final class SessionMenuTile extends JPanel {
            private final MenuItem item;

            private SessionMenuTile(MenuItem item) {
                super(new BorderLayout(8, 6));
                this.item = item;
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setBorder(new EmptyBorder(10, 12, 10, 12));
                setMinimumSize(new Dimension(160, 106));
                setPreferredSize(new Dimension(180, 106));

                JLabel name = new JLabel(
                        "<html><div style='width:130px'><b>" + item.tenMon()
                                + "</b></div></html>");
                name.setForeground(Theme.INK);
                name.setFont(Theme.text(Font.BOLD, 14f));
                JLabel meta = new JLabel("<html><div style='width:130px'>"
                        + item.tenDanhMuc() + " · " + item.loaiMon()
                        + "</div></html>");
                meta.setForeground(Theme.MUTED);
                meta.setFont(Theme.text(Font.PLAIN, 12f));
                JLabel price = new JLabel(Ui.money(item.donGia()));
                price.setForeground(Theme.ACCENT);
                price.setFont(Theme.display(Font.BOLD, 16f));

                JPanel text = new JPanel();
                text.setOpaque(false);
                text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
                text.add(name);
                text.add(javax.swing.Box.createVerticalStrut(5));
                text.add(meta);
                add(text, BorderLayout.CENTER);
                add(price, BorderLayout.SOUTH);

                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent event) {
                        if (event.getButton() == MouseEvent.BUTTON1) {
                            addItem(item);
                        }
                    }
                });
                Ui.describe(this, "Bấm để thêm " + item.tenMon() + " vào order của bàn.");
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int selected = quantityFor(item);
                g.setColor(Theme.secondarySurface());
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g.setColor(selected > 0 ? Theme.ACCENT : Theme.BORDER);
                g.setStroke(new BasicStroke(selected > 0 ? 2f : 1f));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                if (selected > 0) {
                    String text = "x" + selected;
                    g.setFont(Theme.text(Font.BOLD, 12f));
                    int badgeW = Math.max(32, g.getFontMetrics().stringWidth(text) + 16);
                    g.setColor(Theme.ACCENT);
                    g.fillRoundRect(getWidth() - badgeW - 10, 10,
                            badgeW, 26, 14, 14);
                    g.setColor(Theme.isDark() ? Color.BLACK : Color.WHITE);
                    g.drawString(text,
                            getWidth() - badgeW - 10
                                    + (badgeW - g.getFontMetrics().stringWidth(text)) / 2,
                            28);
                }
                g.dispose();
                super.paintComponent(graphics);
            }
        }

        private void cancelLine() {
            try {
                int row = Ui.selectedModelRow(orderTable);
                OrderLine line = orderLines.get(row);
                if (Ui.confirm(this, "Hủy món " + line.tenMon() + "?")) {
                    orderController.cancel(line.maCTHD());
                    refreshOrderLines();
                }
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        }

        private void checkout() {
            if (checkoutInProgress) {
                return;
            }
            CheckoutDialog dlg = new CheckoutDialog(PAYMENT_METHODS);
            dlg.setVisible(true);
            if (dlg.wasSuccessful()) {
                dispose();
                refresh();
            }
        }

        private final class CheckoutDialog extends JDialog {
            private final JComboBox<String> methodBox;
            private final JTextField rateField = Ui.field(14);
            private final JTextField voucherField = Ui.field(14);
            private final JLabel lblTimeIn = summaryValue();
            private final JLabel lblTimeOut = summaryValue();
            private final JLabel lblDuration = summaryValue();
            private final JLabel lblFood = summaryValue();
            private final JLabel lblHour = summaryValue();
            private final JLabel lblDiscount = summaryValue();
            private final JLabel lblPoints = summaryValue();
            private final JLabel lblTotal = new JLabel("—");
            private boolean success = false;

            CheckoutDialog(List<String> methods) {
                super(SessionDialog.this, "Thanh toán · " + table.tenBan(), true);
                methodBox = new JComboBox<>(methods.toArray(String[]::new));
                rateField.setText("30000");
                Ui.placeholder(voucherField, "Bỏ trống nếu không dùng");
                lblTotal.setFont(Theme.display(Font.BOLD, 26f));
                lblTotal.setForeground(Theme.ACCENT);

                setSize(740, 460);
                setMinimumSize(new Dimension(640, 400));
                setLocationRelativeTo(SessionDialog.this);

                JPanel root = new JPanel(new BorderLayout(16, 16));
                root.setBorder(new EmptyBorder(20, 20, 16, 20));

                JPanel left = buildSummaryCard();
                left.setPreferredSize(new Dimension(310, 0));
                JPanel right = buildFormCard();
                JPanel center = new JPanel(new BorderLayout(16, 0));
                center.setOpaque(false);
                center.add(left, BorderLayout.WEST);
                center.add(right, BorderLayout.CENTER);
                root.add(center, BorderLayout.CENTER);

                JButton close = Ui.secondaryButton("Đóng");
                JButton confirm = Ui.primaryButton("Xác nhận thanh toán");
                close.addActionListener(e -> dispose());
                confirm.addActionListener(e -> doCheckout(confirm));
                JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
                footer.setOpaque(false);
                footer.add(close);
                footer.add(confirm);
                root.add(footer, BorderLayout.SOUTH);
                setContentPane(root);
                loadPreview();
            }

            private JPanel buildSummaryCard() {
                JPanel card = Ui.card();
                JPanel content = new JPanel();
                content.setOpaque(false);
                content.setLayout(new javax.swing.BoxLayout(
                        content, javax.swing.BoxLayout.Y_AXIS));

                JLabel heading = new JLabel("TÓM TẮT PHIÊN");
                heading.setForeground(Theme.MUTED);
                heading.setFont(heading.getFont().deriveFont(Font.BOLD, 10f));
                content.add(heading);
                content.add(javax.swing.Box.createVerticalStrut(12));
                content.add(summaryRow("Giờ vào", lblTimeIn));
                content.add(javax.swing.Box.createVerticalStrut(6));
                content.add(summaryRow("Giờ ra", lblTimeOut));
                content.add(javax.swing.Box.createVerticalStrut(6));
                content.add(summaryRow("Thời gian", lblDuration));
                content.add(javax.swing.Box.createVerticalStrut(14));
                content.add(summaryDivider());
                content.add(javax.swing.Box.createVerticalStrut(14));
                content.add(summaryRow("Tiền món", lblFood));
                content.add(javax.swing.Box.createVerticalStrut(6));
                content.add(summaryRow("Tiền giờ", lblHour));
                content.add(javax.swing.Box.createVerticalStrut(6));
                content.add(summaryRow("Giảm giá (voucher)", lblDiscount));
                content.add(javax.swing.Box.createVerticalStrut(6));
                content.add(summaryRow("Điểm tích lũy (dự kiến)", lblPoints));
                content.add(javax.swing.Box.createVerticalStrut(14));
                content.add(summaryDivider());
                content.add(javax.swing.Box.createVerticalStrut(12));

                JLabel totalHeading = new JLabel("TỔNG THANH TOÁN");
                totalHeading.setForeground(Theme.MUTED);
                totalHeading.setFont(totalHeading.getFont().deriveFont(Font.BOLD, 10f));
                content.add(totalHeading);
                content.add(javax.swing.Box.createVerticalStrut(6));
                content.add(lblTotal);
                card.add(content, BorderLayout.CENTER);
                return card;
            }

            private JPanel buildFormCard() {
                JPanel card = Ui.card();
                JPanel content = new JPanel(new java.awt.GridBagLayout());
                content.setOpaque(false);

                JLabel heading = new JLabel("THÔNG TIN THANH TOÁN");
                heading.setForeground(Theme.MUTED);
                heading.setFont(heading.getFont().deriveFont(Font.BOLD, 10f));
                java.awt.GridBagConstraints topSpan = new java.awt.GridBagConstraints();
                topSpan.gridx = 0; topSpan.gridy = 0; topSpan.gridwidth = 2;
                topSpan.anchor = java.awt.GridBagConstraints.WEST;
                topSpan.insets = new Insets(0, 0, 14, 0);
                content.add(heading, topSpan);

                content.add(new JLabel("Phương thức"), Ui.gbc(0, 1));
                content.add(methodBox, Ui.gbc(1, 1));
                content.add(new JLabel("Đơn giá giờ (đ)"), Ui.gbc(0, 2));
                content.add(rateField, Ui.gbc(1, 2));
                content.add(new JLabel("Voucher"), Ui.gbc(0, 3));
                content.add(voucherField, Ui.gbc(1, 3));

                java.awt.GridBagConstraints spacerGbc = new java.awt.GridBagConstraints();
                spacerGbc.gridx = 0; spacerGbc.gridy = 4; spacerGbc.gridwidth = 2;
                spacerGbc.weighty = 1;
                spacerGbc.fill = java.awt.GridBagConstraints.VERTICAL;
                content.add(new JPanel() {
                    { setOpaque(false); }
                }, spacerGbc);

                JButton calc = Ui.secondaryButton("Tính lại dự toán");
                Ui.describe(calc,
                        "Cập nhật dự toán theo đơn giá giờ và voucher đã nhập.");
                calc.addActionListener(e -> loadPreview());
                java.awt.GridBagConstraints calcGbc = new java.awt.GridBagConstraints();
                calcGbc.gridx = 0; calcGbc.gridy = 5; calcGbc.gridwidth = 2;
                calcGbc.anchor = java.awt.GridBagConstraints.EAST;
                calcGbc.insets = new Insets(8, 0, 0, 0);
                content.add(calc, calcGbc);

                card.add(content, BorderLayout.CENTER);
                return card;
            }

            private static JPanel summaryRow(String key, JLabel value) {
                JPanel row = new JPanel(new BorderLayout(4, 0));
                row.setOpaque(false);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
                JLabel keyLabel = new JLabel(key);
                keyLabel.setForeground(Theme.MUTED);
                keyLabel.setFont(keyLabel.getFont().deriveFont(Font.PLAIN, 13f));
                value.setForeground(Theme.INK);
                row.add(keyLabel, BorderLayout.WEST);
                row.add(value, BorderLayout.EAST);
                return row;
            }

            private static JPanel summaryDivider() {
                JPanel sep = new JPanel();
                sep.setOpaque(false);
                sep.setBorder(javax.swing.BorderFactory.createMatteBorder(
                        0, 0, 1, 0, Theme.BORDER));
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                sep.setPreferredSize(new Dimension(0, 1));
                return sep;
            }

            private static JLabel summaryValue() {
                JLabel label = new JLabel("—");
                label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
                label.setForeground(Theme.INK);
                return label;
            }

            private void loadPreview() {
                try {
                    BigDecimal rate = Ui.decimal(rateField.getText(), "Đơn giá giờ");
                    CheckoutPreview preview = controller.previewCheckout(
                            table.maBan(), voucherField.getText(), rate);
                    lblTimeIn.setText(Ui.dateTime(preview.thoiGianVao()));
                    lblTimeOut.setText(Ui.dateTime(preview.thoiGianRa()));
                    lblDuration.setText(preview.tongPhut() + " phút");
                    lblFood.setText(Ui.money(preview.tienMon()));
                    lblHour.setText(Ui.money(preview.tienGio()));
                    BigDecimal gross = preview.tienMon().add(preview.tienGio());
                    BigDecimal discount = gross.subtract(preview.tamTinh());
                    lblDiscount.setText(discount.compareTo(BigDecimal.ZERO) > 0
                            ? "- " + Ui.money(discount) : "0 đ");
                    lblPoints.setText(preview.diemSeTichLuy() > 0
                            ? "+" + preview.diemSeTichLuy() + " điểm" : "—");
                    lblTotal.setText(Ui.money(preview.tamTinh()));
                } catch (RuntimeException ex) {
                    Ui.error(this, ex);
                }
            }

            private void doCheckout(JButton button) {
                if (success) {
                    return;
                }
                try {
                    BigDecimal rate = Ui.decimal(rateField.getText(), "Đơn giá giờ");
                    if (!Ui.confirm(this,
                            "Xác nhận thanh toán " + lblTotal.getText() + "?")) {
                        return;
                    }
                    success = true;
                    button.setEnabled(false);
                    CheckoutReceipt receipt = controller.checkout(
                            table.maBan(),
                            (String) methodBox.getSelectedItem(),
                            voucherField.getText(),
                            rate);
                    String pointsNote = receipt.hangThanhVienMoi() != null
                            && receipt.diemTichLuyThem() > 0
                            ? "<br><br>Tích lũy: <b>+" + receipt.diemTichLuyThem()
                                    + " điểm</b>. Hạng thành viên: <b>"
                                    + receipt.hangThanhVienMoi() + "</b>"
                            : "";
                    Ui.info(this, "<html>"
                            + "<b>Thanh toán thành công</b><br>"
                            + "Hóa đơn: " + receipt.maHD() + "<br><br>"
                            + "Giờ vào: " + Ui.dateTime(receipt.thoiGianVao()) + "<br>"
                            + "Giờ ra: " + Ui.dateTime(receipt.thoiGianRa()) + "<br>"
                            + "Tổng thời gian: " + receipt.tongPhut() + " phút<br><br>"
                            + "Tiền món: " + Ui.money(receipt.tienMon()) + "<br>"
                            + "Tiền giờ: " + Ui.money(receipt.tienGio()) + "<br>"
                            + "Giảm giá: " + Ui.money(receipt.tienGiam()) + "<br>"
                            + "<b>Tổng: " + Ui.money(receipt.tongTien()) + "</b>"
                            + pointsNote
                            + "</html>");
                    dispose();
                } catch (RuntimeException ex) {
                    success = false;
                    button.setEnabled(true);
                    Ui.error(this, ex);
                }
            }

            boolean wasSuccessful() {
                return success;
            }
        }

        private String displayStatus(String status) {
            return switch (status) {
                case "ChoPhaChe" -> "Chờ pha chế";
                case "DangPha" -> "Đang pha";
                case "DaPha" -> "Đã pha";
                case "DaGiao" -> "Đã giao";
                case "DaHuy" -> "Đã hủy";
                default -> status;
            };
        }
    }
}

