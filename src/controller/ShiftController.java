package controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import dao.IShiftDao;
import dao.ShiftDao;
import model.LookupItem;
import model.ShiftRecord;
import model.ShiftRegistration;
import security.Authorization;
import security.Permission;
import security.Session;

public final class ShiftController {
    private final IShiftDao dao;

    public ShiftController() {
        this(new ShiftDao());
    }

    public ShiftController(IShiftDao dao) {
        this.dao = dao;
    }

    // Tóm tắt: Lấy danh sách loại ca làm việc
    public List<LookupItem> shiftTypes() {
        requireShiftAccess();
        return dao.shiftTypes();
    }

    // Tóm tắt: Lấy danh sách nhân viên hoạt động
    public List<LookupItem> activeEmployees() {
        Authorization.require(Session.currentUser(), Permission.SHIFT_MANAGE);
        return dao.activeEmployees();
    }

    // Tóm tắt: Lấy danh sách ca đăng ký của tôi
    public List<ShiftRegistration> myRegistrations() {
        Authorization.require(Session.currentUser(), Permission.SHIFT_OPERATE);
        return dao.registrationsForEmployee(Session.currentUser().maNV());
    }

    // Tóm tắt: Lấy tất cả đăng ký ca làm
    public List<ShiftRegistration> registrations() {
        Authorization.require(Session.currentUser(), Permission.SHIFT_MANAGE);
        return dao.registrations();
    }

    // Tóm tắt: Đăng ký ca làm việc
    public String register(String maCa, LocalDate ngayLam, String ghiChu) {
        Authorization.require(Session.currentUser(), Permission.SHIFT_OPERATE);
        validateSchedule(maCa, ngayLam);
        validateNote(ghiChu);
        return dao.register(Session.currentUser().maNV(), maCa, ngayLam, ghiChu);
    }

    // Tóm tắt: Hủy đăng ký ca làm
    public void cancel(String maDangKy) {
        Authorization.require(Session.currentUser(), Permission.SHIFT_OPERATE);
        requireId(maDangKy);
        dao.cancel(maDangKy, Session.currentUser().maNV());
    }

    // Tóm tắt: Duyệt đăng ký ca làm
    public void approve(String maDangKy, String maCa, LocalDate ngayLam) {
        Authorization.require(Session.currentUser(), Permission.SHIFT_MANAGE);
        requireId(maDangKy);
        validateSchedule(maCa, ngayLam);
        dao.approve(maDangKy, Session.currentUser().maNV(), maCa, ngayLam);
    }

    // Tóm tắt: Từ chối đăng ký ca làm
    public void reject(String maDangKy, String reason) {
        Authorization.require(Session.currentUser(), Permission.SHIFT_MANAGE);
        requireId(maDangKy);
        validateNote(reason);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Phải nhập lý do từ chối đăng ký ca.");
        }
        dao.reject(maDangKy, Session.currentUser().maNV(), reason.trim());
    }

    // Tóm tắt: Phân công ca làm việc cho nhân viên
    public String assign(
            String maNV, String maCa, LocalDate ngayLam, String ghiChu) {
        Authorization.require(Session.currentUser(), Permission.SHIFT_MANAGE);
        if (maNV == null || maNV.isBlank()) {
            throw new IllegalArgumentException("Hãy chọn nhân viên cần phân công.");
        }
        if (Session.currentUser().maNV().equals(maNV)) {
            throw new IllegalArgumentException(
                    "Người phân công không thể đồng thời là người được phân công.");
        }
        validateSchedule(maCa, ngayLam);
        validateNote(ghiChu);
        return dao.assign(
                maNV, maCa, ngayLam, ghiChu, Session.currentUser().maNV());
    }

    // Tóm tắt: Lấy ca làm hiện tại của nhân viên
    public ShiftRecord current() {
        requireShiftAccess();
        return dao.findOpen(Session.currentUser().maNV());
    }

    // Tóm tắt: Mở ca làm việc
    public String open(String maCa, BigDecimal openingCash) {
        Authorization.require(Session.currentUser(), Permission.SHIFT_OPERATE);
        if (maCa == null || maCa.isBlank()
                || openingCash == null || openingCash.signum() < 0) {
            throw new IllegalArgumentException("Thông tin mở ca không hợp lệ.");
        }
        return dao.open(maCa, Session.currentUser().maNV(), openingCash);
    }

    // Tóm tắt: Chốt ca làm việc
    public void close(String maChotCa, BigDecimal actualCash, String reason) {
        Authorization.require(Session.currentUser(), Permission.SHIFT_OPERATE);
        if (maChotCa == null || maChotCa.isBlank()
                || actualCash == null || actualCash.signum() < 0) {
            throw new IllegalArgumentException("Thông tin chốt ca không hợp lệ.");
        }
        dao.close(maChotCa, Session.currentUser().maNV(), actualCash, reason);
    }

    // Tóm tắt: Lấy lịch sử các ca làm việc
    public List<ShiftRecord> history() {
        Authorization.require(Session.currentUser(), Permission.SHIFT_MANAGE);
        return dao.history();
    }

    private static void requireShiftAccess() {
        if (!Authorization.can(Session.currentUser(), Permission.SHIFT_OPERATE)
                && !Authorization.can(Session.currentUser(), Permission.SHIFT_MANAGE)) {
            throw new SecurityException("Tài khoản không có quyền xem ca làm việc.");
        }
    }

    private static void validateSchedule(String maCa, LocalDate ngayLam) {
        if (maCa == null || maCa.isBlank() || ngayLam == null) {
            throw new IllegalArgumentException("Thông tin ca làm không hợp lệ.");
        }
        if (ngayLam.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Không thể xếp ca cho ngày đã qua.");
        }
    }

    private static void validateNote(String note) {
        if (note != null && note.trim().length() > 255) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá 255 ký tự.");
        }
    }

    private static void requireId(String maDangKy) {
        if (maDangKy == null || maDangKy.isBlank()) {
            throw new IllegalArgumentException("Mã đăng ký ca không hợp lệ.");
        }
    }
}
