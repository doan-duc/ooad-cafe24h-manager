package model;

// Tóm tắt: Dữ liệu khu vực (mã, tên, mô tả)
public record Area(String maKhuVuc, String tenKhuVuc, String moTa) {
    @Override
    public String toString() {
        return tenKhuVuc;
    }
}
