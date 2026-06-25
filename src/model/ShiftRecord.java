package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Tóm tắt: Ghi chép cốt ca (mã ca, nhân viên, tiền đầu ca, tiền hệ thống, tiền thực tế, chênh lệch)
public record ShiftRecord(
        String maChotCa,
        String maCa,
        String tenCa,
        String maNV,
        String tenNhanVien,
        BigDecimal tienDauCa,
        BigDecimal tienHeThong,
        BigDecimal tienThucTe,
        BigDecimal chenhLech,
        String lyDoChenhLech,
        LocalDateTime thoiGianChot,
        String trangThaiChot) {
}
