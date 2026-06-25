package dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import db.Db;
import db.SqlErrors;
import model.Employee;

/** SQL Server implementation of authentication data access. */
public final class AuthDao implements IAuthDao {

    // Tóm tắt: Đếm tổng số nhân viên
    @Override
    public int countEmployees() {
        return count("SELECT COUNT(*) FROM dbo.NhanVien",
                "Không đọc được số nhân viên");
    }

    // Tóm tắt: Đếm số tài khoản hợp lệ (đã mã hóa BCrypt)
    @Override
    public int countUsableAccounts() {
        return count("""
                SELECT COUNT(*)
                FROM dbo.NhanVien
                WHERE LEN(MatKhau) = 60
                  AND (MatKhau LIKE '$2a$%' OR MatKhau LIKE '$2b$%')
                  AND TrangThai = 'Active'
                """, "Không kiểm tra được tài khoản đăng nhập");
    }

    // Tóm tắt: Tìm nhân viên để xác thực đăng nhập (theo MaNV hoặc SoDienThoai)
    @Override
    public Employee findForLogin(String login) {
        String sql = """
                SELECT nv.MaNV, nv.HoTen, nv.SoDienThoai, nv.Email, nv.MatKhau,
                       nv.TrangThai, nv.MaVaiTro, vt.TenVaiTro
                FROM dbo.NhanVien nv
                INNER JOIN dbo.VaiTro vt ON vt.MaVaiTro = nv.MaVaiTro
                WHERE (nv.MaNV = ? OR nv.SoDienThoai = ?)
                """;
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            statement.setString(2, login);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapEmployee(result) : null;
            }
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không kiểm tra được tài khoản", ex);
        }
    }

    // Tóm tắt: Tìm nhân viên đang hoạt động theo số điện thoại hoặc email
    @Override
    public Employee findByContact(String contact) {
        String sql = """
                SELECT nv.MaNV, nv.HoTen, nv.SoDienThoai, nv.Email, nv.MatKhau,
                       nv.TrangThai, nv.MaVaiTro, vt.TenVaiTro
                FROM dbo.NhanVien nv
                INNER JOIN dbo.VaiTro vt ON vt.MaVaiTro = nv.MaVaiTro
                WHERE (nv.SoDienThoai = ? OR nv.Email = ?)
                  AND nv.TrangThai = 'Active'
                """;
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, contact);
            statement.setString(2, contact);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapEmployee(result) : null;
            }
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tra cứu được tài khoản", ex);
        }
    }

    // Tóm tắt: Cập nhật mật khẩu đã mã hóa cho nhân viên
    @Override
    public void updatePassword(String maNV, String hash) {
        String sql = "UPDATE dbo.NhanVien SET MatKhau = ? WHERE MaNV = ?";
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hash);
            statement.setString(2, maNV);
            if (statement.executeUpdate() == 0) {
                throw new IllegalStateException("Không tìm thấy tài khoản cần đổi mật khẩu.");
            }
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không cập nhật được mật khẩu", ex);
        }
    }

    // Tóm tắt: Tạo tài khoản Chủ quán đầu tiên (quy trình đặc biệt)
    @Override
    public void createFirstOwner(
            String maNV,
            String hoTen,
            String soDienThoai,
            String email,
            String passwordHash) {
        try (Connection connection = Db.getConnection();
                CallableStatement statement = connection.prepareCall(
                        "{call dbo.usp_TaoNhanVienDauTien(?,?,?,?,?)}")) {
            statement.setString(1, maNV);
            statement.setNString(2, hoTen);
            statement.setString(3, soDienThoai);
            if (email == null || email.isBlank()) {
                statement.setNull(4, Types.VARCHAR);
            } else {
                statement.setString(4, email);
            }
            statement.setString(5, passwordHash);
            statement.execute();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tạo được tài khoản Chủ quán", ex);
        }
    }

    private int count(String sql, String action) {
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        } catch (SQLException ex) {
            throw SqlErrors.wrap(action, ex);
        }
    }

    private static Employee mapEmployee(ResultSet result) throws SQLException {
        return new Employee(
                result.getString("MaNV"),
                result.getNString("HoTen"),
                result.getString("SoDienThoai"),
                result.getString("Email"),
                result.getString("MatKhau"),
                result.getString("TrangThai"),
                result.getString("MaVaiTro"),
                result.getNString("TenVaiTro"));
    }
}
