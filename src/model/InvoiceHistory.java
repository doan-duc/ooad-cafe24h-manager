package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Tóm tắt: Lịch sử hóa đơn (mã, ngày lập, tiền giờ, tiền món, giảm giá, tổng tiền, thanh toán)
public record InvoiceHistory(
        String maHD,
        LocalDateTime ngayLap,
        BigDecimal tienGio,
        BigDecimal tienMon,
        BigDecimal tienGiam,
        BigDecimal tongTien,
        String phuongThucThanhToan,
        String loaiHoaDon) {
}
