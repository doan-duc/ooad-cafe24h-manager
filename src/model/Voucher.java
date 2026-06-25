package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Tóm tắt: Dữ liệu phiếu giảm giá (mã voucher, loại giảm, giá trị, ngày hiệu lực, trạng thái)
public record Voucher(
        String maVoucher,
        String tenVoucher,
        String loaiGiam,
        BigDecimal giaTriGiam,
        LocalDateTime ngayBatDau,
        LocalDateTime ngayKetThuc,
        String trangThai) {
}
