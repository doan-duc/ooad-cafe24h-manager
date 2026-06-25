package model;

/**
 * Employee data stored in the active session.
 *
 * <p>Authorization rules are evaluated by
 * {@link security.Authorization}.
 */
public record Employee(
        String maNV,
        String hoTen,
        String soDienThoai,
        String email,
        String matKhau,
        String trangThai,
        String maVaiTro,
        String tenVaiTro) {

    // Tóm tắt: Trả về tên và vai trò nhân viên để hiển thị
    public String displayName() {
        return hoTen + " · " + tenVaiTro;
    }
}
