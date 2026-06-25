package controller;

import java.math.BigDecimal;
import java.util.List;

import dao.ITableDao;
import dao.TableDao;
import model.CheckoutPreview;
import model.CheckoutReceipt;
import model.OrderRequestLine;
import model.TableInfo;
import security.Authorization;
import security.Permission;
import security.Session;

public final class TableController {
    private final ITableDao dao;

    public TableController() {
        this(new TableDao());
    }

    public TableController(ITableDao dao) {
        this.dao = dao;
    }

    // Tóm tắt: Lấy danh sách tất cả bàn
    public List<TableInfo> list() {
        Authorization.require(Session.currentUser(), Permission.TABLE_VIEW);
        return dao.listTableMap();
    }

    // Tóm tắt: Nhận bàn mà không có đơn hàng
    public String[] checkIn(String maBan, String maKH, String booking) {
        Authorization.require(Session.currentUser(), Permission.TABLE_OPERATE);
        String tableId = requireTableId(maBan);
        return dao.checkIn(
                tableId, maKH, Session.currentUser().maNV(), booking);
    }

    // Tóm tắt: Nhận bàn cùng danh sách đơn hàng ban đầu
    public String[] checkInWithOrder(
            String maBan,
            String maKH,
            String booking,
            List<OrderRequestLine> lines) {
        Authorization.require(Session.currentUser(), Permission.TABLE_OPERATE);
        Authorization.require(Session.currentUser(), Permission.ORDER_CREATE);
        String tableId = requireTableId(maBan);
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException(
                    "Khách cần chọn ít nhất một món trước khi bắt đầu tính giờ.");
        }
        for (OrderRequestLine line : lines) {
            if (line == null || line.maMon() == null || line.maMon().isBlank()
                    || line.soLuong() <= 0) {
                throw new IllegalArgumentException("Giỏ món có dữ liệu không hợp lệ.");
            }
        }
        return dao.checkInWithOrder(
                tableId, maKH, Session.currentUser().maNV(), booking, lines);
    }

    // Tóm tắt: Xem trước hoá đơn thanh toán (không voucher)
    public CheckoutPreview previewCheckout(String maBan, BigDecimal hourlyRate) {
        return previewCheckout(maBan, null, hourlyRate);
    }

    // Tóm tắt: Xem trước hoá đơn thanh toán (kèm voucher)
    public CheckoutPreview previewCheckout(
            String maBan, String voucher, BigDecimal hourlyRate) {
        Authorization.require(Session.currentUser(), Permission.PAYMENT);
        validateCheckout(maBan, "preview", hourlyRate);
        return dao.previewCheckout(maBan.trim(), voucher, hourlyRate);
    }

    // Tóm tắt: Thực hiện thanh toán và lập hoá đơn
    public CheckoutReceipt checkout(
            String maBan,
            String paymentMethod,
            String voucher,
            BigDecimal hourlyRate) {
        Authorization.require(Session.currentUser(), Permission.PAYMENT);
        validateCheckout(maBan, paymentMethod, hourlyRate);
        return dao.checkout(
                maBan.trim(),
                Session.currentUser().maNV(),
                paymentMethod.trim(),
                voucher,
                hourlyRate);
    }

    private static void validateCheckout(
            String maBan, String paymentMethod, BigDecimal hourlyRate) {
        if (maBan == null || maBan.isBlank()
                || paymentMethod == null || paymentMethod.isBlank()
                || hourlyRate == null || hourlyRate.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Thông tin thanh toán hoặc đơn giá giờ không hợp lệ.");
        }
    }

    // Tóm tắt: Đánh dấu bàn đã sạch sẽ
    public void markClean(String maBan) {
        Authorization.require(Session.currentUser(), Permission.TABLE_OPERATE);
        dao.markClean(requireTableId(maBan));
    }

    private static String requireTableId(String maBan) {
        if (maBan == null || maBan.isBlank()) {
            throw new IllegalArgumentException("Hãy chọn bàn cần thao tác.");
        }
        return maBan.trim();
    }
}
