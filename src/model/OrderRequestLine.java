package model;

// Tóm tắt: Yêu cầu đặt món (mã món, tên, số lượng, ghi chú)
public record OrderRequestLine(
        String maMon,
        String tenMon,
        int soLuong,
        String ghiChu) {
}
