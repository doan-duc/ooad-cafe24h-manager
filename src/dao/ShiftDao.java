package dao;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import db.Db;
import db.SqlErrors;
import model.LookupItem;
import model.ShiftRecord;
import model.ShiftRegistration;

public final class ShiftDao implements IShiftDao {
    // Tóm tắt: Lấy danh sách khung ca làm việc
    public List<LookupItem> shiftTypes() {
        List<LookupItem> shifts = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT MaCa, TenCa FROM dbo.CaLamViec ORDER BY MaCa");
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                shifts.add(new LookupItem(result.getString(1), result.getNString(2)));
            }
            return shifts;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được khung ca", ex);
        }
    }

    // Tóm tắt: Lấy danh sách nhân viên đang hoạt động
    public List<LookupItem> activeEmployees() {
        List<LookupItem> employees = new ArrayList<>();
        String sql = """
                SELECT nv.MaNV, nv.HoTen, vt.TenVaiTro
                FROM dbo.NhanVien nv
                INNER JOIN dbo.VaiTro vt ON vt.MaVaiTro = nv.MaVaiTro
                WHERE nv.TrangThai = 'Active'
                ORDER BY nv.HoTen, nv.MaNV
                """;
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                employees.add(new LookupItem(
                        result.getString("MaNV"),
                        result.getNString("HoTen") + " · " + result.getNString("TenVaiTro")));
            }
            return employees;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được danh sách nhân viên", ex);
        }
    }

    // Tóm tắt: Đăng ký ca làm việc
    public String register(
            String maNV, String maCa, LocalDate ngayLam, String ghiChu) {
        try (Connection connection = Db.getConnection()) {
            return register(connection, maNV, maCa, ngayLam, ghiChu);
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không đăng ký được ca", ex);
        }
    }

    // Tóm tắt: Duyệt đơn đăng ký ca (có thể thay đổi ca/ngày)
    public void approve(
            String maDangKy,
            String maNVDuyet,
            String maCaMoi,
            LocalDate ngayLamMoi) {
        try (Connection connection = Db.getConnection()) {
            approve(connection, maDangKy, maNVDuyet, maCaMoi, ngayLamMoi);
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không duyệt được đăng ký ca", ex);
        }
    }

    // Tóm tắt: Từ chối đơn đăng ký ca
    public void reject(String maDangKy, String maNVDuyet, String ghiChu) {
        try (Connection connection = Db.getConnection();
                CallableStatement statement = connection.prepareCall(
                        "{call dbo.usp_TuChoiDangKyCa(?,?,?)}")) {
            statement.setString(1, maDangKy);
            statement.setString(2, maNVDuyet);
            statement.setNString(3, ghiChu);
            statement.execute();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không từ chối được đăng ký ca", ex);
        }
    }

    // Tóm tắt: Hủy đơn đăng ký ca
    public void cancel(String maDangKy, String maNV) {
        try (Connection connection = Db.getConnection();
                CallableStatement statement = connection.prepareCall(
                        "{call dbo.usp_HuyDangKyCa(?,?)}")) {
            statement.setString(1, maDangKy);
            statement.setString(2, maNV);
            statement.execute();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không hủy được đăng ký ca", ex);
        }
    }

    // Tóm tắt: Phân công ca làm việc (đăng ký + duyệt trong một thao tác)
    public String assign(
            String maNV,
            String maCa,
            LocalDate ngayLam,
            String ghiChu,
            String maNVDuyet) {
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String maDangKy = register(connection, maNV, maCa, ngayLam, ghiChu);
                approve(connection, maDangKy, maNVDuyet, maCa, ngayLam);
                connection.commit();
                return maDangKy;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không phân công được ca", ex);
        }
    }

    // Tóm tắt: Lấy danh sách đăng ký ca của một nhân viên
    public List<ShiftRegistration> registrationsForEmployee(String maNV) {
        return listRegistrations(
                "WHERE d.MaNV = ? ORDER BY d.NgayLam DESC, d.ThoiGianDangKy DESC",
                maNV);
    }

    // Tóm tắt: Lấy danh sách tất cả đăng ký ca
    public List<ShiftRegistration> registrations() {
        return listRegistrations(
                "ORDER BY d.NgayLam DESC, d.ThoiGianDangKy DESC", null);
    }

    // Tóm tắt: Mở ca làm việc mới
    public String open(String maCa, String maNV, BigDecimal openingCash) {
        try (Connection connection = Db.getConnection();
                CallableStatement statement = connection.prepareCall(
                        "{call dbo.usp_MoCa(?,?,?,?)}")) {
            statement.setString(1, maCa);
            statement.setString(2, maNV);
            statement.setBigDecimal(3, openingCash);
            statement.registerOutParameter(4, Types.VARCHAR);
            statement.execute();
            return statement.getString(4);
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không mở được ca", ex);
        }
    }

    // Tóm tắt: Chốt ca (ghi nhận tiền thực tế)
    public void close(
            String maChotCa, String employeeId, BigDecimal actualCash, String reason) {
        try (Connection connection = Db.getConnection();
                CallableStatement statement = connection.prepareCall(
                        "{call dbo.usp_ChotCa(?,?,?,?)}")) {
            statement.setString(1, maChotCa);
            statement.setString(2, employeeId);
            statement.setBigDecimal(3, actualCash);
            if (reason == null || reason.isBlank()) {
                statement.setNull(4, Types.NVARCHAR);
            } else {
                statement.setNString(4, reason.trim());
            }
            statement.execute();
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không chốt được ca", ex);
        }
    }

    // Tóm tắt: Tìm ca đang mở của nhân viên
    public ShiftRecord findOpen(String maNV) {
        return list("""
                WHERE c.MaNV = ? AND c.TrangThaiChot = N'Đang mở'
                ORDER BY c.MaChotCa DESC
                """, maNV).stream().findFirst().orElse(null);
    }

    // Tóm tắt: Lấy lịch sử chốt ca
    public List<ShiftRecord> history() {
        return list("ORDER BY c.ThoiGianChot DESC, c.MaChotCa DESC", null);
    }

    private String register(
            Connection connection,
            String maNV,
            String maCa,
            LocalDate ngayLam,
            String ghiChu) throws SQLException {
        try (CallableStatement statement = connection.prepareCall(
                "{call dbo.usp_DangKyCa(?,?,?,?,?)}")) {
            statement.setString(1, maNV);
            statement.setString(2, maCa);
            statement.setDate(3, java.sql.Date.valueOf(ngayLam));
            setNullableNString(statement, 4, ghiChu);
            statement.registerOutParameter(5, Types.VARCHAR);
            statement.execute();
            return statement.getString(5);
        }
    }

    private void approve(
            Connection connection,
            String maDangKy,
            String maNVDuyet,
            String maCaMoi,
            LocalDate ngayLamMoi) throws SQLException {
        try (CallableStatement statement = connection.prepareCall(
                "{call dbo.usp_DuyetDangKyCa(?,?,?,?)}")) {
            statement.setString(1, maDangKy);
            statement.setString(2, maNVDuyet);
            if (maCaMoi == null || maCaMoi.isBlank()) {
                statement.setNull(3, Types.VARCHAR);
            } else {
                statement.setString(3, maCaMoi);
            }
            if (ngayLamMoi == null) {
                statement.setNull(4, Types.DATE);
            } else {
                statement.setDate(4, java.sql.Date.valueOf(ngayLamMoi));
            }
            statement.execute();
        }
    }

    private List<ShiftRegistration> listRegistrations(
            String condition, String parameter) {
        String sql = """
                SELECT d.MaDangKy, d.NgayLam, d.TrangThai, d.GhiChu,
                       d.ThoiGianDangKy, d.ThoiGianDuyet,
                       d.MaCa, ca.TenCa, d.MaNV, nv.HoTen,
                       d.MaNVDuyet, duyet.HoTen AS TenNhanVienDuyet
                FROM dbo.DangKyCa d
                INNER JOIN dbo.CaLamViec ca ON ca.MaCa = d.MaCa
                INNER JOIN dbo.NhanVien nv ON nv.MaNV = d.MaNV
                LEFT JOIN dbo.NhanVien duyet ON duyet.MaNV = d.MaNVDuyet
                """ + condition;
        List<ShiftRegistration> registrations = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameter != null) {
                statement.setString(1, parameter);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Timestamp registeredAt = result.getTimestamp("ThoiGianDangKy");
                    Timestamp reviewedAt = result.getTimestamp("ThoiGianDuyet");
                    registrations.add(new ShiftRegistration(
                            result.getString("MaDangKy"),
                            result.getDate("NgayLam").toLocalDate(),
                            result.getString("TrangThai"),
                            result.getNString("GhiChu"),
                            registeredAt.toLocalDateTime(),
                            reviewedAt == null ? null : reviewedAt.toLocalDateTime(),
                            result.getString("MaCa"),
                            result.getNString("TenCa"),
                            result.getString("MaNV"),
                            result.getNString("HoTen"),
                            result.getString("MaNVDuyet"),
                            result.getNString("TenNhanVienDuyet")));
                }
            }
            return registrations;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được lịch đăng ký ca", ex);
        }
    }

    private List<ShiftRecord> list(String condition, String parameter) {
        String sql = """
                SELECT c.MaChotCa, c.MaCa, ca.TenCa, c.MaNV, nv.HoTen,
                       c.TienDauCa, c.TienHeThong, c.TienThucTe, c.ChenhLech,
                       c.LyDoChenhLech, c.ThoiGianChot, c.TrangThaiChot
                FROM dbo.ChotCa c
                INNER JOIN dbo.CaLamViec ca ON ca.MaCa = c.MaCa
                INNER JOIN dbo.NhanVien nv ON nv.MaNV = c.MaNV
                """ + condition;
        List<ShiftRecord> records = new ArrayList<>();
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameter != null) {
                statement.setString(1, parameter);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Timestamp closeTime = result.getTimestamp("ThoiGianChot");
                    records.add(new ShiftRecord(
                            result.getString("MaChotCa"),
                            result.getString("MaCa"),
                            result.getNString("TenCa"),
                            result.getString("MaNV"),
                            result.getNString("HoTen"),
                            result.getBigDecimal("TienDauCa"),
                            result.getBigDecimal("TienHeThong"),
                            result.getBigDecimal("TienThucTe"),
                            result.getBigDecimal("ChenhLech"),
                            result.getNString("LyDoChenhLech"),
                            closeTime == null ? null : closeTime.toLocalDateTime(),
                            result.getNString("TrangThaiChot")));
                }
            }
            return records;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được dữ liệu ca", ex);
        }
    }

    private static void setNullableNString(
            CallableStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.NVARCHAR);
        } else {
            statement.setNString(index, value.trim());
        }
    }
}
