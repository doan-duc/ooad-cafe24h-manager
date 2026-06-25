package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Tóm tắt: Đăng ký ca làm (mã đăng ký, ngày làm, ca, nhân viên, trạng thái, duyệt)
public record ShiftRegistration(
        String maDangKy,
        LocalDate ngayLam,
        String trangThai,
        String ghiChu,
        LocalDateTime ngayDangKy,
        LocalDateTime ngayDuyet,
        String maCa,
        String tenCa,
        String maNV,
        String tenNhanVien,
        String maNVDuyet,
        String tenNhanVienDuyet) {
}
