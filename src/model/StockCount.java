package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Tóm tắt: Phiếu kiểm kê (mã phiếu, ngày kiểm kê, người kiểm, người duyệt, chênh lệch, trạng thái)
public record StockCount(
        String maPhieuKK,
        LocalDateTime ngayKiemKe,
        String ghiChu,
        String maNV,
        String tenNguoiKiem,
        String maNVDuyet,
        String tenNguoiDuyet,
        int soDong,
        BigDecimal tongChenhLech,
        String trangThai,
        String lyDoTuChoi) {

    // Tóm tắt: Kiểm tra phiếu có ở trạng thái nháp
    public boolean isDraft() {
        return "Nháp".equals(trangThai);
    }

    // Tóm tắt: Kiểm tra phiếu có ở trạng thái chờ duyệt
    public boolean isPending() {
        return "Chờ duyệt".equals(trangThai);
    }

    // Tóm tắt: Kiểm tra phiếu có ở trạng thái từ chối
    public boolean isRejected() {
        return "Từ chối".equals(trangThai);
    }
}
