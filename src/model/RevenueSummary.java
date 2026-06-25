package model;

import java.math.BigDecimal;

// Tóm tắt: Tóm lược doanh thu (số hóa đơn, doanh thu giờ, F&B, gói giờ, giảm giá, tổng)
public record RevenueSummary(
        long soHoaDon,
        BigDecimal doanhThuGio,
        BigDecimal doanhThuFnb,
        BigDecimal doanhThuGoiGio,
        BigDecimal tongGiamGia,
        BigDecimal tongDoanhThu) {
}
