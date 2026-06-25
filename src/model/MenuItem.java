package model;

import java.math.BigDecimal;

// Tóm tắt: Dữ liệu món ăn (mã, tên, giá, hình ảnh, danh mục, loại, giờ quy đổi)
public record MenuItem(
        String maMon,
        String tenMon,
        BigDecimal donGia,
        String hinhAnh,
        String trangThai,
        String maDanhMuc,
        String tenDanhMuc,
        String loaiMon,
        BigDecimal soGioQuyDoi,
        Integer hanSuDung) {

    @Override
    public String toString() {
        return tenMon + " · " + donGia.toPlainString() + " đ";
    }
}
