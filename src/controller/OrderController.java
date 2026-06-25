package controller;

import java.util.List;
import java.util.Set;

import dao.IOrderDao;
import dao.OrderDao;
import model.MenuItem;
import model.OrderLine;
import security.Authorization;
import security.Permission;
import security.Session;

public final class OrderController {
    private static final Set<String> KITCHEN_STATUSES =
            Set.of("DangPha", "DaPha", "DaGiao", "DaHuy");

    private final IOrderDao dao;

    public OrderController() {
        this(new OrderDao());
    }

    public OrderController(IOrderDao dao) {
        this.dao = dao;
    }

    // Tóm tắt: Lấy danh sách món dựa trên từ khóa tìm kiếm (tên, danh mục, loại)
    public List<MenuItem> layThongTinMon(String keyword) {
        Authorization.require(Session.currentUser(), Permission.ORDER_CREATE);
        return dao.searchMenu(keyword, "");
    }

    // Tóm tắt: Lấy danh sách chi tiết hóa đơn (tất cả dòng món của hóa đơn)
    public List<OrderLine> invoiceLines(String maHD) {
        Authorization.require(Session.currentUser(), Permission.ORDER_CREATE);
        return dao.listInvoiceLines(requireInvoiceId(maHD));
    }

    // Tóm tắt: Thêm một dòng món vào hóa đơn với số lượng, ghi chú
    public void xuLyGoiMon(String maHD, String maMon, int soLuong, String ghiChu) {
        Authorization.require(Session.currentUser(), Permission.ORDER_CREATE);
        validateLine(maHD, maMon, soLuong);
        dao.addItem(maHD.trim(), maMon.trim(), soLuong, ghiChu);
    }

    // Tóm tắt: Cập nhật số lượng hoặc ghi chú của một dòng món trong hóa đơn
    public void update(int maCTHD, int quantity, String note) {
        Authorization.require(Session.currentUser(), Permission.ORDER_CREATE);
        if (maCTHD <= 0 || quantity <= 0) {
            throw new IllegalArgumentException("Số lượng món phải lớn hơn 0.");
        }
        dao.updateLine(maCTHD, quantity, note);
    }

    // Tóm tắt: Hủy một dòng món (đánh dấu trạng thái DaHuy)
    public void cancel(int maCTHD) {
        Authorization.require(Session.currentUser(), Permission.ORDER_CREATE);
        requireLineId(maCTHD);
        dao.updateStatus(maCTHD, "DaHuy");
    }

    // Tóm tắt: Lấy danh sách tất cả dòng món đang chờ pha chế hoặc đang pha ở bếp
    public List<OrderLine> kitchenLines() {
        Authorization.require(Session.currentUser(), Permission.KITCHEN_OPERATE);
        return dao.listKitchenLines();
    }

    // Tóm tắt: Cập nhật trạng thái pha chế của một dòng món (DangPha, DaPha, DaGiao, DaHuy)
    public void kitchenStatus(int maCTHD, String status) {
        Authorization.require(Session.currentUser(), Permission.KITCHEN_OPERATE);
        requireLineId(maCTHD);
        if (status == null || !KITCHEN_STATUSES.contains(status.trim())) {
            throw new IllegalArgumentException("Thông tin cập nhật món không hợp lệ.");
        }
        dao.updateStatus(maCTHD, status.trim());
    }

    private static void validateLine(String maHD, String maMon, int quantity) {
        if (maHD == null || maHD.isBlank()
                || maMon == null || maMon.isBlank()
                || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Hóa đơn, món và số lượng lớn hơn 0 là bắt buộc.");
        }
    }

    private static String requireInvoiceId(String maHD) {
        if (maHD == null || maHD.isBlank()) {
            throw new IllegalArgumentException("Hãy chọn hóa đơn cần xem.");
        }
        return maHD.trim();
    }

    private static void requireLineId(int maCTHD) {
        if (maCTHD <= 0) {
            throw new IllegalArgumentException("Mã dòng món không hợp lệ.");
        }
    }
}
