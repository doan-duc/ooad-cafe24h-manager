package model;

import java.math.BigDecimal;

// Tóm tắt: Chi tiết nhập kho (mã nguyên liệu, số lượng, đơn giá)
public record PurchaseLine(String maNL, BigDecimal soLuong, BigDecimal donGia) {
}
