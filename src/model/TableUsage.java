package model;

import java.math.BigDecimal;

// Tóm tắt: Thống kê sử dụng bàn (mã bàn, tổng phiên, tổng giờ, giờ bình quân)
public record TableUsage(
        String maBan,
        String tenBan,
        String tenKhuVuc,
        long tongPhien,
        BigDecimal tongGio,
        BigDecimal tbGio) {
}
