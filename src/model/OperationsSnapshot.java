package model;

import java.math.BigDecimal;

// Tóm tắt: Thông tin hoạt động hiện tại (bàn đang dùng, món chờ xử lý, nguyên liệu sắp hết, ca)
public record OperationsSnapshot(
        int banDangDung,
        int monChoXuLy,
        int nguyenLieuSapHet,
        int caDangMo,
        BigDecimal doanhThuHomNay,
        boolean hienDoanhThu) {
}
