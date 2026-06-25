package model;

import java.math.BigDecimal;

// Tóm tắt: Chi tiết kiểm kê (nguyên liệu, số lượng sổ sách, thực tế, chênh lệch, lý do)
public record StockCountLine(
        String maNL,
        String tenNL,
        String donViTinh,
        BigDecimal soLuongSoSach,
        BigDecimal soLuongThucTe,
        BigDecimal chenhLech,
        String lyDo) {
}
