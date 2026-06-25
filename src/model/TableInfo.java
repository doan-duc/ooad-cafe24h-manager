package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Tóm tắt: Thông tin bàn (cấu hình bàn, phiên sử dụng, khách hàng, tiền món, thời gian)
public record TableInfo(
        String maBan,
        String tenBan,
        String loaiViTri,
        int sucChua,
        String trangThai,
        String maKhuVuc,
        String tenKhuVuc,
        String maPhien,
        LocalDateTime thoiGianVao,
        String maKH,
        String tenKhachHang,
        String soDienThoai,
        String maHD,
        BigDecimal tienMon,
        Integer soPhutDaDung) {
}
