package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Tóm tắt: Dữ liệu chi tiết đơn hàng (bàn, món, số lượng, giá, trạng thái)
public record OrderLine(
        int maCTHD,
        String maHD,
        String maBan,
        String tenBan,
        String maMon,
        String tenMon,
        int soLuong,
        BigDecimal donGia,
        String ghiChu,
        String trangThaiMon,
        LocalDateTime thoiGianTao,
        boolean thoiGianTaoChinhXac) {

    // Tóm tắt: Tính thành tiền = giá x số lượng
    public BigDecimal thanhTien() {
        return donGia.multiply(BigDecimal.valueOf(soLuong));
    }
}
