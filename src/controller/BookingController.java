package controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import dao.BookingDao;
import dao.IBookingDao;
import model.Booking;
import security.Authorization;
import security.Permission;
import security.Session;

public final class BookingController {
    private final IBookingDao dao;

    public BookingController() {
        this(new BookingDao());
    }

    public BookingController(IBookingDao dao) {
        this.dao = dao;
    }

    // Tóm tắt: Lấy danh sách tất cả booking
    public List<Booking> list() {
        Authorization.require(Session.currentUser(), Permission.BOOKING_MANAGE);
        return dao.list();
    }

    // Tóm tắt: Tìm booking đang chờ xử lý cho bàn
    public Optional<Booking> pendingForTable(String maBan) {
        Authorization.require(Session.currentUser(), Permission.TABLE_OPERATE);
        if (maBan == null || maBan.isBlank()) {
            throw new IllegalArgumentException("Hãy chọn bàn cần nhận booking.");
        }
        return dao.pendingForTable(maBan.trim());
    }

    // Tóm tắt: Tạo booking mới cho khách hàng đã có
    public String create(
            String maKH, String maBan, LocalDateTime start, LocalDateTime end) {
        Authorization.require(Session.currentUser(), Permission.BOOKING_MANAGE);
        if (maKH == null || maKH.isBlank()
                || maBan == null || maBan.isBlank()
                || !isValidFutureRange(start, end)) {
            throw new IllegalArgumentException(
                    "Thông tin khách, bàn và khoảng thời gian booking không hợp lệ.");
        }
        return dao.create(maKH.trim(), maBan.trim(), start, end);
    }

    // Tóm tắt: Tạo booking mới cho khách lạ (qua số điện thoại)
    public String createForGuest(
            String phone, String hoTen, String maBan, LocalDateTime start, LocalDateTime end) {
        Authorization.require(Session.currentUser(), Permission.BOOKING_MANAGE);
        if (phone == null || phone.isBlank()
                || maBan == null || maBan.isBlank()
                || !isValidFutureRange(start, end)) {
            throw new IllegalArgumentException("Số điện thoại, bàn và khoảng thời gian booking không hợp lệ.");
        }
        if (!phone.trim().matches("\\d{9,15}")) {
            throw new IllegalArgumentException("Số điện thoại phải có 9 đến 15 chữ số.");
        }
        String name = (hoTen == null || hoTen.isBlank())
                ? ("Khách lạ " + phone.trim()) : hoTen.trim();
        return dao.createForGuest(phone.trim(), name, maBan.trim(), start, end);
    }

    // Tóm tắt: Hủy booking
    public void cancel(String maBooking) {
        Authorization.require(Session.currentUser(), Permission.BOOKING_MANAGE);
        if (maBooking == null || maBooking.isBlank()) {
            throw new IllegalArgumentException("Hãy chọn booking cần hủy.");
        }
        dao.cancel(maBooking.trim());
    }

    private static boolean isValidFutureRange(LocalDateTime start, LocalDateTime end) {
        return start != null
                && end != null
                && end.isAfter(start)
                && start.isAfter(LocalDateTime.now());
    }
}
