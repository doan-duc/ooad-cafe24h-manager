package model;

// Tóm tắt: Cấu hình bàn (mã bàn, tên, loại vị trí, sức chứa, khu vực, trạng thái)
public record TableConfig(
        String maBan,
        String tenBan,
        String loaiViTri,
        int sucChua,
        String trangThai,
        String maKhuVuc,
        String tenKhuVuc) {
}
