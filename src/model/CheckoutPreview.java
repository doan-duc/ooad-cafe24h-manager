package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Tóm tắt: Xem trước thanh toán (thời gian, tiền dịch vụ, tiền giờ, tạm tính, điểm tích lũy)
public record CheckoutPreview(
        LocalDateTime thoiGianVao,
        LocalDateTime thoiGianRa,
        long tongPhut,
        BigDecimal tienMon,
        BigDecimal tienGio,
        BigDecimal tamTinh,
        int diemSeTichLuy) {
}
