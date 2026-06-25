package model;

import java.math.BigDecimal;
import java.time.LocalDate;

// Tóm tắt: Dữ liệu khách hàng (mã, tên, điện thoại, email, ngày sinh, hạng, điểm, số dư giờ)
public record Customer(
        String maKH,
        String hoTen,
        String soDienThoai,
        String email,
        LocalDate ngaySinh,
        String hangThanhVien,
        int diemTichLuy,
        BigDecimal soDuGio) {

    @Override
    public String toString() {
        return hoTen + " · " + soDienThoai;
    }
}
