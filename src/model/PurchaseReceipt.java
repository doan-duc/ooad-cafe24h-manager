package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Tóm tắt: Phiếu nhập kho (mã phiếu, ngày nhập, nhà cung cấp, người lập, tổng tiền, trạng thái)
public record PurchaseReceipt(
        String maPhieuNK,
        LocalDateTime ngayNhap,
        String nhaCungCap,
        String tenNguoiLap,
        String ghiChu,
        BigDecimal tongTien,
        String trangThai) {
}
