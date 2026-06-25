package dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import db.Db;
import db.SqlErrors;
import model.Customer;
import model.InvoiceHistory;
import model.MenuItem;

public final class CustomerDao implements ICustomerDao {
    // Tóm tắt: Tìm khách hàng thành viên theo từ khóa
    public List<Customer> search(String keyword) {
        String value = keyword == null ? "" : keyword.trim();
        String like = "%" + value + "%";
        String sql = """
                SELECT MaKH, HoTen, SoDienThoai, Email, NgaySinh,
                       HangThanhVien, DiemTichLuy, SoDuGio
                FROM dbo.KhachHang
                WHERE LaThanhVien = 1
                  AND (? = '' OR HoTen LIKE ? OR SoDienThoai LIKE ?)
                ORDER BY HoTen
                """;
        List<Customer> customers = new ArrayList<>();
        try (Connection connection = Db.getConnection()) {
            ensureMembershipColumn(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setNString(1, value);
            statement.setNString(2, like);
            statement.setNString(3, like);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Date birthDate = result.getDate("NgaySinh");
                    customers.add(new Customer(
                            result.getString("MaKH"),
                            result.getNString("HoTen"),
                            result.getString("SoDienThoai"),
                            result.getString("Email"),
                            birthDate == null ? null : birthDate.toLocalDate(),
                            result.getNString("HangThanhVien"),
                            result.getInt("DiemTichLuy"),
                            result.getBigDecimal("SoDuGio")));
                }
            }
            }
            return customers;
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được danh sách thành viên", ex);
        }
    }

    // Tóm tắt: Thêm khách hàng thành viên mới
    public void insert(Customer customer) {
        String sql = """
                INSERT dbo.KhachHang
                    (MaKH, HoTen, SoDienThoai, Email, NgaySinh,
                     HangThanhVien, DiemTichLuy, SoDuGio, LaThanhVien)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)
                """;
        executeSave(sql, customer, true);
    }

    // Tóm tắt: Cập nhật thông tin khách hàng
    public void update(Customer customer) {
        String sql = """
                UPDATE dbo.KhachHang
                SET HoTen = ?, SoDienThoai = ?, Email = ?, NgaySinh = ?,
                    HangThanhVien = ?
                WHERE MaKH = ?
                """;
        executeSave(sql, customer, false);
    }

    // Tóm tắt: Nạp giờ sử dụng cho khách hàng
    public String[] topUpHours(
            String maKH, String maMon, String maNV, String tenPT) {
        try (Connection connection = Db.getConnection();
                CallableStatement statement = connection.prepareCall(
                        "{call dbo.usp_NapGio(?,?,?,?,?,?)}")) {
            statement.setString(1, maKH);
            statement.setString(2, maMon);
            statement.setString(3, maNV);
            statement.setNString(4, tenPT);
            statement.registerOutParameter(5, Types.VARCHAR);
            statement.registerOutParameter(6, Types.VARCHAR);
            statement.execute();
            return new String[] {
                statement.getString(5),
                statement.getString(6)
            };
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không nạp được giờ", ex);
        }
    }

    // Tóm tắt: Lấy danh sách gói giờ (sản phẩm)
    public List<MenuItem> listHourPackages() {
        return new OrderDao().searchMenu("", "Gói giờ");
    }

    // Tóm tắt: Lấy lịch sử hóa đơn (thanh toán + nạp giờ) của khách hàng
    public List<InvoiceHistory> invoiceHistory(String maKH) {
        String sql = """
                SELECT hd.MaHD, hd.NgayLap, hd.TienGio, hd.TienMon,
                       hd.TienGiam, hd.TongTien, hd.PhuongThucThanhToan,
                       hd.LoaiHoaDon
                FROM dbo.HoaDon hd
                LEFT JOIN dbo.PhienSuDung p ON p.MaPhien = hd.MaPhien
                LEFT JOIN dbo.LichSuNapGio n ON n.MaHD = hd.MaHD
                WHERE p.MaKH = ? OR n.MaKH = ?
                ORDER BY hd.NgayLap DESC
                """;
        List<InvoiceHistory> invoices = new ArrayList<>();
        try (Connection connection = Db.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, maKH);
                statement.setString(2, maKH);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        Timestamp date = result.getTimestamp("NgayLap");
                        invoices.add(new InvoiceHistory(
                                result.getString("MaHD"),
                                date == null ? null : date.toLocalDateTime(),
                                result.getBigDecimal("TienGio"),
                                result.getBigDecimal("TienMon"),
                                result.getBigDecimal("TienGiam"),
                                result.getBigDecimal("TongTien"),
                                result.getNString("PhuongThucThanhToan"),
                                result.getString("LoaiHoaDon")));
                    }
                }
                return invoices;
            }
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không tải được lịch sử hóa đơn", ex);
        }
    }

    // Tóm tắt: Tìm khách hàng thành viên theo số điện thoại
    public model.Customer findByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String sql = """
                SELECT MaKH, HoTen, SoDienThoai, Email, NgaySinh,
                       HangThanhVien, DiemTichLuy, SoDuGio
                FROM dbo.KhachHang
                WHERE SoDienThoai = ?
                  AND LaThanhVien = 1
                """;
        try (Connection connection = Db.getConnection()) {
            ensureMembershipColumn(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, phone.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    Date birthDate = result.getDate("NgaySinh");
                    return new model.Customer(
                            result.getString("MaKH"),
                            result.getNString("HoTen"),
                            result.getString("SoDienThoai"),
                            result.getString("Email"),
                            birthDate == null ? null : birthDate.toLocalDate(),
                            result.getNString("HangThanhVien"),
                            result.getInt("DiemTichLuy"),
                            result.getBigDecimal("SoDuGio"));
                }
                return null;
            }
            }
        } catch (SQLException ex) {
            throw SqlErrors.wrap("Không truy vấn được thành viên theo số điện thoại", ex);
        }
    }

    private void executeSave(String sql, Customer customer, boolean insert) {
        try (Connection connection = Db.getConnection()) {
            ensureMembershipColumn(connection);
            if (insert && promoteGuestIfNeeded(connection, customer)) {
                return;
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (insert) {
                statement.setString(index++, customer.maKH());
            }
            statement.setNString(index++, customer.hoTen());
            setNullable(statement, index++, customer.soDienThoai());
            setNullable(statement, index++, customer.email());
            if (customer.ngaySinh() == null) {
                statement.setNull(index++, Types.DATE);
            } else {
                statement.setDate(index++, Date.valueOf(customer.ngaySinh()));
            }
            statement.setNString(index++, customer.hangThanhVien());
            if (insert) {
                statement.setInt(index++, customer.diemTichLuy());
                statement.setBigDecimal(index++, customer.soDuGio());
            }
            if (!insert) {
                statement.setString(index, customer.maKH());
            }
            if (statement.executeUpdate() == 0) {
                throw new IllegalStateException("Không tìm thấy thành viên cần cập nhật.");
            }
            }
        } catch (SQLException ex) {
            throw SqlErrors.wrap(insert
                    ? "Không thêm được thành viên"
                    : "Không cập nhật được thành viên", ex);
        }
    }

    public static void ensureMembershipColumn(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                IF COL_LENGTH('dbo.KhachHang', 'LaThanhVien') IS NULL
                BEGIN
                    ALTER TABLE dbo.KhachHang
                    ADD LaThanhVien BIT NOT NULL
                        CONSTRAINT DF_KhachHang_LaThanhVien DEFAULT 1
                        WITH VALUES

                    UPDATE kh
                    SET LaThanhVien = 0
                    FROM dbo.KhachHang kh
                    WHERE kh.MaKH LIKE 'KH[0-9A-F][0-9A-F][0-9A-F][0-9A-F][0-9A-F][0-9A-F][0-9A-F][0-9A-F]'
                      AND kh.Email IS NULL
                      AND kh.DiemTichLuy = 0
                      AND kh.SoDuGio = 0
                      AND EXISTS
                      (
                          SELECT 1
                          FROM dbo.DatPhong d
                          WHERE d.MaKH = kh.MaKH
                      )
                END
                """)) {
            statement.execute();
        }
    }

    private static boolean promoteGuestIfNeeded(Connection connection, Customer customer)
            throws SQLException {
        if (customer.soDienThoai() == null || customer.soDienThoai().isBlank()) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE dbo.KhachHang
                SET HoTen = ?, Email = ?, NgaySinh = ?, HangThanhVien = ?,
                    DiemTichLuy = 0, SoDuGio = 0, LaThanhVien = 1
                WHERE SoDienThoai = ?
                  AND LaThanhVien = 0
                """)) {
            statement.setNString(1, customer.hoTen());
            setNullable(statement, 2, customer.email());
            if (customer.ngaySinh() == null) {
                statement.setNull(3, Types.DATE);
            } else {
                statement.setDate(3, Date.valueOf(customer.ngaySinh()));
            }
            statement.setNString(4, customer.hangThanhVien());
            statement.setString(5, customer.soDienThoai().trim());
            return statement.executeUpdate() > 0;
        }
    }

    private static void setNullable(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }
}
