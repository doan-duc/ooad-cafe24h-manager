package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Tóm tắt: Hóa đơn thanh toán (mã hóa đơn, thời gian, tiền, giảm giá, tổng tiền, điểm tích lũy)
public record CheckoutReceipt(
        String maHD,
        LocalDateTime thoiGianVao,
        LocalDateTime thoiGianRa,
        long tongPhut,
        BigDecimal tienMon,
        BigDecimal tienGio,
        BigDecimal tienGiam,
        BigDecimal tongTien,
        int diemTichLuyThem,
        String hangThanhVienMoi) {
}
