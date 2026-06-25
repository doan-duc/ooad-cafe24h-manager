package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import db.Db;
import db.SqlErrors;
import model.Employee;
import model.LookupItem;

/** SQL Server implementation of employee management data access. */
public final class EmployeeDao implements IEmployeeDao {

    // Tóm tắt: Tìm danh sách nhân viên theo từ khóa
    @Override
    public List<Employee> listEmployees(String keyword) {
        String sql = """
                SELECT nv.MaNV, nv.HoTen, nv.SoDienThoai, nv.Email,
                       CAST(NULL AS VARCHAR(60)) AS MatKhau,
                       nv.TrangThai, nv.MaVaiTro, vt.TenVaiTro
                FROM dbo.NhanVien nv
                INNER JOIN dbo.VaiTro vt ON vt.MaVaiTro = nv.MaVaiTro
                WHERE ? = '' OR nv.MaNV LIKE ? OR nv.HoTen LIKE ?
                   OR nv.SoDienThoai LIKE ? OR vt.TenVaiTro LIKE ?
                ORDER BY nv.MaNV
                """;
        String value = keyword == null ? "" : keyword.trim();
        String like = "%" + value + "%";
        List<Employee> employees = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setNString(1, value);
            for (int index = 2; index <= 5; index++) {
                statement.setNString(index, like);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    employees.add(mapEmployee(result));
                }
            }
            return employees;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được danh sách nhân viên", ex);
        }
    }

    // Tóm tắt: Lấy danh sách vai trò
    @Override
    public List<LookupItem> listRoles() {
        List<LookupItem> roles = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT MaVaiTro, TenVaiTro FROM dbo.VaiTro ORDER BY MaVaiTro");
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                roles.add(new LookupItem(result.getString(1), result.getNString(2)));
            }
            return roles;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được vai trò", ex);
        }
    }

    // Tóm tắt: Thêm nhân viên mới
    @Override
    public void insertEmployee(Employee employee, String passwordHash) {
        String sql = """
                INSERT dbo.NhanVien
                    (MaNV, HoTen, SoDienThoai, Email, MatKhau, TrangThai, MaVaiTro)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bindEmployee(statement, employee, passwordHash);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không thêm được nhân viên", ex);
        }
    }

    // Tóm tắt: Cập nhật thông tin nhân viên (kiểm tra Chủ quán tối thiểu)
    @Override
    public void updateEmployee(Employee employee) {
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                String currentRole;
                String currentStatus;
                try (PreparedStatement current = connection.prepareStatement("""
                        SELECT MaVaiTro, TrangThai
                        FROM dbo.NhanVien WITH (UPDLOCK, HOLDLOCK)
                        WHERE MaNV = ?
                        """)) {
                    current.setString(1, employee.maNV());
                    try (ResultSet result = current.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalStateException(
                                    "Không tìm thấy nhân viên cần cập nhật.");
                        }
                        currentRole = result.getString(1);
                        currentStatus = result.getString(2);
                    }
                }

                boolean removesActiveOwner = "VT01".equals(currentRole)
                        && "Active".equals(currentStatus)
                        && (!"VT01".equals(employee.maVaiTro())
                                || !"Active".equals(employee.trangThai()));
                if (removesActiveOwner) {
                    try (PreparedStatement owners = connection.prepareStatement("""
                            SELECT COUNT(*)
                            FROM dbo.NhanVien WITH (UPDLOCK, HOLDLOCK)
                            WHERE MaVaiTro = 'VT01' AND TrangThai = 'Active'
                            """);
                            ResultSet result = owners.executeQuery()) {
                        result.next();
                        if (result.getInt(1) <= 1) {
                            throw new IllegalStateException(
                                    "Hệ thống phải còn ít nhất một Chủ quán đang hoạt động.");
                        }
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE dbo.NhanVien
                        SET HoTen = ?, SoDienThoai = ?, Email = ?,
                            TrangThai = ?, MaVaiTro = ?
                        WHERE MaNV = ?
                        """)) {
                    statement.setNString(1, employee.hoTen());
                    statement.setString(2, employee.soDienThoai());
                    setNullable(statement, 3, employee.email());
                    statement.setString(4, employee.trangThai());
                    statement.setString(5, employee.maVaiTro());
                    statement.setString(6, employee.maNV());
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không cập nhật được nhân viên", ex);
        }
    }

    // Tóm tắt: Cập nhật mật khẩu nhân viên
    @Override
    public void updatePassword(String maNV, String passwordHash) {
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE dbo.NhanVien SET MatKhau = ? WHERE MaNV = ?")) {
            statement.setString(1, passwordHash);
            statement.setString(2, maNV);
            if (statement.executeUpdate() == 0) {
                throw new IllegalStateException("Không tìm thấy nhân viên cần đổi mật khẩu.");
            }
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không đổi được mật khẩu", ex);
        }
    }

    private static void bindEmployee(
            PreparedStatement statement, Employee employee, String passwordHash)
            throws SQLException {
        statement.setString(1, employee.maNV());
        statement.setNString(2, employee.hoTen());
        statement.setString(3, employee.soDienThoai());
        setNullable(statement, 4, employee.email());
        statement.setString(5, passwordHash);
        statement.setString(6, employee.trangThai());
        statement.setString(7, employee.maVaiTro());
    }

    private static void setNullable(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
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
