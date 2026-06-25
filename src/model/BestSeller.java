package model;

import java.math.BigDecimal;

// Tóm tắt: Thông tin món bán chạy nhất (mã, tên, loại, số lượng bán, doanh thu)
public record BestSeller(
        String maMon,
        String tenMon,
        String loaiMon,
        long soLuongBan,
        BigDecimal doanhThu) {
}
