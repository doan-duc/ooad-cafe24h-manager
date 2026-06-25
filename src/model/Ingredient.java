package model;

import java.math.BigDecimal;

// Tóm tắt: Dữ liệu nguyên liệu (mã, tên, đơn vị tính, số lượng tồn, mức cảnh báo, trạng thái)
public record Ingredient(
        String maNL,
        String tenNL,
        String donViTinh,
        BigDecimal soLuongTon,
        BigDecimal mucCanhBao,
        String trangThaiTon) {
}
