package model;

import java.math.BigDecimal;

// Tóm tắt: Chi tiết công thức (món ăn, nguyên liệu, số lượng tiêu hao)
public record RecipeLine(
        String maMon,
        String tenMon,
        String maNL,
        String tenNL,
        String donViTinh,
        BigDecimal soLuongTieuHao) {
}
