package model;

import java.time.LocalDateTime;

// Tóm tắt: Thông tin đặt bàn (mã, khách hàng, bàn, thời gian, trạng thái)
public record Booking(
        String maDatPhong,
        String maKH,
        String tenKhachHang,
        String soDienThoai,
        String maBan,
        String tenBan,
        LocalDateTime thoiGianBatDau,
        LocalDateTime thoiGianKetThuc,
        String trangThai) {
}
